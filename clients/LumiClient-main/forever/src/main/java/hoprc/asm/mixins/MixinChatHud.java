package hoprc.asm.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import hoprc.api.interfaces.IChatHudHook;
import hoprc.api.interfaces.IChatHudLineHook;
import dev.kissoo.api.utils.math.Easing;
import dev.kissoo.api.utils.math.FadeUtils;
import dev.kissoo.api.utils.render.ColorUtil;
import dev.kissoo.mod.modules.impl.client.ClientSetting;
import dev.kissoo.mod.modules.impl.hud.ChatPosition;
import hoprc.asm.accessors.IChatHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Mixin(ChatHud.class)
public abstract class MixinChatHud implements IChatHudHook {
   @Unique
   private int nextMessageId = 0;

   @Unique
   private boolean nextSync;

   @Unique
   private int chatLineIndex;

   @Unique
   private boolean alienclient$chatMatrixPushed = false;

   @Unique
   private int alienclient$chatOffsetX = 0;

   @Unique
   private int alienclient$chatOffsetY = 0;

   @Final
   @Shadow
   private List<ChatHudLine.Visible> visibleMessages;

   @Shadow
   @Final
   private List<ChatHudLine> messages;

   @Shadow
   @Final
   private MinecraftClient client;

   @Inject(method = "<init>", at = @At("TAIL"))
   public void onInit(MinecraftClient client, CallbackInfo ci) {
      ((IChatHud) this).setMessages(new CopyOnWriteArrayList<>());
      ((IChatHud) this).setVisibleMessages(new CopyOnWriteArrayList<>());
   }

   @Override
   public void alienClient$addMessage(Text message, int id) {
      this.nextMessageId = id;
      this.nextSync = true;
      this.addMessage(message);
      this.nextSync = false;
      this.nextMessageId = 0;
   }

   @Override
   public void alienClient$addMessage(Text message) {
      this.nextSync = true;
      this.addMessage(message);
      this.nextSync = false;
   }

   @Override
   public void alienClient$addMessageOutSync(Text message, int id) {
      this.nextMessageId = id;
      this.addMessage(message);
      this.nextMessageId = 0;
   }

   @Unique
   private static boolean alienClient$hasInstance() {
      return ClientSetting.INSTANCE != null;
   }

   @Inject(
           method = "addVisibleMessage",
           at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V", ordinal = 0, shift = Shift.AFTER)
   )
   private void onAddMessageAfterNewChatHudLineVisible(ChatHudLine message, CallbackInfo ci) {
      IChatHudLineHook line = (IChatHudLineHook) (Object) this.visibleMessages.getFirst();
      if (line != null) {
         line.alienClient$setMessageId(this.nextMessageId);
         line.alienClient$setSync(this.nextSync);
         line.alienClient$setFade(new FadeUtils(alienClient$hasInstance() ? ClientSetting.INSTANCE.animationTime.getValueInt() : 300));
      }
   }

   @Inject(
           method = "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V",
           at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V", ordinal = 0, shift = Shift.AFTER)
   )
   private void onAddMessageAfterNewChatHudLine(ChatHudLine message, CallbackInfo ci) {
      IChatHudLineHook line = (IChatHudLineHook) (Object) this.messages.getFirst();
      if (line != null) {
         line.alienClient$setMessageId(this.nextMessageId);
         line.alienClient$setSync(this.nextSync);
         line.alienClient$setFade(new FadeUtils(alienClient$hasInstance() ? ClientSetting.INSTANCE.animationTime.getValueInt() : 300));
      }
   }

   @Inject(
           method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
           at = @At("HEAD")
   )
   private void onAddMessage(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci) {
      if (this.nextMessageId != 0) {
         this.visibleMessages.removeIf(msg -> ((IChatHudLineHook) (Object) msg).alienClient$getMessageId() == this.nextMessageId);
         this.messages.removeIf(msg -> ((IChatHudLineHook) (Object) msg).alienClient$getMessageId() == this.nextMessageId);
      }
   }

