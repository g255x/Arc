package dev.g255.arc.mod.modules.impl.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.Render2DEvent;
import dev.g255.arc.api.events.impl.ResizeEvent;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.api.utils.math.Animation;
import dev.g255.arc.api.utils.math.Easing;
import dev.g255.arc.api.utils.render.ColorUtil;
import dev.g255.arc.mod.gui.ClickGuiScreen;
import dev.g255.arc.mod.gui.items.Component;
import dev.g255.arc.mod.gui.items.Item;
import dev.g255.arc.mod.gui.items.buttons.Button;
import dev.g255.arc.mod.gui.windows.WindowsScreen;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import dev.g255.arc.mod.modules.settings.impl.StringSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Objects;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.util.Identifier;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage.Format;

public class ClickGui extends Module {
   private static ClickGui INSTANCE;
   public final EnumSetting<Style> style;
   public final EnumSetting<BackgroundStyle> backgroundStyle;
   public final EnumSetting<UI> ui;
   public final BooleanSetting disableNotification;
   public final BooleanSetting sound;
   public final SliderSetting soundPitch;
   public final BooleanSetting guiSound;
   public final SliderSetting moduleButtonHeight;
   public final SliderSetting moduleButtonWidth;
   public final SliderSetting categoryWidth;
   public final SliderSetting categoryBarHeight;
   public final SliderSetting textOffset;
   public final SliderSetting titleOffset;
   public final SliderSetting alpha;
   public final SliderSetting hoverAlpha;
   public final SliderSetting topAlpha;
   public final SliderSetting backgroundAlpha;
   public final SliderSetting tooltipAlpha;
   public final BooleanSetting fade;
   public final SliderSetting length;
   public final EnumSetting<Easing> easing;
   public final BooleanSetting scrollAnim;
   public final SliderSetting scrollAnimLength;
   public final EnumSetting<Easing> scrollAnimEasing;
   public final BooleanSetting mouseMove;
   public final SliderSetting mouseMoveStrength;
   public final SliderSetting mouseMoveSmooth;
   public final BooleanSetting walkShake;
   public final SliderSetting walkShakeStrength;
   public final SliderSetting walkShakeSpeed;
   public final SliderSetting walkShakeSmooth;
   public final SliderSetting walkShakeMax;
   public final BooleanSetting blur;
   public final EnumSetting<BlurType> blurType;
   public final SliderSetting radius;
   public final BooleanSetting snow;
   public final EnumSetting<SnowShape> snowShape;
   public final SliderSetting snowAmount;
   public final SliderSetting snowSpeed;
   public final SliderSetting snowSize;
   public final SliderSetting snowAlpha;
   public final SliderSetting snowWind;
   public final BooleanSetting tips;
   public final BooleanSetting elements;
   public final BooleanSetting line;
   public final ColorSetting gear;
   public final EnumSetting<ExpandIcon> expandIcon;
   public final BooleanSetting recenterLayoutButton;
   public final BooleanSetting colors;
   public final EnumSetting<ColorMode> colorMode;
   public final SliderSetting rainbowSpeed;
   public final SliderSetting saturation;
   public final SliderSetting rainbowDelay;
   public final ColorSetting color;
   public final ColorSetting secondColor;
   public final SliderSetting pulseSpeed;
   public final SliderSetting pulseCounter;
   public final ColorSetting activeColor;
   public final ColorSetting hoverColor;
   public final ColorSetting defaultColor;
   public final ColorSetting defaultTextColor;
   public final ColorSetting enableTextColor;
   public final ColorSetting backGround;
   public final ColorSetting tint;
   public final ColorSetting endColor;
   /** 各分类面板的坐标（"x,y;x,y;..."），跨启动保存，避免每次开界面都被排回默认位置。 */
   public final StringSetting layout;
   public double alphaValue;
   private final Animation animation;
   public static String key = "";
   private UI lastUi;
   private int lastLayoutCategoryWidth = -1;
   private int lastLayoutButtonWidth = -1;
   private int lastLayoutBarHeight = -1;
   private UI lastLayoutUi;
   private static final Identifier SPECTRUM_LUT_ID = Identifier.of("arc", "clickgui_spectrum_lut");
   private NativeImage spectrumLutImage;
   private NativeImageBackedTexture spectrumLutTexture;
   private int spectrumLutHeight;

