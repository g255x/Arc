package dev.luminous.mod.modules.impl.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Nina;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.asm.accessors.ISimpleRegistry;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import net.minecraft.class_1291;
import net.minecraft.class_1293;
import net.minecraft.class_1294;
import net.minecraft.class_1937;
import net.minecraft.class_286;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_408;
import net.minecraft.class_4587;
import net.minecraft.class_640;
import net.minecraft.class_6880;
import net.minecraft.class_757;
import net.minecraft.class_7833;
import net.minecraft.class_7923;
import net.minecraft.class_293.class_5596;
import org.joml.Matrix4f;

public class HUD extends Module {
   public static HUD INSTANCE;
   private static final class_2960 TH_MINI_LOGO = class_2960.method_60655("alien", "textures/hud/icons/mini_logo.png");
   private static final class_2960 TH_PLAYER_ICON = class_2960.method_60655("alien", "textures/gui/headers/player.png");
   private static final class_2960 TH_SERVER_ICON = class_2960.method_60655("alien", "textures/hud/icons/server.png");
   private static final class_2960 TH_PROXY_ICON = class_2960.method_60655("alien", "textures/hud/icons/proxy.png");
   private static final class_2960 TH_BALTIKA = class_2960.method_60655("alien", "textures/hud/elements/baltika.png");
   public final EnumSetting page;
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
   public final EnumSetting easing;
   public final BooleanSetting armor;
   public final SliderSetting armorOffset;
   public final BooleanSetting durability;
   public final BooleanSetting waterMark;
   public final EnumSetting waterMarkMode;
   public final BooleanSetting thunderRU;
   public final ColorSetting pulse;
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
   private final EnumSetting potionMode;
   public final BooleanSetting coords;
   public final BooleanSetting colorSync;
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
   private final DecimalFormat decimal;
   private final ArrayList infoList;
   private final Map potionPanelInfos;
   private final Color potionPanelBgColor;
   private final String[] thunderRuNames;
   private int thunderRuIndex;
   private long thunderRuNextSwitchMs;

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
      this.armor = this.add((new BooleanSetting("Armor", true, () -> this.page.is(HUD.Page.Element))).setParent());
      this.armorOffset = this.add(new SliderSetting("ArmorOffset", (double)1.0F, (double)0.0F, (double)100.0F, (double)-1.0F, () -> this.page.is(HUD.Page.Element) && this.armor.isOpen()));
      this.durability = this.add(new BooleanSetting("Durability", true, () -> this.page.is(HUD.Page.Element) && this.armor.isOpen()));
      this.waterMark = this.add((new BooleanSetting("WaterMark", true, () -> this.page.is(HUD.Page.Element))).setParent());
      this.waterMarkMode = this.add(new EnumSetting("Mode", HUD.WaterMarkMode.Alien, () -> false));
      this.thunderRU = this.add(new BooleanSetting("RU", false, () -> false));
      this.pulse = this.add((new ColorSetting("Pulse", new Color(180, 140, 255), () -> false)).injectBoolean(false));
      this.waterMarkString = this.add(new StringSetting("Title", "Alien", () -> false));
      this.waterMarkOffset = this.add(new SliderSetting("Offset", (double)1.0F, (double)0.0F, (double)100.0F, (double)-1.0F, () -> this.page.is(HUD.Page.Element) && this.waterMark.isOpen()));
      this.fps = this.add(new BooleanSetting("FPS", true, () -> this.page.is(HUD.Page.Element)));
      this.ping = this.add(new BooleanSetting("Ping", true, () -> this.page.is(HUD.Page.Element)));
      this.tps = this.add(new BooleanSetting("TPS", true, () -> this.page.is(HUD.Page.Element)));
      this.ip = this.add(new BooleanSetting("IP", false, () -> this.page.is(HUD.Page.Element)));
      this.time = this.add(new BooleanSetting("Time", false, () -> this.page.is(HUD.Page.Element)));
      this.speed = this.add(new BooleanSetting("Speed", true, () -> this.page.is(HUD.Page.Element)));
      this.brand = this.add(new BooleanSetting("Brand", false, () -> this.page.is(HUD.Page.Element)));
      this.potions = this.add(new BooleanSetting("Potions", true, () -> this.page.is(HUD.Page.Element)));
      this.potionMode = this.add(new EnumSetting("PotionMode", HUD.PotionMode.List, () -> this.page.is(HUD.Page.Element) && this.potions.getValue()));
      this.coords = this.add((new BooleanSetting("Coords", true, () -> this.page.is(HUD.Page.Element))).setParent());
      this.colorSync = this.add(new BooleanSetting("ColorSync", true, () -> this.page.is(HUD.Page.Element) && this.coords.isOpen()));
      this.color = this.add(new ColorSetting("Color", new Color(210, 180, 255), () -> this.page.is(HUD.Page.Color)));
      this.colorMode = this.add(new EnumSetting("ColorMode", HUD.ColorMode.Pulse, () -> this.page.is(HUD.Page.Color)));
      this.rainbowSpeed = this.add(new SliderSetting("RainbowSpeed", (double)4.0F, (double)1.0F, (double)10.0F, 0.1, () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Rainbow));
      this.saturation = this.add(new SliderSetting("Saturation", (double)130.0F, (double)1.0F, (double)255.0F, () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Rainbow));
      this.rainbowDelay = this.add(new SliderSetting("Delay", 350, 0, 1000, () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Rainbow));
      this.endColor = this.add((new ColorSetting("SecondColor", new Color(180, 140, 255, 255), () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Pulse)).injectBoolean(true));
      this.pulseSpeed = this.add(new SliderSetting("PulseSpeed", (double)1.0F, (double)0.0F, (double)5.0F, 0.1, () -> this.page.is(HUD.Page.Color)));
      this.pulseCounter = this.add(new SliderSetting("Counter", 10, 1, 50, () -> this.page.is(HUD.Page.Color)));
      this.blur = this.add((new BooleanSetting("Blur", false, () -> this.page.is(HUD.Page.Color))).setParent());
      this.radius = this.add(new SliderSetting("Radius", (double)10.0F, (double)0.0F, (double)100.0F, () -> this.page.is(HUD.Page.Color) && this.blur.isOpen()));
      this.backGround = this.add((new BooleanSetting("BackGround", false, () -> this.page.is(HUD.Page.Color))).setParent());
      this.width = this.add(new SliderSetting("Width", (double)0.0F, (double)0.0F, (double)15.0F, () -> this.page.is(HUD.Page.Color) && this.backGround.isOpen()));
      this.bgColor = this.add(new ColorSetting("BGColor", new Color(0, 0, 0, 100), () -> this.page.is(HUD.Page.Color) && this.backGround.isOpen()));
      this.rect = this.add((new ColorSetting("Rect", new Color(210, 180, 255), () -> this.page.is(HUD.Page.Color))).injectBoolean(false));
      this.glow = this.add((new ColorSetting("Glow", new Color(200, 160, 255), () -> this.page.is(HUD.Page.Color))).injectBoolean(false));
      this.decimal = new DecimalFormat("0.0");
      this.infoList = new ArrayList();
      this.potionPanelInfos = new HashMap();
      this.potionPanelBgColor = new Color(0, 0, 0, 50);
      this.thunderRuNames = new String[]{"АльенХак", "АлиенКлиент", "АлиенСкид", "АлиенСтайл", "АлиенВзлом", "АлиенРекод", "АлиенХуд", "АлиенКлиент", "АлиенРелиз"};
      this.setChinese("界面");
      INSTANCE = this;

