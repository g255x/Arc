package dev.Lumi.asm.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.Lumi.Lumi;
import dev.Lumi.api.events.Event;
import dev.Lumi.api.events.impl.ClientTickEvent;
import dev.Lumi.api.events.impl.DoAttackEvent;
import dev.Lumi.api.events.impl.GameLeftEvent;
import dev.Lumi.api.events.impl.OpenScreenEvent;
import dev.Lumi.api.events.impl.ResizeEvent;
import dev.Lumi.core.impl.CommandManager;
import dev.Lumi.core.impl.FontManager;
import dev.Lumi.mod.modules.impl.client.ClientSetting;
import dev.Lumi.mod.modules.impl.player.InteractTweaks;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import net.minecraft.class_1041;
import net.minecraft.class_1074;
import net.minecraft.class_1132;
import net.minecraft.class_1268;
import net.minecraft.class_155;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_239;
import net.minecraft.class_310;
import net.minecraft.class_329;
import net.minecraft.class_3965;
import net.minecraft.class_4093;
import net.minecraft.class_412;
import net.minecraft.class_434;
import net.minecraft.class_435;
import net.minecraft.class_437;
import net.minecraft.class_442;
import net.minecraft.class_542;
import net.minecraft.class_634;
import net.minecraft.class_636;
import net.minecraft.class_638;
import net.minecraft.class_642;
import net.minecraft.class_702;
import net.minecraft.class_746;
import net.minecraft.class_239.class_240;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({class_310.class})
public abstract class MixinMinecraftClient extends class_4093<Runnable> {
   @Shadow
   @Final
   public class_329 field_1705;
   @Shadow
   public int field_1771;
   @Shadow
   public class_746 field_1724;
   @Shadow
   public class_239 field_1765;
   @Shadow
   public class_636 field_1761;
   @Final
   @Shadow
   public class_702 field_1713;
   @Shadow
   public class_638 field_1687;
   @Shadow
   private class_1132 field_1766;
   @Shadow
   public class_437 field_1755;
   @Shadow
   @Final
   private class_1041 field_1704;
   @Unique
   private static long FrogStartTs = 0L;
   @Unique
   private boolean iconSet = false;

   public MixinMinecraftClient(String string) {
      super(string);
   }

   @Inject(
      method = {"onResolutionChanged"},
      at = {@At("TAIL")}
   )
   private void captureResize(CallbackInfo ci) {
      Lumi.EVENT_BUS.post(new ResizeEvent(this.field_1704));
   }

