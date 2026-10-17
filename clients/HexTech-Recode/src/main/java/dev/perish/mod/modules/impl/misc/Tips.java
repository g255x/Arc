package dev.perish.mod.modules.impl.misc;

import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.*;
import dev.perish.api.utils.math.Timer;
import dev.perish.api.utils.player.InventoryUtil;
import dev.perish.core.impl.CommandManager;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.client.ClientSetting;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.ColorSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import java.awt.*;
import java.text.DecimalFormat;
import net.minecraft.util.Formatting;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;

public class Tips extends Module {
    public static Tips INSTANCE;
    public final BooleanSetting visualRange = this.add(new BooleanSetting("VisualRange", false).setParent());
    public final BooleanSetting friends = this.add(new BooleanSetting("Friends", false, this.visualRange::isOpen));
    public final BooleanSetting popCounter = this.add(new BooleanSetting("PopCounter", true));
    public final BooleanSetting deathCoords = this.add(new BooleanSetting("DeathCoords", true));
    public final BooleanSetting serverLag = this.add(new BooleanSetting("ServerLag", true));
    public final BooleanSetting lagBack = this.add(new BooleanSetting("LagBack", true));
    public final BooleanSetting potion = this.add(new BooleanSetting("Potion", true).setParent());
    public final BooleanSetting resistanceLevelCheck = this.add(new BooleanSetting("ResistanceLevelCheck", true, this.potion::isOpen));
    public final BooleanSetting totem = this.add(new BooleanSetting("Totem", true));
    private final ColorSetting labelColor = this.add(new ColorSetting("LabelColor", new Color(220, 220, 220), () -> this.potion.getValue() || this.totem.getValue()));
    private final ColorSetting totemColor = this.add(new ColorSetting("TotemColor", new Color(255, 255, 255), this.totem::getValue));
    private final ColorSetting potionColor = this.add(new ColorSetting("PotionColor", new Color(255, 255, 255), this.potion::getValue));
    private final ColorSetting potionTimeColor = this.add(new ColorSetting("PotionTimeColor", new Color(255, 255, 255), this.potion::getValue));
    final DecimalFormat df = new DecimalFormat("0.0");
    final int color = new Color(190, 0, 0).getRGB();
    private final SliderSetting yOffset = this.add(new SliderSetting("YOffset", 0, -200, 200, () -> this.potion.isOpen() || this.totem.isOpen()));
    private final Timer lagTimer = new Timer();
    private final Timer lagBackTimer = new Timer();
    int turtles = 0;
    int totems = 0;

    public Tips() {
        super("Tips", Module.Category.Misc);
        this.setChinese("提示");
        INSTANCE = this;
    }

    @EventListener
    public void onAddEntity(EntitySpawnEvent event) {
        if (this.visualRange.getValue() && event.getEntity() instanceof PlayerEntity && event.getEntity().getDisplayName() != null) {
            String playerName = event.getEntity().getDisplayName().getString();
            boolean isFriend = Perish.FRIEND.isFriend(playerName);
            if ((!isFriend || this.friends.getValue()) && event.getEntity() != mc.player) {
                CommandManager.sendMessageId(
                        (isFriend ? Formatting.AQUA + playerName : Formatting.WHITE + playerName) + "§f entered your visual range.",
                        event.getEntity().getId() + 777
                );
                mc.world.playSound(mc.player, mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 100.0F, 1.9F);
            }
        }
    }

    @EventListener
    public void onRemoveEntity(RemoveEntityEvent event) {
        if (this.visualRange.getValue() && event.getEntity() instanceof PlayerEntity && event.getEntity().getDisplayName() != null) {
            String playerName = event.getEntity().getDisplayName().getString();
            boolean isFriend = Perish.FRIEND.isFriend(playerName);
            if ((!isFriend || this.friends.getValue()) && event.getEntity() != mc.player) {
                CommandManager.sendMessageId(
                        (isFriend ? Formatting.AQUA + playerName : Formatting.WHITE + playerName) + "§f left your visual range.", event.getEntity().getId() + 777
                );
                mc.world.playSound(mc.player, mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 100.0F, 1.9F);
            }
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (this.potion.getValue()) {
            this.turtles = InventoryUtil.getPotionCount(StatusEffects.RESISTANCE.value());
        }
        if (this.totem.getValue()) {
            this.totems = InventoryUtil.getItemCount(Items.TOTEM_OF_UNDYING);
        }
    }

    @EventListener
    public void onPacketEvent(PacketEvent.Receive event) {
        this.lagTimer.reset();
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.lagBackTimer.reset();
        }
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (this.serverLag.getValue() && this.lagTimer.passedS(1.4)) {
            String text = "Server not responding (" + this.df.format(this.lagTimer.getMs() / 1000.0) + "s)";
            drawContext.drawText(mc.textRenderer, text, mc.getWindow().getScaledWidth() / 2 - mc.textRenderer.getWidth(text) / 2, 10 + 9, this.color, true);
        }

        if (this.lagBack.getValue() && !this.lagBackTimer.passedS(1.5)) {
            String text = "Lagback (" + this.df.format((1500L - this.lagBackTimer.getMs()) / 1000.0) + "s)";
            drawContext.drawText(mc.textRenderer, text, mc.getWindow().getScaledWidth() / 2 - mc.textRenderer.getWidth(text) / 2, 10 + 9 * 2, this.color, true);
        }

        if (this.potion.getValue() || this.totem.getValue()) {
            int lineY = mc.getWindow().getScaledHeight() / 2 + 9 - this.yOffset.getValueInt();
            int lineHeight = 10;
            int currentLine = 0;

            if (this.totem.getValue()) {
                this.drawCenteredPair(
                        drawContext,
                        "Totem ",
                        String.valueOf(this.totems),
                        lineY + currentLine * lineHeight,
                        this.labelColor.getValue().getRGB(),
                        this.totemColor.getValue().getRGB()
                );
                currentLine++;
            }

            if (this.potion.getValue()) {
                this.drawCenteredPair(
                        drawContext,
                        "Potion ",
                        String.valueOf(this.turtles),
                        lineY + currentLine * lineHeight,
                        this.labelColor.getValue().getRGB(),
                        this.potionColor.getValue().getRGB()
                );
                currentLine++;
            }

            int potionDuration = this.getPotionDurationTicks();
            if (this.potion.getValue() && potionDuration > 0) {
                this.drawCenteredPair(
                        drawContext,
                        "PotionTime ",
                        this.formatPotionTime(potionDuration),
                        lineY + currentLine * lineHeight,
                        this.labelColor.getValue().getRGB(),
                        this.potionTimeColor.getValue().getRGB()
                );
            }
        }
    }