      for(class_1291 potionEffect : class_7923.field_41174) {
         try {
            class_6880 effectRegistryEntry = (class_6880)((ISimpleRegistry)class_7923.field_41174).getValueToEntry().get(potionEffect);
            this.infoList.add(new Info(() -> {
               class_1293 effect = mc.field_1724.method_6112(effectRegistryEntry);
               if (effect != null) {
                  String var10000 = potionEffect.method_5560().getString();
                  String s = var10000 + " " + (effect.method_5578() + 1);
                  String s2 = getDuration(effect);
                  return s + " §f" + s2;
               } else {
                  return "";
               }
            }, () -> mc.field_1724.method_6059(effectRegistryEntry) && this.potions.getValue() && this.potionMode.getValue() == HUD.PotionMode.List));
         } catch (Exception var4) {
         }
      }

      ArrayList var10000 = this.infoList;
      Callable var10004 = () -> {
         String var10000 = mc.method_1542() ? "Vanilla" : mc.method_1562().method_52790().replaceAll("\\(.*?\\)", "");
         return "ServerBrand §f" + var10000;
      };
      BooleanSetting var10005 = this.brand;
      Objects.requireNonNull(var10005);
      var10000.add(new Info(var10004, var10005::getValue));
      var10000 = this.infoList;
      var10004 = () -> {
         String var10000 = mc.method_1542() ? "SinglePlayer" : mc.method_1558().field_3761;
         return "Server §f" + var10000;
      };
      var10005 = this.ip;
      Objects.requireNonNull(var10005);
      var10000.add(new Info(var10004, var10005::getValue));
      var10000 = this.infoList;
      var10004 = () -> {
         float var10000 = Nina.SERVER.getTPS();
         return "TPS §f" + var10000 + " [" + Nina.SERVER.getCurrentTPS() + "]";
      };
      var10005 = this.tps;
      Objects.requireNonNull(var10005);
      var10000.add(new Info(var10004, var10005::getValue));
      var10000 = this.infoList;
      var10004 = () -> {
         double x = mc.field_1724.method_23317() - mc.field_1724.field_6014;
         double z = mc.field_1724.method_23321() - mc.field_1724.field_5969;
         double dist = Math.sqrt(x * x + z * z) / (double)1000.0F;
         double div = 1.388888888888889E-5;
         float timer = Nina.TIMER.get();
         double playerSpeed = dist / div * (double)timer;
         return String.format("Speed §f%skm/h", this.decimal.format(playerSpeed));
      };
      var10005 = this.speed;
      Objects.requireNonNull(var10005);
      var10000.add(new Info(var10004, var10005::getValue));
      var10000 = this.infoList;
      var10004 = () -> {
         SimpleDateFormat var10000 = new SimpleDateFormat("h:mm keyCodec", Locale.ENGLISH);
         Date var10001 = new Date();
         return "Time §f" + var10000.format(var10001);
      };
      var10005 = this.time;
      Objects.requireNonNull(var10005);
      var10000.add(new Info(var10004, var10005::getValue));
      var10000 = this.infoList;
      var10004 = () -> {
         class_640 playerListEntry = mc.method_1562().method_2871(mc.field_1724.method_5667());
         Object playerPing = playerListEntry == null ? "Unknown" : playerListEntry.method_2959() + "ms";
         return "Ping §f" + (String)playerPing;
      };
      var10005 = this.ping;
      Objects.requireNonNull(var10005);
      var10000.add(new Info(var10004, var10005::getValue));
      var10000 = this.infoList;
      var10004 = () -> "FPS §f" + Nina.FPS.getFps();
      var10005 = this.fps;
      Objects.requireNonNull(var10005);
      var10000.add(new Info(var10004, var10005::getValue));
   }

   public static String getDuration(class_1293 pe) {
      if (pe.method_48559()) {
         return "∞";
      } else {
         int var1 = pe.method_5584();
         int mins = var1 / 1200;
         int sec = var1 % 1200 / 20;
         return String.format("%d:%02d", mins, sec);
      }
   }

   public void onRender2D(class_332 drawContext, float tickDelta) {
      if (this.waterMark.getValue()) {
         this.renderWaterMark(drawContext, tickDelta);
      }

      int fontHeight = this.getHeight();
      if (this.coords.getValue()) {
         String coordsString = getCoords();
         this.drawCoord(drawContext, coordsString, mc.method_22683().method_4502() - fontHeight - (mc.field_1755 instanceof class_408 ? 15 : 0));
      }

      if (this.potions.getValue() && this.potionMode.getValue() == HUD.PotionMode.Panel) {
         this.renderPotionPanels(drawContext);
      }

      HUD.Info.onRender(drawContext, this.infoList, this.renderingUp.getValue());
   }

   @EventListener(
      priority = -999
   )
   public void onUpdate(ClientTickEvent event) {
      if (!nullCheck()) {
         if (event.isPost()) {
            this.tickThunderRu();
            HUD.Info.onUpdate(this.infoList, this.sort.getValue());
         }

      }
   }

   private void tickThunderRu() {
      if (this.waterMarkMode.getValue() == HUD.WaterMarkMode.Small && this.thunderRU.getValue()) {
         long now = System.currentTimeMillis();
         if (now >= this.thunderRuNextSwitchMs) {
            this.thunderRuIndex = (this.thunderRuIndex + 1) % this.thunderRuNames.length;
            this.thunderRuNextSwitchMs = now + 500L;
         }
      }
   }

   private void renderWaterMark(class_332 ctx, float tickDelta) {
      int x = this.waterMarkOffset.getValueInt();
      int y = this.waterMarkOffset.getValueInt();
      this.renderAlienWatermark(ctx, x, y);
   }

   private void renderAlienWatermark(class_332 ctx, int x, int y) {
      TextUtil.drawString(ctx, "Alien-More", (double)x, (double)y, this.getColor((double)0.0F), this.font.getValue(), this.shadow.getValue());
   }

   private void renderThunderBig(class_332 ctx, int x, int y) {
      Color bg = new Color(0, 0, 0, 110);
      if (this.blur.getValue()) {
         Nina.BLUR.applyBlur((float)this.radius.getValue(), (float)x, (float)y, 106.0F, 30.0F);
      }

      Render2DUtil.drawRound(ctx.method_51448(), x, y, 106.0F, 30.0F, 4.0F, bg);
      TextUtil.drawString(ctx, "ALIEN", (double)x + (double)6.0F, (double)y + (double)5.0F, -1, this.font.getValue(), this.shadow.getValue());
      if (this.pulse.booleanValue) {
         TextUtil.drawStringPulse(ctx, "recode", (double)x + (double)36.0F, (double)y + (double)21.0F, this.color.getValue(), this.pulse.getValue(), this.pulseSpeed.getValue(), this.pulseCounter.getValueInt(), this.font.getValue(), this.shadow.getValue());
      } else {
         TextUtil.drawString(ctx, "recode", (double)x + (double)36.0F, (double)y + (double)21.0F, this.getColor((double)1.0F), this.font.getValue(), this.shadow.getValue());
      }
   }

   private void renderThunderSmall(class_332 ctx, int x, int y) {
      String username = mc.method_1548().method_1676();
      String server = mc.method_1542() ? "SinglePlayer" : mc.method_1558().field_3761;
      int pingMs = this.getSelfPingMs();
      if (this.blur.getValue()) {
         float usernameW = (float)this.getTextWidth(username);
         float serverW = (float)this.getTextWidth(server);
         float offset1 = usernameW + 72.0F;
         float rightW = offset1 + serverW - 36.0F;
         Nina.BLUR.applyBlur((float)this.radius.getValue(), (float)x, (float)y, 50.0F, 15.0F);
         Nina.BLUR.applyBlur((float)this.radius.getValue(), (float)x + 55.0F, (float)y, rightW, 15.0F);
         Render2DUtil.drawRound(ctx.method_51448(), x, y, 50.0F, 15.0F, 3.0F, new Color(0, 0, 0, 110));
         Render2DUtil.drawRound(ctx.method_51448(), (float)x + 55.0F, (float)y, rightW, 15.0F, 3.0F, new Color(0, 0, 0, 110));
         Render2DUtil.drawRect(ctx.method_51448(), (float)x + 13.0F, (float)y + 1.5F, 0.5F, 11.0F, new Color(1157627903, true));
         if (this.pulse.booleanValue) {
            TextUtil.drawStringPulse(ctx, "Recode", (double)x + (double)18.0F, (double)y + (double)5.0F, this.color.getValue(), this.pulse.getValue(), this.pulseSpeed.getValue(), this.pulseCounter.getValueInt(), this.font.getValue(), this.shadow.getValue());
         } else {
            TextUtil.drawString(ctx, "Recode", (double)x + (double)18.0F, (double)y + (double)5.0F, this.getColor((double)20.0F), this.font.getValue(), this.shadow.getValue());
         }

         int c270 = this.getColor((double)270.0F);
         int c0 = this.getColor((double)0.0F);
         int c180 = this.getColor((double)180.0F);
         int c90 = this.getColor((double)90.0F);
         this.drawGradientTexturedQuad(ctx.method_51448(), TH_MINI_LOGO, (float)x + 1.0F, (float)y + 2.0F, 11.0F, 11.0F, c270, c0, c180, c90);
         this.drawGradientTexturedQuad(ctx.method_51448(), TH_PLAYER_ICON, (float)x + 58.0F, (float)y + 3.0F, 8.0F, 8.0F, c270, c0, c180, c90);
         this.drawGradientTexturedQuad(ctx.method_51448(), TH_SERVER_ICON, (float)x + offset1, (float)y + 2.0F, 10.0F, 10.0F, c270, c0, c180, c90);
         TextUtil.drawString(ctx, username, (double)x + (double)68.0F, (double)y + (double)4.5F, -1, this.font.getValue(), this.shadow.getValue());
         TextUtil.drawString(ctx, server, (double)x + (double)offset1 + (double)13.0F, (double)y + (double)4.5F, -1, this.font.getValue(), this.shadow.getValue());
      } else {
         String prefix = this.thunderRU.getValue() ? this.thunderRuNames[this.thunderRuIndex] : "Alien";
         String info = "§8| §r" + username + "§8 | §r" + pingMs + " ms§8 | §r" + server;
         String full = prefix + " " + info;
         float width = (float)this.getTextWidth(full) + 5.0F;
         if (this.blur.getValue()) {
            Nina.BLUR.applyBlur((float)this.radius.getValue(), (float)x, (float)y, width, 10.0F);
         }

         Render2DUtil.drawRound(ctx.method_51448(), x, y, width, 10.0F, 3.0F, new Color(0, 0, 0, 110));
         if (this.pulse.booleanValue) {
            TextUtil.drawStringPulse(ctx, prefix + " ", (double)x + (double)2.0F, (double)y + (double)2.5F, this.color.getValue(), this.pulse.getValue(), this.pulseSpeed.getValue(), this.pulseCounter.getValueInt(), this.font.getValue(), this.shadow.getValue());
         } else {
            TextUtil.drawString(ctx, prefix + " ", (double)x + (double)2.0F, (double)y + (double)2.5F, this.getColor((double)10.0F), this.font.getValue(), this.shadow.getValue());
         }

         TextUtil.drawString(ctx, info, (double)x + (double)2.0F + (double)this.getTextWidth(prefix + " "), (double)y + (double)2.5F, -1, this.font.getValue(), this.shadow.getValue());
      }
   }

   private void renderThunderClassic(class_332 ctx, int x, int y) {
      String text = "Alien-More";
      if (this.pulse.booleanValue) {
         TextUtil.drawStringPulse(ctx, text, (double)x + (double)6.0F, (double)y + (double)5.0F, this.color.getValue(), this.pulse.getValue(), this.pulseSpeed.getValue(), this.pulseCounter.getValueInt(), this.font.getValue(), this.shadow.getValue());
      } else {
         TextUtil.drawString(ctx, text, (double)x + (double)6.0F, (double)y + (double)5.0F, this.getColor((double)10.0F), this.font.getValue(), this.shadow.getValue());
      }
   }

   private void renderThunderBaltika(class_332 ctx, int x, int y, float tickDelta) {
      if (this.blur.getValue()) {
         Nina.BLUR.applyBlur((float)this.radius.getValue(), (float)x, (float)y, 100.0F, 64.0F);
      }

      Render2DUtil.drawRound(ctx.method_51448(), x, y, 100.0F, 64.0F, 4.0F, new Color(0, 0, 0, 110));
      class_4587 matrices = ctx.method_51448();
      matrices.method_22903();
      matrices.method_46416((float)x + 10.0F, (float)y + 32.0F, 0.0F);
      float age = mc.field_1724 == null ? 0.0F : (float)mc.field_1724.field_6012;
      matrices.method_22907(class_7833.field_40718.rotation((float)Math.toRadians((double)(age * 3.0F + tickDelta))));
      matrices.method_46416(-((float)x + 10.0F), -((float)y + 32.0F), 0.0F);
      this.drawTexturedQuad(matrices, TH_BALTIKA, (float)x - 10.0F, (float)y + 2.0F, 40.0F, 64.0F, -1);
      matrices.method_22909();
      TextUtil.drawString(ctx, "BALTIKA", (double)x + (double)43.0F, (double)y + (double)41.5F, -1, this.font.getValue(), this.shadow.getValue());
   }

   private void renderThunderRifk(class_332 ctx, int x, int y) {
      Date date = new Date(System.currentTimeMillis());
      SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
      Object[] var10001 = new Object[]{Math.round(Nina.SERVER.getTPS()), format.format(date)};
      String info = "§a" + String.format("alien | build: 16/06/2024 | rate: %d | %s", var10001);
      float width = (float)this.getTextWidth(info) + 5.0F;
      Render2DUtil.drawRectWithOutline(ctx.method_51448(), (float)x, (float)y, width, 8.0F, Color.decode("#192A1A"), Color.decode("#833B7B"));
      TextUtil.drawString(ctx, info, (double)x + (double)3.0F, (double)y + (double)3.0F, -1, this.font.getValue(), this.shadow.getValue());
   }

   private int getSelfPingMs() {
      if (mc.method_1562() != null && mc.field_1724 != null) {
         class_640 playerListEntry = mc.method_1562().method_2871(mc.field_1724.method_5667());
         return playerListEntry == null ? -1 : playerListEntry.method_2959();
      } else {
         return -1;
      }
   }

   private int getTextWidth(String text) {
      return this.font.getValue() ? (int)FontManager.ui.getWidth(text) : mc.field_1772.method_1727(text);
   }

   private void drawTexturedQuad(class_4587 matrices, class_2960 texture, float x, float y, float w, float h, int color) {
      float a = (float)(color >> 24 & 255) / 255.0F;
      float r = (float)(color >> 16 & 255) / 255.0F;
      float g = (float)(color >> 8 & 255) / 255.0F;
      float b = (float)(color & 255) / 255.0F;
      Matrix4f matrix = matrices.method_23760().method_23761();
      RenderSystem.enableBlend();
      RenderSystem.setShader(class_757::method_34543);
      RenderSystem.setShaderTexture(0, texture);
      class_287 buffer = class_289.method_1348().method_60827(class_5596.field_27382, class_290.field_1575);
      buffer.method_22918(matrix, x, y, 0.0F).method_22913(0.0F, 0.0F).method_22915(r, g, b, a);
      buffer.method_22918(matrix, x, y + h, 0.0F).method_22913(0.0F, 1.0F).method_22915(r, g, b, a);
      buffer.method_22918(matrix, x + w, y + h, 0.0F).method_22913(1.0F, 1.0F).method_22915(r, g, b, a);
      buffer.method_22918(matrix, x + w, y, 0.0F).method_22913(1.0F, 0.0F).method_22915(r, g, b, a);
      class_286.method_43433(buffer.method_60800());
      RenderSystem.disableBlend();
   }

   private void drawGradientTexturedQuad(class_4587 matrices, class_2960 texture, float x, float y, float w, float h, int topLeft, int topRight, int bottomRight, int bottomLeft) {
      Matrix4f matrix = matrices.method_23760().method_23761();
      RenderSystem.enableBlend();
      RenderSystem.setShader(class_757::method_34543);
      RenderSystem.setShaderTexture(0, texture);
      class_287 buffer = class_289.method_1348().method_60827(class_5596.field_27382, class_290.field_1575);
      buffer.method_22918(matrix, x, y, 0.0F).method_22913(0.0F, 0.0F).method_39415(topLeft);
      buffer.method_22918(matrix, x, y + h, 0.0F).method_22913(0.0F, 1.0F).method_39415(bottomLeft);
      buffer.method_22918(matrix, x + w, y + h, 0.0F).method_22913(1.0F, 1.0F).method_39415(bottomRight);
      buffer.method_22918(matrix, x + w, y, 0.0F).method_22913(1.0F, 0.0F).method_39415(topRight);
      class_286.method_43433(buffer.method_60800());
      RenderSystem.disableBlend();
   }

   private static String getCoords() {
      boolean inNether = mc.field_1687.method_27983().equals(class_1937.field_25180);
      int posX = mc.field_1724.method_31477();
      int posY = mc.field_1724.method_31478();
      int posZ = mc.field_1724.method_31479();
      float factor = !inNether ? 0.125F : 8.0F;
      int anotherWorldX = (int)(mc.field_1724.method_23317() * (double)factor);
      int anotherWorldZ = (int)(mc.field_1724.method_23321() * (double)factor);
      String var10000 = inNether ? posX + ", " + posY + ", " + posZ + " §7[§f" + anotherWorldX + ", " + anotherWorldZ + "§7]§f" : posX + ", " + posY + ", " + posZ + "§7 [§f" + anotherWorldX + ", " + anotherWorldZ + "§7]";
      return "XYZ §f" + var10000;
   }

   private int getWidth(String s) {
      if (this.lowerCase.getValue()) {
         s = s.toLowerCase();
      }

      return this.font.getValue() ? (int)FontManager.ui.getWidth(s) : mc.field_1772.method_1727(s);
   }

   private int getHeight() {
      if (this.font.getValue()) {
         return (int)FontManager.ui.getFontHeight();
      } else {
         Objects.requireNonNull(mc.field_1772);
         return 9;
      }
   }

   private void drawCoord(class_332 drawContext, String s, int y) {
      if (this.lowerCase.getValue()) {
         s = s.toLowerCase();
      }

      int c = this.colorSync.getValue() ? this.getColor((double)20.0F) : this.color.getValue().getRGB();
      TextUtil.drawString(drawContext, s, (double)2.0F, (double)y, c, this.font.getValue(), this.shadow.getValue());
   }

   private void renderPotionPanels(class_332 ctx) {
      if (mc.field_1724 == null) {
         this.potionPanelInfos.clear();
      } else {
         for(PotionPanelInfo info : this.potionPanelInfos.values()) {
            info.active = false;
         }

         Set<class_1291> activeEffects = new HashSet();

         for(class_1293 se : mc.field_1724.method_6026()) {
            class_1291 effect = (class_1291)se.method_5579().comp_349();
            activeEffects.add(effect);
            PotionPanelInfo info = (PotionPanelInfo)this.potionPanelInfos.get(effect);
            if (info == null) {
               info = new PotionPanelInfo();
               this.potionPanelInfos.put(effect, info);
            }

            info.active = true;
            info.maxDuration = Math.max(info.maxDuration, se.method_5584());
            info.duration = se.method_5584();
            info.infinite = se.method_48559();
            info.amplifier = se.method_5578();
         }

         List<class_1291> effects = new ArrayList(this.potionPanelInfos.keySet());
         effects.sort(Comparator.comparing((eff) -> eff.method_5560().getString()));
         int fontH = this.getHeight();
         float panelH = (float)(fontH * 2 + 8);
         float rowH = panelH + 4.0F;
         float radius = 5.0F;
         int startY = (int)((float)mc.method_22683().method_4502() / 2.0F - (float)effects.size() * rowH / 2.0F);
         List<class_1291> toRemove = new ArrayList();
         int idx = 0;

         for(class_1291 effect : effects) {
            PotionPanelInfo info = (PotionPanelInfo)this.potionPanelInfos.get(effect);
            if (info != null) {
               boolean isActive = activeEffects.contains(effect);
               String name = effect.method_5560().getString();
               String title = name + this.getPotionAmplifierSuffix(info.amplifier);
               if (this.lowerCase.getValue()) {
                  title = title.toLowerCase();
               }

               String duration = HUD.PotionPanelInfo.formatDurationTicks(info.duration, info.infinite);
               int titleW = this.getWidth(title);
               float panelW = 26.0F + (float)titleW + 20.0F;
               info.width = panelW;
               double targetY = (double)(startY + (int)((float)idx * rowH));
               if (!info.initialized) {
                  double offscreenX = (double)(-panelW - 20.0F);
                  info.xAnimation.from = offscreenX;
                  info.xAnimation.to = offscreenX;
                  info.yAnimation.from = targetY;
                  info.yAnimation.to = targetY;
                  info.fadeAnimation.from = (double)0.0F;
                  info.fadeAnimation.to = (double)0.0F;
                  info.barAnimation.from = (double)0.0F;
                  info.barAnimation.to = (double)0.0F;
                  info.initialized = true;
               }

               double fade = info.fadeAnimation.get(isActive ? (double)1.0F : (double)0.0F, (long)this.fadeLength.getValueInt(), (Easing)this.easing.getValue());
               double x = info.xAnimation.get(isActive ? (double)10.0F : (double)(-panelW - 20.0F), isActive ? (long)this.enableLength.getValueInt() : (long)this.disableLength.getValueInt(), (Easing)this.easing.getValue());
               double y = info.yAnimation.get(targetY, (long)this.enableLength.getValueInt(), (Easing)this.easing.getValue());
               double barTarget = isActive && info.maxDuration > 0 ? (double)panelW * ((double)info.duration / (double)info.maxDuration) : (double)0.0F;
               double barW = info.barAnimation.get(barTarget, (long)this.enableLength.getValueInt(), (Easing)this.easing.getValue());
               if (fade > 0.02) {
                  Color bg = new Color(this.potionPanelBgColor.getRed(), this.potionPanelBgColor.getGreen(), this.potionPanelBgColor.getBlue(), (int)((double)this.potionPanelBgColor.getAlpha() * fade));
                  Color effectColor = ColorUtil.injectAlpha(this.getEffectColor(effect), (int)((double)255.0F * fade));
                  if (this.blur.getValue()) {
                     Nina.BLUR.applyBlur((float)(this.radius.getValue() * fade), (float)x, (float)y, panelW, panelH);
                  }

                  Render2DUtil.drawRoundedRect(ctx.method_51448(), (float)x, (float)y, panelW, panelH, radius, bg);
                  Render2DUtil.drawRoundedRect(ctx.method_51448(), (float)x, (float)y, (float)barW, panelH, radius, bg);
                  Render2DUtil.drawRoundedRect(ctx.method_51448(), (float)(x + (double)panelW - (double)7.0F), (float)(y + (double)6.0F), 3.0F, panelH - 12.0F, 1.5F, effectColor);
                  float textX = (float)x + 6.0F;
                  float textY = (float)y + 3.0F;
                  TextUtil.drawString(ctx, title, (double)textX, (double)textY, effectColor.getRGB(), this.font.getValue(), this.shadow.getValue());
                  TextUtil.drawString(ctx, duration, (double)textX, (double)(textY + (float)fontH + 2.0F), ColorUtil.injectAlpha(16777215, (int)((double)255.0F * fade)), this.font.getValue(), this.shadow.getValue());
               }

               if (!isActive && fade < 0.02 && x <= (double)(-panelW - 19.0F)) {
                  toRemove.add(effect);
               }

               ++idx;
            }
         }

         for(class_1291 effect : toRemove) {
            this.potionPanelInfos.remove(effect);
         }

      }
   }

   private String getPotionAmplifierSuffix(int amplifier) {
      String var10000;
      switch (amplifier) {
         case 0 -> var10000 = "";
         case 1 -> var10000 = " II";
         case 2 -> var10000 = " III";
         case 3 -> var10000 = " IV";
         default -> var10000 = " " + (amplifier + 1);
      }

      return var10000;
   }

   private Color getEffectColor(class_1291 effect) {
      if (effect == class_1294.field_5904.comp_349()) {
         return new Color(0, 255, 255);
      } else if (effect == class_1294.field_5909.comp_349()) {
         return new Color(128, 128, 128);
      } else if (effect == class_1294.field_5917.comp_349()) {
         return new Color(255, 165, 0);
      } else if (effect == class_1294.field_5901.comp_349()) {
         return new Color(139, 69, 19);
      } else if (effect == class_1294.field_5910.comp_349()) {
         return new Color(255, 0, 0);
      } else if (effect == class_1294.field_5915.comp_349()) {
         return new Color(255, 105, 180);
      } else if (effect == class_1294.field_5921.comp_349()) {
         return new Color(128, 0, 128);
      } else if (effect == class_1294.field_5913.comp_349()) {
         return new Color(0, 255, 0);
      } else if (effect == class_1294.field_5924.comp_349()) {
         return new Color(255, 20, 147);
      } else if (effect == class_1294.field_5907.comp_349()) {
         return new Color(255, 215, 0);
      } else if (effect == class_1294.field_5918.comp_349()) {
         return new Color(255, 140, 0);
      } else if (effect == class_1294.field_5923.comp_349()) {
         return new Color(0, 191, 255);
      } else if (effect == class_1294.field_5905.comp_349()) {
         return new Color(192, 192, 192);
      } else if (effect == class_1294.field_5925.comp_349()) {
         return new Color(0, 100, 0);
      } else if (effect == class_1294.field_5899.comp_349()) {
         return new Color(0, 128, 0);
      } else {
         return effect == class_1294.field_5920.comp_349() ? new Color(44, 44, 44) : new Color(effect.method_5556());
      }
   }

   public int getColor(double counter) {
      return this.color.getValue().getRGB();
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
         Objects.requireNonNull(mc.field_1772);
         return 9;
      }
   }

   public static enum WaterMarkMode {
      Alien,
      Big,
      Small,
      Classic,
      BaltikaClient,
      Rifk;

      // $FF: synthetic method
      private static WaterMarkMode[] $values() {
         return new WaterMarkMode[]{Alien, Big, Small, Classic, BaltikaClient, Rifk};
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

   private static enum PotionMode {
      List,
      Panel;

      // $FF: synthetic method
      private static PotionMode[] $values() {
         return new PotionMode[]{List, Panel};
      }
   }

   private static class PotionPanelInfo {
      public final Animation xAnimation = new Animation();
      public final Animation yAnimation = new Animation();
      public final Animation fadeAnimation = new Animation();
      public final Animation barAnimation = new Animation();
      public int maxDuration = -1;
      public int duration;
      public boolean infinite;
      public int amplifier;
      public float width;
      public boolean active;
      public boolean initialized;

      private static String formatDurationTicks(int ticks, boolean infinite) {
         if (infinite) {
            return "∞";
         } else {
            int mins = ticks / 1200;
            int sec = ticks % 1200 / 20;
            return String.format("%d:%02d", mins, sec);
         }
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
         HUD.Info.fromUp = fromUp;
         fontHeight = (double)HUD.INSTANCE.getFontHeight();
         currentY = fromUp ? (double)1.0F + HUD.INSTANCE.yOffset.getValue() : (double)Wrapper.mc.method_22683().method_4502() - fontHeight - (double)1.0F - (Wrapper.mc.field_1755 instanceof class_408 && HUD.INSTANCE.yOffset.getValue() < (double)12.0F ? (double)12.0F - HUD.INSTANCE.yOffset.getValue() + HUD.INSTANCE.interval.getValue() / (double)2.0F : (double)0.0F) - HUD.INSTANCE.yOffset.getValue();
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

      public void draw(class_332 context) {
         if (this.currentX > (double)0.0F || this.isOn) {
            this.currentX = this.animation.get(this.isOn ? (double)(HUD.this.getWidth(this.string) + 1) : (double)0.0F, this.isOn ? (long)HUD.this.enableLength.getValueInt() : (long)HUD.this.disableLength.getValueInt(), (Easing)HUD.this.easing.getValue());
            double width = this.currentX + (double)HUD.this.xOffset.getValueFloat();
            double fade = this.fadeAnimation.get(this.isOn ? (double)1.0F : (double)0.0F, (long)HUD.this.fadeLength.getValueInt(), (Easing)HUD.this.easing.getValue());
            if (fade > 0.04) {
               int c = ColorUtil.injectAlpha(HUD.this.getColor(counter += fromUp ? fade : -fade), (int)((double)HUD.this.color.getValue().getAlpha() * fade));
               if (HUD.this.blur.getValue()) {
                  Nina.BLUR.applyBlur((float)(HUD.this.radius.getValue() * fade), (float)((double)windowWidth - width - (double)(HUD.this.width.getValueFloat() / 2.0F)), (float)currentY - 1.0F - HUD.this.interval.getValueFloat() / 2.0F, (float)width + HUD.this.width.getValueFloat() - HUD.this.xOffset.getValueFloat(), (float)fontHeight + HUD.this.interval.getValueFloat());
               }

               if (HUD.this.backGround.getValue()) {
                  Render2DUtil.drawRect(context.method_51448(), (float)((double)windowWidth - width - (double)(HUD.this.width.getValueFloat() / 2.0F)), (float)currentY - 1.0F - HUD.this.interval.getValueFloat() / 2.0F, (float)width + HUD.this.width.getValueFloat() - HUD.this.xOffset.getValueFloat(), (float)fontHeight + HUD.this.interval.getValueFloat(), ColorUtil.injectAlpha(HUD.this.bgColor.sync ? c : HUD.this.bgColor.getValue().getRGB(), (int)((double)HUD.this.bgColor.getValue().getAlpha() * fade)));
               }

               if (HUD.this.glow.booleanValue) {
                  Render2DUtil.drawGlow(context.method_51448(), (float)((double)windowWidth - width - (double)(HUD.this.width.getValueFloat() / 2.0F)), (float)currentY - 1.0F - HUD.this.interval.getValueFloat() / 2.0F, (float)width + HUD.this.width.getValueFloat() - HUD.this.xOffset.getValueFloat(), (float)fontHeight + HUD.this.interval.getValueFloat(), ColorUtil.injectAlpha(HUD.this.glow.sync ? c : HUD.this.glow.getValue().getRGB(), (int)((double)HUD.this.glow.getValue().getAlpha() * fade)));
               }

               TextUtil.drawString(context, this.string, (double)windowWidth - width, currentY + (double)HUD.this.textOffset.getValueFloat(), c, HUD.this.font.getValue(), HUD.this.shadow.getValue());
               if (HUD.this.rect.booleanValue) {
                  Render2DUtil.drawRect(context.method_51448(), (float)windowWidth + HUD.this.width.getValueFloat() / 2.0F - HUD.this.xOffset.getValueFloat(), (float)currentY - 1.0F - HUD.this.interval.getValueFloat() / 2.0F, 1.0F, (float)fontHeight + HUD.this.interval.getValueFloat(), HUD.this.rect.sync ? c : ColorUtil.injectAlpha(HUD.this.rect.getValue(), (int)((double)HUD.this.rect.getValue().getAlpha() * fade)).getRGB());
               }

               currentY += fromUp ? (fontHeight + HUD.this.interval.getValue()) * fade : -(fontHeight + HUD.this.interval.getValue()) * fade;
            }
         }

      }
   }
}
