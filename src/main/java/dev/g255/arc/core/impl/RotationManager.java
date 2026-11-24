package dev.g255.arc.core.impl;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.DoAttackEvent;
import dev.g255.arc.api.events.impl.FireworkShooterRotationEvent;
import dev.g255.arc.api.events.impl.InteractBlockEvent;
import dev.g255.arc.api.events.impl.InteractItemEvent;
import dev.g255.arc.api.events.impl.JumpEvent;
import dev.g255.arc.api.events.impl.KeyboardInputEvent;
import dev.g255.arc.api.events.impl.PacketEvent;
import dev.g255.arc.api.events.impl.RotationEvent;
import dev.g255.arc.api.events.impl.SendMovementPacketsEvent;
import dev.g255.arc.api.events.impl.TickEvent;
import dev.g255.arc.api.events.impl.TickMovementEvent;
import dev.g255.arc.api.events.impl.TravelEvent;
import dev.g255.arc.api.events.impl.UpdateRotateEvent;
import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.api.utils.math.MathUtil;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.path.BaritoneUtil;
import dev.g255.arc.asm.accessors.IClientPlayerEntity;
import dev.g255.arc.mod.modules.impl.client.AntiCheat;
import dev.g255.arc.mod.modules.impl.client.ClientSetting;
import dev.g255.arc.mod.modules.impl.movement.HoleSnap;
import dev.g255.arc.mod.modules.impl.player.Freecam;
import dev.g255.arc.mod.modules.settings.enums.SnapBack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;

public class RotationManager implements Wrapper {
   public static final Timer ROTATE_TIMER = new Timer();
   public static Vec3d directionVec = null;
   public static boolean snapBack = false;
   private static float renderPitch;
   private static float renderYawOffset;
   private static float prevRenderPitch;
   private static float prevRenderYawOffset;
   private static float prevRotationYawHead;
   private static float rotationYawHead;
   public float nextYaw;
   public float nextPitch;
   public float rotationYaw;
   public float rotationPitch;
   public float lastYaw;
   public float lastPitch;
   public Vec3d crossHairUpdatePos;
   private int ticksExisted;
   public static float fixYaw;
   public static float fixPitch;
   private float prevYaw;
   private float prevPitch;
   /**
    * 旋转独占锁：优先级 / 到期 tick。
    *
    * <p>珍珠穿墙这种「这一 tick 的朝向错一点就整发珍珠飞歪」的动作，光靠
    * {@code RotationEvent} 的 priority 不够 —— 别的模块走的是 {@link #lookAt}/{@link #snapAt}
    * 这条即时通道，根本不看 priority，照样能把朝向拽走。锁期间，任何
    * priority 低于锁的即时转向请求全部丢弃。</p>
    */
   private static float lockPriority = 0.0F;
   private static long lockUntilMs = 0L;

   /**
    * 朝向包配额。
    *
    * <p>「瞬转」这条路每调用一次就发一个 {@code LookAndOnGround}。自围一圈 4 块、
    * 锚光环「塞 + 充能」、水晶摆放……全挤在同一 tick 里就是十几个朝向包，
    * 服务端看到的是「这个人的头在一 tick 内甩了十几下」，比交互包本身更容易被盯上。</p>
    *
    * <p>所以这里做两件事：</p>
    * <ul>
    *   <li><b>去重</b>：本 tick 已经发过同一个角度（差 &lt; 0.05°）就不再发 ——
    *       服务端看到的朝向本来就是那个方向，多发一个是纯浪费；</li>
    *   <li><b>配额</b>：每 tick 最多发几个，超了就让调用方<b>放弃这次动作</b>
    *       （不是让动作带着旧朝向发出去 —— 那样服务端会判「朝向对不上」）。</li>
    * </ul>
    */
   private static final long[] rotationTimes = new long[16];
   private static int rotationCursor;
   private static long lastRotationMs = -1000000L;
   private static float sentYaw = Float.NaN;
   private static float sentPitch = Float.NaN;

   static {
      java.util.Arrays.fill(rotationTimes, -1000000L);
   }

