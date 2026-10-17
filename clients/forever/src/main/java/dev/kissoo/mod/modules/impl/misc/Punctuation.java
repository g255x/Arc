package dev.kissoo.mod.modules.impl.misc;

import dev.kissoo.Forever;
import dev.kissoo.chat.ChatClient;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.PacketEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.math.MathUtil;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.api.utils.render.Render3DUtil;
import dev.kissoo.api.utils.render.TextUtil;
import dev.kissoo.api.utils.world.BlockPosX;
import dev.kissoo.core.impl.CommandManager;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BindSetting;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import dev.kissoo.mod.modules.settings.impl.StringSetting;
import dev.kissoo.mod.modules.impl.misc.CryptUtil;

import java.awt.Color;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class Punctuation extends Module {
   public static Punctuation INSTANCE;


   // 定义发送模式枚举
   public enum SendMode {
      Public, Single, Friend
   }

   // --- 设置项 ---
   private final BooleanSetting sound = this.add(new BooleanSetting("Sound", true));
   private final SliderSetting clearTime = this.add(new SliderSetting("ClearTime", 10.0, 0.0, 100.0, 0.1).setSuffix("s"));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 100)));
   private final BindSetting enemySpot = this.add(new BindSetting("EnemySpot", -1));
   private final StringSetting key = this.add(new StringSetting("EncryptKey", "IDKWTFTHIS"));

   // 新增模式设置项
   private final EnumSetting<SendMode> sendMode = this.add(new EnumSetting<>("SendMode", SendMode.Public));
   private final StringSetting singleName = this.add(new StringSetting("SingleName", "TargetPlayer", () -> this.sendMode.getValue() == SendMode.Single));
   private final SliderSetting friendDelay = this.add(new SliderSetting("FriendDelay", 1.0, 0.1, 10.0, 0.1, () -> this.sendMode.getValue() == SendMode.Friend).setSuffix("s"));
   private final BooleanSetting sendIRC = this.add(new BooleanSetting("SendIRC", false));

   public final ConcurrentHashMap<String, Punctuation.Spot> waypoint = new ConcurrentHashMap<>();

   // 用于 Friend 模式的消息队列
   private final Queue<String> friendQueue = new ConcurrentLinkedQueue<>();
   private String currentPayload = "";
   private final Timer sendTimer = new Timer();

   private boolean pressed = false;

   public Punctuation() {
      super("Punctuation", Module.Category.Misc);
      this.setChinese("标点");
      INSTANCE = this;
   }


   @Override
   public void onDisable() {
      this.waypoint.clear();
      this.friendQueue.clear();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.waypoint.values().removeIf(t -> t.timer.passedS(this.clearTime.getValue()));

      // 1. 处理按键触发与加密信息生成
      if (this.enemySpot.isPressed() && mc.currentScreen == null) {
         if (!this.pressed) {
            net.minecraft.entity.Entity cam = mc.getCameraEntity();
            if (cam != null && cam.raycast(256.0, 0.0F, false) instanceof BlockHitResult blockHitResult) {
               BlockPos pos = blockHitResult.getBlockPos();
               String payload = this.Encrypt("EnemyHere{" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "," + this.color.getValue().getRGB() + "}");

               if (payload == null) {
                  CommandManager.sendMessage("§cPunctuation: 加密失败，请检查设置");
               } else {
                  // 立即在本地显示标记，不等待服务器 echo
                  String self = mc.player.getName().getString();
                  this.waypoint.put(self, new Punctuation.Spot(self, pos, this.color.getValue(), new Timer()));
                  if (this.sound.getValue()) {
                     mc.world.playSound(mc.player, mc.player.getBlockPos(), net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, net.minecraft.sound.SoundCategory.PLAYERS, 100.0F, 1.9F);
                  }
                  CommandManager.sendMessage(self + " marked at §r(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")");

                  switch (this.sendMode.getValue()) {
                     case Public -> mc.player.networkHandler.sendChatMessage(payload);
                     case Single -> {
                        String target = this.singleName.getValue();
                        if (target != null && !target.isEmpty()) {
                           mc.player.networkHandler.sendChatCommand("tell " + target.trim() + " " + payload);
                        }
                     }
                     case Friend -> {
                        if (mc.getNetworkHandler() != null) {
                           this.friendQueue.clear();
                           this.currentPayload = payload;
                           for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                              String playerName = entry.getProfile().getName();
                              if (!playerName.equals(self) && isFriend(playerName)) {
                                 this.friendQueue.add(playerName);
                              }
                           }
                           this.sendTimer.reset();
                        }
                     }
                  }
                  if (this.sendIRC.getValue()) {
                     ChatClient.send(payload);
                  }
               }
            }
         }
         this.pressed = true;
      } else {
         this.pressed = false;
      }

      // 2. 处理 Friend 模式的延迟发送队列
      if (!this.friendQueue.isEmpty() && this.sendMode.getValue() == SendMode.Friend) {
         if (this.sendTimer.passedS(this.friendDelay.getValue())) {
            String targetFriend = this.friendQueue.poll(); // 取出并移除队列头部好友
            if (targetFriend != null) {
               mc.player.networkHandler.sendChatCommand("tell " + targetFriend + " " + this.currentPayload);
               CommandManager.sendMessage("Mark sent to friend: " + targetFriend);
            }
            this.sendTimer.reset(); // 重置计时器，等待发给下一个好友
         }
      }
   }

   // 封装一个通过字符串检查好友的方法（防崩处理）
   private boolean isFriend(String name) {
      try {
         return Forever.FRIEND.isFriend(name);
      } catch (Exception e) {
         return false; // 如果报错或找不到好友系统，默认不是好友
      }
   }

   @Override
   public void onRender2D(DrawContext context, float tickDelta) {
      for (Punctuation.Spot spot : this.waypoint.values()) {
         Vec3d vector = TextUtil.worldSpaceToScreenSpace(spot.pos().toCenterPos().add(0.0, 1.0, 0.0));
         String text = "§a" + spot.name() + " §f(" + spot.pos().getX() + ", " + spot.pos().getY() + ", " + spot.pos().getZ() + ")";
         if (vector.z > 0.0 && vector.z < 1.0) {
            double posX = vector.x;
            double posY = vector.y;
            double endPosX = Math.max(vector.x, vector.z);
            float diff = (float) (endPosX - posX) / 2.0F;
            float textWidth = mc.textRenderer.getWidth(text);
            float tagX = (float) ((posX + diff - textWidth / 4.0F) * 1.0);
            context.getMatrices().push();
            context.getMatrices().scale(0.5F, 0.5F, 1.0F);
            context.drawText(mc.textRenderer, text, (int) tagX * 2, (int) (posY - 11.0 + 9.0 * 1.2) * 2, -1, true);
            context.getMatrices().pop();
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      for (Punctuation.Spot spot : this.waypoint.values()) {
         Render3DUtil.drawFill(
                 matrixStack,
                 new Box(spot.pos().getX() + 0.25, -60.0, spot.pos().getZ() + 0.25, spot.pos().getX() + 0.75, 320.0, spot.pos().getZ() + 0.75),
                 spot.color()
         );
      }
   }

   @EventListener
   private void PacketReceive(PacketEvent.Receive event) {
      if (!nullCheck()) {
         if (event.getPacket() instanceof GameMessageS2CPacket packet && packet.content() != null) {
            mc.execute(() -> this.receive(packet.content().getString()));
         }
         if (event.getPacket() instanceof ChatMessageS2CPacket packet) {
            if (packet.unsignedContent() != null) {
               mc.execute(() -> this.receive(packet.unsignedContent().getString()));
            } else {
               mc.execute(() -> this.receive(packet.body().content()));
            }
         }
      }
   }

   /**
    * @return true if s contained a valid encrypted Punctuation mark (caller should suppress raw display)
    */
   public boolean receive(String s) {
      try {
         if (s == null) return false;
         String cleanStr = s.replaceAll("§[a-zA-Z0-9]", "");

         String[] words = cleanStr.split(" ");
         String decrypt = null;
         for (String word : words) {
            word = word.replaceAll("[^a-zA-Z0-9+/=]", "");
            if (word.length() < 10) continue;

            String tempDecrypt = this.Decrypt(word);
            if (tempDecrypt != null && tempDecrypt.contains("EnemyHere")) {
               decrypt = tempDecrypt;
               break;
            }
         }

         if (decrypt == null) return false;

         Pattern pattern = Pattern.compile("\\{(.*?)}");
         Matcher matcher = pattern.matcher(decrypt);
         if (matcher.find()) {
            String pos = matcher.group(1);
            String[] posSplit = pos.split(",");
            if (posSplit.length == 3 || posSplit.length == 4) {
               if (this.sound.getValue()) {
                  mc.world.playSound(mc.player, mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 100.0F, 1.9F);
               }

               double x = Double.parseDouble(posSplit[0]);
               double y = Double.parseDouble(posSplit[1]);
               double z = Double.parseDouble(posSplit[2]);
               Color c = posSplit.length == 4 ? new Color((int) Double.parseDouble(posSplit[3]), true) : this.color.getValue();

               pattern = Pattern.compile("<(.*?)>");
               matcher = pattern.matcher(s);
               String sender = matcher.find() ? matcher.group(1) : cleanStr.split(" ")[0].replaceAll("[^a-zA-Z0-9_]", "");
               String keyName = sender.isEmpty() ? MathUtil.random(0.0F, 1.0E9F) + "" : sender;

               this.waypoint.put(keyName, new Punctuation.Spot(sender, new BlockPosX(x, y, z), c, new Timer()));
               CommandManager.sendMessage(sender + " marked at §r(" + posSplit[0] + ", " + posSplit[1] + ", " + posSplit[2] + ")");
               return true;
            }
         }
      } catch (Exception var20) {
         var20.printStackTrace();
      }
      return false;
   }

   public String Encrypt(String strToEncrypt) {
      return CryptUtil.encrypt(strToEncrypt, this.key.getValue());
   }

   public String Decrypt(String strToDecrypt) {
      return CryptUtil.decrypt(strToDecrypt, this.key.getValue());
   }

   public record Spot(String name, BlockPos pos, Color color, Timer timer) {
   }
}