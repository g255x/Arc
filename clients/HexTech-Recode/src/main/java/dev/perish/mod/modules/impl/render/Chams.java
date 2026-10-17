package dev.perish.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.Render3DEvent;
import dev.perish.api.events.impl.UpdateEvent;
import dev.perish.api.utils.render.ColorUtil;
import dev.perish.api.utils.render.EntityModelChamsRenderer;
import dev.perish.api.utils.render.ModelPlayer;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.ColorSetting;
import dev.perish.mod.modules.settings.impl.EnumSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import java.awt.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.client.util.math.MatrixStack;

public class Chams extends Module {
    public static Chams INSTANCE;

    public final BooleanSetting crystal = this.add(new BooleanSetting("Crystal", true).setParent());
    public final BooleanSetting custom = this.add(new BooleanSetting("Custom", false, this.crystal::isOpen).setParent());
    public final BooleanSetting depth = this.add(new BooleanSetting("Depth", false, () -> this.crystal.isOpen() && this.custom.isOpen()));
    public final BooleanSetting crystalPulse = this.add(new BooleanSetting("CrystalPulse", false, this.crystal::isOpen));
    public final BooleanSetting crystalShine = this.add(new BooleanSetting("CrystalShine", false, this.crystal::isOpen));
    public final EnumSetting<Mode> crystalMode = this.add(new EnumSetting<>("CrystalMode", Mode.Both, this.crystal::isOpen));
    public final BooleanSetting chamsTexture = this.add(new BooleanSetting("ChamsTexture", true, () -> this.crystal.isOpen() && this.custom.isOpen()));
    public final ColorSetting fill = this.add(
            new ColorSetting("Fill", new Color(130, 202, 255, 40), () -> this.crystal.isOpen() && this.crystalMode.getValue() != Mode.Outline).injectBoolean(true)
    );
    public final ColorSetting line = this.add(
            new ColorSetting("Line", new Color(130, 202, 255, 120), () -> this.crystal.isOpen() && this.crystalMode.getValue() != Mode.Fill).injectBoolean(true)
    );
    public final ColorSetting core = this.add(new ColorSetting("Core", new Color(255, 255, 255, 255), this.crystal::isOpen).injectBoolean(true));
    public final ColorSetting outerFrame = this.add(new ColorSetting("OuterFrame", new Color(255, 255, 255, 255), this.crystal::isOpen).injectBoolean(true));
    public final ColorSetting innerFrame = this.add(new ColorSetting("InnerFrame", new Color(255, 255, 255, 255), this.crystal::isOpen).injectBoolean(true));
    public final BooleanSetting glint = this.add(new BooleanSetting("Glint", true, this.crystal::isOpen));
    public final BooleanSetting texture = this.add(new BooleanSetting("Texture", true, this.crystal::isOpen));
    public final BooleanSetting spinSync = this.add(new BooleanSetting("SpinSync", false, this.crystal::isOpen));
    public final SliderSetting scale = this.add(new SliderSetting("Scale", 1.0, 0.0, 3.0, 0.01, this.crystal::isOpen));
    public final SliderSetting spinValue = this.add(new SliderSetting("SpinSpeed", 1.0, 0.0, 10.0, 0.01, this.crystal::isOpen));
    public final SliderSetting bounceHeight = this.add(new SliderSetting("BounceHeight", 1.0, 0.0, 3.0, 0.01, this.crystal::isOpen));
    public final SliderSetting floatValue = this.add(new SliderSetting("BounceSpeed", 1.0, 0.0, 3.0, 0.01, this.crystal::isOpen));
    public final SliderSetting floatOffset = this.add(new SliderSetting("YOffset", 0.0, -1.0, 1.0, 0.01, this.crystal::isOpen));

