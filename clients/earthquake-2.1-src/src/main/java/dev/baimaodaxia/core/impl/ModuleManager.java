package dev.baimaodaxia.core.impl;

import dev.baimaodaxia.earthquake;
import dev.baimaodaxia.api.events.impl.Render2DEvent;
import dev.baimaodaxia.api.events.impl.Render3DEvent;
import dev.baimaodaxia.api.utils.Wrapper;
import dev.baimaodaxia.api.utils.render.Render2DUtil;
import dev.baimaodaxia.api.utils.render.TextUtil;
import dev.baimaodaxia.mod.Mod;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.client.AntiCheat;
import dev.baimaodaxia.mod.modules.impl.client.Cape;
import dev.baimaodaxia.mod.modules.impl.client.ClickGui;
import dev.baimaodaxia.mod.modules.impl.client.ClientSetting;
import dev.baimaodaxia.mod.modules.impl.client.ColorsModule;
import dev.baimaodaxia.mod.modules.impl.client.FakeFPS;
import dev.baimaodaxia.mod.modules.impl.client.Fonts;
import dev.baimaodaxia.mod.modules.impl.client.HUD;
import dev.baimaodaxia.mod.modules.impl.client.ItemsCounter;
import dev.baimaodaxia.mod.modules.impl.client.NotificationsHud;
import dev.baimaodaxia.mod.modules.impl.client.TextRadar;
import dev.baimaodaxia.mod.modules.impl.client.WaterMark;
import dev.baimaodaxia.mod.modules.impl.combat.AntiCrawl;
import dev.baimaodaxia.mod.modules.impl.combat.AntiPhase;
import dev.baimaodaxia.mod.modules.impl.combat.AntiPistonPush;
import dev.baimaodaxia.mod.modules.impl.combat.AntiRegear;
import dev.baimaodaxia.mod.modules.impl.combat.AntiWeak;
import dev.baimaodaxia.mod.modules.impl.combat.Aura;
import dev.baimaodaxia.mod.modules.impl.combat.AutoAnchor;
import dev.baimaodaxia.mod.modules.impl.combat.AutoCrystal;
import dev.baimaodaxia.mod.modules.impl.combat.AutoLog;
import dev.baimaodaxia.mod.modules.impl.combat.AutoMace;
import dev.baimaodaxia.mod.modules.impl.combat.AutoMend;
import dev.baimaodaxia.mod.modules.impl.combat.AutoPush;
import dev.baimaodaxia.mod.modules.impl.combat.AutoRegear;
import dev.baimaodaxia.mod.modules.impl.combat.AutoSpear;
import dev.baimaodaxia.mod.modules.impl.combat.AutoTrap;
import dev.baimaodaxia.mod.modules.impl.combat.AutoWeb;
import dev.baimaodaxia.mod.modules.impl.combat.Blocker;
import dev.baimaodaxia.mod.modules.impl.combat.Breaker;
import dev.baimaodaxia.mod.modules.impl.combat.Burrow;
import dev.baimaodaxia.mod.modules.impl.combat.CevBreaker;
import dev.baimaodaxia.mod.modules.impl.combat.ChestStealer;
import dev.baimaodaxia.mod.modules.impl.combat.Criticals;
import dev.baimaodaxia.mod.modules.impl.combat.Follower;
import dev.baimaodaxia.mod.modules.impl.combat.GlassAura;
import dev.baimaodaxia.mod.modules.impl.combat.HoleFiller;
import dev.baimaodaxia.mod.modules.impl.combat.HolePush;
import dev.baimaodaxia.mod.modules.impl.combat.IQBoost;
import dev.baimaodaxia.mod.modules.impl.combat.Offhand;
import dev.baimaodaxia.mod.modules.impl.combat.PistonCrystal;
import dev.baimaodaxia.mod.modules.impl.combat.Quiver;
import dev.baimaodaxia.mod.modules.impl.combat.SelfTrap;
import dev.baimaodaxia.mod.modules.impl.combat.Surround;
import dev.baimaodaxia.mod.modules.impl.exploit.AntiHunger;
import dev.baimaodaxia.mod.modules.impl.exploit.AntiPacket;
import dev.baimaodaxia.mod.modules.impl.exploit.Blink;
import dev.baimaodaxia.mod.modules.impl.exploit.BowBomb;
import dev.baimaodaxia.mod.modules.impl.exploit.ChorusControl;
import dev.baimaodaxia.mod.modules.impl.exploit.ChunkESP;
import dev.baimaodaxia.mod.modules.impl.exploit.Clip;
import dev.baimaodaxia.mod.modules.impl.exploit.InfiniteTrident;
import dev.baimaodaxia.mod.modules.impl.exploit.NoResourcePack;
import dev.baimaodaxia.mod.modules.impl.exploit.PacketControl;
import dev.baimaodaxia.mod.modules.impl.exploit.Phase;
import dev.baimaodaxia.mod.modules.impl.exploit.PingSpoof;
import dev.baimaodaxia.mod.modules.impl.exploit.ResetVL;
import dev.baimaodaxia.mod.modules.impl.exploit.RocketExtend;
import dev.baimaodaxia.mod.modules.impl.exploit.ServerLagger;
import dev.baimaodaxia.mod.modules.impl.exploit.SkinFlicker;
import dev.baimaodaxia.mod.modules.impl.exploit.TeleportLogger;
import dev.baimaodaxia.mod.modules.impl.exploit.XCarry;
import dev.baimaodaxia.mod.modules.impl.misc.AutoEZ;
import dev.baimaodaxia.mod.modules.impl.misc.AutoFuck;
import dev.baimaodaxia.mod.modules.impl.misc.AutoKit;
import dev.baimaodaxia.mod.modules.impl.misc.AutoKouZi;
import dev.baimaodaxia.mod.modules.impl.misc.AutoReconnect;
import dev.baimaodaxia.mod.modules.impl.misc.AutoSex;
import dev.baimaodaxia.mod.modules.impl.misc.BedCrafter;
import dev.baimaodaxia.mod.modules.impl.misc.ChatAppend;
import dev.baimaodaxia.mod.modules.impl.misc.ExtraTab;
import dev.baimaodaxia.mod.modules.impl.misc.FakePlayer;
import dev.baimaodaxia.mod.modules.impl.misc.Friend;
import dev.baimaodaxia.mod.modules.impl.misc.KillEffect;
import dev.baimaodaxia.mod.modules.impl.misc.LavaFiller;
import dev.baimaodaxia.mod.modules.impl.misc.NoSound;
import dev.baimaodaxia.mod.modules.impl.misc.NoTerrainScreen;
import dev.baimaodaxia.mod.modules.impl.misc.Nuker;
import dev.baimaodaxia.mod.modules.impl.misc.PacketLogger;
import dev.baimaodaxia.mod.modules.impl.misc.PopEz;
import dev.baimaodaxia.mod.modules.impl.misc.Punctuation;
import dev.baimaodaxia.mod.modules.impl.misc.ShulkerViewer;
import dev.baimaodaxia.mod.modules.impl.misc.Spammer;
import dev.baimaodaxia.mod.modules.impl.misc.Tips;
import dev.baimaodaxia.mod.modules.impl.movement.AntiVoid;
import dev.baimaodaxia.mod.modules.impl.movement.AutoWalk;
import dev.baimaodaxia.mod.modules.impl.movement.BlockStrafe;
import dev.baimaodaxia.mod.modules.impl.movement.ElytraFly;
import dev.baimaodaxia.mod.modules.impl.movement.EntityControl;
import dev.baimaodaxia.mod.modules.impl.movement.FastFall;
import dev.baimaodaxia.mod.modules.impl.movement.FastSwim;
import dev.baimaodaxia.mod.modules.impl.movement.FastWeb;
import dev.baimaodaxia.mod.modules.impl.movement.Flatten;
import dev.baimaodaxia.mod.modules.impl.movement.Fly;
import dev.baimaodaxia.mod.modules.impl.movement.GrimLowHop;
import dev.baimaodaxia.mod.modules.impl.movement.HighJump;
import dev.baimaodaxia.mod.modules.impl.movement.HoleSnap;
import dev.baimaodaxia.mod.modules.impl.movement.LongJump;
import dev.baimaodaxia.mod.modules.impl.movement.MovementSync;
import dev.baimaodaxia.mod.modules.impl.movement.NoJumpDelay;
import dev.baimaodaxia.mod.modules.impl.movement.NoSlow;
import dev.baimaodaxia.mod.modules.impl.movement.PacketFly;
import dev.baimaodaxia.mod.modules.impl.movement.SafeWalk;
import dev.baimaodaxia.mod.modules.impl.movement.Scaffold;
import dev.baimaodaxia.mod.modules.impl.movement.Speed;
import dev.baimaodaxia.mod.modules.impl.movement.Sprint;
import dev.baimaodaxia.mod.modules.impl.movement.Step;
import dev.baimaodaxia.mod.modules.impl.movement.Strafe;
import dev.baimaodaxia.mod.modules.impl.movement.TpAura;
import dev.baimaodaxia.mod.modules.impl.movement.TpMace;
import dev.baimaodaxia.mod.modules.impl.movement.VClip;
import dev.baimaodaxia.mod.modules.impl.movement.Velocity;
import dev.baimaodaxia.mod.modules.impl.player.AirPlace;
import dev.baimaodaxia.mod.modules.impl.player.AntiEffects;
import dev.baimaodaxia.mod.modules.impl.player.AutoArmor;
import dev.baimaodaxia.mod.modules.impl.player.AutoPearl;
import dev.baimaodaxia.mod.modules.impl.player.AutoPotion;
import dev.baimaodaxia.mod.modules.impl.player.AutoTool;
import dev.baimaodaxia.mod.modules.impl.player.FastUse;
import dev.baimaodaxia.mod.modules.impl.player.FreeLook;
import dev.baimaodaxia.mod.modules.impl.player.Freecam;
import dev.baimaodaxia.mod.modules.impl.player.InteractTweaks;
import dev.baimaodaxia.mod.modules.impl.player.InvManager;
import dev.baimaodaxia.mod.modules.impl.player.LowArmorAlert;
import dev.baimaodaxia.mod.modules.impl.player.NoFall;
import dev.baimaodaxia.mod.modules.impl.player.PacketEat;
import dev.baimaodaxia.mod.modules.impl.player.PacketMine;
import dev.baimaodaxia.mod.modules.impl.player.Reach;
import dev.baimaodaxia.mod.modules.impl.player.Replenish;
import dev.baimaodaxia.mod.modules.impl.player.Sorter;
import dev.baimaodaxia.mod.modules.impl.player.TimerModule;
import dev.baimaodaxia.mod.modules.impl.player.Yaw;
import dev.baimaodaxia.mod.modules.impl.player.earthquakeMine;
import dev.baimaodaxia.mod.modules.impl.render.Ambience;
import dev.baimaodaxia.mod.modules.impl.render.AspectRatio;
import dev.baimaodaxia.mod.modules.impl.render.BreakESP;
import dev.baimaodaxia.mod.modules.impl.render.CameraClip;
import dev.baimaodaxia.mod.modules.impl.render.Chams;
import dev.baimaodaxia.mod.modules.impl.render.Crosshair;
import dev.baimaodaxia.mod.modules.impl.render.ESP;
import dev.baimaodaxia.mod.modules.impl.render.ForceSneak;
import dev.baimaodaxia.mod.modules.impl.render.Fov;
import dev.baimaodaxia.mod.modules.impl.render.HighLight;
import dev.baimaodaxia.mod.modules.impl.render.HitBubbles;
import dev.baimaodaxia.mod.modules.impl.render.HoleESP;
import dev.baimaodaxia.mod.modules.impl.render.InventoryHUD;
import dev.baimaodaxia.mod.modules.impl.render.JumpCircle;
import dev.baimaodaxia.mod.modules.impl.render.LogoutSpots;
import dev.baimaodaxia.mod.modules.impl.render.MotionBlur;
import dev.baimaodaxia.mod.modules.impl.render.MotionCamera;
import dev.baimaodaxia.mod.modules.impl.render.NameTags;
import dev.baimaodaxia.mod.modules.impl.render.NoRender;
import dev.baimaodaxia.mod.modules.impl.render.Particles;
import dev.baimaodaxia.mod.modules.impl.render.PhaseESP;
import dev.baimaodaxia.mod.modules.impl.render.PlaceRender;
import dev.baimaodaxia.mod.modules.impl.render.PopChams;
import dev.baimaodaxia.mod.modules.impl.render.Search;
import dev.baimaodaxia.mod.modules.impl.render.ShaderModule;
import dev.baimaodaxia.mod.modules.impl.render.Skybox;
import dev.baimaodaxia.mod.modules.impl.render.TotemParticle;
import dev.baimaodaxia.mod.modules.impl.render.Trajectories;
import dev.baimaodaxia.mod.modules.impl.render.ViewModel;
import dev.baimaodaxia.mod.modules.impl.render.Xray;
import dev.baimaodaxia.mod.modules.impl.render.Zoom;
import dev.baimaodaxia.mod.modules.settings.Setting;
import dev.baimaodaxia.mod.modules.settings.impl.BindSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;
public class ModuleManager implements Wrapper {
   private final ArrayList<Module> modules = new ArrayList();
   private final ArrayList<ToggleBanner> toggleBanners = new ArrayList();
   private boolean notificationMouseWasDown = false;
   private boolean interactiveToggleInBanner = false;

