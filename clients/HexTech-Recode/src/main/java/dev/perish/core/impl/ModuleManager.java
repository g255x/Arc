package dev.perish.core.impl;


import dev.perish.Perish;

import dev.perish.api.events.impl.Render2DEvent;
import dev.perish.api.events.impl.Render3DEvent;
import dev.perish.api.utils.Wrapper;
import dev.perish.api.utils.render.Render2DUtil;
import dev.perish.api.utils.render.TextUtil;
import dev.perish.mod.Mod;
import dev.perish.mod.gui.hud.HudComponent;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.client.*;
import dev.perish.mod.modules.impl.combat.*;
import dev.perish.mod.modules.impl.exploit.*;
import dev.perish.mod.modules.impl.hud.*;
import dev.perish.mod.modules.impl.misc.*;
import dev.perish.mod.modules.impl.movement.*;
import dev.perish.mod.modules.impl.movement.moveup.MoveUp;
import dev.perish.mod.modules.impl.player.*;
import dev.perish.mod.modules.impl.render.*;
import dev.perish.mod.modules.impl.render.skybox.Skybox;
import dev.perish.mod.modules.impl.render.skybox.SkyFix;
import dev.perish.mod.modules.settings.Setting;
import dev.perish.mod.modules.settings.impl.BindSetting;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class ModuleManager implements Wrapper {
    private final ArrayList<Module> modules = new ArrayList();

    public ModuleManager() {
        this.init();
    }

    public ArrayList<Module> getModules() {
        return this.modules;
    }

    public List<Module> getModulesFromCategory(Module.Category category) {
        List<Module> list = new ArrayList<>();
        for (Module module : this.modules) {
            if (module.getCategory() == category) {
                list.add(module);
            }
        }
        return list;
    }

    public void init() {
        addModule(new ConfigSwitcher());
        addModule(new SkyFix());
        addModule(new Skybox());
        addModule(new SkyColor());
        addModule(new Speed());
        this.addModule(new AutoEnderChest());
        //this.addModule(new BAHalo());
        this.addModule(new CapeModule());
        this.addModule(new MoveUp());
        //this.addModule(new AutoKit());
        //this.addModule(new Bot());
        this.addModule(new Fonts());
        //this.addModule(new NoTerrainScreen());
        this.addModule(new AutoCrystal());
        this.addModule(new Ambience());
        this.addModule(new AntiHunger());
        //this.addModule(new AntiVoid());
        this.addModule(new AutoWalk());
        this.addModule(new VClip());
        this.addModule(new ExtraTab());
        //this.addModule(new AntiWeak());
        //this.addModule(new BedCrafter());
        this.addModule(new Friend());
        this.addModule(new AspectRatio());
        //this.addModule(new ChunkESP());
        this.addModule(new Aura());
        this.addModule(new PistonCrystal());
        this.addModule(new AutoAnchor());
        this.addModule(new AntiAnchor());
        this.addModule(new PhaseESP());
        this.addModule(new AutoArmor());
        this.addModule(new LowArmorAlert());
        this.addModule(new Weather());
        this.addModule(new AutoMine());
        this.addModule(new Helper());
        //this.addModule(new AutoPowder());
        this.addModule(new AutoLog());
        this.addModule(new AutoEZ());
        this.addModule(new SelfTrap());
        this.addModule(new Sorter());
        this.addModule(new AutoMend());
        this.addModule(new AutoPot());
        this.addModule(new AutoFuck());
        this.addModule(new Offhand());
        //this.addModule(new Nuker());
        this.addModule(new AutoTrap());
        //this.addModule(new AutoTrapDoor());
        this.addModule(new AutoWeb());
        this.addModule(new Blink());
        this.addModule(new ChorusControl());
        this.addModule(new BlockStrafe());
        //this.addModule(new FastSwim());
        //this.addModule(new Blocker());
        this.addModule(new Quiver());
        //this.addModule(new BowBomb());
        this.addModule(new BreakESP());
        this.addModule(new BreakerEsp());
        this.addModule(new Burrow());
        this.addModule(new Punctuation());
        //this.addModule(new PearlBot());
        this.addModule(new MaceSpoof());
        this.addModule(new MaceAura());
        this.addModule(new CameraClip());
        this.addModule(new ChatAppend());
        this.addModule(new ClickGui());
        //this.addModule(new InfiniteTrident());
        this.addModule(new ColorsModule());
        //this.addModule(new AutoRegear());
        //this.addModule(new LavaFiller());
        //this.addModule(new AntiPhase());
        this.addModule(new Clip());
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
        this.addModule(new TwoDESP());
        this.addModule(new HoleESP());
        this.addModule(new Tracers());
        this.addModule(new LastWalkEsp());
        this.addModule(new MoveFix());
        this.addModule(new ElytraFly());
        //this.addModule(new PacketLogger());
        this.addModule(new TeleportLogger());
        this.addModule(new SkinFlicker());
        this.addModule(new EntityControl());
        this.addModule(new NameTags());
        this.addModule(new ShulkerViewer());
        this.addModule(new PingSpoof());
        this.addModule(new FakePlayer());
        //this.addModule(new Spammer());
        this.addModule(new MotionCamera());
        this.addModule(new NoRotate());
        this.addModule(new HighLight());
        this.addModule(new FastFall());
        this.addModule(new FastWeb());
        this.addModule(new Flatten());
        this.addModule(new Fly());
        //this.addModule(new Yaw());
        this.addModule(new Freecam());
        this.addModule(new FreeLook());
        this.addModule(new TimerModule());
        this.addModule(new Tips());
        this.addModule(new MusicPlayerModule());
        this.addModule(new IrcChatDisplay());
        this.addModule(new LyricDisplay());
        this.addModule(new ClientSetting());
        this.addModule(new TextRadar());
        this.addModule(new ArrayListDisplay());
        this.addModule(new ArmorDisplay());
        this.addModule(new WaterMarkDisplay());
        this.addModule(new InfoDisplay());
        this.addModule(new CoordsDisplay());
        this.addModule(new PotionsDisplay());
        this.addModule(new TargetHudDisplay());
        this.addModule(new KeyBindsDisplay());
        this.addModule(new CrosshairDisplay());
        this.addModule(new HUD());
        this.addModule(new NoResourcePack());
        //this.addModule(new RocketExtend());
        //this.addModule(new HoleFiller());
        this.addModule(new HoleSnap());
        this.addModule(new LogoutSpots());
        //this.addModule(new AutoTool());
        this.addModule(new Trajectories());
        this.addModule(new KillEffect());
        //this.addModule(new KillThunder());
        this.addModule(new AutoPearl());
        this.addModule(new AntiEffects());
        this.addModule(new NoFall());
        this.addModule(new NoRender());
        this.addModule(new NoSlow());
        this.addModule(new NoSound());
        this.addModule(new AirPlace());
        this.addModule(new Xray());
        this.addModule(new PacketEat());
        //this.addModule(new AutoMace());
        //this.addModule(new PacketFly());
        this.addModule(new SpeedMine());
        this.addModule(new PacketControl());
        this.addModule(new Phase());
        this.addModule(new PlaceRender());
        this.addModule(new CrystalPlaceRender());
        this.addModule(new InteractTweaks());
        this.addModule(new PopChams());
        this.addModule(new Replenish());
        //this.addModule(new ServerLagger());
        this.addModule(new Scaffold());
        this.addModule(new ShaderModule());
        this.addModule(new AntiCrawl());
        //this.addModule(new AntiRegear());
        //this.addModule(new SafeWalk());
        this.addModule(new NoJumpDelay());
        this.addModule(new HighJump());
        this.addModule(new LongJump());
        this.addModule(new Sprint());
        this.addModule(new Strafe());
        this.addModule(new Step());
        this.addModule(new Surround());
        this.addModule(new TotemParticle());
        this.addModule(new Velocity());
        this.addModule(new ViewModel());
        this.addModule(new NameTag2D());
        //this.addModule(new XCarry());
        //this.addModule(new Zoom());
        this.addModule(new ChestStealer());
        this.addModule(new ShulkerGet());
        //this.addModule(new DropAntiRegear());
        //this.addModule(new DropShulkerBox());
        //this.addModule(new AutoDrop());
        //this.addModule(new PopEz());
        this.addModule(new PopEsp());
        //this.addModule(new IQBoost());
        //this.addModule(new EsuESP());
        this.addModule(new TargetFollow());
        this.addModule(new Follower());
        this.addModule(new FakeFlight());
        //this.addModule(new TrapShulker());
        //this.addModule(new ArmorPreserve());
        //this.addModule(new AutoCommand());
        this.addModule(new AutoPush());
        this.addModule(new Disabler());
        //this.addModule(new DeathRespawn());
        this.addModule(new AutoTK());
        this.addModule(new AutoDick());
        //this.addModule(new JumpCircle());
        this.addModule(new FakeName());
        this.addModule(new HudEditor());

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

        for (Module module : this.modules) {
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

            for (Setting setting : module.getSettings()) {
                if (setting instanceof BindSetting bind && bind.getValue() == key) {
                    bind.setPressed(isPressed);
                }
            }
        }
    }

    public void onLogin() {

        for (Module module : this.modules) {
            if (module.isOn()) {
                module.onLogin();
            }
        }
    }

    public void onLogout() {

        for (Module module : this.modules) {
            if (module.isOn()) {
                module.onLogout();
            }
        }
    }

    public void onRender2D(DrawContext drawContext) {

        for (Module module : this.modules) {
            if (module.isOn()) {
                try {
                    module.onRender2D(drawContext, mc.getRenderTickCounter().getTickDelta(true));
                } catch (Exception var6) {
                    var6.printStackTrace();
                    if (ClientSetting.INSTANCE.debug.getValue()) {
                        CommandManager.sendMessage("§4An error has occurred (" + module.getName() + " [onRender2D]) Message: [" + var6.getMessage() + "]");
                    }
                }
            }
        }

        try {
            Perish.EVENT_BUS.post(Render2DEvent.get(drawContext, mc.getRenderTickCounter().getTickDelta(true)));
        } catch (Exception var5) {
            var5.printStackTrace();
            if (ClientSetting.INSTANCE.debug.getValue()) {
                CommandManager.sendMessage("§4An error has occurred (Render3DEvent) Message: [" + var5.getMessage() + "]");
            }
        }

        renderHudComponents(drawContext);
    }

    private void renderHudComponents(DrawContext drawContext) {
        if (Perish.HUD_EDITOR == null) return;
        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);

        for (HudComponent component : Perish.HUD_EDITOR.getComponents()) {
            if (!component.isVisible()) continue;

            try { component.render(drawContext, tickDelta); } catch (Exception e) { e.printStackTrace(); }

            if (Perish.HUD_EDITOR.isEditMode()) {
                boolean selected = Perish.HUD_EDITOR.isSelected(component);
                component.renderOverlay(drawContext, selected);
            }
        }
    }

    public void render3D(MatrixStack matrices) {

        GL11.glEnable(2848);

        for (Module module : this.modules) {
            if (module.isOn()) {
                try {
                    module.onRender3D(matrices);
                } catch (Exception var6) {
                    var6.printStackTrace();
                    if (ClientSetting.INSTANCE.debug.getValue()) {
                        CommandManager.sendMessage("§4An error has occurred (" + module.getName() + " [onRender3D]) Message: [" + var6.getMessage() + "]");
                    }
                }
            }
        }

        try {
            Perish.EVENT_BUS.post(Render3DEvent.get(matrices, mc.getRenderTickCounter().getTickDelta(true)));
        } catch (Exception var5) {
            var5.printStackTrace();
            if (ClientSetting.INSTANCE.debug.getValue()) {
                CommandManager.sendMessage("§4An error has occurred (Render3DEvent) Message: [" + var5.getMessage() + "]");
            }
        }

        GL11.glDisable(2848);
    }


    private void renderPotionListLegacy(DrawContext ctx) {
        if (mc.player != null) {
            int margin = 14;
            int startX = margin + 2;
            int startY = margin + 92;
            int pillH = 14;
            int pillPad = 6;
            int idx = 0;

            for (StatusEffectInstance se : mc.player.getStatusEffects()) {
                String name = se.getEffectType().value().getName().getString();
                int ticks = se.getDuration();
                int totalSec = Math.max(0, ticks / 20);
                int mm = totalSec / 60;
                int ss = totalSec % 60;
                String time = String.format("%d:%02d", mm, ss);
                String text = name + " " + time;
                int tw = ClickGui.INSTANCE.font.getValue() ? (int) FontManager.ui.getWidth(text) : (int) TextUtil.getWidth(text);
                int pillW = tw + pillPad * 2;
                int y = startY + idx * (pillH + 4);
                int a = 180;
                Render2DUtil.drawRoundedRect(ctx.getMatrices(), startX, y, pillW, pillH, 4.0F, new Color(255, 255, 255, a));
                Render2DUtil.drawRoundedStroke(ctx.getMatrices(), startX, y, pillW, pillH, 4.0F, new Color(220, 224, 230, 160), 48);
                int tx = startX + pillPad;
                double ty = y + (pillH - (ClickGui.INSTANCE.font.getValue() ? FontManager.ui.getFontHeight() : TextUtil.getHeight())) / 2.0;
                TextUtil.drawString(ctx, text, tx, ty, new Color(30, 30, 30).getRGB(), ClickGui.INSTANCE.font.getValue());
                if (++idx >= 5) {
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
        for (Module m : this.modules) {
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
        for (Module module : this.modules) {
            if (module.getName().equalsIgnoreCase(string)) {
                return module;
            }
        }

        return null;
    }

    public <T> T getModule(Class<T> clazz) {
        for (Module module : this.modules) {
            if (module.getClass() == clazz) {
                return (T) module;
            }
        }
        return null;
    }
}
