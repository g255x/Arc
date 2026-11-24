package dev.g255.arc.api.utils.world;

import dev.g255.arc.Arc;
import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.core.impl.BreakManager;
import dev.g255.arc.mod.modules.impl.combat.AutoAnchor;
import dev.g255.arc.mod.modules.impl.combat.AutoMine;
import dev.g255.arc.mod.modules.impl.player.PacketMine;
import dev.g255.arc.mod.modules.impl.render.BreakESP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * 锚 / 水晶 / 包挖 的联动中枢。
 *
 * <p>三个模块原本各算各的：{@code AutoAnchor} 只看「方块还在不在」，{@code AutoCrystal} 只看
 * {@code Arc.BREAK.isMining}，而包挖自己的进度只存在于 {@link PacketMine} 内部。结果就是
 * 明明包挖下一 tick 就要把这一格挖穿了，锚和水晶还在等它变成真正的空气，白丢一个 tick 的伤害窗口。</p>
 *
 * <p>这里把「这一格还要多久变空气」统一成一个 tick 数，三种挖源都算进去：</p>
 * <ul>
 *   <li><b>自己的包挖</b>：{@link PacketMine#progress} + {@link BreakESP#getBreakTime} 估剩余；</li>
 *   <li><b>别人（或原版挖掘）</b>：{@link BreakManager.BreakData} 的 elapsed / breakTime；</li>
 *   <li>两者都有时取更早的那个。</li>
 * </ul>
 *
 * <p>{@link #request(BlockPos)} 是反向联动：锚/水晶需要清出位置时，可以让包挖去挖掉挡路的方块，
 * 但只在包挖空闲时发起，不打断已经在挖的目标，也不在 {@link AutoMine} 开着的时候抢格。</p>
 */
public final class MineLink implements Wrapper {
   private MineLink() {
   }

   /** 自己的包挖是不是正挖这一格 */
   public static boolean selfMining(BlockPos pos) {
      if (pos == null) {
         return false;
      }

      BlockPos breaking = PacketMine.getBreakPos();
      return breaking != null && breaking.equals(pos);
   }

   /** 除自己包挖以外，还有人在挖这一格吗（含原版挖掘的破块进度包） */
   public static boolean otherMining(BlockPos pos) {
      return pos != null && Arc.BREAK != null && Arc.BREAK.isMining(pos, false);
   }

   public static boolean mining(BlockPos pos) {
      return selfMining(pos) || otherMining(pos);
   }

   /** 自己的包挖还要几 tick 挖穿；没在挖返回 -1 */
   public static int selfEtaTicks(BlockPos pos) {
      if (!selfMining(pos) || mc.world == null || mc.player == null) {
         return -1;
      }

      double total = Math.max(BreakESP.getBreakTime(pos, false), 50.0);
      double left = (1.0 - MathHelper.clamp(PacketMine.progress, 0.0, 1.0)) * total;
      return (int)Math.ceil(left / 50.0);
   }

   /** 别人挖这一格还要几 tick；没有记录返回 -1，已经空了返回 0 */
   public static int otherEtaTicks(BlockPos pos) {
      if (pos == null || Arc.BREAK == null) {
         return -1;
      }

      BreakManager.BreakData data = Arc.BREAK.getBreakData(pos);
      if (data == null) {
         return -1;
      }

      if (data.complete || mc.world == null || mc.world.isAir(pos)) {
         return 0;
      }

      double left = Math.max(data.breakTime, 50.0) - (double)data.timer.getMs();
      return (int)Math.ceil(Math.max(left, 0.0) / 50.0);
   }

   /** 这一格还有几 tick 变成空气；没人挖返回 -1 */
   public static int etaTicks(BlockPos pos) {
      int self = selfEtaTicks(pos);
      int other = otherEtaTicks(pos);
      if (self < 0) {
         return other;
      }

      return other < 0 ? self : Math.min(self, other);
   }

   /**
    * 这一格在 {@code ticks} 个 tick 内会变成空气吗。
    *
    * <p>锚和水晶用它代替「等方块真的没掉」：包挖/对方挖到只剩一两 tick 时，
    * 现在就把这一格当作空气来塞，落子正好和挖穿同一 tick 到服务器。</p>
    */
   public static boolean willBeAir(BlockPos pos, int ticks) {
      int eta = etaTicks(pos);
      return eta >= 0 && eta <= ticks;
   }

   /**
    * 让包挖去挖掉这一格（给锚/水晶清位置）。
    *
    * <p>只在包挖空闲、或者它本来就在挖这一格时才动手：抢掉一个正在挖的目标会让进度从头开始，
    * 反而更慢。{@link AutoMine} 开着时完全不碰 —— 那边每 tick 都在自己挑格，这里插手只会互相打断。</p>
    *
    * @return 这一格现在是不是已经进入包挖（含本来就挖着的情况）
    */
   public static boolean request(BlockPos pos) {
      if (pos == null || mc.world == null || mc.player == null) {
         return false;
      }

      if (!PacketMine.INSTANCE.isOn() || PacketMine.INSTANCE.isOff()) {
         return false;
      }

      if (AutoMine.INSTANCE != null && AutoMine.INSTANCE.isOn()) {
         return false;
      }

      if (selfMining(pos)) {
         return true;
      }

      // 光环正在作业的格子不抢（锚模块开着时一律不碰重生锚，避免抢掉自己刚塞的那一颗）
      if (AutoAnchor.INSTANCE != null && AutoAnchor.INSTANCE.isOn()
            && BlockUtil.getBlock(pos) == net.minecraft.block.Blocks.RESPAWN_ANCHOR) {
         return false;
      }

      if (mc.world.isAir(pos) || BlockUtil.getClickSideStrict(pos) == null) {
         return false;
      }

      BlockPos breaking = PacketMine.getBreakPos();
      if (breaking != null && !breaking.equals(pos) && !mc.world.isAir(breaking)) {
         return false;
      }

      PacketMine.INSTANCE.mine(pos);
      return selfMining(pos);
   }
}