   public ClickGui() {
      super("ClickGui", Module.Category.Client);
      this.style = this.add((new EnumSetting("Style", ClickGui.Style.Static)).injectTask(this::updateStyle));
      this.backgroundStyle = this.add((new EnumSetting("BackgroundStyle", ClickGui.BackgroundStyle.Transparent)).injectTask(this::updateBackgroundStyle));
      this.ui = this.add((new EnumSetting("UI", ClickGui.UI.Purple)).injectTask(this::onUiChanged));
      this.disableNotification = this.add(new BooleanSetting("DisableNotification", false));
      this.sound = this.add((new BooleanSetting("Sound", true)).setParent());
      BooleanSetting var10009 = this.sound;
      Objects.requireNonNull(var10009);
      this.soundPitch = this.add(new SliderSetting("SoundPitch", 1.6, (double)0.0F, (double)2.0F, 0.1, var10009::isOpen));
      this.guiSound = this.add(new BooleanSetting("GuiSound", true));
      this.moduleButtonHeight = this.add((new SliderSetting("ModuleButtonHeight", 13, 10, 25)).injectTask(this::applyHeights));
      this.moduleButtonWidth = this.add((new SliderSetting("ModuleButtonWidth", 93, 60, 160)).injectTask(this::applyHeights));
      this.categoryWidth = this.add((new SliderSetting("CategoryWidth", 95, 60, 200)).injectTask(this::applyHeights));
      this.categoryBarHeight = this.add((new SliderSetting("CategoryBarHeight", 17, 8, 30)).injectTask(this::applyHeights));
      this.textOffset = this.add(new SliderSetting("TextOffset", (double)0.0F, (double)-5.0F, (double)5.0F, (double)1.0F));
      this.titleOffset = this.add(new SliderSetting("TitleOffset", (double)1.0F, (double)-5.0F, (double)5.0F, (double)1.0F));
      this.alpha = this.add(new SliderSetting("Alpha", 150, 0, 255));
      this.hoverAlpha = this.add(new SliderSetting("HoverAlpha", 220, 0, 255));
      this.topAlpha = this.add(new SliderSetting("TopAlpha", 210, 0, 255));
      this.backgroundAlpha = this.add(new SliderSetting("BackgroundAlpha", 0, 0, 255));
      this.tooltipAlpha = this.add(new SliderSetting("TooltipAlpha", 70, 0, 100));
      this.fade = this.add((new BooleanSetting("Fade", true)).setParent());
      BooleanSetting var10008 = this.fade;
      Objects.requireNonNull(var10008);
      this.length = this.add(new SliderSetting("Length", 400, 0, 1000, var10008::isOpen));
      Easing var10005 = Easing.BackInOut;
      BooleanSetting var10006 = this.fade;
      Objects.requireNonNull(var10006);
      this.easing = this.add(new EnumSetting("Easing", var10005, var10006::isOpen));
      this.scrollAnim = this.add((new BooleanSetting("ScrollAnim", false)).setParent());
      var10008 = this.scrollAnim;
      Objects.requireNonNull(var10008);
      this.scrollAnimLength = this.add(new SliderSetting("ScrollAnimLength", 220, 1, 1000, var10008::isOpen));
      var10005 = Easing.SineOut;
      var10006 = this.scrollAnim;
      Objects.requireNonNull(var10006);
      this.scrollAnimEasing = this.add(new EnumSetting("ScrollAnimEasing", var10005, var10006::isOpen));
      this.mouseMove = this.add((new BooleanSetting("MouseMove", false)).setParent());
      var10009 = this.mouseMove;
      Objects.requireNonNull(var10009);
      this.mouseMoveStrength = this.add(new SliderSetting("MouseMoveStrength", (double)16.0F, (double)0.0F, (double)30.0F, (double)0.5F, var10009::isOpen));
      var10009 = this.mouseMove;
      Objects.requireNonNull(var10009);
      this.mouseMoveSmooth = this.add(new SliderSetting("MouseMoveSmooth", (double)10.0F, (double)0.0F, (double)30.0F, (double)0.5F, var10009::isOpen));
      this.walkShake = this.add((new BooleanSetting("WalkShake", false)).setParent());
      var10009 = this.walkShake;
      Objects.requireNonNull(var10009);
      this.walkShakeStrength = this.add(new SliderSetting("WalkShakeStrength", (double)8.0F, (double)0.0F, (double)20.0F, (double)0.5F, var10009::isOpen));
      var10009 = this.walkShake;
      Objects.requireNonNull(var10009);
      this.walkShakeSpeed = this.add(new SliderSetting("WalkShakeSpeed", (double)12.0F, (double)0.0F, (double)30.0F, (double)0.5F, var10009::isOpen));
      var10009 = this.walkShake;
      Objects.requireNonNull(var10009);
      this.walkShakeSmooth = this.add(new SliderSetting("WalkShakeSmooth", (double)14.0F, (double)0.0F, (double)30.0F, (double)0.5F, var10009::isOpen));
      var10009 = this.walkShake;
      Objects.requireNonNull(var10009);
      this.walkShakeMax = this.add(new SliderSetting("WalkShakeMax", (double)8.0F, (double)0.0F, (double)30.0F, (double)0.5F, var10009::isOpen));
      this.blur = this.add((new BooleanSetting("Blur", true)).setParent());
      BlurType var2 = ClickGui.BlurType.Kawase;
      var10006 = this.blur;
      Objects.requireNonNull(var10006);
      this.blurType = this.add(new EnumSetting("BlurType", var2, var10006::isOpen));
      var10008 = this.blur;
      Objects.requireNonNull(var10008);
      this.radius = this.add(new SliderSetting("Radius", (double)5.0F, (double)0.0F, (double)100.0F, var10008::isOpen));
      this.snow = this.add((new BooleanSetting("Snow", false)).setParent());
      SnowShape var3 = ClickGui.SnowShape.Snowflake;
      var10006 = this.snow;
      Objects.requireNonNull(var10006);
      this.snowShape = this.add(new EnumSetting("SnowShape", var3, var10006::isOpen));
      var10008 = this.snow;
      Objects.requireNonNull(var10008);
      this.snowAmount = this.add(new SliderSetting("SnowAmount", 120, 0, 600, var10008::isOpen));
      var10009 = this.snow;
      Objects.requireNonNull(var10009);
      this.snowSpeed = this.add(new SliderSetting("SnowSpeed", (double)38.0F, (double)1.0F, (double)160.0F, (double)1.0F, var10009::isOpen));
      var10009 = this.snow;
      Objects.requireNonNull(var10009);
      this.snowSize = this.add(new SliderSetting("SnowSize", 1.8, (double)0.5F, (double)5.0F, 0.1, var10009::isOpen));
      var10008 = this.snow;
      Objects.requireNonNull(var10008);
      this.snowAlpha = this.add(new SliderSetting("SnowAlpha", 160, 0, 255, var10008::isOpen));
      var10009 = this.snow;
      Objects.requireNonNull(var10009);
      this.snowWind = this.add(new SliderSetting("SnowWind", (double)10.0F, (double)-80.0F, (double)80.0F, (double)1.0F, var10009::isOpen));
      this.tips = this.add(new BooleanSetting("Tips", false));
      this.elements = this.add((new BooleanSetting("Elements", false)).setParent().injectTask(this::keyCodec));
      var10006 = this.elements;
      Objects.requireNonNull(var10006);
      this.line = this.add(new BooleanSetting("Line", true, var10006::isOpen));
      var10006 = this.elements;
      Objects.requireNonNull(var10006);
      this.gear = this.add((new ColorSetting("Gear", -1, var10006::isOpen)).injectBoolean(false));
      ExpandIcon var4 = ClickGui.ExpandIcon.PlusMinus;
      var10006 = this.elements;
      Objects.requireNonNull(var10006);
      this.expandIcon = this.add(new EnumSetting("ExpandIcon", var4, var10006::isOpen));
      var10006 = this.elements;
      Objects.requireNonNull(var10006);
      this.recenterLayoutButton = this.add((new BooleanSetting("RecenterLayout", false, var10006::isOpen)).injectTask(this::recenterLayoutCodec));
      this.colors = this.add((new BooleanSetting("Colors", false)).setParent().injectTask(this::elementCodec));
      ColorMode var5 = ClickGui.ColorMode.Custom;
      var10006 = this.colors;
      Objects.requireNonNull(var10006);
      this.colorMode = this.add(new EnumSetting("ColorMode", var5, var10006::isOpen));
      this.rainbowSpeed = this.add(new SliderSetting("RainbowSpeed", (double)1.0F, (double)1.0F, (double)10.0F, 0.1, () -> this.colors.isOpen() && (this.colorMode.getValue() == ClickGui.ColorMode.Rainbow || this.colorMode.getValue() == ClickGui.ColorMode.Spectrum)));
      this.saturation = this.add(new SliderSetting("Saturation", (double)210.0F, (double)1.0F, (double)255.0F, () -> this.colors.isOpen() && (this.colorMode.getValue() == ClickGui.ColorMode.Rainbow || this.colorMode.getValue() == ClickGui.ColorMode.Spectrum)));
      this.rainbowDelay = this.add(new SliderSetting("Delay", 50, 0, 1000, () -> this.colors.isOpen() && (this.colorMode.getValue() == ClickGui.ColorMode.Rainbow || this.colorMode.getValue() == ClickGui.ColorMode.Spectrum)));
      this.color = this.add(new ColorSetting("FirstColor", new Color(186, 220, 245, 255), () -> this.colors.isOpen() && this.colorMode.getValue() == ClickGui.ColorMode.Custom));
      this.secondColor = this.add((new ColorSetting("SecondColor", new Color(255, 0, 0, 255), () -> this.colors.isOpen() && this.colorMode.getValue() == ClickGui.ColorMode.Pulse)).injectBoolean(false));
      this.pulseSpeed = this.add(new SliderSetting("PulseSpeed", 1.1, (double)0.0F, (double)5.0F, 0.1, () -> this.colors.isOpen() && this.colorMode.getValue() == ClickGui.ColorMode.Pulse));
      this.pulseCounter = this.add(new SliderSetting("Counter", 14, 1, 50, () -> this.colors.isOpen() && this.colorMode.getValue() == ClickGui.ColorMode.Pulse));
      this.activeColor = this.add(new ColorSetting("ActiveColor", new Color(173, 208, 235, 255), () -> this.colors.isOpen() && this.colorMode.getValue() == ClickGui.ColorMode.Custom));
      Color var6 = new Color(50, 50, 50, 200);
      var10006 = this.colors;
      Objects.requireNonNull(var10006);
      this.hoverColor = this.add(new ColorSetting("HoverColor", var6, var10006::isOpen));
      var6 = new Color(0, 0, 0, 0);
      var10006 = this.colors;
      Objects.requireNonNull(var10006);
      this.defaultColor = this.add(new ColorSetting("DefaultColor", var6, var10006::isOpen));
      var6 = new Color(220, 220, 220);
      var10006 = this.colors;
      Objects.requireNonNull(var10006);
      this.defaultTextColor = this.add(new ColorSetting("DefaultTextColor", var6, var10006::isOpen));
      var6 = new Color(255, 255, 255);
      var10006 = this.colors;
      Objects.requireNonNull(var10006);
      this.enableTextColor = this.add(new ColorSetting("EnableTextColor", var6, var10006::isOpen));
      var6 = new Color(30, 30, 30, 236);
      var10006 = this.colors;
      Objects.requireNonNull(var10006);
      this.backGround = this.add((new ColorSetting("BackGround", var6, var10006::isOpen)).injectBoolean(false));
      this.tint = this.add((new ColorSetting("Tint", new Color(0, 112, 212, 36))).injectBoolean(false));
      this.endColor = this.add(new ColorSetting("End", new Color(0, 0, 255, 50), () -> this.tint.booleanValue));
      this.layout = this.add(new StringSetting("Layout", ""));
      this.layout.setHidden();
      this.animation = new Animation();
      this.spectrumLutHeight = -1;
      this.setChinese("点击界面");
      INSTANCE = this;
      Arc.EVENT_BUS.subscribe(new FadeOut());
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

   public void recenterLayoutCodec() {
      this.recenterLayoutButton.setValueWithoutTask(false);
      this.recenterLayout();
   }

   private void applyHeights() {
      ArrayList<Component> components = ClickGuiScreen.getInstance().getComponents();
      int categoryWidth = this.categoryWidth.getValueInt();
      int moduleButtonWidth = this.moduleButtonWidth.getValueInt();
      int layoutWidth = Math.max(categoryWidth, moduleButtonWidth);
      int spacing = layoutWidth + 1;
      int count = components.size();
      int startX = 10;
      int startY = 4;
      boolean purple = this.ui.getValue() == ClickGui.UI.Purple;
      if (purple) {
         startX = 9;
         startY = 5;
      } else if (mc != null && mc.getWindow() != null) {
         int screenWidth = mc.getWindow().getScaledWidth();
         int screenHeight = mc.getWindow().getScaledHeight();
         int totalWidth = count * layoutWidth + (count - 1);
         startX = Math.round(((float)screenWidth - (float)totalWidth) / 2.0F);
         startY = Math.round((float)screenHeight / 6.0F);
      }

      // 只有"从没被拖动过"或者"尺寸/主题变了"才重新排版，
      // 否则每次打开 ClickGUI 都会把玩家拖好的位置复位。
      boolean sizeChanged = categoryWidth != this.lastLayoutCategoryWidth
         || moduleButtonWidth != this.lastLayoutButtonWidth
         || this.categoryBarHeight.getValueInt() != this.lastLayoutBarHeight
         || this.ui.getValue() != this.lastLayoutUi;
      boolean defaultLayout = true;
      int offsetX = Math.round(((float)layoutWidth - (float)moduleButtonWidth) / 2.0F);
      int expectedX = startX + offsetX;

      for(int i = 0; i < components.size(); ++i) {
         Component component = (Component)components.get(i);
         if (component.getTargetX() != expectedX || component.getTargetY() != startY) {
            defaultLayout = false;
            break;
         }

         expectedX += spacing;
      }

      int componentHeight = this.categoryBarHeight.getValueInt() + 5;
      int x = startX + offsetX;

      for(int i = 0; i < components.size(); ++i) {
         Component component = (Component)components.get(i);
         component.setWidth(moduleButtonWidth);
         component.setHeight(componentHeight);
         if (defaultLayout || sizeChanged) {
            component.setX(x);
            component.setY(startY);
            x += spacing;
         }

         for(Item item : component.getItems()) {
            item.setHeight(this.moduleButtonHeight.getValueInt());
         }
      }

      this.lastLayoutCategoryWidth = categoryWidth;
      this.lastLayoutButtonWidth = moduleButtonWidth;
      this.lastLayoutBarHeight = this.categoryBarHeight.getValueInt();
      this.lastLayoutUi = this.ui.getValue();
   }

   private void recenterLayout() {
      ArrayList<Component> components = ClickGuiScreen.getInstance().getComponents();
      int categoryWidth = this.categoryWidth.getValueInt();
      int moduleButtonWidth = this.moduleButtonWidth.getValueInt();
      int layoutWidth = Math.max(categoryWidth, moduleButtonWidth);
      int spacing = layoutWidth + 1;
      int count = components.size();
      int startX;
      int startY;
      if (this.ui.getValue() == ClickGui.UI.Purple) {
         startX = 9;
         startY = 5;
      } else {
         if (mc == null || mc.getWindow() == null) {
            return;
         }

         int screenWidth = mc.getWindow().getScaledWidth();
         int screenHeight = mc.getWindow().getScaledHeight();
         int totalWidth = count * layoutWidth + (count - 1);
         startX = Math.round(((float)screenWidth - (float)totalWidth) / 2.0F);
         startY = Math.round((float)screenHeight / 6.0F);
      }

      int componentHeight = this.categoryBarHeight.getValueInt() + 5;
      int offsetX = Math.round(((float)layoutWidth - (float)moduleButtonWidth) / 2.0F);
      int x = startX + offsetX;

      for(int i = 0; i < components.size(); ++i) {
         Component component = (Component)components.get(i);
         component.setWidth(moduleButtonWidth);
         component.setHeight(componentHeight);
         component.setX(x);
         component.setY(startY);
         x += spacing;

         for(Item item : component.getItems()) {
            item.setHeight(this.moduleButtonHeight.getValueInt());
         }
      }

      this.lastLayoutCategoryWidth = categoryWidth;
      this.lastLayoutButtonWidth = moduleButtonWidth;
      this.lastLayoutBarHeight = this.categoryBarHeight.getValueInt();
      this.lastLayoutUi = this.ui.getValue();
   }

   /**
    * SHIFT+左键重置本模块设置后由 GUI 调用：重置只改了设置值，主题配色与排版本该跟着
    * 重新应用一遍，否则会出现颜色、形态都错乱，必须关掉再开一次 ClickGUI 才恢复。
    */
   @Override
   public void onSettingsReset() {
      this.lastUi = this.ui.getValue();
      this.applyThemePreset();
      this.updateColor();
      this.applyHeights();
      this.recenterLayout();
      this.saveLayout();
   }

   /** 记录当前各分类面板的位置，随配置一起保存。 */
   public void saveLayout() {
      // 配置加载阶段（还没进世界）也会走到 onDisable，此时不能去碰 ClickGuiScreen：
      // 它是静态单例，过早构造会只注册到一半模块。
      if (mc == null || mc.world == null || mc.player == null) {
         return;
      }

      ArrayList<Component> components = ClickGuiScreen.getInstance().getComponents();
      if (components.isEmpty()) {
         return;
      }

      StringBuilder builder = new StringBuilder();

      for(Component component : components) {
         if (builder.length() > 0) {
            builder.append(';');
         }

         builder.append(component.getTargetX()).append(',').append(component.getTargetY());
      }

      this.layout.setValue(builder.toString());
   }

   /** 打开界面时恢复上次保存的面板位置（越界坐标会被夹回可视区域）。 */
   private void restoreLayout() {
      String data = this.layout.getValue();
      if (data == null || data.isEmpty()) {
         return;
      }

      String[] parts = data.split(";");
      ArrayList<Component> components = ClickGuiScreen.getInstance().getComponents();
      if (parts.length != components.size()) {
         return;
      }

      int maxX = mc != null && mc.getWindow() != null ? mc.getWindow().getScaledWidth() - 20 : 1000;
      int maxY = mc != null && mc.getWindow() != null ? mc.getWindow().getScaledHeight() - 20 : 1000;

      for(int i = 0; i < parts.length; ++i) {
         String[] xy = parts[i].split(",");
         if (xy.length != 2) {
            return;
         }

         try {
            int x = Integer.parseInt(xy[0].trim());
            int y = Integer.parseInt(xy[1].trim());
            components.get(i).setX(Math.max(0, Math.min(maxX, x)));
            components.get(i).setY(Math.max(0, Math.min(maxY, y)));
         } catch (NumberFormatException e) {
            return;
         }
      }
   }

   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         if (this.ui.getValue() == ClickGui.UI.Purple) {
            if (this.categoryWidth.getValueInt() == 95 && this.moduleButtonWidth.getValueInt() == 93) {
               this.purplePreset();
            }
         } else if (this.categoryWidth.getValueInt() == 100 && this.moduleButtonWidth.getValueInt() == 100) {
            this.defaultPreset();
         }

         this.lastUi = this.ui.getValue();
         this.updateColor();
         if (this.guiSound.getValue() && mc.getSoundManager() != null) {
            mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, this.soundPitch.getValueFloat()));
         }

         this.applyHeights();
         this.restoreLayout();
         mc.setScreen(ClickGuiScreen.getInstance());
      }
   }

   public static boolean isGuiScreen(Screen screen) {
      return screen instanceof ClickGuiScreen;
   }

   private void onUiChanged() {
      this.applyThemePreset();
      this.updateColor();
      this.applyHeights();
      this.recenterLayout();
   }

   private void applyThemePreset() {
      if (this.ui.getValue() == ClickGui.UI.Purple) {
         this.purplePreset();
      } else {
         this.defaultPreset();
      }

   }

   private void defaultPreset() {
      this.updateStyle();
      this.line.setValue(true);
      this.gear.booleanValue = false;
      this.tint.booleanValue = false;
      this.categoryWidth.setValue((double)95.0F);
      this.moduleButtonWidth.setValue((double)93.0F);
      this.categoryBarHeight.setValue((double)17.0F);
      this.moduleButtonHeight.setValue((double)13.0F);
   }

   private void purplePreset() {
      if (this.ui.getValue() == ClickGui.UI.Purple) {
         this.color.setValue(new Color(108, 86, 142));
         this.activeColor.setValue(new Color(122, 100, 165));
         this.hoverColor.setValue(new Color(90, 70, 120, 140));
         this.defaultColor.setValue(new Color(60, 45, 78, 110));
         this.defaultTextColor.setValue(new Color(238, 234, 248));
         this.enableTextColor.setValue(new Color(255, 255, 255));
         this.backGround.setValue(new Color(0, 0, 0));
         this.backGround.booleanValue = true;
         this.colorMode.setValue(ClickGui.ColorMode.Custom);
         this.color.rainbow = false;
         this.activeColor.rainbow = false;
         this.tint.rainbow = false;
         this.endColor.rainbow = false;
         this.tint.booleanValue = false;
         this.endColor.setValue(new Color(40, 20, 80, 40));
         this.secondColor.booleanValue = false;
         this.line.setValue(true);
         this.gear.booleanValue = true;
         this.gear.setValue(new Color(232, 228, 245));
         this.expandIcon.setValue(ClickGui.ExpandIcon.PlusMinus);
         this.alpha.setValue((double)120.0F);
         this.hoverAlpha.setValue((double)160.0F);
         this.topAlpha.setValue((double)215.0F);
         this.backgroundAlpha.setValue((double)110.0F);
         this.categoryWidth.setValue((double)100.0F);
         this.moduleButtonWidth.setValue((double)100.0F);
         this.categoryBarHeight.setValue((double)15.0F);
         this.moduleButtonHeight.setValue((double)13.0F);
      }
   }

   public void themeSync() {
      UI current = this.ui.getValue();
      if (current != this.lastUi) {
         this.lastUi = current;
         this.applyThemePreset();
         this.updateColor();
         this.applyHeights();
         this.recenterLayout();
      }
   }

   public void onDisable() {
      this.saveLayout();
      if (isGuiScreen(mc.currentScreen)) {
         mc.currentScreen.close();
      }

      if (this.guiSound.getValue() && mc.getSoundManager() != null) {
         mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, this.soundPitch.getValueFloat()));
      }

      if (mc != null && this.spectrumLutTexture != null) {
         mc.getTextureManager().destroyTexture(SPECTRUM_LUT_ID);
         this.spectrumLutTexture = null;
      }

      if (this.spectrumLutImage != null) {
         this.spectrumLutImage.close();
         this.spectrumLutImage = null;
      }

      this.spectrumLutHeight = -1;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.updateColor();
      // 方块/物品选择窗口是 ClickGUI 的子界面，打开它时不能把 ClickGui 模块关掉，
      // 否则退出选择窗口后回不到 ClickGUI。
      if (!isGuiScreen(mc.currentScreen) && !(mc.currentScreen instanceof WindowsScreen)) {
         this.disable();
      }

   }

   @EventListener
   public void onResize(ResizeEvent event) {
      if (mc != null && mc.currentScreen instanceof ClickGuiScreen) {
         this.recenterLayout();
      }

   }

   public void updateColor() {
      Button.hoverColor = this.hoverColor.getValue().getRGB();
      Button.defaultTextColor = this.defaultTextColor.getValue().getRGB();
      Button.defaultColor = this.defaultColor.getValue().getRGB();
      Button.enableTextColor = this.enableTextColor.getValue().getRGB();
   }

   public Identifier getSpectrumLutId() {
      return SPECTRUM_LUT_ID;
   }

   public int getSpectrumLutHeight() {
      return this.spectrumLutHeight;
   }

   public void updateSpectrumLut(int scaledHeight) {
      if (scaledHeight > 0 && mc != null) {
         if (this.spectrumLutTexture == null || this.spectrumLutImage == null || this.spectrumLutHeight != scaledHeight) {
            this.recreateSpectrumLut(scaledHeight);
         }

         if (this.spectrumLutTexture != null && this.spectrumLutImage != null && this.spectrumLutHeight > 0) {
            long now = System.currentTimeMillis();
            double speed = this.rainbowSpeed.getValue();
            double delayMul = this.rainbowDelay.getValue();
            float sat = this.saturation.getValueFloat() / 255.0F;
            int h = this.spectrumLutHeight;

            for(int y = 0; y < h; ++y) {
               double delay = (double)y * (double)0.25F;
               double rainbowState = Math.ceil(((double)now * speed + delay * delayMul) / (double)20.0F);
               int argb = Color.getHSBColor((float)(rainbowState % (double)360.0F / (double)360.0F), sat, 1.0F).getRGB();
               this.spectrumLutImage.setColor(0, y, argbToAbgr(argb));
            }

            if (RenderSystem.isOnRenderThread()) {
               this.spectrumLutTexture.upload();
            } else {
               RenderSystem.recordRenderCall(() -> this.spectrumLutTexture.upload());
            }

         }
      }
   }

   private void recreateSpectrumLut(int scaledHeight) {
      if (mc != null) {
         if (this.spectrumLutTexture != null) {
            mc.getTextureManager().destroyTexture(SPECTRUM_LUT_ID);
            this.spectrumLutTexture = null;
         }

         if (this.spectrumLutImage != null) {
            this.spectrumLutImage.close();
            this.spectrumLutImage = null;
         }

         this.spectrumLutHeight = scaledHeight;
         this.spectrumLutImage = new NativeImage(Format.RGBA, 1, scaledHeight, false);
         this.spectrumLutTexture = new NativeImageBackedTexture(this.spectrumLutImage);
         if (RenderSystem.isOnRenderThread()) {
            this.spectrumLutTexture.upload();
            mc.getTextureManager().registerTexture(SPECTRUM_LUT_ID, this.spectrumLutTexture);
         } else {
            RenderSystem.recordRenderCall(() -> {
               this.spectrumLutTexture.upload();
               mc.getTextureManager().registerTexture(SPECTRUM_LUT_ID, this.spectrumLutTexture);
            });
         }

      }
   }

   private static int argbToAbgr(int argb) {
      int a = argb >> 24 & 255;
      int r = argb >> 16 & 255;
      int g = argb >> 8 & 255;
      int b = argb & 255;
      return a << 24 | b << 16 | g << 8 | r;
   }

   public Color getColor() {
      return this.getColor((double)0.0F);
   }

   public Color getColor(double delay) {
      return this.getModeColor(this.color.getValue(), delay);
   }

   public Color getActiveColor() {
      return this.getActiveColor((double)0.0F);
   }

   public Color getActiveColor(double delay) {
      return this.getModeColor(this.activeColor.getValue(), delay);
   }

   private Color getModeColor(Color customColor, double delay) {
      return this.colorMode.getValue() == ClickGui.ColorMode.Custom ? customColor : this.dynamicColor(delay);
   }

   private Color dynamicColor(double delay) {
      if (this.colorMode.getValue() == ClickGui.ColorMode.Pulse) {
         return this.secondColor.booleanValue ? ColorUtil.pulseColor(this.color.getValue(), this.secondColor.getValue(), delay, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue()) : ColorUtil.pulseColor(this.color.getValue(), delay, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue());
      } else if (this.colorMode.getValue() != ClickGui.ColorMode.Rainbow && this.colorMode.getValue() != ClickGui.ColorMode.Spectrum) {
         return this.color.getValue();
      } else {
         double rainbowState = Math.ceil(((double)System.currentTimeMillis() * this.rainbowSpeed.getValue() + delay * this.rainbowDelay.getValue()) / (double)20.0F);
         return Color.getHSBColor((float)(rainbowState % (double)360.0F / (double)360.0F), this.saturation.getValueFloat() / 255.0F, 1.0F);
      }
   }

   public void updateBackgroundStyle() {
      BackgroundStyle mode = this.backgroundStyle.getValue();
      if (mode == null) {
         mode = ClickGui.BackgroundStyle.Dark;
      }

      if (mode == ClickGui.BackgroundStyle.Dark) {
         this.backGround.setValue(new Color(30, 30, 30, 236));
         this.backGround.booleanValue = true;
         this.backgroundAlpha.setValue((double)236.0F);
      } else {
         if (mode == ClickGui.BackgroundStyle.Transparent) {
            this.backGround.booleanValue = false;
            this.defaultColor.setValue(new Color(0, 0, 0, 50));
            this.backgroundAlpha.setValue((double)0.0F);
         }

      }
   }

   public void updateStyle() {
      Style mode = this.style.getValue();
      if (mode == null) {
         mode = ClickGui.Style.Static;
      }

      this.color.setValue(new Color(0, 120, 212));
      this.activeColor.setValue(new Color(0, 120, 212));
      this.hoverColor.setValue(new Color(50, 50, 50, 200));
      this.defaultColor.setValue(new Color(30, 30, 30, 236));
      this.defaultTextColor.setValue(new Color(220, 220, 220));
      this.enableTextColor.setValue(new Color(255, 255, 255));
      this.backGround.setValue(new Color(30, 30, 30, 236));
      this.backGround.booleanValue = true;
      this.color.rainbow = false;
      this.activeColor.rainbow = false;
      this.tint.rainbow = false;
      this.endColor.rainbow = false;
      this.alpha.setValue((double)150.0F);
      this.hoverAlpha.setValue((double)220.0F);
      this.topAlpha.setValue((double)210.0F);
      this.backgroundAlpha.setValue((double)236.0F);
      if (mode == ClickGui.Style.Static) {
         this.colors.setValueWithoutTask(false);
         this.colors.setOpen(false);
         this.colorMode.setValue(ClickGui.ColorMode.Custom);
         this.tint.booleanValue = true;
         this.tint.rainbow = false;
         this.tint.setValue(new Color(0, 120, 212, 36));
         this.endColor.rainbow = false;
         this.endColor.setValue(new Color(0, 0, 255, 50));
         this.secondColor.booleanValue = false;
         this.updateBackgroundStyle();
      } else {
         this.colors.setValueWithoutTask(true);
         this.colors.setOpen(true);
         if (mode == ClickGui.Style.RainbowDelay) {
            this.colorMode.setValue(ClickGui.ColorMode.Rainbow);
            this.rainbowSpeed.setValue((double)1.0F);
            this.saturation.setValue((double)210.0F);
            this.rainbowDelay.setValue((double)50.0F);
            this.defaultTextColor.setValue(new Color(255, 255, 255));
            this.tint.booleanValue = true;
            this.tint.rainbow = true;
            this.endColor.rainbow = false;
            this.endColor.setValue(new Color(255, 255, 255, 50));
            this.secondColor.booleanValue = false;
            this.updateBackgroundStyle();
         } else if (mode == ClickGui.Style.SimpleRainbow) {
            this.colorMode.setValue(ClickGui.ColorMode.Custom);
            this.color.rainbow = true;
            this.activeColor.rainbow = true;
            this.tint.booleanValue = true;
            this.tint.rainbow = true;
            this.endColor.rainbow = false;
            this.endColor.setValue(new Color(255, 255, 255, 50));
            this.secondColor.booleanValue = false;
            this.updateBackgroundStyle();
         } else if (mode == ClickGui.Style.Spectrum) {
            this.colorMode.setValue(ClickGui.ColorMode.Spectrum);
            this.rainbowSpeed.setValue((double)1.0F);
            this.saturation.setValue((double)210.0F);
            this.rainbowDelay.setValue((double)50.0F);
            this.tint.booleanValue = true;
            this.tint.rainbow = true;
            this.endColor.rainbow = false;
            this.endColor.setValue(new Color(255, 255, 255, 50));
            this.secondColor.booleanValue = false;
            this.updateBackgroundStyle();
         } else {
            if (mode == ClickGui.Style.Pulse) {
               this.colorMode.setValue(ClickGui.ColorMode.Pulse);
               this.pulseSpeed.setValue(1.15);
               this.pulseCounter.setValue((double)14.0F);
               this.color.setValue(new Color(0, 120, 212));
               this.activeColor.setValue(new Color(0, 120, 212));
               this.secondColor.setValue(new Color(255, 0, 0, 255));
               this.secondColor.booleanValue = true;
               this.tint.booleanValue = true;
               this.tint.rainbow = false;
               this.tint.setValue(new Color(0, 120, 212, 36));
               this.endColor.rainbow = false;
               this.endColor.setValue(new Color(255, 0, 0, 24));
               this.updateBackgroundStyle();
            }

         }
      }
   }

   public Color dynamicColor(int delay) {
      return this.dynamicColor((double)delay);
   }

   public static enum Style {
      Static,
      RainbowDelay,
      SimpleRainbow,
      Spectrum,
      Pulse;

      // $FF: synthetic method
      private static Style[] $values() {
         return new Style[]{Static, RainbowDelay, SimpleRainbow, Spectrum, Pulse};
      }
   }

   public static enum UI {
      Default,
      Purple;

      // $FF: synthetic method
      private static UI[] $values() {
         return new UI[]{Default, Purple};
      }
   }

   public static enum BackgroundStyle {
      Dark,
      Transparent;

      // $FF: synthetic method
      private static BackgroundStyle[] $values() {
         return new BackgroundStyle[]{Dark, Transparent};
      }
   }

   public static enum ColorMode {
      Custom,
      Pulse,
      Rainbow,
      Spectrum;

      // $FF: synthetic method
      private static ColorMode[] $values() {
         return new ColorMode[]{Custom, Pulse, Rainbow, Spectrum};
      }
   }

   public static enum ExpandIcon {
      PlusMinus,
      Chevron,
      Gear;

      // $FF: synthetic method
      private static ExpandIcon[] $values() {
         return new ExpandIcon[]{PlusMinus, Chevron, Gear};
      }
   }

   public static enum SnowShape {
      Snowflake,
      Circle;

      // $FF: synthetic method
      private static SnowShape[] $values() {
         return new SnowShape[]{Snowflake, Circle};
      }
   }

   public static enum BlurType {
      Box,
      Tent,
      Gaussian,
      Kawase,
      Radial;

      // $FF: synthetic method
      private static BlurType[] $values() {
         return new BlurType[]{Box, Tent, Gaussian, Kawase, Radial};
      }
   }

   public class FadeOut {
      @EventListener(
         priority = -99999
      )
      public void onRender2D(Render2DEvent event) {
         if (ClickGui.this.fade.getValue()) {
            if (ClickGui.this.alphaValue > (double)0.0F || ClickGui.this.isOn()) {
               ClickGui.this.alphaValue = ClickGui.this.animation.get(ClickGui.this.isOn() ? (double)1.0F : (double)0.0F, (long)ClickGui.this.length.getValueInt(), ClickGui.this.easing.getValue());
            }

            if (ClickGui.this.alphaValue > (double)0.0F && !(Wrapper.mc.currentScreen instanceof ClickGuiScreen)) {
               event.drawContext.getMatrices().push();
               event.drawContext.getMatrices().translate(0.0F, 0.0F, 5000.0F);
               ClickGuiScreen.getInstance().render(event.drawContext, 0, 0, event.tickDelta);
               event.drawContext.getMatrices().pop();
            }
         } else {
            ClickGui.this.alphaValue = (double)1.0F;
         }

      }
   }
}
