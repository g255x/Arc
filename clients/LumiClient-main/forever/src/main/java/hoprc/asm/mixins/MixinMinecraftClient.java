package hoprc.asm.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.kissoo.Forever;
import dev.kissoo.chat.ChatClient;
import dev.kissoo.optimizations.FrameInterpolation;
import dev.kissoo.mod.modules.impl.misc.IrcModule;
import dev.kissoo.api.events.Event;
import dev.kissoo.api.events.impl.ClientTickEvent;
import dev.kissoo.api.events.impl.DoAttackEvent;
import dev.kissoo.api.events.impl.GameLeftEvent;
import dev.kissoo.api.events.impl.OpenScreenEvent;
import dev.kissoo.api.events.impl.ResizeEvent;
import dev.kissoo.api.utils.render.Render2DUtil;
import hoprc.api.utils.memory.FramebufferLeakTracker;
import dev.kissoo.core.impl.CommandManager;
import hoprc.api.core.impl.FontManager;
import hoprc.api.core.impl.ShaderManager;
import dev.kissoo.mod.modules.impl.client.ClientSetting;
import dev.kissoo.mod.gui.background.MainMenuModePreference;
import dev.kissoo.mod.modules.impl.combat.AutoRegear;
import dev.kissoo.mod.modules.impl.player.InteractTweaks;
import java.awt.Color;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Hand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
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


