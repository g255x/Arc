package dev.kissoo.loader;

import java.util.Locale;
import java.util.prefs.Preferences;

final class LoaderPreferences {
    enum Language {
        EN, ZH
    }

    enum Theme {
        DARK, LIGHT
    }

    private static final Preferences PREFS = Preferences.userNodeForPackage(LoaderPreferences.class);
    private static Language language = readLanguage();
    private static Theme theme = readTheme();

    private LoaderPreferences() {
    }

    static Language language() {
        return language;
    }

    static boolean chinese() {
        return language == Language.ZH;
    }

    static void toggleLanguage() {
        language = language == Language.ZH ? Language.EN : Language.ZH;
        PREFS.put("language", language.name().toLowerCase(Locale.ROOT));
    }

    static Theme theme() {
        return theme;
    }

    static void toggleTheme() {
        theme = theme == Theme.LIGHT ? Theme.DARK : Theme.LIGHT;
        PREFS.put("theme", theme.name().toLowerCase(Locale.ROOT));
        Palette.apply(theme);
    }

    private static Language readLanguage() {
        String value = PREFS.get("language", "").trim().toLowerCase(Locale.ROOT);
        if ("zh".equals(value)) return Language.ZH;
        if ("en".equals(value)) return Language.EN;
        return Locale.getDefault().getLanguage().toLowerCase(Locale.ROOT).startsWith("zh") ? Language.ZH : Language.EN;
    }

    private static Theme readTheme() {
        String value = PREFS.get("theme", "dark").trim().toLowerCase(Locale.ROOT);
        return "light".equals(value) ? Theme.LIGHT : Theme.DARK;
    }
}