    public final BooleanSetting entities = this.add(new BooleanSetting("Entities", true).setParent());
    public final BooleanSetting players = this.add(new BooleanSetting("Players", true, this.entities::isOpen));
    public final BooleanSetting hostiles = this.add(new BooleanSetting("Hostiles", false, this.entities::isOpen));
    public final BooleanSetting passives = this.add(new BooleanSetting("Passives", false, this.entities::isOpen));
    public final BooleanSetting self = this.add(new BooleanSetting("Self", false, this.entities::isOpen));
    public final BooleanSetting entityPulse = this.add(new BooleanSetting("EntityPulse", false, this.entities::isOpen));
    public final BooleanSetting entityShine = this.add(new BooleanSetting("EntityShine", false, this.entities::isOpen));
    public final EnumSetting<Mode> entityMode = this.add(new EnumSetting<>("EntityMode", Mode.Both, this.entities::isOpen));
    public final EnumSetting<FriendColorMode> friendMode = this.add(new EnumSetting<>("FriendMode", FriendColorMode.Default, this.entities::isOpen));
    public final ColorSetting entityFill = this.add(
            new ColorSetting("EntityFill", new Color(255, 255, 255, 100), () -> this.entities.isOpen() && this.entityMode.getValue() != Mode.Outline).injectBoolean(true)
    );
    public final ColorSetting entityLine = this.add(
            new ColorSetting("EntityLine", new Color(255, 255, 255, 255), () -> this.entities.isOpen() && this.entityMode.getValue() != Mode.Fill).injectBoolean(true)
    );
    public final ColorSetting friendFill = this.add(
            new ColorSetting("FriendFill", new Color(85, 255, 255, 100), () -> this.entities.isOpen() && this.friendMode.getValue() == FriendColorMode.Custom && this.entityMode.getValue() != Mode.Outline).injectBoolean(true)
    );
    public final ColorSetting friendLine = this.add(
            new ColorSetting("FriendLine", new Color(85, 255, 255, 255), () -> this.entities.isOpen() && this.friendMode.getValue() == FriendColorMode.Custom && this.entityMode.getValue() != Mode.Fill).injectBoolean(true)
    );
    public final BooleanSetting damageModify = this.add(new BooleanSetting("DamageModify", false, this.entities::isOpen));
    public final ColorSetting damageColor = this.add(
            new ColorSetting("DamageColor", new Color(255, 0, 0, 255), () -> this.entities.isOpen() && this.damageModify.getValue())
    );

    public final BooleanSetting throughWall = this.add(new BooleanSetting("ThroughWall", false).setParent());
    public final ColorSetting hand = this.add(new ColorSetting("Hand", -1).injectBoolean(true));
    private final BooleanSetting Crystals = this.add(new BooleanSetting("Crystals", true, this.throughWall::isOpen));
    private final BooleanSetting Players = this.add(new BooleanSetting("Players", true, this.throughWall::isOpen));
    private final BooleanSetting Mobs = this.add(new BooleanSetting("Mobs", true, this.throughWall::isOpen));
    private final BooleanSetting Animals = this.add(new BooleanSetting("Animals", true, this.throughWall::isOpen));
    private final BooleanSetting Villagers = this.add(new BooleanSetting("Villagers", true, this.throughWall::isOpen));
    private final BooleanSetting Slimes = this.add(new BooleanSetting("Slimes", true, this.throughWall::isOpen));
    public int age;

    public Chams() {
        super("Chams", Module.Category.Render);
        this.setChinese("模型上色");
        INSTANCE = this;
    }

