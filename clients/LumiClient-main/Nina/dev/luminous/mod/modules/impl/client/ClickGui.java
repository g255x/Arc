package dev.luminous.mod.modules.impl.client;

import dev.luminous.Alien;
import dev.luminous.Nina;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.Render2DEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.items.Component;
import dev.luminous.mod.gui.items.Item;
import dev.luminous.mod.gui.items.buttons.Button;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.Objects;
import net.minecraft.class_1109;
import net.minecraft.class_3417;
import net.minecraft.class_437;

public class ClickGui extends Module {
   private static ClickGui INSTANCE;
   public final EnumSetting<Style> style;
   public final BooleanSetting autoSave;
   public final BooleanSetting font;
   public final BooleanSetting shadow;
   public final BooleanSetting disableNotification;
   public final BooleanSetting sound;
   public final BooleanSetting guiSound;
   public final SliderSetting soundPitch;
   public final SliderSetting height;
   public final SliderSetting textOffset;
   public final SliderSetting titleOffset;
   public final SliderSetting alpha;
   public final SliderSetting hoverAlpha;
   public final SliderSetting topAlpha;
   public final SliderSetting backgroundAlpha;
   public final BooleanSetting fade;
   public final SliderSetting length;
   public final EnumSetting<Easing> easing;
   public final SliderSetting scale;
   public final SliderSetting scrollSpeed;
   public final BooleanSetting blur;
   public final SliderSetting radius;
   public final BooleanSetting descriptions;
   public final BooleanSetting elements;
   public final BooleanSetting line;
   public final ColorSetting gear;
   public final BooleanSetting colors;
   public final ColorSetting color;
   public final ColorSetting activeColor;
   public final ColorSetting hoverColor;
   public final ColorSetting defaultColor;
   public final ColorSetting defaultTextColor;
   public final ColorSetting enableTextColor;
   public final ColorSetting backGround;
   public final ColorSetting tint;
   public final ColorSetting endColor;
   public double alphaValue;
   public double openValue;
   private final Animation animation;
   private final Animation openAnimation;
   private final Animation alphaAnimation;
   public static String key = "";
   private boolean styleApplied;

