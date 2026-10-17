package dev.baimaodaxia.asm.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import dev.baimaodaxia.api.interfaces.IChatHudHook;
import dev.baimaodaxia.api.interfaces.IChatHudLineHook;
import dev.baimaodaxia.api.utils.math.FadeUtils;
import dev.baimaodaxia.api.utils.render.ColorUtil;
import dev.baimaodaxia.asm.accessors.IChatHud;
import dev.baimaodaxia.core.impl.FontManager;
import dev.baimaodaxia.mod.modules.impl.client.ClientSetting;
import dev.baimaodaxia.mod.modules.impl.client.Fonts;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.text.Text;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.TextColor;
import net.minecraft.text.OrderedText;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.client.gui.hud.MessageIndicator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.hud.ChatHudLine.Visible;
@Mixin({ChatHud.class})
public abstract class MixinChatHud implements IChatHudHook {
   @Unique
   private int nextMessageId = 0;
   @Unique
   private boolean nextSync;
   @Unique
   private int chatLineIndex;
   @Final
   @Shadow
   private List<ChatHudLine.Visible> visibleMessages;
   @Shadow
   @Final
   private List<ChatHudLine> messages;

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   public void onInit(MinecraftClient client, CallbackInfo ci) {
      ((IChatHud)this).setMessages(new CopyOnWriteArrayList());
      ((IChatHud)this).setVisibleMessages(new CopyOnWriteArrayList());
   }

   public void alienClient$addMessage(Text message, int id) {
      this.nextMessageId = id;
      this.nextSync = true;
      this.addMessage(message);
      this.nextSync = false;
      this.nextMessageId = 0;
   }

   public void alienClient$addMessage(Text message) {
      this.nextSync = true;
      this.addMessage(message);
      this.nextSync = false;
   }

   public void alienClient$addMessageOutSync(Text message, int id) {
      this.nextMessageId = id;
      this.addMessage(message);
      this.nextMessageId = 0;
   }

   @Inject(
      method = {"addVisibleMessage"},
      at = {@At(
   value = "INVOKE",
   target = "Ljava/util/List;add(ILjava/lang/Object;)V",
   ordinal = 0,
   shift = Shift.AFTER
)}
   )
   private void onAddMessageAfterNewChatHudLineVisible(ChatHudLine message, CallbackInfo ci) {
      IChatHudLineHook line = (IChatHudLineHook)(Object)this.visibleMessages.getFirst();
      if (line != null) {
         line.alienClient$setMessageId(this.nextMessageId);
         line.alienClient$setSync(this.nextSync);
         line.alienClient$setFade(new FadeUtils((long)ClientSetting.INSTANCE.animationTime.getValueInt()));
      }

   }

   @Inject(
      method = {"addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V"},
      at = {@At(
   value = "INVOKE",
   target = "Ljava/util/List;add(ILjava/lang/Object;)V",
   ordinal = 0,
   shift = Shift.AFTER
)}
   )
   private void onAddMessageAfterNewChatHudLine(ChatHudLine message, CallbackInfo ci) {
      IChatHudLineHook line = (IChatHudLineHook)(Object)this.messages.getFirst();
      if (line != null) {
         line.alienClient$setMessageId(this.nextMessageId);
         line.alienClient$setSync(this.nextSync);
         line.alienClient$setFade(new FadeUtils((long)ClientSetting.INSTANCE.animationTime.getValueInt()));
      }

   }

   @Inject(
      at = {@At("HEAD")},
      method = {"addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V"}
   )
   private void onAddMessage(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci) {
      if (this.nextMessageId != 0) {
         this.visibleMessages.removeIf((msg) -> ((IChatHudLineHook)(Object)msg).alienClient$getMessageId() == this.nextMessageId);
         this.messages.removeIf((msg) -> ((IChatHudLineHook)(Object)msg).alienClient$getMessageId() == this.nextMessageId);
      }

   }

   @Redirect(
      method = {"addVisibleMessage"},
      at = @At(
   value = "INVOKE",
   target = "Ljava/util/List;size()I",
   ordinal = 2,
   remap = false
),
      require = 0
   )
   public int chatLinesSize(List<ChatHudLine.Visible> list) {
      return ClientSetting.INSTANCE.isOn() && ClientSetting.INSTANCE.infiniteChat.getValue() ? -2147483647 : list.size();
   }

   @Redirect(
      method = {"render"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I"
),
      require = 0
   )
   private int drawStringWithShadow(DrawContext drawContext, TextRenderer textRenderer, OrderedText text, int x, int y, int color) {
      IChatHudLineHook line = (IChatHudLineHook)(Object)this.visibleMessages.get(this.chatLineIndex);
      if (line != null) {
         FadeUtils fadeUtils = line.alienClient$getFade();
         double ease = fadeUtils == null ? (double)0.0F : fadeUtils.ease(ClientSetting.INSTANCE.ease.getValue());
         double fade = (double)1.0F - ease;
         x += (int)(fade * ClientSetting.INSTANCE.animateOffset.getValue());
         double c = Math.max((double)10.0F, (double)(color >> 24 & 255) * ease);
         if (line.alienClient$getSync()) {
            color = ColorUtil.injectAlpha(ClientSetting.INSTANCE.color.getValue(), ClientSetting.INSTANCE.fade.getValue() ? (int)c : color >> 24 & 255).getRGB();
         } else if (ClientSetting.INSTANCE.fade.getValue()) {
            color = ColorUtil.injectAlpha(color, (int)c);
         }
      }

      if (Fonts.INSTANCE.isOn() && Fonts.INSTANCE.chat.getValue()) {
         String str = orderedTextToString(text);
         FontManager.ui.drawString(drawContext.getMatrices(), str, (double)x, (double)y, color, Fonts.INSTANCE.shadow.getValue());
         return (int)FontManager.ui.getWidth(str);
      } else {
         return drawContext.drawTextWithShadow(textRenderer, text, x, y, color);
      }
   }

