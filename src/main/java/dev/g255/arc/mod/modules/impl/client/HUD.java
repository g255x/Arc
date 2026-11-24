package dev.g255.arc.mod.modules.impl.client;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.ClientTickEvent;
import dev.g255.arc.api.events.impl.InitEvent;
import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.api.utils.math.Animation;
import dev.g255.arc.api.utils.math.Easing;
import dev.g255.arc.api.utils.render.ColorUtil;
import dev.g255.arc.api.utils.render.Render2DUtil;
import dev.g255.arc.api.utils.render.TextUtil;
import dev.g255.arc.core.impl.FontManager;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import dev.g255.arc.mod.modules.settings.impl.StringSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.world.World;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.Registries;

public class HUD extends Module {
   public static HUD INSTANCE;
   public final EnumSetting<Page> page;
   public final BooleanSetting renderingUp;
   public final BooleanSetting font;
   public final BooleanSetting shadow;
   public final BooleanSetting lowerCase;
   public final BooleanSetting sort;
   public final SliderSetting xOffset;
   public final SliderSetting yOffset;
   public final SliderSetting textOffset;
   public final SliderSetting interval;
   public final SliderSetting enableLength;
   public final SliderSetting disableLength;
   public final SliderSetting fadeLength;
   public final EnumSetting<Easing> easing;
   public final BooleanSetting arrayList;
   public final BooleanSetting listSort;
   public final BooleanSetting waterMark;
   public final StringSetting waterMarkString;
   public final SliderSetting waterMarkOffset;
   public final BooleanSetting fps;
   public final BooleanSetting ping;
   public final BooleanSetting tps;
   public final BooleanSetting ip;
   public final BooleanSetting time;
   public final BooleanSetting speed;
   public final BooleanSetting brand;
   public final BooleanSetting potions;
   public final BooleanSetting potionsIcon;
   public final BooleanSetting coords;
   public final BooleanSetting colorSync;
   private final EnumSetting<ColorMode> colorMode;
   public final ColorSetting color;
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
   private final DecimalFormat decimal;
   private final ArrayList<Info> infoList;
   private final ArrayList<Info> moduleList;

