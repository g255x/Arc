package dev.kissoo.core.impl;

import java.awt.Color;

public interface ShaderParams {

    float getSpeed();
    Color getFillColor();
    float getMaxSample();
    float getDivider();
    float getRadius();
    float getSmoothness();
    float getAlpha();

    Color getPulseColor();
    float getPulseSpeed();

    Color getSmoke1();
    Color getSmoke2();
    Color getSmoke3();
    Color getSmoke4();
    float getStep();
    int getOctaves();

    static ShaderParams fromModule(dev.kissoo.mod.modules.impl.render.ShaderModule module) {
        return new ShaderParams() {
            @Override public float getSpeed()      { return module.speed.getValueFloat(); }
            @Override public Color getFillColor()  { return module.fill.getValue(); }
            @Override public float getMaxSample()  { return module.maxSample.getValueFloat(); }
            @Override public float getDivider()    { return module.divider.getValueFloat(); }
            @Override public float getRadius()     { return module.radius.getValueFloat(); }
            @Override public float getSmoothness() { return module.smoothness.getValueFloat(); }
            @Override public float getAlpha()      { return module.alpha.getValueFloat(); }
            @Override public Color getPulseColor() { return module.pulse.getValue(); }
            @Override public float getPulseSpeed() { return module.pulseSpeed.getValueFloat(); }
            @Override public Color getSmoke1()     { return module.smoke1.getValue(); }
            @Override public Color getSmoke2()     { return module.smoke2.getValue(); }
            @Override public Color getSmoke3()     { return module.smoke3.getValue(); }
            @Override public Color getSmoke4()     { return module.smoke4.getValue(); }
            @Override public float getStep()       { return module.step.getValueFloat(); }
            @Override public int getOctaves()      { return (int) module.octaves.getValue(); }
        };
    }

    /** 按分组读参数（Global 等价于无参版本） */
    static ShaderParams fromModule(
            dev.kissoo.mod.modules.impl.render.ShaderModule module,
            dev.kissoo.mod.modules.impl.render.ShaderModule.Group group
    ) {
        dev.kissoo.mod.modules.impl.render.ShaderModule.ShaderGroup g = module.getGroup(group);
        return new ShaderParams() {
            @Override public float getSpeed()      { return g.speed.getValueFloat(); }
            @Override public Color getFillColor()  { return g.color.getValue(); }
            @Override public float getMaxSample()  { return g.maxSample.getValueFloat(); }
            @Override public float getDivider()    { return g.divider.getValueFloat(); }
            @Override public float getRadius()     { return g.radius.getValueFloat(); }
            @Override public float getSmoothness() { return g.smoothness.getValueFloat(); }
            @Override public float getAlpha()      { return g.alpha.getValueFloat(); }
            @Override public Color getPulseColor() { return g.pulse.getValue(); }
            @Override public float getPulseSpeed() { return g.pulseSpeed.getValueFloat(); }
            @Override public Color getSmoke1()     { return g.smoke1.getValue(); }
            @Override public Color getSmoke2()     { return g.smoke2.getValue(); }
            @Override public Color getSmoke3()     { return g.smoke3.getValue(); }
            @Override public Color getSmoke4()     { return g.smoke4.getValue(); }
            @Override public float getStep()       { return g.step.getValueFloat(); }
            @Override public int getOctaves()      { return (int) g.octaves.getValue(); }
        };
    }
}