   @Inject(
      method = {"render"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/hud/ChatHudLine$Visible;addedTime()I"
)}
   )
   public void getChatLineIndex(CallbackInfo ci, @Local(ordinal = 13) int chatLineIndex) {
      this.chatLineIndex = chatLineIndex;
   }

   @ModifyVariable(
      method = {"render"},
      at = @At("STORE")
   )
   private MessageIndicator removeMessageIndicator(MessageIndicator messageIndicator) {
      return ClientSetting.INSTANCE.hideIndicator.getValue() ? null : messageIndicator;
   }

   @Shadow
   public abstract void addMessage(Text var1);

   @Shadow
   public int getWidth() {
      return 0;
   }

   private static String orderedTextToString(OrderedText orderedText) {
      StringBuilder sb = new StringBuilder();
      int[] lastColor = new int[]{-1};
      boolean[] bold = new boolean[]{false};
      boolean[] italic = new boolean[]{false};
      boolean[] underline = new boolean[]{false};
      boolean[] strikethrough = new boolean[]{false};
      boolean[] obfuscated = new boolean[]{false};
      orderedText.accept((index, style, codePoint) -> {
         TextColor textColor = style.getColor();
         if (textColor != null) {
            int colorValue = textColor.getRgb();
            if (colorValue != lastColor[0]) {
               lastColor[0] = colorValue;
               sb.append('§');
               char colorChar = getColorChar(colorValue);
               sb.append(colorChar);
            }
         } else if (lastColor[0] != -1) {
            lastColor[0] = -1;
            sb.append('§').append('r');
         }

         if (style.isBold() && !bold[0]) {
            bold[0] = true;
            sb.append('§').append('l');
         } else if (!style.isBold() && bold[0]) {
            bold[0] = false;
            sb.append('§').append('r');
            if (lastColor[0] != -1) {
               sb.append('§').append(getColorChar(lastColor[0]));
            }
         }

         if (style.isItalic() && !italic[0]) {
            italic[0] = true;
            sb.append('§').append('o');
         } else if (!style.isItalic() && italic[0]) {
            italic[0] = false;
            sb.append('§').append('r');
            if (lastColor[0] != -1) {
               sb.append('§').append(getColorChar(lastColor[0]));
            }
         }

         if (style.isUnderlined() && !underline[0]) {
            underline[0] = true;
            sb.append('§').append('n');
         } else if (!style.isUnderlined() && underline[0]) {
            underline[0] = false;
            sb.append('§').append('r');
            if (lastColor[0] != -1) {
               sb.append('§').append(getColorChar(lastColor[0]));
            }
         }

         if (style.isStrikethrough() && !strikethrough[0]) {
            strikethrough[0] = true;
            sb.append('§').append('m');
         } else if (!style.isStrikethrough() && strikethrough[0]) {
            strikethrough[0] = false;
            sb.append('§').append('r');
            if (lastColor[0] != -1) {
               sb.append('§').append(getColorChar(lastColor[0]));
            }
         }

         if (style.isObfuscated() && !obfuscated[0]) {
            obfuscated[0] = true;
            sb.append('§').append('k');
         } else if (!style.isObfuscated() && obfuscated[0]) {
            obfuscated[0] = false;
            sb.append('§').append('r');
            if (lastColor[0] != -1) {
               sb.append('§').append(getColorChar(lastColor[0]));
            }
         }

         sb.appendCodePoint(codePoint);
         return true;
      });
      return sb.toString();
   }

   private static char getColorChar(int rgb) {
      int r = rgb >> 16 & 255;
      int g = rgb >> 8 & 255;
      int b = rgb & 255;
      int[][] colors = new int[][]{{0, 0, 0, 48}, {0, 0, 170, 49}, {0, 170, 0, 50}, {0, 170, 170, 51}, {170, 0, 0, 52}, {170, 0, 170, 53}, {255, 170, 0, 54}, {170, 170, 170, 55}, {85, 85, 85, 56}, {85, 85, 255, 57}, {85, 255, 85, 97}, {85, 255, 255, 98}, {255, 85, 85, 99}, {255, 85, 255, 100}, {255, 255, 85, 101}, {255, 255, 255, 102}};
      int minDist = Integer.MAX_VALUE;
      char closestChar = 'f';

      for(int[] c : colors) {
         int dist = Math.abs(r - c[0]) + Math.abs(g - c[1]) + Math.abs(b - c[2]);
         if (dist < minDist) {
            minDist = dist;
            closestChar = (char)c[3];
         }
      }

      return closestChar;
   }
}