   public HUD() {
      super("HUD", Module.Category.Client);
      this.page = this.add(new EnumSetting("Page", HUD.Page.General));
      this.renderingUp = this.add(new BooleanSetting("RenderingUp", false, () -> this.page.is(HUD.Page.General)));
      this.font = this.add(new BooleanSetting("Font", true, () -> this.page.is(HUD.Page.General)));
      this.shadow = this.add(new BooleanSetting("Shadow", true, () -> this.page.is(HUD.Page.General)));
      this.lowerCase = this.add(new BooleanSetting("LowerCase", false, () -> this.page.is(HUD.Page.General)));
      this.sort = this.add(new BooleanSetting("Sort", false, () -> this.page.is(HUD.Page.General)));
      this.xOffset = this.add(new SliderSetting("XOffset", (double)0.0F, (double)0.0F, (double)50.0F, 0.1, () -> this.page.is(HUD.Page.General)));
      this.yOffset = this.add(new SliderSetting("YOffset", (double)0.0F, (double)0.0F, (double)50.0F, 0.1, () -> this.page.is(HUD.Page.General)));
      this.textOffset = this.add(new SliderSetting("TextOffset", (double)0.0F, (double)-10.0F, (double)10.0F, 0.1, () -> this.page.is(HUD.Page.General)));
      this.interval = this.add(new SliderSetting("Interval", (double)0.0F, (double)0.0F, (double)15.0F, 0.1, () -> this.page.is(HUD.Page.General)));
      this.enableLength = this.add(new SliderSetting("EnableLength", 200, 0, 1000, () -> this.page.is(HUD.Page.General)));
      this.disableLength = this.add(new SliderSetting("DisableLength", 200, 0, 1000, () -> this.page.is(HUD.Page.General)));
      this.fadeLength = this.add(new SliderSetting("FadeLength", 200, 0, 1000, () -> this.page.is(HUD.Page.General)));
      this.easing = this.add(new EnumSetting("Easing", Easing.CircInOut, () -> this.page.is(HUD.Page.General)));
      this.arrayList = this.add((new BooleanSetting("ArrayList", true, () -> this.page.is(HUD.Page.Element))).setParent());
      this.listSort = this.add(new BooleanSetting("ListSort", true, () -> this.page.is(HUD.Page.Element) && this.arrayList.isOpen()));
      this.waterMark = this.add((new BooleanSetting("WaterMark", true, () -> this.page.is(HUD.Page.Element))).setParent());
      this.waterMarkString = this.add(new StringSetting("Title", "%ClientName% %Version% - By G255", () -> this.page.is(HUD.Page.Element) && this.waterMark.isOpen()));
      this.waterMarkOffset = this.add(new SliderSetting("Offset", (double)1.0F, (double)0.0F, (double)100.0F, (double)-1.0F, () -> this.page.is(HUD.Page.Element) && this.waterMark.isOpen()));
      this.fps = this.add(new BooleanSetting("FPS", true, () -> this.page.is(HUD.Page.Element)));
      this.ping = this.add(new BooleanSetting("Ping", true, () -> this.page.is(HUD.Page.Element)));
      this.tps = this.add(new BooleanSetting("TPS", true, () -> this.page.is(HUD.Page.Element)));
      this.ip = this.add(new BooleanSetting("IP", false, () -> this.page.is(HUD.Page.Element)));
      this.time = this.add(new BooleanSetting("Time", false, () -> this.page.is(HUD.Page.Element)));
      this.speed = this.add(new BooleanSetting("Speed", true, () -> this.page.is(HUD.Page.Element)));
      this.brand = this.add(new BooleanSetting("Brand", false, () -> this.page.is(HUD.Page.Element)));
      this.potions = this.add(new BooleanSetting("Potions", true, () -> this.page.is(HUD.Page.Element)));
      this.potionsIcon = this.add(new BooleanSetting("PotionsIcon", true, () -> this.page.is(HUD.Page.Element) && this.potions.getValue()));
      this.coords = this.add((new BooleanSetting("Coords", true, () -> this.page.is(HUD.Page.Element))).setParent());
      this.colorSync = this.add(new BooleanSetting("ColorSync", true, () -> this.page.is(HUD.Page.Element) && this.coords.isOpen()));
      this.colorMode = this.add(new EnumSetting("ColorMode", HUD.ColorMode.Pulse, () -> this.page.is(HUD.Page.Color)));
      this.color = this.add(new ColorSetting("Color", new Color(208, 0, 0), () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Custom));
      this.rainbowSpeed = this.add(new SliderSetting("RainbowSpeed", (double)1.0F, (double)1.0F, (double)10.0F, 0.1, () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Rainbow));
      this.saturation = this.add(new SliderSetting("Saturation", (double)220.0F, (double)1.0F, (double)255.0F, () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Rainbow));
      this.rainbowDelay = this.add(new SliderSetting("Delay", 220, 0, 1000, () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Rainbow));
      this.endColor = this.add((new ColorSetting("SecondColor", new Color(255, 0, 0, 255), () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Pulse)).injectBoolean(true));
      this.pulseSpeed = this.add(new SliderSetting("PulseSpeed", (double)1.0F, (double)0.0F, (double)5.0F, 0.1, () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Pulse));
      this.pulseCounter = this.add(new SliderSetting("Counter", 10, 1, 50, () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Pulse));
      this.blur = this.add((new BooleanSetting("Blur", false, () -> this.page.is(HUD.Page.Color))).setParent());
      this.radius = this.add(new SliderSetting("Radius", (double)10.0F, (double)0.0F, (double)100.0F, () -> this.page.is(HUD.Page.Color) && this.blur.isOpen()));
      this.backGround = this.add((new BooleanSetting("BackGround", false, () -> this.page.is(HUD.Page.Color))).setParent());
      this.width = this.add(new SliderSetting("Width", (double)0.0F, (double)0.0F, (double)15.0F, () -> this.page.is(HUD.Page.Color) && this.backGround.isOpen()));
      this.bgColor = this.add(new ColorSetting("BGColor", new Color(0, 0, 0, 178), () -> this.page.is(HUD.Page.Color) && this.backGround.isOpen()));
      this.rect = this.add((new ColorSetting("Rect", new Color(208, 0, 0), () -> this.page.is(HUD.Page.Color))).injectBoolean(false));
      this.glow = this.add((new ColorSetting("Glow", new Color(208, 0, 100), () -> this.page.is(HUD.Page.Color))).injectBoolean(false));
      this.decimal = new DecimalFormat("0.0");
      this.infoList = new ArrayList();
      this.moduleList = new ArrayList();
      this.setChinese("界面");
      INSTANCE = this;
      Arc.EVENT_BUS.subscribe(new InitHandler());

      // 用注册表的公开 API 取引用。旧写法依赖 SimpleRegistry 的内部字段 valueToEntry，
      // 那个字段在 1.21 已不存在，accessor 取回 null 后被 catch 静默吞掉，药水条目一条都没加进列表。
      for(StatusEffect potionEffect : Registries.STATUS_EFFECT) {
         RegistryEntry<StatusEffect> effectEntry = Registries.STATUS_EFFECT.getEntry(potionEffect);
         if (effectEntry == null) {
            continue;
         }

         this.infoList.add(new Info(() -> {
            if (mc.player == null) {
               return "";
            } else {
               StatusEffectInstance effect = mc.player.getStatusEffect(effectEntry);
               if (effect == null) {
                  return "";
               } else {
                  String name = potionEffect.getName().getString();
                  String s = name + " " + (effect.getAmplifier() + 1);
                  String s2 = getDuration(effect);
                  return s + " §f" + s2;
               }
            }
         }, () -> mc.player != null && mc.player.hasStatusEffect(effectEntry) && this.potions.getValue(), potionEffect::getColor, effectEntry));
      }

      ArrayList var10000 = this.infoList;
      Callable var10004 = () -> {
         String brand = mc.isInSingleplayer() ? "Vanilla" : mc.getNetworkHandler().getBrand().replaceAll("\\(.*?\\)", "");
         return "ServerBrand §f" + brand;
      };
      BooleanSetting var10005 = this.brand;
      Objects.requireNonNull(var10005);
      var10000.add(new Info(var10004, var10005::getValue));
      var10000 = this.infoList;
      var10004 = () -> {
         String ip = mc.isInSingleplayer() ? "SinglePlayer" : mc.getCurrentServerEntry().address;
         return "Server §f" + ip;
      };
      var10005 = this.ip;
      Objects.requireNonNull(var10005);
      var10000.add(new Info(var10004, var10005::getValue));
      var10000 = this.infoList;
      var10004 = () -> {
         float tps = Arc.SERVER.getTPS();
         return "TPS §f" + tps + " [" + Arc.SERVER.getCurrentTPS() + "]";
      };
      var10005 = this.tps;
      Objects.requireNonNull(var10005);
      var10000.add(new Info(var10004, var10005::getValue));
      var10000 = this.infoList;
      var10004 = () -> {
         double x = mc.player.getX() - mc.player.prevX;
         double z = mc.player.getZ() - mc.player.prevZ;
         double dist = Math.sqrt(x * x + z * z) / (double)1000.0F;
         double div = 1.388888888888889E-5;
         float timer = Arc.TIMER.get();
         double playerSpeed = dist / div * (double)timer;
         return String.format("Speed §f%skm/h", this.decimal.format(playerSpeed));
      };
      var10005 = this.speed;
      Objects.requireNonNull(var10005);
      var10000.add(new Info(var10004, var10005::getValue));
      var10000 = this.infoList;
      var10004 = () -> {
         SimpleDateFormat format = new SimpleDateFormat("h:mm keyCodec", Locale.ENGLISH);
         Date date = new Date();
         return "Time §f" + format.format(date);
      };
      var10005 = this.time;
      Objects.requireNonNull(var10005);
      var10000.add(new Info(var10004, var10005::getValue));
      var10000 = this.infoList;
      var10004 = () -> {
         PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
         Object playerPing = playerListEntry == null ? "Unknown" : playerListEntry.getLatency() + "ms";
         return "Ping §f" + (String)playerPing;
      };
      var10005 = this.ping;
      Objects.requireNonNull(var10005);
      var10000.add(new Info(var10004, var10005::getValue));
      var10000 = this.infoList;
      var10004 = () -> "FPS §f" + Arc.FPS.getFps();
      var10005 = this.fps;
      Objects.requireNonNull(var10005);
      var10000.add(new Info(var10004, var10005::getValue));
   }

