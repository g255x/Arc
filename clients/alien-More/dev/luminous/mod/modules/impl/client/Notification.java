package dev.luminous.mod.modules.impl.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.FadeUtils;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import net.minecraft.class_286;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_332;
import net.minecraft.class_3532;
import net.minecraft.class_4587;
import net.minecraft.class_757;
import net.minecraft.class_293.class_5596;
import org.joml.Matrix4f;

public class Notification extends Module {
   public static final ArrayList notifyList = new ArrayList();
   public static Notification INSTANCE;
   private final EnumSetting styles;
   public final EnumSetting type;
   public final EnumSetting mode;
   public final SliderSetting notifyX;
   private final SliderSetting notifyY;
   private final BooleanSetting font;
   private final SliderSetting radius;
   private final ColorSetting typeColor;
   private final ColorSetting fillColor;
   private final ColorSetting lineColor;

   public Notification() {
      super("Notification", Module.Category.Client);
      this.styles = this.add((new EnumSetting("Styles", Notification.Styles.Custom)).injectTask(this::applyStylePreset));
      this.type = this.add(new EnumSetting("Type", Notification.Mode.Chat));
      this.mode = this.add(new EnumSetting("Mode", Notification.Type.Fill));
      this.notifyX = this.add(new SliderSetting("notifyX", 256, 18, 550));
      this.notifyY = this.add(new SliderSetting("Y", 18, -50, 500));
      this.font = this.add(new BooleanSetting("Font", true));
      this.radius = this.add(new SliderSetting("Radius", (double)4.0F, (double)0.0F, (double)10.0F, (double)0.5F));
      this.typeColor = this.add(new ColorSetting("TypeColor", new Color(20, 20, 20, 100)));
      this.fillColor = this.add(new ColorSetting("FillColor", new Color(20, 20, 20, 100)));
      this.lineColor = this.add(new ColorSetting("LineColor", new Color(140, 140, 250, 225)));
      INSTANCE = this;
   }

   private void applyStylePreset() {
      switch (((Styles)this.styles.getValue()).ordinal()) {
         case 1:
            this.font.setValueWithoutTask(true);
            this.radius.setValue((double)6.0F);
            this.fillColor.setValue(new Color(20, 20, 20, 120));
            this.lineColor.setValue(new Color(140, 140, 250, 225));
            break;
         case 2:
            this.font.setValueWithoutTask(false);
            this.radius.setValue((double)0.0F);
            this.fillColor.setValue(new Color(0, 0, 0, 144));
            this.lineColor.setValue(new Color(140, 140, 250, 225));
            break;
         case 3:
            this.font.setValueWithoutTask(false);
            this.radius.setValue((double)0.0F);
            this.fillColor.setValue(new Color(0, 0, 0, 140));
            this.lineColor.setValue(new Color(255, 255, 255, 180));
            break;
         case 4:
            this.font.setValueWithoutTask(true);
            this.radius.setValue((double)4.0F);
            this.fillColor.setValue(new Color(0, 0, 0, 0));
            this.lineColor.setValue(new Color(140, 140, 250, 225));
      }

   }

   public static void push(String message) {
      if (message != null && !message.isEmpty() && INSTANCE != null && INSTANCE.isOn()) {
         Mode type = (Mode)INSTANCE.type.getValue();
         if (type == Notification.Mode.Chat || type == Notification.Mode.Both) {
            INSTANCE.sendMessage(message);
         }

         if (type != Notification.Mode.Chat) {
            notifyList.add(new NotificationInfo(message, "", Notification.Kind.INFO));
         }

      }
   }

   public static void pushToggle(String moduleName, boolean enabled) {
      if (moduleName != null && !moduleName.isEmpty() && INSTANCE != null && INSTANCE.isOn()) {
         String message = moduleName + " " + (enabled ? "Enabled" : "Disabled");
         Mode type = (Mode)INSTANCE.type.getValue();
         if (type == Notification.Mode.Chat || type == Notification.Mode.Both) {
            INSTANCE.sendMessage(message);
         }

         if (type != Notification.Mode.Chat) {
            notifyList.add(new NotificationInfo(moduleName, enabled ? "Enabled" : "Disabled", enabled ? Notification.Kind.SUCCESS : Notification.Kind.ERROR));
         }

      }
   }

