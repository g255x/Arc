package dev.kissoo.mod.modules.impl.render;

import dev.kissoo.Forever;
import dev.kissoo.core.impl.ShaderParams;
import hoprc.asm.accessors.IGameRenderer;
import hoprc.api.core.impl.ShaderManager;
import hoprc.api.core.impl.ShaderManager.Shader;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.function.BooleanSupplier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

public class ShaderModule extends Module {
    public static ShaderModule INSTANCE;

    private final EnumSetting<ShaderModule.Page> page = this.add(new EnumSetting("Page", ShaderModule.Page.Shader));

    public final EnumSetting<ShaderManager.Shader> mode = this.add(
            new EnumSetting("Mode", Shader.Flow, () -> this.page.getValue() == ShaderModule.Page.Shader && this.group.is(Group.Global))
    );

    public final SliderSetting speed = this.add(
            new SliderSetting("Speed", 4.0, 0.0, 20.0, 0.1, () -> this.page.getValue() == ShaderModule.Page.Shader && this.group.is(Group.Global))
    );

    public final ColorSetting fill = this.add(
            new ColorSetting("Color", (BooleanSupplier) (() -> this.page.getValue() == ShaderModule.Page.Shader && this.group.is(Group.Global)))
    );

    public final SliderSetting maxSample = this.add(
            new SliderSetting("MaxSample", 10.0, 0.0, 20.0, () -> this.page.getValue() == ShaderModule.Page.Shader && this.group.is(Group.Global))
    );

    public final SliderSetting divider = this.add(
            new SliderSetting("Divider", 150.0, 0.0, 300.0, () -> this.page.getValue() == ShaderModule.Page.Shader && this.group.is(Group.Global))
    );

    public final SliderSetting radius = this.add(
            new SliderSetting("Radius", 2.0, 0.0, 6.0, () -> this.page.getValue() == ShaderModule.Page.Shader && this.group.is(Group.Global))
    );

    public final SliderSetting smoothness = this.add(
            new SliderSetting("Smoothness", 1.0, 0.0, 1.0, 0.01, () -> this.page.getValue() == ShaderModule.Page.Shader && this.group.is(Group.Global))
    );

    public final SliderSetting alpha = this.add(
            new SliderSetting("GlowAlpha", 255, 0, 255, () -> this.page.getValue() == ShaderModule.Page.Shader && this.group.is(Group.Global))
    );

    public final SliderSetting maxRange = this.add(
            new SliderSetting("MaxRange", 64, 16, 512, () -> this.page.getValue() == ShaderModule.Page.Target)
    );

    public final SliderSetting pulseSpeed = this.add(
            new SliderSetting("PulseSize", 10.0, 0.0, 200.0, 0.1, () -> this.page.getValue() == ShaderModule.Page.Color && this.group.is(Group.Global))
    );

    public final ColorSetting pulse = this.add(
            new ColorSetting("Pulse", new Color(255, 255, 255), () -> this.page.getValue() == ShaderModule.Page.Color && this.group.is(Group.Global))
    );

    public final SliderSetting step = this.add(
            new SliderSetting("Step", 0.5, 0.0, 4.0, 0.01F, () -> this.page.getValue() == ShaderModule.Page.Color && this.group.is(Group.Global))
    );

    public final SliderSetting octaves = this.add(
            new SliderSetting("Octaves", 10, 5, 30, () -> this.page.getValue() == ShaderModule.Page.Color && this.group.is(Group.Global))
    );

    public final ColorSetting smoke1 = this.add(
            new ColorSetting("Color1", new Color(255, 255, 255), () -> this.page.getValue() == ShaderModule.Page.Color && this.group.is(Group.Global))
    );

    public final ColorSetting smoke2 = this.add(
            new ColorSetting("Color2", new Color(255, 255, 255), () -> this.page.getValue() == ShaderModule.Page.Color && this.group.is(Group.Global))
    );

    public final ColorSetting smoke3 = this.add(
            new ColorSetting("Color3", new Color(255, 255, 255), () -> this.page.getValue() == ShaderModule.Page.Color && this.group.is(Group.Global))
    );

    public final ColorSetting smoke4 = this.add(
            new ColorSetting("Color4", new Color(255, 255, 255), () -> this.page.getValue() == ShaderModule.Page.Color && this.group.is(Group.Global))
    );

    private final BooleanSetting hands = this.add(
            new BooleanSetting("Hands", true, () -> this.page.getValue() == ShaderModule.Page.Target)
    );

    private final BooleanSetting lighting = this.add(
            new BooleanSetting("Lighting", false, () -> this.page.getValue() == ShaderModule.Page.Target)
    );

