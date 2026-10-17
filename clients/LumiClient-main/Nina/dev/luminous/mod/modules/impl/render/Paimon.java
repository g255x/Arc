package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import dev.luminous.Nina;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.DeathEvent;
import dev.luminous.api.events.impl.TotemEvent;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.class_1657;
import net.minecraft.class_286;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_4587;
import net.minecraft.class_757;
import net.minecraft.class_293.class_5596;
import org.joml.Matrix4f;

public class Paimon extends Module {
   private final SliderSetting x = this.add(new SliderSetting("X", 50, 0, 5000));
   private final SliderSetting y = this.add(new SliderSetting("Y", 10, 0, 5000));
   private final SliderSetting scale = this.add(new SliderSetting("Scale", 50, 1, 200));
   private final SliderSetting messageTimeMs = this.add(new SliderSetting("MessageTime", 2000, 0, 10000));
   private final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));
   private final BooleanSetting customFont = this.add(new BooleanSetting("Font", true));
   private final ColorSetting bubbleColor = this.add(new ColorSetting("BubbleColor", new Color(16570333)));
   private final ColorSetting textColor = this.add(new ColorSetting("TextColor", new Color(4737096)));
   private final class_2960 paimonTexture = class_2960.method_60655("alien", "textures/paimon.png");
   private final Timer lastEvent = new Timer();
   private final Timer frameRate = new Timer();
   private int currentFrame;
   private String message = "";

   public Paimon() {
      super("Paimon", Module.Category.Render);
      this.setChinese("派蒙");
   }

   public void onEnable() {
      this.currentFrame = 0;
      this.frameRate.reset();
      this.lastEvent.setMs(99999999L);
   }

   public void onRender2D(class_332 context, float tickDelta) {
      if (mc.field_1724 != null && mc.field_1687 != null) {
         float scaleFactor = (float)this.scale.getValue() / 100.0F;
         float px = (float)this.x.getValue();
         float py = (float)this.y.getValue();
         context.method_51448().method_22903();
         context.method_51448().method_46416(px + 100.0F, py + 100.0F, 0.0F);
         context.method_51448().method_22905(scaleFactor, scaleFactor, 1.0F);
         context.method_51448().method_46416(-(px + 100.0F), -(py + 100.0F), 0.0F);
         this.drawSubTexture(context.method_51448(), this.paimonTexture, px, py, 200.0F, 200.0F, 0.0F, (float)this.currentFrame * 200.0F, 200.0F, 10600.0F);
         context.method_51448().method_22909();
         long messageTime = (long)this.messageTimeMs.getValue();
         if (messageTime > 0L && !this.lastEvent.passedMs((double)messageTime)) {
            float padX = 4.0F;
            float bubbleH = 10.0F;
            float bubbleX = px + (float)this.scale.getValue() / 3.0F;
            float bubbleY = py + 70.0F - (float)this.scale.getValue();
            float textW = this.getTextWidth(this.message);
            float bubbleW = textW + padX * 2.0F;
            float t = (float)MathUtil.clamp((double)this.lastEvent.getMs(), (double)0.0F, (double)500.0F) / 500.0F;
            Render2DUtil.drawRound(context.method_51448(), bubbleX, bubbleY, t * bubbleW, bubbleH, 3.0F, this.bubbleColor.getValue());
            TextUtil.drawString(context, this.message, (double)(bubbleX + 2.0F), (double)(bubbleY + 2.0F), this.textColor.getValue().getRGB(), this.customFont.getValue(), this.shadow.getValue());
         }

         if (this.frameRate.passedMs((double)32.0F)) {
            this.frameRate.reset();
            ++this.currentFrame;
            if (this.currentFrame > 52) {
               this.currentFrame = 0;
            }
         }

      }
   }

   @EventListener
   public void onTotemPop(TotemEvent event) {
      class_1657 player = event.getPlayer();
      if (player != null && player != mc.field_1724) {
         if (Alien.FRIEND == null || !Alien.FRIEND.isFriend(player)) {
            int pops = Nina.POP == null ? 1 : Nina.POP.getPop(player);
            if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.chinese.getValue()) {
               String var4 = player.method_5477().getString();
               this.message = var4 + " pop了 " + (pops > 1 ? pops + " 个图腾!" : "图腾!");
            } else {
               String var10001 = player.method_5477().getString();
               this.message = var10001 + " popped " + (pops > 1 ? pops + " totems!" : "a totem!");
            }

            this.lastEvent.reset();
         }
      }
   }

   @EventListener
   public void onDeath(DeathEvent event) {
      class_1657 player = event.getPlayer();
      if (player != null && player != mc.field_1724) {
         if (Alien.FRIEND == null || !Alien.FRIEND.isFriend(player)) {
            int pops = Nina.POP == null ? 0 : Nina.POP.getPop(player);
            if (pops > 0) {
               if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.chinese.getValue()) {
                  String var4 = player.method_5477().getString();
                  this.message = var4 + " pop了 " + pops + " 个图腾然后死了!";
               } else {
                  String var10001 = player.method_5477().getString();
                  this.message = var10001 + " popped " + pops + " totems and died!";
               }

               this.lastEvent.reset();
            }
         }
      }
   }

   private float getTextWidth(String text) {
      return this.customFont.getValue() ? FontManager.ui.getWidth(text) : TextUtil.getWidth(text);
   }

   private void drawSubTexture(class_4587 matrices, class_2960 texture, float x, float y, float width, float height, float u, float v, float texW, float texH) {
      RenderSystem.enableBlend();
      RenderSystem.setShader(class_757::method_34542);
      RenderSystem.setShaderTexture(0, texture);
      float u0 = u / texW;
      float v0 = v / texH;
      float u1 = (u + width) / texW;
      float v1 = (v + height) / texH;
      Matrix4f matrix = matrices.method_23760().method_23761();
      class_287 buffer = class_289.method_1348().method_60827(class_5596.field_27382, class_290.field_1585);
      buffer.method_22918(matrix, x, y + height, 0.0F).method_22913(u0, v1);
      buffer.method_22918(matrix, x + width, y + height, 0.0F).method_22913(u1, v1);
      buffer.method_22918(matrix, x + width, y, 0.0F).method_22913(u1, v0);
      buffer.method_22918(matrix, x, y, 0.0F).method_22913(u0, v0);
      class_286.method_43433(buffer.method_60800());
      RenderSystem.disableBlend();
   }
}
