package dev.luminous.mod.modules.impl.client;

import dev.luminous.Alien;
import dev.luminous.Nina;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.InitEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.render.ColorUtil;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import net.minecraft.class_332;
import net.minecraft.class_408;

public class ArrayListMod extends Module {
   public static ArrayListMod INSTANCE;
   public final EnumSetting page;
   public final BooleanSetting renderingUp;
   public final BooleanSetting font;
   public final BooleanSetting shadow;
   public final BooleanSetting lowerCase;
   public final SliderSetting xOffset;
   public final SliderSetting yOffset;
   public final SliderSetting textOffset;
   public final SliderSetting interval;
   public final SliderSetting enableLength;
   public final SliderSetting disableLength;
   public final SliderSetting fadeLength;
   public final EnumSetting easing;
   public final BooleanSetting listSort;
   public final ColorSetting color;
   private final EnumSetting colorMode;
   private final SliderSetting rainbowSpeed;
   private final SliderSetting saturation;
   private final SliderSetting rainbowDelay;
   private final ColorSetting endColor;
   private final SliderSetting pulseSpeed;
   private final SliderSetting pulseCounter;
   public final BooleanSetting blur;
   public final SliderSetting radius;
   private final BooleanSetting backGround;
   public final SliderSetting width;
   private final ColorSetting bgColor;
   private final ColorSetting rect;
   private final ColorSetting glow;
   private final ArrayList moduleList;

