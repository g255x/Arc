package dev.kissoo.mod.modules.impl.client;

import dev.kissoo.mod.gui.ModernGuiScreen;
import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.Render2DEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.Wrapper;
import dev.kissoo.auth.AuthFail;
import dev.kissoo.auth.KeyAuth;
import dev.kissoo.api.utils.math.Animation;
import dev.kissoo.api.utils.math.Easing;
import hoprc.api.core.impl.ShaderManager;
import dev.kissoo.core.impl.ShaderParams;
import dev.kissoo.mod.gui.ClickGuiScreen;
import dev.kissoo.mod.gui.items.Component;
import dev.kissoo.mod.gui.items.Item;
import dev.kissoo.mod.gui.items.buttons.Button;
import dev.kissoo.mod.gui.TrollClickGuiScreen;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

import java.awt.Color;
import java.util.function.BooleanSupplier;

public class ClickGui extends Module {
   private static ClickGui INSTANCE;
   private boolean p = false;

   public enum Theme { Old, Modern, Troll }

   public enum FontSize {
      Small(6.0f),
      Medium(8.0f),
      Large(10.0f),
      Super(12.0f);

      private final float size;
      FontSize(float size) {
         this.size = size;
      }
      public float getSize() {
         return size;
      }
   }

   public final EnumSetting<Theme> theme = this.add(new EnumSetting<>("Theme", Theme.Modern).injectTask(() -> {
      if (mc.currentScreen != null) {
         if (this.theme.getValue() == Theme.Modern) {
            mc.setScreen(new ModernGuiScreen(this));
         } else if (this.theme.getValue() == Theme.Troll) {
            mc.setScreen(new TrollClickGuiScreen(this));
         } else {
            for (Component component : ClickGuiScreen.getInstance().getComponents()) {
               component.setHeight(18);
               for (Item item : component.getItems()) {
                  item.setHeight(10 + this.height.getValueInt());
               }
            }
            mc.setScreen(ClickGuiScreen.getInstance());
         }
      }
   }));

   public final SliderSetting modernScale = this.add(new SliderSetting("ModernScale", 1.0, 0.5, 2.0, 0.05, () -> this.theme.getValue() == Theme.Modern));
   public final EnumSetting<FontSize> modernFontSize = this.add(new EnumSetting<>("FontSize", FontSize.Medium, () -> this.theme.getValue() == Theme.Modern));

   public final ColorSetting modernSidebarColor = this.add(new ColorSetting("SidebarColor", new Color(20, 20, 22, 220), () -> this.theme.getValue() == Theme.Modern));
   public final ColorSetting modernMainColor = this.add(new ColorSetting("MainColor", new Color(14, 14, 16, 200), () -> this.theme.getValue() == Theme.Modern));

   private final BooleanSupplier isOld = () -> this.theme.getValue() == Theme.Old;

   public final BooleanSetting autoSave = this.add(new BooleanSetting("AutoSave", true));

   public final BooleanSetting font                = this.add(new BooleanSetting("Font", true));
   public final BooleanSetting shadow              = this.add(new BooleanSetting("Shadow", true, isOld));
   public final BooleanSetting sound      = this.add(new BooleanSetting("Sound", true, isOld).setParent());
   public final BooleanSetting guiSound   = this.add(new BooleanSetting("GuiSound", true, isOld));
   public final SliderSetting  soundPitch = this.add(new SliderSetting("SoundPitch", 1.0, 0.0, 2.0, 0.1, () -> isOld.getAsBoolean() && this.sound.isOpen()));

   public final SliderSetting maxHeight   = this.add(new SliderSetting("MaxHeight",  250.0, 100.0, 800.0, 10.0, isOld));
   public final SliderSetting height      = this.add(new SliderSetting("Height",     2,     0,     7, isOld));
   public final SliderSetting textOffset  = this.add(new SliderSetting("TextOffset", -1.0, -5.0,  5.0, 1.0, isOld));
   public final SliderSetting titleOffset = this.add(new SliderSetting("TitleOffset",-2.0, -5.0,  5.0, 1.0, isOld));

   public final BooleanSetting fade   = this.add(new BooleanSetting("Fade", true, isOld).setParent());
   public final SliderSetting  length = this.add(new SliderSetting("Length",  103, 0, 1000, () -> isOld.getAsBoolean() && this.fade.isOpen()));
   public final EnumSetting<Easing> easing = this.add(new EnumSetting<>("Easing", Easing.QuadInOut, () -> isOld.getAsBoolean() && this.fade.isOpen()));

