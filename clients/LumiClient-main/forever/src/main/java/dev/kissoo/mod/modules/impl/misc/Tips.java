package dev.kissoo.mod.modules.impl.misc;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.DeathEvent;
import dev.kissoo.api.events.impl.EntitySpawnEvent;
import dev.kissoo.api.events.impl.PacketEvent;
import dev.kissoo.api.events.impl.RemoveEntityEvent;
import dev.kissoo.api.events.impl.TotemEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.core.impl.CommandManager;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.client.ClientSetting;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.systems.RenderSystem;

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
   private final SliderSetting yOffset = this.add(new SliderSetting("YOffset", 0, -200, 200, this.potion::isOpen));
   public final BooleanSetting healthWarn = this.add(new BooleanSetting("HealthWarn", true).setParent());
   public final SliderSetting minHealth = this.add(new SliderSetting("MinHealth", 6.0, 0.0, 20.0, 1.0, this.healthWarn::isOpen));
   public final SliderSetting xPos = this.add(new SliderSetting("XPos", 469, 0.0, 1000.0, 1.0, this.healthWarn::isOpen));
   public final SliderSetting yPos = this.add(new SliderSetting("YPos", 378, 0.0, 1000.0, 1.0, this.healthWarn::isOpen));
   public final BooleanSetting hwPlaySound = this.add(new BooleanSetting("HWPlaySound", false, this.healthWarn::isOpen));
   public final BooleanSetting blinkEffect = this.add(new BooleanSetting("BlinkEffect", true, this.healthWarn::isOpen));
   public final BooleanSetting showHealthText = this.add(new BooleanSetting("ShowHealthText", true, this.healthWarn::isOpen));
   public final BooleanSetting shadowText = this.add(new BooleanSetting("ShadowText", true, this.healthWarn::isOpen));
   public final SliderSetting iconSize = this.add(new SliderSetting("IconSize", 32.0, 16.0, 64.0, 1.0, this.healthWarn::isOpen));
   private static final Identifier WARNING_ICON = Identifier.of("alienclient", "textures/healthwarn/warning.png");
   private static final int ORIGINAL_ICON_WIDTH = 32;
   private static final int ORIGINAL_ICON_HEIGHT = 32;
   private static final long SOUND_COOLDOWN = 1000L;

   final DecimalFormat df = new DecimalFormat("0.0");
   final int color = new Color(190, 0, 0).getRGB();
   private final Timer lagTimer = new Timer();
   private final Timer lagBackTimer = new Timer();
   int turtles = 0;

   private long lastSoundTime = 0L;

   public Tips() {
      super("Tips", Module.Category.Misc);
      this.setChinese("提示");
      INSTANCE = this;
   }


   @EventListener
   public void onAddEntity(EntitySpawnEvent event) {
      if (this.visualRange.getValue() && event.getEntity() instanceof PlayerEntity && event.getEntity().getDisplayName() != null) {
         String playerName = event.getEntity().getDisplayName().getString();
         boolean isFriend = Forever.FRIEND.isFriend(playerName);
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
         boolean isFriend = Forever.FRIEND.isFriend(playerName);
         if ((!isFriend || this.friends.getValue()) && event.getEntity() != mc.player) {
            CommandManager.sendMessageId(
                    (isFriend ? Formatting.AQUA + playerName : Formatting.WHITE + playerName) + "§f left your visual range.",
                    event.getEntity().getId() + 777
            );
            mc.world.playSound(mc.player, mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 100.0F, 1.9F);
         }
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.potion.getValue()) {
         this.turtles = InventoryUtil.getPotionCount((StatusEffect)StatusEffects.RESISTANCE.value());
      }

      if (this.healthWarn.getValue() && !nullCheck()) {
         PlayerEntity player = mc.player;
         if (player != null) {
            float currentHealth = player.getHealth() + player.getAbsorptionAmount();
            float minHealthValue = (float) this.minHealth.getValue();

            if (currentHealth <= minHealthValue && this.hwPlaySound.getValue()) {
               long currentTime = System.currentTimeMillis();
               if (currentTime - lastSoundTime > SOUND_COOLDOWN) {
                  mc.world.playSound(
                          player,
                          player.getBlockPos(),
                          SoundEvents.ENTITY_PLAYER_HURT,
                          SoundCategory.PLAYERS,
                          1.0F,
                          1.0F
                  );
                  lastSoundTime = currentTime;
               }
            }
         }
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
         drawContext.drawText(
                 mc.textRenderer,
                 text,
                 mc.getWindow().getScaledWidth() / 2 - mc.textRenderer.getWidth(text) / 2,
                 10 + 9,
                 this.color,
                 true
         );
      }

      if (this.lagBack.getValue() && !this.lagBackTimer.passedS(1.5)) {
         String text = "Lagback (" + this.df.format((1500L - this.lagBackTimer.getMs()) / 1000.0) + "s)";
         drawContext.drawText(
                 mc.textRenderer,
                 text,
                 mc.getWindow().getScaledWidth() / 2 - mc.textRenderer.getWidth(text) / 2,
                 10 + 9 * 2,
                 this.color,
                 true
         );
      }

      if (this.potion.getValue()) {
         StringBuilder stringBuilder = new StringBuilder();
         if (this.turtles > 0) {
            stringBuilder.append("§e").append(this.turtles);
         }

         if (mc.player.hasStatusEffect(StatusEffects.RESISTANCE)
                 && (!this.resistanceLevelCheck.getValue() || mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() > 0)) {
            if (!stringBuilder.isEmpty()) {
               stringBuilder.append(" ");
            }
            stringBuilder.append("§9").append(mc.player.getStatusEffect(StatusEffects.RESISTANCE).getDuration() / 20 + 1);
         }

         if (mc.player.hasStatusEffect(StatusEffects.STRENGTH)) {
            if (!stringBuilder.isEmpty()) {
               stringBuilder.append(" ");
            }
            stringBuilder.append("§4").append(mc.player.getStatusEffect(StatusEffects.STRENGTH).getDuration() / 20 + 1);
         }

         if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            if (!stringBuilder.isEmpty()) {
               stringBuilder.append(" ");
            }
            stringBuilder.append("§b").append(mc.player.getStatusEffect(StatusEffects.SPEED).getDuration() / 20 + 1);
         }

         drawContext.drawText(
                 mc.textRenderer,
                 stringBuilder.toString(),
                 mc.getWindow().getScaledWidth() / 2 - mc.textRenderer.getWidth(stringBuilder.toString()) / 2,
                 mc.getWindow().getScaledHeight() / 2 + 9 - this.yOffset.getValueInt(),
                 -1,
                 true
         );
      }

      if (this.healthWarn.getValue() && !nullCheck() && mc.player != null) {
         float currentHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
         float minHealthValue = (float) this.minHealth.getValue();

         if (currentHealth <= minHealthValue) {
            int x = (int) this.xPos.getValue();
            int y = (int) this.yPos.getValue();
            drawWarningIcon(drawContext, x, y, currentHealth);
         }
      }
   }

   private void drawWarningIcon(DrawContext context, int x, int y, float currentHealth) {
      int size = (int) this.iconSize.getValue();

      try {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         context.drawTexture(
                 WARNING_ICON,
                 x, y,
                 size, size,
                 0.0f, 0.0f,
                 ORIGINAL_ICON_WIDTH, ORIGINAL_ICON_HEIGHT,
                 ORIGINAL_ICON_WIDTH, ORIGINAL_ICON_HEIGHT
         );

         RenderSystem.disableBlend();
         if (this.blinkEffect.getValue() && System.currentTimeMillis() % 1000 < 500) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            context.drawBorder(x, y, size, size, 0x40FF0000);
            RenderSystem.disableBlend();
         }
         if (this.showHealthText.getValue()) {
            String healthText = String.format("%.1f", currentHealth);
            TextRenderer textRenderer = mc.textRenderer;
            int textWidth = textRenderer.getWidth(healthText);
            int textX = x + size / 2 - textWidth / 2;
            int textY = y + size + 3;
            int textColor = getHealthColor(currentHealth, (float) this.minHealth.getValue());

            context.drawText(textRenderer, healthText, textX, textY, textColor, this.shadowText.getValue());
         }

      } catch (Exception e) {
         context.fill(x, y, x + size, y + size, 0x80FF0000);
         context.drawBorder(x, y, size, size, 0xFFFF0000);
         context.drawText(mc.textRenderer, "!", x + size / 2 - 3, y + size / 2 - 4, 0xFFFF0000, true);
      }
   }

   private int getHealthColor(float currentHealth, float minHealthThreshold) {
      if (minHealthThreshold <= 0) return 0xFFFF0000;
      float percentage = currentHealth / minHealthThreshold;

      if (percentage <= 0.5f) return 0xFFFF0000;
      if (percentage <= 0.75f) return 0xFFFFA500;
      if (percentage <= 1.0f) return 0xFFFFFF00;
      return 0xFF00FF00;
   }


   @EventListener
   public void onPlayerDeath(DeathEvent event) {
      PlayerEntity player = event.getPlayer();
      if (this.popCounter.getValue()) {
         if (Forever.POP.popContainer.containsKey(player.getName().getString())) {
            int l_Count = (Integer) Forever.POP.popContainer.get(player.getName().getString());
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
         if (Forever.POP.popContainer.containsKey(player.getName().getString())) {
            l_Count = (Integer) Forever.POP.popContainer.get(player.getName().getString());
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

   public void sendMessage(String message) {
      sendMessage(message, (int)(Math.random() * Integer.MAX_VALUE));
   }
}