    private final BooleanSetting self = this.add(
            new BooleanSetting("Self", true, () -> this.page.getValue() == ShaderModule.Page.Target)
    );

    private final BooleanSetting players = this.add(
            new BooleanSetting("Players", true, () -> this.page.getValue() == ShaderModule.Page.Target)
    );

    private final BooleanSetting friends = this.add(
            new BooleanSetting("Friends", true, () -> this.page.getValue() == ShaderModule.Page.Target)
    );

    private final BooleanSetting crystals = this.add(
            new BooleanSetting("Crystals", true, () -> this.page.getValue() == ShaderModule.Page.Target)
    );

    private final BooleanSetting creatures = this.add(
            new BooleanSetting("Creatures", false, () -> this.page.getValue() == ShaderModule.Page.Target)
    );

    private final BooleanSetting monsters = this.add(
            new BooleanSetting("Monsters", false, () -> this.page.getValue() == ShaderModule.Page.Target)
    );

    private final BooleanSetting ambients = this.add(
            new BooleanSetting("Ambients", false, () -> this.page.getValue() == ShaderModule.Page.Target)
    );

    private final BooleanSetting items = this.add(
            new BooleanSetting("Items", true, () -> this.page.getValue() == ShaderModule.Page.Target)
    );

    private final BooleanSetting others = this.add(
            new BooleanSetting("Others", false, () -> this.page.getValue() == ShaderModule.Page.Target)
    );

    public ShaderModule() {
        super("Shader", Module.Category.Render);
        this.setChinese("着色器");
        INSTANCE = this;
        this.groups.put(Group.Global, new ShaderGroup(
                this.mode, this.speed, this.fill, this.maxSample, this.divider, this.radius,
                this.smoothness, this.alpha, this.pulse, this.pulseSpeed, this.step, this.octaves,
                this.smoke1, this.smoke2, this.smoke3, this.smoke4));
        this.groups.put(Group.Players, createGroup(Group.Players));
        this.groups.put(Group.Crystals, createGroup(Group.Crystals));
        this.groups.put(Group.Hands, createGroup(Group.Hands));
    }

    private ShaderGroup createGroup(Group g) {
        String p = g.name();
        BooleanSupplier shaderVisible = () -> this.page.getValue() == Page.Shader && this.group.is(g);
        BooleanSupplier colorVisible = () -> this.page.getValue() == Page.Color && this.group.is(g);
        return new ShaderGroup(
                this.add(new EnumSetting<>("Mode" + p, Shader.Flow, shaderVisible)),
                this.add(new SliderSetting("Speed" + p, 4.0, 0.0, 20.0, 0.1, shaderVisible)),
                this.add(new ColorSetting("Color" + p, colorVisible)),
                this.add(new SliderSetting("MaxSample" + p, 10.0, 0.0, 20.0, shaderVisible)),
                this.add(new SliderSetting("Divider" + p, 150.0, 0.0, 300.0, shaderVisible)),
                this.add(new SliderSetting("Radius" + p, 2.0, 0.0, 6.0, shaderVisible)),
                this.add(new SliderSetting("Smoothness" + p, 1.0, 0.0, 1.0, 0.01, shaderVisible)),
                this.add(new SliderSetting("GlowAlpha" + p, 255, 0, 255, shaderVisible)),
                this.add(new ColorSetting("Pulse" + p, new Color(255, 255, 255), colorVisible)),
                this.add(new SliderSetting("PulseSize" + p, 10.0, 0.0, 200.0, 0.1, colorVisible)),
                this.add(new SliderSetting("Step" + p, 0.5, 0.0, 4.0, 0.01F, colorVisible)),
                this.add(new SliderSetting("Octaves" + p, 10, 5, 30, colorVisible)),
                this.add(new ColorSetting("Color1" + p, new Color(255, 255, 255), colorVisible)),
                this.add(new ColorSetting("Color2" + p, new Color(255, 255, 255), colorVisible)),
                this.add(new ColorSetting("Color3" + p, new Color(255, 255, 255), colorVisible)),
                this.add(new ColorSetting("Color4" + p, new Color(255, 255, 255), colorVisible))
        );
    }

    @Override
    public String getInfo() {
        Group g = this.group.getValue();
        ShaderGroup sg = this.groups.get(g);
        return ((sg != null ? sg.mode.getValue() : this.mode.getValue())).name();
    }

    public boolean shouldRenderLighting() {
        return this.isOn() && this.lighting.getValue();
    }

