package dev.baimaodaxia.mod.modules.impl.client;

import dev.baimaodaxia.earthquake;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.Render2DEvent;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.Wrapper;
import dev.baimaodaxia.api.utils.math.Animation;
import dev.baimaodaxia.api.utils.math.Easing;
import dev.baimaodaxia.mod.gui.ClickGuiScreen;
import dev.baimaodaxia.mod.gui.items.Component;
import dev.baimaodaxia.mod.gui.items.Item;
import dev.baimaodaxia.mod.gui.items.buttons.Button;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.ColorSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.Objects;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.font.Font;
import net.minecraft.client.sound.Sound;
import net.minecraft.text.TextColor;
import net.minecraft.util.Colors;
public class ClickGui extends Module {
   private static ClickGui INSTANCE;
   public static String key = "";
   public final BooleanSetting autoSave = this.add(new BooleanSetting("AutoSave", true));
   public final BooleanSetting font = this.add(new BooleanSetting("Font", true));
   public final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", false));
   public final BooleanSetting sound = this.add((new BooleanSetting("Sound", true)).setParent());
   public final BooleanSetting guiSound = this.add(new BooleanSetting("GuiSound", true));
   public final SliderSetting soundPitch;
   public final SliderSetting buttonheight;
   public final SliderSetting textOffset;
   public final SliderSetting titleOffset;
   public final BooleanSetting fade;
   public final SliderSetting length;
   public final EnumSetting<Easing> easing;
   public final BooleanSetting blur;
   public final SliderSetting radius;
   public final BooleanSetting elements;
   public final BooleanSetting line;
   public final BooleanSetting gear;
   public final BooleanSetting text;
   public final BooleanSetting bounce;
   public final ColorSetting textColor;
   public final ColorSetting enableTextColor;
   public final BooleanSetting colors;
   public final ColorSetting color;
   public final ColorSetting bgEnable;
   public final ColorSetting bgColor;
   public final ColorSetting bgButton;
   public final ColorSetting backGround;
   public double alphaValue;
   private final Animation animation;
   private boolean paletteApplied;

   public ClickGui() {
      super("ClickGui", Module.Category.Client);
      BooleanSetting booleanSetting = this.sound;
      Objects.requireNonNull(booleanSetting);
      Objects.requireNonNull(booleanSetting);
      this.soundPitch = this.add(new SliderSetting("SoundPitch", (double)1.0F, (double)0.0F, (double)2.0F, 0.1, booleanSetting::isOpen));
      this.buttonheight = this.add(new SliderSetting("ButtonHeight", 0, 0, 7));
      this.textOffset = this.add(new SliderSetting("TextOffset", (double)0.0F, (double)-5.0F, (double)5.0F, (double)1.0F));
      this.titleOffset = this.add(new SliderSetting("TitleOffset", (double)0.0F, (double)-5.0F, (double)5.0F, (double)1.0F));
      this.fade = this.add((new BooleanSetting("Fade", true)).setParent());
      BooleanSetting booleanSetting2 = this.fade;
      Objects.requireNonNull(booleanSetting2);
      Objects.requireNonNull(booleanSetting2);
      this.length = this.add(new SliderSetting("Length", 126, 0, 1000, booleanSetting2::isOpen));
      Easing easing = Easing.SineInOut;
      BooleanSetting booleanSetting3 = this.fade;
      Objects.requireNonNull(booleanSetting3);
      Objects.requireNonNull(booleanSetting3);
      this.easing = this.add(new EnumSetting("Easing", easing, booleanSetting3::isOpen));
      this.blur = this.add((new BooleanSetting("Blur", true)).setParent());
      BooleanSetting booleanSetting4 = this.blur;
      Objects.requireNonNull(booleanSetting4);
      Objects.requireNonNull(booleanSetting4);
      this.radius = this.add(new SliderSetting("Radius", 58.4, (double)0.0F, (double)100.0F, booleanSetting4::isOpen));
      this.elements = this.add((new BooleanSetting("Elements", true)).setParent().injectTask(this::a));
      BooleanSetting booleanSetting5 = this.elements;
      Objects.requireNonNull(booleanSetting5);
      Objects.requireNonNull(booleanSetting5);
      this.line = this.add(new BooleanSetting("Line", true, booleanSetting5::isOpen));
      BooleanSetting booleanSetting6 = this.elements;
      Objects.requireNonNull(booleanSetting6);
      Objects.requireNonNull(booleanSetting6);
      this.gear = this.add(new BooleanSetting("Gear", false, booleanSetting6::isOpen));
      this.text = this.add((new BooleanSetting("Text", true)).setParent().injectTask(this::c));
      BooleanSetting booleanSetting7 = this.text;
      Objects.requireNonNull(booleanSetting7);
      Objects.requireNonNull(booleanSetting7);
      this.bounce = this.add(new BooleanSetting("Bounce", true, booleanSetting7::isOpen));
      Color color = new Color(-1, true);
      BooleanSetting booleanSetting8 = this.text;
      Objects.requireNonNull(booleanSetting8);
      Objects.requireNonNull(booleanSetting8);
      this.textColor = this.add(new ColorSetting("TextColor", color, booleanSetting8::isOpen));
      Color color2 = new Color(-6710785, true);
      BooleanSetting booleanSetting9 = this.text;
      Objects.requireNonNull(booleanSetting9);
      Objects.requireNonNull(booleanSetting9);
      this.enableTextColor = this.add(new ColorSetting("EnableColor", color2, booleanSetting9::isOpen));
      this.colors = this.add((new BooleanSetting("Colors", false)).setParent().injectTask(this::b));
      Color color3 = new Color(-11755874, true);
      BooleanSetting booleanSetting10 = this.colors;
      Objects.requireNonNull(booleanSetting10);
      Objects.requireNonNull(booleanSetting10);
      this.color = this.add(new ColorSetting("Color", color3, booleanSetting10::isOpen));
      Color color4 = new Color(60398079, true);
      BooleanSetting booleanSetting11 = this.colors;
      Objects.requireNonNull(booleanSetting11);
      Objects.requireNonNull(booleanSetting11);
      this.bgEnable = this.add(new ColorSetting("BgEnable", color4, booleanSetting11::isOpen));
      Color color5 = new Color(60398079, true);
      BooleanSetting booleanSetting12 = this.colors;
      Objects.requireNonNull(booleanSetting12);
      Objects.requireNonNull(booleanSetting12);
      this.bgColor = this.add(new ColorSetting("BgColor", color5, booleanSetting12::isOpen));
      Color color6 = new Color(852225, true);
      BooleanSetting booleanSetting13 = this.colors;
      Objects.requireNonNull(booleanSetting13);
      Objects.requireNonNull(booleanSetting13);
      this.bgButton = this.add(new ColorSetting("BgButton", color6, booleanSetting13::isOpen));
      Color color7 = new Color(1429089838, true);
      BooleanSetting booleanSetting14 = this.colors;
      Objects.requireNonNull(booleanSetting14);
      Objects.requireNonNull(booleanSetting14);
      this.backGround = this.add(new ColorSetting("BackGround", color7, booleanSetting14::isOpen));
      this.animation = new Animation();
      this.paletteApplied = false;
      this.setChinese("点击界面");
      INSTANCE = this;
      earthquake.EVENT_BUS.subscribe(new FadeOut());
   }

