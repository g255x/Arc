package dev.g255.arc.mod.modules.impl.render;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.PacketEvent;
import dev.g255.arc.api.events.impl.Render2DEvent;
import dev.g255.arc.api.events.impl.TotemEvent;
import dev.g255.arc.api.utils.render.Render2DUtil;
import dev.g255.arc.api.utils.render.TextUtil;
import dev.g255.arc.core.impl.FontManager;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.player.Freecam;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector4d;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Formatting;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.potion.Potions;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.world.GameMode;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.DataComponentTypes;
import java.util.Map;

public class NameTag2D extends Module {
    public static NameTag2D INSTANCE;
    public final EnumSetting<Font> font = add(new EnumSetting<>("FontMode", Font.Fast));
    public final EnumSetting<Armor> armorMode = add(new EnumSetting<>("ArmorMode", Armor.Full));
    private final SliderSetting scale = add(new SliderSetting("Scale", 0.68f, 0.1f, 2f, 0.01));
    private final SliderSetting minScale = add(new SliderSetting("MinScale", 0.2f, 0.1f, 1f, 0.01));
    private final SliderSetting scaled = add(new SliderSetting("Scaled", 1, 0, 2, 0.01));
    private final SliderSetting offset = add(new SliderSetting("Offset", 0.315f, 0.001f, 1f, 0.001));
    private final SliderSetting height = add(new SliderSetting("Height", 0, -3, 3, 0.01));
    private final BooleanSetting god = add(new BooleanSetting("God", true));
    private final BooleanSetting gamemode = add(new BooleanSetting("Gamemode", false));
    private final BooleanSetting ping = add(new BooleanSetting("Ping", false));
    private final BooleanSetting health = add(new BooleanSetting("Health", true));
    private final BooleanSetting distance = add(new BooleanSetting("Distance", true));
    private final BooleanSetting pops = add(new BooleanSetting("TotemPops", true));
    private final BooleanSetting enchants = add(new BooleanSetting("Enchants", true));
    private final BooleanSetting turtle = add(new BooleanSetting("Turtle", true));
    private final ColorSetting outline =
            add(new ColorSetting("Outline", new Color(0x99FFFFFF, true)).injectBoolean(true));
    private final ColorSetting rect =
            add(new ColorSetting("Rect", new Color(0x99000001, true)).injectBoolean(true));
    private final ColorSetting friendColor =
            add(new ColorSetting("FriendColor", new Color(0xFF1DFF1D, true)));
    private final ColorSetting color = add(new ColorSetting("Color", new Color(0xFFFFFFFF, true)));
    private final SliderSetting armorHeight = add(new SliderSetting("ArmorHeight", 0.3f, -10, 10f));
    private final SliderSetting armorScale =
            add(new SliderSetting("ArmorScale", 0.9f, 0.1f, 2f, 0.01f));
    private final Map<Integer, Long> turtlePlayers = new ConcurrentHashMap<>();

    public NameTag2D() {
        super("NameTag2D", Category.Render);
        setChinese("2D名字标签");
        INSTANCE = this;
    }

    public static String getEntityPing(PlayerEntity entity) {
        if (mc.getNetworkHandler() == null) return "-1";
        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
        if (playerListEntry == null) return "-1";
        int ping = playerListEntry.getLatency();
        Formatting color = Formatting.GREEN;
        if (ping >= 100) {
            color = Formatting.YELLOW;
        }
        if (ping >= 250) {
            color = Formatting.RED;
        }
        return color.toString() + ping;
    }

    public static GameMode getEntityGamemode(PlayerEntity entity) {
        if (entity == null) return null;
        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
        return playerListEntry == null ? null : playerListEntry.getGameMode();
    }