   public static String getDuration(StatusEffectInstance pe) {
      if (pe.isInfinite()) {
         return "∞";
      } else {
         int var1 = pe.getDuration();
         int mins = var1 / 1200;
         int sec = var1 % 1200 / 20;
         return String.format("%d:%02d", mins, sec);
      }
   }

   private String formatWaterMark(String text) {
      return Arc.formatBrand(text);
   }

   public void onRender2D(DrawContext drawContext, float tickDelta) {
      if (this.waterMark.getValue()) {
         // 水印和模块列表（ArrayList）用同一套取色：ColorMode/Color/SecondColor 改了水印立刻跟着变
         TextUtil.drawString(
            drawContext,
            this.formatWaterMark(this.waterMarkString.getValue()),
            (double)this.waterMarkOffset.getValueInt(),
            (double)this.waterMarkOffset.getValueInt(),
            this.getColor(0.0),
            this.font.getValue(),
            this.shadow.getValue()
         );
      }

      int fontHeight = this.getHeight();
      if (this.coords.getValue()) {
         String coordsString = getCoords();
         this.drawCoord(drawContext, coordsString, mc.getWindow().getScaledHeight() - fontHeight - (mc.currentScreen instanceof ChatScreen ? 15 : 0));
      }

      HUD.Info.onRender(drawContext, this.infoList, this.renderingUp.getValue());
      if (this.arrayList.getValue()) {
         HUD.Info.onRender(drawContext, this.moduleList, !this.renderingUp.getValue());
      }

   }