   public ArrayListMod() {
      super("ArrayList", Module.Category.Client);
      this.page = this.add(new EnumSetting("Page", ArrayListMod.Page.General));
      this.renderingUp = this.add(new BooleanSetting("RenderingUp", false, () -> this.page.is(ArrayListMod.Page.General)));
      this.font = this.add(new BooleanSetting("Font", true, () -> this.page.is(ArrayListMod.Page.General)));
      this.shadow = this.add(new BooleanSetting("Shadow", true, () -> this.page.is(ArrayListMod.Page.General)));
      this.lowerCase = this.add(new BooleanSetting("LowerCase", false, () -> this.page.is(ArrayListMod.Page.General)));
      this.xOffset = this.add(new SliderSetting("XOffset", (double)0.0F, (double)0.0F, (double)50.0F, 0.1, () -> this.page.is(ArrayListMod.Page.General)));
      this.yOffset = this.add(new SliderSetting("YOffset", (double)0.0F, (double)0.0F, (double)50.0F, 0.1, () -> this.page.is(ArrayListMod.Page.General)));
      this.textOffset = this.add(new SliderSetting("TextOffset", (double)0.0F, (double)-10.0F, (double)10.0F, 0.1, () -> this.page.is(ArrayListMod.Page.General)));
      this.interval = this.add(new SliderSetting("Interval", (double)0.0F, (double)0.0F, (double)15.0F, 0.1, () -> this.page.is(ArrayListMod.Page.General)));
      this.enableLength = this.add(new SliderSetting("EnableLength", 200, 0, 1000, () -> this.page.is(ArrayListMod.Page.General)));
      this.disableLength = this.add(new SliderSetting("DisableLength", 200, 0, 1000, () -> this.page.is(ArrayListMod.Page.General)));
      this.fadeLength = this.add(new SliderSetting("FadeLength", 200, 0, 1000, () -> this.page.is(ArrayListMod.Page.General)));
      this.easing = this.add(new EnumSetting("Easing", Easing.CircInOut, () -> this.page.is(ArrayListMod.Page.General)));
      this.listSort = this.add(new BooleanSetting("ListSort", true, () -> this.page.is(ArrayListMod.Page.Element)));
      this.color = this.add(new ColorSetting("Color", new Color(208, 0, 0), () -> this.page.is(ArrayListMod.Page.Color)));
      this.colorMode = this.add(new EnumSetting("ColorMode", ArrayListMod.ColorMode.Pulse, () -> this.page.is(ArrayListMod.Page.Color)));
      this.rainbowSpeed = this.add(new SliderSetting("RainbowSpeed", (double)4.0F, (double)1.0F, (double)10.0F, 0.1, () -> this.page.is(ArrayListMod.Page.Color) && this.colorMode.getValue() == ArrayListMod.ColorMode.Rainbow));
      this.saturation = this.add(new SliderSetting("Saturation", (double)130.0F, (double)1.0F, (double)255.0F, () -> this.page.is(ArrayListMod.Page.Color) && this.colorMode.getValue() == ArrayListMod.ColorMode.Rainbow));
      this.rainbowDelay = this.add(new SliderSetting("Delay", 350, 0, 1000, () -> this.page.is(ArrayListMod.Page.Color) && this.colorMode.getValue() == ArrayListMod.ColorMode.Rainbow));
      this.endColor = this.add((new ColorSetting("SecondColor", new Color(255, 0, 0, 255), () -> this.page.is(ArrayListMod.Page.Color) && this.colorMode.getValue() == ArrayListMod.ColorMode.Pulse)).injectBoolean(true));
      this.pulseSpeed = this.add(new SliderSetting("PulseSpeed", (double)1.0F, (double)0.0F, (double)5.0F, 0.1, () -> this.page.is(ArrayListMod.Page.Color)));
      this.pulseCounter = this.add(new SliderSetting("Counter", 10, 1, 50, () -> this.page.is(ArrayListMod.Page.Color)));
      this.blur = this.add((new BooleanSetting("Blur", false, () -> this.page.is(ArrayListMod.Page.Color))).setParent());
      this.radius = this.add(new SliderSetting("Radius", (double)10.0F, (double)0.0F, (double)100.0F, () -> this.page.is(ArrayListMod.Page.Color) && this.blur.isOpen()));
      this.backGround = this.add((new BooleanSetting("BackGround", false, () -> this.page.is(ArrayListMod.Page.Color))).setParent());
      this.width = this.add(new SliderSetting("Width", (double)0.0F, (double)0.0F, (double)15.0F, () -> this.page.is(ArrayListMod.Page.Color) && this.backGround.isOpen()));
      this.bgColor = this.add(new ColorSetting("BGColor", new Color(0, 0, 0, 100), () -> this.page.is(ArrayListMod.Page.Color) && this.backGround.isOpen()));
      this.rect = this.add((new ColorSetting("Rect", new Color(208, 0, 0), () -> this.page.is(ArrayListMod.Page.Color))).injectBoolean(false));
      this.glow = this.add((new ColorSetting("Glow", new Color(208, 0, 100), () -> this.page.is(ArrayListMod.Page.Color))).injectBoolean(false));
      this.moduleList = new ArrayList();
      this.setChinese("功能列表");
      INSTANCE = this;
      Nina.EVENT_BUS.subscribe((Object)(new InitHandler()));
   }

   public void onRender2D(class_332 drawContext, float tickDelta) {
      ArrayListMod.Info.onRender(drawContext, this.moduleList, !this.renderingUp.getValue());
   }

   @EventListener(
      priority = -999
   )
   public void onUpdate(ClientTickEvent event) {
      if (!nullCheck()) {
         if (event.isPost()) {
            ArrayListMod.Info.onUpdate(this.moduleList, this.listSort.getValue());
         }

      }
   }

   private int getWidth(String s) {
      if (this.lowerCase.getValue()) {
         s = s.toLowerCase();
      }

      return this.font.getValue() ? (int)FontManager.ui.getWidth(s) : Wrapper.mc.field_1772.method_1727(s);
   }

   private int getHeight() {
      if (this.font.getValue()) {
         return (int)FontManager.ui.getFontHeight();
      } else {
         Objects.requireNonNull(Wrapper.mc.field_1772);
         return 9;
      }
   }

   public int getColor(double counter) {
      return this.colorMode.getValue() != ArrayListMod.ColorMode.Custom ? this.rainbow(counter).getRGB() : this.color.getValue().getRGB();
   }