   public ArrayList<Module> getModules() {
      return this.modules;
   }

   public ModuleManager() {
      this.init();
   }

   public void init() {
      // Baritone not available
      // if (BaritoneUtil.loaded) {
      //    this.addModule(new BaritoneModule());
      // }

      this.addModule(new GlassAura());
      this.addModule(new FakeFPS());
      this.addModule(new AutoSex());
      this.addModule(new InventoryHUD());
      this.addModule(new TpMace());
      this.addModule(new TpAura());
      this.addModule(new HighJump());
      this.addModule(new IQBoost());
      this.addModule(new Reach());
      this.addModule(new FastUse());
      this.addModule(new AntiPistonPush());
      this.addModule(new LongJump());
      this.addModule(new AutoKouZi());
      this.addModule(new Follower());
      this.addModule(new ShaderModule());
      this.addModule(new LowArmorAlert());
      this.addModule(new PopEz());
      this.addModule(new ChestStealer());
      this.addModule(new AutoKit());
      this.addModule(new Fonts());
      this.addModule(new NoTerrainScreen());
      this.addModule(new AutoCrystal());
      this.addModule(new Ambience());
      this.addModule(new AntiHunger());
      this.addModule(new AntiVoid());
      this.addModule(new AutoWalk());
      this.addModule(new VClip());
      this.addModule(new ExtraTab());
      this.addModule(new AntiWeak());
      this.addModule(new BedCrafter());
      this.addModule(new Friend());
      this.addModule(new AspectRatio());
      this.addModule(new ChunkESP());
      this.addModule(new Aura());
      this.addModule(new PistonCrystal());
      this.addModule(new AutoAnchor());
      this.addModule(new AutoSpear());
      this.addModule(new PhaseESP());
      this.addModule(new AutoArmor());
      this.addModule(new Breaker());
      this.addModule(new AutoLog());
      this.addModule(new AutoEZ());
      this.addModule(new SelfTrap());
      this.addModule(new Sorter());
      this.addModule(new InvManager());
      this.addModule(new AutoMend());
      this.addModule(new Skybox());
      this.addModule(new AutoPotion());
      this.addModule(new AutoPush());
      this.addModule(new HolePush());
      this.addModule(new Offhand());
      this.addModule(new Nuker());
      this.addModule(new AutoTrap());
      this.addModule(new AutoWeb());
      this.addModule(new Blink());
      this.addModule(new ChorusControl());
      this.addModule(new BlockStrafe());
      this.addModule(new FastSwim());
      this.addModule(new Blocker());
      this.addModule(new Quiver());
      this.addModule(new BowBomb());
      this.addModule(new BreakESP());
      this.addModule(new Burrow());
      this.addModule(new Punctuation());
      this.addModule(new AutoMace());
      this.addModule(new CameraClip());
      this.addModule(new ChatAppend());
      this.addModule(new ClickGui());
      this.addModule(new InfiniteTrident());
      this.addModule(new ColorsModule());
      this.addModule(new AutoRegear());
      this.addModule(new LavaFiller());
      this.addModule(new AntiPhase());
      this.addModule(new Clip());
      this.addModule(new ResetVL());
      this.addModule(new AntiCheat());
      this.addModule(new ItemsCounter());
      this.addModule(new Fov());
      this.addModule(new Criticals());
      this.addModule(new CevBreaker());
      this.addModule(new Crosshair());
      this.addModule(new Chams());
      this.addModule(new AntiPacket());
      this.addModule(new AutoReconnect());
      this.addModule(new ESP());
      this.addModule(new HitBubbles());
      this.addModule(new JumpCircle());
      this.addModule(new HoleESP());
      this.addModule(new Search());
      this.addModule(new MovementSync());
      this.addModule(new ElytraFly());
      this.addModule(new PacketLogger());
      this.addModule(new TeleportLogger());
      this.addModule(new SkinFlicker());
      this.addModule(new EntityControl());
      this.addModule(new NameTags());
      this.addModule(new Particles());
      this.addModule(new ShulkerViewer());
      this.addModule(new PingSpoof());
      this.addModule(new FakePlayer());
      this.addModule(new Spammer());
      this.addModule(new MotionBlur());
      this.addModule(new MotionCamera());
      this.addModule(new HighLight());
      this.addModule(new FastFall());
      this.addModule(new FastWeb());
      this.addModule(new Flatten());
      this.addModule(new Fly());
      this.addModule(new Yaw());
      this.addModule(new Freecam());
      this.addModule(new FreeLook());
      this.addModule(new TimerModule());
      this.addModule(new Tips());
      this.addModule(new AutoFuck());
      this.addModule(new ClientSetting());
      this.addModule(new Cape());
      this.addModule(new WaterMark());
      this.addModule(new TextRadar());
      this.addModule(new HUD());
      this.addModule(new NotificationsHud());
      this.addModule(new NoResourcePack());
      this.addModule(new RocketExtend());
      this.addModule(new HoleFiller());
      this.addModule(new HoleSnap());
      this.addModule(new LogoutSpots());
      this.addModule(new AutoTool());
      this.addModule(new Trajectories());
      this.addModule(new KillEffect());
      this.addModule(new AutoPearl());
      this.addModule(new AntiEffects());
      this.addModule(new NoFall());
      this.addModule(new NoRender());
      this.addModule(new NoSlow());
      this.addModule(new GrimLowHop());
      this.addModule(new NoSound());
      this.addModule(new AirPlace());
      this.addModule(new Xray());
      this.addModule(new PacketEat());
      this.addModule(new PacketFly());
      this.addModule(new PacketMine());
      this.addModule(new earthquakeMine());
      this.addModule(new PacketControl());
      this.addModule(new Phase());
      this.addModule(new PlaceRender());
      this.addModule(new InteractTweaks());
      this.addModule(new PopChams());
      this.addModule(new Replenish());
      this.addModule(new ServerLagger());
      this.addModule(new Scaffold());
      this.addModule(new AntiCrawl());
      this.addModule(new AntiRegear());
      this.addModule(new SafeWalk());
      this.addModule(new NoJumpDelay());
      this.addModule(new Speed());
      this.addModule(new Sprint());
      this.addModule(new Strafe());
      this.addModule(new Step());
      this.addModule(new Surround());
      this.addModule(new TotemParticle());
      this.addModule(new Velocity());
      this.addModule(new ViewModel());
      this.addModule(new XCarry());
      this.addModule(new Zoom());
      this.addModule(new ForceSneak());
      this.modules.sort(Comparator.comparing(Mod::getName));
   }