   public static void pushPop(String player, int count) {
      if (player != null && !player.isEmpty() && INSTANCE != null && INSTANCE.isOn()) {
         String message = "popped " + count + " totems";
         if (count == 1) {
            message = "popped " + count + " totem";
         }

         Mode type = (Mode)INSTANCE.type.getValue();
         if (type == Notification.Mode.Chat || type == Notification.Mode.Both) {
            INSTANCE.sendMessage(player + " " + message);
         }

         if (type != Notification.Mode.Chat) {
            notifyList.add(new NotificationInfo(player, message, Notification.Kind.POP));
         }

      }
   }

   public static void pushKill(String player, String weapon) {
      if (player != null && !player.isEmpty() && INSTANCE != null && INSTANCE.isOn()) {
         String message = "was killed by you";
         if (weapon != null && !weapon.isEmpty()) {
            message = message + " using " + weapon;
         }

         Mode type = (Mode)INSTANCE.type.getValue();
         if (type == Notification.Mode.Chat || type == Notification.Mode.Both) {
            INSTANCE.sendMessage(player + " " + message);
         }

         if (type != Notification.Mode.Chat) {
            notifyList.add(new NotificationInfo(player, message, Notification.Kind.KILL));
         }

      }
   }

   public void onRender2D(class_332 drawContext, float tickDelta) {
      if (this.type.getValue() == Notification.Mode.Chat) {
         notifyList.clear();
      } else if (this.type.getValue() == Notification.Mode.CrossHair) {
         this.renderCrossHair(drawContext);
      } else if (this.styles.getValue() == Notification.Styles.Myau) {
         this.renderMyau(drawContext);
      } else {
         boolean empty = true;
         int y = (int)((double)(mc.method_22683().method_4502() - 21) - this.notifyY.getValue());
         int xAnchor = this.notifyX.getValueInt() + 500;
         float r = this.radius.getValueFloat();

         for(NotificationInfo info : notifyList) {
            if (info != null && info.first != null && info.firstFade != null && info.delayed >= 1) {
               empty = false;
               if (info.delayed < 5 && !info.end) {
                  info.end = true;
                  info.endFade.reset();
               }

               y = (int)((double)y - (double)18.0F * info.yFade.easeOutQuad());
               String text = info.first;
               int textWidth = this.font.getValue() ? (int)FontManager.ui.getWidth(text) : mc.field_1772.method_1727(text);
               int padX = 6;
               int iconW = 10;
               int totalW = padX + iconW + 4 + textWidth + padX;
               double x = info.delayed < 5 ? (double)xAnchor - (double)totalW * ((double)1.0F - info.endFade.easeOutQuad()) : (double)xAnchor - (double)totalW * info.firstFade.easeOutQuad();
               int ix = (int)x;
               Render2DUtil.drawRound(drawContext.method_51448(), ix, y, (float)totalW, 15.0F, r, this.fillColor.getValue());
               float iconT = (float)info.typeFrist.easeOutQuad();
               float iconScale = 0.6F + 0.4F * iconT;
               int iconAlpha = Math.max(0, Math.min(255, (int)(80.0F + 175.0F * iconT)));
               int iconRgb;
               if (info.kind == Notification.Kind.POP) {
                  iconRgb = (new Color(255, 215, 0, iconAlpha)).getRGB();
               } else if (info.kind == Notification.Kind.KILL) {
                  iconRgb = (new Color(170, 0, 255, iconAlpha)).getRGB();
               } else {
                  iconRgb = info.kind == Notification.Kind.ERROR ? (new Color(255, 60, 60, iconAlpha)).getRGB() : (new Color(255, 255, 255, iconAlpha)).getRGB();
               }

               drawContext.method_51448().method_22903();
               drawContext.method_51448().method_46416((float)(ix + padX), (float)(y + 3), 0.0F);
               drawContext.method_51448().method_22905(iconScale, iconScale, 1.0F);
               if (info.kind == Notification.Kind.ERROR) {
                  drawCrossIcon(drawContext.method_51448(), iconT, iconRgb);
               } else if (info.kind == Notification.Kind.POP) {
                  drawExclamationIcon(drawContext.method_51448(), iconT, iconRgb);
               } else if (info.kind == Notification.Kind.KILL) {
                  drawContext.method_51448().method_22905(1.2F, 1.2F, 1.0F);
                  TextUtil.drawString(drawContext, "ζ", (double)1.0F, (double)1.0F, iconRgb, false, false);
               } else {
                  drawCheckIcon(drawContext.method_51448(), iconT, iconRgb);
               }

               drawContext.method_51448().method_22909();
               TextUtil.drawString(drawContext, text, (double)(ix + padX + iconW + 4), (double)(4 + y), -1, this.font.getValue(), true);
               if (info.delayed < 5) {
                  y = (int)((double)y + (double)18.0F * info.yFade.easeOutQuad() - (double)18.0F * ((double)1.0F - info.endFade.easeOutQuad()));
               } else {
                  int barColor = this.lineColor.getValue().getRGB();
                  if (info.kind == Notification.Kind.POP) {
                     barColor = (new Color(255, 215, 0, 255)).getRGB();
                  } else if (info.kind == Notification.Kind.KILL) {
                     barColor = (new Color(170, 0, 255, 255)).getRGB();
                  }

                  Render2DUtil.drawRect(drawContext.method_51448(), (float)ix + 2.0F, (float)(y + 14), (float)((double)(totalW * (info.delayed - 4) - 2) / (double)62.0F), 1.0F, barColor);
               }
            }
         }

         if (empty) {
            notifyList.clear();
         }

      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      Iterator<NotificationInfo> it = notifyList.iterator();

      while(it.hasNext()) {
         NotificationInfo info = (NotificationInfo)it.next();
         if (info != null && info.first != null && info.firstFade != null) {
            --info.delayed;
            if (info.delayed < 1) {
               it.remove();
            }
         } else {
            it.remove();
         }
      }

   }

   public void onDisable() {
      notifyList.clear();
   }

   private void renderCrossHair(class_332 drawContext) {
      if (!notifyList.isEmpty()) {
         NotificationInfo info = (NotificationInfo)notifyList.get(notifyList.size() - 1);
         if (info != null && info.first != null && info.firstFade != null && info.delayed >= 1) {
            if (info.delayed < 5 && !info.end) {
               info.end = true;
               info.endFade.reset();
            }

            double anim = info.delayed < 5 ? (double)1.0F - info.endFade.easeOutQuad() : info.firstFade.easeOutQuad();
            float t = (float)anim;
            if (!(t <= 0.0F)) {
               int screenW = mc.method_22683().method_4486();
               int screenH = mc.method_22683().method_4502();
               String text = info.first;
               int textWidth = this.font.getValue() ? (int)FontManager.ui.getWidth(text) : mc.field_1772.method_1727(text);
               int padX = 6;
               int padY = 4;
               int w = padX * 2 + textWidth;
               int h = 12 + padY * 2;
               int x = screenW / 2 - w / 2;
               int y = screenH / 2 + 18;
               int bg = this.applyCrossHairOpacity(this.fillColor.getValue().getRGB(), t);
               int fg = this.applyCrossHairOpacity(-1, t);
               Render2DUtil.drawRound(drawContext.method_51448(), x, y, (float)w, (float)h, this.radius.getValueFloat(), new Color(bg, true));
               TextUtil.drawString(drawContext, text, (double)(x + padX), (double)(y + padY + 3), fg, this.font.getValue(), true);
            }
         }
      }
   }

   private int applyCrossHairOpacity(int argb, float opacity) {
      opacity = class_3532.method_15363(opacity, 0.0F, 1.0F);
      int a = (int)((float)(argb >> 24 & 255) * opacity);
      return a << 24 | argb & 16777215;
   }

   private void renderMyau(class_332 drawContext) {
      boolean empty = true;
      int screenW = mc.method_22683().method_4486();
      float targetY = (float)(mc.method_22683().method_4502() - 40 - this.notifyY.getValueInt());
      int rightPadding = this.notifyX.getValueInt();

      for(NotificationInfo info : notifyList) {
         if (info != null && info.first != null && info.firstFade != null && info.delayed >= 1) {
            empty = false;
            if (info.delayed < 5 && !info.end) {
               info.end = true;
               info.endFade.reset();
            }

            String title = info.title != null && !info.title.isEmpty() ? info.title : info.first;
            String message = info.message == null ? "" : info.message;
            float w = (float)Math.max((double)120.0F, (double)Math.max(mc.field_1772.method_1727(title), mc.field_1772.method_1727(message)) + (double)40.0F);
            float h = 30.0F;
            float animValue = info.delayed < 5 ? w * (1.0F - (float)info.endFade.easeOutQuad()) : w * (float)info.firstFade.easeOutQuad();
            float x = (float)(screenW - rightPadding) - animValue;
            if (info.renderY == 0.0F) {
               info.renderY = targetY;
            }

            info.renderY += (targetY - info.renderY) * 0.1F;
            float y = info.renderY;
            int barColor = this.getMyauBarColor(info.kind).getRGB();
            Render2DUtil.drawRect(drawContext.method_51448(), x + 2.0F, y + 2.0F, w, h, 1610612736);
            Render2DUtil.drawRect(drawContext.method_51448(), x, y, w, h, -1879048192);
            Render2DUtil.drawRect(drawContext.method_51448(), x, y, 2.0F, h, barColor);
            float iconT = (float)info.typeFrist.easeOutQuad();
            int iconAlpha = Math.max(0, Math.min(255, (int)(80.0F + 175.0F * iconT)));
            int iconRgb;
            if (info.kind == Notification.Kind.POP) {
               iconRgb = (new Color(255, 215, 0, iconAlpha)).getRGB();
            } else if (info.kind == Notification.Kind.KILL) {
               iconRgb = (new Color(170, 0, 255, iconAlpha)).getRGB();
            } else {
               iconRgb = info.kind == Notification.Kind.ERROR ? (new Color(255, 60, 60, iconAlpha)).getRGB() : (new Color(255, 255, 255, iconAlpha)).getRGB();
            }

            drawContext.method_51448().method_22903();
            drawContext.method_51448().method_46416(x + 5.0F, y + 7.0F, 0.0F);
            drawContext.method_51448().method_22905(1.6F, 1.6F, 1.0F);
            if (info.kind == Notification.Kind.ERROR) {
               drawCrossIcon(drawContext.method_51448(), iconT, iconRgb);
            } else if (info.kind == Notification.Kind.POP) {
               drawExclamationIcon(drawContext.method_51448(), iconT, iconRgb);
            } else if (info.kind == Notification.Kind.KILL) {
               drawContext.method_51448().method_22905(1.2F, 1.2F, 1.0F);
               TextUtil.drawString(drawContext, "ζ", (double)1.0F, (double)1.0F, iconRgb, false, false);
            } else {
               drawCheckIcon(drawContext.method_51448(), iconT, iconRgb);
            }

            drawContext.method_51448().method_22909();
            TextUtil.drawString(drawContext, title, (double)(x + 25.0F), (double)(y + 4.0F), -1, false, true);
            if (!message.isEmpty()) {
               TextUtil.drawString(drawContext, message, (double)(x + 25.0F), (double)(y + 15.0F), (new Color(170, 170, 170, 255)).getRGB(), false, true);
            }

            float barW = w * Math.max(0.0F, Math.min(1.0F, (float)info.delayed / 55.0F));
            Render2DUtil.drawRect(drawContext.method_51448(), x, y + h - 2.0F, barW, 2.0F, barColor);
            targetY -= h + 5.0F;
         }
      }

      if (empty) {
         notifyList.clear();
      }

   }

   private Color getMyauBarColor(Kind kind) {
      if (kind == Notification.Kind.POP) {
         return new Color(255, 215, 0, 255);
      } else if (kind == Notification.Kind.KILL) {
         return new Color(170, 0, 255, 255);
      } else if (kind == Notification.Kind.ERROR) {
         return new Color(255, 80, 80, 255);
      } else {
         return kind == Notification.Kind.SUCCESS ? new Color(120, 255, 120, 255) : this.lineColor.getValue();
      }
   }

   private static void drawCheckIcon(class_4587 matrices, float t, int color) {
      float x0 = 1.0F;
      float y0 = 6.0F;
      float x1 = 4.0F;
      float y1 = 9.0F;
      float x2 = 9.0F;
      float y2 = 2.0F;
      float dx1 = x1 - x0;
      float dy1 = y1 - y0;
      float dx2 = x2 - x1;
      float dy2 = y2 - y1;
      float l1 = (float)Math.sqrt((double)(dx1 * dx1 + dy1 * dy1));
      float l2 = (float)Math.sqrt((double)(dx2 * dx2 + dy2 * dy2));
      float p = Math.max(0.0F, Math.min(1.0F, t)) * (l1 + l2);
      float w = 1.6F;
      if (p <= l1) {
         float u = l1 <= 0.0F ? 1.0F : p / l1;
         drawThickLine(matrices, x0, y0, lerp(x0, x1, u), lerp(y0, y1, u), w, color);
      } else {
         drawThickLine(matrices, x0, y0, x1, y1, w, color);
         float p2 = p - l1;
         float u2 = l2 <= 0.0F ? 1.0F : Math.max(0.0F, Math.min(1.0F, p2 / l2));
         drawThickLine(matrices, x1, y1, lerp(x1, x2, u2), lerp(y1, y2, u2), w, color);
      }
   }

   private static void drawCrossIcon(class_4587 matrices, float t, int color) {
      float x0 = 2.0F;
      float y0 = 2.0F;
      float x1 = 8.0F;
      float y1 = 8.0F;
      float x2 = 8.0F;
      float y2 = 2.0F;
      float x3 = 2.0F;
      float y3 = 8.0F;
      float dx1 = x1 - x0;
      float dy1 = y1 - y0;
      float dx2 = x3 - x2;
      float dy2 = y3 - y2;
      float l1 = (float)Math.sqrt((double)(dx1 * dx1 + dy1 * dy1));
      float l2 = (float)Math.sqrt((double)(dx2 * dx2 + dy2 * dy2));
      float p = Math.max(0.0F, Math.min(1.0F, t)) * (l1 + l2);
      float w = 1.6F;
      if (p <= l1) {
         float u = l1 <= 0.0F ? 1.0F : p / l1;
         drawThickLine(matrices, x0, y0, lerp(x0, x1, u), lerp(y0, y1, u), w, color);
      } else {
         drawThickLine(matrices, x0, y0, x1, y1, w, color);
         float p2 = p - l1;
         float u2 = l2 <= 0.0F ? 1.0F : Math.max(0.0F, Math.min(1.0F, p2 / l2));
         drawThickLine(matrices, x2, y2, lerp(x2, x3, u2), lerp(y2, y3, u2), w, color);
      }
   }

   private static void drawExclamationIcon(class_4587 matrices, float t, int color) {
      float x = 5.5F;
      float yStart = 2.0F;
      float yEnd = 6.5F;
      float yDotStart = 8.0F;
      float yDotEnd = 9.0F;
      float w = 1.6F;
      float l1 = yEnd - yStart;
      float l2 = yDotEnd - yDotStart;
      float totalLen = l1 + l2;
      float p = Math.max(0.0F, Math.min(1.0F, t)) * totalLen;
      if (p <= l1) {
         float u = l1 <= 0.0F ? 1.0F : p / l1;
         drawThickLine(matrices, x, yStart, x, lerp(yStart, yEnd, u), w, color);
      } else {
         drawThickLine(matrices, x, yStart, x, yEnd, w, color);
         float p2 = p - l1;
         float u2 = l2 <= 0.0F ? 1.0F : Math.max(0.0F, Math.min(1.0F, p2 / l2));
         drawThickLine(matrices, x, yDotStart, x, lerp(yDotStart, yDotEnd, u2), w, color);
      }
   }

   private static float lerp(float a, float b, float t) {
      return a + (b - a) * t;
   }

   private static void drawThickLine(class_4587 matrices, float x0, float y0, float x1, float y1, float width, int color) {
      Matrix4f matrix = matrices.method_23760().method_23761();
      float dx = x1 - x0;
      float dy = y1 - y0;
      float len = (float)Math.sqrt((double)(dx * dx + dy * dy));
      if (!(len <= 1.0E-4F)) {
         float nx = -dy / len * width * 0.5F;
         float ny = dx / len * width * 0.5F;
         float ax = x0 + nx;
         float ay = y0 + ny;
         float bx = x0 - nx;
         float by = y0 - ny;
         float cx = x1 - nx;
         float cy = y1 - ny;
         float dxp = x1 + nx;
         float dyp = y1 + ny;
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.setShader(class_757::method_34540);
         class_287 bufferBuilder = class_289.method_1348().method_60827(class_5596.field_27382, class_290.field_1576);
         bufferBuilder.method_22918(matrix, ax, ay, 0.0F).method_39415(color);
         bufferBuilder.method_22918(matrix, bx, by, 0.0F).method_39415(color);
         bufferBuilder.method_22918(matrix, cx, cy, 0.0F).method_39415(color);
         bufferBuilder.method_22918(matrix, dxp, dyp, 0.0F).method_39415(color);
         class_286.method_43433(bufferBuilder.method_60800());
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   public static enum Type {
      Line,
      Fill;

      // $FF: synthetic method
      private static Type[] $values() {
         return new Type[]{Line, Fill};
      }
   }

   public static enum Mode {
      Notify,
      Chat,
      Both,
      CrossHair;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Notify, Chat, Both, CrossHair};
      }
   }

   public static enum Kind {
      SUCCESS,
      ERROR,
      INFO,
      POP,
      KILL;

      // $FF: synthetic method
      private static Kind[] $values() {
         return new Kind[]{SUCCESS, ERROR, INFO, POP, KILL};
      }
   }

   public static enum Styles {
      Custom,
      iOS,
      Myau,
      Classic,
      Minimal;

      // $FF: synthetic method
      private static Styles[] $values() {
         return new Styles[]{Custom, iOS, Myau, Classic, Minimal};
      }
   }

   public static class NotificationInfo {
      public final FadeUtils typeFrist;
      public final FadeUtils firstFade;
      public final FadeUtils endFade;
      public final FadeUtils yFade;
      public final String first;
      public final String title;
      public final String message;
      public final Kind kind;
      public int delayed;
      public boolean end;
      public float renderY;

      public NotificationInfo(String first, Kind kind) {
         this(first, "", kind);
      }

      public NotificationInfo(String title, String message, Kind kind) {
         this.typeFrist = new FadeUtils(500L);
         this.firstFade = new FadeUtils(500L);
         this.endFade = new FadeUtils(350L);
         this.yFade = new FadeUtils(500L);
         this.delayed = 55;
         this.title = title == null ? "" : title;
         this.message = message == null ? "" : message;
         this.first = this.message.isEmpty() ? this.title : this.title + " " + this.message;
         this.kind = kind;
         this.typeFrist.reset();
         this.firstFade.reset();
         this.yFade.reset();
         this.endFade.reset();
         this.end = false;
         this.renderY = 0.0F;
      }
   }
}
