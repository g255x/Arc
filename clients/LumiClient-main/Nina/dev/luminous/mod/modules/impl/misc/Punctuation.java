package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_2596;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3417;
import net.minecraft.class_3419;
import net.minecraft.class_3965;
import net.minecraft.class_4587;
import net.minecraft.class_7438;
import net.minecraft.class_7439;

public class Punctuation extends Module {
   private final BooleanSetting sound = this.add(new BooleanSetting("Sound", true));
   private final SliderSetting clearTime = this.add((new SliderSetting("ClearTime", (double)10.0F, (double)0.0F, (double)100.0F, 0.1)).setSuffix("s"));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 100)));
   private final BindSetting enemySpot = this.add(new BindSetting("EnemySpot", -1));
   private final StringSetting key = this.add(new StringSetting("EncryptKey", "IDKWTFTHIS"));
   private final ConcurrentHashMap<String, Spot> waypoint = new ConcurrentHashMap();
   private boolean pressed = false;

   public Punctuation() {
      super("Punctuation", Module.Category.Misc);
      this.setChinese("标点");
   }

   public static SecretKeySpec getKey(String myKey) {
      try {
         byte[] key = myKey.getBytes(StandardCharsets.UTF_8);
         MessageDigest sha = MessageDigest.getInstance("SHA-256");
         key = sha.digest(key);
         key = Arrays.copyOf(key, 16);
         return new SecretKeySpec(key, "AES");
      } catch (Exception var3) {
         return null;
      }
   }

   public void onDisable() {
      this.waypoint.clear();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.waypoint.values().removeIf((t) -> t.timer.passedS(this.clearTime.getValue()));
      if (this.enemySpot.isPressed() && mc.field_1755 == null) {
         class_239 hitResult;
         if (!this.pressed && (hitResult = mc.method_1560().method_5745((double)256.0F, 0.0F, false)) instanceof class_3965) {
            class_3965 blockHitResult = (class_3965)hitResult;
            class_2338 pos = blockHitResult.method_17777();
            this.waypoint.put("Local", new Spot("Local", new BlockPosX((double)pos.method_10263(), (double)pos.method_10264(), (double)pos.method_10260()), this.color.getValue(), new Timer()));
            int var10000 = pos.method_10263();
            CommandManager.sendMessage("Marked locally at §r(" + var10000 + ", " + pos.method_10264() + ", " + pos.method_10260() + ")");
         }

         this.pressed = true;
      } else {
         this.pressed = false;
      }

   }

   public void onRender2D(class_332 context, float tickDelta) {
      for(Spot spot : this.waypoint.values()) {
         class_243 vector = TextUtil.worldSpaceToScreenSpace(spot.pos.method_46558().method_1031((double)0.0F, (double)1.0F, (double)0.0F));
         String var10000 = spot.name;
         String text = "§a" + var10000 + " §f(" + spot.pos.method_10263() + ", " + spot.pos.method_10264() + ", " + spot.pos.method_10260() + ")";
         if (vector.field_1350 > (double)0.0F && vector.field_1350 < (double)1.0F) {
            double posX = vector.field_1352;
            double posY = vector.field_1351;
            double endPosX = Math.max(vector.field_1352, vector.field_1350);
            float diff = (float)(endPosX - posX) / 2.0F;
            float textWidth = (float)mc.field_1772.method_1727(text);
            float tagX = (float)((posX + (double)diff - (double)(textWidth / 4.0F)) * (double)1.0F);
            context.method_51448().method_22903();
            context.method_51448().method_22905(0.5F, 0.5F, 1.0F);
            class_327 textRenderer = mc.field_1772;
            int n = (int)tagX * 2;
            Objects.requireNonNull(mc.field_1772);
            context.method_51433(textRenderer, text, n, (int)(posY - (double)11.0F + 10.799999999999999) * 2, -1, true);
            context.method_51448().method_22909();
         }
      }

   }

   public void onRender3D(class_4587 matrixStack) {
      for(Spot spot : this.waypoint.values()) {
         Render3DUtil.drawFill(matrixStack, new class_238((double)spot.pos.method_10263() + (double)0.25F, (double)-60.0F, (double)spot.pos.method_10260() + (double)0.25F, (double)spot.pos.method_10263() + (double)0.75F, (double)320.0F, (double)spot.pos.method_10260() + (double)0.75F), spot.color);
      }

   }

   @EventListener
   private void PacketReceive(PacketEvent.Receive event) {
      if (!nullCheck()) {
         class_2596<?> packet2 = event.getPacket();
         class_7439 packet;
         if (packet2 instanceof class_7439 && (packet = (class_7439)packet2).comp_763() != null) {
            mc.execute(() -> this.receive(packet.comp_763().getString()));
         }

         if ((packet2 = event.getPacket()) instanceof class_7438) {
            class_7438 chatPacket = (class_7438)packet2;
            if (chatPacket.comp_1103() != null) {
               mc.execute(() -> this.receive(chatPacket.comp_1103().getString()));
            } else {
               mc.execute(() -> this.receive(chatPacket.comp_1102().comp_1090()));
            }
         }

      }
   }

   private void receive(String s) {
      try {
         if (s == null) {
            return;
         }

         String decrypt = this.Decrypt(s.replaceAll("§[0-9a-fk-or]", "").replaceAll("<[^>]*> ", ""));
         if (decrypt == null) {
            return;
         }

         Matcher matcher;
         if (decrypt.contains("EnemyHere") && (matcher = Pattern.compile("\\{(.*?)}").matcher(decrypt)).find()) {
            String pos = matcher.group(1);
            String[] posSplit = pos.split(",");
            if (posSplit.length == 3) {
               if (this.sound.getValue()) {
                  mc.field_1687.method_8396(mc.field_1724, mc.field_1724.method_24515(), class_3417.field_14627, class_3419.field_15248, 100.0F, 1.9F);
               }

               String xString = posSplit[0];
               String yString = posSplit[1];
               String zString = posSplit[2];
               Pattern pattern = Pattern.compile("<(.*?)>");
               matcher = pattern.matcher(s);
               if (!this.isNumeric(xString)) {
                  return;
               }

               double x = Double.parseDouble(xString);
               if (!this.isNumeric(yString)) {
                  return;
               }

               double y = Double.parseDouble(yString);
               if (!this.isNumeric(zString)) {
                  return;
               }

               double z = Double.parseDouble(zString);
               if (matcher.find()) {
                  String sender = matcher.group(1);
                  this.waypoint.put(sender, new Spot(sender, new BlockPosX(x, y, z), this.color.getValue(), new Timer()));
                  CommandManager.sendMessage(sender + " marked at §r(" + xString + ", " + yString + ", " + zString + ")");
               } else {
                  this.waypoint.put("" + MathUtil.random(0.0F, 1.0E9F), new Spot("Unknown", new BlockPosX(x, y, z), this.color.getValue(), new Timer()));
                  CommandManager.sendMessage("Unknown marked at §r(" + xString + ", " + yString + ", " + zString + ")");
               }
            } else if (posSplit.length == 4) {
               if (this.sound.getValue()) {
                  mc.field_1687.method_8396(mc.field_1724, mc.field_1724.method_24515(), class_3417.field_14627, class_3419.field_15248, 100.0F, 1.9F);
               }

               String xString = posSplit[0];
               String yString = posSplit[1];
               String zString = posSplit[2];
               String colorString = posSplit[3];
               Pattern e = Pattern.compile("<(.*?)>");
               matcher = e.matcher(s);
               if (!this.isNumeric(xString)) {
                  return;
               }

               double x = Double.parseDouble(xString);
               if (!this.isNumeric(yString)) {
                  return;
               }

               double y = Double.parseDouble(yString);
               if (!this.isNumeric(zString)) {
                  return;
               }

               double z = Double.parseDouble(zString);
               if (!this.isNumeric(colorString)) {
                  return;
               }

               double color = Double.parseDouble(colorString);
               if (matcher.find()) {
                  String sender = matcher.group(1);
                  this.waypoint.put(sender, new Spot(sender, new BlockPosX(x, y, z), new Color((int)color, true), new Timer()));
                  CommandManager.sendMessage(sender + " marked at §r(" + xString + ", " + yString + ", " + zString + ")");
               } else {
                  this.waypoint.put("" + MathUtil.random(0.0F, 1.0E9F), new Spot("Unknown", new BlockPosX(x, y, z), new Color((int)color, true), new Timer()));
                  CommandManager.sendMessage("Unknown marked at §r(" + xString + ", " + yString + ", " + zString + ")");
               }
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   private boolean isNumeric(String str) {
      return str.matches("-?\\d+(\\.\\d+)?");
   }

   public String Decrypt(String strToDecrypt) {
      try {
         Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
         SecretKeySpec secretKey = getKey(this.key.getValue());
         byte[] iv = new byte[16];
         IvParameterSpec ivParams = new IvParameterSpec(iv);
         cipher.init(2, secretKey, ivParams);
         byte[] original = cipher.doFinal(Base64.getDecoder().decode(strToDecrypt));
         return new String(original, StandardCharsets.UTF_8);
      } catch (Exception var7) {
         return null;
      }
   }

   public String Encrypt(String strToEncrypt) {
      try {
         Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
         SecretKeySpec secretKey = getKey(this.key.getValue());
         byte[] iv = new byte[16];
         IvParameterSpec ivParams = new IvParameterSpec(iv);
         cipher.init(1, secretKey, ivParams);
         return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
      } catch (Exception var6) {
         return null;
      }
   }

   private static record Spot(String name, class_2338 pos, Color color, Timer timer) {
   }
}