   public final BooleanSetting blur   = this.add(new BooleanSetting("Blur", true, isOld).setParent());
   public final SliderSetting  radius = this.add(new SliderSetting("Radius", 44.6, 0.0, 100.0, () -> isOld.getAsBoolean() && this.blur.isOpen()));

   public final BooleanSetting elements = this.add(new BooleanSetting("Elements", false, isOld).setParent().injectTask(this::a));
   public final BooleanSetting line = this.add(new BooleanSetting("Line", true, () -> isOld.getAsBoolean() && this.elements.isOpen()));
   public final ColorSetting gear = this.add(new ColorSetting("Gear", new Color(-945057058, true), () -> isOld.getAsBoolean() && this.elements.isOpen()).injectBoolean(true));
   public final ColorSetting bind = this.add(new ColorSetting("Bind", new Color(255, 255, 255, 255), () -> isOld.getAsBoolean() && this.elements.isOpen()).injectBoolean(false));

   public final BooleanSetting colors = this.add(new BooleanSetting("Colors", false, isOld).setParent().injectTask(this::b));
   public final ColorSetting topBarColor      = this.add(new ColorSetting("TopBarColor",      new Color(30, 30, 40, 163),   () -> isOld.getAsBoolean() && this.colors.isOpen()));
   public final ColorSetting enableBGColor    = this.add(new ColorSetting("EnableBGColor",    new Color(10583275, true),    () -> isOld.getAsBoolean() && this.colors.isOpen()));
   public final ColorSetting hoverBGColor     = this.add(new ColorSetting("HoverBGColor",     new Color(1968328527, true),  () -> isOld.getAsBoolean() && this.colors.isOpen()));
   public final ColorSetting defaultBGColor   = this.add(new ColorSetting("DefaultBGColor",   new Color(399617519, true),   () -> isOld.getAsBoolean() && this.colors.isOpen()));
   public final ColorSetting defaultTextColor = this.add(new ColorSetting("DefaultTextColor", new Color(-438970923, true),  () -> isOld.getAsBoolean() && this.colors.isOpen()));
   public final ColorSetting enableTextColor  = this.add(new ColorSetting("EnableTextColor",  new Color(-493194264, true),  () -> isOld.getAsBoolean() && this.colors.isOpen()));
   public final ColorSetting panelBGColor     = this.add(new ColorSetting("PanelBGColor",     new Color(16114916, true),    () -> isOld.getAsBoolean() && this.colors.isOpen()).injectBoolean(false));

   public final ColorSetting tintColor    = this.add(new ColorSetting("TintColor",    new Color(1436124399, true), isOld).injectBoolean(true));
   public final ColorSetting tintEndColor = this.add(new ColorSetting("TintEndColor", new Color(1342374659, true), () -> isOld.getAsBoolean() && this.tintColor.booleanValue));
   public final ColorSetting lineColor    = this.add(new ColorSetting("LineColor",    new Color(1869829355, true), () -> isOld.getAsBoolean() && this.elements.isOpen()));

