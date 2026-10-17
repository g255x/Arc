package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.HUD;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_2960;
import net.minecraft.class_4587;
import net.minecraft.class_5498;
import net.minecraft.class_757;
import net.minecraft.class_7833;
import net.minecraft.class_293.class_5596;
import org.joml.Matrix4f;

public class Particles extends Module {
   private final BooleanSetting fireFlies = this.add((new BooleanSetting("FireFlies", true)).setParent());
   private final SliderSetting ffCount;
   private final SliderSetting ffSize;
   private final EnumSetting mode;
   private final SliderSetting count;
   private final SliderSetting size;
   private final EnumSetting colorMode;
   private final ColorSetting color;
   private final EnumSetting physics;
   private final ArrayList fireFlyParticles;
   private final ArrayList particles;
   private final Random random;
   private final class_2960 textureFirefly;
   private final class_2960 textureSnowflake;
   private final class_2960 textureStar;
   private final class_2960 textureHeart;
   private final class_2960 textureDollar;

   public Particles() {
      super("Particles", Module.Category.Render);
      BooleanSetting var10008 = this.fireFlies;
      Objects.requireNonNull(var10008);
      this.ffCount = this.add(new SliderSetting("FFCount", 30, 20, 200, var10008::isOpen));
      BooleanSetting var10009 = this.fireFlies;
      Objects.requireNonNull(var10009);
      this.ffSize = this.add(new SliderSetting("FFSize", (double)1.0F, 0.1, (double)2.0F, 0.05, var10009::isOpen));
      this.mode = this.add(new EnumSetting("Mode", Particles.Mode.SnowFlake));
      this.count = this.add(new SliderSetting("Count", 100, 20, 800, () -> this.mode.getValue() != Particles.Mode.Off));
      this.size = this.add(new SliderSetting("Size", (double)1.0F, 0.1, (double)6.0F, 0.1, () -> this.mode.getValue() != Particles.Mode.Off));
      this.colorMode = this.add(new EnumSetting("ColorMode", Particles.ColorMode.Sync, () -> this.mode.getValue() != Particles.Mode.Off));
      this.color = this.add(new ColorSetting("Color", new Color(3649978), () -> this.mode.getValue() != Particles.Mode.Off && this.colorMode.getValue() == Particles.ColorMode.Custom));
      this.physics = this.add(new EnumSetting("Physics", Particles.Physics.Fly, () -> this.mode.getValue() != Particles.Mode.Off));
      this.fireFlyParticles = new ArrayList();
      this.particles = new ArrayList();
      this.random = new Random();
      this.textureFirefly = class_2960.method_60655("alien", "textures/particles/firefly.png");
      this.textureSnowflake = class_2960.method_60655("alien", "textures/particles/snowflake.png");
      this.textureStar = class_2960.method_60655("alien", "textures/particles/star.png");
      this.textureHeart = class_2960.method_60655("alien", "textures/particles/heart.png");
      this.textureDollar = class_2960.method_60655("alien", "textures/particles/dollar.png");
      this.setChinese("粒子");
   }

   public void onEnable() {
      this.fireFlyParticles.clear();
      this.particles.clear();
   }