   @EventListener(
      priority = -999
   )
   public void onUpdate(ClientTickEvent event) {
      if (!nullCheck()) {
         if (event.isPost()) {
            HUD.Info.onUpdate(this.infoList, this.sort.getValue());
            if (this.arrayList.getValue()) {
               HUD.Info.onUpdate(this.moduleList, this.listSort.getValue());
            }
         }

      }
   }

   private static String getCoords() {
      boolean inNether = mc.world.getRegistryKey().equals(World.NETHER);
      int posX = mc.player.getBlockX();
      int posY = mc.player.getBlockY();
      int posZ = mc.player.getBlockZ();
      float factor = !inNether ? 0.125F : 8.0F;
      int anotherWorldX = (int)(mc.player.getX() * (double)factor);
      int anotherWorldZ = (int)(mc.player.getZ() * (double)factor);
      String var10000 = inNether ? posX + ", " + posY + ", " + posZ + " §7[§f" + anotherWorldX + ", " + anotherWorldZ + "§7]§f" : posX + ", " + posY + ", " + posZ + "§7 [§f" + anotherWorldX + ", " + anotherWorldZ + "§7]";
      return "XYZ §f" + var10000;
   }

   private int getWidth(String s) {
      if (this.lowerCase.getValue()) {
         s = s.toLowerCase();
      }

      return this.font.getValue() ? (int)FontManager.ui.getWidth(s) : mc.textRenderer.getWidth(s);
   }

   private int getHeight() {
      if (this.font.getValue()) {
         // 用紧凑行高，不要用 getFontHeight()（那是字体的逻辑行高，含字内留白，会让行间隙变得很宽）
         return Math.max(1, (int)FontManager.ui.getLineHeight());
      } else {
         Objects.requireNonNull(mc.textRenderer);
         return 9;
      }
   }

   private void drawCoord(DrawContext drawContext, String s, int y) {
      if (this.colorSync.getValue()) {
         if (this.lowerCase.getValue()) {
            s = s.toLowerCase();
         }

         TextUtil.drawString(drawContext, s, (double)2.0F, (double)y, this.getColor((double)20.0F), this.font.getValue(), this.shadow.getValue());
      } else if (this.endColor.booleanValue) {
         TextUtil.drawStringPulse(drawContext, s, (double)2.0F, (double)y, this.color.getValue(), this.endColor.getValue(), this.pulseSpeed.getValue(), this.pulseCounter.getValueInt(), this.font.getValue(), this.shadow.getValue());
      } else {
         TextUtil.drawString(drawContext, s, (double)2.0F, (double)y, this.color.getValue().getRGB(), this.font.getValue(), this.shadow.getValue());
      }

   }

   public int getColor(double counter) {
      return this.colorMode.getValue() != HUD.ColorMode.Custom ? this.rainbow(counter).getRGB() : this.color.getValue().getRGB();
   }

