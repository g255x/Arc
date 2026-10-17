package dev.kissoo.mod.modules.impl.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.DeathEvent;
import dev.kissoo.api.events.impl.TotemEvent;
import hoprc.api.live2d.CubismNativeModel;
import hoprc.api.live2d.CubismNativeRenderer;
import dev.kissoo.api.utils.render.Render2DUtil;
import dev.kissoo.api.utils.render.TextUtil;
import dev.kissoo.mod.modules.HudElement;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.Set;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class NoirLive2D extends Module implements HudElement {
   public static NoirLive2D INSTANCE;

   private static final Identifier PREVIEW_TEXTURE = Identifier.of("forever", "live2d/noir/preview.png");
   private static final Identifier MODEL_JSON = Identifier.of("forever", "live2d/noir/noir.model3.json");
   private static final Identifier MOC_FILE = Identifier.of("forever", "live2d/noir/noir.moc3");
   private static final Identifier MODEL_TEXTURE = Identifier.of("forever", "live2d/noir/noir.2048/texture_00.png");
   private static final float PREVIEW_ASPECT = 897.0F / 900.0F;
   private static final Set<String> HIDDEN_HAIR_DRAWABLES = Set.of("ArtMesh34", "ArtMesh35");

   private final SliderSetting size = this.add(new SliderSetting("Height", 220, 80, 600, 1));
   private final SliderSetting swayStrength = this.add(new SliderSetting("Sway", 3.0, 0.0, 3.0, 0.05));
   private final SliderSetting smoothing = this.add(new SliderSetting("Smoothing", 0.1, 0.02, 0.6, 0.01));
   private final BooleanSetting followPitch = this.add(new BooleanSetting("FollowPitch", true));
   private final EnumSetting<FormMode> form = this.add(new EnumSetting<>("Form", FormMode.Normal));
   private final BooleanSetting deathAni = this.add(new BooleanSetting("DealthAni", true));
   private final SliderSetting deathKeepTime = this.add(new SliderSetting("DealthKeepTime", 2.5, 0.1, 10.0, 0.1, this.deathAni::getValue).setSuffix("s"));
   private final BooleanSetting popAni = this.add(new BooleanSetting("PopAni", true));
   private final SliderSetting popKeepTime = this.add(new SliderSetting("PopKeepTime", 1.5, 0.1, 10.0, 0.1, this.popAni::getValue).setSuffix("s"));
   private final BooleanSetting selfPopAni = this.add(new BooleanSetting("SelfPopAni", true));
   private final SliderSetting selfPopKeepTime = this.add(new SliderSetting("SelfPopKeepTime", 1.5, 0.1, 10.0, 0.1, this.selfPopAni::getValue).setSuffix("s"));
   private final BooleanSetting showRuntimeHint = this.add(new BooleanSetting("RuntimeHint", true));

   private int hudX = 20;
   private int hudY = 140;
   private AlignMode alignMode = AlignMode.LEFT;

   private boolean firstFrame = true;
   private float lastYaw;
   private float lastPitch;
   private float angleX;
   private float angleY;
   private float angleZ;
   private float bodyAngleX;
   private float springX;
   private float springVelocityX;
   private float springY;
   private float springVelocityY;
   private float earSpring;
   private float earVelocity;
   private long nextBlinkAt = System.currentTimeMillis() + 1800L;
   private long blinkStartedAt = -1L;
   private long deathExpressionUntil;
   private long popExpressionUntil;
   private long selfPopExpressionUntil;
   private float tearsWeight;
   private float dizzyWeight;
   private float eyeCloseWeight;
   private float formWeight;
   private long lastExpressionUpdateMs = System.currentTimeMillis();
   private CubismNativeModel nativeModel;
   private CubismNativeRenderer nativeRenderer;
   private boolean nativeLoadAttempted;
   private String nativeStatus = "not loaded";

   public NoirLive2D() {
      super("NoirLive2D", Category.HUD);
      this.setChinese("NoirLive2D");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.firstFrame = true;
      this.lastExpressionUpdateMs = System.currentTimeMillis();
      loadNativeRuntime();
   }

   @Override
   public void onDisable() {
      if (this.nativeModel != null) {
         this.nativeModel.close();
         this.nativeModel = null;
      }
      this.nativeRenderer = null;
      this.nativeLoadAttempted = false;
      clearExpressionTimers();
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      renderElement(drawContext, tickDelta);
   }

   @Override
   public void renderElement(DrawContext context, float tickDelta) {
      if (mc.player == null || mc.world == null) {
         return;
      }
      loadNativeRuntime();

      updateTracking(tickDelta);

      int x = getElementX();
      int y = getElementY();
      int w = getElementWidth();
      int h = getElementHeight();

      float visualX = x + this.springX * 0.55F;
      float visualY = y + this.springY * 0.30F + idleBob();
      float rotation = this.angleZ * 0.22F;
      float scaleX = 1.0F + Math.abs(this.springX) * 0.0009F;
      float scaleY = 1.0F - Math.abs(this.springX) * 0.00045F;

      context.getMatrices().push();

      if (this.nativeModel != null && this.nativeRenderer != null) {
         context.getMatrices().translate(x, y, 0.0F);
         try {
            renderNative(context, w, h);
         } catch (Throwable throwable) {
            disableNativeRuntime(throwable);
            context.getMatrices().translate(-x, -y, 0.0F);
            context.getMatrices().translate(visualX + w / 2.0F, visualY + h / 2.0F, 0.0F);
            context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
            context.getMatrices().scale(scaleX, scaleY, 1.0F);
            context.getMatrices().translate(-w / 2.0F, -h / 2.0F, 0.0F);
            renderPreview(context, w, h);
         }
      } else {
         context.getMatrices().translate(visualX + w / 2.0F, visualY + h / 2.0F, 0.0F);
         context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
         context.getMatrices().scale(scaleX, scaleY, 1.0F);
         context.getMatrices().translate(-w / 2.0F, -h / 2.0F, 0.0F);
         renderPreview(context, w, h);
      }
      context.getMatrices().pop();

      if (this.showRuntimeHint.getValue() && this.nativeModel == null) {
         renderRuntimeHint(context, x, y, w, h);
      }
   }

   private void renderNative(DrawContext context, int w, int h) {
      applyIdleAnimation();
      applyExpressions();
      this.nativeModel.setParameter("ParamAngleX", this.angleX);
      this.nativeModel.setParameter("ParamAngleY", this.angleY);
      this.nativeModel.setParameter("ParamAngleZ", this.angleZ);
      this.nativeModel.setParameter("ParamBodyAngleX", this.bodyAngleX);
      this.nativeModel.update();
      this.nativeRenderer.setFlipV(true);
      this.nativeRenderer.setSkipMaskedDrawables(false);
      this.nativeRenderer.setFrontBangsFix(false);
      this.nativeRenderer.setFrontBangDrawables(Set.of());
      this.nativeRenderer.setHiddenDrawables(HIDDEN_HAIR_DRAWABLES);
      this.nativeRenderer.render(context, this.nativeModel, 0, 0, w, h, 1.0F);
   }

   private void applyIdleAnimation() {
      long now = System.currentTimeMillis();
      double t = now / 1000.0;

      float eyeOpen = 1.0F;
      if (this.blinkStartedAt < 0L && now >= this.nextBlinkAt) {
         this.blinkStartedAt = now;
      }
      if (this.blinkStartedAt >= 0L) {
         float p = MathHelper.clamp((now - this.blinkStartedAt) / 180.0F, 0.0F, 1.0F);
         eyeOpen = p < 0.5F ? 1.0F - p * 2.0F : (p - 0.5F) * 2.0F;
         if (p >= 1.0F) {
            this.blinkStartedAt = -1L;
            this.nextBlinkAt = now + 2200L + (long)(Math.sin(t * 1.7) * 500.0 + 650.0);
         }
      }

      float breath = (float)((Math.sin(t * 2.1) + 1.0) * 0.5);
      this.earVelocity += (this.angleZ * 0.55F + this.angleX * 0.18F - this.earSpring) * 0.16F;
      this.earVelocity *= 0.74F;
      this.earSpring += this.earVelocity;
      float ear = MathHelper.clamp(this.earSpring, -12.0F, 12.0F);
      float hairX = this.angleX * 0.09F + (float)Math.sin(t * 2.8) * 1.2F;
      float hairY = this.angleY * 0.08F + (float)Math.sin(t * 2.1 + 1.0) * 0.8F;

      this.nativeModel.setParameter("ParamEyeLOpen", eyeOpen);
      this.nativeModel.setParameter("ParamEyeROpen", eyeOpen);
      this.nativeModel.setParameter("ParamBreath", breath);
      this.nativeModel.setParameter("ParamEyeBallX", MathHelper.clamp(this.angleX / 30.0F, -1.0F, 1.0F));
      this.nativeModel.setParameter("ParamEyeBallY", MathHelper.clamp(this.angleY / 20.0F, -1.0F, 1.0F));

      this.nativeModel.setParameter("Param10", ear);
      this.nativeModel.setParameter("Param14", -ear);
      this.nativeModel.setParameter("Param11", ear * 0.35F);
      this.nativeModel.setParameter("Param12", ear * 0.20F);
      this.nativeModel.setParameter("Param15", -ear * 0.35F);
      this.nativeModel.setParameter("Param16", -ear * 0.20F);

      for (String id : new String[]{"Param20", "Param21", "Param24", "Param25", "Param26", "Param27", "Param28", "Param29", "Param30", "Param31", "Param32", "Param33"}) {
         this.nativeModel.setParameter(id, hairX);
      }
      for (String id : new String[]{"Param22", "Param23", "Param34", "Param35", "Param36", "Param37"}) {
         this.nativeModel.setParameter(id, hairY);
      }
   }

   private void applyExpressions() {
      long now = System.currentTimeMillis();
      float dt = Math.min(0.1F, Math.max(0.0F, (now - this.lastExpressionUpdateMs) / 1000.0F));
      this.lastExpressionUpdateMs = now;

      boolean deathActive = this.deathAni.getValue() && now < this.deathExpressionUntil;
      boolean selfPopActive = this.selfPopAni.getValue() && now < this.selfPopExpressionUntil;
      boolean popActive = this.popAni.getValue() && now < this.popExpressionUntil;

      if (deathActive) {
         selfPopActive = false;
         popActive = false;
      } else if (selfPopActive) {
         popActive = false;
      }

      this.tearsWeight = approachExpressionWeight(this.tearsWeight, deathActive ? 1.0F : 0.0F, dt, 0.12F, 0.20F);
      this.dizzyWeight = approachExpressionWeight(this.dizzyWeight, selfPopActive ? 1.0F : 0.0F, dt, 0.12F, 0.20F);
      this.eyeCloseWeight = approachExpressionWeight(this.eyeCloseWeight, popActive ? 1.0F : 0.0F, dt, 0.12F, 0.20F);
      this.formWeight = approachExpressionWeight(this.formWeight, this.form.getValue().target, dt, 0.10F, 0.20F);

      this.nativeModel.setParameter("Param17", this.tearsWeight);
      this.nativeModel.setParameter("ParamCheek", this.tearsWeight);
      this.nativeModel.setParameter("Param39", this.dizzyWeight);
      this.nativeModel.setParameter("Param40", -this.eyeCloseWeight);
      this.nativeModel.setParameter("Param41", this.formWeight);
   }

   private float approachExpressionWeight(float current, float target, float dt, float fadeIn, float fadeOut) {
      float fadeTime = target > current ? fadeIn : fadeOut;
      if (fadeTime <= 0.0F) {
         return target;
      }
      return lerp(current, target, MathHelper.clamp(dt / fadeTime, 0.0F, 1.0F));
   }

   @EventListener
   public void onDeath(DeathEvent event) {
      if (isSelf(event.getPlayer()) && this.deathAni.getValue()) {
         this.deathExpressionUntil = keepUntil(this.deathKeepTime);
      }
   }

   @EventListener
   public void onTotemPop(TotemEvent event) {
      if (isSelf(event.getPlayer())) {
         if (this.selfPopAni.getValue()) {
            this.selfPopExpressionUntil = keepUntil(this.selfPopKeepTime);
         }
      } else if (this.popAni.getValue()) {
         this.popExpressionUntil = keepUntil(this.popKeepTime);
      }
   }

   private long keepUntil(SliderSetting keepTime) {
      return System.currentTimeMillis() + Math.round(keepTime.getValue() * 1000.0);
   }

   private boolean isSelf(PlayerEntity player) {
      if (player == null || mc.player == null) {
         return false;
      }
      return player.getUuid().equals(mc.player.getUuid())
         || player.getName().getString().equalsIgnoreCase(mc.player.getName().getString());
   }

   private void clearExpressionTimers() {
      this.deathExpressionUntil = 0L;
      this.popExpressionUntil = 0L;
      this.selfPopExpressionUntil = 0L;
   }

   private void renderPreview(DrawContext context, int w, int h) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionTexProgram);
      context.drawTexture(PREVIEW_TEXTURE, 0, 0, 0, 0, w, h, w, h);
      RenderSystem.disableBlend();
   }

   private void loadNativeRuntime() {
      if (this.nativeLoadAttempted || this.nativeModel != null || mc == null || mc.getResourceManager() == null) {
         return;
      }
      this.nativeLoadAttempted = true;

      try {
         this.nativeModel = CubismNativeModel.load(mc, MOC_FILE, mc.runDirectory.toPath());
         this.nativeRenderer = new CubismNativeRenderer(MODEL_TEXTURE);
         this.nativeRenderer.preloadTextures(mc);
         this.nativeStatus = "native cubism loaded";
      } catch (Throwable throwable) {
         this.nativeStatus = throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
         this.nativeModel = null;
         this.nativeRenderer = null;
         System.err.println("[NoirLive2D] Cubism native runtime unavailable: " + this.nativeStatus);
      }
   }

   private void disableNativeRuntime(Throwable throwable) {
      this.nativeStatus = throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
      if (this.nativeModel != null) {
         this.nativeModel.close();
      }
      this.nativeModel = null;
      this.nativeRenderer = null;
      this.nativeLoadAttempted = true;
      System.err.println("[NoirLive2D] Cubism native renderer failed: " + this.nativeStatus);
   }

   private void updateTracking(float tickDelta) {
      float yaw = mc.player.getYaw();
      float pitch = mc.player.getPitch();

      if (this.firstFrame) {
         this.lastYaw = yaw;
         this.lastPitch = pitch;
         this.firstFrame = false;
         return;
      }

      float dt = Math.max(0.01F, tickDelta / 20.0F);
      float deltaYaw = MathHelper.wrapDegrees(yaw - this.lastYaw);
      float deltaPitch = pitch - this.lastPitch;
      this.lastYaw = yaw;
      this.lastPitch = pitch;

      float strength = this.swayStrength.getValueFloat();
      float targetX = MathHelper.clamp(-deltaYaw / dt * 0.018F * strength, -30.0F, 30.0F);
      float targetY = this.followPitch.getValue() ? MathHelper.clamp(-deltaPitch / dt * 0.014F * strength, -20.0F, 20.0F) : 0.0F;
      float targetZ = MathHelper.clamp(deltaYaw / dt * 0.010F * strength, -18.0F, 18.0F);

      float smooth = this.smoothing.getValueFloat();
      this.angleX = lerp(this.angleX, targetX, smooth);
      this.angleY = lerp(this.angleY, targetY, smooth);
      this.angleZ = lerp(this.angleZ, targetZ, smooth * 0.75F);
      this.bodyAngleX = lerp(this.bodyAngleX, this.angleX * 0.35F, smooth * 0.55F);

      this.springX = updateSpring(this.springX, this.angleX + this.bodyAngleX, true);
      this.springY = updateSpring(this.springY, this.angleY, false);
   }

   private float updateSpring(float value, float target, boolean horizontal) {
      float stiffness = horizontal ? 0.12F : 0.08F;
      float damping = horizontal ? 0.78F : 0.82F;
      if (horizontal) {
         this.springVelocityX += (target - value) * stiffness;
         this.springVelocityX *= damping;
         return value + this.springVelocityX;
      }

      this.springVelocityY += (target - value) * stiffness;
      this.springVelocityY *= damping;
      return value + this.springVelocityY;
   }

   private void renderRuntimeHint(DrawContext context, int x, int y, int w, int h) {
      int hintH = 28;
      int hintY = y + h - hintH - 3;
      Color bg = new Color(12, 12, 14, 125);
      Color fg = new Color(245, 245, 245, 210);
      Render2DUtil.drawRoundedRect(context.getMatrices(), x + 4, hintY, w - 8, hintH, 4.0F, bg);
      TextUtil.drawString(context, "Cubism native fallback", x + 10, hintY + 5, fg.getRGB(), false);
      TextUtil.drawString(context, this.nativeStatus, x + 10, hintY + 16, new Color(210, 210, 210, 170).getRGB(), false);
   }

   private float idleBob() {
      return (float)Math.sin(System.currentTimeMillis() / 650.0) * 1.5F;
   }

   private float lerp(float from, float to, float factor) {
      return from + (to - from) * MathHelper.clamp(factor, 0.0F, 1.0F);
   }

   public float getParamAngleX() {
      return this.angleX;
   }

   public float getParamAngleY() {
      return this.angleY;
   }

   public float getParamAngleZ() {
      return this.angleZ;
   }

   public float getParamBodyAngleX() {
      return this.bodyAngleX;
   }

   @Override
   public String getElementName() {
      return getChinese() != null ? getChinese() : getName();
   }

   @Override
   public int getElementX() {
      return this.alignMode == AlignMode.RIGHT ? this.hudX - getElementWidth() : this.hudX;
   }

   @Override
   public void setElementX(int x) {
      this.hudX = this.alignMode == AlignMode.RIGHT ? x + getElementWidth() : x;
   }

   @Override
   public int getElementY() {
      return this.hudY;
   }

   @Override
   public void setElementY(int y) {
      this.hudY = y;
   }

   @Override
   public int getElementWidth() {
      return Math.max(1, Math.round(getElementHeight() * PREVIEW_ASPECT));
   }

   @Override
   public int getElementHeight() {
      return this.size.getValueInt();
   }

   @Override
   public int getDragBoundsHeight() {
      return Math.max(1, getElementHeight() / 2);
   }

   @Override
   public boolean isElementVisible() {
      return isOn();
   }

   @Override
   public AlignMode getAlignMode() {
      return this.alignMode;
   }

   @Override
   public void setAlignMode(AlignMode mode) {
      if (mode == this.alignMode) {
         return;
      }
      if (mode == AlignMode.RIGHT && this.alignMode == AlignMode.LEFT) {
         this.hudX += getElementWidth();
      } else if (mode == AlignMode.LEFT && this.alignMode == AlignMode.RIGHT) {
         this.hudX -= getElementWidth();
      }
      this.alignMode = mode;
   }

   @Override
   public int getRightAnchorX() {
      return this.alignMode == AlignMode.RIGHT ? this.hudX : this.hudX + getElementWidth();
   }

   @Override
   public void setRightAnchorX(int rightX) {
      this.hudX = this.alignMode == AlignMode.RIGHT ? rightX : rightX - getElementWidth();
   }

   private enum FormMode {
      Normal(0.0F),
      White(1.0F);

      private final float target;

      FormMode(float target) {
         this.target = target;
      }
   }
}
