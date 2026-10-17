package dev.perish.asm.mixins;

import dev.perish.api.utils.Wrapper;
import dev.perish.mod.modules.impl.misc.AutoReconnect;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.text.Text;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget.Builder;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public abstract class MixinDisconnectedScreen extends Screen {
    @Shadow
    @Final
    private DirectionalLayoutWidget grid;
    @Unique
    private ButtonWidget reconnectBtn;
    @Unique
    private double time;

    protected MixinDisconnectedScreen(Text title) {
        super(title);
    }

    @Inject(
            method = "init",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/DirectionalLayoutWidget;refreshPositions()V", shift = Shift.BEFORE)
    )
    private void addButtons(CallbackInfo ci) {
        AutoReconnect autoReconnect = AutoReconnect.INSTANCE;
        if (autoReconnect != null && autoReconnect.lastServerConnection != null && this.grid != null) {
            this.resetReconnectTimer(autoReconnect);
            this.reconnectBtn = new Builder(Text.literal(this.getText(autoReconnect)), button -> this.tryConnecting()).build();
            this.grid.add(this.reconnectBtn);
        }
    }

    @Override
    public void tick() {
        AutoReconnect autoReconnect = AutoReconnect.INSTANCE;
        if (autoReconnect != null && autoReconnect.rejoin() && autoReconnect.lastServerConnection != null) {
            if (this.time <= 0.0) {
                this.tryConnecting();
            } else {
                this.time--;
                if (this.reconnectBtn != null) {
                    this.reconnectBtn.setMessage(Text.literal(this.getText(autoReconnect)));
                }
            }
        }
    }

    @Unique
    private void resetReconnectTimer(AutoReconnect autoReconnect) {
        this.time = autoReconnect.delay.getValue() * 20.0;
    }

    @Unique
    private String getText(AutoReconnect autoReconnect) {
        String reconnectText = "Reconnect";
        if (autoReconnect != null && autoReconnect.rejoin()) {
            reconnectText = reconnectText + " " + String.format("(%.1f)", this.time / 20.0);
        }

        return reconnectText;
    }

    @Unique
    private void tryConnecting() {
        AutoReconnect autoReconnect = AutoReconnect.INSTANCE;
        if (autoReconnect == null) {
            return;
        }

        Pair<ServerAddress, ServerInfo> lastServer = autoReconnect.lastServerConnection;
        if (lastServer == null) {
            return;
        }

        ConnectScreen.connect(new TitleScreen(), Wrapper.mc, lastServer.left(), lastServer.right(), false, null);
    }
}
