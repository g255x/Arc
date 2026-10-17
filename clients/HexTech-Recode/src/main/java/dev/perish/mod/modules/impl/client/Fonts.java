package dev.perish.mod.modules.impl.client;

import dev.perish.core.impl.FontManager;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.EnumSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;

public class Fonts extends Module {
    public static Fonts INSTANCE;
    public final EnumSetting<FontType> font = this.add(new EnumSetting<>("Font", FontType.Font).injectTask(this::refresh));
    public final EnumSetting<FontType> alternate = this.add(new EnumSetting<>("Alternate", FontType.MicrosoftYaHei).injectTask(this::refresh));
    public final EnumSetting<Fonts.Style> style = this.add(new EnumSetting("Style", Fonts.Style.PLAIN));
    public final SliderSetting size = this.add(new SliderSetting("Size", 8.0, 1.0, 15.0, 1.0));
    public final SliderSetting shift = this.add(new SliderSetting("Shift", 0.0, -10.0, 10.0, 1.0));
    public final SliderSetting translate = this.add(new SliderSetting("Translate", 0.0, -10.0, 10.0, 1.0));

    public Fonts() {
        super("Fonts", Module.Category.Client);
        this.setChinese("字体");
        INSTANCE = this;
    }

    @Override
    public void enable() {
        this.refresh();
    }

    public void refresh() {
        try {
            String fontName = this.font.getValue().fontName;
            String alternateName = this.alternate.getValue().fontName;
            boolean isAssetFont = fontName.equals("default") || fontName.equals("font") || fontName.equalsIgnoreCase("LexendDeca-Regular");
            if (isAssetFont) {
                if (alternateName.equals("null")) {
                    FontManager.ui = FontManager.assets(this.size.getValueInt(), fontName, this.style.getValue().get());
                    FontManager.small = FontManager.assets(6.0F, fontName, this.style.getValue().get());
                } else {
                    FontManager.ui = FontManager.assets(
                            this.size.getValueInt(), fontName, this.style.getValue().get(), alternateName
                    );
                    FontManager.small = FontManager.assets(6.0F, fontName, this.style.getValue().get(), alternateName);
                }
            } else if (alternateName.equals("null")) {
                FontManager.ui = FontManager.create(this.size.getValueInt(), fontName, this.style.getValue().get());
                FontManager.small = FontManager.create(6, fontName, this.style.getValue().get());
            } else {
                FontManager.ui = FontManager.create(
                        this.size.getValueInt(), fontName, this.style.getValue().get(), alternateName
                );
                FontManager.small = FontManager.create(6, fontName, this.style.getValue().get(), alternateName);
            }
        } catch (Exception var2) {
            var2.printStackTrace();
        }
    }

    public enum Style {
        PLAIN {
            @Override
            public int get() {
                return 0;
            }
        },
        BOLD {
            @Override
            public int get() {
                return 1;
            }
        },
        ITALIC {
            @Override
            public int get() {
                return 2;
            }
        };

        public abstract int get();
    }

    public enum FontType {
        Default("font"),
        Font("font"),
        LexendDeca("LexendDeca-Regular"),
        Arial("arial"),
        Bahnschrift("bahnschrift"),
        Calibri("calibri"),
        Consolas("consola"),
        MicrosoftYaHei("msyh"),
        SegoeUI("segoeui"),
        SimHei("simhei"),
        SimSun("simsun"),
        Tahoma("tahoma"),
        Verdana("verdana"),
        None("null");

        private final String fontName;

        FontType(String fontName) {
            this.fontName = fontName;
        }
    }
}