@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient extends ReentrantThreadExecutor<Runnable> {
   @Shadow
   @Final
   public InGameHud inGameHud;
   @Shadow
   public int attackCooldown;
   @Shadow
   public ClientPlayerEntity player;
   @Shadow
   public HitResult crosshairTarget;
   @Shadow
   public ClientPlayerInteractionManager interactionManager;
   @Final
   @Shadow
   public ParticleManager particleManager;
   @Shadow
   public ClientWorld world;
   @Shadow
   private IntegratedServer server;
   @Shadow
   public Screen currentScreen;
   @Shadow
   @Final
   private Window window;

   @Unique
   private static long alienStartTs = 0L;

   @Unique
   private static String currentBetaUser = null;

   public MixinMinecraftClient(String string) {
      super(string);
   }

   @Inject(method = "onResolutionChanged", at = @At("TAIL"))
   private void captureResize(CallbackInfo ci) {
      Forever.EVENT_BUS.post(new ResizeEvent(this.window));
   }

   @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;limitDisplayFPS(I)V"), require = 0)
   public void fpsHook(int fps) {
      if (ClientSetting.INSTANCE != null && !ClientSetting.INSTANCE.fuckFPSLimit.getValue()) {
         RenderSystem.limitDisplayFPS(fps);
      }
   }

   @Inject(method = "<init>", at = @At("TAIL"))
   private void postWindowInit(RunArgs args, CallbackInfo ci) {
      try {
         FontManager.init();
      } catch (Throwable t) {
         System.err.println("[Forever] FontManager init failed; continuing startup.");
         t.printStackTrace();
      }
      alienStartTs = System.currentTimeMillis();

      if (currentBetaUser == null) {
         currentBetaUser = System.getProperty("foreverClient.betaUser", "Unknown");
      }
   }

   // 强制初始化 Wrapper.mc 静态字段(必须在 getInstance() 非 null 之后)。
   // native 混淆把 mc 的读取转成 JNI GetStaticObjectField，而 JNI 读静态字段
   // 不会触发 Wrapper 接口的 <clinit>，导致 native 代码读到 null 的 mc → NPE/段错误。
   // 之前放在 <init> TAIL 不生效:TAIL 在 super() 调用后立即注入，此时
   // MinecraftClient.instance(=field_1700) 还没赋值(构造器偏移 154 才 putstatic)，
   // getInstance() 仍返回 null。run() 在构造器完全结束后才被 Main.main 调用，
   // 而 fabric CLIENT_STARTED(module 初始化)发生在 run() 内部(偏移 864)，比 HEAD 晚，
   // 所以在这里触发 <clinit> 能保证 native 代码第一次读 Wrapper.mc 时它已非 null。
   @Inject(method = "run", at = @At("HEAD"))
   private void touchWrapperMcBeforeInit(CallbackInfo ci) {
      Object _mc = dev.kissoo.api.utils.Wrapper.mc;
   }

   @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
   private void onSetScreen(Screen screen, CallbackInfo info) {
      if (screen instanceof ShulkerBoxScreen) {
         if (AutoRegear.INSTANCE != null && AutoRegear.INSTANCE.isOn()) {
            if (AutoRegear.INSTANCE.silentDisplay.getValue()) {
               AutoRegear.INSTANCE.silentScreenActive = true;
               info.cancel();
            }
            return;
         }

         OpenScreenEvent event = OpenScreenEvent.get(screen);
         Forever.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            info.cancel();
         }
      }
   }
   @Inject(method = "render", at = @At("HEAD"))
   private void onRenderHead(boolean tick, CallbackInfo ci) {
      FramebufferLeakTracker.INSTANCE.onRenderTick();
   }

   @Inject(method = "render", at = @At(value = "INVOKE",
           target = "Lnet/minecraft/client/util/Window;swapBuffers()V"))
   private void beforeSwapBuffers(boolean tick, CallbackInfo ci) {
      FrameInterpolation.interp();
   }

   @Inject(at = @At("HEAD"), method = "tick()V")
   public void tickHead(CallbackInfo info) {
      FramebufferLeakTracker.INSTANCE.tick();

      if (AutoRegear.INSTANCE != null && AutoRegear.INSTANCE.silentScreenActive) {
         if (this.player == null || !(this.player.currentScreenHandler instanceof ShulkerBoxScreenHandler)) {
            AutoRegear.INSTANCE.silentScreenActive = false;
         }
      }

      try {
         Forever.EVENT_BUS.post(ClientTickEvent.get(Event.Stage.Pre));
      } catch (Exception var3) {
         var3.printStackTrace();
         if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (MinecraftClient.tick() [HEAD]) Message: [" + var3.getMessage() + "]");
         }
      }
   }

   @Inject(method = "render", at = @At("TAIL"))
   private void vitalityMagentaOverlay(CallbackInfo ci) {
      if (MainMenuModePreference.isPhobosSelected()) {
         return;
      }
      Screen screen = this.currentScreen;
      if (screen != null) {
         boolean isMainMenu = screen instanceof TitleScreen;
         boolean isLoading = screen instanceof ProgressScreen || screen instanceof DownloadingTerrainScreen || screen instanceof ConnectScreen;
         if (isMainMenu || isLoading) {
            if (Forever.SHADER != null && !Forever.SHADER.fullNullCheck()) {
               float w = this.window.getScaledWidth();
               float h = this.window.getScaledHeight();
               float t = (float)(System.currentTimeMillis() % 100000L) / 1000.0F;
               ManagedShaderEffect gradient = Forever.SHADER.getShader(ShaderManager.Shader.Gradient);
               gradient.setUniformValue("alpha2", 0.85F);
               gradient.setUniformValue("rgb", 0.78F, 0.05F, 0.59F);
               gradient.setUniformValue("rgb1", 0.56F, 0.06F, 0.68F);
               gradient.setUniformValue("rgb2", 0.93F, 0.12F, 0.63F);
               gradient.setUniformValue("rgb3", 0.64F, 0.0F, 0.64F);
               gradient.setUniformValue("step", 180.0F);
               gradient.setUniformValue("radius", 2.0F);
               gradient.setUniformValue("quality", 1.0F);
               gradient.setUniformValue("divider", 150.0F);
               gradient.setUniformValue("maxSample", 10.0F);
               gradient.setUniformValue("resolution", w, h);
               gradient.setUniformValue("time", t * 300.0F);
               gradient.render(((MinecraftClient)((Object)this)).getRenderTickCounter().getTickDelta(true));
               ManagedShaderEffect pulse = Forever.SHADER.getShader(ShaderManager.Shader.Pulse);
               pulse.setUniformValue("mixFactor", 0.65F);
               pulse.setUniformValue("minAlpha", 0.35F);
               pulse.setUniformValue("radius", 2.0F);
               pulse.setUniformValue("quality", 1.0F);
               pulse.setUniformValue("divider", 150.0F);
               pulse.setUniformValue("maxSample", 10.0F);
               pulse.setUniformValue("color", 0.85F, 0.05F, 0.66F);
               pulse.setUniformValue("color2", 0.56F, 0.06F, 0.68F);
               pulse.setUniformValue("time", t);
               pulse.setUniformValue("size", 12.0F);
               pulse.setUniformValue("resolution", w, h);
               pulse.render(((MinecraftClient)((Object)this)).getRenderTickCounter().getTickDelta(true));
               MatrixStack m = new MatrixStack();
               w = this.window.getScaledWidth();
               h = this.window.getScaledHeight();
               long now = System.currentTimeMillis();
               long elapsed = now - alienStartTs;
               if (elapsed < 2400L) {
                  float p = Math.max(0.0F, Math.min(1.0F, (float)elapsed / 2400.0F));
                  int aTop = (int)(180.0 * Math.sin(p * Math.PI));
                  Color c1 = new Color(28, 60, 110, aTop);
                  Color c2 = new Color(190, 50, 160, aTop);
                  Render2DUtil.verticalGradient(m, 0.0F, 0.0F, w, h, c1, c2);
                  float r = Math.min(w, h) * (0.12F + 0.18F * p);
                  Render2DUtil.drawCircle(m, w / 2.0F, h / 2.0F, r, new Color(255, 255, 255, (int)(70.0F * p)), 80);
                  Render2DUtil.drawCircle(m, w / 2.0F, h / 2.0F, r * 1.2F, new Color(120, 220, 255, (int)(50.0F * p)), 80);
               } else {
                  float phase = (float)(now % 6000L) / 6000.0F;
                  float angle = 0.523599F;
                  float dx = (float)Math.tan(angle) * h;
                  float base = -h;
                  float spacing = 28.0F;
                  float shift = phase * spacing * 5.2F;
                  int cA1 = new Color(230, 60, 170, 64).getRGB();
                  int cA2 = new Color(160, 40, 130, 48).getRGB();
                  int nodeC = new Color(255, 255, 255, 42).getRGB();

                  for (float i = base; i < w; i += spacing) {
                     float x0 = i + shift;
                     Render2DUtil.drawLine(m, x0, 0.0F, x0 + dx, h, cA1);
                     float nx = x0 + dx * 0.25F;
                     float ny = h * 0.25F;
                     Render2DUtil.drawCircle(m, nx, ny, 2.5F, new Color(nodeC, true), 32);
                  }

                  for (float i = base + spacing / 2.0F; i < w; i += spacing) {
                     float x0 = i + shift * 0.85F;
                     Render2DUtil.drawLine(m, x0, 0.0F, x0 + dx, h, cA2);
                     float nx = x0 + dx * 0.65F;
                     float ny = h * 0.6F;
                     Render2DUtil.drawCircle(m, nx, ny, 2.0F, new Color(nodeC, true), 28);
                  }
               }
            }
         }
      }
   }

   @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
   private void forever$blockIrcAttack(CallbackInfoReturnable<Boolean> cir) {
      if (crosshairTarget instanceof EntityHitResult ehr
              && ehr.getEntity() instanceof PlayerEntity target
              && IrcModule.shouldProtectIrcUser(target.getGameProfile().getName())) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
           method = "doAttack",
           at = @At(value = "INVOKE", target = "Lnet/minecraft/util/hit/HitResult;getType()Lnet/minecraft/util/hit/HitResult$Type;", shift = Shift.BEFORE)
   )
   public void onAttack(CallbackInfoReturnable<Boolean> cir) {
      Forever.EVENT_BUS.post(DoAttackEvent.getPre());
   }

   @Inject(
           method = "doAttack",
           at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;swingHand(Lnet/minecraft/util/Hand;)V", shift = Shift.AFTER)
   )
   public void onAttackPost(CallbackInfoReturnable<Boolean> cir) {
      Forever.EVENT_BUS.post(DoAttackEvent.getPost());
   }

   @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
   private void onDisconnect(Screen screen, CallbackInfo info) {
      if (this.world != null) {
         Forever.EVENT_BUS.post(GameLeftEvent.INSTANCE);
      }
   }

   @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
   private void clearTitleMixin(Screen screen, CallbackInfo info) {
      if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.titleFix.getValue()) {
         this.inGameHud.clearTitle();
         this.inGameHud.setDefaultTitleFade();
      }
   }

   @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
   private void handleBlockBreaking(boolean breaking, CallbackInfo ci) {
      if (this.attackCooldown <= 0 && this.player.isUsingItem() && InteractTweaks.INSTANCE != null && InteractTweaks.INSTANCE.multiTask()) {
         if (breaking && this.crosshairTarget != null && this.crosshairTarget.getType() == Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult)this.crosshairTarget;
            BlockPos blockPos = blockHitResult.getBlockPos();
            if (!this.world.getBlockState(blockPos).isAir()) {
               Direction direction = blockHitResult.getSide();
               if (this.interactionManager.updateBlockBreakingProgress(blockPos, direction)) {
                  this.particleManager.addBlockBreakingParticles(blockPos, direction);
                  this.player.swingHand(Hand.MAIN_HAND);
               }
            }
         } else {
            this.interactionManager.cancelBlockBreaking();
         }

         ci.cancel();
      }
   }

   @Inject(at = @At("TAIL"), method = "tick()V")
   public void tickTail(CallbackInfo info) {
      try {
         Forever.EVENT_BUS.post(ClientTickEvent.get(Event.Stage.Post));
      } catch (Exception var3) {
         var3.printStackTrace();
         if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (MinecraftClient.tick() [TAIL]) Message: [" + var3.getMessage() + "]");
         }
      }
   }


   @Overwrite
   private String getWindowTitle() {
      StringBuilder title = new StringBuilder("ForeverClient");
      ClientPlayNetworkHandler cpnh = this.getNetworkHandler();
      if (cpnh != null && cpnh.getConnection().isOpen()) {
         title.append(" - ");
         ServerInfo serverInfo = this.getCurrentServerEntry();
         if (this.server != null && !this.server.isRemote()) {
            title.append(I18n.translate("title.singleplayer"));
         } else if (serverInfo != null && serverInfo.isRealm()) {
            title.append(I18n.translate("title.multiplayer.realms"));
         } else if (this.server != null || serverInfo != null && serverInfo.isLocal()) {
            title.append(I18n.translate("title.multiplayer.lan"));
         } else {
            title.append(I18n.translate("title.multiplayer.other"));
         }
      }
      return title.toString();
   }

   @Shadow
   public ClientPlayNetworkHandler getNetworkHandler() {
      return null;
   }

   @Shadow
   public ServerInfo getCurrentServerEntry() {
      return null;
   }
}
