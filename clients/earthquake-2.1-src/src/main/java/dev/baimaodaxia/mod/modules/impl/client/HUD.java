package dev.baimaodaxia.mod.modules.impl.client;

import dev.baimaodaxia.earthquake;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.ClientTickEvent;
import dev.baimaodaxia.api.events.impl.InitEvent;
import dev.baimaodaxia.api.utils.Wrapper;
import dev.baimaodaxia.api.utils.math.Animation;
import dev.baimaodaxia.api.utils.math.Easing;
import dev.baimaodaxia.api.utils.render.ColorUtil;
import dev.baimaodaxia.api.utils.render.Render2DUtil;
import dev.baimaodaxia.api.utils.render.TextUtil;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.ColorSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import dev.baimaodaxia.mod.modules.settings.impl.StringSetting;
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
import net.minecraft.client.gui.Element;
import net.minecraft.client.realms.Ping;
import net.minecraft.potion.Potions;
import net.minecraft.world.gen.YOffset;

public class HUD extends Module {
   public static HUD INSTANCE;
   public final EnumSetting<Page> page;
   public final BooleanSetting renderingUp;
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
   public final BooleanSetting armor;
   public final SliderSetting armorOffset;
   public final BooleanSetting durability;
   public final BooleanSetting waterMark;
   public final ColorSetting pulse;
   public final StringSetting waterMarkString;
   public final SliderSetting waterMarkOffset;
   public final BooleanSetting fps;
   public final BooleanSetting ping;
   public final BooleanSetting tps;
   public final BooleanSetting packets;
   public final BooleanSetting ip;
   public final BooleanSetting time;
   public final BooleanSetting speed;
   public final BooleanSetting brand;
   public final BooleanSetting potions;
   public final BooleanSetting coords;
   public final BooleanSetting colorSync;
   public final ColorSetting color;
   private final EnumSetting<ColorMode> colorMode;
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
   private final SliderSetting borderRadius;
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
      this.armor = this.add((new BooleanSetting("Armor", true, () -> this.page.is(HUD.Page.Element))).setParent());
      this.armorOffset = this.add(new SliderSetting("ArmorOffset", (double)1.0F, (double)0.0F, (double)100.0F, (double)-1.0F, () -> this.page.is(HUD.Page.Element) && this.armor.isOpen()));
      this.durability = this.add(new BooleanSetting("Durability", true, () -> this.page.is(HUD.Page.Element) && this.armor.isOpen()));
      this.waterMark = this.add((new BooleanSetting("WaterMark", false, () -> this.page.is(HUD.Page.Element))).setParent());
      this.pulse = this.add((new ColorSetting("Pulse", new Color(0), () -> this.page.is(HUD.Page.Element) && this.waterMark.isOpen())).injectBoolean(true));
      this.waterMarkString = this.add(new StringSetting("Title", "%hackname% %version%", () -> this.page.is(HUD.Page.Element) && this.waterMark.isOpen()));
      this.waterMarkOffset = this.add(new SliderSetting("Offset", (double)1.0F, (double)0.0F, (double)100.0F, (double)-1.0F, () -> this.page.is(HUD.Page.Element) && this.waterMark.isOpen()));
      this.fps = this.add(new BooleanSetting("FPS", true, () -> this.page.is(HUD.Page.Element)));
      this.ping = this.add(new BooleanSetting("Ping", true, () -> this.page.is(HUD.Page.Element)));
      this.tps = this.add(new BooleanSetting("TPS", true, () -> this.page.is(HUD.Page.Element)));
      this.packets = this.add(new BooleanSetting("Packets", false, () -> this.page.is(HUD.Page.Element)));
      this.ip = this.add(new BooleanSetting("IP", false, () -> this.page.is(HUD.Page.Element)));
      this.time = this.add(new BooleanSetting("Time", false, () -> this.page.is(HUD.Page.Element)));
      this.speed = this.add(new BooleanSetting("Speed", true, () -> this.page.is(HUD.Page.Element)));
      this.brand = this.add(new BooleanSetting("Brand", false, () -> this.page.is(HUD.Page.Element)));
      this.potions = this.add(new BooleanSetting("Potions", true, () -> this.page.is(HUD.Page.Element)));
      this.coords = this.add((new BooleanSetting("Coords", true, () -> this.page.is(HUD.Page.Element))).setParent());
      this.colorSync = this.add(new BooleanSetting("ColorSync", true, () -> this.page.is(HUD.Page.Element) && this.coords.isOpen()));
      this.color = this.add(new ColorSetting("Color", new Color(16711680), () -> this.page.is(HUD.Page.Color)));
      this.colorMode = this.add(new EnumSetting("ColorMode", HUD.ColorMode.Pulse, () -> this.page.is(HUD.Page.Color)));
      this.rainbowSpeed = this.add(new SliderSetting("RainbowSpeed", (double)4.0F, (double)1.0F, (double)10.0F, 0.1, () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Rainbow));
      this.saturation = this.add(new SliderSetting("Saturation", (double)130.0F, (double)1.0F, (double)255.0F, () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Rainbow));
      this.rainbowDelay = this.add(new SliderSetting("Delay", (double)500.0F, (double)0.0F, (double)1000.0F, () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Rainbow));
      this.endColor = this.add((new ColorSetting("SecondColor", new Color(0), () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Pulse)).injectBoolean(true));
      this.pulseSpeed = this.add(new SliderSetting("PulseSpeed", (double)1.0F, (double)0.0F, (double)5.0F, 0.1, () -> this.page.is(HUD.Page.Color)));
      this.pulseCounter = this.add(new SliderSetting("Counter", 10, 1, 50, () -> this.page.is(HUD.Page.Color)));
      this.blur = this.add((new BooleanSetting("Blur", false, () -> this.page.is(HUD.Page.Color))).setParent());
      this.radius = this.add(new SliderSetting("Radius", (double)10.0F, (double)0.0F, (double)100.0F, () -> this.page.is(HUD.Page.Color) && this.blur.isOpen()));
      this.backGround = this.add((new BooleanSetting("BackGround", false, () -> this.page.is(HUD.Page.Color))).setParent());
      this.width = this.add(new SliderSetting("Width", (double)0.0F, (double)0.0F, (double)15.0F, () -> this.page.is(HUD.Page.Color) && this.backGround.isOpen()));
      this.borderRadius = this.add(new SliderSetting("BorderRadius", (double)4.0F, (double)0.0F, (double)20.0F, 0.1, () -> this.page.is(HUD.Page.Color) && this.backGround.isOpen()));
      this.bgColor = this.add(new ColorSetting("BGColor", new Color(0, 0, 0, 100), () -> this.page.is(HUD.Page.Color) && this.backGround.isOpen()));
      this.rect = this.add((new ColorSetting("Rect", new Color(16711680, true), () -> this.page.is(HUD.Page.Color))).injectBoolean(false));
      this.glow = this.add((new ColorSetting("Glow", new Color(16711680, true), () -> this.page.is(HUD.Page.Color))).injectBoolean(false));
      this.decimal = new DecimalFormat("0.0");
      this.infoList = new ArrayList();
      this.moduleList = new ArrayList();
      this.setChinese("界面");
      INSTANCE = this;
      earthquake.EVENT_BUS.subscribe(new InitHandler());
      ArrayList var10000 = this.infoList;
      Callable var10004 = () -> {
         int var10001 = FakeFPS.INSTANCE.isOn() ? FakeFPS.INSTANCE.getFakeFPS() : earthquake.FPS.getFps();
         return "FPS §f" + var10001;
      };
      BooleanSetting var10005 = this.fps;
      Objects.requireNonNull(var10005);
      var10000.add(new Info(var10004, var10005::getValue));
      var10000 = this.infoList;
      var10004 = () -> {
         float var10002 = earthquake.SERVER.getTPS();
         return "TPS §f" + var10002 + " [" + earthquake.SERVER.getCurrentTPS() + "]";
      };
      var10005 = this.tps;
      Objects.requireNonNull(var10005);
      var10000.add(new Info(var10004, var10005::getValue));
      var10000 = this.infoList;
      var10004 = () -> {
         int out = earthquake.NETWORK.getOutgoingPPS();
         int in = earthquake.NETWORK.getIncomingPPS();
         return "Packets §f" + out + "->" + in;
      };
      var10005 = this.packets;
      Objects.requireNonNull(var10005);
      var10000.add(new Info(var10004, var10005::getValue));
      var10000 = this.infoList;
      var10004 = () -> {
         PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
         String pingStr = entry == null ? "Unknown" : entry.getLatency() + "ms";
         return "Ping §f" + pingStr;
      };
      var10005 = this.ping;
      Objects.requireNonNull(var10005);
      var10000.add(new Info(var10004, var10005::getValue));
      var10000 = this.infoList;
      var10004 = () -> {
         double dx = mc.player.getX() - mc.player.lastRenderX;
         double dz = mc.player.getZ() - mc.player.lastRenderZ;
         double dist = Math.sqrt(dx * dx + dz * dz) / (double)1000.0F;
         float timer = earthquake.TIMER.get();
         double speed = dist / 1.388888888888889E-5 * (double)timer;
         return String.format("Speed §f%skm/h", this.decimal.format(speed));
      };
      var10005 = this.speed;
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
         SimpleDateFormat var10003 = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
         Date date10004 = new Date();
         return "Time §f" + var10003.format(date10004);
      };
      var10005 = this.time;
      Objects.requireNonNull(var10005);
      var10000.add(new Info(var10004, var10005::getValue));
      var10000 = this.infoList;
      var10004 = () -> {
         String brand = mc.isInSingleplayer() ? "Vanilla" : mc.getNetworkHandler().getBrand();
         if (brand == null) {
            brand = "Unknown";
         }

         return "ServerBrand §f" + brand.replaceAll("\\(.*?\\)", "");
      };
      var10005 = this.brand;
      Objects.requireNonNull(var10005);
      var10000.add(new Info(var10004, var10005::getValue));

      for(StatusEffect effect : Registries.STATUS_EFFECT) {
         try {
            RegistryEntry<StatusEffect> entry = Registries.STATUS_EFFECT.getEntry(effect);
            int potionColor = effect.getColor();
            this.infoList.add(new Info(() -> {
               StatusEffectInstance instance = mc.player.getStatusEffect(entry);
               if (instance != null) {
                  String name = effect.getName().getString();
                  int amplifier = instance.getAmplifier() + 1;
                  String duration = getDuration(instance);
                  return name + " " + amplifier + " §f" + duration;
               } else {
                  return "";
               }
            }, () -> mc.player.hasStatusEffect(entry) && this.potions.getValue(), () -> potionColor, entry));
         } catch (Exception var5) {
         }
      }

   }

   public static String getDuration(StatusEffectInstance instance) {
      if (instance.isInfinite()) {
         return "∞";
      } else {
         int ticks = instance.getDuration();
         int mins = ticks / 1200;
         int sec = ticks % 1200 / 20;
         return String.format("%d:%02d", mins, sec);
      }
   }

   public void onRender2D(DrawContext drawContext, float tickDelta) {
      if (this.waterMark.getValue()) {
         String text = this.waterMarkString.getValue().replace("%version%", "2.1").replace("%hackname%", "earthquake");
         if (this.pulse.booleanValue) {
            TextUtil.drawStringPulse(drawContext, text, (double)this.waterMarkOffset.getValueInt(), (double)this.waterMarkOffset.getValueInt(), this.color.getValue(), this.pulse.getValue(), this.pulseSpeed.getValue(), this.pulseCounter.getValueInt(), false, this.shadow.getValue());
         } else {
            TextUtil.drawString(drawContext, text, (double)this.waterMarkOffset.getValueInt(), (double)this.waterMarkOffset.getValueInt(), this.color.getValue().getRGB(), false, this.shadow.getValue());
         }
      }

      int fontHeight = this.getHeight();
      if (this.coords.getValue()) {
         String coordsString = getCoords();
         int y = mc.getWindow().getScaledHeight() - fontHeight;
         if (mc.currentScreen instanceof ChatScreen) {
            y -= 15;
         }

         this.drawCoord(drawContext, coordsString, y);
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
      if (!nullCheck() && event.isPost()) {
         HUD.Info.onUpdate(this.infoList, this.sort.getValue());
         if (this.arrayList.getValue()) {
            HUD.Info.onUpdate(this.moduleList, this.listSort.getValue());
         }
      }

   }

   private static String getCoords() {
      boolean inNether = mc.world.getRegistryKey() == World.NETHER;
      int posX = mc.player.getBlockX();
      int posY = mc.player.getBlockY();
      int posZ = mc.player.getBlockZ();
      float factor = inNether ? 8.0F : 0.125F;
      int anotherWorldX = (int)(mc.player.getX() * (double)factor);
      int anotherWorldZ = (int)(mc.player.getZ() * (double)factor);
      return inNether ? String.format("XYZ §f%d, %d, %d §7[§f%d, %d§7]§f", posX, posY, posZ, anotherWorldX, anotherWorldZ) : String.format("XYZ §f%d, %d, %d§7 [§f%d, %d§7]", posX, posY, posZ, anotherWorldX, anotherWorldZ);
   }

   private int getWidth(String s) {
      if (this.lowerCase.getValue()) {
         s = s.toLowerCase();
      }

      return mc.textRenderer.getWidth(s);
   }

   private int getHeight() {
      return 9;
   }

   private void drawCoord(DrawContext drawContext, String s, int y) {
      if (this.lowerCase.getValue()) {
         s = s.toLowerCase();
      }

      if (this.colorSync.getValue()) {
         TextUtil.drawString(drawContext, s, (double)2.0F, (double)y, this.getColor((double)20.0F), false, this.shadow.getValue());
      } else if (this.pulse.booleanValue) {
         TextUtil.drawStringPulse(drawContext, s, (double)2.0F, (double)y, this.color.getValue(), this.pulse.getValue(), this.pulseSpeed.getValue(), this.pulseCounter.getValueInt(), false, this.shadow.getValue());
      } else {
         TextUtil.drawString(drawContext, s, (double)2.0F, (double)y, this.color.getValue().getRGB(), false, this.shadow.getValue());
      }

   }

   public int getColor(double counter) {
      return this.colorMode.getValue() == HUD.ColorMode.Custom ? this.color.getValue().getRGB() : this.rainbow(counter).getRGB();
   }

   private Color rainbow(double delay) {
      if (this.colorMode.getValue() == HUD.ColorMode.Pulse) {
         return this.endColor.booleanValue ? ColorUtil.pulseColor(this.color.getValue(), this.endColor.getValue(), delay, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue()) : ColorUtil.pulseColor(this.color.getValue(), delay, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue());
      } else if (this.colorMode.getValue() == HUD.ColorMode.Rainbow) {
         double state = Math.ceil(((double)System.currentTimeMillis() * this.rainbowSpeed.getValue() + delay * this.rainbowDelay.getValue()) / (double)20.0F);
         return Color.getHSBColor((float)(state % (double)360.0F / (double)360.0F), this.saturation.getValueFloat() / 255.0F, 1.0F);
      } else {
         return this.color.getValue();
      }
   }

   private int getFontHeight() {
      return 9;
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

   public static enum Page {
      General,
      Element,
      Color;

      // $FF: synthetic method
      private static Page[] $values() {
         return new Page[]{General, Element, Color};
      }
   }

   public class Info {
      public final Callable<String> info;
      public String string;
      public final BooleanSupplier drawn;
      public final Supplier<Integer> colorProvider;
      public RegistryEntry<StatusEffect> effectEntry;
      public boolean isOn;
      static double fontHeight;
      static double currentY;
      static int windowWidth;
      static boolean fromUp;
      static double counter;
      public double currentX;
      public final Animation animation;
      public final Animation fadeAnimation;

      public Info(Callable<String> info, BooleanSupplier drawn) {
         this(info, drawn, (Supplier)null, (RegistryEntry)null);
      }

      public Info(Callable<String> info, BooleanSupplier drawn, Supplier<Integer> colorProvider) {
         this(info, drawn, colorProvider, (RegistryEntry)null);
      }

      public Info(Callable<String> info, BooleanSupplier drawn, Supplier<Integer> colorProvider, RegistryEntry<StatusEffect> effectEntry) {
         this.currentX = (double)0.0F;
         this.animation = new Animation();
         this.fadeAnimation = new Animation();
         this.info = info;
         this.drawn = drawn;
         this.colorProvider = colorProvider;
         this.effectEntry = effectEntry;

         try {
            this.string = (String)info.call();
         } catch (Exception var7) {
         }

      }

      public static void onRender(DrawContext context, List<Info> list, boolean fromUpFlag) {
         counter = (double)20.0F;
         fromUp = fromUpFlag;
         fontHeight = (double)HUD.INSTANCE.getFontHeight();
         windowWidth = Wrapper.mc.getWindow().getScaledWidth();
         if (fromUpFlag) {
            currentY = (double)1.0F + HUD.INSTANCE.yOffset.getValue();

            for(Info info : list) {
               info.draw(context);
            }
         } else {
            double baseY = (double)Wrapper.mc.getWindow().getScaledHeight() - fontHeight - (double)1.0F;
            double offset = (double)0.0F;
            if (Wrapper.mc.currentScreen instanceof ChatScreen && HUD.INSTANCE.yOffset.getValue() < (double)12.0F) {
               offset = (double)12.0F - HUD.INSTANCE.yOffset.getValue() + HUD.INSTANCE.interval.getValue() / (double)2.0F;
            }

            currentY = baseY - offset - HUD.INSTANCE.yOffset.getValue();

            for(int i = list.size() - 1; i >= 0; --i) {
               ((Info)list.get(i)).draw(context);
            }
         }

      }

      public static void onUpdate(List<Info> list, boolean sort) {
         for(Info info : list) {
            info.onUpdate();
         }

         if (sort) {
            list.sort(Comparator.comparingInt((i) -> i.string == null ? 0 : -HUD.INSTANCE.getWidth(i.string)));
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
            boolean hasIcon = this.effectEntry != null;
            int iconSize = (int)fontHeight;
            int iconSpacing = 2;
            int extraWidth = hasIcon ? iconSize + iconSpacing : 0;
            this.currentX = this.animation.get(this.isOn ? (double)(HUD.this.getWidth(this.string) + 1 + extraWidth) : (double)0.0F, this.isOn ? (long)HUD.this.enableLength.getValueInt() : (long)HUD.this.disableLength.getValueInt(), HUD.this.easing.getValue());
            double width = this.currentX + (double)HUD.this.xOffset.getValueFloat();
            double fade = this.fadeAnimation.get(this.isOn ? (double)1.0F : (double)0.0F, (long)HUD.this.fadeLength.getValueInt(), HUD.this.easing.getValue());
            if (fade > 0.04) {
               counter += fromUp ? fade : -fade;
               int baseColor;
               if (this.colorProvider != null) {
                  baseColor = (Integer)this.colorProvider.get();
               } else {
                  baseColor = HUD.this.getColor(counter);
               }

               int c = ColorUtil.injectAlpha(baseColor, (int)((double)HUD.this.color.getValue().getAlpha() * fade));
               float bgX = (float)((double)windowWidth - width - (double)HUD.this.width.getValueFloat() / (double)2.0F);
               float bgY = (float)(currentY - (double)1.0F - (double)HUD.this.interval.getValueFloat() / (double)2.0F);
               float bgWidth = (float)width + HUD.this.width.getValueFloat() - HUD.this.xOffset.getValueFloat();
               float bgHeight = (float)fontHeight + HUD.this.interval.getValueFloat();
               if (HUD.this.blur.getValue()) {
                  earthquake.BLUR.applyBlur((float)(HUD.this.radius.getValue() * fade), bgX, bgY, bgWidth, bgHeight);
               }

               if (HUD.this.backGround.getValue()) {
                  int bgColorValue = ColorUtil.injectAlpha(HUD.this.bgColor.sync ? c : HUD.this.bgColor.getValue().getRGB(), (int)((double)HUD.this.bgColor.getValue().getAlpha() * fade));
                  Render2DUtil.drawRoundedRect(context.getMatrices(), bgX, bgY, bgWidth, bgHeight, HUD.this.borderRadius.getValueFloat(), new Color(bgColorValue, true));
               }

               if (HUD.this.glow.booleanValue) {
                  Render2DUtil.drawGlow(context.getMatrices(), bgX, bgY, bgWidth, bgHeight, ColorUtil.injectAlpha(HUD.this.glow.sync ? c : HUD.this.glow.getValue().getRGB(), (int)((double)HUD.this.glow.getValue().getAlpha() * fade)));
               }

               double textX = (double)windowWidth - width;
               if (hasIcon) {
                  Sprite sprite = Wrapper.mc.getStatusEffectSpriteManager().getSprite(this.effectEntry);
                  int iconX = (int)textX;
                  int iconY = (int)(currentY + (double)HUD.this.textOffset.getValueFloat());
                  context.drawSprite(iconX, iconY, 0, iconSize, iconSize, sprite);
                  textX += (double)(iconSize + iconSpacing);
               }

               TextUtil.drawString(context, this.string, textX, currentY + (double)HUD.this.textOffset.getValueFloat(), c, false, HUD.this.shadow.getValue());
               if (HUD.this.rect.booleanValue) {
                  Render2DUtil.drawRect(context.getMatrices(), (float)windowWidth + HUD.this.width.getValueFloat() / 2.0F - HUD.this.xOffset.getValueFloat(), (float)currentY - 1.0F - HUD.this.interval.getValueFloat() / 2.0F, 1.0F, (float)fontHeight + HUD.this.interval.getValueFloat(), HUD.this.rect.sync ? c : ColorUtil.injectAlpha(HUD.this.rect.getValue(), (int)((double)HUD.this.rect.getValue().getAlpha() * fade)).getRGB());
               }

               currentY += fromUp ? (fontHeight + HUD.this.interval.getValue()) * fade : -(fontHeight + HUD.this.interval.getValue()) * fade;
            }
         }

      }
   }

   public class InitHandler {
      @EventListener
      public void onInit(InitEvent event) {
         for(Module module : earthquake.MODULE.getModules()) {
            ArrayList var10000 = HUD.this.moduleList;
            HUD var10003 = HUD.this;
            Objects.requireNonNull(module);
            var10000.add(var10003.new Info(module::getArrayName, () -> module.isOn() && module.drawn.getValue()));
         }

         earthquake.EVENT_BUS.unsubscribe(this);
      }
   }
}
