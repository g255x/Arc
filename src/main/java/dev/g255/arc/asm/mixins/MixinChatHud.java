package dev.g255.arc.asm.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import dev.g255.arc.api.interfaces.IChatHudHook;
import dev.g255.arc.api.interfaces.IChatHudLineHook;
import dev.g255.arc.api.utils.math.FadeUtils;
import dev.g255.arc.api.utils.render.ColorUtil;
import dev.g255.arc.asm.accessors.IChatHud;
import dev.g255.arc.mod.modules.impl.client.ClientSetting;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.text.Text;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
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

   public void arc$addMessage(Text message, int id) {
      this.nextMessageId = id;
      this.nextSync = true;
      this.addMessage(message);
      this.nextSync = false;
      this.nextMessageId = 0;
   }

   public void arc$addMessage(Text message) {
      this.nextSync = true;
      this.addMessage(message);
      this.nextSync = false;
   }

   public void arc$addMessageOutSync(Text message, int id) {
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
         line.arc$setMessageId(this.nextMessageId);
         line.arc$setSync(this.nextSync);
         line.arc$setFade(new FadeUtils((long)ClientSetting.INSTANCE.animationTime.getValueInt()));
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
         line.arc$setMessageId(this.nextMessageId);
         line.arc$setSync(this.nextSync);
         line.arc$setFade(new FadeUtils((long)ClientSetting.INSTANCE.animationTime.getValueInt()));
      }

   }

   @Inject(
      at = {@At("HEAD")},
      method = {"addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V"}
   )
   private void onAddMessage(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci) {
      if (this.nextMessageId != 0) {
         this.visibleMessages.removeIf((msg) -> ((IChatHudLineHook)(Object)msg).arc$getMessageId() == this.nextMessageId);
         this.messages.removeIf((msg) -> ((IChatHudLineHook)(Object)msg).arc$getMessageId() == this.nextMessageId);
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
         FadeUtils fadeUtils = line.arc$getFade();
         double ease = fadeUtils == null ? (double)0.0F : fadeUtils.ease(ClientSetting.INSTANCE.ease.getValue());
         double fade = (double)1.0F - ease;
         x += (int)(fade * ClientSetting.INSTANCE.animateOffset.getValue());
         double c = Math.max((double)10.0F, (double)(color >> 24 & 255) * ease);
         return line.arc$getSync() ? drawContext.drawTextWithShadow(textRenderer, text, x, y, ColorUtil.injectAlpha(ClientSetting.INSTANCE.color.getValue(), ClientSetting.INSTANCE.fade.getValue() ? (int)c : color >> 24 & 255).getRGB()) : drawContext.drawTextWithShadow(textRenderer, text, x, y, ColorUtil.injectAlpha(color, ClientSetting.INSTANCE.fade.getValue() ? (int)c : color >> 24 & 255));
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
}