   private Color rainbow(double delay) {
      if (this.colorMode.getValue() == ArrayListMod.ColorMode.Pulse) {
         return this.endColor.booleanValue ? ColorUtil.pulseColor(this.color.getValue(), this.endColor.getValue(), delay, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue()) : ColorUtil.pulseColor(this.color.getValue(), delay, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue());
      } else if (this.colorMode.getValue() == ArrayListMod.ColorMode.Rainbow) {
         double rainbowState = Math.ceil(((double)System.currentTimeMillis() * this.rainbowSpeed.getValue() + delay * this.rainbowDelay.getValue()) / (double)20.0F);
         return Color.getHSBColor((float)(rainbowState % (double)360.0F / (double)360.0F), this.saturation.getValueFloat() / 255.0F, 1.0F);
      } else {
         return this.color.getValue();
      }
   }

   private int getFontHeight() {
      if (this.font.getValue()) {
         return (int)FontManager.ui.getFontHeight();
      } else {
         Objects.requireNonNull(Wrapper.mc.field_1772);
         return 9;
      }
   }

   public static enum Page {
      General,
      Element,
      Color;

      // $FF: synthetic method
      private static Page[] $values() {
         return new Page[]{General, Element, Color};
      }
   }

   private static enum ColorMode {
      Custom,
      Pulse,
      Rainbow;

      // $FF: synthetic method
      private static ColorMode[] $values() {
         return new ColorMode[]{Custom, Pulse, Rainbow};
      }
   }

   public class InitHandler {
      @EventListener
      public void onInit(InitEvent event) {
         for(Module module : Alien.MODULE.getModules()) {
            ArrayList var10000 = ArrayListMod.this.moduleList;
            ArrayListMod var10003 = ArrayListMod.this;
            Objects.requireNonNull(module);
            var10000.add(var10003.new Info(module::getArrayName, () -> module.isOn() && module.drawn.getValue()));
         }

         Alien.EVENT_BUS.unsubscribe((Object)this);
      }
   }

   public class Info {
      public final Callable info;
      public String string;
      public final BooleanSupplier drawn;
      public double currentX = (double)0.0F;
      public boolean isOn;
      public final Animation animation = new Animation();
      public final Animation fadeAnimation = new Animation();
      static double fontHeight;
      static double currentY;
      static int windowWidth;
      static boolean fromUp;
      static double counter;

      public Info(Callable info, BooleanSupplier drawn) {
         this.info = info;
         this.drawn = drawn;

         try {
            this.string = (String)this.info.call();
         } catch (Exception var5) {
         }

      }

      public static void onRender(class_332 context, List list, boolean fromUp) {
         counter = (double)20.0F;
         ArrayListMod.Info.fromUp = fromUp;
         fontHeight = (double)ArrayListMod.INSTANCE.getFontHeight();
         currentY = fromUp ? (double)1.0F + ArrayListMod.INSTANCE.yOffset.getValue() : (double)Wrapper.mc.method_22683().method_4502() - fontHeight - (double)1.0F - (Wrapper.mc.field_1755 instanceof class_408 && ArrayListMod.INSTANCE.yOffset.getValue() < (double)12.0F ? (double)12.0F - ArrayListMod.INSTANCE.yOffset.getValue() + ArrayListMod.INSTANCE.interval.getValue() / (double)2.0F : (double)0.0F) - ArrayListMod.INSTANCE.yOffset.getValue();
         windowWidth = Wrapper.mc.method_22683().method_4486();

         for(Info s : list) {
            s.draw(context);
         }

      }

      public static void onUpdate(List list, boolean sort) {
         for(Info s : list) {
            s.onUpdate();
         }

         if (sort) {
            list.sort(Comparator.comparingInt((info) -> info.string == null ? 0 : -ArrayListMod.INSTANCE.getWidth(info.string)));
         }

      }

      public void onUpdate() {
         this.isOn = this.drawn.getAsBoolean();
         if (this.isOn) {
            try {
               this.string = ArrayListMod.this.lowerCase.getValue() ? ((String)this.info.call()).toLowerCase() : (String)this.info.call();
            } catch (Exception e) {
               e.printStackTrace();
            }
         }

      }

