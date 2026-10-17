package dev.baimaodaxia.mod.modules.impl.movement;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.KeyboardInputEvent;
import dev.baimaodaxia.api.events.impl.MoveEvent;
import dev.baimaodaxia.api.events.impl.PacketEvent;
import dev.baimaodaxia.api.events.impl.RotationEvent;
import dev.baimaodaxia.api.events.impl.TimerEvent;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.player.MovementUtil;
import dev.baimaodaxia.api.utils.render.ColorUtil;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.client.AntiCheat;
import dev.baimaodaxia.mod.modules.impl.player.Freecam;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.ColorSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.Objects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormat.DrawMode;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class HoleSnap extends Module {
   public static HoleSnap INSTANCE;
   public final BooleanSetting any = this.add(new BooleanSetting("AnyHole", true));
   public final SliderSetting timer = this.add(new SliderSetting("Timer", (double)1.0F, 0.1, (double)8.0F, 0.1));
   public final BooleanSetting up = this.add(new BooleanSetting("Up", true));
   public final BooleanSetting grim = this.add(new BooleanSetting("Grim", false));
   public final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 100)));
   public final SliderSetting circleSize = this.add(new SliderSetting("CircleSize", (double)1.0F, (double)0.1F, (double)2.5F));
   public final BooleanSetting fade = this.add(new BooleanSetting("Fade", true));
   public final SliderSetting segments = this.add(new SliderSetting("Segments", 180, 0, 360));
   private final SliderSetting range = this.add(new SliderSetting("Range", 5, 1, 50));
   private final SliderSetting timeoutTicks = this.add(new SliderSetting("TimeOut", 40, 0, 100));
   private final SliderSetting steps;
   private final SliderSetting priority;
   boolean resetMove;
   boolean applyTimer;
   Vec3d targetPos;
   private BlockPos holePos;
   private int stuckTicks;
   private int enabledTicks;

   public HoleSnap() {
      super("HoleSnap", "HoleSnap", Module.Category.Movement);
      BooleanSetting var10009 = this.grim;
      Objects.requireNonNull(var10009);
      this.steps = this.add(new SliderSetting("Steps", 0.8, (double)0.0F, (double)1.0F, 0.01, var10009::getValue));
      BooleanSetting var10008 = this.grim;
      Objects.requireNonNull(var10008);
      this.priority = this.add(new SliderSetting("Priority", 10, 0, 100, var10008::getValue));
      this.resetMove = false;
      this.applyTimer = false;
      this.setChinese("拉坑");
      INSTANCE = this;
   }

   public static Vec2f getRotationTo(Vec3d posFrom, Vec3d posTo) {
      Vec3d vec3d = posTo.subtract(posFrom);
      return getRotationFromVec(vec3d);
   }

   public static void drawCircle(MatrixStack matrixStack, Color color, double circleSize, Vec3d pos, int segments) {
      Vec3d camPos = mc.getBlockEntityRenderDispatcher().camera.getPos();
      RenderSystem.disableDepthTest();
      Matrix4f matrix = matrixStack.peek().getPositionMatrix();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      float keyCodec = (float)color.getAlpha() / 255.0F;
      float r = (float)color.getRed() / 255.0F;
      float g = (float)color.getGreen() / 255.0F;
      float elementCodec = (float)color.getBlue() / 255.0F;
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

      for(double i = (double)0.0F; i < (double)360.0F; i += (double)360.0F / (double)segments) {
         double x = Math.sin(Math.toRadians(i)) * circleSize;
         double z = Math.cos(Math.toRadians(i)) * circleSize;
         Vec3d tempPos = (new Vec3d(pos.x + x, pos.y, pos.z + z)).add(-camPos.x, -camPos.y, -camPos.z);
         bufferBuilder.vertex(matrix, (float)tempPos.x, (float)tempPos.y, (float)tempPos.z).color(r, g, elementCodec, keyCodec);
      }

      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.enableDepthTest();
   }

   private static Vec2f getRotationFromVec(Vec3d vec) {
      double d = vec.x;
      double d2 = vec.z;
      double xz = Math.hypot(d, d2);
      d2 = vec.z;
      double d3 = vec.x;
      double yaw = normalizeAngle(Math.toDegrees(Math.atan2(d2, d3)) - (double)90.0F);
      double pitch = normalizeAngle(Math.toDegrees(-Math.atan2(vec.y, xz)));
      return new Vec2f((float)yaw, (float)pitch);
   }

   private static double normalizeAngle(double angleIn) {
      double angle = angleIn % (double)360.0F;
      if (angle >= (double)180.0F) {
         angle -= (double)360.0F;
      }

      if (angle < (double)-180.0F) {
         angle += (double)360.0F;
      }

      return angle;
   }

   @EventListener(
      priority = -99
   )
   public void onTimer(TimerEvent event) {
      if (this.applyTimer) {
         event.set(this.timer.getValueFloat());
      }

   }

   public void onEnable() {
      this.applyTimer = false;
      if (nullCheck()) {
         this.disable();
      } else {
         this.resetMove = false;
         this.holePos = Alien.HOLE.getHole((float)this.range.getValue(), true, this.any.getValue(), this.up.getValue());
      }
   }

   public void onDisable() {
      this.holePos = null;
      this.stuckTicks = 0;
      this.enabledTicks = 0;
      if (!nullCheck()) {
         if (this.resetMove && !this.grim.getValue()) {
            MovementUtil.setMotionX((double)0.0F);
            MovementUtil.setMotionZ((double)0.0F);
         }

      }
   }

   @EventListener
   public void onReceivePacket(PacketEvent.Receive event) {
      if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
         this.disable();
      }

   }

   @EventListener(
      priority = -999
   )
   public void onKeyInput(KeyboardInputEvent e) {
      if (this.grim.getValue()) {
         if (!AntiCheat.INSTANCE.movementSync()) {
            this.sendMessage("§4HoleSnap require MovementSync.");
            this.disable();
         } else if (!mc.player.isRiding() && !Freecam.INSTANCE.isOn()) {
            mc.player.input.movementSideways = 0.0F;
            mc.player.input.movementForward = 1.0F;
         }
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.holePos = Alien.HOLE.getHole((float)this.range.getValue(), true, this.any.getValue(), this.up.getValue());
      if (this.holePos == null) {
         this.disable();
      } else {
         ++this.enabledTicks;
         if ((double)this.enabledTicks > this.timeoutTicks.getValue() - (double)1.0F) {
            this.disable();
         } else {
            this.applyTimer = true;
            if (this.grim.getValue()) {
               if (mc.player.isAlive() && !mc.player.isFallFlying()) {
                  if (this.stuckTicks > 8) {
                     this.disable();
                  } else if (this.holePos == null) {
                     this.disable();
                  } else {
                     Vec3d playerPos = mc.player.getPos();
                     this.targetPos = new Vec3d((double)this.holePos.getX() + (double)0.5F, mc.player.getY(), (double)this.holePos.getZ() + (double)0.5F);
                     Direction facing;
                     if (Alien.HOLE.isDoubleHole(this.holePos) && (facing = Alien.HOLE.is3Block(this.holePos)) != null) {
                        this.targetPos = this.targetPos.add(new Vec3d((double)facing.getVector().getX() * (double)0.5F, (double)facing.getVector().getY() * (double)0.5F, (double)facing.getVector().getZ() * (double)0.5F));
                     }

                     this.applyTimer = true;
                     this.resetMove = true;
                     float rotation = getRotationTo(playerPos, this.targetPos).x;
                     float yawRad = rotation / 180.0F * (float)Math.PI;
                     double dist = playerPos.distanceTo(this.targetPos);
                     double cappedSpeed = Math.min(0.2873, dist);
                     double x = (double)(-((float)Math.sin((double)yawRad))) * cappedSpeed;
                     double z = (double)((float)Math.cos((double)yawRad)) * cappedSpeed;
                     if (Math.abs(x) < (double)0.25F && Math.abs(z) < (double)0.25F && playerPos.y <= (double)this.holePos.getY() + 0.8) {
                        this.disable();
                     } else {
                        this.stuckTicks = mc.player.horizontalCollision ? ++this.stuckTicks : 0;
                     }
                  }
               } else {
                  this.disable();
               }
            }
         }
      }
   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (this.grim.getValue() && this.holePos != null) {
         this.targetPos = new Vec3d((double)this.holePos.getX() + (double)0.5F, mc.player.getY(), (double)this.holePos.getZ() + (double)0.5F);
         Direction facing;
         if (Alien.HOLE.isDoubleHole(this.holePos) && (facing = Alien.HOLE.is3Block(this.holePos)) != null) {
            this.targetPos = this.targetPos.add(new Vec3d((double)facing.getVector().getX() * (double)0.5F, (double)facing.getVector().getY() * (double)0.5F, (double)facing.getVector().getZ() * (double)0.5F));
         }

         event.setTarget(this.targetPos, this.steps.getValueFloat(), this.priority.getValueFloat());
      }

   }

   @EventListener
   public void onMove(MoveEvent event) {
      if (!this.grim.getValue()) {
         if (mc.player.isAlive() && !mc.player.isFallFlying()) {
            if (this.stuckTicks > 8) {
               this.disable();
            } else if (this.holePos == null) {
               this.disable();
            } else {
               Vec3d playerPos = mc.player.getPos();
               this.targetPos = new Vec3d((double)this.holePos.getX() + (double)0.5F, mc.player.getY(), (double)this.holePos.getZ() + (double)0.5F);
               Direction facing;
               if (Alien.HOLE.isDoubleHole(this.holePos) && (facing = Alien.HOLE.is3Block(this.holePos)) != null) {
                  this.targetPos = this.targetPos.add(new Vec3d((double)facing.getVector().getX() * (double)0.5F, (double)facing.getVector().getY() * (double)0.5F, (double)facing.getVector().getZ() * (double)0.5F));
               }

               this.applyTimer = true;
               this.resetMove = true;
               float rotation = getRotationTo(playerPos, this.targetPos).x;
               float yawRad = rotation / 180.0F * (float)Math.PI;
               double dist = playerPos.distanceTo(this.targetPos);
               double cappedSpeed = Math.min(0.2873, dist);
               double x = (double)(-((float)Math.sin((double)yawRad))) * cappedSpeed;
               double z = (double)((float)Math.cos((double)yawRad)) * cappedSpeed;
               event.setX(x);
               event.setZ(z);
               if (Math.abs(x) < 0.1 && Math.abs(z) < 0.1 && playerPos.y <= (double)this.holePos.getY() + (double)0.5F) {
                  this.disable();
               }

               this.stuckTicks = mc.player.horizontalCollision ? ++this.stuckTicks : 0;
            }
         } else {
            this.disable();
         }
      }
   }

   public void onRender3D(MatrixStack matrixStack) {
      if (this.targetPos != null && this.holePos != null) {
         GL11.glEnable(3042);
         Color color = this.color.getValue();
         Vec3d pos = new Vec3d(this.targetPos.x, (double)this.holePos.getY(), this.targetPos.getZ());
         if (this.fade.getValue()) {
            double temp = 0.01;

            for(double i = (double)0.0F; i < this.circleSize.getValue(); i += temp) {
               drawCircle(matrixStack, ColorUtil.injectAlpha(color, (int)Math.min((double)(color.getAlpha() * 2) / (this.circleSize.getValue() / temp), (double)255.0F)), i, pos, this.segments.getValueInt());
            }
         } else {
            drawCircle(matrixStack, color, this.circleSize.getValue(), pos, this.segments.getValueInt());
         }

         GL11.glDisable(3042);
      }
   }
}