    private void drawCenteredPair(DrawContext drawContext, String label, String value, int y, int labelColor, int valueColor) {
        int totalWidth = mc.textRenderer.getWidth(label) + mc.textRenderer.getWidth(value);
        int startX = mc.getWindow().getScaledWidth() / 2 - totalWidth / 2;
        drawContext.drawText(mc.textRenderer, label, startX, y, labelColor, true);
        drawContext.drawText(mc.textRenderer, value, startX + mc.textRenderer.getWidth(label), y, valueColor, true);
    }

    private int getPotionDurationTicks() {
        int duration = 0;
        if (mc.player.hasStatusEffect(StatusEffects.RESISTANCE)
                && (!this.resistanceLevelCheck.getValue() || mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() > 0)) {
            duration = Math.max(duration, mc.player.getStatusEffect(StatusEffects.RESISTANCE).getDuration());
        }
        if (mc.player.hasStatusEffect(StatusEffects.STRENGTH)) {
            duration = Math.max(duration, mc.player.getStatusEffect(StatusEffects.STRENGTH).getDuration());
        }
        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            duration = Math.max(duration, mc.player.getStatusEffect(StatusEffects.SPEED).getDuration());
        }
        return duration;
    }

    private String formatPotionTime(int ticks) {
        int totalSeconds = Math.max(0, (ticks + 19) / 20);
        return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    @EventListener
    public void onPlayerDeath(DeathEvent event) {
        PlayerEntity player = event.getPlayer();
        if (this.popCounter.getValue()) {
            if (Perish.POP.popContainer.containsKey(player.getName().getString())) {
                int l_Count = Perish.POP.popContainer.get(player.getName().getString());
                if (l_Count == 1) {
                    if (player.equals(mc.player)) {
                        this.sendMessage("§fYou§r died after popping §f" + l_Count + "§r totem.", player.getId());
                    } else {
                        this.sendMessage("§f" + player.getName().getString() + "§r died after popping §f" + l_Count + "§r totem.", player.getId());
                    }
                } else if (player.equals(mc.player)) {
                    this.sendMessage("§fYou§r died after popping §f" + l_Count + "§r totems.", player.getId());
                } else {
                    this.sendMessage("§f" + player.getName().getString() + "§r died after popping §f" + l_Count + "§r totems.", player.getId());
                }
            } else if (player.equals(mc.player)) {
                this.sendMessage("§fYou§r died.", player.getId());
            } else {
                this.sendMessage("§f" + player.getName().getString() + "§r died.", player.getId());
            }
        }

        if (this.deathCoords.getValue() && player == mc.player) {
            this.sendMessage("§4You died at " + player.getBlockX() + ", " + player.getBlockY() + ", " + player.getBlockZ());
        }
    }

    @EventListener
    public void onTotem(TotemEvent event) {
        if (this.popCounter.getValue()) {
            PlayerEntity player = event.getPlayer();
            int l_Count = 1;
            if (Perish.POP.popContainer.containsKey(player.getName().getString())) {
                l_Count = Perish.POP.popContainer.get(player.getName().getString());
            }

            if (l_Count == 1) {
                if (player.equals(mc.player)) {
                    this.sendMessage("§fYou§r popped §f" + l_Count + "§r totem.", player.getId());
                } else {
                    this.sendMessage("§f" + player.getName().getString() + " §rpopped §f" + l_Count + "§r totems.", player.getId());
                }
            } else if (player.equals(mc.player)) {
                this.sendMessage("§fYou§r popped §f" + l_Count + "§r totem.", player.getId());
            } else {
                this.sendMessage("§f" + player.getName().getString() + " §rhas popped §f" + l_Count + "§r totems.", player.getId());
            }
        }
    }

    public void sendMessage(String message, int id) {
        if (!nullCheck()) {
            if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Moon) {
                CommandManager.sendMessageId("§f[§3" + this.getName() + "§f] " + message, id);
                return;
            }

            CommandManager.sendMessageId(message, id);
        }
    }
}