    public boolean shouldRender(Entity entity) {
        if (entity == null) {
            return false;
        } else if (mc.player == null) {
            return false;
        } else if (MathHelper.sqrt((float) mc.player.squaredDistanceTo(entity.getPos())) > this.maxRange.getValue()) {
            return false;
        } else {
            if (entity instanceof PlayerEntity playerEntity) {
                if (entity == mc.player) {
                    return this.self.getValue();
                } else {
                    if (Forever.FRIEND.isFriend(playerEntity)) {
                        return this.friends.getValue();
                    }
                    return this.players.getValue();
                }
            } else if (entity instanceof EndCrystalEntity) {
                return this.crystals.getValue();
            } else if (entity instanceof LightningEntity) {
                return this.lighting.getValue();
            } else if (entity instanceof ItemEntity) {
                return this.items.getValue();
            } else {
                return switch (entity.getType().getSpawnGroup()) {
                    case CREATURE, WATER_CREATURE -> this.creatures.getValue();
                    case MONSTER -> this.monsters.getValue();
                    case AMBIENT, WATER_AMBIENT -> this.ambients.getValue();
                    default -> this.others.getValue();
                };
            }
        }
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (this.hands.getValue()) {
            ShaderGroup h = this.groups.get(Group.Hands);
            if (h != null) {
                Forever.SHADER.renderShader(
                        () -> ((IGameRenderer) mc.gameRenderer).IRenderHand(
                                mc.gameRenderer.getCamera(),
                                mc.getRenderTickCounter().getTickDelta(true),
                                matrixStack.peek().getPositionMatrix()
                        ),
                        h.mode.getValue(),
                        ShaderParams.fromModule(this, Group.Hands)
                );
            }
        }
    }

    // ── 分组访问 ──
    public ShaderGroup getGroup(Group g) {
        return this.groups.get(g);
    }

    public ShaderManager.Shader getMode(Group g) {
        ShaderGroup sg = this.groups.get(g);
        return sg != null ? sg.mode.getValue() : this.mode.getValue();
    }

    public ShaderParams getParams(Group g) {
        return ShaderParams.fromModule(this, g);
    }

    /** 实体所属着色器分组：玩家→Players、水晶→Crystals、其余→null（走全局发光） */
    public Group getGroupForEntity(Entity entity) {
        if (entity instanceof EndCrystalEntity) return Group.Crystals;
        if (entity instanceof PlayerEntity) return Group.Players;
        return null;
    }

    @Override
    public void onToggle() {
        Forever.SHADER.reloadShaders();
    }

    @Override
    public void onLogin() {
        Forever.SHADER.reloadShaders();
    }

    private static enum Page {
        Shader,
        Target,
        Color;
    }

    // 着色器分组：各目标类型独立一套参数
    public static enum Group {
        Global,
        Players,
        Crystals,
        Hands;
    }

    // 一组着色器参数设置（Global 复用现有字段，其余各组新建）
    public static class ShaderGroup {
        public final EnumSetting<ShaderManager.Shader> mode;
        public final SliderSetting speed;
        public final ColorSetting color;
        public final SliderSetting maxSample;
        public final SliderSetting divider;
        public final SliderSetting radius;
        public final SliderSetting smoothness;
        public final SliderSetting alpha;
        public final ColorSetting pulse;
        public final SliderSetting pulseSpeed;
        public final SliderSetting step;
        public final SliderSetting octaves;
        public final ColorSetting smoke1;
        public final ColorSetting smoke2;
        public final ColorSetting smoke3;
        public final ColorSetting smoke4;

        public ShaderGroup(
                EnumSetting<ShaderManager.Shader> mode, SliderSetting speed, ColorSetting color,
                SliderSetting maxSample, SliderSetting divider, SliderSetting radius,
                SliderSetting smoothness, SliderSetting alpha, ColorSetting pulse,
                SliderSetting pulseSpeed, SliderSetting step, SliderSetting octaves,
                ColorSetting smoke1, ColorSetting smoke2, ColorSetting smoke3, ColorSetting smoke4) {
            this.mode = mode;
            this.speed = speed;
            this.color = color;
            this.maxSample = maxSample;
            this.divider = divider;
            this.radius = radius;
            this.smoothness = smoothness;
            this.alpha = alpha;
            this.pulse = pulse;
            this.pulseSpeed = pulseSpeed;
            this.step = step;
            this.octaves = octaves;
            this.smoke1 = smoke1;
            this.smoke2 = smoke2;
            this.smoke3 = smoke3;
            this.smoke4 = smoke4;
        }
    }

    public final EnumSetting<Group> group = this.add(new EnumSetting("Group", Group.Global, () -> this.page.getValue() == Page.Shader));
    public final java.util.Map<Group, ShaderGroup> groups = new java.util.EnumMap<>(Group.class);
}