   @Redirect(
      method = {"render"},
      at = @At(
   value = "INVOKE",
   target = "Lcom/mojang/blaze3d/systems/RenderSystem;limitDisplayFPS(I)V"
),
      require = 0
   )
   public void fpsHook(int fps) {
      if (!ClientSetting.INSTANCE.fuckFPSLimit.getValue()) {
         RenderSystem.limitDisplayFPS(fps);
      }

   }

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   void postWindowInit(class_542 args, CallbackInfo ci) {
      FontManager.init();
      FrogStartTs = System.currentTimeMillis();
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   void onTickHead(CallbackInfo ci) {
      if (!this.iconSet) {
         this.iconSet = true;
         this.setIcon();
      }

   }

   @Unique
   private void setIcon() {
      try {
         InputStream iconStream = MixinMinecraftClient.class.getResourceAsStream("/icon.png");
         if (iconStream != null) {
            BufferedImage image = ImageIO.read(iconStream);
            iconStream.close();
            if (image != null) {
               int width = image.getWidth();
               int height = image.getHeight();
               int[] pixels = new int[width * height];
               image.getRGB(0, 0, width, height, pixels, 0, width);
               ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);

               for(int y = 0; y < height; ++y) {
                  for(int x = 0; x < width; ++x) {
                     int pixel = pixels[y * width + x];
                     buffer.put((byte)(pixel >> 16 & 255));
                     buffer.put((byte)(pixel >> 8 & 255));
                     buffer.put((byte)(pixel & 255));
                     buffer.put((byte)(pixel >> 24 & 255));
                  }
               }

               buffer.flip();
               GLFWImage glfwImage = GLFWImage.malloc();
               glfwImage.set(width, height, buffer);
               GLFWImage.Buffer iconBuffer = GLFWImage.malloc(1);
               iconBuffer.put(0, glfwImage);
               GLFW.glfwSetWindowIcon(this.field_1704.method_4490(), iconBuffer);
               glfwImage.free();
               iconBuffer.free();
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   @Inject(
      method = {"setScreen"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onSetScreen(class_437 screen, CallbackInfo info) {
      if (Lumi.EVENT_BUS != null) {
         OpenScreenEvent event = OpenScreenEvent.get(screen);
         Lumi.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            info.cancel();
         }
      }

   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void frogMagentaOverlay(CallbackInfo ci) {
      class_437 screen = this.field_1755;
      if (screen != null) {
         boolean isMainMenu = screen instanceof class_442;
         boolean isLoading = screen instanceof class_435 || screen instanceof class_434 || screen instanceof class_412;
         if (isMainMenu || isLoading) {
            RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 1.0F);
            RenderSystem.clear(16640, false);
         }
      }
   }

   @Inject(
      method = {"doAttack"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/util/hit/HitResult;getType()Lnet/minecraft/util/hit/HitResult$Type;",
   shift = Shift.BEFORE
)}
   )
   public void onAttack(CallbackInfoReturnable<Boolean> cir) {
      Lumi.EVENT_BUS.post(DoAttackEvent.getPre());
   }

   @Inject(
      method = {"doAttack"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/ClientPlayerEntity;swingHand(Lnet/minecraft/util/Hand;)V",
   shift = Shift.AFTER
)}
   )
   public void onAttackPost(CallbackInfoReturnable<Boolean> cir) {
      if (Lumi.EVENT_BUS != null) {
         Lumi.EVENT_BUS.post(DoAttackEvent.getPost());
      }

   }

   @Inject(
      method = {"doAttack"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void doAttackHook(CallbackInfoReturnable<Boolean> cir) {
      if (Lumi.EVENT_BUS != null) {
         DoAttackEvent event = new DoAttackEvent();
         Lumi.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            cir.setReturnValue(false);
         }
      }

   }

   @Inject(
      method = {"disconnect(Lnet/minecraft/client/gui/screen/Screen;)V"},
      at = {@At("HEAD")}
   )
   private void onDisconnect(class_437 screen, CallbackInfo info) {
      if (Lumi.EVENT_BUS != null) {
         Lumi.EVENT_BUS.post(GameLeftEvent.INSTANCE);
      }

      if (Lumi.MODULE != null) {
         Lumi.MODULE.onLogout();
      }

   }

   @Inject(
      method = {"disconnect(Lnet/minecraft/client/gui/screen/Screen;)V"},
      at = {@At("HEAD")}
   )
   private void clearTitleMixin(class_437 screen, CallbackInfo info) {
      if (ClientSetting.INSTANCE.titleFix.getValue()) {
         this.field_1705.method_34003();
         this.field_1705.method_1742();
      }

   }

   @Inject(
      method = {"handleBlockBreaking"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void handleBlockBreaking(boolean breaking, CallbackInfo ci) {
      if (this.field_1771 <= 0 && this.field_1724.method_6115() && InteractTweaks.INSTANCE.multiTask()) {
         if (breaking && this.field_1765 != null && this.field_1765.method_17783() == class_240.field_1332) {
            class_3965 blockHitResult = (class_3965)this.field_1765;
            class_2338 blockPos = blockHitResult.method_17777();
            class_2350 direction;
            if (!this.field_1687.method_8320(blockPos).method_26215() && this.field_1761.method_2902(blockPos, direction = blockHitResult.method_17780())) {
               this.field_1713.method_3054(blockPos, direction);
               this.field_1724.method_6104(class_1268.field_5808);
            }
         } else {
            this.field_1761.method_2925();
         }

         ci.cancel();
      }

   }

   @Inject(
      at = {@At("HEAD")},
      method = {"tick()V"}
   )
   public void tickHead(CallbackInfo info) {
      try {
         if (Lumi.EVENT_BUS != null) {
            Lumi.EVENT_BUS.post(ClientTickEvent.get(Event.Stage.Pre));
         }
      } catch (Exception e) {
         e.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (MinecraftClient.tick() [HEAD]) Message: [" + e.getMessage() + "]");
         }
      }

   }

   @Inject(
      at = {@At("TAIL")},
      method = {"tick()V"}
   )
   public void tickTail(CallbackInfo info) {
      try {
         if (Lumi.EVENT_BUS != null) {
            Lumi.EVENT_BUS.post(ClientTickEvent.get(Event.Stage.Post));
         }
      } catch (Exception e) {
         e.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (MinecraftClient.tick() [TAIL]) Message: [" + e.getMessage() + "]");
         }
      }

   }

   @Overwrite
   private String method_24287() {
      if (ClientSetting.INSTANCE == null) {
         return "Lumi Client: Loading..";
      } else if (ClientSetting.INSTANCE.titleOverride.getValue()) {
         return ClientSetting.INSTANCE.windowTitle.getValue();
      } else {
         StringBuilder stringBuilder = new StringBuilder("Lumi Client");
         stringBuilder.append(" ");
         stringBuilder.append(class_155.method_16673().method_48019());
         class_634 clientPlayNetworkHandler = this.method_1562();
         if (clientPlayNetworkHandler != null && clientPlayNetworkHandler.method_48296().method_10758()) {
            stringBuilder.append(" - ");
            class_642 serverInfo = this.method_1558();
            if (this.field_1766 != null && !this.field_1766.method_3860()) {
               stringBuilder.append(class_1074.method_4662("title.singleplayer", new Object[0]));
            } else if (serverInfo != null && serverInfo.method_52811()) {
               stringBuilder.append(class_1074.method_4662("title.multiplayer.realms", new Object[0]));
            } else if (this.field_1766 != null || serverInfo != null && serverInfo.method_2994()) {
               stringBuilder.append(class_1074.method_4662("title.multiplayer.lan", new Object[0]));
            } else {
               stringBuilder.append(class_1074.method_4662("title.multiplayer.other", new Object[0]));
            }
         }

         return stringBuilder.toString();
      }
   }

   @Shadow
   public class_634 method_1562() {
      return null;
   }

   @Shadow
   public class_642 method_1558() {
      return null;
   }
}
