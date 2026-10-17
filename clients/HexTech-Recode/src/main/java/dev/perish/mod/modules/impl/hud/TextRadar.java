package dev.perish.mod.modules.impl.hud;

import dev.perish.Perish;
import dev.perish.api.utils.render.TextUtil;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.PacketEvent;
import dev.perish.api.events.impl.TotemEvent;
import dev.perish.core.impl.FontManager;
import dev.perish.mod.gui.hud.IHudPosition;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.ColorSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Formatting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.potion.Potions;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;

public class TextRadar extends Module implements IHudPosition {
    public static TextRadar INSTANCE;
    public final BooleanSetting red = this.add(new BooleanSetting("Red", false));
    private final DecimalFormat df = new DecimalFormat("0.0");
    public final BooleanSetting font = this.add(new BooleanSetting("Font", true));
    public final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));
    private final SliderSetting x = this.add(new SliderSetting("X", 0, 0, 1500));
    private final SliderSetting y = this.add(new SliderSetting("Y", 100, 0, 1000));
    public final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255)));
    public final ColorSetting friend = this.add(new ColorSetting("Friend").injectBoolean(true));
    public final BooleanSetting doubleBlank = this.add(new BooleanSetting("Double", false));
    public final BooleanSetting health = this.add(new BooleanSetting("Health", true));
    public final BooleanSetting pops = this.add(new BooleanSetting("Pops", true));
    public final BooleanSetting distance = this.add(new BooleanSetting("Distance", true));
    public final BooleanSetting effects = this.add(new BooleanSetting("Effects", true));
    public final BooleanSetting turtle = this.add(new BooleanSetting("Turtle", true));

    private final Map<Integer, Long> turtlePlayers = new HashMap<>();

    public TextRadar() {
        super("TextRadar", Category.Hud);
        this.setChinese("文字雷达");
        INSTANCE = this;
    }

    public static Formatting getHealthColor(PlayerEntity player) {
        double health = player.getHealth() + player.getAbsorptionAmount();
        if (health > 18.0) {
            return Formatting.GREEN;
        } else if (health > 16.0) {
            return Formatting.DARK_GREEN;
        } else if (health > 12.0) {
            return Formatting.YELLOW;
        } else if (health > 8.0) {
            return Formatting.GOLD;
        } else {
            return health > 4.0 ? Formatting.RED : Formatting.DARK_RED;
        }
    }

    public static Formatting getPopColor(int totems) {
        if (INSTANCE.red.getValue()) {
            return Formatting.RED;
        } else if (totems > 10) {
            return Formatting.DARK_RED;
        } else if (totems > 8) {
            return Formatting.RED;
        } else if (totems > 6) {
            return Formatting.GOLD;
        } else if (totems > 4) {
            return Formatting.YELLOW;
        } else {
            return totems > 2 ? Formatting.DARK_GREEN : Formatting.GREEN;
        }
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

    private float hudX = 5, hudY = 30, hudW, hudH;

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (!isOn() || nullCheck()) return;

        boolean useFont = this.font.getValue();
        boolean shadow = this.shadow.getValue();
        float fh = useFont ? FontManager.ui.getFontHeight() : 9;

        List<AbstractClientPlayerEntity> players = new ArrayList<>(mc.world.getPlayers());
        players.sort(Comparator.comparingDouble(p -> mc.player.distanceTo(p)));

        float cy = 0;
        float maxW = 0;
        int count = 0;

        for (PlayerEntity player : players) {
            if (player == mc.player) continue;
            boolean isFriend = Perish.FRIEND.isFriend(player);
            if (!isFriend && !this.friend.booleanValue) continue;

            StringBuilder sb = new StringBuilder();
            String blank = this.doubleBlank.getValue() ? "  " : " ";
            if (this.health.getValue()) {
                sb.append(getHealthColor(player));
                sb.append(df.format(player.getHealth() + player.getAbsorptionAmount()));
                sb.append(blank);
            }
            sb.append("§r");
            String playerName = player.getName().getString();
            sb.append(dev.perish.mod.modules.impl.exploit.FakeName.INSTANCE.processName(playerName));
            if (this.distance.getValue()) { sb.append(blank); sb.append("§f"); sb.append(df.format(mc.player.distanceTo(player))); sb.append("m"); }
            if (this.pops.getValue()) {
                int pops = Perish.POP.getPop(player);
                if (pops > 0) { sb.append(blank); sb.append(getPopColor(pops)); sb.append("-"); sb.append(pops); }
            }

            String text = sb.toString();
            int color = isFriend ? this.friend.getValue().getRGB() : this.color.getValue().getRGB();

            float tw = useFont ? FontManager.ui.getWidth(text) : mc.textRenderer.getWidth(text);
            ColorSetting colorSetting = isFriend ? this.friend : this.color;
            if (colorSetting.isGlobalPulse()) TextUtil.drawStringSetting(drawContext, text, hudX, hudY + cy, colorSetting, useFont, shadow);
            else if (useFont) FontManager.ui.drawString(drawContext.getMatrices(), text, hudX, hudY + cy, color, shadow);
            else drawContext.drawText(mc.textRenderer, text, (int)hudX, (int)(hudY + cy), color, shadow);

            maxW = Math.max(maxW, tw);
            cy += fh + 1;
            count++;
        }

        hudW = maxW + 4;
        hudH = count > 0 ? cy : 0;
    }

    public float getHudX() { return hudX; }
    public float getHudY() { return hudY; }
    public float getHudW() { return hudW; }
    public float getHudH() { return hudH; }
    public void setHudPos(float x, float y) { this.hudX = x; this.hudY = y; }
}
