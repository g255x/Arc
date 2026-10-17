package dev.G255.Lumi.core.impl;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.events.impl.Render2DEvent;
import dev.G255.Lumi.api.events.impl.Render3DEvent;
import dev.G255.Lumi.api.utils.Wrapper;
import dev.G255.Lumi.api.utils.path.BaritoneUtil;
import dev.G255.Lumi.api.utils.render.Render2DUtil;
import dev.G255.Lumi.api.utils.render.TextUtil;
import dev.G255.Lumi.mod.Mod;
import dev.G255.Lumi.mod.gui.clickgui.ClickGuiScreen;
import dev.G255.Lumi.mod.modules.HudModule;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.impl.client.AntiCheat;
import dev.G255.Lumi.mod.modules.impl.client.BaritoneModule;
import dev.G255.Lumi.mod.modules.impl.client.ClickGui;
import dev.G255.Lumi.mod.modules.impl.client.ClientSetting;
import dev.G255.Lumi.mod.modules.impl.client.Fonts;
import dev.G255.Lumi.mod.modules.impl.client.IRC;
import dev.G255.Lumi.mod.modules.impl.client.NotificationsHud;
import dev.G255.Lumi.mod.modules.impl.client.hud.ArmorHudModule;
import dev.G255.Lumi.mod.modules.impl.client.hud.ArrayListHudModule;
import dev.G255.Lumi.mod.modules.impl.client.hud.CoordsHudModule;
import dev.G255.Lumi.mod.modules.impl.client.hud.HudSetting;
import dev.G255.Lumi.mod.modules.impl.client.hud.InfoHudModule;
import dev.G255.Lumi.mod.modules.impl.client.hud.ItemCounterHudModule;
import dev.G255.Lumi.mod.modules.impl.client.hud.PacketHudModule;
import dev.G255.Lumi.mod.modules.impl.client.hud.TextRadar;
import dev.G255.Lumi.mod.modules.impl.client.hud.WaterMarkHudModule;
import dev.G255.Lumi.mod.modules.impl.combat.AimPearl;
import dev.G255.Lumi.mod.modules.impl.combat.AntiCrawl;
import dev.G255.Lumi.mod.modules.impl.combat.AntiHCQ;
import dev.G255.Lumi.mod.modules.impl.combat.AntiPhase;
import dev.G255.Lumi.mod.modules.impl.combat.AntiPistonPush;
import dev.G255.Lumi.mod.modules.impl.combat.AntiRegear;
import dev.G255.Lumi.mod.modules.impl.combat.AntiWeak;
import dev.G255.Lumi.mod.modules.impl.combat.AuraPlus;
import dev.G255.Lumi.mod.modules.impl.combat.Aura;
import dev.G255.Lumi.mod.modules.impl.combat.AutoAnchor;
import dev.G255.Lumi.mod.modules.impl.combat.AutoCrystal;
import dev.G255.Lumi.mod.modules.impl.combat.AutoMend;
import dev.G255.Lumi.mod.modules.impl.combat.AutoPush;
import dev.G255.Lumi.mod.modules.impl.combat.AutoRegear;
import dev.G255.Lumi.mod.modules.impl.combat.AutoTrap;
import dev.G255.Lumi.mod.modules.impl.combat.AutoWeb;
import dev.G255.Lumi.mod.modules.impl.combat.AutoArrow;
import dev.G255.Lumi.mod.modules.impl.combat.AutoButton;
import dev.G255.Lumi.mod.modules.impl.combat.AutoTrapDoor;
import dev.G255.Lumi.mod.modules.impl.combat.BackTrack;
import dev.G255.Lumi.mod.modules.impl.combat.Blocker;
import dev.G255.Lumi.mod.modules.impl.combat.Breaker;
import dev.G255.Lumi.mod.modules.impl.combat.Burrow;
import dev.G255.Lumi.mod.modules.impl.combat.CevBreaker;
import dev.G255.Lumi.mod.modules.impl.combat.Criticals;
import dev.G255.Lumi.mod.modules.impl.combat.CrossbowAim;
import dev.G255.Lumi.mod.modules.impl.combat.CrystalBot;
import dev.G255.Lumi.mod.modules.impl.combat.CrystalBotPlus;

