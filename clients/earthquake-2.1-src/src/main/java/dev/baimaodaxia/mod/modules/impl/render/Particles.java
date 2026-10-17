package dev.baimaodaxia.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.Wrapper;
import dev.baimaodaxia.api.utils.math.MathUtil;
import dev.baimaodaxia.api.utils.render.ColorUtil;
import dev.baimaodaxia.api.utils.render.Render3DUtil;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.client.HUD;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.ColorSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.render.VertexFormat.DrawMode;
import org.joml.Matrix4f;

public class Particles extends Module {
   private final BooleanSetting fireFlies = this.add((new BooleanSetting("FireFlies", true)).setParent());
   private final SliderSetting ffCount;
   private final SliderSetting ffSize;
   private final EnumSetting<Mode> mode;
   private final SliderSetting count;
   private final SliderSetting size;
   private final EnumSetting<ColorMode> colorMode;
   private final ColorSetting color;
   private final EnumSetting<Physics> physics;
   private final ArrayList<ParticleBase> fireFlyParticles;
   private final ArrayList<ParticleBase> particles;
   private final Random random;
   private final Identifier textureFirefly;
   private final Identifier textureSnowflake;
   private final Identifier textureStar;
   private final Identifier textureHeart;
   private final Identifier textureDollar;

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
      this.textureFirefly = Identifier.of("alien", "textures/particles/firefly.png");
      this.textureSnowflake = Identifier.of("alien", "textures/particles/snowflake.png");
      this.textureStar = Identifier.of("alien", "textures/particles/star.png");
      this.textureHeart = Identifier.of("alien", "textures/particles/heart.png");
      this.textureDollar = Identifier.of("alien", "textures/particles/dollar.png");
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

   public void onRender3D(MatrixStack stack) {
      if (!nullCheck()) {
         float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
         Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
         if (this.fireFlies.getValue() && !this.fireFlyParticles.isEmpty()) {
            stack.push();
            stack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(770, 1, 1, 0);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            RenderSystem.setShaderTexture(0, this.textureFirefly);
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for(ParticleBase p : this.fireFlyParticles) {
               p.render(stack, bufferBuilder, tickDelta, (float)this.ffSize.getValue(), this.getColor(p.age * 10), true);
            }

            Render3DUtil.endBuilding(bufferBuilder);
            RenderSystem.depthMask(true);
            RenderSystem.disableDepthTest();
            RenderSystem.disableBlend();
            stack.pop();
         }

         if (this.mode.getValue() != Particles.Mode.Off && !this.particles.isEmpty()) {
            Identifier tex = this.getModeTexture();
            if (tex == null) {
               return;
            }

            stack.push();
            stack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(770, 1, 1, 0);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            RenderSystem.setShaderTexture(0, tex);
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for(ParticleBase p : this.particles) {
               p.render(stack, bufferBuilder, tickDelta, (float)this.size.getValue(), this.getColor(p.age * 2), false);
            }

            Render3DUtil.endBuilding(bufferBuilder);
            RenderSystem.depthMask(true);
            RenderSystem.blendFuncSeparate(770, 771, 1, 0);
            RenderSystem.disableDepthTest();
            RenderSystem.disableBlend();
            stack.pop();
         }

      }
   }

   private void spawnFireflies() {
      if (this.fireFlies.getValue()) {
         int target = this.ffCount.getValueInt();

         for(int i = this.fireFlyParticles.size(); i < target; ++i) {
            this.fireFlyParticles.add(new FireFly((float)(mc.player.getX() + (double)this.random(-25.0F, 25.0F)), (float)(mc.player.getY() + (double)this.random(2.0F, 15.0F)), (float)(mc.player.getZ() + (double)this.random(-25.0F, 25.0F)), this.random(-0.2F, 0.2F), this.random(-0.1F, 0.1F), this.random(-0.2F, 0.2F)));
         }

      }
   }

   private void spawnParticles() {
      if (this.mode.getValue() != Particles.Mode.Off) {
         int target = this.count.getValueInt();
         boolean drop = this.physics.getValue() == Particles.Physics.Drop;

         for(int i = this.particles.size(); i < target; ++i) {
            this.particles.add(new ParticleBase((float)(mc.player.getX() + (double)this.random(-48.0F, 48.0F)), (float)(mc.player.getY() + (double)this.random(2.0F, 48.0F)), (float)(mc.player.getZ() + (double)this.random(-48.0F, 48.0F)), drop ? 0.0F : this.random(-0.4F, 0.4F), drop ? this.random(-0.2F, -0.05F) : this.random(-0.1F, 0.1F), drop ? 0.0F : this.random(-0.4F, 0.4F)));
         }

      }
   }

   private Identifier getModeTexture() {
      Identifier var10000;
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
      private final List<TrailPoint> trail = new ArrayList();

      public FireFly(float posX, float posY, float posZ, float motionX, float motionY, float motionZ) {
         super(posX, posY, posZ, motionX, motionY, motionZ);
      }

      public boolean tick() {
         if (Wrapper.mc.player.squaredDistanceTo((double)this.posX, (double)this.posY, (double)this.posZ) > (double)100.0F) {
            this.age -= 4;
         } else if (!Wrapper.mc.world.getBlockState(new BlockPos((int)this.posX, (int)this.posY, (int)this.posZ)).isAir()) {
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
            this.trail.add(new TrailPoint(new Vec3d((double)this.posX, (double)this.posY, (double)this.posZ), this.age));
            if (this.trail.size() > 20) {
               this.trail.remove(0);
            }

            this.motionX *= 0.99F;
            this.motionY *= 0.99F;
            this.motionZ *= 0.99F;
            return false;
         }
      }

      public void render(MatrixStack stack, BufferBuilder bb, float tickDelta, float size, int argb, boolean fireFly) {
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
         if (Wrapper.mc.player.squaredDistanceTo((double)this.posX, (double)this.posY, (double)this.posZ) > (double)4096.0F) {
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

      public void render(MatrixStack stack, BufferBuilder bb, float tickDelta, float size, int argb, boolean fireFly) {
         Vec3d pos = new Vec3d((double)MathUtil.interpolate(this.prevposX, this.posX, tickDelta), (double)MathUtil.interpolate(this.prevposY, this.posY, tickDelta), (double)MathUtil.interpolate(this.prevposZ, this.posZ, tickDelta));
         this.renderAt(stack, bb, pos, size, ColorUtil.injectAlpha(argb, (int)(255.0F * ((float)this.age / (float)this.maxAge))), fireFly);
      }

      protected void renderAt(MatrixStack stack, BufferBuilder bb, Vec3d pos, float size, int argb, boolean fireFly) {
         stack.push();
         stack.translate(pos.x, pos.y, pos.z);
         float yaw = Wrapper.mc.gameRenderer.getCamera().getYaw();
         float pitch = Wrapper.mc.gameRenderer.getCamera().getPitch();
         boolean mirror = Wrapper.mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT;
         stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
         stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch * (mirror ? -1.0F : 1.0F)));
         Matrix4f matrix = stack.peek().getPositionMatrix();
         bb.vertex(matrix, 0.0F, -size, 0.0F).texture(0.0F, 1.0F).color(argb);
         bb.vertex(matrix, -size, -size, 0.0F).texture(1.0F, 1.0F).color(argb);
         bb.vertex(matrix, -size, 0.0F, 0.0F).texture(1.0F, 0.0F).color(argb);
         bb.vertex(matrix, 0.0F, 0.0F, 0.0F).texture(0.0F, 0.0F).color(argb);
         stack.pop();
      }
   }

   private static record TrailPoint(Vec3d pos, int age) {
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