   public void onDisable() {
      this.fireFlyParticles.clear();
      this.particles.clear();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck()) {
         this.fireFlyParticles.removeIf(ParticleBase::tick);
         this.particles.removeIf(ParticleBase::tick);
         this.spawnFireflies();
         this.spawnParticles();
      }
   }

   public void onRender3D(class_4587 stack) {
      if (!nullCheck()) {
         float tickDelta = mc.method_60646().method_60637(true);
         class_243 cameraPos = mc.field_1773.method_19418().method_19326();
         if (this.fireFlies.getValue() && !this.fireFlyParticles.isEmpty()) {
            stack.method_22903();
            stack.method_22904(-cameraPos.field_1352, -cameraPos.field_1351, -cameraPos.field_1350);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(770, 1, 1, 0);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(class_757::method_34543);
            RenderSystem.setShaderTexture(0, this.textureFirefly);
            class_287 bufferBuilder = class_289.method_1348().method_60827(class_5596.field_27382, class_290.field_1575);

            for(ParticleBase p : this.fireFlyParticles) {
               p.render(stack, bufferBuilder, tickDelta, (float)this.ffSize.getValue(), this.getColor(p.age * 10), true);
            }

            Render3DUtil.endBuilding(bufferBuilder);
            RenderSystem.depthMask(true);
            RenderSystem.disableDepthTest();
            RenderSystem.disableBlend();
            stack.method_22909();
         }

         if (this.mode.getValue() != Particles.Mode.Off && !this.particles.isEmpty()) {
            class_2960 tex = this.getModeTexture();
            if (tex == null) {
               return;
            }

            stack.method_22903();
            stack.method_22904(-cameraPos.field_1352, -cameraPos.field_1351, -cameraPos.field_1350);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(770, 1, 1, 0);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(class_757::method_34543);
            RenderSystem.setShaderTexture(0, tex);
            class_287 bufferBuilder = class_289.method_1348().method_60827(class_5596.field_27382, class_290.field_1575);

            for(ParticleBase p : this.particles) {
               p.render(stack, bufferBuilder, tickDelta, (float)this.size.getValue(), this.getColor(p.age * 2), false);
            }

            Render3DUtil.endBuilding(bufferBuilder);
            RenderSystem.depthMask(true);
            RenderSystem.blendFuncSeparate(770, 771, 1, 0);
            RenderSystem.disableDepthTest();
            RenderSystem.disableBlend();
            stack.method_22909();
         }

      }
   }

   private void spawnFireflies() {
      if (this.fireFlies.getValue()) {
         int target = this.ffCount.getValueInt();

         for(int i = this.fireFlyParticles.size(); i < target; ++i) {
            this.fireFlyParticles.add(new FireFly((float)(mc.field_1724.method_23317() + (double)this.random(-25.0F, 25.0F)), (float)(mc.field_1724.method_23318() + (double)this.random(2.0F, 15.0F)), (float)(mc.field_1724.method_23321() + (double)this.random(-25.0F, 25.0F)), this.random(-0.2F, 0.2F), this.random(-0.1F, 0.1F), this.random(-0.2F, 0.2F)));
         }

      }
   }

   private void spawnParticles() {
      if (this.mode.getValue() != Particles.Mode.Off) {
         int target = this.count.getValueInt();
         boolean drop = this.physics.getValue() == Particles.Physics.Drop;

         for(int i = this.particles.size(); i < target; ++i) {
            this.particles.add(new ParticleBase((float)(mc.field_1724.method_23317() + (double)this.random(-48.0F, 48.0F)), (float)(mc.field_1724.method_23318() + (double)this.random(2.0F, 48.0F)), (float)(mc.field_1724.method_23321() + (double)this.random(-48.0F, 48.0F)), drop ? 0.0F : this.random(-0.4F, 0.4F), drop ? this.random(-0.2F, -0.05F) : this.random(-0.1F, 0.1F), drop ? 0.0F : this.random(-0.4F, 0.4F)));
         }

      }
   }

   private class_2960 getModeTexture() {
      class_2960 var10000;
      switch (((Mode)this.mode.getValue()).ordinal()) {
         case 0 -> var10000 = null;
         case 1 -> var10000 = this.textureSnowflake;
         case 2 -> var10000 = this.textureStar;
         case 3 -> var10000 = this.textureHeart;
         case 4 -> var10000 = this.textureDollar;
         case 5 -> var10000 = this.textureFirefly;
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   private int getColor(int counter) {
      if (this.colorMode.getValue() == Particles.ColorMode.Sync) {
         return HUD.INSTANCE != null ? HUD.INSTANCE.getColor((double)counter) : -1;
      } else {
         return this.color.getValue().getRGB();
      }
   }

   private float random(float min, float max) {
      return min + (max - min) * this.random.nextFloat();
   }

   public class FireFly extends ParticleBase {
      private final List trail = new ArrayList();

      public FireFly(float posX, float posY, float posZ, float motionX, float motionY, float motionZ) {
         super(posX, posY, posZ, motionX, motionY, motionZ);
      }

      public boolean tick() {
         if (Wrapper.mc.field_1724.method_5649((double)this.posX, (double)this.posY, (double)this.posZ) > (double)100.0F) {
            this.age -= 4;
         } else if (!Wrapper.mc.field_1687.method_8320(new class_2338((int)this.posX, (int)this.posY, (int)this.posZ)).method_26215()) {
            this.age -= 8;
         } else {
            --this.age;
         }

         if (this.age < 0) {
            return true;
         } else {
            this.prevposX = this.posX;
            this.prevposY = this.posY;
            this.prevposZ = this.posZ;
            this.posX += this.motionX;
            this.posY += this.motionY;
            this.posZ += this.motionZ;
            this.trail.add(new TrailPoint(new class_243((double)this.posX, (double)this.posY, (double)this.posZ), this.age));
            if (this.trail.size() > 20) {
               this.trail.remove(0);
            }

            this.motionX *= 0.99F;
            this.motionY *= 0.99F;
            this.motionZ *= 0.99F;
            return false;
         }
      }

      public void render(class_4587 stack, class_287 bb, float tickDelta, float size, int argb, boolean fireFly) {
         int trailSize = this.trail.size();
         if (trailSize > 1) {
            for(int i = 0; i < trailSize; ++i) {
               TrailPoint tp = (TrailPoint)this.trail.get(i);
               float k = (float)i / (float)trailSize;
               int alpha = (int)(255.0F * ((float)this.age / (float)this.maxAge) * k);
               super.renderAt(stack, bb, tp.pos, size, ColorUtil.injectAlpha(argb, alpha), true);
            }
         }

         super.render(stack, bb, tickDelta, size, argb, true);
      }
   }

   public class ParticleBase {
      protected float prevposX;
      protected float prevposY;
      protected float prevposZ;
      protected float posX;
      protected float posY;
      protected float posZ;
      protected float motionX;
      protected float motionY;
      protected float motionZ;
      protected int age;
      protected int maxAge;

      public ParticleBase(float posX, float posY, float posZ, float motionX, float motionY, float motionZ) {
         this.posX = posX;
         this.posY = posY;
         this.posZ = posZ;
         this.prevposX = posX;
         this.prevposY = posY;
         this.prevposZ = posZ;
         this.motionX = motionX;
         this.motionY = motionY;
         this.motionZ = motionZ;
         this.age = (int)Particles.this.random(100.0F, 300.0F);
         this.maxAge = this.age;
      }

      public boolean tick() {
         if (Wrapper.mc.field_1724.method_5649((double)this.posX, (double)this.posY, (double)this.posZ) > (double)4096.0F) {
            this.age -= 8;
         } else {
            --this.age;
         }

         if (this.age < 0) {
            return true;
         } else {
            this.prevposX = this.posX;
            this.prevposY = this.posY;
            this.prevposZ = this.posZ;
            this.posX += this.motionX;
            this.posY += this.motionY;
            this.posZ += this.motionZ;
            this.motionX *= 0.9F;
            if (Particles.this.physics.getValue() == Particles.Physics.Fly) {
               this.motionY *= 0.9F;
            }

            this.motionZ *= 0.9F;
            this.motionY -= 0.001F;
            return false;
         }
      }

      public void render(class_4587 stack, class_287 bb, float tickDelta, float size, int argb, boolean fireFly) {
         class_243 pos = new class_243((double)MathUtil.interpolate(this.prevposX, this.posX, tickDelta), (double)MathUtil.interpolate(this.prevposY, this.posY, tickDelta), (double)MathUtil.interpolate(this.prevposZ, this.posZ, tickDelta));
         this.renderAt(stack, bb, pos, size, ColorUtil.injectAlpha(argb, (int)(255.0F * ((float)this.age / (float)this.maxAge))), fireFly);
      }

      protected void renderAt(class_4587 stack, class_287 bb, class_243 pos, float size, int argb, boolean fireFly) {
         stack.method_22903();
         stack.method_22904(pos.field_1352, pos.field_1351, pos.field_1350);
         float yaw = Wrapper.mc.field_1773.method_19418().method_19330();
         float pitch = Wrapper.mc.field_1773.method_19418().method_19329();
         boolean mirror = Wrapper.mc.field_1690.method_31044() == class_5498.field_26666;
         stack.method_22907(class_7833.field_40716.rotationDegrees(-yaw));
         stack.method_22907(class_7833.field_40714.rotationDegrees(pitch * (mirror ? -1.0F : 1.0F)));
         Matrix4f matrix = stack.method_23760().method_23761();
         bb.method_22918(matrix, 0.0F, -size, 0.0F).method_22913(0.0F, 1.0F).method_39415(argb);
         bb.method_22918(matrix, -size, -size, 0.0F).method_22913(1.0F, 1.0F).method_39415(argb);
         bb.method_22918(matrix, -size, 0.0F, 0.0F).method_22913(1.0F, 0.0F).method_39415(argb);
         bb.method_22918(matrix, 0.0F, 0.0F, 0.0F).method_22913(0.0F, 0.0F).method_39415(argb);
         stack.method_22909();
      }
   }

   private static record TrailPoint(class_243 pos, int age) {
   }

   public static enum ColorMode {
      Custom,
      Sync;

      // $FF: synthetic method
      private static ColorMode[] $values() {
         return new ColorMode[]{Custom, Sync};
      }
   }

   public static enum Mode {
      Off,
      SnowFlake,
      Stars,
      Hearts,
      Dollars,
      Bloom;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Off, SnowFlake, Stars, Hearts, Dollars, Bloom};
      }
   }

   public static enum Physics {
      Drop,
      Fly;

      // $FF: synthetic method
      private static Physics[] $values() {
         return new Physics[]{Drop, Fly};
      }
   }
}