   public final BooleanSetting toggleShader = this.add(new BooleanSetting("ToggleShader", false, isOld).setParent());
   public final EnumSetting<ShaderManager.Shader> shaderMode = this.add(new EnumSetting<>("ShaderMode", ShaderManager.Shader.Flow, () -> isOld.getAsBoolean() && this.toggleShader.isOpen()));
   public final ColorSetting guiShaderColor = this.add(new ColorSetting("ShaderColor", new Color(255, 255, 255, 180), () -> isOld.getAsBoolean() && this.toggleShader.isOpen()));
   public final SliderSetting guiShaderSpeed = this.add(new SliderSetting("ShaderSpeed", 4.0, 0.0, 20.0, 0.1, () -> isOld.getAsBoolean() && this.toggleShader.isOpen()));
   public final SliderSetting guiShaderRadius = this.add(new SliderSetting("ShaderRadius", 2.0, 0.0, 6.0, () -> isOld.getAsBoolean() && this.toggleShader.isOpen()));
   public final SliderSetting guiShaderSmoothness = this.add(new SliderSetting("ShaderSmoothness", 1.0, 0.0, 1.0, 0.01, () -> isOld.getAsBoolean() && this.toggleShader.isOpen()));
   public final SliderSetting guiShaderMaxSample = this.add(new SliderSetting("ShaderMaxSample", 10.0, 0.0, 20.0, () -> isOld.getAsBoolean() && this.toggleShader.isOpen()));
   public final SliderSetting guiShaderDivider = this.add(new SliderSetting("ShaderDivider", 150.0, 0.0, 300.0, () -> isOld.getAsBoolean() && this.toggleShader.isOpen()));
   public final SliderSetting guiShaderAlpha = this.add(new SliderSetting("ShaderAlpha", 255, 0, 255, () -> isOld.getAsBoolean() && this.toggleShader.isOpen()));
   public final ColorSetting guiShaderPulseColor = this.add(new ColorSetting("ShaderPulse", new Color(255, 255, 255), () -> isOld.getAsBoolean() && this.toggleShader.isOpen()));
   public final SliderSetting guiShaderPulseSpeed = this.add(new SliderSetting("ShaderPulseSize", 10.0, 0.0, 200.0, 0.1, () -> isOld.getAsBoolean() && this.toggleShader.isOpen()));
   public final ColorSetting guiShaderSmoke1 = this.add(new ColorSetting("ShaderColor1", new Color(255, 255, 255), () -> isOld.getAsBoolean() && this.toggleShader.isOpen()));
   public final ColorSetting guiShaderSmoke2 = this.add(new ColorSetting("ShaderColor2", new Color(255, 255, 255), () -> isOld.getAsBoolean() && this.toggleShader.isOpen()));
   public final ColorSetting guiShaderSmoke3 = this.add(new ColorSetting("ShaderColor3", new Color(255, 255, 255), () -> isOld.getAsBoolean() && this.toggleShader.isOpen()));
   public final ColorSetting guiShaderSmoke4 = this.add(new ColorSetting("ShaderColor4", new Color(255, 255, 255), () -> isOld.getAsBoolean() && this.toggleShader.isOpen()));
   public final SliderSetting guiShaderStep = this.add(new SliderSetting("ShaderStep", 0.5, 0.0, 4.0, 0.01F, () -> isOld.getAsBoolean() && this.toggleShader.isOpen()));
   public final SliderSetting guiShaderOctaves = this.add(new SliderSetting("ShaderOctaves", 10, 5, 30, () -> isOld.getAsBoolean() && this.toggleShader.isOpen()));

   public final BooleanSetting image       = this.add(new BooleanSetting("Image", false, isOld).setParent());
   public final EnumSetting<ImageMode> imageMode = this.add(new EnumSetting<>("ImageMode", ImageMode.biao1, () -> isOld.getAsBoolean() && this.image.isOpen()));
   public final SliderSetting imageScale   = this.add(new SliderSetting("ImageScale",   160.0, 50.0, 400.0, 1.0, () -> isOld.getAsBoolean() && this.image.isOpen()));
   public final SliderSetting imageOffsetX = this.add(new SliderSetting("ImageOffsetX",  60.0, 0.0,  600.0, 1.0, () -> isOld.getAsBoolean() && this.image.isOpen()));
   public final SliderSetting imageOffsetY = this.add(new SliderSetting("ImageOffsetY",  60.0, 0.0,  600.0, 1.0, () -> isOld.getAsBoolean() && this.image.isOpen()));

   public double alphaValue;
   private final Animation animation = new Animation();

   public enum ImageMode {
      biao1, biao2, biao3, love, cat;
      public String getPath() {
         return switch (this) {
            case biao1 -> "alienclient:textures/biao1.png";
            case biao2 -> "alienclient:textures/biao2.png";
            case biao3 -> "alienclient:textures/biao3.png";
            case love  -> "alienclient:textures/love.png";
            case cat   -> "alienclient:textures/cat.png";
         };
      }
   }

   public ClickGui() {
      super("ClickGui", Module.Category.Client);
      this.setChinese("点击界面");
      INSTANCE = this;
      Forever.EVENT_BUS.subscribe(new FadeOut());
      Forever.EVENT_BUS.subscribe(new Q());
   }

   public static ClickGui getInstance() { return INSTANCE; }