    public boolean customCrystal() {
        return this.isOn() && this.crystal.getValue();
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        this.age++;
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (nullCheck() || !this.entities.getValue()) {
            return;
        }

        RenderSystem.depthMask(false);
        try {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (!this.shouldRenderEntityChams(player)) {
                    continue;
                }

                new ModelPlayer(player).render(matrixStack, this.getEntityFillSetting(player), this.getEntityLineSetting(player));
            }
        } finally {
            RenderSystem.depthMask(true);
        }
    }

    @EventListener(priority = Integer.MIN_VALUE)
    public void onRender3D(Render3DEvent event) {
        if (!nullCheck() && this.customCrystal() && !this.custom.getValue()) {
            ColorSetting fillSetting = this.getCrystalFillSetting();
            ColorSetting lineSetting = this.getCrystalLineSetting();
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof EndCrystalEntity crystal && !crystal.isRemoved()) {
                        EntityModelChamsRenderer.render(crystal, event.matrixStack, event.tickDelta, fillSetting, lineSetting, this.crystalShine.getValue(), this.depth.getValue());
                }
            }
        }

        if (this.hand.booleanValue) {
            Color color = this.hand.getValue();
            RenderSystem.setShaderColor(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
        }
    }

    public boolean chams(Entity entity) {
        if (entity instanceof EndCrystalEntity) {
            return this.Crystals.getValue();
        } else if (entity instanceof SlimeEntity) {
            return this.Slimes.getValue();
        } else if (entity instanceof PlayerEntity) {
            return this.Players.getValue();
        } else if (entity instanceof VillagerEntity || entity instanceof WanderingTraderEntity) {
            return this.Villagers.getValue();
        } else if (entity instanceof AnimalEntity) {
            return this.Animals.getValue();
        } else {
            return entity instanceof MobEntity && this.Mobs.getValue();
        }
    }

    public ColorSetting getCrystalFillSetting() {
        return this.createRenderSetting(
                this.getDisplayColor(this.fill.getValue(), this.crystalPulse.getValue()),
                this.fill.booleanValue && this.crystalMode.getValue() != Mode.Outline
        );
    }

    public ColorSetting getCrystalLineSetting() {
        return this.createRenderSetting(
                this.getDisplayColor(this.line.getValue(), this.crystalPulse.getValue()),
                this.line.booleanValue && this.crystalMode.getValue() != Mode.Fill
        );
    }

    public boolean shouldRenderEntityChams(LivingEntity entity) {
        if (!this.isOn() || !this.entities.getValue() || entity == null || entity.isRemoved() || !entity.isAlive()) {
            return false;
        }

        if (entity instanceof PlayerEntity player) {
            if (player == mc.player) {
                return this.self.getValue() && !mc.options.getPerspective().isFirstPerson();
            }
            return this.players.getValue();
        }

        SpawnGroup spawnGroup = entity.getType().getSpawnGroup();
        if (this.hostiles.getValue() && spawnGroup == SpawnGroup.MONSTER) {
            return true;
        }

        return this.passives.getValue() && (
                spawnGroup == SpawnGroup.CREATURE
                        || spawnGroup == SpawnGroup.WATER_CREATURE
                        || spawnGroup == SpawnGroup.WATER_AMBIENT
                        || spawnGroup == SpawnGroup.UNDERGROUND_WATER_CREATURE
                        || spawnGroup == SpawnGroup.AXOLOTLS
        );
    }

    public boolean shouldApplyEntityShine() {
        return this.isOn() && this.entities.getValue() && this.entityShine.getValue();
    }

    public Color getEntityRenderColor(LivingEntity entity) {
        Color baseColor = switch (this.entityMode.getValue()) {
            case Outline -> this.entityLine.getValue();
            case Both, Fill -> this.entityFill.getValue();
        };
        Color friendColor = this.getFriendColor(this.entityMode.getValue() != Mode.Outline, baseColor.getAlpha());
        Color hurtColor = new Color(
                this.damageColor.getValue().getRed(),
                this.damageColor.getValue().getGreen(),
                this.damageColor.getValue().getBlue(),
                baseColor.getAlpha()
        );

        Color color = baseColor;
        if (this.damageModify.getValue() && entity.hurtTime > 0) {
            color = hurtColor;
        } else if (entity instanceof PlayerEntity player && Perish.FRIEND.isFriend(player) && this.friendMode.getValue() != FriendColorMode.Sync) {
            color = friendColor;
        }

        return this.getDisplayColor(color, this.entityPulse.getValue());
    }

    public ColorSetting getEntityFillSetting(Entity entity) {
        return this.createRenderSetting(this.getEntityFillColor(entity), this.entityMode.getValue() != Mode.Outline && this.entityFill.booleanValue);
    }

    public ColorSetting getEntityLineSetting(Entity entity) {
        return this.createRenderSetting(this.getEntityLineColor(entity), this.entityMode.getValue() != Mode.Fill && this.entityLine.booleanValue);
    }

    private Color getEntityFillColor(Entity entity) {
        return this.getEntityLayerColor(entity, true);
    }

    private Color getEntityLineColor(Entity entity) {
        return this.getEntityLayerColor(entity, false);
    }

    private Color getEntityLayerColor(Entity entity, boolean fillColor) {
        Color baseColor = fillColor ? this.entityFill.getValue() : this.entityLine.getValue();
        Color friendColor = this.getFriendColor(fillColor, baseColor.getAlpha());
        Color hurtColor = new Color(
                this.damageColor.getValue().getRed(),
                this.damageColor.getValue().getGreen(),
                this.damageColor.getValue().getBlue(),
                baseColor.getAlpha()
        );

        Color color = baseColor;
        if (this.damageModify.getValue() && entity instanceof LivingEntity livingEntity && livingEntity.hurtTime > 0) {
            color = hurtColor;
        } else if (entity instanceof PlayerEntity player && Perish.FRIEND.isFriend(player) && this.friendMode.getValue() != FriendColorMode.Sync) {
            color = friendColor;
        }

        return this.getDisplayColor(color, this.entityPulse.getValue());
    }

    private Color getFriendColor(boolean fillColor, int alpha) {
        if (this.friendMode.getValue() == FriendColorMode.Custom) {
            return fillColor ? this.friendFill.getValue() : this.friendLine.getValue();
        }

        return new Color(85, 255, 255, alpha);
    }

    private Color getDisplayColor(Color color, boolean pulse) {
        if (!pulse) {
            return color;
        }

        Color pulseColor = ColorUtil.pulseColor(color, 0.0, 10, 1.0);
        return new Color(pulseColor.getRed(), pulseColor.getGreen(), pulseColor.getBlue(), color.getAlpha());
    }

    private ColorSetting createRenderSetting(Color color, boolean enabled) {
        return new ColorSetting("Render", color).injectBoolean(enabled);
    }

    public enum FriendColorMode {
        Default,
        Custom,
        Sync
    }

    public enum Mode {
        Fill,
        Outline,
        Both
    }
}