import dev.G255.Lumi.mod.modules.impl.combat.Follower;
import dev.G255.Lumi.mod.modules.impl.combat.Helper;
import dev.G255.Lumi.mod.modules.impl.combat.HoleFiller;
import dev.G255.Lumi.mod.modules.impl.combat.MaceAura;
import dev.G255.Lumi.mod.modules.impl.combat.Offhand;
import dev.G255.Lumi.mod.modules.impl.combat.ForceEat;
import dev.G255.Lumi.mod.modules.impl.combat.HolePush;
import dev.G255.Lumi.mod.modules.impl.combat.MaceGodModule;
import dev.G255.Lumi.mod.modules.impl.combat.MaceGodPlus;
import dev.G255.Lumi.mod.modules.impl.combat.PearlBlocker;
import dev.G255.Lumi.mod.modules.impl.combat.PistonCrystalPlus;
import dev.G255.Lumi.mod.modules.impl.combat.SpearAura;
import dev.G255.Lumi.mod.modules.impl.combat.TimerMace;
import dev.G255.Lumi.mod.modules.impl.misc.Panic;
import dev.G255.Lumi.mod.modules.impl.combat.PistonCrystal;
import dev.G255.Lumi.mod.modules.impl.combat.PredictTrap;
import dev.G255.Lumi.mod.modules.impl.combat.Quiver;
import dev.G255.Lumi.mod.modules.impl.combat.SelfTrap;
import dev.G255.Lumi.mod.modules.impl.combat.Surround;
import dev.G255.Lumi.mod.modules.impl.exploit.AntiHunger;
import dev.G255.Lumi.mod.modules.impl.exploit.AntiPacket;
import dev.G255.Lumi.mod.modules.impl.exploit.Blink;
import dev.G255.Lumi.mod.modules.impl.exploit.BowBomb;
import dev.G255.Lumi.mod.modules.impl.exploit.BowSpam;
import dev.G255.Lumi.mod.modules.impl.exploit.ChorusControl;
import dev.G255.Lumi.mod.modules.impl.exploit.ChunkESP;
import dev.G255.Lumi.mod.modules.impl.exploit.Clip;
import dev.G255.Lumi.mod.modules.impl.exploit.InfiniteTrident;
import dev.G255.Lumi.mod.modules.impl.exploit.MaceSpoof;
import dev.G255.Lumi.mod.modules.impl.exploit.MaceSpoofPlus;
import dev.G255.Lumi.mod.modules.impl.exploit.NoResourcePack;
import dev.G255.Lumi.mod.modules.impl.exploit.PacketControl;
import dev.G255.Lumi.mod.modules.impl.exploit.Phase;
import dev.G255.Lumi.mod.modules.impl.exploit.PingSpoof;
import dev.G255.Lumi.mod.modules.impl.exploit.RocketExtend;
import dev.G255.Lumi.mod.modules.impl.exploit.ServerLagger;
import dev.G255.Lumi.mod.modules.impl.exploit.SkinFlicker;
import dev.G255.Lumi.mod.modules.impl.exploit.TeleportLogger;
import dev.G255.Lumi.mod.modules.impl.exploit.ThirtyTwoKMace;
import dev.G255.Lumi.mod.modules.impl.exploit.XCarry;
import dev.G255.Lumi.mod.modules.impl.misc.AddFriend;
import dev.G255.Lumi.mod.modules.impl.misc.AntiMonkey;
import dev.G255.Lumi.mod.modules.impl.misc.AutoEZ;
import dev.G255.Lumi.mod.modules.impl.misc.AutoExplain;
import dev.G255.Lumi.mod.modules.impl.misc.AutoKit;
import dev.G255.Lumi.mod.modules.impl.misc.AutoLog;
import dev.G255.Lumi.mod.modules.impl.misc.AutoReconnect;
import dev.G255.Lumi.mod.modules.impl.misc.BedCrafter;
// import dev.luminous.mod.modules.impl.misc.Bot;
import dev.G255.Lumi.mod.modules.impl.misc.Bot;
import dev.G255.Lumi.mod.modules.impl.misc.ChatAppend;
import dev.G255.Lumi.mod.modules.impl.misc.ExtraTab;
import dev.G255.Lumi.mod.modules.impl.misc.FakePlayer;
import dev.G255.Lumi.mod.modules.impl.misc.Friend;
import dev.G255.Lumi.mod.modules.impl.misc.KillEffect;
import dev.G255.Lumi.mod.modules.impl.misc.LavaFiller;
import dev.G255.Lumi.mod.modules.impl.misc.MissAlert;
import dev.G255.Lumi.mod.modules.impl.misc.NoSound;
import dev.G255.Lumi.mod.modules.impl.misc.NoTerrainScreen;
import dev.G255.Lumi.mod.modules.impl.misc.Nuker;
import dev.G255.Lumi.mod.modules.impl.misc.PacketLogger;
import dev.G255.Lumi.mod.modules.impl.misc.PopCounter;
import dev.G255.Lumi.mod.modules.impl.misc.Punctuation;
import dev.G255.Lumi.mod.modules.impl.misc.PearlRun;
import dev.G255.Lumi.mod.modules.impl.misc.ShulkerViewer;
import dev.G255.Lumi.mod.modules.impl.misc.Spammer;
import dev.G255.Lumi.mod.modules.impl.misc.Tips;
import dev.G255.Lumi.mod.modules.impl.movement.AntiCobweb;
import dev.G255.Lumi.mod.modules.impl.movement.AntiVoid;
import dev.G255.Lumi.mod.modules.impl.movement.AutoWalk;
import dev.G255.Lumi.mod.modules.impl.movement.BlockStrafe;
import dev.G255.Lumi.mod.modules.impl.movement.BoatFly;
import dev.G255.Lumi.mod.modules.impl.movement.BurstStrafe;
import dev.G255.Lumi.mod.modules.impl.movement.ElytraFlight;
import dev.G255.Lumi.mod.modules.impl.movement.ElytraFly;
import dev.G255.Lumi.mod.modules.impl.movement.EntityControl;
import dev.G255.Lumi.mod.modules.impl.movement.FastFall;
import dev.G255.Lumi.mod.modules.impl.movement.FastSwim;
import dev.G255.Lumi.mod.modules.impl.movement.FastWeb;
import dev.G255.Lumi.mod.modules.impl.movement.Flatten;
import dev.G255.Lumi.mod.modules.impl.movement.FastMoveatAi;
import dev.G255.Lumi.mod.modules.impl.movement.LieDownMove;
import dev.G255.Lumi.mod.modules.impl.movement.Fly;
import dev.G255.Lumi.mod.modules.impl.movement.HoleSnap;
import dev.G255.Lumi.mod.modules.impl.movement.MovementSync;
import dev.G255.Lumi.mod.modules.impl.movement.NoJumpDelay;
import dev.G255.Lumi.mod.modules.impl.movement.NoSlow;
import dev.G255.Lumi.mod.modules.impl.movement.PacketFly;
import dev.G255.Lumi.mod.modules.impl.movement.SafeWalk;
import dev.G255.Lumi.mod.modules.impl.movement.Scaffold;
import dev.G255.Lumi.mod.modules.impl.movement.Speed;
import dev.G255.Lumi.mod.modules.impl.movement.Sprint;
import dev.G255.Lumi.mod.modules.impl.movement.Step;
import dev.G255.Lumi.mod.modules.impl.movement.Strafe;
import dev.G255.Lumi.mod.modules.impl.movement.StrafeSpeed;
import dev.G255.Lumi.mod.modules.impl.movement.SuperLongJump;
import dev.G255.Lumi.mod.modules.impl.movement.VClip;
import dev.G255.Lumi.mod.modules.impl.movement.Velocity;
import dev.G255.Lumi.mod.modules.impl.movement.WindLongJump;
import dev.G255.Lumi.mod.modules.impl.movement.WindTP;
import dev.G255.Lumi.mod.modules.impl.player.AntiBaseKill;
import dev.G255.Lumi.mod.modules.impl.player.AirPlace;
import dev.G255.Lumi.mod.modules.impl.player.AntiEffects;
import dev.G255.Lumi.mod.modules.impl.player.AutoArmor;
import dev.G255.Lumi.mod.modules.impl.player.AutoKouZi;
import dev.G255.Lumi.mod.modules.impl.player.AutoPearl;
import dev.G255.Lumi.mod.modules.impl.player.AutoPot;
import dev.G255.Lumi.mod.modules.impl.player.AutoTool;
import dev.G255.Lumi.mod.modules.impl.player.Driller;
import dev.G255.Lumi.mod.modules.impl.player.FreeLook;
import dev.G255.Lumi.mod.modules.impl.player.Freecam;
import dev.G255.Lumi.mod.modules.impl.player.InteractTweaks;
import dev.G255.Lumi.mod.modules.impl.player.ItemAlert;
import dev.G255.Lumi.mod.modules.impl.player.NoFall;
import dev.G255.Lumi.mod.modules.impl.player.PacketEat;
import dev.G255.Lumi.mod.modules.impl.player.PacketMine;
import dev.G255.Lumi.mod.modules.impl.player.Replenish;
import dev.G255.Lumi.mod.modules.impl.player.Sorter;
import dev.G255.Lumi.mod.modules.impl.player.SpeedMine;
import dev.G255.Lumi.mod.modules.impl.player.TimerModule;
import dev.G255.Lumi.mod.modules.impl.player.Yaw;
import dev.G255.Lumi.mod.modules.impl.render.Ambience;
import dev.G255.Lumi.mod.modules.impl.render.AspectRatio;
import dev.G255.Lumi.mod.modules.impl.render.BreakESP;
import dev.G255.Lumi.mod.modules.impl.render.CameraClip;
import dev.G255.Lumi.mod.modules.impl.render.Camera;
import dev.G255.Lumi.mod.modules.impl.render.Chams;
import dev.G255.Lumi.mod.modules.impl.render.Crosshair;
import dev.G255.Lumi.mod.modules.impl.render.ESP;
import dev.G255.Lumi.mod.modules.impl.render.Fov;
import dev.G255.Lumi.mod.modules.impl.render.HighLight;
import dev.G255.Lumi.mod.modules.impl.render.HoleESP;
import dev.G255.Lumi.mod.modules.impl.render.LogoutSpots;
import dev.G255.Lumi.mod.modules.impl.render.MotionCamera;
import dev.G255.Lumi.mod.modules.impl.render.NameTags;
import dev.G255.Lumi.mod.modules.impl.render.NoRender;
import dev.G255.Lumi.mod.modules.impl.render.PhaseESP;
import dev.G255.Lumi.mod.modules.impl.render.PlaceRender;
import dev.G255.Lumi.mod.modules.impl.render.PlayerTrail;
import dev.G255.Lumi.mod.modules.impl.render.PopChams;
import dev.G255.Lumi.mod.modules.impl.render.ShaderModule;
import dev.G255.Lumi.mod.modules.impl.render.Skybox;
import dev.G255.Lumi.mod.modules.impl.render.TotemParticle;
import dev.G255.Lumi.mod.modules.impl.render.Tracers;
import dev.G255.Lumi.mod.modules.impl.render.Trajectories;
import dev.G255.Lumi.mod.modules.impl.render.ViewModel;
import dev.G255.Lumi.mod.modules.impl.render.Xray;
import dev.G255.Lumi.mod.modules.impl.render.Zoom;
import dev.G255.Lumi.mod.modules.settings.Setting;
import dev.G255.Lumi.mod.modules.settings.impl.BindSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.lwjgl.opengl.GL11;