   public ShaderParams buildGuiShaderParams() {
      ClickGui self = this;
      return new ShaderParams() {
         @Override public float getSpeed()      { return self.guiShaderSpeed.getValueFloat(); }
         @Override public Color getFillColor()  { return self.guiShaderColor.getValue(); }
         @Override public float getMaxSample()  { return self.guiShaderMaxSample.getValueFloat(); }
         @Override public float getDivider()    { return self.guiShaderDivider.getValueFloat(); }
         @Override public float getRadius()     { return self.guiShaderRadius.getValueFloat(); }
         @Override public float getSmoothness() { return self.guiShaderSmoothness.getValueFloat(); }
         @Override public float getAlpha()      { return self.guiShaderAlpha.getValueFloat(); }
         @Override public Color getPulseColor() { return self.guiShaderPulseColor.getValue(); }
         @Override public float getPulseSpeed() { return self.guiShaderPulseSpeed.getValueFloat(); }
         @Override public Color getSmoke1()     { return self.guiShaderSmoke1.getValue(); }
         @Override public Color getSmoke2()     { return self.guiShaderSmoke2.getValue(); }
         @Override public Color getSmoke3()     { return self.guiShaderSmoke3.getValue(); }
         @Override public Color getSmoke4()     { return self.guiShaderSmoke4.getValue(); }
         @Override public float getStep()       { return self.guiShaderStep.getValueFloat(); }
         @Override public int getOctaves()      { return self.guiShaderOctaves.getValueInt(); }
      };
   }

   public void a() {
      this.elements.setValueWithoutTask(false);
      this.elements.setOpen(!this.elements.isOpen());
   }

   public void b() {
      this.colors.setValueWithoutTask(false);
      this.colors.setOpen(!this.colors.isOpen());
   }

   private void verifyIntegrity() {
      try {
         if (false) { c(); return; }
         if (!KeyAuth.isLoginRequired() && (!KeyAuth.isCachedAllowed() || !KeyAuth.verifySessionSeal() || KeyAuth.guardValue() == 0)) {
            c();
         }
      } catch (Exception e) {
         c();
      }
   }

   private void c() {
      AuthFail.hardStop("clickgui integrity guard");
   }

   @Override
   public void onEnable() {
      if (nullCheck()) {
         this.disable();
         return;
      }
      this.updateColor();

      if (this.theme.getValue() == Theme.Modern) {
         mc.setScreen(new ModernGuiScreen(this));
      } else if (this.theme.getValue() == Theme.Troll) {
         mc.setScreen(new TrollClickGuiScreen(this));
      } else {
         for (Component component : ClickGuiScreen.getInstance().getComponents()) {
            component.setHeight(18);
            for (Item item : component.getItems()) {
               item.setHeight(10 + this.height.getValueInt());
            }
         }
         mc.setScreen(ClickGuiScreen.getInstance());
      }
   }

   @Override
   public void onDisable() {
      if (mc.currentScreen instanceof ClickGuiScreen || mc.currentScreen instanceof ModernGuiScreen || mc.currentScreen instanceof TrollClickGuiScreen) {
         mc.currentScreen.close();
      }
      if (this.autoSave.getValue()) Forever.save();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.updateColor();
      if (!(mc.currentScreen instanceof ClickGuiScreen) && !(mc.currentScreen instanceof ModernGuiScreen) && !(mc.currentScreen instanceof TrollClickGuiScreen)) {
         this.disable();
      }
   }

   public void updateColor() {
      Button.hoverBGColor     = this.hoverBGColor.getValue().getRGB();
      Button.defaultTextColor = this.defaultTextColor.getValue().getRGB();
      Button.defaultBGColor   = this.defaultBGColor.getValue().getRGB();
      Button.enableTextColor  = this.enableTextColor.getValue().getRGB();
   }

   public class Q {
      @EventListener
      public void onUpdate(UpdateEvent event) {
         if (!ClickGui.this.p && mc.world != null && mc.player != null) {
            ClickGui.this.verifyIntegrity();
            ClickGui.this.p = true;
         }
      }
   }

   public class FadeOut {
      @EventListener(priority = -99999)
      public void onRender2D(Render2DEvent event) {
         if (ClickGui.this.theme.getValue() == Theme.Modern || ClickGui.this.theme.getValue() == Theme.Troll) return;
         if (ClickGui.this.fade.getValue()) {
            if (ClickGui.this.alphaValue > 0.0 || ClickGui.this.isOn()) {
               ClickGui.this.alphaValue = ClickGui.this.animation.get(
                       ClickGui.this.isOn() ? 1.0 : 0.0,
                       ClickGui.this.length.getValueInt(),
                       (Easing) ClickGui.this.easing.getValue()
               );
            }
            if (ClickGui.this.alphaValue > 0.0 && !(Wrapper.mc.currentScreen instanceof ClickGuiScreen)) {
               event.drawContext.getMatrices().push();
               event.drawContext.getMatrices().translate(0.0F, 0.0F, 5000.0F);
               ClickGuiScreen.getInstance().render(event.drawContext, 0, 0, event.tickDelta);
               event.drawContext.getMatrices().pop();
            }
         } else {
            ClickGui.this.alphaValue = 1.0;
         }
      }
   }
}
