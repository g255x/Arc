package dev.baimaodaxia.mod.modules.impl.misc;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.DeathEvent;
import dev.baimaodaxia.api.events.impl.EntitySpawnEvent;
import dev.baimaodaxia.api.events.impl.PacketEvent;
import dev.baimaodaxia.api.events.impl.RemoveEntityEvent;
import dev.baimaodaxia.api.events.impl.TotemEvent;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.interfaces.IChatHudHook;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.api.utils.player.InventoryUtil;
import dev.baimaodaxia.core.impl.CommandManager;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.client.ClientSetting;
import dev.baimaodaxia.mod.modules.impl.combat.Criticals;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.ColorSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.util.Formatting;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import dev.baimaodaxia.api.utils.InteractTypeUtil;

public class Tips extends Module {
   public static Tips INSTANCE;
   public final BooleanSetting visualRange = this.add((new BooleanSetting("VisualRange", false)).setParent());
   public final BooleanSetting friends;
   public final ColorSetting visualRangeColor;
   public final ColorSetting bracketColor;
   public final BooleanSetting popCounter;
   public final BooleanSetting deathCoords;
   public final BooleanSetting serverLag;
   public final BooleanSetting lagBack;
   public final BooleanSetting potion;
   public final BooleanSetting resistanceLevelCheck;
   private final SliderSetting yOffset;
   final DecimalFormat df;
   final int color;
   private final Timer lagTimer;
   private final Timer lagBackTimer;
   int turtles;
   private final Map<String, Long> attackTimestamps;

   public Tips() {
      super("Tips", Module.Category.Misc);
      BooleanSetting var10006 = this.visualRange;
      Objects.requireNonNull(var10006);
      this.friends = this.add(new BooleanSetting("Friends", false, var10006::isOpen));
      Color var10005 = new Color(85, 255, 255);
      var10006 = this.visualRange;
      Objects.requireNonNull(var10006);
      this.visualRangeColor = this.add(new ColorSetting("VisualRangeColor", var10005, var10006::isOpen));
      var10005 = new Color(255, 255, 255);
      var10006 = this.visualRange;
      Objects.requireNonNull(var10006);
      this.bracketColor = this.add(new ColorSetting("BracketColor", var10005, var10006::isOpen));
      this.popCounter = this.add(new BooleanSetting("PopCounter", true));
      this.deathCoords = this.add(new BooleanSetting("DeathCoords", true));
      this.serverLag = this.add(new BooleanSetting("ServerLag", true));
      this.lagBack = this.add(new BooleanSetting("LagBack", true));
      this.potion = this.add((new BooleanSetting("Potion", true)).setParent());
      var10006 = this.potion;
      Objects.requireNonNull(var10006);
      this.resistanceLevelCheck = this.add(new BooleanSetting("ResistanceLevelCheck", true, var10006::isOpen));
      BooleanSetting var10008 = this.potion;
      Objects.requireNonNull(var10008);
      this.yOffset = this.add(new SliderSetting("YOffset", 0, -200, 200, var10008::isOpen));
      this.df = new DecimalFormat("0.0");
      this.color = (new Color(190, 0, 0)).getRGB();
      this.lagTimer = new Timer();
      this.lagBackTimer = new Timer();
      this.turtles = 0;
      this.attackTimestamps = new HashMap();
      this.setChinese("提示");
      INSTANCE = this;
   }

   @EventListener
   public void onAddEntity(EntitySpawnEvent event) {
      if (this.visualRange.getValue() && event.getEntity() instanceof PlayerEntity && event.getEntity().getDisplayName() != null) {
         String playerName = event.getEntity().getDisplayName().getString();
         boolean isFriend = Alien.FRIEND.isFriend(playerName);
         if ((!isFriend || this.friends.getValue()) && event.getEntity() != mc.player) {
            String bracketColorCode = this.getColorCode(this.bracketColor.getValue());
            String textColorCode = this.getColorCode(this.visualRangeColor.getValue());
            String message = bracketColorCode + "[" + textColorCode + "VisualRange" + bracketColorCode + "] " + (isFriend ? String.valueOf(Formatting.AQUA) + playerName : String.valueOf(Formatting.WHITE) + playerName) + "§f entered your visual range.";
            ((IChatHudHook)mc.inGameHud.getChatHud()).alienClient$addMessage(Text.of(message), event.getEntity().getId() + 777);
            mc.world.playSound(mc.player, mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 100.0F, 1.9F);
         }
      }
   }

