package dev.baimaodaxia.mod.modules.impl.render;

import com.google.common.collect.Maps;
import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.PacketEvent;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.render.ModelPlayer;
import dev.baimaodaxia.api.utils.render.Render3DUtil;
import dev.baimaodaxia.asm.accessors.IEntity;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.client.TextRadar;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.ColorSetting;
import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.GameMode;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;

public class LogoutSpots extends Module {
   private final ColorSetting box = this.add((new ColorSetting("Box", new Color(255, 255, 255, 100))).injectBoolean(true));
   private final ColorSetting fill = this.add((new ColorSetting("Fill", new Color(255, 255, 255, 100))).injectBoolean(true));
   private final ColorSetting text = this.add((new ColorSetting("Text", new Color(255, 255, 255, 255))).injectBoolean(true));
   private final ColorSetting chamsFill = this.add((new ColorSetting("ChamsFill", new Color(255, 255, 255, 100))).injectBoolean(true));
   private final ColorSetting chamsLine = this.add((new ColorSetting("ChamsLine", new Color(255, 255, 255, 100))).injectBoolean(true));
   final Map<UUID, PlayerEntity> playerCache = Maps.newConcurrentMap();
   final Map<UUID, ModelPlayer> logoutCache = Maps.newConcurrentMap();
   private final BooleanSetting health = this.add(new BooleanSetting("Health", true));
   private final BooleanSetting totem = this.add(new BooleanSetting("Totem", true));
   private final BooleanSetting message = this.add(new BooleanSetting("Message", true));

   public LogoutSpots() {
      super("LogoutSpots", Module.Category.Render);
      this.setChinese("退出记录");
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      if (!nullCheck()) {
         Object object = event.getPacket();
         if (object instanceof PlayerListS2CPacket) {
            PlayerListS2CPacket packet = (PlayerListS2CPacket)object;
            if (packet.getActions().contains(Action.ADD_PLAYER)) {
               for(PlayerListS2CPacket.Entry addedPlayer : packet.getPlayerAdditionEntries()) {
                  if (addedPlayer.gameMode() != GameMode.SPECTATOR) {
                     for(UUID uuid : this.logoutCache.keySet()) {
                        if (uuid.equals(addedPlayer.profile().getId())) {
                           PlayerEntity player = ((ModelPlayer)this.logoutCache.get(uuid)).player;
                           if (this.message.getValue()) {
                              mc.execute(() -> {
                                 String var10001 = player.getName().getString();
                                 this.sendMessage("§f" + var10001 + " §rLogged back at §f" + player.getBlockX() + ", " + player.getBlockY() + ", " + player.getBlockZ());
                              });
                           }

                           this.logoutCache.remove(uuid);
                        }
                     }
                  }
               }
            }
         } else {
            Object var12 = event.getPacket();
            if (var12 instanceof PlayerRemoveS2CPacket) {
               PlayerRemoveS2CPacket playerRemoveS2CPacket = (PlayerRemoveS2CPacket)var12;

               List<UUID> profileIds;
               try {
                  profileIds = playerRemoveS2CPacket.profileIds();
               } catch (Throwable throwable) {
                  throw new MatchException(throwable.toString(), throwable);
               }

               for(UUID uuid2 : profileIds) {
                  for(UUID uuid : this.playerCache.keySet()) {
                     if (uuid.equals(uuid2)) {
                        PlayerEntity player = (PlayerEntity)this.playerCache.get(uuid);
                        if (!this.logoutCache.containsKey(uuid) && player != null) {
                           ModelPlayer modelPlayer = new ModelPlayer(player);
                           if (this.message.getValue()) {
                              mc.execute(() -> {
                                 String var10001 = player.getName().getString();
                                 this.sendMessage("§f" + var10001 + " §rLogged out at §f" + player.getBlockX() + ", " + player.getBlockY() + ", " + player.getBlockZ());
                              });
                           }

                           this.logoutCache.put(uuid, modelPlayer);
                        }
                     }
                  }
               }
            }
         }

      }
   }

   public void onDisable() {
      this.playerCache.clear();
      this.logoutCache.clear();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.playerCache.clear();

      for(AbstractClientPlayerEntity player : Alien.THREAD.getPlayers()) {
         if (player != null && !player.equals(mc.player)) {
            this.playerCache.put(player.getGameProfile().getId(), player);
         }
      }

   }

   public void onRender3D(MatrixStack matrixStack) {
      for(ModelPlayer data : this.logoutCache.values()) {
         PlayerEntity player = data.player;
         Box box = ((IEntity)player).getDimensions().getBoxAt(player.getPos());
         if (this.box.booleanValue) {
            Render3DUtil.drawBox(matrixStack, box, this.box.getValue());
         }

         if (this.fill.booleanValue) {
            Render3DUtil.drawFill(matrixStack, box, this.fill.getValue());
         }

         if (this.chamsFill.booleanValue || this.chamsLine.booleanValue) {
            data.render(matrixStack, this.chamsFill, this.chamsLine);
         }

         if (this.text.booleanValue) {
            String var10000 = player.getName().getString();
            Render3DUtil.drawText3D(var10000 + (this.health.getValue() ? String.valueOf(TextRadar.getHealthColor(player)) + " " + round2((double)(player.getHealth() + player.getAbsorptionAmount())) : "") + (this.totem.getValue() && Alien.POP.getPop(player) > 0 ? String.valueOf(TextRadar.getPopColor(Alien.POP.getPop(player))) + " -" + Alien.POP.getPop(player) : ""), new Vec3d(player.getX(), ((IEntity)player).getDimensions().getBoxAt(player.getPos()).maxY + (double)0.5F, player.getZ()), this.text.getValue());
         }
      }

   }

   public static float round2(double value) {
      BigDecimal bd = new BigDecimal(value);
      bd = bd.setScale(1, RoundingMode.HALF_UP);
      return bd.floatValue();
   }
}
