package dev.g255.arc.mod.modules.impl.hud;

import dev.g255.arc.api.utils.i18n.LangUtil;
import dev.g255.arc.api.utils.math.AnimateUtil;
import dev.g255.arc.api.utils.math.Easing;
import dev.g255.arc.api.utils.render.Render2DUtil;
import dev.g255.arc.core.impl.FontManager;
import dev.g255.arc.mod.gui.HudElement;
import dev.g255.arc.mod.gui.fonts.FontRenderer;
import dev.g255.arc.mod.gui.clickgui.ClickGuiScreen;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public final class NotificationHud extends Module implements HudElement {
   public static NotificationHud INSTANCE;
   private static final HashMap<Integer, FontRenderer> fontCache = new HashMap();
   private final SliderSetting scale = this.add(new SliderSetting("Scale", (double)1.0F, (double)0.5F, (double)2.0F, 0.05));
   private final SliderSetting x = this.add(new SliderSetting("X", 10, 0, 1920));
   private final SliderSetting y = this.add(new SliderSetting("Y", 10, 0, 1080));
   private final ColorSetting enableColor = this.add(new ColorSetting("Enable Color", new Color(48, 226, 178, 255)));
   private final ColorSetting disableColor = this.add(new ColorSetting("Disable Color", new Color(220, 50, 50, 255)));
   private final ColorSetting infoColor = this.add(new ColorSetting("Info Color", new Color(99, 150, 145, 255)));
   private final ColorSetting bgColor = this.add(new ColorSetting("Bg Color", new Color(20, 20, 20, 180)));
   private final ColorSetting titleColor = this.add(new ColorSetting("Title Color", new Color(255, 255, 255, 255)));
   private final ColorSetting msgColor = this.add(new ColorSetting("Msg Color", new Color(181, 181, 181, 255)));
   private final SliderSetting textOffset = this.add(new SliderSetting("Text Offset", (double)0.0F, (double)-10.0F, (double)10.0F, (double)1.0F));
   private final List<Notification> notifications = new CopyOnWriteArrayList();

   public NotificationHud() {
      super("NotificationHud", "Module toggle message notification HUD", Module.Category.HUD);
      this.setChinese("通知消息");
      INSTANCE = this;
      this.enable();
   }

   private static float getCardWidth() {
      return 170.0F * INSTANCE.scale.getValueFloat();
   }

   private static float getCardHeight() {
      return 36.0F * INSTANCE.scale.getValueFloat();
   }

   private static float getAccentWidth() {
      return 2.0F * INSTANCE.scale.getValueFloat();
   }

   private static float getPaddingLeft() {
      return 10.0F * INSTANCE.scale.getValueFloat();
   }

   private static float getLineGap() {
      return 2.0F * INSTANCE.scale.getValueFloat();
   }

   private static float getCardSpacing() {
      return 4.0F * INSTANCE.scale.getValueFloat();
   }

   private static float getTitleFontSize() {
      return 8.5F * INSTANCE.scale.getValueFloat();
   }

   private static float getMsgFontSize() {
      return 9.0F * INSTANCE.scale.getValueFloat();
   }

   private static FontRenderer getNotificationFont(float f) {
      int n = Math.round(f);
      FontRenderer fontRenderer = (FontRenderer)fontCache.get(n);
      if (fontRenderer == null) {
         try {
            fontRenderer = FontManager.assets(f, "default", 0, "msyh");
         } catch (Exception var4) {
            fontRenderer = FontManager.ui;
         }

         fontCache.put(n, fontRenderer);
      }

      return fontRenderer;
   }

   public static void send(String string, String string2, NotificationType notificationType) {
      if (INSTANCE != null && INSTANCE.isOn()) {
         INSTANCE.notifications.add(new Notification(string, string2, notificationType));

         while(INSTANCE.notifications.size() > 12) {
            INSTANCE.notifications.remove(0);
         }

      }
   }

   public static void onToggle(Module module, boolean bl) {
      if (INSTANCE != null && INSTANCE.isOn() && !Module.nullCheck()) {
         String var10000 = module.getCategory().name();
         String var10001 = module.getDisplayName();
         send(var10000, var10001 + (bl ? " Enabled" : " Disabled"), bl ? NotificationHud.NotificationType.ENABLE : NotificationHud.NotificationType.DISABLE);
      }
   }

   public void onRender2D(DrawContext ServerConfigException, float f) {
      if (!this.notifications.isEmpty() && !ClickGuiScreen.isEditingHud()) {
         this.renderAt(ServerConfigException);
      }
   }

   private void renderAt(DrawContext ServerConfigException) {
      float f2 = (float)this.y.getValueInt();

      for(Notification notification2 : this.notifications) {
         notification2.updateY(f2);
         if (!notification2.isDead) {
            f2 += (getCardHeight() + getCardSpacing()) * notification2.getHeightProgress();
         }
      }

      for(Notification notification2 : this.notifications) {
         notification2.render(ServerConfigException);
      }

      this.notifications.removeIf((notification) -> notification.isDead);
   }

   public int getX() {
      return this.x.getValueInt();
   }

   public void setX(int nx) {
      this.x.setValue((double)nx);
   }

   public int getY() {
      return this.y.getValueInt();
   }

   public void setY(int ny) {
      this.y.setValue((double)ny);
   }

   public int getWidth() {
      return Math.max(1, Math.round(getCardWidth()));
   }

   public int getHeight() {
      return Math.max(1, Math.round(getCardHeight()));
   }

   public boolean isVisible() {
      return this.isOn();
   }

   public void render(DrawContext context, float tickDelta) {
      if (this.notifications.isEmpty()) {
         Notification demo = new Notification("Demo", LangUtil.isChinese() ? "示例通知 · 通知 HUD" : "Sample notification · Notification HUD", NotificationHud.NotificationType.INFO);
         demo.renderStage3(context, this.x.getValueInt(), this.y.getValueInt());
      } else {
         this.renderAt(context);
      }
   }

   private static class Notification {
      private final String title;
      private final String message;
      private final NotificationType type;
      private final long startTime;
      private float renderY = -1.0F;
      private float heightProgress = 1.0F;
      public boolean isDead = false;

      public Notification(String string, String string2, NotificationType notificationType) {
         this.title = string;
         this.message = string2;
         this.type = notificationType;
         this.startTime = System.currentTimeMillis();
      }

      public float getHeightProgress() {
         return this.heightProgress;
      }

      public void updateY(float f) {
         if (this.renderY < 0.0F) {
            this.renderY = f;
         }

         this.renderY = AnimateUtil.fast(f, this.renderY, 16.0F);
      }

      public void render(DrawContext ServerConfigException) {
         if (NotificationHud.INSTANCE != null) {
            long l = System.currentTimeMillis() - this.startTime;
            long l2 = 300L;
            long l3 = 500L;
            long l4 = 1500L;
            long l5 = l4 + 200L;
            long l6 = l4 + 200L + 300L;
            if (l > l6) {
               this.isDead = true;
            } else {
               int n = NotificationHud.INSTANCE.x.getValueInt();
               int n2 = Math.round(this.renderY);
               if (l <= l2) {
                  float f2 = (float)l / 300.0F;
                  float f;
                  this.heightProgress = f = (float)Easing.CubicOut.ease((double)f2);
                  this.renderStage1(ServerConfigException, n, n2, f);
               } else if (l <= l3) {
                  float f = (float)(l - l2) / 200.0F;
                  float f3 = (float)Easing.CubicOut.ease((double)f);
                  this.heightProgress = 1.0F;
                  this.renderStage2(ServerConfigException, n, n2, f3);
               } else if (l <= l4) {
                  this.heightProgress = 1.0F;
                  this.renderStage3(ServerConfigException, n, n2);
               } else if (l <= l5) {
                  float f = (float)(l - l4) / 200.0F;
                  float f4 = (float)Easing.CubicOut.ease((double)f);
                  this.heightProgress = 1.0F;
                  this.renderStage2(ServerConfigException, n, n2, 1.0F - f4);
               } else {
                  float f = (float)(l - l5) / 300.0F;
                  float f5 = (float)Easing.CubicOut.ease((double)f);
                  this.heightProgress = 1.0F - f5;
                  this.renderStage1(ServerConfigException, n, n2, 1.0F - f5);
               }

            }
         }
      }

      private void renderStage1(DrawContext ServerConfigException, int n, int n2, float f) {
         Color color = this.getAccentColor();
         MatrixStack matrixStack = ServerConfigException.getMatrices();
         float f2 = NotificationHud.getCardWidth();
         float f3 = NotificationHud.getCardHeight();
         float f4 = f2 * f;
         Render2DUtil.drawRect(matrixStack, (float)n, (float)n2, f4, f3, color);
      }

      private void renderStage2(DrawContext ServerConfigException, int n, int n2, float f) {
         Color color = NotificationHud.INSTANCE.bgColor.getValue();
         Color color2 = this.getAccentColor();
         MatrixStack matrixStack = ServerConfigException.getMatrices();
         float f2 = NotificationHud.getCardWidth();
         float f3 = NotificationHud.getCardHeight();
         float f4 = NotificationHud.getAccentWidth();
         Render2DUtil.drawRect(matrixStack, (float)n, (float)n2, f2, f3, color);
         int n3 = (int)(255.0F * f);
         if (n3 > 4) {
            this.drawNotifText(ServerConfigException, n, n2, n3);
         }

         float f5 = (float)n + (f2 - f4) * f;
         float f6 = f2 - (f5 - (float)n);
         Render2DUtil.drawRect(matrixStack, f5, (float)n2, f6, f3, color2);
      }

      private void renderStage3(DrawContext ServerConfigException, int n, int n2) {
         Color color = NotificationHud.INSTANCE.bgColor.getValue();
         Color color2 = this.getAccentColor();
         MatrixStack matrixStack = ServerConfigException.getMatrices();
         float f = NotificationHud.getCardWidth();
         float f2 = NotificationHud.getCardHeight();
         float f3 = NotificationHud.getAccentWidth();
         Render2DUtil.drawRect(matrixStack, (float)n, (float)n2, f, f2, color);
         Render2DUtil.drawRect(matrixStack, (float)n + f - f3, (float)n2, f3, f2, color2);
         this.drawNotifText(ServerConfigException, n, n2, 255);
      }

      private void drawNotifText(DrawContext ServerConfigException, int n, int n2, int n3) {
         int n4 = NotificationHud.INSTANCE.textOffset.getValueInt();
         float f = NotificationHud.getTitleFontSize();
         float f2 = NotificationHud.getMsgFontSize();
         FontRenderer fontRenderer = NotificationHud.getNotificationFont(f);
         FontRenderer fontRenderer2 = NotificationHud.getNotificationFont(f2);
         float f3 = fontRenderer.getFontHeight("A");
         float f4 = fontRenderer2.getFontHeight("A");
         float f5 = NotificationHud.getLineGap();
         float f6 = f3 + f5 + f4;
         float f7 = NotificationHud.getCardHeight();
         float f8 = NotificationHud.getPaddingLeft();
         float f9 = (float)n2 + (f7 - f6) * 0.5F + (float)n4;
         float f10 = (float)n + f8;
         Color color = NotificationHud.INSTANCE.titleColor.getValue();
         Color color2 = NotificationHud.INSTANCE.msgColor.getValue();
         float f11 = (float)n3 / 255.0F * ((float)color.getAlpha() / 255.0F);
         float f12 = (float)n3 / 255.0F * ((float)color2.getAlpha() / 255.0F);
         MatrixStack matrixStack = ServerConfigException.getMatrices();
         fontRenderer.drawString(matrixStack, this.title, f10, (float)Math.round(f9), (float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, f11, false);
         fontRenderer2.drawString(matrixStack, this.message, f10, (float)Math.round(f9 + f3 + f5), (float)color2.getRed() / 255.0F, (float)color2.getGreen() / 255.0F, (float)color2.getBlue() / 255.0F, f12, false);
      }

      private Color getAccentColor() {
         if (NotificationHud.INSTANCE == null) {
            return new Color(100, 200, 100);
         } else {
            switch (this.type.ordinal()) {
               case 0 -> {
                  return NotificationHud.INSTANCE.enableColor.getValue();
               }
               case 1 -> {
                  return NotificationHud.INSTANCE.disableColor.getValue();
               }
               default -> {
                  return NotificationHud.INSTANCE.infoColor.getValue();
               }
            }
         }
      }
   }

   public static enum NotificationType {
      ENABLE,
      DISABLE,
      INFO;

      // $FF: synthetic method
      private static NotificationType[] $values() {
         return new NotificationType[]{ENABLE, DISABLE, INFO};
      }
   }
}