   @EventListener
   public void onRemoveEntity(RemoveEntityEvent event) {
      if (this.visualRange.getValue() && event.getEntity() instanceof PlayerEntity && event.getEntity().getDisplayName() != null) {
         String playerName = event.getEntity().getDisplayName().getString();
         boolean isFriend = Alien.FRIEND.isFriend(playerName);
         if ((!isFriend || this.friends.getValue()) && event.getEntity() != mc.player) {
            String bracketColorCode = this.getColorCode(this.bracketColor.getValue());
            String textColorCode = this.getColorCode(this.visualRangeColor.getValue());
            String message = bracketColorCode + "[" + textColorCode + "VisualRange" + bracketColorCode + "] " + (isFriend ? String.valueOf(Formatting.AQUA) + playerName : String.valueOf(Formatting.WHITE) + playerName) + "§f left your visual range.";
            ((IChatHudHook)mc.inGameHud.getChatHud()).alienClient$addMessage(Text.of(message), event.getEntity().getId() + 777);
            mc.world.playSound(mc.player, mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 100.0F, 1.9F);
         }
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.potion.getValue()) {
         this.turtles = InventoryUtil.getPotionCount((StatusEffect)StatusEffects.RESISTANCE.value());
      }

   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      this.lagTimer.reset();
      if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
         this.lagBackTimer.reset();
      }

   }

   @EventListener
   public void onPacketSend(PacketEvent.Send event) {
      Packet var3 = event.getPacket();
      if (var3 instanceof PlayerInteractEntityC2SPacket packet) {
         if (Criticals.getInteractType(packet) == InteractTypeUtil.ATTACK) {
            Entity entity = Criticals.getEntity(packet);
            if (entity instanceof PlayerEntity) {
               this.attackTimestamps.put(entity.getName().getString(), System.currentTimeMillis());
            }
         }
      }

   }

   public void onRender2D(DrawContext drawContext, float tickDelta) {
      if (this.serverLag.getValue() && this.lagTimer.passedS(1.4)) {
         DecimalFormat var10000 = this.df;
         double var10001 = (double)this.lagTimer.getMs();
         String text = "Server not responding (" + var10000.format(var10001 / (double)1000.0F) + "s)";
         TextRenderer textRenderer = mc.textRenderer;
         int n = mc.getWindow().getScaledWidth() / 2 - mc.textRenderer.getWidth(text) / 2;
         Objects.requireNonNull(mc.textRenderer);
         drawContext.drawText(textRenderer, text, n, 19, this.color, true);
      }

      if (this.lagBack.getValue() && !this.lagBackTimer.passedS((double)1.5F)) {
         DecimalFormat var14 = this.df;
         double var15 = (double)(1500L - this.lagBackTimer.getMs());
         String text = "Lagback (" + var14.format(var15 / (double)1000.0F) + "s)";
         TextRenderer textRenderer = mc.textRenderer;
         int n = mc.getWindow().getScaledWidth() / 2 - mc.textRenderer.getWidth(text) / 2;
         Objects.requireNonNull(mc.textRenderer);
         drawContext.drawText(textRenderer, text, n, 28, this.color, true);
      }

      if (this.potion.getValue()) {
         StringBuilder stringBuilder = new StringBuilder();
         if (this.turtles > 0) {
            stringBuilder.append("§e").append(this.turtles);
         }

         if (mc.player.hasStatusEffect(StatusEffects.RESISTANCE) && (!this.resistanceLevelCheck.getValue() || mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() > 0)) {
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

         TextRenderer textRenderer = mc.textRenderer;
         String string = stringBuilder.toString();
         int n = mc.getWindow().getScaledWidth() / 2 - mc.textRenderer.getWidth(stringBuilder.toString()) / 2;
         int n2 = mc.getWindow().getScaledHeight() / 2;
         Objects.requireNonNull(mc.textRenderer);
         drawContext.drawText(textRenderer, string, n, n2 + 9 - this.yOffset.getValueInt(), -1, true);
      }

   }

   @EventListener
   public void onPlayerDeath(DeathEvent event) {
      PlayerEntity player = event.getPlayer();
      if (this.attackTimestamps.containsKey(player.getName().getString())) {
         long time = (Long)this.attackTimestamps.get(player.getName().getString());
         if (System.currentTimeMillis() - time < 10000L) {
            this.attackTimestamps.remove(player.getName().getString());
         }
      }

      if (this.popCounter.getValue()) {
         if (Alien.POP.popContainer.containsKey(player.getName().getString())) {
            int l_Count = (Integer)Alien.POP.popContainer.get(player.getName().getString());
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
         int var10001 = player.getBlockX();
         this.sendMessage("§4You died at " + var10001 + ", " + player.getBlockY() + ", " + player.getBlockZ());
      }

   }

   @EventListener
   public void onTotem(TotemEvent event) {
      if (this.popCounter.getValue()) {
         PlayerEntity player = event.getPlayer();
         int l_Count = 1;
         if (Alien.POP.popContainer.containsKey(player.getName().getString())) {
            l_Count = (Integer)Alien.POP.popContainer.get(player.getName().getString());
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

   private String getColorCode(Color color) {
      String hex = String.format("%06X", color.getRGB() & 16777215);
      StringBuilder sb = new StringBuilder("§x");

      for(char c : hex.toCharArray()) {
         sb.append("§").append(c);
      }

      return sb.toString();
   }
}
