package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.StreamSupport;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_286;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_2960;
import net.minecraft.class_3532;
import net.minecraft.class_4587;
import net.minecraft.class_5498;
import net.minecraft.class_757;
import net.minecraft.class_7833;
import net.minecraft.class_293.class_5596;
import org.joml.Matrix4f;

public class FireFlies extends Module {
   private final BooleanSetting darkImprint = this.add(new BooleanSetting("DarkImprint", false));
   private final BooleanSetting lighting = this.add(new BooleanSetting("Lighting", false));
   private final BooleanSetting rainbowColor = this.add(new BooleanSetting("RainbowColor", true));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255)));
   private final SliderSetting rainbowSpeed = this.add(new SliderSetting("RainbowSpeed", (double)4000.0F, (double)500.0F, (double)10000.0F, (double)250.0F));
   private final SliderSetting spawnDelay = this.add(new SliderSetting("SpawnDelay", (double)3.0F, (double)1.0F, (double)10.0F, (double)0.5F));
   private final SliderSetting animationSpeed = this.add(new SliderSetting("AnimationSpeed", (double)1.0F, 0.2, (double)3.0F, 0.1));
   private final List<FirePart> fireParts = new ArrayList();
   private final class_2960 firePartTexture = class_2960.method_60655("alien", "textures/fireflies/firepart.png");
   private final Random random = new Random();

   public FireFlies() {
      super("FireFlies", Module.Category.Render);
      this.setChinese("萤火虫粒子");
   }

   private long getMaxPartAliveTime() {
      float speed = (float)this.animationSpeed.getValue();
      return (long)(6000.0F / speed);
   }

   private int getPartColor() {
      if (this.rainbowColor.getValue()) {
         float speed = (float)this.rainbowSpeed.getValue();
         if (speed < 100.0F) {
            speed = 100.0F;
         }

         float hue = (float)(System.currentTimeMillis() % (long)speed) / speed;
         return -16777216 | Color.getHSBColor(hue, 1.0F, 1.0F).getRGB() & 16777215;
      } else {
         return -16777216 | this.color.getValue().getRGB() & 16777215;
      }
   }

   private float getAnimationSpeed() {
      float v = (float)this.animationSpeed.getValue();
      return class_3532.method_15363(v, 0.2F, 3.0F);
   }

   private double random(double min, double max) {
      return min + (max - min) * this.random.nextDouble();
   }

   private class_243 generateVecForPart(double rangeXZ, double rangeY) {
      class_243 base = mc.field_1724.method_19538();
      class_243 pos = base.method_1031(this.random(-rangeXZ, rangeXZ), this.random(-rangeY / (double)2.0F, rangeY), this.random(-rangeXZ, rangeXZ));

      for(int i = 0; i < 30; ++i) {
         pos = base.method_1031(this.random(-rangeXZ, rangeXZ), this.random(-rangeY / (double)2.0F, rangeY), this.random(-rangeXZ, rangeXZ));
      }

      return pos;
   }

   private void bindTexture(class_2960 location) {
      RenderSystem.setShaderTexture(0, location);
   }

   private void drawBoundTexture(class_4587 stack, float x, float y, float x2, float y2, int color) {
      float a = (float)(color >> 24 & 255) / 255.0F;
      float r = (float)(color >> 16 & 255) / 255.0F;
      float g = (float)(color >> 8 & 255) / 255.0F;
      float b = (float)(color & 255) / 255.0F;
      Matrix4f matrix = stack.method_23760().method_23761();
      RenderSystem.setShader(class_757::method_34543);
      class_287 buffer = class_289.method_1348().method_60827(class_5596.field_27382, class_290.field_1575);
      buffer.method_22918(matrix, x, y, 0.0F).method_22913(0.0F, 0.0F).method_22915(r, g, b, a);
      buffer.method_22918(matrix, x, y2, 0.0F).method_22913(0.0F, 1.0F).method_22915(r, g, b, a);
      buffer.method_22918(matrix, x2, y2, 0.0F).method_22913(1.0F, 1.0F).method_22915(r, g, b, a);
      buffer.method_22918(matrix, x2, y, 0.0F).method_22913(1.0F, 0.0F).method_22915(r, g, b, a);
      class_286.method_43433(buffer.method_60800());
   }

   private void drawBillboard(class_4587 stack, FirePart part, float partialTicks, int color) {
      class_243 pos = part.getRenderPosVec(partialTicks);
      stack.method_22903();
      stack.method_22904(pos.field_1352, pos.field_1351, pos.field_1350);
      float yaw = mc.field_1773.method_19418().method_19330();
      float pitch = mc.field_1773.method_19418().method_19329();
      boolean mirror = mc.field_1690.method_31044() == class_5498.field_26666;
      stack.method_22907(class_7833.field_40716.rotationDegrees(-yaw));
      stack.method_22907(class_7833.field_40714.rotationDegrees(pitch * (mirror ? -1.0F : 1.0F)));
      stack.method_22905(-0.1F, -0.1F, 0.1F);
      float scale = 7.0F;
      this.bindTexture(this.firePartTexture);
      this.drawBoundTexture(stack, -scale / 2.0F, -scale / 2.0F, scale / 2.0F, scale / 2.0F, color);
      if (this.lighting.getValue()) {
         scale *= 3.0F;
         int darker = this.darker(color, 0.8F);
         int lightingColor = this.applyOpacity(darker, (float)this.getAlpha(color) / 7.0F / 255.0F);
         this.drawBoundTexture(stack, -scale / 2.0F, -scale / 2.0F, scale / 2.0F, scale / 2.0F, lightingColor);
      }

      stack.method_22909();
   }

   @EventListener
   public void onUpdate(ClientTickEvent event) {
      if (mc.field_1724 != null && mc.field_1687 != null) {
         if (mc.field_1724.field_6012 == 1) {
            this.fireParts.forEach(FirePart::setToRemove);
         }

         this.fireParts.forEach(FirePart::updatePart);
         this.fireParts.removeIf(FirePart::isToRemove);
         int delayTicks = (int)(this.spawnDelay.getValue() + (double)1.0F);
         if (delayTicks < 1) {
            delayTicks = 1;
         }

         if (mc.field_1724.field_6012 % delayTicks == 0) {
            this.fireParts.add(new FirePart(this.generateVecForPart((double)10.0F, (double)4.0F), this.getMaxPartAliveTime()));
            this.fireParts.add(new FirePart(this.generateVecForPart((double)6.0F, (double)5.0F), this.getMaxPartAliveTime()));
         }

      }
   }

   public void onRender3D(class_4587 stack) {
      if (mc.field_1724 != null && mc.field_1687 != null) {
         if (!this.fireParts.isEmpty()) {
            float partialTicks = mc.method_60646().method_60637(true);
            stack.method_22903();
            class_243 cameraPos = mc.field_1773.method_19418().method_19326();
            stack.method_22904(-cameraPos.field_1352, -cameraPos.field_1351, -cameraPos.field_1350);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(770, 1, 1, 0);
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);

            for(FirePart part : this.fireParts) {
               int baseColor = this.getPartColor();
               float alphaPC = part.getAlphaPC();
               int color = this.withAlpha(baseColor, (int)(alphaPC * 255.0F));
               if (this.darkImprint.getValue()) {
                  RenderSystem.blendFuncSeparate(770, 771, 1, 0);
                  this.drawSparkPartsList(stack, baseColor, part, partialTicks);
                  this.drawTrailPartsList(stack, baseColor, part);
                  RenderSystem.blendFuncSeparate(770, 1, 1, 0);
               } else {
                  this.drawSparkPartsList(stack, baseColor, part, partialTicks);
                  this.drawTrailPartsList(stack, baseColor, part);
               }

               this.drawBillboard(stack, part, partialTicks, color);
            }

            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            stack.method_22909();
         }
      }
   }

   public void onEnable() {
      this.fireParts.clear();
   }

   public void onDisable() {
      this.fireParts.clear();
   }

   private void drawSparkPartsList(class_4587 stack, int baseColor, FirePart firePart, float partialTicks) {
      if (firePart.sparkParts.size() >= 2) {
         double distance = mc.field_1724.method_19538().method_1022(firePart.getPosVec().method_1031((double)0.0F, 1.6, (double)0.0F));
         float sizeFactor = 1.0F - (float)((distance - (double)3.0F) / (double)10.0F);
         sizeFactor = class_3532.method_15363(sizeFactor, 0.0F, 1.0F);
         float pointSize = 1.5F + 6.0F * sizeFactor;
         RenderSystem.lineWidth(pointSize);
         Matrix4f matrix = stack.method_23760().method_23761();
         RenderSystem.setShader(class_757::method_34540);
         class_287 buffer = class_289.method_1348().method_60827(class_5596.field_29344, class_290.field_1576);

         for(SparkPart spark : firePart.sparkParts) {
            float timePc = (float)spark.timePC();
            Color from = new Color(255, 255, 255, 255);
            Color to = new Color(baseColor, true);
            int r = (int)((float)from.getRed() + (float)(to.getRed() - from.getRed()) * timePc);
            int g = (int)((float)from.getGreen() + (float)(to.getGreen() - from.getGreen()) * timePc);
            int b = (int)((float)from.getBlue() + (float)(to.getBlue() - from.getBlue()) * timePc);
            int a = (int)((float)from.getAlpha() + (float)(to.getAlpha() - from.getAlpha()) * timePc);
            float alpha = (float)this.getAlpha(baseColor) / 255.0F * (1.0F - timePc);
            int c = this.applyOpacity(a << 24 | r << 16 | g << 8 | b, alpha);
            float fa = (float)(c >> 24 & 255) / 255.0F;
            float fr = (float)(c >> 16 & 255) / 255.0F;
            float fg = (float)(c >> 8 & 255) / 255.0F;
            float fb = (float)(c & 255) / 255.0F;
            float x = (float)spark.getRenderPosX(partialTicks);
            float y = (float)spark.getRenderPosY(partialTicks);
            float z = (float)spark.getRenderPosZ(partialTicks);
            buffer.method_22918(matrix, x, y, z).method_22915(fr, fg, fb, fa);
            buffer.method_22918(matrix, x, y + 0.001F, z).method_22915(fr, fg, fb, fa);
         }

         class_286.method_43433(buffer.method_60800());
         RenderSystem.lineWidth(1.0F);
      }
   }

   private void drawTrailPartsList(class_4587 stack, int baseColor, FirePart firePart) {
      if (firePart.trailParts.size() >= 2) {
         double distance = mc.field_1724.method_19538().method_1022(firePart.getPosVec().method_1031((double)0.0F, 1.6, (double)0.0F));
         float sizeFactor = 1.0F - (float)((distance - (double)3.0F) / (double)20.0F);
         sizeFactor = class_3532.method_15363(sizeFactor, 0.0F, 1.0F);
         float lineWidth = 1.0E-5F + 8.0F * sizeFactor;
         RenderSystem.lineWidth(lineWidth);
         int point = 0;
         int pointsCount = firePart.trailParts.size();
         Matrix4f matrix = stack.method_23760().method_23761();
         RenderSystem.setShader(class_757::method_34540);
         class_287 buffer = class_289.method_1348().method_60827(class_5596.field_29345, class_290.field_1576);

         for(TrailPart trail : firePart.trailParts) {
            float sizePc = (float)point / (float)pointsCount;
            sizePc = ((double)sizePc > (double)0.5F ? 1.0F - sizePc : sizePc) * 2.0F;
            sizePc = class_3532.method_15363(sizePc, 0.0F, 1.0F);
            float alpha = (float)this.getAlpha(baseColor) / 255.0F * sizePc;
            int c = this.applyOpacity(baseColor, alpha);
            float a = (float)(c >> 24 & 255) / 255.0F;
            float r = (float)(c >> 16 & 255) / 255.0F;
            float g = (float)(c >> 8 & 255) / 255.0F;
            float b = (float)(c & 255) / 255.0F;
            buffer.method_22918(matrix, (float)trail.x, (float)trail.y, (float)trail.z).method_22915(r, g, b, a);
            ++point;
         }

         class_286.method_43433(buffer.method_60800());
         RenderSystem.lineWidth(1.0F);
      }
   }

   private int applyOpacity(int color, float opacity) {
      opacity = class_3532.method_15363(opacity, 0.0F, 1.0F);
      int a = (int)((float)(color >> 24 & 255) * opacity);
      if (a < 0) {
         a = 0;
      }

      if (a > 255) {
         a = 255;
      }

      return a << 24 | color & 16777215;
   }

   private int withAlpha(int color, int alpha) {
      if (alpha < 0) {
         alpha = 0;
      }

      if (alpha > 255) {
         alpha = 255;
      }

      return alpha << 24 | color & 16777215;
   }

   private int darker(int color, float factor) {
      int r = (int)((float)(color >> 16 & 255) * factor);
      int g = (int)((float)(color >> 8 & 255) * factor);
      int b = (int)((float)(color & 255) * factor);
      int a = color >> 24 & 255;
      return a << 24 | r << 16 | g << 8 | b;
   }

   private int getAlpha(int color) {
      return color >> 24 & 255;
   }

   private final class FirePart {
      private final List<TrailPart> trailParts = new ArrayList();
      private final List<SparkPart> sparkParts = new ArrayList();
      private class_243 prevPos;
      private class_243 pos;
      private float alphaPC;
      private float alphaTarget = 1.0F;
      private int msChangeSideRate = this.getMsChangeSideRate();
      private float moveYawSet = (float)FireFlies.this.random((double)0.0F, (double)360.0F);
      private float speed = (float)FireFlies.this.random(0.1, (double)0.25F);
      private float yMotion = (float)FireFlies.this.random(-0.075, 0.1);
      private float moveYaw;
      private final float maxAlive;
      private final long startTime;
      private long rateTimer;
      private boolean toRemove;

      public FirePart(class_243 pos, long maxAlive) {
         this.moveYaw = this.moveYawSet;
         this.pos = pos;
         this.prevPos = pos;
         this.maxAlive = (float)maxAlive;
         this.startTime = System.currentTimeMillis();
         this.rateTimer = this.startTime;
      }

      public float getTimePC() {
         float speed = FireFlies.this.getAnimationSpeed();
         return class_3532.method_15363((float)(System.currentTimeMillis() - this.startTime) * speed / this.maxAlive, 0.0F, 1.0F);
      }

      public void setAlphaPCTo(float to) {
         this.alphaTarget = to;
      }

      public float getAlphaPC() {
         return this.alphaPC;
      }

      private void updateAlpha() {
         float base = 0.05F;
         float speed = FireFlies.this.getAnimationSpeed();
         this.alphaPC += (this.alphaTarget - this.alphaPC) * base * speed;
      }

      public class_243 getPosVec() {
         return this.pos;
      }

      public class_243 getRenderPosVec(float partialTicks) {
         class_243 p = this.getPosVec();
         return p.method_1031(-(this.prevPos.field_1352 - p.field_1352) * (double)partialTicks, -(this.prevPos.field_1351 - p.field_1351) * (double)partialTicks, -(this.prevPos.field_1350 - p.field_1350) * (double)partialTicks);
      }

      public void updatePart() {
         if (System.currentTimeMillis() - this.rateTimer >= (long)this.msChangeSideRate) {
            this.msChangeSideRate = this.getMsChangeSideRate();
            this.rateTimer = System.currentTimeMillis();
            this.moveYawSet = (float)FireFlies.this.random((double)0.0F, (double)360.0F);
         }

         float interpSpeed = 0.065F * FireFlies.this.getAnimationSpeed();
         this.moveYaw += (this.moveYawSet - this.moveYaw) * interpSpeed;
         float motionX = -((float)Math.sin(Math.toRadians((double)this.moveYaw))) * (this.speed /= 1.005F);
         float motionZ = (float)Math.cos(Math.toRadians((double)this.moveYaw)) * this.speed;
         this.prevPos = this.pos;
         float scaleBox = 0.1F;
         class_238 box = new class_238(this.pos.field_1352 - (double)(scaleBox / 2.0F), this.pos.field_1351, this.pos.field_1350 - (double)(scaleBox / 2.0F), this.pos.field_1352 + (double)(scaleBox / 2.0F), this.pos.field_1351 + (double)scaleBox, this.pos.field_1350 + (double)(scaleBox / 2.0F));
         boolean colliding = StreamSupport.stream(Wrapper.mc.field_1687.method_20812(Wrapper.mc.field_1724, box).spliterator(), false).findAny().isPresent();
         float divider = colliding ? 0.3F : 1.0F;
         float speedMul = FireFlies.this.getAnimationSpeed();
         this.pos = this.pos.method_1031((double)(motionX * speedMul / divider), (double)((this.yMotion /= 1.02F) * speedMul / divider), (double)(motionZ * speedMul / divider));
         float timePc = this.getTimePC();
         if (timePc >= 1.0F) {
            this.setAlphaPCTo(0.0F);
         }

         this.updateAlpha();
         if (timePc >= 1.0F && this.alphaPC < 0.003921569F) {
            this.setToRemove();
         }

         this.trailParts.add(FireFlies.this.new TrailPart(this, 400L));
         if (!this.trailParts.isEmpty()) {
            this.trailParts.removeIf(TrailPart::toRemove);
         }

         for(int i = 0; i < 2; ++i) {
            this.sparkParts.add(FireFlies.this.new SparkPart(this, 300L));
         }

         this.sparkParts.forEach(SparkPart::motionSparkProcess);
         if (!this.sparkParts.isEmpty()) {
            this.sparkParts.removeIf(SparkPart::toRemove);
         }

      }

      public void setToRemove() {
         this.toRemove = true;
      }

      public boolean isToRemove() {
         return this.toRemove;
      }

      int getMsChangeSideRate() {
         float speed = FireFlies.this.getAnimationSpeed();
         return (int)(FireFlies.this.random((double)300.5F, (double)900.5F) / (double)speed);
      }
   }

   private final class SparkPart {
      private double posX;
      private double posY;
      private double posZ;
      private double prevPosX;
      private double prevPosY;
      private double prevPosZ;
      private final double speed = Math.random() / (double)30.0F;
      private final double radianYaw = Math.random() * (double)360.0F;
      private final double radianPitch = (double)-90.0F + Math.random() * (double)180.0F;
      private final long time;
      private final long maxTime;

      public SparkPart(FirePart parent, long maxTime) {
         this.posX = parent.pos.field_1352;
         this.posY = parent.pos.field_1351;
         this.posZ = parent.pos.field_1350;
         this.prevPosX = this.posX;
         this.prevPosY = this.posY;
         this.prevPosZ = this.posZ;
         this.maxTime = maxTime;
         this.time = System.currentTimeMillis();
      }

      public boolean toRemove() {
         return System.currentTimeMillis() - this.time >= this.maxTime;
      }

      public double timePC() {
         return class_3532.method_15350((double)(System.currentTimeMillis() - this.time) / (double)this.maxTime, (double)0.0F, (double)1.0F);
      }

      public void motionSparkProcess() {
         this.prevPosX = this.posX;
         this.prevPosY = this.posY;
         this.prevPosZ = this.posZ;
         double radYaw = Math.toRadians(this.radianYaw);
         this.posX += Math.sin(radYaw) * this.speed;
         this.posY += Math.cos(Math.toRadians(this.radianPitch - (double)90.0F)) * this.speed;
         this.posZ += Math.cos(radYaw) * this.speed;
      }

      public double getRenderPosX(float partialTicks) {
         return this.prevPosX + (this.posX - this.prevPosX) * (double)partialTicks;
      }

      public double getRenderPosY(float partialTicks) {
         return this.prevPosY + (this.posY - this.prevPosY) * (double)partialTicks;
      }

      public double getRenderPosZ(float partialTicks) {
         return this.prevPosZ + (this.posZ - this.prevPosZ) * (double)partialTicks;
      }
   }

   private final class TrailPart {
      private final double x;
      private final double y;
      private final double z;
      private final long time;
      private final long maxTime;

      public TrailPart(FirePart parent, long maxTime) {
         this.x = parent.pos.field_1352;
         this.y = parent.pos.field_1351;
         this.z = parent.pos.field_1350;
         this.time = System.currentTimeMillis();
         this.maxTime = maxTime;
      }

      public boolean toRemove() {
         return System.currentTimeMillis() - this.time >= this.maxTime;
      }
   }
}