   public ClickGui() {
      super("ClickGui", Module.Category.Client);
      this.style = this.add((new EnumSetting("Style", ClickGui.Style.Dark)).injectTask(this::updateStyle));
      this.autoSave = this.add(new BooleanSetting("AutoSave", true));
      this.font = this.add(new BooleanSetting("Font", false));
      this.shadow = this.add(new BooleanSetting("Shadow", true));
      this.disableNotification = this.add(new BooleanSetting("DisableNotification", true));
      this.sound = this.add((new BooleanSetting("Sound", true)).setParent());
      this.guiSound = this.add(new BooleanSetting("GuiSound", true));
      BooleanSetting var10009 = this.sound;
      Objects.requireNonNull(var10009);
      this.soundPitch = this.add(new SliderSetting("SoundPitch", 0.2, (double)0.0F, (double)2.0F, 0.1, var10009::isOpen));
      this.height = this.add(new SliderSetting("Height", 3, 0, 7));
      this.textOffset = this.add(new SliderSetting("TextOffset", (double)-2.0F, (double)-5.0F, (double)5.0F, (double)1.0F));
      this.titleOffset = this.add(new SliderSetting("TitleOffset", (double)-1.0F, (double)-5.0F, (double)5.0F, (double)1.0F));
      this.alpha = this.add(new SliderSetting("Alpha", 180, 0, 255));
      this.hoverAlpha = this.add(new SliderSetting("HoverAlpha", 240, 0, 255));
      this.topAlpha = this.add(new SliderSetting("TopAlpha", 240, 0, 255));
      this.backgroundAlpha = this.add(new SliderSetting("BackgroundAlpha", 187, 0, 255));
      this.fade = this.add((new BooleanSetting("Fade", true)).setParent());
      BooleanSetting var10008 = this.fade;
      Objects.requireNonNull(var10008);
      this.length = this.add(new SliderSetting("Length", 250, 0, 1000, var10008::isOpen));
      Easing var10005 = Easing.Expo;
      BooleanSetting var10006 = this.fade;
      Objects.requireNonNull(var10006);
      this.easing = this.add(new EnumSetting("Easing", var10005, var10006::isOpen));
      this.scale = this.add(new SliderSetting("Scale", (double)1.0F, (double)0.5F, (double)3.0F, 0.05));
      this.scrollSpeed = this.add(new SliderSetting("ScrollSpeed", 30, 5, 100));
      this.blur = this.add((new BooleanSetting("Blur", false)).setParent());
      var10008 = this.blur;
      Objects.requireNonNull(var10008);
      this.radius = this.add(new SliderSetting("Radius", (double)10.0F, (double)0.0F, (double)100.0F, var10008::isOpen));
      this.descriptions = this.add(new BooleanSetting("Descriptions", true));
      this.elements = this.add((new BooleanSetting("Elements", false)).setParent().injectTask(this::keyCodec));
      var10006 = this.elements;
      Objects.requireNonNull(var10006);
      this.line = this.add(new BooleanSetting("Line", true, var10006::isOpen));
      var10006 = this.elements;
      Objects.requireNonNull(var10006);
      this.gear = this.add((new ColorSetting("Gear", -1, var10006::isOpen)).injectBoolean(false));
      this.colors = this.add((new BooleanSetting("Colors", false)).setParent().injectTask(this::elementCodec));
      Color var1 = new Color(-4087809, true);
      var10006 = this.colors;
      Objects.requireNonNull(var10006);
      this.color = this.add(new ColorSetting("Color", var1, var10006::isOpen));
      var1 = new Color(-4087809, true);
      var10006 = this.colors;
      Objects.requireNonNull(var10006);
      this.activeColor = this.add(new ColorSetting("ActiveColor", var1, var10006::isOpen));
      var1 = new Color(3813448, true);
      var10006 = this.colors;
      Objects.requireNonNull(var10006);
      this.hoverColor = this.add(new ColorSetting("HoverColor", var1, var10006::isOpen));
      var1 = new Color(1446428, true);
      var10006 = this.colors;
      Objects.requireNonNull(var10006);
      this.defaultColor = this.add(new ColorSetting("DefaultColor", var1, var10006::isOpen));
      var1 = new Color(-2041104, true);
      var10006 = this.colors;
      Objects.requireNonNull(var10006);
      this.defaultTextColor = this.add(new ColorSetting("DefaultTextColor", var1, var10006::isOpen));
      var1 = new Color(255, 255, 255);
      var10006 = this.colors;
      Objects.requireNonNull(var10006);
      this.enableTextColor = this.add(new ColorSetting("EnableTextColor", var1, var10006::isOpen));
      var1 = new Color(-1441656808, true);
      var10006 = this.colors;
      Objects.requireNonNull(var10006);
      this.backGround = this.add((new ColorSetting("BackGround", var1, var10006::isOpen)).injectBoolean(true));
      this.tint = this.add((new ColorSetting("Tint", new Color(1191116800, true))).injectBoolean(false));
      this.endColor = this.add(new ColorSetting("End", new Color(1014528209, true), () -> this.tint.booleanValue));
      this.animation = new Animation();
      this.openAnimation = new Animation();
      this.alphaAnimation = new Animation();
      this.styleApplied = false;
      this.setChinese("点击界面");
      INSTANCE = this;
      Nina.EVENT_BUS.subscribe(new FadeOut());
   }

   public static ClickGui getInstance() {
      return INSTANCE;
   }

   public void keyCodec() {
      this.elements.setValueWithoutTask(false);
      this.elements.setOpen(!this.elements.isOpen());
   }