   public void onKeyReleased(int eventKey) {
      if (eventKey != -1 && eventKey != 0) {
         this.handleKeyEvent(eventKey, false);
      }
   }

   public void onKeyPressed(int eventKey) {
      if (eventKey != -1 && eventKey != 0) {
         this.handleKeyEvent(eventKey, true);
      }
   }

   private void handleKeyEvent(int key, boolean isPressed) {
      for(Module module : this.modules) {
         BindSetting bindSetting = module.getBindSetting();
         if (bindSetting.getValue() == key) {
            if (isPressed && mc.currentScreen == null) {
               module.toggle();
               bindSetting.holding = true;
            } else if (!isPressed && bindSetting.isHoldEnable() && bindSetting.holding) {
               module.toggle();
               bindSetting.holding = false;
            }
         }

         for(Setting setting : module.getSettings()) {
            BindSetting bind;
            if (setting instanceof BindSetting && (bind = (BindSetting)setting).getValue() == key) {
               bind.setPressed(isPressed);
            }
         }
      }

   }

   public void onLogin() {
      for(Module module : this.modules) {
         if (module.isOn()) {
            module.onLogin();
         }
      }

   }

   public void onUpdate() {
      if (!Module.nullCheck()) {
         this.modules.stream().filter(Module::isOn).forEach((module) -> {
            try {
               module.onUpdate();
            } catch (Exception e) {
               e.printStackTrace();
               if (ClientSetting.INSTANCE.debug.getValue()) {
                  String var10000 = module.getName();
                  CommandManager.sendMessage("§4[" + var10000 + "] An error has occurred:" + e.getMessage());
               }
            }

         });
      }
   }