      public void draw(class_332 context) {
         if (this.currentX > (double)0.0F || this.isOn) {
            this.currentX = this.animation.get(this.isOn ? (double)(ArrayListMod.this.getWidth(this.string) + 1) : (double)0.0F, this.isOn ? (long)ArrayListMod.this.enableLength.getValueInt() : (long)ArrayListMod.this.disableLength.getValueInt(), (Easing)ArrayListMod.this.easing.getValue());
            double width = this.currentX + (double)ArrayListMod.this.xOffset.getValueFloat();
            double fade = this.fadeAnimation.get(this.isOn ? (double)1.0F : (double)0.0F, (long)ArrayListMod.this.fadeLength.getValueInt(), (Easing)ArrayListMod.this.easing.getValue());
            if (fade > 0.04) {
               int c = ColorUtil.injectAlpha(ArrayListMod.this.getColor(counter += fromUp ? fade : -fade), (int)((double)ArrayListMod.this.color.getValue().getAlpha() * fade));
               if (ArrayListMod.this.blur.getValue()) {
                  Nina.BLUR.applyBlur((float)(ArrayListMod.this.radius.getValue() * fade), (float)((double)windowWidth - width - (double)(ArrayListMod.this.width.getValueFloat() / 2.0F)), (float)currentY - 1.0F - ArrayListMod.this.interval.getValueFloat() / 2.0F, (float)width + ArrayListMod.this.width.getValueFloat() - ArrayListMod.this.xOffset.getValueFloat(), (float)fontHeight + ArrayListMod.this.interval.getValueFloat());
               }

               if (ArrayListMod.this.backGround.getValue()) {
                  Render2DUtil.drawRect(context.method_51448(), (float)((double)windowWidth - width - (double)(ArrayListMod.this.width.getValueFloat() / 2.0F)), (float)currentY - 1.0F - ArrayListMod.this.interval.getValueFloat() / 2.0F, (float)width + ArrayListMod.this.width.getValueFloat() - ArrayListMod.this.xOffset.getValueFloat(), (float)fontHeight + ArrayListMod.this.interval.getValueFloat(), ColorUtil.injectAlpha(ArrayListMod.this.bgColor.sync ? c : ArrayListMod.this.bgColor.getValue().getRGB(), (int)((double)ArrayListMod.this.bgColor.getValue().getAlpha() * fade)));
               }

               if (ArrayListMod.this.glow.booleanValue) {
                  Render2DUtil.drawGlow(context.method_51448(), (float)((double)windowWidth - width - (double)(ArrayListMod.this.width.getValueFloat() / 2.0F)), (float)currentY - 1.0F - ArrayListMod.this.interval.getValueFloat() / 2.0F, (float)width + ArrayListMod.this.width.getValueFloat() - ArrayListMod.this.xOffset.getValueFloat(), (float)fontHeight + ArrayListMod.this.interval.getValueFloat(), ColorUtil.injectAlpha(ArrayListMod.this.glow.sync ? c : ArrayListMod.this.glow.getValue().getRGB(), (int)((double)ArrayListMod.this.glow.getValue().getAlpha() * fade)));
               }

               TextUtil.drawString(context, this.string, (double)windowWidth - width, currentY + (double)ArrayListMod.this.textOffset.getValueFloat(), c, ArrayListMod.this.font.getValue(), ArrayListMod.this.shadow.getValue());
               if (ArrayListMod.this.rect.booleanValue) {
                  Render2DUtil.drawRect(context.method_51448(), (float)windowWidth + ArrayListMod.this.width.getValueFloat() / 2.0F - ArrayListMod.this.xOffset.getValueFloat(), (float)currentY - 1.0F - ArrayListMod.this.interval.getValueFloat() / 2.0F, 1.0F, (float)fontHeight + ArrayListMod.this.interval.getValueFloat(), ArrayListMod.this.rect.sync ? c : ColorUtil.injectAlpha(ArrayListMod.this.rect.getValue(), (int)((double)ArrayListMod.this.rect.getValue().getAlpha() * fade)).getRGB());
               }

               currentY += fromUp ? (fontHeight + ArrayListMod.this.interval.getValue()) * fade : -(fontHeight + ArrayListMod.this.interval.getValue()) * fade;
            }
         }

      }
   }
}
