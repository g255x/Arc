package dev.g255.arc.api.utils.combat;

import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.mod.modules.impl.client.AntiCheat;
import dev.g255.arc.mod.modules.settings.enums.SwingSide;

/**
 * 战斗动作的「每 tick 发包预算」。
 *
 * <p>水晶光环被踢出服务器的原因几乎都是同一类：模块内部有好几条完全独立的发包路径
 * （{@code onTick → doInteract → doBreak/doPlace}、水晶生成包触发的 {@code onAdd 打断}、
 * 线程里的 {@code calc} 命中即 {@code doBreak}、以及基座放置里顺手打水晶），
 * 这些路径各自有自己的 {@code Timer} 节流，但<b>互不相干</b>。用户把
 * {@code PlaceDelay / BreakDelay} 调到 30ms、{@code Delay} 调到 0 之后，
 * 三条路径叠在一起轻松就到 60~100 个动作包每秒，而服务端只看到「这个客户端一直在打/一直在放」。</p>
 *
 * <p>这里给它们一个共享的预算：以 50ms 为一个桶（≈1 tick），
 * 桶内的动作按<b>权重</b>累加，超过上限的动作直接丢弃（不是排队），
 * 于是最坏情况的发包率被硬性钳死在 {@code limit / 50ms} 以内。</p>
 *
 * <p>权重按真实发包数算：一次攻击 = {@code InteractEntity} + 1~2 个 {@code HandSwing}；
 * 一次放置 = 1 个 {@code PlayerInteractBlock}；一次换手 = 1 个 {@code UpdateSelectedSlot}。</p>
 */
public final class PacketLimiter implements Wrapper {
   /** 每 tick（50ms 桶）允许的动作权重总和 */
   private static volatile int limit = 5;
   private static volatile boolean enabled = true;
   private static volatile long bucket = -1L;
   /** 当前桶已用掉的权重 */
   private static int used;
   /** 上一次因为超预算被丢掉的动作数，供调试/提示 */
   private static int lastDenied;

   // ---------------------------------------------------------------
   // 全局交互预算：和上面的「光环自己的预算」不是一回事。
   //
   // 上面那个只给 AutoCrystal 用（limit 可以调到 40）。真正让人被踢的是
   // 所有模块叠在一起的总量：自围 2 块 + 围脚 1 块 + 锚光环「塞+充能」2 下 +
   // 困住 1 块 + 水晶 2 个 …… 同一 tick 里十几到二十几个交互包，
   // 每个交互还带上一次「瞬转朝向」和一个摆手包，服务端看到的就是
   // 每秒几百包的客户端。
   //
   // 这里给所有「放置 / 点击方块」一个共享配额：一 tick 最多几个交互，
   // 超了就这一发直接放弃（调用方下一 tick 自然重试），
   // 于是最坏情况被硬钳死在 {@code actionLimit * 20} 个交互/秒。
   // ---------------------------------------------------------------
   private static volatile int actionLimit = 3;
   private static volatile boolean actionEnabled = true;
   /** 最近若干次交互的时间戳（环形），用滑动窗口而不是「50ms 一个桶」 */
   private static final long[] actionTimes = new long[16];
   private static int actionCursor;
   private static int actionDenied;
   private static final long WINDOW_MS = 50L;

   static {
      // 初始值给一个很久以前的时间，避免刚启动时被当成「有历史」而误拒
      java.util.Arrays.fill(actionTimes, -1000000L);
   }

   private PacketLimiter() {
   }

   /** 全局交互配额（每 50ms 桶） */
   public static void setActionLimit(int value) {
      actionLimit = Math.max(1, Math.min(12, value));
   }

   public static int getActionLimit() {
      return actionLimit;
   }

   public static int getActionDenied() {
      return actionDenied;
   }

   public static void setActionEnabled(boolean value) {
      actionEnabled = value;
   }

   public static boolean isActionEnabled() {
      return actionEnabled;
   }

   /**
    * 申请一次「放置 / 点击方块」的全局额度。
    *
    * <p>和 {@link #acquire(int)} 分开记账：那个是光环内部的权重预算，
    * 这个是跨模块的硬闸门，即使光环的 limiter 关掉，这里也照样生效。</p>
    *
    * @return false = 本 tick 的全局交互额度用完了，这一发不要发
    */
   public static boolean acquireAction() {
      if (AntiCheat.INSTANCE != null) {
         actionLimit = AntiCheat.INSTANCE.actionLimit.getValueInt();
         actionEnabled = AntiCheat.INSTANCE.limitActions.getValue();
      }

      if (!actionEnabled) {
         return true;
      }

      long now = System.currentTimeMillis();
      synchronized (PacketLimiter.class) {
         // 窗口里「最老的那一发」还不到 50ms，就说明这 50ms 内已经发满 actionLimit 个了。
         // 用滑动窗口而不是固定桶：否则 49ms 发 3 个、51ms 再发 3 个，
         // 服务端看到的就是「2ms 内 6 个交互包」，一样会被判超速。
         int oldestSlot = (actionCursor + actionTimes.length - actionLimit) % actionTimes.length;
         if (now - actionTimes[oldestSlot] < WINDOW_MS) {
            ++actionDenied;
            return false;
         }

         actionTimes[actionCursor] = now;
         actionCursor = (actionCursor + 1) % actionTimes.length;
         return true;
      }
   }

   /** 最近 50ms 内已经发出去的交互数 */
   public static int actionUsedThisTick() {
      long now = System.currentTimeMillis();
      synchronized (PacketLimiter.class) {
         int used = 0;

         for(int i = 0; i < actionTimes.length; ++i) {
            if (now - actionTimes[i] < WINDOW_MS) {
               ++used;
            }
         }

         return used;
      }
   }

   /** 现在还能再发几个交互 */
   public static int actionLeftThisTick() {
      return Math.max(0, actionLimit - actionUsedThisTick());
   }

   public static void setEnabled(boolean value) {
      enabled = value;
   }

   public static boolean isEnabled() {
      return enabled;
   }

   public static void setLimit(int value) {
      limit = Math.max(1, Math.min(40, value));
   }

   public static int getLimit() {
      return limit;
   }

   public static int getLastDenied() {
      return lastDenied;
   }

   /** 攻击包的真实开销：攻击本身 + 摆动包 */
   public static int attackCost(SwingSide side) {
      if (side == null) {
         return 2;
      }
      return switch (side) {
         case All -> 3;
         case Server, Client -> 2;
         case None -> 1;
      };
   }

   /**
    * 申请一次发包额度。
    *
    * @param cost 这次动作会发几个包（至少 1）
    * @return true = 可以发；false = 这一 tick 的额度用完了，调用方直接放弃这次动作
    */
   public static boolean acquire(int cost) {
      if (!enabled) {
         return true;
      }
      int need = Math.max(1, cost);
      long now = System.currentTimeMillis() / 50L;
      synchronized (PacketLimiter.class) {
         if (now != bucket) {
            bucket = now;
            used = 0;
            lastDenied = 0;
         }
         if (used + need > limit) {
            ++lastDenied;
            return false;
         }
         used += need;
         return true;
      }
   }

   public static boolean acquire() {
      return acquire(1);
   }

   /** 换模块 / 回大厅时清空，避免上一段窗口残留 */
   public static void reset() {
      synchronized (PacketLimiter.class) {
         bucket = -1L;
         used = 0;
         lastDenied = 0;
         java.util.Arrays.fill(actionTimes, -1000000L);
         actionCursor = 0;
         actionDenied = 0;
      }
   }
}
