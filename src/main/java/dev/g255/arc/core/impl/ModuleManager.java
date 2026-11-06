package dev.g255.arc.core.impl;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.impl.Render2DEvent;
import dev.g255.arc.api.events.impl.Render3DEvent;
import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.api.utils.path.BaritoneUtil;
import dev.g255.arc.api.utils.render.Render2DUtil;
import dev.g255.arc.api.utils.render.TextUtil;
import dev.g255.arc.mod.Mod;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.client.AntiCheat;
import dev.g255.arc.mod.modules.impl.client.BackPackHUD;
import dev.g255.arc.mod.modules.impl.client.BaritoneModule;
import dev.g255.arc.mod.modules.impl.client.ClickGui;
import dev.g255.arc.mod.modules.impl.client.ColorsModule;
import dev.g255.arc.mod.modules.impl.client.ClientSetting;
import dev.g255.arc.mod.modules.impl.client.Fonts;
import dev.g255.arc.mod.modules.impl.client.HUD;
import dev.g255.arc.mod.modules.impl.client.HoverSound;
import dev.g255.arc.mod.modules.impl.client.IRC;
import dev.g255.arc.mod.modules.impl.client.ItemsCounter;
import dev.g255.arc.mod.modules.impl.client.TextRadar;
import dev.g255.arc.mod.modules.impl.combat.AntiCobweb;
import dev.g255.arc.mod.modules.impl.combat.AntiPiston;
import dev.g255.arc.mod.modules.impl.combat.AntiRegear;
import dev.g255.arc.mod.modules.impl.combat.AntiWeak;
import dev.g255.arc.mod.modules.impl.combat.Aura;
import dev.g255.arc.mod.modules.impl.combat.AutoAnchor;
import dev.g255.arc.mod.modules.impl.combat.AutoArrow;
import dev.g255.arc.mod.modules.impl.combat.AutoMend;
import dev.g255.arc.mod.modules.impl.combat.AutoPush;
import dev.g255.arc.mod.modules.impl.combat.AutoSupplyPlus;
import dev.g255.arc.mod.modules.impl.combat.AutoTrap;
import dev.g255.arc.mod.modules.impl.combat.AutoTrapDoor;
import dev.g255.arc.mod.modules.impl.combat.AutoWeb;
import dev.g255.arc.mod.modules.impl.combat.BedAura;
import dev.g255.arc.mod.modules.impl.combat.BlockLag;
import dev.g255.arc.mod.modules.impl.combat.CevBreaker;
import dev.g255.arc.mod.modules.impl.combat.Criticals;
import dev.g255.arc.mod.modules.impl.combat.CrystalBot;
import dev.g255.arc.mod.modules.impl.combat.CrystalHurtPeople;
import dev.g255.arc.mod.modules.impl.combat.FlatGroundMace;
import dev.g255.arc.mod.modules.impl.combat.HoleFiller;
import dev.g255.arc.mod.modules.impl.combat.MaceAura;
import dev.g255.arc.mod.modules.impl.combat.MaceGodPlus;
import dev.g255.arc.mod.modules.impl.combat.MacePlus;
import dev.g255.arc.mod.modules.impl.combat.Offhand;
import dev.g255.arc.mod.modules.impl.combat.PistonCrystal;
import dev.g255.arc.mod.modules.impl.combat.PopEz;
import dev.g255.arc.mod.modules.impl.combat.SelfTrap;
import dev.g255.arc.mod.modules.impl.exploit.AntiHunger;
import dev.g255.arc.mod.modules.impl.exploit.AntiPacket;
import dev.g255.arc.mod.modules.impl.exploit.Blink;
import dev.g255.arc.mod.modules.impl.exploit.Clip;
import dev.g255.arc.mod.modules.impl.exploit.FakeVanilla;
import dev.g255.arc.mod.modules.impl.exploit.FastLatency;
import dev.g255.arc.mod.modules.impl.exploit.FastProjectile;
import dev.g255.arc.mod.modules.impl.exploit.MaceSpoof;
import dev.g255.arc.mod.modules.impl.exploit.NoMineAnimation;
import dev.g255.arc.mod.modules.impl.exploit.NoResourcePack;
import dev.g255.arc.mod.modules.impl.exploit.PacketControl;
import dev.g255.arc.mod.modules.impl.exploit.Phase;
import dev.g255.arc.mod.modules.impl.exploit.PingSpoof;
import dev.g255.arc.mod.modules.impl.exploit.RaytraceBypass;
import dev.g255.arc.mod.modules.impl.exploit.RocketExtend;
import dev.g255.arc.mod.modules.impl.exploit.TeleportLogger;
import dev.g255.arc.mod.modules.impl.exploit.ThirtyTwoKMace;
import dev.g255.arc.mod.modules.impl.exploit.XCarry;
import dev.g255.arc.mod.modules.impl.hud.AnimatedGif;
import dev.g255.arc.mod.modules.impl.hud.NotificationHud;
import dev.g255.arc.mod.modules.impl.hud.PlayerModel;
import dev.g255.arc.mod.modules.impl.misc.AntiAFK;
import dev.g255.arc.mod.modules.impl.misc.AntiAim;
import dev.g255.arc.mod.modules.impl.misc.AntiQuit;
import dev.g255.arc.mod.modules.impl.misc.AutoBengDuan;
import dev.g255.arc.mod.modules.impl.misc.AutoEZ;
import dev.g255.arc.mod.modules.impl.misc.AutoLog;
import dev.g255.arc.mod.modules.impl.misc.AutoReconnect;
import dev.g255.arc.mod.modules.impl.misc.AutoRespawn;
import dev.g255.arc.mod.modules.impl.misc.ChatAppend;
import dev.g255.arc.mod.modules.impl.misc.CoordLogger;
import dev.g255.arc.mod.modules.impl.misc.FakePlayer;
import dev.g255.arc.mod.modules.impl.misc.Friend;
import dev.g255.arc.mod.modules.impl.misc.KillEffect;
import dev.g255.arc.mod.modules.impl.misc.MiddleClick;
import dev.g255.arc.mod.modules.impl.misc.Nuker;
import dev.g255.arc.mod.modules.impl.misc.PacketLogger;
import dev.g255.arc.mod.modules.impl.misc.ShulkerViewer;
import dev.g255.arc.mod.modules.impl.misc.Spammer;
import dev.g255.arc.mod.modules.impl.misc.Tips;
import dev.g255.arc.mod.modules.impl.misc.VisualRange;
import dev.g255.arc.mod.modules.impl.movement.AntiSlowFalling;
import dev.g255.arc.mod.modules.impl.movement.AntiVoid;
import dev.g255.arc.mod.modules.impl.movement.BlockStrafe;
import dev.g255.arc.mod.modules.impl.movement.ElytraFly;
import dev.g255.arc.mod.modules.impl.movement.FastFall;
import dev.g255.arc.mod.modules.impl.movement.FastSwim;
import dev.g255.arc.mod.modules.impl.movement.FastWeb;
import dev.g255.arc.mod.modules.impl.movement.Flatten;
import dev.g255.arc.mod.modules.impl.movement.Fly;
import dev.g255.arc.mod.modules.impl.movement.HoleSnap;
import dev.g255.arc.mod.modules.impl.movement.LongJump;
import dev.g255.arc.mod.modules.impl.movement.MovementSync;
import dev.g255.arc.mod.modules.impl.movement.NoSlow;
import dev.g255.arc.mod.modules.impl.movement.PacketFly;
import dev.g255.arc.mod.modules.impl.movement.SafeWalk;
import dev.g255.arc.mod.modules.impl.movement.Scaffold;
import dev.g255.arc.mod.modules.impl.movement.Speed;
import dev.g255.arc.mod.modules.impl.movement.Spin;
import dev.g255.arc.mod.modules.impl.movement.Spiral;
import dev.g255.arc.mod.modules.impl.movement.Sprint;
import dev.g255.arc.mod.modules.impl.movement.Step;
import dev.g255.arc.mod.modules.impl.movement.Strafe;
import dev.g255.arc.mod.modules.impl.movement.VClip;
import dev.g255.arc.mod.modules.impl.movement.Velocity;
import dev.g255.arc.mod.modules.impl.movement.WindLongJump;
import dev.g255.arc.mod.modules.impl.player.AntiEffects;
import dev.g255.arc.mod.modules.impl.player.AutoArmor;
import dev.g255.arc.mod.modules.impl.player.AutoEat;
import dev.g255.arc.mod.modules.impl.player.AutoPearl;
import dev.g255.arc.mod.modules.impl.player.AutoPot;
import dev.g255.arc.mod.modules.impl.player.AutoTool;
import dev.g255.arc.mod.modules.impl.player.ChestStealer;
import dev.g255.arc.mod.modules.impl.player.FreeLook;
import dev.g255.arc.mod.modules.impl.player.Freecam;
import dev.g255.arc.mod.modules.impl.player.IQBoost;
import dev.g255.arc.mod.modules.impl.player.InteractTweaks;
import dev.g255.arc.mod.modules.impl.player.NoFall;
import dev.g255.arc.mod.modules.impl.player.PacketEat;
import dev.g255.arc.mod.modules.impl.player.PacketMine;
import dev.g255.arc.mod.modules.impl.player.Replenish;
import dev.g255.arc.mod.modules.impl.player.TimerModule;
import dev.g255.arc.mod.modules.impl.render.Ambience;
import dev.g255.arc.mod.modules.impl.render.ArmorHUD;
import dev.g255.arc.mod.modules.impl.render.AspectRatio;
import dev.g255.arc.mod.modules.impl.render.BreakESP;
import dev.g255.arc.mod.modules.impl.render.CameraClip;
import dev.g255.arc.mod.modules.impl.render.Chams;
import dev.g255.arc.mod.modules.impl.render.Crosshair;
import dev.g255.arc.mod.modules.impl.render.DynamicIsland;
import dev.g255.arc.mod.modules.impl.render.ESP;
import dev.g255.arc.mod.modules.impl.render.ItemCounter;
import dev.g255.arc.mod.modules.impl.render.KillThunder;
import dev.g255.arc.mod.modules.impl.render.EnemyInfo;
import dev.g255.arc.mod.modules.impl.render.Fov;
import dev.g255.arc.mod.modules.impl.render.HoleESP;
import dev.g255.arc.mod.modules.impl.render.LogoutSpots;
import dev.g255.arc.mod.modules.impl.render.MotionCamera;
import dev.g255.arc.mod.modules.impl.render.NameTags;
import dev.g255.arc.mod.modules.impl.render.NoRender;
import dev.g255.arc.mod.modules.impl.render.PhaseESP;
import dev.g255.arc.mod.modules.impl.render.PlaceRender;
import dev.g255.arc.mod.modules.impl.render.PopChams;
import dev.g255.arc.mod.modules.impl.render.ShaderModule;
import dev.g255.arc.mod.modules.impl.render.TargetESP;
import dev.g255.arc.mod.modules.impl.render.Tooltips;
import dev.g255.arc.mod.modules.impl.render.Tracers;
import dev.g255.arc.mod.modules.impl.render.Trails;
import dev.g255.arc.mod.modules.impl.render.Trajectories;
import dev.g255.arc.mod.modules.impl.render.TwoDESP;
import dev.g255.arc.mod.modules.impl.render.ViewModel;
import dev.g255.arc.mod.modules.impl.render.Waypoints;
import dev.g255.arc.mod.modules.impl.render.Zoom;
import dev.g255.arc.mod.modules.impl.render.skybox.Skybox;
import dev.g255.arc.mod.modules.settings.Setting;
import dev.g255.arc.mod.modules.settings.impl.BindSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;
import dev.g255.arc.mod.modules.impl.combat.AntiCrawl;
import dev.g255.arc.mod.modules.impl.combat.AntiPhase;
import dev.g255.arc.mod.modules.impl.combat.AutoCrystal;
import dev.g255.arc.mod.modules.impl.combat.Blocker;
import dev.g255.arc.mod.modules.impl.combat.Panic;
import dev.g255.arc.mod.modules.impl.combat.Quiver;
import dev.g255.arc.mod.modules.impl.exploit.BowBomb;
import dev.g255.arc.mod.modules.impl.exploit.ChorusControl;
import dev.g255.arc.mod.modules.impl.exploit.ChunkESP;
import dev.g255.arc.mod.modules.impl.exploit.InfiniteTrident;
import dev.g255.arc.mod.modules.impl.exploit.ServerLagger;
import dev.g255.arc.mod.modules.impl.exploit.SkinFlicker;
import dev.g255.arc.mod.modules.impl.misc.AutoKit;
import dev.g255.arc.mod.modules.impl.misc.BedCrafter;
import dev.g255.arc.mod.modules.impl.misc.ExtraTab;
import dev.g255.arc.mod.modules.impl.misc.LavaFiller;
import dev.g255.arc.mod.modules.impl.misc.NoSound;
import dev.g255.arc.mod.modules.impl.misc.NoTerrainScreen;
import dev.g255.arc.mod.modules.impl.misc.Punctuation;
import dev.g255.arc.mod.modules.impl.movement.AutoWalk;
import dev.g255.arc.mod.modules.impl.movement.EntityControl;
import dev.g255.arc.mod.modules.impl.movement.NoJumpDelay;
import dev.g255.arc.mod.modules.impl.player.AirPlace;
import dev.g255.arc.mod.modules.impl.player.Sorter;
import dev.g255.arc.mod.modules.impl.player.Yaw;
import dev.g255.arc.mod.modules.impl.render.HighLight;
import dev.g255.arc.mod.modules.impl.render.TotemParticle;
import dev.g255.arc.mod.modules.impl.render.Xray;
import dev.g255.arc.mod.modules.impl.combat.AntiAnchor;
import dev.g255.arc.mod.modules.impl.combat.AutoMine;
import dev.g255.arc.mod.modules.impl.combat.AutoPowder;
import dev.g255.arc.mod.modules.impl.combat.TrapShulker;
import dev.g255.arc.mod.modules.impl.combat.AutoTK;
import dev.g255.arc.mod.modules.impl.exploit.DeathRespawn;
import dev.g255.arc.mod.modules.impl.exploit.Disabler;
import dev.g255.arc.mod.modules.impl.exploit.FakeName;
import dev.g255.arc.mod.modules.impl.misc.ArmorPreserve;
import dev.g255.arc.mod.modules.impl.misc.AutoCommand;
import dev.g255.arc.mod.modules.impl.misc.AutoDrop;
import dev.g255.arc.mod.modules.impl.misc.AutoEnderChest;
import dev.g255.arc.mod.modules.impl.misc.DropAntiRegear;
import dev.g255.arc.mod.modules.impl.misc.DropShulkerBox;
import dev.g255.arc.mod.modules.impl.misc.Bot;
import dev.g255.arc.mod.modules.impl.movement.FakeFlight;
import dev.g255.arc.mod.modules.impl.movement.HighJump;
import dev.g255.arc.mod.modules.impl.movement.HighJumpPlus;
import dev.g255.arc.mod.modules.impl.movement.TargetFollow;
import dev.g255.arc.mod.modules.impl.player.Follower;
import dev.g255.arc.mod.modules.impl.player.NoRotate;
import dev.g255.arc.mod.modules.impl.render.BAHalo;
import dev.g255.arc.mod.modules.impl.render.BreakerEsp;
import dev.g255.arc.mod.modules.impl.render.CrystalPlaceRender;
import dev.g255.arc.mod.modules.impl.render.EsuESP;
import dev.g255.arc.mod.modules.impl.render.JumpCircle;
import dev.g255.arc.mod.modules.impl.render.LastWalkEsp;
import dev.g255.arc.mod.modules.impl.render.NameTag2D;
import dev.g255.arc.mod.modules.impl.render.PopEsp;
import dev.g255.arc.mod.modules.impl.render.SkyColor;
import dev.g255.arc.mod.modules.impl.render.Weather;

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
      if (BaritoneUtil.loaded) {
         this.addModule(new BaritoneModule());
      }

      this.addModule(new Fonts());
      this.addModule(new CrystalBot());
      this.addModule(new CrystalHurtPeople());
      this.addModule(new Ambience());
      this.addModule(new AntiHunger());
      this.addModule(new AntiVoid());
      this.addModule(new VClip());
      this.addModule(new AntiWeak());
      this.addModule(new Friend());
      this.addModule(new AspectRatio());
      this.addModule(new Aura());
      this.addModule(new PistonCrystal());
      this.addModule(new AutoAnchor());
      this.addModule(new PhaseESP());
      this.addModule(new AutoArmor());
      this.addModule(new AutoLog());
      this.addModule(new AutoEZ());
      this.addModule(new SelfTrap());
      this.addModule(new AutoMend());
      this.addModule(new MaceAura());
      this.addModule(new MaceGodPlus());
      this.addModule(new FlatGroundMace());
      this.addModule(new AutoPot());
      this.addModule(new AutoPush());
      this.addModule(new Offhand());
      this.addModule(new Nuker());
      this.addModule(new AutoTrap());
      this.addModule(new AutoTrapDoor());
      this.addModule(new AutoWeb());
      this.addModule(new Blink());
      this.addModule(new BlockStrafe());
      this.addModule(new FastSwim());
      this.addModule(new BreakESP());
      this.addModule(new BlockLag());
      this.addModule(new MaceSpoof());
      this.addModule(new CameraClip());
      this.addModule(new ChatAppend());
      this.addModule(new ClickGui());
      this.addModule(new ColorsModule());
      this.addModule(new HoverSound());
      this.addModule(new AutoSupplyPlus());
      this.addModule(new Clip());
      this.addModule(new AntiCheat());
      this.addModule(new IRC());
      this.addModule(new ItemsCounter());
      this.addModule(new Fov());
      this.addModule(new Criticals());
      this.addModule(new CevBreaker());
      this.addModule(new Crosshair());
      this.addModule(new Chams());
      this.addModule(new AntiPacket());
      this.addModule(new AutoReconnect());
      this.addModule(new ESP());
      this.addModule(new ItemCounter());
      this.addModule(new HoleESP());
      this.addModule(new Tracers());
      this.addModule(new MovementSync());
      this.addModule(new LongJump());
      this.addModule(new Spin());
      this.addModule(new ElytraFly());
      this.addModule(new PacketLogger());
      this.addModule(new TeleportLogger());
      this.addModule(new NameTags());
      this.addModule(new ShulkerViewer());
      this.addModule(new PingSpoof());
      this.addModule(new FakePlayer());
      this.addModule(new Spammer());
      this.addModule(new MotionCamera());
      this.addModule(new FastFall());
      this.addModule(new FastWeb());
      this.addModule(new Flatten());
      this.addModule(new Fly());
      this.addModule(new Freecam());
      this.addModule(new FreeLook());
      this.addModule(new TimerModule());
      this.addModule(new Tips());
      this.addModule(new ClientSetting());
      this.addModule(new TextRadar());
      this.addModule(new HUD());
      this.addModule(new NoResourcePack());
      this.addModule(new RocketExtend());
      this.addModule(new HoleFiller());
      this.addModule(new HoleSnap());
      this.addModule(new LogoutSpots());
      this.addModule(new AutoTool());
      this.addModule(new Trajectories());
      this.addModule(new KillEffect());
      this.addModule(new KillThunder());
      this.addModule(new AutoPearl());
      this.addModule(new AntiEffects());
      this.addModule(new NoFall());
      this.addModule(new NoRender());
      this.addModule(new NoSlow());
      this.addModule(new PacketEat());
      this.addModule(new PacketFly());
      this.addModule(new PacketMine());
      this.addModule(new PacketControl());
      this.addModule(new Phase());
      this.addModule(new PlaceRender());
      this.addModule(new InteractTweaks());
      this.addModule(new PopChams());
      this.addModule(new Replenish());
      this.addModule(new Scaffold());
      this.addModule(new ShaderModule());
      this.addModule(new AntiRegear());
      this.addModule(new SafeWalk());
      this.addModule(new Speed());
      this.addModule(new Sprint());
      this.addModule(new Strafe());
      this.addModule(new Step());
      this.addModule(new Velocity());
      this.addModule(new ViewModel());
      this.addModule(new XCarry());
      this.addModule(new Zoom());
      this.addModule(new AutoArrow());
      this.addModule(new PopEz());
      this.addModule(new IQBoost());
      this.addModule(new FastProjectile());
      this.addModule(new AntiAFK());
      this.addModule(new AutoRespawn());
      this.addModule(new VisualRange());
      this.addModule(new CoordLogger());
      this.addModule(new Spiral());
      this.addModule(new BedAura());
      this.addModule(new FastLatency());
      this.addModule(new NoMineAnimation());
      this.addModule(new AntiQuit());
      this.addModule(new AutoEat());
      this.addModule(new ChestStealer());
      this.addModule(new Trails());
      this.addModule(new FakeVanilla());
      this.addModule(new RaytraceBypass());
      this.addModule(new AntiAim());
      this.addModule(new MiddleClick());
      this.addModule(new Tooltips());
      this.addModule(new Waypoints());
      this.addModule(new ArmorHUD());
      this.addModule(new AntiPiston());
      this.addModule(new AntiCobweb());
      this.addModule(new MacePlus());
      this.addModule(new AntiSlowFalling());
      this.addModule(new WindLongJump());
      this.addModule(new EnemyInfo());
      this.addModule(new TargetESP());
      this.addModule(new TwoDESP());
      this.addModule(new Skybox());
      this.addModule(new ThirtyTwoKMace());
      this.addModule(new AutoBengDuan());
      this.addModule(new BackPackHUD());
      this.addModule(new DynamicIsland());
      this.addModule(new NotificationHud());
      this.addModule(new AnimatedGif());
      this.addModule(new PlayerModel());
      this.addModule(new AntiCrawl());
      this.addModule(new AntiPhase());
      this.addModule(new AutoCrystal());
      this.addModule(new Blocker());
      this.addModule(new Panic());
      this.addModule(new Quiver());
      this.addModule(new BowBomb());
      this.addModule(new ChorusControl());
      this.addModule(new ChunkESP());
      this.addModule(new InfiniteTrident());
      this.addModule(new ServerLagger());
      this.addModule(new SkinFlicker());
      this.addModule(new AutoKit());
      this.addModule(new BedCrafter());
      this.addModule(new ExtraTab());
      this.addModule(new LavaFiller());
      this.addModule(new NoSound());
      this.addModule(new NoTerrainScreen());
      this.addModule(new Punctuation());
      this.addModule(new AutoWalk());
      this.addModule(new EntityControl());
      this.addModule(new NoJumpDelay());
      this.addModule(new AirPlace());
      this.addModule(new Sorter());
      this.addModule(new Yaw());
      this.addModule(new HighLight());
      this.addModule(new TotemParticle());
      this.addModule(new Xray());
      this.addModule(new AntiAnchor());
      this.addModule(new AutoMine());
      this.addModule(new AutoPowder());
      this.addModule(new TrapShulker());
      this.addModule(new AutoTK());
      this.addModule(new DeathRespawn());
      this.addModule(new Disabler());
      this.addModule(new FakeName());
      this.addModule(new ArmorPreserve());
      this.addModule(new AutoCommand());
      this.addModule(new AutoDrop());
      this.addModule(new AutoEnderChest());
      this.addModule(new DropAntiRegear());
      this.addModule(new DropShulkerBox());
      this.addModule(new Bot());
      this.addModule(new FakeFlight());
      this.addModule(new HighJump());
      this.addModule(new HighJumpPlus());
      this.addModule(new Follower());
      this.addModule(new TargetFollow());
      this.addModule(new NoRotate());
      this.addModule(new BAHalo());
      this.addModule(new BreakerEsp());
      this.addModule(new CrystalPlaceRender());
      this.addModule(new EsuESP());
      this.addModule(new JumpCircle());
      this.addModule(new LastWalkEsp());
      this.addModule(new NameTag2D());
      this.addModule(new PopEsp());
      this.addModule(new SkyColor());
      this.addModule(new Weather());
      this.modules.sort(Comparator.comparing(Mod::getName));
      System.out.println("[Arc] by G255 - Module Manager Initialized (" + this.modules.size() + " modules)");
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

      CommandManager.sendMessage("§7[§eArc§7] §7Developed by §e§lG255§f - Welcome!");
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
         Arc.EVENT_BUS.post(Render2DEvent.get(drawContext, mc.getRenderTickCounter().getTickDelta(true)));
      } catch (Exception e) {
         e.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (Render2DEvent) Message: [" + e.getMessage() + "]");
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
         Arc.EVENT_BUS.post(Render3DEvent.get(matrices, mc.getRenderTickCounter().getTickDelta(true)));
      } catch (Exception e) {
         e.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (Render3DEvent) Message: [" + e.getMessage() + "]");
         }
      }

      GL11.glDisable(2848);
   }

   public void showToggleBanner(Module module, boolean enabled) {
      if (NotificationHud.INSTANCE != null) {
         NotificationHud.onToggle(module, enabled);
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