   @Redirect(
           method = "addVisibleMessage",
           at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 2, remap = false),
           require = 0
   )
   public int chatLinesSize(List<ChatHudLine.Visible> list) {
      return alienClient$hasInstance() && ClientSetting.INSTANCE.isOn() && ClientSetting.INSTANCE.infiniteChat.getValue()
              ? Integer.MIN_VALUE
              : list.size();
   }

   @Redirect(
           method = "getWidth",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/gui/hud/ChatHud;getWidth(D)I"
           ),
           require = 0
   )
   private int redirectChatWidth(double widthOption) {
      if (ChatPosition.shouldEdit()) {
         return ChatPosition.getWidthValue();
      }
      return ChatHud.getWidth(widthOption);
   }

   @Redirect(
           method = "getHeight",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/gui/hud/ChatHud;getHeight(D)I"
           ),
           require = 0
   )
   private int redirectChatHeight(double heightOption) {
      if (ChatPosition.shouldEdit()) {
         return ChatPosition.getHeightValue();
      }
      return ChatHud.getHeight(heightOption);
   }

   @Inject(method = "render", at = @At("HEAD"))
   private void onRenderHead(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
      if (!ChatPosition.shouldEdit()) {
         this.alienclient$chatOffsetX = 0;
         this.alienclient$chatOffsetY = 0;
         return;
      }

      this.alienclient$chatMatrixPushed = true;
      context.getMatrices().push();

      float defaultX = 4.0f;
      float defaultBottomY = this.client.getWindow().getScaledHeight() - 40.0f;

      float targetX = ChatPosition.getXPos();
      float targetBottomY = ChatPosition.getYPos() + ChatPosition.getHeightValue();

      this.alienclient$chatOffsetX = Math.round(targetX - defaultX);
      this.alienclient$chatOffsetY = Math.round(targetBottomY - defaultBottomY);

      context.getMatrices().translate(this.alienclient$chatOffsetX, this.alienclient$chatOffsetY, 0.0f);
   }

   @Inject(method = "render", at = @At("RETURN"))
   private void onRenderReturn(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
      if (this.alienclient$chatMatrixPushed) {
         this.alienclient$chatMatrixPushed = false;
         context.getMatrices().pop();
      }
   }

   @Redirect(
           method = "render",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I"
           ),
           require = 0
   )
   private int drawStringWithShadow(DrawContext drawContext, TextRenderer textRenderer, OrderedText text, int x, int y, int color) {
      IChatHudLineHook line = (IChatHudLineHook) (Object) this.visibleMessages.get(this.chatLineIndex);
      if (line != null && alienClient$hasInstance()) {
         FadeUtils fadeUtils = line.alienClient$getFade();
         double ease = fadeUtils == null ? 0.0 : fadeUtils.ease((Easing) ClientSetting.INSTANCE.ease.getValue());
         double fade = 1.0 - ease;
         x += (int) (fade * ClientSetting.INSTANCE.animateOffset.getValue());
         double c = Math.max(10.0, (color >> 24 & 0xFF) * ease);

         return line.alienClient$getSync()
                 ? drawContext.drawTextWithShadow(
                 textRenderer,
                 text,
                 x,
                 y,
                 ColorUtil.injectAlpha(
                         ClientSetting.INSTANCE.color.getValue(),
                         ClientSetting.INSTANCE.fade.getValue() ? (int) c : color >> 24 & 0xFF
                 ).getRGB()
         )
                 : drawContext.drawTextWithShadow(
                 textRenderer,
                 text,
                 x,
                 y,
                 ColorUtil.injectAlpha(
                         color,
                         ClientSetting.INSTANCE.fade.getValue() ? (int) c : color >> 24 & 0xFF
                 )
         );
      } else {
         return drawContext.drawTextWithShadow(textRenderer, text, x, y, color);
      }
   }

   @Inject(
           method = "render",
           at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHudLine$Visible;addedTime()I")
   )
   public void getChatLineIndex(CallbackInfo ci, @Local(ordinal = 13) int chatLineIndex) {
      this.chatLineIndex = chatLineIndex;
   }

   @ModifyVariable(method = "render", at = @At("STORE"))
   private MessageIndicator removeMessageIndicator(MessageIndicator messageIndicator) {
      return alienClient$hasInstance() && ClientSetting.INSTANCE.hideIndicator.getValue() ? null : messageIndicator;
   }

   // ======================
   // 鼠标坐标修正
   // ======================

   @ModifyVariable(method = "toChatLineX", at = @At("HEAD"), ordinal = 0, argsOnly = true)
   private double modifyChatMouseX(double x) {
      if (ChatPosition.shouldEdit()) {
         return x - this.alienclient$chatOffsetX;
      }
      return x;
   }

   @ModifyVariable(method = "toChatLineY", at = @At("HEAD"), ordinal = 0, argsOnly = true)
   private double modifyChatMouseY(double y) {
      if (ChatPosition.shouldEdit()) {
         return y - this.alienclient$chatOffsetY;
      }
      return y;
   }

   @Inject(method = "getTextStyleAt", at = @At("HEAD"), cancellable = true)
   private void onGetTextStyleAt(double mouseX, double mouseY, CallbackInfoReturnable<Style> cir) {
   }

   @Shadow
   public abstract void addMessage(Text var1);

   @Shadow
   public abstract int getWidth();

   @Shadow
   public abstract int getHeight();
}