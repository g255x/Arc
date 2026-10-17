package dev.perish.mod.modules.impl.client;

import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.EnumSetting;
import net.minecraft.util.Identifier;

public class CapeModule extends Module {
    public static CapeModule INSTANCE;
    private final EnumSetting<CapeType> cape = this.add(new EnumSetting<>("Texture", CapeType.e_2));

    public CapeModule() {
        super("Cape", Category.Client);
        this.setChinese("斗篷");
        INSTANCE = this;
    }

    public CapeType getSelectedCape() {
        return cape.getValue();
    }

    // 直接返回预定义的 Identifier
    public Identifier getSelectedTexture() {
        return getSelectedCape().getTexture();
    }

    public boolean isOn() {
        return super.isOn();
    }

    public enum CapeType {
        // 使用预定义的 Identifier，和 nami 一样
        e_2(Identifier.of("perish", "textures/cape/e_2.png")),
        MIO(Identifier.of("perish", "textures/cape/mio.png")),
        LiquidBounce(Identifier.of("perish", "textures/cape/liquidbounce.png")),
        LIWANGZI(Identifier.of("perish", "textures/cape/liwangzi.png")),
        Rise(Identifier.of("perish", "textures/cape/rise.png")),
        Vape_Lite(Identifier.of("perish", "textures/cape/vapelite.png")),
        Vape_V4(Identifier.of("perish", "textures/cape/vapev4.png")),
        Opal(Identifier.of("perish", "textures/cape/opal.png"));
        private final Identifier texture;

        CapeType(Identifier texture) {
            this.texture = texture;
        }

        public Identifier getTexture() {
            return texture;
        }

        // 添加显示名称
        public String getDisplayName() {
            switch (this) {
                case Vape_V4:
                    return "Vape_V4";
                case Vape_Lite:
                    return "Vape_Lite";
                case Rise:
                    return "Rise";
                case MIO:
                    return "MIO";
                case LiquidBounce:
                    return "LiquidBounce";
                case LIWANGZI:
                    return "礼王子";
                case Opal:
                    return "Opal";
                case e_2:
                default:
                    return "e_2";
            }
        }

        @Override
        public String toString() {
            return getDisplayName();
        }
    }
}