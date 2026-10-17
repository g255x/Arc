package dev.kissoo.mod.modules.impl.movement;

import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.MoveEvent;
import dev.kissoo.api.events.impl.PacketEvent;
import dev.kissoo.api.events.impl.TimerEvent;
import dev.kissoo.api.events.impl.TickEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.player.MovementUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.combat.SelfTrap;

import dev.kissoo.api.utils.path.BaritoneUtil;
import dev.kissoo.api.utils.player.EntityUtil;
import dev.kissoo.api.utils.world.BlockUtil;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;

import dev.kissoo.Forever;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.*;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HoleSnap extends Module {
   public static HoleSnap INSTANCE;
   public final EnumSetting<SpeedMode> speedMode = this.add(new EnumSetting<>("SpeedMode", SpeedMode.Timer));
   public final SliderSetting timerSpeed = this.add(new SliderSetting("TimerSpeed", 1.0, 0.1, 8.0, 0.1,
           () -> speedMode.getValue() == SpeedMode.Timer));
   public final SliderSetting physicSpeed = this.add(new SliderSetting("PhysicSpeed", 0.2, 0.01, 1.0, 0.01,
           () -> speedMode.getValue() == SpeedMode.Physic));
   public final BooleanSetting up = this.add(new BooleanSetting("Up", true));
   private final SliderSetting range = this.add(new SliderSetting("Range", 5, 1, 50));
   private final SliderSetting timeoutTicks = this.add(new SliderSetting("TimeOut", 40, 0, 100));
   public final BooleanSetting stepEnable = this.add(new BooleanSetting("Step", false));
   private final EnumSetting<StepMode> stepMode = this.add(new EnumSetting<>("StepMode", StepMode.Vanilla,
           this.stepEnable::getValue));
   private final SliderSetting stepHeight = this.add(new SliderSetting("StepHeight", 1.0, 0.0, 5.0, 0.5,
           this.stepEnable::getValue));
   private final BooleanSetting render = this.add(new BooleanSetting("Render", true));
   private final SliderSetting renderHeight = this.add(new SliderSetting("RenderHeight", 0.05, 0.01, 0.3, 0.01,
           this.render::getValue));
   boolean resetMove = false;
   boolean applyTimer = false;
   Vec3d targetPos;
   private BlockPos holePos;
   private int holeScore;
   private int stuckTicks;
   private int enabledTicks;
   boolean stepTimer;
   int stepPackets = 0;
   private int physicIndex = 0;
   private float animAngle = 0f;
   private float pulsePhase = 0f;
   private final boolean stepPathingPause = true;
   private final boolean stepSneakingPause = true;
   private final boolean stepInBlockPause = true;
   private final boolean stepInWebPause = true;
   private final boolean stepOnlyMoving = true;
   private final boolean stepSurroundPause = true;

   public HoleSnap() {
      super("HoleSnap", "HoleSnap", Module.Category.Movement);
      this.setChinese("拉坑");
      INSTANCE = this;
   }
   private boolean isPureBedrock(BlockPos pos) {
      if (mc.world == null) return false;
      if (mc.world.getBlockState(pos.down()).getBlock() != Blocks.BEDROCK) return false;

      for (Direction dir : Direction.Type.HORIZONTAL) {
         Block block = mc.world.getBlockState(pos.offset(dir)).getBlock();
         if (block != Blocks.BEDROCK && block != Blocks.AIR) {
            return false;
         }
      }
      return true;
   }

   private int scoreHole(BlockPos pos, boolean isDouble) {
      boolean isBedrock = isPureBedrock(pos);

      if (isBedrock) {
         return isDouble ? 80 : 100;
      } else {
         return isDouble ? 40 : 60;
      }
   }
   private BlockPos findBestHole() {
      if (mc.player == null || mc.world == null) return null;

      Vec3d playerPos = mc.player.getPos();
      float searchRange = (float) this.range.getValue();
      boolean allowUp = this.up.getValue();

      List<BlockPos> candidates = new ArrayList<>();
      for (BlockPos check : BlockUtil.getSphere(searchRange, playerPos)) {
         if (!allowUp && check.getY() > mc.player.getY()) continue;
         if (Math.abs(check.getY() - mc.player.getY()) > 3) continue;
         boolean isSingle = Forever.HOLE.isHole(check, true, true, false);
         boolean isDouble = !isSingle && Forever.HOLE.isDoubleHole(check);

         if (isSingle || isDouble) {
            candidates.add(check);
         }
      }

      if (candidates.isEmpty()) return null;
      candidates.sort(Comparator
              .<BlockPos>comparingInt(p -> -scoreHole(p, Forever.HOLE.isDoubleHole(p))) // 负号表降序
              .thenComparingDouble(p ->
                      playerPos.squaredDistanceTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5))
      );

      BlockPos best = candidates.get(0);
      this.holeScore = scoreHole(best, Forever.HOLE.isDoubleHole(best));
      return best;
   }

   private Vec3d computeTargetPos() {
      if (this.holePos == null) return null;
      Vec3d target = new Vec3d(
              this.holePos.getX() + 0.5,
              mc.player.getY(),
              this.holePos.getZ() + 0.5
      );
      if (Forever.HOLE.isDoubleHole(this.holePos)) {
         Direction facing = Forever.HOLE.is3Block(this.holePos);
         if (facing != null) {
            target = target.add(
                    facing.getVector().getX() * 0.5,
                    facing.getVector().getY() * 0.5,
                    facing.getVector().getZ() * 0.5
            );
         }
      }
      return target;
   }

   public static Vec2f getRotationTo(Vec3d posFrom, Vec3d posTo) {
      Vec3d vec3d = posTo.subtract(posFrom);
      return getRotationFromVec(vec3d);
   }

   private static Vec2f getRotationFromVec(Vec3d vec) {
      double xz = Math.hypot(vec.x, vec.z);
      double yaw = normalizeAngle(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90.0);
      double pitch = normalizeAngle(Math.toDegrees(-Math.atan2(vec.y, xz)));
      return new Vec2f((float) yaw, (float) pitch);
   }

   private static double normalizeAngle(double angleIn) {
      double angle = angleIn % 360.0;
      if (angle >= 180.0) angle -= 360.0;
      if (angle < -180.0) angle += 360.0;
      return angle;
   }

   @Override
   public void onEnable() {
      this.applyTimer = false;
      this.physicIndex = 0;
      this.animAngle = 0f;
      this.pulsePhase = 0f;
      if (nullCheck()) {
         this.disable();
      } else {
         this.resetMove = false;
         this.holePos = findBestHole();
      }
   }

   @Override
   public void onDisable() {
      this.holePos = null;
      this.stuckTicks = 0;
      this.enabledTicks = 0;
      this.physicIndex = 0;
      if (!nullCheck()) {
         if (this.resetMove) {
            MovementUtil.setMotionX(0.0);
            MovementUtil.setMotionZ(0.0);
         }
      }
      if (this.stepEnable.getValue() && !nullCheck()) {
         setStepHeight(0.6F);
         Forever.TIMER.reset();
      }
   }

   @EventListener(priority = -99)
   public void onTimer(TimerEvent event) {
      if (this.applyTimer && this.speedMode.getValue() == SpeedMode.Timer) {
         event.set(this.timerSpeed.getValueFloat());
      }
   }

   @EventListener
   public void onReceivePacket(PacketEvent.Receive event) {
      if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
         if (this.speedMode.getValue() == SpeedMode.Physic) {
            if (physicIndex % 2 == 1 && mc.player != null && mc.player.isOnGround()
                    && (!MovementUtil.isJumping() || mc.player.verticalCollision)) {
               physicIndex++;
            }
         }
         this.disable();
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      updateStepLogic();
      this.holePos = findBestHole();
      if (this.holePos == null) {
         this.disable();
         return;
      }

      this.enabledTicks++;
      if (this.enabledTicks > this.timeoutTicks.getValue() - 1.0) {
         this.disable();
         return;
      }

      this.applyTimer = true;
      this.targetPos = computeTargetPos();

      if (this.speedMode.getValue() == SpeedMode.Physic) {
         handlePhysicPackets();
      }
   }

   private void updateStepLogic() {
      if (!this.stepEnable.getValue() || nullCheck()) return;

      boolean pause = false;
      if (stepPathingPause && BaritoneUtil.isActive()) pause = true;
      if (stepSneakingPause && mc.player.isSneaking()) pause = true;
      if (stepInBlockPause && EntityUtil.isInsideBlock()) pause = true;
      if (mc.player.isInLava() || mc.player.isTouchingWater()) pause = true;
      if (stepInWebPause && Forever.PLAYER.isInWeb(mc.player)) pause = true;
      if (!mc.player.isOnGround()) pause = true;
      if (stepOnlyMoving && !MovementUtil.isMoving()) pause = true;
      Module surround = Forever.MODULE != null ? Forever.MODULE.getModuleByName("Surround") : null;
      if (stepSurroundPause && ((surround != null && surround.isOn()) || SelfTrap.INSTANCE.isOn())) pause = true;

      if (!pause) {
         setStepHeight(this.stepHeight.getValueFloat());
      } else {
         setStepHeight(0.6F);
      }
   }

   private void handlePhysicPackets() {
      if (mc.player == null || mc.world == null || this.targetPos == null) return;
      if (physicIndex % 2 == 0 || mc.player.isOnGround()) {
         mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(
                 mc.player,
                 ClientCommandC2SPacket.Mode.START_FALL_FLYING
         ));
      }
      physicIndex++;
      if (!mc.player.isOnGround()) physicIndex = 1;
   }

   @EventListener
   public void onStep(TickEvent event) {
      if (!this.stepEnable.getValue()) return;
      if (event.isPost()) {
         this.stepPackets--;
      } else {
         if (this.stepTimer && this.stepPackets <= 0) {
            Forever.TIMER.reset();
            this.stepTimer = false;
         }
         boolean strict = this.stepMode.getValue() == StepMode.NCP;
         if (this.stepMode.getValue() == StepMode.OldNCP || strict) {
            double sh = mc.player.getY() - mc.player.prevY;
            if (sh <= 0.75 || sh > this.stepHeight.getValue()) return;
            double[] offsets = this.getStepOffset(sh);
            if (offsets != null && offsets.length > 1) {
               Forever.TIMER.set((float) this.getStepTimer(sh));
               this.stepTimer = true;
               this.stepPackets = 2;
               for (double offset : offsets) {
                  mc.getNetworkHandler().sendPacket(
                          new PositionAndOnGround(mc.player.prevX, mc.player.prevY + offset, mc.player.prevZ, false));
               }
            }
         }
      }
   }

   public double getStepTimer(double height) {
      if (height > 0.6 && height <= 1.0) {
         return this.stepMode.getValue() == StepMode.NCP ? 0.5 : 0.3333333333333333;
      }
      double[] offsets = this.getStepOffset(height);
      return offsets == null ? 1.0 : 1.0 / offsets.length;
   }

   public double[] getStepOffset(double height) {
      boolean strict = this.stepMode.getValue() == StepMode.NCP;
      if (height == 0.75)   return strict ? new double[]{0.42, 0.753, 0.75}  : new double[]{0.42, 0.753};
      if (height == 0.8125) return strict ? new double[]{0.39, 0.7, 0.8125}  : new double[]{0.39, 0.7};
      if (height == 0.875)  return strict ? new double[]{0.39, 0.7, 0.875}   : new double[]{0.39, 0.7};
      if (height == 1.0)    return strict ? new double[]{0.42, 0.753, 1.0}   : new double[]{0.42, 0.753};
      if (height == 1.5)    return new double[]{0.42, 0.75, 1.0, 1.16, 1.23, 1.2};
      if (height == 2.0)    return new double[]{0.42, 0.78, 0.63, 0.51, 0.9, 1.21, 1.45, 1.43};
      if (height == 2.5)    return new double[]{0.425, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652, 1.869, 2.019, 1.907};
      return null;
   }

   @EventListener
   public void onMove(MoveEvent event) {
      if (!mc.player.isAlive() || mc.player.isFallFlying()) { this.disable(); return; }
      if (this.stuckTicks > 8)   { this.disable(); return; }
      if (this.holePos == null)   { this.disable(); return; }

      Vec3d playerPos = mc.player.getPos();
      this.targetPos = computeTargetPos();
      if (this.targetPos == null) { this.disable(); return; }

      this.applyTimer = true;
      this.resetMove = true;

      float rotation = getRotationTo(playerPos, this.targetPos).x;
      float yawRad = rotation / 180.0F * (float) Math.PI;
      double dist = Math.hypot(this.targetPos.x - playerPos.x, this.targetPos.z - playerPos.z);

      if (this.speedMode.getValue() == SpeedMode.Physic) {
         double speed = Math.min(this.physicSpeed.getValue(), dist);
         double x = -(float) Math.sin(yawRad) * speed;
         double z =  (float) Math.cos(yawRad) * speed;
         mc.player.setVelocity(x, mc.player.getVelocity().y, z);
         event.setX(x);
         event.setZ(z);
         if (dist < 0.1 && playerPos.y <= this.holePos.getY() + 0.5) { this.disable(); return; }
      } else {
         double cappedSpeed = Math.min(0.2873, dist);
         double x = -(float) Math.sin(yawRad) * cappedSpeed;
         double z =  (float) Math.cos(yawRad) * cappedSpeed;
         event.setX(x);
         event.setZ(z);
         if (Math.abs(x) < 0.1 && Math.abs(z) < 0.1 && playerPos.y <= this.holePos.getY() + 0.5) {
            this.disable(); return;
         }
      }

      if (mc.player.horizontalCollision) this.stuckTicks++;
      else this.stuckTicks = 0;
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (!this.render.getValue()) return;
      if (this.targetPos == null || this.holePos == null) return;
      animAngle = (animAngle + 2.0f) % 360.0f;
      pulsePhase = (pulsePhase + 0.05f) % ((float) Math.PI * 2);
      float[] rgb = getColorByScore(this.holeScore);
      float baseAlpha = 0.55f + 0.25f * (float) Math.sin(pulsePhase); // 0.3~0.8呼吸

      Vec3d camPos = mc.getBlockEntityRenderDispatcher().camera.getPos();
      double renderY = this.holePos.getY() + (double) this.renderHeight.getValue();
      double cx = this.targetPos.x - camPos.x;
      double cy = renderY - camPos.y;
      double cz = this.targetPos.z - camPos.z;

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableDepthTest();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);

      matrixStack.push();
      drawRing(matrixStack, cx, cy, cz, 0.48, 0.50, 72, animAngle, rgb[0], rgb[1], rgb[2], baseAlpha * 0.9f);
      drawRing(matrixStack, cx, cy, cz, 0.33, 0.35, 60, -animAngle * 1.4f, rgb[0], rgb[1], rgb[2], baseAlpha * 0.7f);
      drawFilledCircle(matrixStack, cx, cy, cz, 0.30, 48, rgb[0], rgb[1], rgb[2], baseAlpha * 0.18f);
      drawTickMarks(matrixStack, cx, cy, cz, 0.50, 0.58, 8, animAngle, rgb[0], rgb[1], rgb[2], baseAlpha);

      matrixStack.pop();

      RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
   }

   private void drawRing(MatrixStack matrixStack, double cx, double cy, double cz,
                         double innerR, double outerR, int segments, float startAngle,
                         float r, float g, float b, float a) {
      Matrix4f matrix = matrixStack.peek().getPositionMatrix();
      BufferBuilder buf = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

      for (int i = 0; i <= segments; i++) {
         double angle = Math.toRadians(startAngle + (360.0 * i / segments));
         double sinA = Math.sin(angle);
         double cosA = Math.cos(angle);

         buf.vertex(matrix, (float)(cx + cosA * outerR), (float) cy, (float)(cz + sinA * outerR)).color(r, g, b, a);
         buf.vertex(matrix, (float)(cx + cosA * innerR), (float) cy, (float)(cz + sinA * innerR)).color(r, g, b, a * 0.5f);
      }

      BufferRenderer.drawWithGlobalProgram(buf.end());
   }

   private void drawFilledCircle(MatrixStack matrixStack, double cx, double cy, double cz,
                                 double radius, int segments, float r, float g, float b, float a) {
      Matrix4f matrix = matrixStack.peek().getPositionMatrix();
      BufferBuilder buf = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

      buf.vertex(matrix, (float) cx, (float) cy, (float) cz).color(r, g, b, a);

      for (int i = 0; i <= segments; i++) {
         double angle = Math.toRadians(360.0 * i / segments);
         buf.vertex(matrix,
                         (float)(cx + Math.cos(angle) * radius),
                         (float) cy,
                         (float)(cz + Math.sin(angle) * radius))
                 .color(r, g, b, 0f);
      }

      BufferRenderer.drawWithGlobalProgram(buf.end());
   }

   private void drawTickMarks(MatrixStack matrixStack, double cx, double cy, double cz,
                              double fromR, double toR, int count, float startAngle,
                              float r, float g, float b, float a) {
      Matrix4f matrix = matrixStack.peek().getPositionMatrix();
      BufferBuilder buf = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

      for (int i = 0; i < count; i++) {
         double angle = Math.toRadians(startAngle + (360.0 * i / count));
         double sinA = Math.sin(angle);
         double cosA = Math.cos(angle);

         buf.vertex(matrix, (float)(cx + cosA * fromR), (float) cy, (float)(cz + sinA * fromR)).color(r, g, b, a);
         buf.vertex(matrix, (float)(cx + cosA * toR), (float) cy, (float)(cz + sinA * toR)).color(r, g, b, 0f);
      }

      BufferRenderer.drawWithGlobalProgram(buf.end());
   }

   private float[] getColorByScore(int score) {
      return switch (score) {
         case 100 -> new float[]{0f, 1f,    0.78f};
         case 80  -> new float[]{0f, 0.78f, 1f};
         case 60  -> new float[]{0f, 0.86f, 0.31f};
         default  -> new float[]{1f, 0.47f, 0f};
      };
   }

   private static void setStepHeight(float v) {
      if (mc.player != null) {
         mc.player.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT).setBaseValue(v);
      }
   }

   public enum SpeedMode {
      Timer, Physic
   }

   public enum StepMode {
      Vanilla, OldNCP, NCP
   }
}