   public RotationManager() {
      Arc.EVENT_BUS.subscribe(this);
   }

   @EventListener
   public void onInteract(InteractItemEvent event) {
      if (AntiCheat.INSTANCE.interactRotation.getValue() && mc.player != null) {
         if (event.isPre()) {
            this.snapAt(mc.player.getYaw(), mc.player.getPitch());
         } else {
            this.snapBack();
         }
      }

   }

   @EventListener
   public void onInteract(InteractBlockEvent event) {
      if (AntiCheat.INSTANCE.interactRotation.getValue() && mc.player != null) {
         if (event.isPre()) {
            this.snapAt(mc.player.getYaw(), mc.player.getPitch());
         } else {
            this.snapBack();
         }
      }

   }

   @EventListener
   public void doAttack(DoAttackEvent event) {
      if (AntiCheat.INSTANCE.interactRotation.getValue() && mc.player != null) {
         if (event.isPre()) {
            this.snapAt(mc.player.getYaw(), mc.player.getPitch());
         } else {
            this.snapBack();
         }
      }

   }

   public void snapBack() {
      if (AntiCheat.INSTANCE.snapBackEnum.is(SnapBack.Force)) {
         mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), this.rotationYaw, this.rotationPitch, mc.player.isOnGround()));
         // 服务端朝向已经恢复，去重缓存必须一起失效：否则 50ms 内的下一次 aim 会被判定成
         // 「刚发过同一个角度、服务端还朝着那里」而不再补朝向包，交互包带着恢复后的旧朝向出去、
         // 直接被服务端顶回来（塞了锚却不炸、连炸断断续续都是这个）。
         sentYaw = Float.NaN;
         sentPitch = Float.NaN;
      } else if (AntiCheat.INSTANCE.snapBackEnum.is(SnapBack.Tick)) {
         snapBack = true;
         sentYaw = Float.NaN;
         sentPitch = Float.NaN;
      }

   }

   public void lookAt(Vec3d directionVec) {
      if (isLocked(0.0F)) {
         return;
      }

      this.rotationTo(directionVec);
      this.snapAt(directionVec);
   }

   public void lookAt(BlockPos pos, Direction side) {
      Vec3d hitVec = pos.toCenterPos().add(new Vec3d((double)side.getVector().getX() * (double)0.5F, (double)side.getVector().getY() * (double)0.5F, (double)side.getVector().getZ() * (double)0.5F));
      this.lookAt(hitVec);
   }

   /**
    * 申请独占朝向若干 tick。只有优先级不低于当前锁才生效，
    * 所以珍珠穿墙（100）能压住锚光环 / 陷阱（10 左右）。
    *
    * <p>用毫秒记而不是玩家 age：过图 / 重生会把 age 归零，
    * 那时用 age 比较会让锁一直「未来到期」，转向就再也恢复不了了。</p>
    */
   public static void lockRotation(float priority, int ticks) {
      long now = System.currentTimeMillis();
      if (priority < lockPriority && now <= lockUntilMs) {
         return;
      }

      lockPriority = priority;
      lockUntilMs = now + (long)Math.max(1, ticks) * 50L;
   }

   /** 当前是否被更高优先级的动作锁住（precision：请求方自己的优先级） */
   public static boolean isLocked(float priority) {
      return priority < lockPriority && System.currentTimeMillis() <= lockUntilMs;
   }

   public static void clearRotationLock() {
      lockUntilMs = 0L;
      lockPriority = 0.0F;
   }

   public void snapAt(float yaw, float pitch) {
      this.snapAt(yaw, pitch, 0.0F);
   }

   public void snapAt(float yaw, float pitch, float priority) {
      // 被更高优先级的动作锁着：这一发朝向不能动，否则珍珠/关键发包直接飞歪
      if (isLocked(priority)) {
         return;
      }

      // NaN 朝向发进服务端会被踢/回弹，直接放弃这次转向
      if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) {
         return;
      }

      if (!this.reserveRotation(yaw, pitch, priority)) {
         return;
      }

      this.setRenderRotation(yaw, pitch, true);
   }

   /**
    * 让服务端看到的朝向对准 {@code vec}，并告诉调用方「现在能不能基于这个朝向发交互包」。
    *
    * <p>放置 / 点击方块之前必须先过这一关：返回 false 就<b>别发包</b>。
    * 原来无论朝向配额有没有都会照发，结果是要么朝向对不上被顶回来（废包 + 被记录），
    * 要么一 tick 甩十几次头。</p>
    */
   public boolean aim(Vec3d vec, float priority) {
      if (mc.player == null || vec == null) {
         return false;
      }

      if (isLocked(priority)) {
         return false;
      }

      float[] angle = getRotation(vec);
      return this.aimAt(angle[0], angle[1], priority);
   }

   /** 同 {@link #aim(Vec3d, float)}，直接给角度 */
   public boolean aimAt(float yaw, float pitch, float priority) {
      if (mc.player == null) {
         return false;
      }

      if (isLocked(priority)) {
         return false;
      }

      if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) {
         return false;
      }

      if (!this.reserveRotation(yaw, pitch, priority)) {
         return false;
      }

      this.setRenderRotation(yaw, pitch, true);
      return true;
   }

   /**
    * 占一个朝向包额度，需要时把包发出去。
    *
    * @return true = 服务端现在看到的就是这个朝向（本 tick 已经发过同一个角度，或者刚发出去）
    */
   private boolean reserveRotation(float yaw, float pitch, float priority) {
      int cap = 4;
      if (AntiCheat.INSTANCE != null) {
         cap = Math.max(1, AntiCheat.INSTANCE.rotationLimit.getValueInt());
      }

      long now = System.currentTimeMillis();
      synchronized (RotationManager.class) {
         // 刚刚（50ms 内）已经把这个角度发出去过：服务端看到的就是它，不用重复发
         if (now - lastRotationMs < 50L && Float.isFinite(sentYaw)
               && Math.abs(MathHelper.wrapDegrees(yaw - sentYaw)) < 0.05F
               && Math.abs(pitch - sentPitch) < 0.05F) {
            return true;
         }

         // 滑动窗口：最近 cap 发里最老的那发还不到 50ms 就是超速了
         int oldestSlot = (rotationCursor + rotationTimes.length - cap) % rotationTimes.length;
         // 珍珠穿墙这类关键动作不受配额限制，否则会被普通放置挤掉
         if (priority < 50.0F && now - rotationTimes[oldestSlot] < 50L) {
            return false;
         }

         rotationTimes[rotationCursor] = now;
         rotationCursor = (rotationCursor + 1) % rotationTimes.length;
         lastRotationMs = now;
         sentYaw = yaw;
         sentPitch = pitch;
      }

      if (AntiCheat.INSTANCE.grimRotation.getValue()) {
         mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.isOnGround()));
      } else {
         mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, mc.player.isOnGround()));
      }

      return true;
   }

   public void snapAt(Vec3d directionVec) {
      this.snapAt(directionVec, 0.0F);
   }

   public void snapAt(Vec3d directionVec, float priority) {
      if (isLocked(priority)) {
         return;
      }

      float[] angle = getRotation(directionVec);
      this.snapAt(angle[0], angle[1], priority);
   }

   public void rotationTo(Vec3d vec3d) {
      if (isLocked(0.0F)) {
         return;
      }

      ROTATE_TIMER.reset();
      directionVec = vec3d;
   }

   public boolean inFov(Vec3d directionVec, float fov) {
      float[] angle = getRotation(this.crossHairUpdatePos != null ? this.crossHairUpdatePos : new Vec3d(mc.player.getX(), mc.player.getY() + (double)mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ()), directionVec);
      return this.inFov(angle[0], angle[1], fov);
   }

   public boolean inFov(float yaw, float pitch, float fov) {
      float yawDifferent = MathHelper.angleBetween(yaw, this.rotationYaw);
      float pitchDifferent;
      return yawDifferent * yawDifferent + (pitchDifferent = Math.abs(pitch - this.rotationPitch)) * pitchDifferent <= fov * fov;
   }

   @EventListener
   public void onTickMovement(TickMovementEvent event) {
      if (mc.player != null) {
         this.crossHairUpdatePos = new Vec3d(mc.player.getX(), mc.player.getY() + (double)mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
      }
   }

   @EventListener
   public void update(SendMovementPacketsEvent event) {
      if (AntiCheat.INSTANCE.movementSync() && !BaritoneUtil.isActive()) {
         event.setYaw(Float.isFinite(this.nextYaw) ? this.nextYaw : event.getYaw());
         event.setPitch(Float.isFinite(this.nextPitch) ? this.nextPitch : event.getPitch());
      } else {
         UpdateRotateEvent updateRotateEvent = UpdateRotateEvent.get(event.getYaw(), event.getPitch());
         Arc.EVENT_BUS.post(updateRotateEvent);
         event.setYaw(updateRotateEvent.getYaw());
         event.setPitch(updateRotateEvent.getPitch());
      }

   }

   @EventListener(
      priority = 999
   )
   public void update(TickMovementEvent event) {
      if (mc.player != null) {
         if (AntiCheat.INSTANCE.movementSync() && !BaritoneUtil.isActive()) {
            UpdateRotateEvent updateRotateEvent = UpdateRotateEvent.get(mc.player.getYaw(), mc.player.getPitch());
            Arc.EVENT_BUS.post(updateRotateEvent);
            this.nextYaw = updateRotateEvent.getYaw();
            this.nextPitch = updateRotateEvent.getPitch();
            fixYaw = this.nextYaw;
            fixPitch = this.nextPitch;
         }

      }
   }

   @EventListener(
      priority = -200
   )
   public void onLastRotation(UpdateRotateEvent event) {
      RotationEvent rotationEvent = RotationEvent.get();
      Arc.EVENT_BUS.post(rotationEvent);
      if (rotationEvent.getRotation()) {
         float[] newAngle = this.injectStep(new float[]{rotationEvent.getYaw(), rotationEvent.getPitch()}, rotationEvent.getSpeed());
         event.setYaw(newAngle[0]);
         event.setPitch(newAngle[1]);
      } else if (rotationEvent.getTarget() != null) {
         float[] newAngle = this.injectStep(rotationEvent.getTarget(), rotationEvent.getSpeed());
         event.setYaw(newAngle[0]);
         event.setPitch(newAngle[1]);
      } else if (!event.isModified() && AntiCheat.INSTANCE.look.getValue() && directionVec != null && !ROTATE_TIMER.passed((long)(AntiCheat.INSTANCE.rotateTime.getValue() * (double)1000.0F))) {
         float[] newAngle = this.injectStep(directionVec, AntiCheat.INSTANCE.steps.getValueFloat());
         event.setYaw(newAngle[0]);
         event.setPitch(newAngle[1]);
      }

   }

   @EventListener
   public void travel(TravelEvent e) {
      if (AntiCheat.INSTANCE.movementSync()) {
         if (!BaritoneUtil.isActive()) {
            if (!mc.player.isRiding()) {
               if (e.isPre()) {
                  this.prevYaw = mc.player.getYaw();
                  this.prevPitch = mc.player.getPitch();
                  mc.player.setYaw(Float.isFinite(fixYaw) ? fixYaw : this.prevYaw);
                  mc.player.setPitch(Float.isFinite(fixPitch) ? fixPitch : this.prevPitch);
               } else {
                  mc.player.setYaw(this.prevYaw);
                  mc.player.setPitch(this.prevPitch);
               }

            }
         }
      }
   }

   @EventListener
   public void onJump(JumpEvent e) {
      if (AntiCheat.INSTANCE.movementSync()) {
         if (!BaritoneUtil.isActive()) {
            if (!mc.player.isRiding()) {
               if (e.isPre()) {
                  this.prevYaw = mc.player.getYaw();
                  this.prevPitch = mc.player.getPitch();
                  mc.player.setYaw(Float.isFinite(fixYaw) ? fixYaw : this.prevYaw);
                  mc.player.setPitch(Float.isFinite(fixPitch) ? fixPitch : this.prevPitch);
               } else {
                  mc.player.setYaw(this.prevYaw);
                  mc.player.setPitch(this.prevPitch);
               }

            }
         }
      }
   }

   @EventListener
   public void onFirework(FireworkShooterRotationEvent event) {
      if (AntiCheat.INSTANCE.movementSync()) {
         if (!BaritoneUtil.isActive()) {
            if (event.shooter == mc.player) {
               event.yaw = fixYaw;
               event.pitch = fixPitch;
               event.cancel();
            }

         }
      }
   }

   @EventListener(
      priority = -999
   )
   public void onKeyInput(KeyboardInputEvent e) {
      if (AntiCheat.INSTANCE.movementSync()) {
         if (!BaritoneUtil.isActive()) {
            if (!HoleSnap.INSTANCE.isOn()) {
               if (!mc.player.isRiding() && !Freecam.INSTANCE.isOn()) {
                  float mF = mc.player.input.movementForward;
                  float mS = mc.player.input.movementSideways;
                  float delta = (mc.player.getYaw() - fixYaw) * ((float)Math.PI / 180F);
                  float cos = MathHelper.cos(delta);
                  float sin = MathHelper.sin(delta);
                  mc.player.input.movementSideways = (float)Math.round(mS * cos - mF * sin);
                  mc.player.input.movementForward = (float)Math.round(mF * cos + mS * sin);
               }
            }
         }
      }
   }

   public float[] injectStep(Vec3d vec, float steps) {
      float currentYaw = AntiCheat.INSTANCE.serverSide.getValue() ? this.getLastYaw() : this.rotationYaw;
      float currentPitch = AntiCheat.INSTANCE.serverSide.getValue() ? this.getLastPitch() : this.rotationPitch;
      float yawDelta = MathHelper.wrapDegrees((float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z - mc.player.getZ(), vec.x - mc.player.getX())) - (double)90.0F) - currentYaw);
      float pitchDelta = (float)(-Math.toDegrees(Math.atan2(vec.y - (mc.player.getPos().y + (double)mc.player.getEyeHeight(mc.player.getPose())), Math.sqrt(Math.pow(vec.x - mc.player.getX(), (double)2.0F) + Math.pow(vec.z - mc.player.getZ(), (double)2.0F))))) - currentPitch;
      if (AntiCheat.INSTANCE.random.getValue()) {
         float angleToRad = (float)Math.toRadians((double)(27 * (mc.player.age % 30)));
         yawDelta = (float)((double)yawDelta + Math.sin((double)angleToRad) * (double)3.0F) + MathUtil.random(-1.0F, 1.0F);
         pitchDelta += MathUtil.random(-0.6F, 0.6F);
      }

      if (yawDelta > 180.0F) {
         yawDelta -= 180.0F;
      }

      float yawStepVal = 180.0F * steps;
      float clampedYawDelta = MathHelper.clamp(MathHelper.abs(yawDelta), -yawStepVal, yawStepVal);
      float clampedPitchDelta = MathHelper.clamp(pitchDelta, -45.0F, 45.0F);
      float newYaw = currentYaw + (yawDelta > 0.0F ? clampedYawDelta : -clampedYawDelta);
      float newPitch = MathHelper.clamp(currentPitch + clampedPitchDelta, -90.0F, 90.0F);
      // 目标坐标是 NaN 时不要把 NaN 传出去（会跟着发包含进服务端）
      if (!Float.isFinite(newYaw) || !Float.isFinite(newPitch)) {
         float fallbackYaw = Float.isFinite(currentYaw) ? currentYaw : mc.player.getYaw();
         float fallbackPitch = Float.isFinite(currentPitch) ? currentPitch : mc.player.getPitch();
         return new float[]{fallbackYaw, fallbackPitch};
      }

      return new float[]{newYaw, newPitch};
   }

   public float[] injectStep(float[] angle, float steps) {
      float currentYaw = AntiCheat.INSTANCE.serverSide.getValue() ? this.getLastYaw() : this.rotationYaw;
      float currentPitch = AntiCheat.INSTANCE.serverSide.getValue() ? this.getLastPitch() : this.rotationPitch;
      float yawDelta = MathHelper.wrapDegrees(angle[0] - currentYaw);
      float pitchDelta = angle[1] - currentPitch;
      if (AntiCheat.INSTANCE.random.getValue()) {
         float angleToRad = (float)Math.toRadians((double)(27 * (mc.player.age % 30)));
         yawDelta = (float)((double)yawDelta + Math.sin((double)angleToRad) * (double)3.0F) + MathUtil.random(-1.0F, 1.0F);
         pitchDelta += MathUtil.random(-0.6F, 0.6F);
      }

      if (yawDelta > 180.0F) {
         yawDelta -= 180.0F;
      }

      float yawStepVal = 180.0F * steps;
      float pitchStepVal = 90.0F * steps;
      float clampedYawDelta = MathHelper.clamp(MathHelper.abs(yawDelta), -yawStepVal, yawStepVal);
      float clampedPitchDelta = MathHelper.clamp(pitchDelta, -pitchStepVal, pitchStepVal);
      float newYaw = currentYaw + (yawDelta > 0.0F ? clampedYawDelta : -clampedYawDelta);
      float newPitch = MathHelper.clamp(currentPitch + clampedPitchDelta, -90.0F, 90.0F);
      // 目标坐标是 NaN 时不要把 NaN 传出去（会跟着发包含进服务端）
      if (!Float.isFinite(newYaw) || !Float.isFinite(newPitch)) {
         float fallbackYaw = Float.isFinite(currentYaw) ? currentYaw : mc.player.getYaw();
         float fallbackPitch = Float.isFinite(currentPitch) ? currentPitch : mc.player.getPitch();
         return new float[]{fallbackYaw, fallbackPitch};
      }

      return new float[]{newYaw, newPitch};
   }

   @EventListener(
      priority = -999
   )
   public void onPacketSend(PacketEvent.Sent event) {
      if (mc.player != null) {
         Packet<?> packet2 = event.getPacket();
         PlayerMoveC2SPacket packet;
         if (packet2 instanceof PlayerMoveC2SPacket && (packet = (PlayerMoveC2SPacket)packet2).changesLook()) {
            this.setLastYaw(packet.getYaw(this.getLastYaw()));
            this.setLastPitch(packet.getPitch(this.getLastPitch()));
            this.setRenderRotation(this.getLastYaw(), this.getLastPitch(), ClientSetting.INSTANCE.sync.getValue());
         }

      }
   }

   @EventListener
   public void onUpdateWalkingPost(TickEvent event) {
      if (event.isPost()) {
         this.setRenderRotation(this.getLastYaw(), this.getLastPitch(), false);
      }

   }

   public void setRenderRotation(float yaw, float pitch, boolean force) {
      if (mc.player == null) {
         return;
      }

      // 一旦把 NaN 写进 renderYawOffset / renderPitch，就会一直是 NaN，
      // 渲染时每帧往实体上写一次，原版就刷 "Invalid entity rotation: NaN, discarding."
      if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) {
         yaw = mc.player.getYaw();
         pitch = mc.player.getPitch();
         if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            return;
         }
      }

      if (!Float.isFinite(prevRenderYawOffset)) {
         prevRenderYawOffset = yaw;
      }

      if (!Float.isFinite(prevRenderPitch)) {
         prevRenderPitch = pitch;
      }

      if (mc.player.age != this.ticksExisted || force) {
         this.ticksExisted = mc.player.age;
         prevRenderPitch = renderPitch;
         prevRenderYawOffset = renderYawOffset;
         float offset = this.getRenderYawOffset(yaw, prevRenderYawOffset);
         renderYawOffset = Float.isFinite(offset) ? offset : yaw;
         prevRotationYawHead = rotationYawHead;
         rotationYawHead = yaw;
         renderPitch = pitch;
      }
   }

   private float getRenderYawOffset(float yaw, float offsetIn) {
      float result = offsetIn;
      double xDif = mc.player.getX() - mc.player.prevX;
      double zDif;
      if (xDif * xDif + (zDif = mc.player.getZ() - mc.player.prevZ) * zDif > (double)0.0025000002F) {
         float offset = (float)MathHelper.atan2(zDif, xDif) * (180F / (float)Math.PI) - 90.0F;
         float wrap = MathHelper.abs(MathHelper.wrapDegrees(yaw) - offset);
         result = 95.0F < wrap && wrap < 265.0F ? offset - 180.0F : offset;
      }

      if (mc.player.handSwingProgress > 0.0F) {
         result = yaw;
      }

      float offset;
      if ((offset = MathHelper.wrapDegrees(yaw - (offsetIn + MathHelper.wrapDegrees(result - offsetIn) * 0.3F))) < -75.0F) {
         offset = -75.0F;
      } else if (offset >= 75.0F) {
         offset = 75.0F;
      }

      result = yaw - offset;
      if (offset * offset > 2500.0F) {
         result += offset * 0.2F;
      }

      return result;
   }

   public static float[] getRotation(Vec3d eyesPos, Vec3d vec) {
      double diffX = vec.x - eyesPos.x;
      double diffY = vec.y - eyesPos.y;
      double diffZ = vec.z - eyesPos.z;
      double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
      float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(diffY, diffXZ)));
      yaw = MathHelper.wrapDegrees(yaw);
      pitch = MathHelper.wrapDegrees(pitch);
      // 坐标是 NaN 时不把 NaN 传出去（否则会一路写进实体朝向）
      if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) {
         yaw = mc.player != null ? mc.player.getYaw() : 0.0F;
         pitch = mc.player != null ? mc.player.getPitch() : 0.0F;
      }

      return new float[]{yaw, pitch};
   }

   public static float[] getRotation(Vec3d vec) {
      Vec3d eyesPos = mc.player.getEyePos();
      return getRotation(eyesPos, vec);
   }

   public static float getRenderPitch() {
      return Float.isFinite(renderPitch) ? renderPitch : fallbackPitch();
   }

   public static float getRotationYawHead() {
      return Float.isFinite(rotationYawHead) ? rotationYawHead : fallbackYaw();
   }

   public static float getRenderYawOffset() {
      return Float.isFinite(renderYawOffset) ? renderYawOffset : fallbackYaw();
   }

   public static float getPrevRenderPitch() {
      return Float.isFinite(prevRenderPitch) ? prevRenderPitch : fallbackPitch();
   }

   public static float getPrevRotationYawHead() {
      return Float.isFinite(prevRotationYawHead) ? prevRotationYawHead : fallbackYaw();
   }

   public static float getPrevRenderYawOffset() {
      return Float.isFinite(prevRenderYawOffset) ? prevRenderYawOffset : fallbackYaw();
   }

   /** 渲染朝向一旦被污染成 NaN，渲染线程会每帧把 NaN 写进实体并刷日志，这里统一退回玩家真实朝向 */
   private static float fallbackYaw() {
      return mc.player != null && Float.isFinite(mc.player.getYaw()) ? mc.player.getYaw() : 0.0F;
   }

   private static float fallbackPitch() {
      return mc.player != null && Float.isFinite(mc.player.getPitch()) ? mc.player.getPitch() : 0.0F;
   }

   public float getLastYaw() {
      return this.lastYaw;
   }

   public void setLastYaw(float lastYaw) {
      this.lastYaw = lastYaw;
      if (AntiCheat.INSTANCE.forceSync.getValue() && Arc.SERVER.playerNull.passedS(0.15)) {
         ((IClientPlayerEntity)mc.player).setLastYaw(lastYaw);
      }

   }

   public float getLastPitch() {
      return this.lastPitch;
   }

   public void setLastPitch(float lastPitch) {
      this.lastPitch = lastPitch;
      if (AntiCheat.INSTANCE.forceSync.getValue() && Arc.SERVER.playerNull.passedS(0.15)) {
         ((IClientPlayerEntity)mc.player).setLastPitch(lastPitch);
      }

   }
}
