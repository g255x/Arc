package dev.perish.mod.modules.impl.combat;

import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.SliderSetting;

public class IQBoost extends Module {

    private final SliderSetting iq = add(new SliderSetting("IQ", 114514, 0, 114514, 1));

    public IQBoost() {
        super("IQBoost", Category.Combat);
        setChinese("IQ提升");
    }

    @Override
    public String getInfo() {
        return String.valueOf(iq.getValueInt());
    }
}