    public static float round2(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(1, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    @EventListener
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.getPacket() instanceof WorldEventS2CPacket packet && packet.getEventId() == 2002) {
            Vec3d splashPos = Vec3d.ofCenter(packet.getPos());
            PotionEntity potionEntity = null;
            double minDst = Double.MAX_VALUE;

            for (Entity e : mc.world.getEntities()) {
                if (e instanceof PotionEntity pe) {
                    double dst = e.getPos().squaredDistanceTo(splashPos);
                    if (dst < 16.0 && dst < minDst) {
                        minDst = dst;
                        potionEntity = pe;
                    }
                }
            }

            if (potionEntity != null) {
                ItemStack stack = potionEntity.getStack();
                PotionContentsComponent contents =
                        stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
                if (contents.matches(Potions.TURTLE_MASTER)
                        || contents.matches(Potions.STRONG_TURTLE_MASTER)
                        || contents.matches(Potions.LONG_TURTLE_MASTER)) {
                    for (PlayerEntity player : mc.world.getPlayers()) {
                        double dist = player.squaredDistanceTo(splashPos);
                        if (dist < 16.0) {
                            int baseDuration = 400;
                            if (contents.matches(Potions.LONG_TURTLE_MASTER)) baseDuration = 800;

                            double impact = 1.0 - Math.sqrt(dist) / 4.0;
                            if (impact > 0) {
                                int duration = (int) (impact * baseDuration + 0.5);
                                if (duration > 20) {
                                    turtlePlayers.put(
                                            player.getId(), System.currentTimeMillis() + duration * 50L);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventListener
    public void onTotemPop(TotemEvent event) {
        PlayerEntity player = event.getPlayer();
        if (player != null) {
            turtlePlayers.remove(player.getId());
        }
    }

    @EventListener
    public void onRender2D(Render2DEvent event) {
        DrawContext context = event.drawContext;
        float tickDelta = event.tickDelta;
        for (PlayerEntity ent : mc.world.getPlayers()) {
            if (ent == mc.player
                    && mc.options.getPerspective().isFirstPerson()
                    && Freecam.INSTANCE.isOff()) continue;
            double x = ent.prevX + (ent.getX() - ent.prevX) * tickDelta;
            double y = ent.prevY + (ent.getY() - ent.prevY) * tickDelta;
            double z = ent.prevZ + (ent.getZ() - ent.prevZ) * tickDelta;
            Vec3d vector =
                    new Vec3d(x, y + height.getValue() + ent.getBoundingBox().getLengthY() + 0.3, z);
            Vec3d preVec = vector;
            // 使用当前项目的3D到2D转换方法
            vector = TextUtil.worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
            if (vector.z > 0 && vector.z < 1) {
                Vector4d position = new Vector4d(vector.x, vector.y, vector.z, 0);
                position.x = Math.min(vector.x, position.x);
                position.y = Math.min(vector.y, position.y);
                position.z = Math.max(vector.x, position.z);

                String final_string = "";
                if (god.getValue() && ent.hasStatusEffect(StatusEffects.SLOWNESS)) {
                    final_string += "§4GOD ";
                }
                if (ping.getValue()) {
                    final_string += getEntityPing(ent) + "ms ";
                }
                if (gamemode.getValue()) {
                    final_string += translateGamemode(getEntityGamemode(ent)) + " ";
                }
                final_string += Formatting.RESET + dev.g255.arc.mod.modules.impl.exploit.FakeName.INSTANCE.processName(ent.getName().getString());
                if (health.getValue()) {
                    final_string +=
                            " " + getHealthColor(ent) + round2(ent.getAbsorptionAmount() + ent.getHealth());
                }
                if (distance.getValue()) {
                    final_string +=
                            " " + Formatting.RESET + String.format("%.1f", mc.player.distanceTo(ent)) + "m";
                }
                if (pops.getValue() && Arc.POP.getPop(ent.getName().getString()) != 0) {
                    final_string +=
                            " §bPop"
                                    + " "
                                    + Formatting.LIGHT_PURPLE
                                    + Arc.POP.getPop(ent.getName().getString());
                }

                double posX = position.x;
                double posY = position.y;
                double endPosX = position.z;

                float diff = (float) (endPosX - posX) / 2;
                float textWidth;

                if (font.getValue() == Font.Fancy) {
                    textWidth = (FontManager.ui.getWidth(final_string) * 1);
                } else {
                    textWidth = mc.textRenderer.getWidth(final_string);
                }

                float tagX = (float) ((posX + diff - textWidth / 2) * 1);

                ArrayList<ItemStack> stacks = new ArrayList<>();

                stacks.add(ent.getMainHandStack());
                stacks.add(ent.getInventory().armor.get(3));
                stacks.add(ent.getInventory().armor.get(2));
                stacks.add(ent.getInventory().armor.get(1));
                stacks.add(ent.getInventory().armor.get(0));
                stacks.add(ent.getOffHandStack());

                context.getMatrices().push();
                context
                        .getMatrices()
                        .translate(tagX - 2 + (textWidth + 4) / 2f, (float) (posY - 13f) + 6.5f, 0);
                float size =
                        (float)
                                Math.max(
                                        1
                                                - MathHelper.sqrt((float) mc.cameraEntity.squaredDistanceTo(preVec))
                                                * 0.01
                                                * scaled.getValue(),
                                        0);
                context
                        .getMatrices()
                        .scale(
                                Math.max(scale.getValueFloat() * size, minScale.getValueFloat()),
                                Math.max(scale.getValueFloat() * size, minScale.getValueFloat()),
                                1f);
                context
                        .getMatrices()
                        .translate(
                                0,
                                offset.getValueFloat()
                                        * MathHelper.sqrt((float) mc.cameraEntity.squaredDistanceTo(preVec)),
                                0);
                context
                        .getMatrices()
                        .translate(-(tagX - 2 + (textWidth + 4) / 2f), -(float) ((posY - 13f) + 6.5f), 0);

                float item_offset = 0;
                if (armorMode.getValue() != Armor.None) {
                    int count = 0;
                    for (ItemStack armorComponent : stacks) {
                        count++;
                        if (!armorComponent.isEmpty()) {
                            context.getMatrices().push();
                            context
                                    .getMatrices()
                                    .translate(tagX - 2 + (textWidth + 4) / 2f, (float) (posY - 13f) + 6.5f, 0);
                            context
                                    .getMatrices()
                                    .scale(armorScale.getValueFloat(), armorScale.getValueFloat(), 1f);
                            context
                                    .getMatrices()
                                    .translate(-(tagX - 2 + (textWidth + 4) / 2f), -(float) ((posY - 13f) + 6.5f), 0);
                            context
                                    .getMatrices()
                                    .translate(
                                            posX - 52.5 + item_offset,
                                            (float) (posY - 29f) + armorHeight.getValueFloat(),
                                            0);
                            float durability = armorComponent.getMaxDamage() - armorComponent.getDamage();
                            int percent = (int) ((durability / (float) armorComponent.getMaxDamage()) * 100F);
                            Color color;
                            if (percent <= 33) {
                                color = Color.RED;
                            } else if (percent <= 66) {
                                color = Color.ORANGE;
                            } else {
                                color = Color.GREEN;
                            }
                            switch (armorMode.getValue()) {
                                case OnlyArmor -> {
                                    if (count > 1 && count < 6) {
                                        DiffuseLighting.disableGuiDepthLighting();
                                        context.drawItem(armorComponent, 0, 0);
                                        context.drawItemInSlot(mc.textRenderer, armorComponent, 0, 0);
                                    }
                                }
                                case Item -> {
                                    DiffuseLighting.disableGuiDepthLighting();
                                    context.drawItem(armorComponent, 0, 0);
                                    context.drawItemInSlot(mc.textRenderer, armorComponent, 0, 0);
                                }
                                case Full -> {
                                    DiffuseLighting.disableGuiDepthLighting();
                                    context.drawItem(armorComponent, 0, 0);
                                    context.drawItemInSlot(mc.textRenderer, armorComponent, 0, 0);
                                    if (armorComponent.getMaxDamage() > 0) {
                                        if (font.getValue() == Font.Fancy) {
                                            FontManager.ui.drawString(
                                                    context.getMatrices(),
                                                    String.valueOf(percent),
                                                    9 - FontManager.ui.getWidth(String.valueOf(percent)) / 2,
                                                    -FontManager.ui.getFontHeight() + 3,
                                                    color.getRGB());
                                        } else {
                                            context.drawText(
                                                    mc.textRenderer,
                                                    String.valueOf(percent),
                                                    9 - mc.textRenderer.getWidth(String.valueOf(percent)) / 2,
                                                    -mc.textRenderer.fontHeight + 1,
                                                    color.getRGB(),
                                                    true);
                                        }
                                    }
                                }
                                case Durability -> {
                                    context.drawItemInSlot(mc.textRenderer, armorComponent, 0, 0);
                                    if (armorComponent.getMaxDamage() > 0) {
                                        if (!armorComponent.isItemBarVisible()) {
                                            int i = armorComponent.getItemBarStep();
                                            int j = armorComponent.getItemBarColor();
                                            int k = 2;
                                            int l = 13;
                                            context.fill(RenderLayer.getGuiOverlay(), k, l, k + 13, l + 2, -16777216);
                                            context.fill(RenderLayer.getGuiOverlay(), k, l, k + i, l + 1, j | -16777216);
                                        }
                                        if (font.getValue() == Font.Fancy) {
                                            FontManager.ui.drawString(
                                                    context.getMatrices(),
                                                    String.valueOf(percent),
                                                    9 - FontManager.ui.getWidth(String.valueOf(percent)) / 2,
                                                    7,
                                                    color.getRGB());
                                        } else {
                                            context.drawText(
                                                    mc.textRenderer,
                                                    String.valueOf(percent),
                                                    9 - mc.textRenderer.getWidth(String.valueOf(percent)) / 2,
                                                    5,
                                                    color.getRGB(),
                                                    true);
                                        }
                                    }
                                }
                            }
                            context.getMatrices().pop();

                            if (this.enchants.getValue()) {
                                float enchantmentY = 0;
                                if (armorComponent.hasEnchantments()) {
                                    ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(armorComponent);
                                    for (RegistryEntry<Enchantment> enchantment : enchants.getEnchantments()) {
                                        int lvl = enchants.getLevel(enchantment);
                                        StringBuilder encName = new StringBuilder();
                                        String translatedName = enchantment.value().toString().replace("Enchantment ", "");
                                        if (translatedName.contains("BlastProtection")) {
                                            encName.append("B").append(lvl);
                                        } else if (translatedName.contains("Protection")) {
                                            encName.append("P").append(lvl);
                                        } else if (translatedName.contains("Thorns")) {
                                            encName.append("T").append(lvl);
                                        } else if (translatedName.contains("Sharpness")) {
                                            encName.append("S").append(lvl);
                                        } else if (translatedName.contains("Efficiency")) {
                                            encName.append("E").append(lvl);
                                        } else if (translatedName.contains("Unbreaking")) {
                                            encName.append("U").append(lvl);
                                        } else if (translatedName.contains("Power")) {
                                            encName.append("PO").append(lvl);
                                        } else {
                                            continue;
                                        }

                                        if (font.getValue() == Font.Fancy) {
                                            FontManager.ui.drawString(
                                                    context.getMatrices(),
                                                    encName.toString(),
                                                    posX - 50 + item_offset,
                                                    (float) posY - 45 + enchantmentY,
                                                    -1);
                                        } else {
                                            context.getMatrices().push();
                                            context
                                                    .getMatrices()
                                                    .translate((posX - 50f + item_offset), (posY - 45f + enchantmentY), 0);
                                            context.drawText(mc.textRenderer, encName.toString(), 0, 0, -1, true);
                                            context.getMatrices().pop();
                                        }
                                        enchantmentY -= 8;
                                    }
                                }
                            }
                        }
                        item_offset += 18f;
                    }
                }

                if (this.turtle.getValue() && turtlePlayers.containsKey(ent.getId())) {
                    long expiry = turtlePlayers.get(ent.getId());
                    if (System.currentTimeMillis() < expiry) {
                        Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(StatusEffects.RESISTANCE);
                        long remaining = expiry - System.currentTimeMillis();
                        int seconds = (int) (remaining / 1000);
                        String time = String.format("%d:%02d", seconds / 60, seconds % 60);
                        float iconSize = 10;
                        float iconX = tagX - iconSize - 2;
                        float iconY = (float) posY - 11;

                        context.drawSprite((int) iconX, (int) iconY, 0, (int) iconSize, (int) iconSize, sprite);

                        if (font.getValue() == Font.Fancy) {
                            float timeWidth = FontManager.ui.getWidth(time);
                            FontManager.ui.drawString(
                                    context.getMatrices(),
                                    time,
                                    iconX + (iconSize - timeWidth) / 2,
                                    iconY - 6,
                                    -1);
                        } else {
                            int timeWidth = mc.textRenderer.getWidth(time);
                            context.drawText(
                                    mc.textRenderer,
                                    time,
                                    (int) (iconX + (iconSize - timeWidth) / 2),
                                    (int) (iconY - 8),
                                    -1,
                                    true);
                        }
                    } else {
                        turtlePlayers.remove(ent.getId());
                    }
                }

                if (rect.booleanValue) {
                    Render2DUtil.drawRect(
                            context.getMatrices(),
                            tagX - 2,
                            (float) (posY - 14f),
                            textWidth + 4,
                            13,
                            rect.getValue());
                }
                if (outline.booleanValue) {
                    Render2DUtil.drawRect(
                            context.getMatrices(),
                            tagX - 3,
                            (float) (posY - 14f),
                            textWidth + 6,
                            1,
                            outline.getValue());
                    Render2DUtil.drawRect(
                            context.getMatrices(),
                            tagX - 3,
                            (float) (posY - 2f),
                            textWidth + 6,
                            1,
                            outline.getValue());
                    Render2DUtil.drawRect(
                            context.getMatrices(), tagX - 3, (float) (posY - 14f), 1, 12, outline.getValue());
                    Render2DUtil.drawRect(
                            context.getMatrices(),
                            tagX + textWidth + 2,
                            (float) (posY - 14f),
                            1,
                            12,
                            outline.getValue());
                }
                if (font.getValue() == Font.Fancy) {
                    FontManager.ui.drawString(
                            context.getMatrices(),
                            final_string,
                            tagX,
                            (float) posY - 10,
                            Arc.FRIEND.isFriend(ent)
                                    ? friendColor.getValue().getRGB()
                                    : this.color.getValue().getRGB());
                } else {
                    context.getMatrices().push();
                    context.getMatrices().translate(tagX, ((float) posY - 11), 0);
                    context.drawText(
                            mc.textRenderer,
                            final_string,
                            0,
                            0,
                            Arc.FRIEND.isFriend(ent)
                                    ? friendColor.getValue().getRGB()
                                    : this.color.getValue().getRGB(),
                            true);
                    context.getMatrices().pop();
                }
                context.getMatrices().pop();
            }
        }
    }

    private String translateGamemode(GameMode gamemode) {
        if (gamemode == null) return "§7[BOT]";
        return switch (gamemode) {
            case SURVIVAL -> "§b[S]";
            case CREATIVE -> "§c[C]";
            case SPECTATOR -> "§7[SP]";
            case ADVENTURE -> "§e[A]";
        };
    }

    private Formatting getHealthColor(@NotNull PlayerEntity entity) {
        int health = (int) ((int) entity.getHealth() + entity.getAbsorptionAmount());
        if (health >= 18) {
            return Formatting.GREEN;
        }
        if (health >= 12) {
            return Formatting.YELLOW;
        }
        if (health >= 6) {
            return Formatting.RED;
        }
        return Formatting.DARK_RED;
    }

    public enum Font {
        Fancy,
        Fast
    }

    public enum Armor {
        None,
        Full,
        Durability,
        Item,
        OnlyArmor
    }
}