   public static ClickGui getInstance() {
      return INSTANCE;
   }

   public void a() {
      this.elements.setValueWithoutTask(false);
      this.elements.setOpen(!this.elements.isOpen());
   }

   public void b() {
      this.colors.setValueWithoutTask(false);
      this.colors.setOpen(!this.colors.isOpen());
   }

   public void c() {
      this.text.setValueWithoutTask(false);
      this.text.setOpen(!this.text.isOpen());
   }

   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         this.updateColor();
         if (this.guiSound.getValue() && mc.getSoundManager() != null) {
            mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, this.soundPitch.getValueFloat()));
         }

         for(Component component : ClickGuiScreen.getInstance().getComponents()) {
            component.setHeight(18);

            for(Item item : component.getItems()) {
               item.setHeight(10 + this.buttonheight.getValueInt());
            }
         }

         mc.setScreen(ClickGuiScreen.getInstance());
      }
   }

   public void onDisable() {
      if (mc.currentScreen instanceof ClickGuiScreen) {
         mc.currentScreen.close();
      }

      if (this.guiSound.getValue() && mc.getSoundManager() != null) {
         mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, this.soundPitch.getValueFloat()));
      }

      if (this.autoSave.getValue()) {
         earthquake.save();
      }

   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.updateColor();
      if (!(mc.currentScreen instanceof ClickGuiScreen)) {
         this.disable();
      }

   }

   public void updateColor() {
      Button.textColor = this.textColor.getValue().getRGB();
      Button.bgButton = this.bgButton.getValue().getRGB();
      Button.enableTextColor = this.enableTextColor.getValue().getRGB();
   }

   public class FadeOut {
      @EventListener(
         priority = -99999
      )
      public void onRender2D(Render2DEvent event) {
         if (!ClickGui.this.fade.getValue()) {
            ClickGui.this.alphaValue = (double)1.0F;
         } else {
            if (ClickGui.this.alphaValue > (double)0.0F || ClickGui.this.isOn()) {
               ClickGui.this.alphaValue = ClickGui.this.animation.get(ClickGui.this.isOn() ? (double)1.0F : (double)0.0F, (long)ClickGui.this.length.getValueInt(), ClickGui.this.easing.getValue());
            }

            if (ClickGui.this.alphaValue > (double)0.0F && !(Wrapper.mc.currentScreen instanceof ClickGuiScreen)) {
               event.drawContext.getMatrices().push();
               event.drawContext.getMatrices().translate(0.0F, 0.0F, 5000.0F);
               ClickGuiScreen.getInstance().render(event.drawContext, 0, 0, event.tickDelta);
               event.drawContext.getMatrices().pop();
            }

         }
      }
   }
}