public class ModuleManager
implements Wrapper {
    private final ArrayList<Module> modules = new ArrayList();
    private final ArrayList<ToggleBanner> toggleBanners = new ArrayList();
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
        this.addModule(new Panic());
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
        this.addModule(new PredictTrap());
        this.addModule(new AutoAnchor());
        this.addModule(new PhaseESP());
        this.addModule(new AutoArmor());
        this.addModule(new Breaker());
        this.addModule(new AutoLog());
        this.addModule(new AutoEZ());
        this.addModule(new PopCounter());
        this.addModule(new SelfTrap());
        this.addModule(new Sorter());
        this.addModule(new AutoMend());
        this.addModule(new AutoPot());
        this.addModule(new AutoPush());
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
        this.addModule(new MaceSpoof());
        this.addModule(new CameraClip());
        this.addModule(new ChatAppend());
        this.addModule(new ClickGui());
        this.addModule(new InfiniteTrident());
        this.addModule(new AutoRegear());
        this.addModule(new LavaFiller());
        this.addModule(new MaceAura());
        this.addModule(new AntiPhase());
        this.addModule(new AntiPistonPush());
        this.addModule(new Clip());
        this.addModule(new AntiCheat());
        this.addModule(new IRC());
        this.addModule(new NotificationsHud());
        this.addModule(new ItemCounterHudModule("Items", "\u7269\u54c1", 100, 100));
        this.addModule(new Fov());
        this.addModule(new Criticals());
        this.addModule(new CevBreaker());
        this.addModule(new Follower());
        this.addModule(new Helper());
        this.addModule(new Crosshair());
        this.addModule(new Chams());
        this.addModule(new AntiPacket());
        this.addModule(new AutoReconnect());
        this.addModule(new ESP());
        this.addModule(new HoleESP());
        this.addModule(new Tracers());
        this.addModule(new MovementSync());
        this.addModule(new ElytraFly());
        this.addModule(new PacketLogger());
        this.addModule(new TeleportLogger());
        this.addModule(new SkinFlicker());
        this.addModule(new EntityControl());
        this.addModule(new NameTags());
        this.addModule(new ShulkerViewer());
        this.addModule(new PingSpoof());
        this.addModule(new FakePlayer());
        this.addModule(new Spammer());
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
        this.addModule(new ClientSetting());
        this.addModule(new HudSetting());
        this.addModule(new TextRadar());
        this.addModule(new ArmorHudModule());
        this.addModule(new WaterMarkHudModule());
        this.addModule(new ArrayListHudModule());
        this.addModule(new CoordsHudModule());
        this.addModule(new InfoHudModule());
        this.addModule(new PacketHudModule());
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
        this.addModule(new NoSound());
        this.addModule(new AirPlace());
        this.addModule(new Xray());
        this.addModule(new PacketEat());
        this.addModule(new PacketFly());
        this.addModule(new PacketMine());
        this.addModule(new PacketControl());
        this.addModule(new Phase());
        this.addModule(new PlaceRender());
        this.addModule(new InteractTweaks());
        this.addModule(new PopChams());
        this.addModule(new Replenish());
        this.addModule(new ServerLagger());
        this.addModule(new Scaffold());
        this.addModule(new ShaderModule());
        this.addModule(new Skybox());
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
        // Forever modules
        // combat
        this.addModule(new AimPearl());
        this.addModule(new AntiHCQ());
        this.addModule(new AuraPlus());
        this.addModule(new AutoArrow());
        this.addModule(new AutoButton());
        this.addModule(new AutoTrapDoor());
        this.addModule(new BackTrack());
        this.addModule(new CrossbowAim());
        this.addModule(new CrystalBot());
        this.addModule(new CrystalBotPlus());
        this.addModule(new ForceEat());
        this.addModule(new HolePush());
        this.addModule(new MaceGodModule());
        this.addModule(new MaceGodPlus());
        this.addModule(new PearlBlocker());
        this.addModule(new PistonCrystalPlus());
        this.addModule(new SpearAura());
        this.addModule(new TimerMace());
        // movement
        this.addModule(new AntiCobweb());
        this.addModule(new BoatFly());
        this.addModule(new BurstStrafe());
        this.addModule(new ElytraFlight());
        this.addModule(new FastMoveatAi());
        this.addModule(new LieDownMove());
        this.addModule(new StrafeSpeed());
        this.addModule(new SuperLongJump());
        this.addModule(new WindLongJump());
        this.addModule(new WindTP());
        // misc
        this.addModule(new AddFriend());
        this.addModule(new AntiMonkey());
        this.addModule(new AutoExplain());
        this.addModule(new Bot());
        this.addModule(new MissAlert());
        this.addModule(new PearlRun());
        // player
        this.addModule(new AntiBaseKill());
        this.addModule(new AutoKouZi());
        this.addModule(new Driller());
        this.addModule(new ItemAlert());
        this.addModule(new SpeedMine());
        // render
        this.addModule(new Camera());
        this.addModule(new PlayerTrail());
        // exploit
        this.addModule(new BowSpam());
        this.addModule(new MaceSpoofPlus());
        this.addModule(new ThirtyTwoKMace());
        this.modules.sort(Comparator.comparing(Mod::getName));
    }

    public void onKeyReleased(int eventKey) {
        if (eventKey == -1 || eventKey == 0) {
            return;
        }
        this.handleKeyEvent(eventKey, false);
    }

    public void onKeyPressed(int eventKey) {
        if (eventKey == -1 || eventKey == 0) {
            return;
        }
        this.handleKeyEvent(eventKey, true);
    }

    private void handleKeyEvent(int key, boolean isPressed) {
        for (Module module : this.modules) {
            BindSetting bindSetting = module.getBindSetting();
            if (bindSetting.getValue() == key) {
                if (isPressed && ModuleManager.mc.currentScreen == null) {
                    module.toggle();
                    bindSetting.holding = true;
                } else if (!isPressed && bindSetting.isHoldEnable() && bindSetting.holding) {
                    module.toggle();
                    bindSetting.holding = false;
                }
            }
            for (Setting setting : module.getSettings()) {
                BindSetting bind;
                if (!(setting instanceof BindSetting) || (bind = (BindSetting)setting).getValue() != key) continue;
                bind.setPressed(isPressed);
            }
        }
    }

    public void onLogin() {
        for (Module module : this.modules) {
            if (!module.isOn()) continue;
            module.onLogin();
        }
    }

    public void onLogout() {
        for (Module module : this.modules) {
            if (!module.isOn()) continue;
            module.onLogout();
        }
    }

    public void onRender2D(DrawContext drawContext) {
        boolean skipHudModules = false;
        if (ModuleManager.mc.currentScreen instanceof ClickGuiScreen) {
            skipHudModules = ((ClickGuiScreen)ModuleManager.mc.currentScreen).getPage() == ClickGuiScreen.Page.Hud;
        }
        block5: {
            for (Module module : this.modules) {
                if (!module.isOn()) continue;
                if (skipHudModules && module instanceof HudModule) continue;
                try {
                    module.onRender2D(drawContext, mc.getRenderTickCounter().getTickDelta(true));
                }
                catch (Exception e) {
                    e.printStackTrace();
                    if (!ClientSetting.INSTANCE.debug.getValue()) continue;
                    CommandManager.sendMessage("\u00a74An error has occurred (" + module.getName() + " [onRender2D]) Message: [" + e.getMessage() + "]");
                }
            }
            try {
                Lumi.EVENT_BUS.post(Render2DEvent.get(drawContext, mc.getRenderTickCounter().getTickDelta(true)));
            }
            catch (Exception e) {
                e.printStackTrace();
                if (!ClientSetting.INSTANCE.debug.getValue()) break block5;
                CommandManager.sendMessage("\u00a74An error has occurred (Render3DEvent) Message: [" + e.getMessage() + "]");
            }
        }
        this.renderToggleBanners(drawContext);
    }

    public void render3D(MatrixStack matrices) {
        block5: {
            GL11.glEnable((int)2848);
            for (Module module : this.modules) {
                if (!module.isOn()) continue;
                try {
                    module.onRender3D(matrices);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    if (!ClientSetting.INSTANCE.debug.getValue()) continue;
                    CommandManager.sendMessage("\u00a74An error has occurred (" + module.getName() + " [onRender3D]) Message: [" + e.getMessage() + "]");
                }
            }
            try {
                Lumi.EVENT_BUS.post(Render3DEvent.get(matrices, mc.getRenderTickCounter().getTickDelta(true)));
            }
            catch (Exception e) {
                e.printStackTrace();
                if (!ClientSetting.INSTANCE.debug.getValue()) break block5;
                CommandManager.sendMessage("\u00a74An error has occurred (Render3DEvent) Message: [" + e.getMessage() + "]");
            }
        }
        GL11.glDisable((int)2848);
    }

    public void showToggleBanner(Module module, boolean enabled) {
        NotificationsHud.addModuleNotification(module.getDisplayName(), enabled);
    }

    private void renderToggleBanners(DrawContext ctx) {
        this.toggleBanners.clear();
    }

    private void renderPotionListLegacy(DrawContext ctx) {
        if (ModuleManager.mc.player == null) {
            return;
        }
        int margin = 14;
        int startX = margin + 2;
        int startY = margin + 92;
        int pillH = 14;
        int pillPad = 6;
        int idx = 0;
        for (StatusEffectInstance se : ModuleManager.mc.player.getStatusEffects()) {
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
            int x = startX;
            int y = startY + idx * (pillH + 4);
            int keyCodec = 180;
            Render2DUtil.drawRoundedRect(ctx.getMatrices(), x, y, pillW, pillH, 4.0f, new Color(255, 255, 255, keyCodec));
            Render2DUtil.drawRoundedStroke(ctx.getMatrices(), x, y, pillW, pillH, 4.0f, new Color(220, 224, 230, 160), 48);
            int tx = x + pillPad;
            double ty = (double)y + (double)((float)pillH - (customFont ? FontManager.ui.getFontHeight() : TextUtil.getHeight())) / 2.0;
            TextUtil.drawString(ctx, text, tx, ty, new Color(30, 30, 30).getRGB(), customFont);
            if (++idx < 5) continue;
            break;
        }
    }

    public void addModule(Module module) {
        this.modules.add(module);
    }

    public Module getModuleByName(String string) {
        for (Module module : this.modules) {
            if (!module.getName().equalsIgnoreCase(string)) continue;
            return module;
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