   private Color rainbow(double delay) {
      if (this.colorMode.getValue() == HUD.ColorMode.Pulse) {
         return this.endColor.booleanValue ? ColorUtil.pulseColor(this.color.getValue(), this.endColor.getValue(), delay, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue()) : ColorUtil.pulseColor(this.color.getValue(), delay, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue());
      } else if (this.colorMode.getValue() == HUD.ColorMode.Rainbow) {
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
         Objects.requireNonNull(mc.textRenderer);
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
         for(Module module : Arc.MODULE.getModules()) {
            ArrayList var10000 = HUD.this.moduleList;
            HUD var10003 = HUD.this;
            Objects.requireNonNull(module);
            var10000.add(var10003.new Info(module::getArrayName, () -> module.isOn() && module.drawn.getValue()));
         }

         Arc.EVENT_BUS.unsubscribe(this);
      }
   }

   public class Info {
      public final Callable<String> info;
      public String string;
      public final BooleanSupplier drawn;
      // 药水条目：颜色来源（效果自身的颜色）与状态效果引用（用来取图标精灵）
      public final Supplier<Integer> colorProvider;
      public final RegistryEntry<StatusEffect> effectEntry;
      public double currentX = (double)0.0F;
      public boolean isOn;
      public final Animation animation = new Animation();
      public final Animation fadeAnimation = new Animation();
      static double fontHeight;
      static double currentY;
      static int windowWidth;
      static boolean fromUp;
      static double counter;

      public Info(Callable<String> info, BooleanSupplier drawn) {
         this(info, drawn, (Supplier<Integer>)null, (RegistryEntry<StatusEffect>)null);
      }

      public Info(Callable<String> info, BooleanSupplier drawn, Supplier<Integer> colorProvider, RegistryEntry<StatusEffect> effectEntry) {
         this.info = info;
         this.drawn = drawn;
         this.colorProvider = colorProvider;
         this.effectEntry = effectEntry;

         try {
            this.string = (String)this.info.call();
         } catch (Exception var5) {
         }

      }

      public static void onRender(DrawContext context, List<Info> list, boolean fromUp) {
         counter = (double)20.0F;
         HUD.Info.fromUp = fromUp;
         // 行距必须用紧凑行高（字号 + 1），不能用 getFontHeight()：
         // 后者是 Java2D 的逻辑行高（ascent + descent + leading），8px 字体量出来 11~12px，
         // 逐行排下来就是「模块列表文字 y 间隙大」的真正原因
         fontHeight = (double)HUD.INSTANCE.getHeight();
         currentY = fromUp ? (double)1.0F + HUD.INSTANCE.yOffset.getValue() : (double)Wrapper.mc.getWindow().getScaledHeight() - fontHeight - (double)1.0F - (Wrapper.mc.currentScreen instanceof ChatScreen && HUD.INSTANCE.yOffset.getValue() < (double)12.0F ? (double)12.0F - HUD.INSTANCE.yOffset.getValue() + HUD.INSTANCE.interval.getValue() / (double)2.0F : (double)0.0F) - HUD.INSTANCE.yOffset.getValue();
         windowWidth = Wrapper.mc.getWindow().getScaledWidth();

         for(Info s : list) {
            s.draw(context);
         }

      }

      public static void onUpdate(List<Info> list, boolean sort) {
         for(Info s : list) {
            s.onUpdate();
         }

         if (sort) {
            list.sort(Comparator.comparingInt((info) -> info.string == null ? 0 : -HUD.INSTANCE.getWidth(info.string)));
         }

      }

      public void onUpdate() {
         this.isOn = this.drawn.getAsBoolean();
         if (this.isOn) {
            try {
               this.string = HUD.this.lowerCase.getValue() ? ((String)this.info.call()).toLowerCase() : (String)this.info.call();
            } catch (Exception e) {
               e.printStackTrace();
            }
         }

      }