   public void elementCodec() {
      this.colors.setValueWithoutTask(false);
      this.colors.setOpen(!this.colors.isOpen());
   }

   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         if (!this.styleApplied) {
            this.updateStyle();
            this.styleApplied = true;
         }

         this.updateColor();
         if (this.guiSound.getValue() && mc.method_1483() != null) {
            mc.method_1483().method_4873(class_1109.method_47978(class_3417.field_15015, this.soundPitch.getValueFloat()));
         }

         class_437 screen = (class_437)(this.style.getValue() == ClickGui.Style.Shoreline ? ClickGuiScreen.ShorelineScreen.getInstance() : ClickGuiScreen.getInstance());
         if (screen instanceof ClickGuiScreen) {
            ClickGuiScreen clickGuiScreen = (ClickGuiScreen)screen;
            this.applySavedLayout("ClickGuiScreen", clickGuiScreen.getComponents());

            for(Component component : clickGuiScreen.getComponents()) {
               component.setHeight(18);

               for(Item item : component.getItems()) {
                  item.setHeight(10 + this.height.getValueInt());
               }
            }
         } else if (screen instanceof ClickGuiScreen.ShorelineScreen) {
            ClickGuiScreen.ShorelineScreen shorelineScreen = (ClickGuiScreen.ShorelineScreen)screen;
            this.applySavedLayout("ShorelineScreen", shorelineScreen.getComponents());

            for(Component component : shorelineScreen.getComponents()) {
               component.setHeight(18);

               for(Item item : component.getItems()) {
                  item.setHeight(10 + this.height.getValueInt());
               }
            }
         }

