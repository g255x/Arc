package dev.G255.Lumi.asm.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.events.Event;
import dev.G255.Lumi.api.events.impl.ClientTickEvent;
import dev.G255.Lumi.api.events.impl.DoAttackEvent;
import dev.G255.Lumi.api.events.impl.GameLeftEvent;
import dev.G255.Lumi.api.events.impl.OpenScreenEvent;
import dev.G255.Lumi.api.events.impl.ResizeEvent;
import dev.G255.Lumi.api.utils.render.Render2DUtil;
import dev.G255.Lumi.core.impl.CommandManager;
import dev.G255.Lumi.core.impl.FontManager;
import dev.G255.Lumi.core.impl.ShaderManager;
import dev.G255.Lumi.mod.modules.impl.client.ClientSetting;
import dev.G255.Lumi.mod.modules.impl.player.InteractTweaks;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;

@Mixin(value={MinecraftClient.class})
public abstract class MixinMinecraftClient
extends ReentrantThreadExecutor<Runnable> {
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
    private static long FrogStartTs = 0L;

    public MixinMinecraftClient(String string) {
        super(string);
    }

    @Inject(method={"onResolutionChanged"}, at={@At(value="TAIL")})
    private void captureResize(CallbackInfo ci) {
        Lumi.EVENT_BUS.post(new ResizeEvent(this.window));
    }

    @Redirect(method={"render"}, at=@At(value="INVOKE", target="Lcom/mojang/blaze3d/systems/RenderSystem;limitDisplayFPS(I)V"), require=0)
    public void fpsHook(int fps) {
        if (!ClientSetting.INSTANCE.fuckFPSLimit.getValue()) {
            RenderSystem.limitDisplayFPS((int)fps);
        }
    }

    @Inject(method={"<init>"}, at={@At(value="TAIL")})
    void postWindowInit(RunArgs args, CallbackInfo ci) {
        FontManager.init();
        FrogStartTs = System.currentTimeMillis();
        try {
            InputStream iconStream = MixinMinecraftClient.class.getResourceAsStream("/icon.png");
            if (iconStream != null) {
                BufferedImage image = ImageIO.read(iconStream);
                iconStream.close();
                if (image != null) {
                    int w = image.getWidth();
                    int h = image.getHeight();
                    int[] pixels = new int[w * h];
                    image.getRGB(0, 0, w, h, pixels, 0, w);
                    ByteBuffer buffer = MemoryUtil.memAlloc(w * h * 4);
                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            int pixel = pixels[y * w + x];
                            buffer.put((byte)((pixel >> 16) & 0xFF));
                            buffer.put((byte)((pixel >> 8) & 0xFF));
                            buffer.put((byte)(pixel & 0xFF));
                            buffer.put((byte)((pixel >> 24) & 0xFF));
                        }
                    }
                    buffer.flip();
                    try (MemoryStack stack = MemoryStack.stackPush()) {
                        GLFWImage.Buffer gb = GLFWImage.malloc(1, stack);
                        gb.width(w).height(h).pixels(buffer);
                        long handle = ((MinecraftClient)(Object)this).getWindow().getHandle();
                        GLFW.glfwSetWindowIcon(handle, gb);
                    }
                    MemoryUtil.memFree(buffer);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Inject(method={"setScreen"}, at={@At(value="HEAD")}, cancellable=true)
    private void onSetScreen(Screen screen, CallbackInfo info) {
        if (Lumi.EVENT_BUS != null) {
            OpenScreenEvent event = OpenScreenEvent.get(screen);
            Lumi.EVENT_BUS.post(event);
            if (event.isCancelled()) {
                info.cancel();
            }
        }
    }

    @Inject(method={"render"}, at={@At(value="TAIL")})
    private void frogMagentaOverlay(CallbackInfo ci) {
        float ny;
        float nx;
        float x0;
        float i;
        Screen screen = this.currentScreen;
        if (screen == null) {
            return;
        }
        if (Lumi.SHADER.fullNullCheck()) {
            return;
        }
        float w = this.window.getScaledWidth();
        float h = this.window.getScaledHeight();
        float t = (float)(System.currentTimeMillis() % 100000L) / 1000.0f;
        ManagedShaderEffect gradient = Lumi.SHADER.getShader(ShaderManager.Shader.Gradient);
        gradient.setUniformValue("alpha2", 0.85f);
        gradient.setUniformValue("rgb", 0.78f, 0.05f, 0.59f);
        gradient.setUniformValue("rgb1", 0.56f, 0.06f, 0.68f);
        gradient.setUniformValue("rgb2", 0.93f, 0.12f, 0.63f);
        gradient.setUniformValue("rgb3", 0.64f, 0.0f, 0.64f);
        gradient.setUniformValue("step", 180.0f);
        gradient.setUniformValue("radius", 2.0f);
        gradient.setUniformValue("quality", 1.0f);
        gradient.setUniformValue("divider", 150.0f);
        gradient.setUniformValue("maxSample", 10.0f);
        gradient.setUniformValue("resolution", w, h);
        gradient.setUniformValue("time", t * 300.0f);
        gradient.render(((MinecraftClient)(Object)this).getRenderTickCounter().getTickDelta(true));
        MatrixStack m = new MatrixStack();
        w = this.window.getScaledWidth();
        h = this.window.getScaledHeight();
        long now = System.currentTimeMillis();
        long elapsed = now - FrogStartTs;
        if (elapsed < 2400L) {
            float p = Math.max(0.0f, Math.min(1.0f, (float)elapsed / 2400.0f));
            int aTop = (int)(180.0 * Math.sin((double)p * Math.PI));
            Color c1 = new Color(28, 60, 110, aTop);
            Color c2 = new Color(190, 50, 160, aTop);
            Render2DUtil.verticalGradient(m, 0.0f, 0.0f, w, h, c1, c2);
            float r = Math.min(w, h) * (0.12f + 0.18f * p);
            Render2DUtil.drawCircle(m, w / 2.0f, h / 2.0f, r, new Color(255, 255, 255, (int)(70.0f * p)), 80);
            Render2DUtil.drawCircle(m, w / 2.0f, h / 2.0f, r * 1.2f, new Color(120, 220, 255, (int)(50.0f * p)), 80);
            return;
        }
        float phase = (float)(now % 6000L) / 6000.0f;
        float angle = 0.523599f;
        float dx = (float)Math.tan(angle) * h;
        float base = -h;
        float spacing = 28.0f;
        float shift = phase * spacing * 5.2f;
        int cA1 = new Color(230, 60, 170, 64).getRGB();
        int cA2 = new Color(160, 40, 130, 48).getRGB();
        int nodeC = new Color(255, 255, 255, 42).getRGB();
        for (i = base; i < w; i += spacing) {
            x0 = i + shift;
            Render2DUtil.drawLine(m, x0, 0.0f, x0 + dx, h, cA1);
            nx = x0 + dx * 0.25f;
            ny = h * 0.25f;
            Render2DUtil.drawCircle(m, nx, ny, 2.5f, new Color(nodeC, true), 32);
        }
        for (i = base + spacing / 2.0f; i < w; i += spacing) {
            x0 = i + shift * 0.85f;
            Render2DUtil.drawLine(m, x0, 0.0f, x0 + dx, h, cA2);
            nx = x0 + dx * 0.65f;
            ny = h * 0.6f;
            Render2DUtil.drawCircle(m, nx, ny, 2.0f, new Color(nodeC, true), 28);
        }
    }

    @Inject(method={"doAttack"}, at={@At(value="INVOKE", target="Lnet/minecraft/util/hit/HitResult;getType()Lnet/minecraft/util/hit/HitResult$Type;", shift=At.Shift.BEFORE)})
    public void onAttack(CallbackInfoReturnable<Boolean> cir) {
        Lumi.EVENT_BUS.post(DoAttackEvent.getPre());
    }

    @Inject(method={"doAttack"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/network/ClientPlayerEntity;swingHand(Lnet/minecraft/util/Hand;)V", shift=At.Shift.AFTER)})
    public void onAttackPost(CallbackInfoReturnable<Boolean> cir) {
        if (Lumi.EVENT_BUS != null) {
            Lumi.EVENT_BUS.post(DoAttackEvent.getPost());
        }
    }

    @Inject(method={"doAttack"}, at={@At(value="HEAD")}, cancellable=true)
    public void doAttackHook(CallbackInfoReturnable<Boolean> cir) {
        if (Lumi.EVENT_BUS != null) {
            DoAttackEvent event = new DoAttackEvent();
            Lumi.EVENT_BUS.post(event);
            if (event.isCancelled()) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method={"disconnect(Lnet/minecraft/client/gui/screen/Screen;)V"}, at={@At(value="HEAD")})
    private void onDisconnect(Screen screen, CallbackInfo info) {
        if (Lumi.EVENT_BUS != null) {
            Lumi.EVENT_BUS.post(GameLeftEvent.INSTANCE);
        }
        if (Lumi.MODULE != null) {
            Lumi.MODULE.onLogout();
        }
    }

    @Inject(method={"disconnect(Lnet/minecraft/client/gui/screen/Screen;)V"}, at={@At(value="HEAD")})
    private void clearTitleMixin(Screen screen, CallbackInfo info) {
        if (ClientSetting.INSTANCE.titleFix.getValue()) {
            this.inGameHud.clearTitle();
            this.inGameHud.setDefaultTitleFade();
        }
    }

    @Inject(method={"handleBlockBreaking"}, at={@At(value="HEAD")}, cancellable=true)
    private void handleBlockBreaking(boolean breaking, CallbackInfo ci) {
        if (this.attackCooldown <= 0 && this.player.isUsingItem() && InteractTweaks.INSTANCE.multiTask()) {
            if (breaking && this.crosshairTarget != null && this.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                Direction direction;
                BlockHitResult blockHitResult = (BlockHitResult)this.crosshairTarget;
                BlockPos blockPos = blockHitResult.getBlockPos();
                if (!this.world.getBlockState(blockPos).isAir() && this.interactionManager.updateBlockBreakingProgress(blockPos, direction = blockHitResult.getSide())) {
                    this.particleManager.addBlockBreakingParticles(blockPos, direction);
                    this.player.swingHand(Hand.MAIN_HAND);
                }
            } else {
                this.interactionManager.cancelBlockBreaking();
            }
            ci.cancel();
        }
    }

    @Inject(at={@At(value="HEAD")}, method={"tick()V"})
    public void tickHead(CallbackInfo info) {
        block2: {
            try {
                if (Lumi.EVENT_BUS != null) {
                    Lumi.EVENT_BUS.post(ClientTickEvent.get(Event.Stage.Pre));
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                if (!ClientSetting.INSTANCE.debug.getValue()) break block2;
                CommandManager.sendMessage("\u00a74An error has occurred (MinecraftClient.tick() [HEAD]) Message: [" + e.getMessage() + "]");
            }
        }
    }

    @Inject(at={@At(value="TAIL")}, method={"tick()V"})
    public void tickTail(CallbackInfo info) {
        block2: {
            try {
                if (Lumi.EVENT_BUS != null) {
                    Lumi.EVENT_BUS.post(ClientTickEvent.get(Event.Stage.Post));
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                if (!ClientSetting.INSTANCE.debug.getValue()) break block2;
                CommandManager.sendMessage("\u00a74An error has occurred (MinecraftClient.tick() [TAIL]) Message: [" + e.getMessage() + "]");
            }
        }
        // 出口点混淆：定期许可证完整性检查
        if ((Lumi._tickCounter++ & 0x3FF) == 0) {
            Lumi._periodicGuard();
        }
    }

    @Inject(method={"stop"}, at={@At(value="HEAD")})
    private void onStop(CallbackInfo ci) {
        if (Lumi.loaded) {
            Lumi.save();
        }
    }

    /**
     * @author Frog
     * @reason Custom Window Title
     */
    @Overwrite
    private String getWindowTitle() {
        if (ClientSetting.INSTANCE == null) {
            return Lumi.NAME + ": Loading..";
        }
        if (ClientSetting.INSTANCE.titleOverride.getValue()) {
            return ClientSetting.INSTANCE.windowTitle.getValue();
        }
        StringBuilder stringBuilder = new StringBuilder(Lumi.NAME);
        stringBuilder.append(" ");
        stringBuilder.append(SharedConstants.getGameVersion().getName());
        ClientPlayNetworkHandler clientPlayNetworkHandler = this.getNetworkHandler();
        if (clientPlayNetworkHandler != null && clientPlayNetworkHandler.getConnection().isOpen()) {
            stringBuilder.append(" - ");
            ServerInfo serverInfo = this.getCurrentServerEntry();
            if (this.server != null && !this.server.isRemote()) {
                stringBuilder.append(I18n.translate((String)"title.singleplayer", (Object[])new Object[0]));
            } else if (serverInfo != null && serverInfo.isRealm()) {
                stringBuilder.append(I18n.translate((String)"title.multiplayer.realms", (Object[])new Object[0]));
            } else if (!(server != null || serverInfo != null && serverInfo.isLocal())) {
                stringBuilder.append(I18n.translate((String)"title.multiplayer.other", (Object[])new Object[0]));
            } else {
                stringBuilder.append(I18n.translate((String)"title.multiplayer.lan", (Object[])new Object[0]));
            }
        }
        return stringBuilder.toString();
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