   public void onLogout() {
      for(Module module : this.modules) {
         if (module.isOn()) {
            module.onLogout();
         }
      }

   }

   public void onRender2D(DrawContext drawContext) {
      for(Module module : this.modules) {
         if (module.isOn()) {
            try {
               module.onRender2D(drawContext, mc.getRenderTickCounter().getTickDelta(true));
            } catch (Exception e) {
               e.printStackTrace();
               if (ClientSetting.INSTANCE.debug.getValue()) {
                  String var10000 = module.getName();
                  CommandManager.sendMessage("§4An error has occurred (" + var10000 + " [onRender2D]) Message: [" + e.getMessage() + "]");
               }
            }
         }
      }

      try {
         earthquake.EVENT_BUS.post(Render2DEvent.get(drawContext, mc.getRenderTickCounter().getTickDelta(true)));
      } catch (Exception e) {
         e.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (Render3DEvent) Message: [" + e.getMessage() + "]");
         }
      }

      this.renderToggleBanners(drawContext);
   }

   public void render3D(MatrixStack matrices) {
      GL11.glEnable(2848);

      for(Module module : this.modules) {
         if (module.isOn()) {
            try {
               module.onRender3D(matrices);
            } catch (Exception e) {
               e.printStackTrace();
               if (ClientSetting.INSTANCE.debug.getValue()) {
                  String var10000 = module.getName();
                  CommandManager.sendMessage("§4An error has occurred (" + var10000 + " [onRender3D]) Message: [" + e.getMessage() + "]");
               }
            }
         }
      }

      try {
         earthquake.EVENT_BUS.post(Render3DEvent.get(matrices, mc.getRenderTickCounter().getTickDelta(true)));
      } catch (Exception e) {
         e.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (Render3DEvent) Message: [" + e.getMessage() + "]");
         }
      }

      GL11.glDisable(2848);
   }

   public void showToggleBanner(Module module, boolean enabled) {
      if (module.drawn.getValue()) {
         NotificationsHud.addModuleNotification(module.getDisplayName(), enabled);
      }

   }

   private void renderToggleBanners(DrawContext ctx) {
      this.toggleBanners.clear();
   }

   private void renderPotionListLegacy(DrawContext ctx) {
      if (mc.player != null) {
         int margin = 14;
         int startX = margin + 2;
         int startY = margin + 92;
         int pillH = 14;
         int pillPad = 6;
         int idx = 0;

         for(StatusEffectInstance se : mc.player.getStatusEffects()) {
            String name = ((StatusEffect)se.getEffectType().value()).getName().getString();
            int ticks = se.getDuration();
            int totalSec = Math.max(0, ticks / 20);
            int mm = totalSec / 60;
            int ss = totalSec % 60;
            String time = String.format("%d:%02d", mm, ss);
            String text = name + " " + time;
            boolean customFont = FontManager.isCustomFontEnabled();
            int tw = customFont ? (int)FontManager.ui.getWidth(text) : (int)TextUtil.getWidth(text);
            int pillW = tw + pillPad * 2;
            int y = startY + idx * (pillH + 4);
            int keyCodec = 180;
            Render2DUtil.drawRoundedRect(ctx.getMatrices(), (float)startX, (float)y, (float)pillW, (float)pillH, 4.0F, new Color(255, 255, 255, keyCodec));
            Render2DUtil.drawRoundedStroke(ctx.getMatrices(), (float)startX, (float)y, (float)pillW, (float)pillH, 4.0F, new Color(220, 224, 230, 160), 48);
            int tx = startX + pillPad;
            double ty = (double)y + (double)((float)pillH - (customFont ? FontManager.ui.getFontHeight() : TextUtil.getHeight())) / (double)2.0F;
            TextUtil.drawString(ctx, text, (double)tx, ty, (new Color(30, 30, 30)).getRGB(), customFont);
            ++idx;
            if (idx >= 5) {
               break;
            }
         }

      }
   }

   private void renderPotionBanners(DrawContext ctx) {
   }

   private String formatDuration(int ticks) {
      int totalSec = Math.max(0, ticks / 20);
      int mm = totalSec / 60;
      int ss = totalSec % 60;
      return String.format("%d:%02d", mm, ss);
   }

   private Module getModuleByDisplayName(String name) {
      for(Module m : this.modules) {
         if (m.getDisplayName().equalsIgnoreCase(name) || m.getName().equalsIgnoreCase(name)) {
            return m;
         }
      }

      return null;
   }

   public void addModule(Module module) {
      this.modules.add(module);
   }

   public Module getModuleByName(String string) {
      for(Module module : this.modules) {
         if (module.getName().equalsIgnoreCase(string)) {
            return module;
         }
      }

      return null;
   }

   private static class ToggleBanner {
      public final String name;
      public final boolean enabled;
      public final long start;

      public ToggleBanner(String name, boolean enabled, long start) {
         this.name = name;
         this.enabled = enabled;
         this.start = start;
      }
   }
}
