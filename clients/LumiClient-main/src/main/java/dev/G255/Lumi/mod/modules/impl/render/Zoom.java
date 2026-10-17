/*
 * Decompiled with CFR 0.152.
 */
package dev.G255.Lumi.mod.modules.impl.render;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.Render3DEvent;
import dev.G255.Lumi.api.utils.math.Animation;
import dev.G255.Lumi.api.utils.math.Easing;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.settings.impl.EnumSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;

public class Zoom
extends Module {
    public static Zoom INSTANCE;
    public static boolean on;
    public final EnumSetting<Easing> ease = this.add(new EnumSetting<Easing>("Ease", Easing.CubicInOut));
    final SliderSetting fov = this.add(new SliderSetting("ZoomFov", 60.0, 0.0, 130.0, 1.0));
    final Animation animation = new Animation();
    private final SliderSetting animTime = this.add(new SliderSetting("AnimTime", 300, 0, 1000));
    public double currentFov;
    private final ZoomAnim zoomAnim;

    public Zoom() {
        super("Zoom", Module.Category.Render);
        this.setChinese("\u653e\u5927");
        INSTANCE = this;
        this.zoomAnim = new ZoomAnim();
        Lumi.EVENT_BUS.subscribe(this.zoomAnim);
    }

    @Override
    public void onDisable() {
        Lumi.EVENT_BUS.unsubscribe(this.zoomAnim);
    }

    @Override
    public void onEnable() {
        if (Zoom.nullCheck()) {
            this.disable();
        }
    }

    static {
        on = false;
    }

    public class ZoomAnim {
        @EventListener
        public void onRender3D(Render3DEvent event) {
            if (Zoom.this.isOn()) {
                Zoom.this.currentFov = Zoom.this.animation.get(Zoom.this.fov.getValue(), Zoom.this.animTime.getValueInt(), Zoom.this.ease.getValue());
                on = true;
            } else if (on) {
                Zoom.this.currentFov = Zoom.this.animation.get(0.0, Zoom.this.animTime.getValueInt(), Zoom.this.ease.getValue());
                if ((int)Zoom.this.currentFov == 0) {
                    on = false;
                }
            }
        }
    }
}