      public void draw(DrawContext context) {
         if (this.currentX > (double)0.0F || this.isOn) {
            boolean hasIcon = this.effectEntry != null && HUD.this.potionsIcon.getValue();
            int iconSize = (int)fontHeight;
            int iconSpacing = 2;
            int extraWidth = hasIcon ? iconSize + iconSpacing : 0;
            this.currentX = this.animation.get(this.isOn ? (double)(HUD.this.getWidth(this.string) + 1 + extraWidth) : (double)0.0F, this.isOn ? (long)HUD.this.enableLength.getValueInt() : (long)HUD.this.disableLength.getValueInt(), HUD.this.easing.getValue());
            double width = this.currentX + (double)HUD.this.xOffset.getValueFloat();
            double fade = this.fadeAnimation.get(this.isOn ? (double)1.0F : (double)0.0F, (long)HUD.this.fadeLength.getValueInt(), HUD.this.easing.getValue());
            if (fade > 0.04) {
               counter += fromUp ? fade : -fade;
               int baseColor = hasIcon && this.colorProvider != null ? this.colorProvider.get() : HUD.this.getColor(counter);
               int c = ColorUtil.injectAlpha(baseColor, (int)((double)HUD.this.color.getValue().getAlpha() * fade));
               if (HUD.this.blur.getValue()) {
                  Arc.BLUR.applyBlur((float)(HUD.this.radius.getValue() * fade), (float)((double)windowWidth - width - (double)(HUD.this.width.getValueFloat() / 2.0F)), (float)currentY - 1.0F - HUD.this.interval.getValueFloat() / 2.0F, (float)width + HUD.this.width.getValueFloat() - HUD.this.xOffset.getValueFloat(), (float)fontHeight + HUD.this.interval.getValueFloat());
               }

               if (HUD.this.backGround.getValue()) {
                  Render2DUtil.drawRect(context.getMatrices(), (float)((double)windowWidth - width - (double)(HUD.this.width.getValueFloat() / 2.0F)), (float)currentY - 1.0F - HUD.this.interval.getValueFloat() / 2.0F, (float)width + HUD.this.width.getValueFloat() - HUD.this.xOffset.getValueFloat(), (float)fontHeight + HUD.this.interval.getValueFloat(), ColorUtil.injectAlpha(HUD.this.bgColor.rainbow ? c : HUD.this.bgColor.getValue().getRGB(), (int)((double)HUD.this.bgColor.getValue().getAlpha() * fade)));
               }

               if (HUD.this.glow.booleanValue) {
                  Render2DUtil.drawGlow(context.getMatrices(), (float)((double)windowWidth - width - (double)(HUD.this.width.getValueFloat() / 2.0F)), (float)currentY - 1.0F - HUD.this.interval.getValueFloat() / 2.0F, (float)width + HUD.this.width.getValueFloat() - HUD.this.xOffset.getValueFloat(), (float)fontHeight + HUD.this.interval.getValueFloat(), ColorUtil.injectAlpha(HUD.this.glow.rainbow ? c : HUD.this.glow.getValue().getRGB(), (int)((double)HUD.this.glow.getValue().getAlpha() * fade)));
               }

               double textX = (double)windowWidth - width;
               if (hasIcon) {
                  // 药水图标用原版状态效果精灵，尺寸跟行高一致
                  Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(this.effectEntry);
                  context.drawSprite((int)textX, (int)(currentY + (double)HUD.this.textOffset.getValueFloat() - 1.0), 0, iconSize, iconSize, sprite);
                  textX += (double)(iconSize + iconSpacing);
               }

               TextUtil.drawString(context, this.string, textX, currentY + (double)HUD.this.textOffset.getValueFloat(), c, HUD.this.font.getValue(), HUD.this.shadow.getValue());
               if (HUD.this.rect.booleanValue) {
                  Render2DUtil.drawRect(context.getMatrices(), (float)windowWidth + HUD.this.width.getValueFloat() / 2.0F - HUD.this.xOffset.getValueFloat(), (float)currentY - 1.0F - HUD.this.interval.getValueFloat() / 2.0F, 1.0F, (float)fontHeight + HUD.this.interval.getValueFloat(), HUD.this.rect.rainbow ? c : ColorUtil.injectAlpha(HUD.this.rect.getValue(), (int)((double)HUD.this.rect.getValue().getAlpha() * fade)).getRGB());
               }

               currentY += fromUp ? (fontHeight + HUD.this.interval.getValue()) * fade : -(fontHeight + HUD.this.interval.getValue()) * fade;
            }
         }

      }
   }
}