         mc.method_1507(screen);
      }
   }

   private void applySavedLayout(String screenKey, Iterable<Component> components) {
      for(Component component : components) {
         String base = screenKey + "_" + component.getName();
         component.setX(Alien.CONFIG.getInt(base + "_x", component.getX()));
         component.setY(Alien.CONFIG.getInt(base + "_y", component.getY()));
         component.setOpen(Alien.CONFIG.getBoolean(base + "_open", component.isOpen()));
      }

   }

   public void onDisable() {
      if (mc.field_1755 instanceof ClickGuiScreen || mc.field_1755 instanceof ClickGuiScreen.ShorelineScreen) {
         mc.field_1755.method_25419();
      }

      if (this.guiSound.getValue() && mc.method_1483() != null) {
         mc.method_1483().method_4873(class_1109.method_47978(class_3417.field_15015, this.soundPitch.getValueFloat()));
      }

      if (this.autoSave.getValue()) {
         Nina.save();
      }

   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!this.styleApplied) {
         this.updateStyle();
         this.styleApplied = true;
      }

      this.updateColor();
      if (!(mc.field_1755 instanceof ClickGuiScreen) && !(mc.field_1755 instanceof ClickGuiScreen.ShorelineScreen)) {
         this.disable();
      }

   }

   public void updateColor() {
      Button.hoverColor = this.hoverColor.getValue().getRGB();
      Button.defaultTextColor = this.defaultTextColor.getValue().getRGB();
      Button.defaultColor = this.defaultColor.getValue().getRGB();
      Button.enableTextColor = this.enableTextColor.getValue().getRGB();
   }

   public void updateStyle() {
      switch (((Style)this.style.getValue()).ordinal()) {
         case 0:
            this.color.setSync(false);
            this.activeColor.setSync(false);
            this.color.setValue(new Color(-4087809, true));
            this.activeColor.setValue(new Color(-4087809, true));
            this.hoverColor.setValue(new Color(3813448, true));
            this.defaultColor.setValue(new Color(1446428, true));
            this.defaultTextColor.setValue(new Color(-2041104, true));
            this.enableTextColor.setValue(new Color(255, 255, 255));
            this.backGround.setValue(new Color(-1441656808, true));
            this.tint.setValue(new Color(1191116800, true));
            this.endColor.setValue(new Color(1019771135, true));
            break;
         case 1:
            this.color.setSync(false);
            this.activeColor.setSync(false);
            this.color.setValue(new Color(-4087809, true));
            this.activeColor.setValue(new Color(-4087809, true));
            this.hoverColor.setValue(new Color(3813448, true));
            this.defaultColor.setValue(new Color(1446428, true));
            this.defaultTextColor.setValue(new Color(-2041104, true));
            this.enableTextColor.setValue(new Color(255, 255, 255));
            this.backGround.setValue(new Color(-1441656808, true));
            this.tint.setValue(new Color(1191116800, true));
            this.endColor.setValue(new Color(1014528209, true));
            break;
         case 2:
            this.color.setSync(true);
            this.activeColor.setSync(true);
            this.hoverColor.setValue(new Color(45, 45, 45, 200));
            this.defaultColor.setValue(new Color(20, 20, 20, 236));
            this.defaultTextColor.setValue(new Color(210, 210, 210));
            this.enableTextColor.setValue(new Color(255, 255, 255));
            this.backGround.setValue(new Color(18, 18, 18, 236));
            this.tint.setValue(new Color(0, 0, 0, 0));
            this.endColor.setValue(new Color(0, 0, 0, 0));
            this.scale.setValue((double)1.0F);
            this.scrollSpeed.setValue((double)30.0F);
            this.descriptions.setValue(true);
      }

   }

   public static enum Style {
      Default,
      Dark,
      Shoreline;

      // $FF: synthetic method
      private static Style[] $values() {
         return new Style[]{Default, Dark, Shoreline};
      }
   }

   public class FadeOut {
      @EventListener(
         priority = -99999
      )
      public void onRender2D(Render2DEvent event) {
         if (ClickGui.this.fade.getValue()) {
            if (ClickGui.this.style.getValue() == ClickGui.Style.Shoreline) {
               if (ClickGui.this.alphaValue > (double)0.0F || ClickGui.this.isOn()) {
                  ClickGui.this.openValue = ClickGui.this.openAnimation.get(ClickGui.this.isOn() ? (double)1.0F : (double)0.0F, 400L, Easing.BackOut);
                  ClickGui.this.alphaValue = ClickGui.this.alphaAnimation.get(ClickGui.this.isOn() ? (double)1.0F : (double)0.0F, 300L, Easing.CubicInOut);
               }
            } else if (ClickGui.this.alphaValue > (double)0.0F || ClickGui.this.isOn()) {
               ClickGui.this.alphaValue = ClickGui.this.animation.get(ClickGui.this.isOn() ? (double)1.0F : (double)0.0F, (long)ClickGui.this.length.getValueInt(), ClickGui.this.easing.getValue());
               ClickGui.this.openValue = ClickGui.this.alphaValue;
            }

            if (ClickGui.this.alphaValue > (double)0.0F && ClickGui.this.style.getValue() == ClickGui.Style.Shoreline && !(Wrapper.mc.field_1755 instanceof ClickGuiScreen.ShorelineScreen)) {
               event.drawContext.method_51448().method_22903();
               event.drawContext.method_51448().method_46416(0.0F, 0.0F, 5000.0F);
               ClickGuiScreen.ShorelineScreen.getInstance().method_25394(event.drawContext, 0, 0, event.tickDelta);
               event.drawContext.method_51448().method_22909();
            }

            if (ClickGui.this.alphaValue > (double)0.0F && ClickGui.this.style.getValue() != ClickGui.Style.Shoreline && !(Wrapper.mc.field_1755 instanceof ClickGuiScreen)) {
               event.drawContext.method_51448().method_22903();
               event.drawContext.method_51448().method_46416(0.0F, 0.0F, 5000.0F);
               ClickGuiScreen.getInstance().method_25394(event.drawContext, 0, 0, event.tickDelta);
               event.drawContext.method_51448().method_22909();
            }
         } else {
            ClickGui.this.alphaValue = (double)1.0F;
            ClickGui.this.openValue = (double)1.0F;
         }

      }
   }
}
