package dev.kissoo.loader;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

final class BrowserLauncher {
    private BrowserLauncher() {
    }

    static boolean open(String value) {
        try {
            URI uri = URI.create(value == null ? "" : value.trim());
            String scheme = uri.getScheme();
            if (!("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))) return false;
            String url = uri.toASCIIString();
            if (isWindows() && openWithUrlHandler(url)) return true;
            if (isWindows() && openWithStartCommand(url)) return true;
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
                return true;
            }
        } catch (Throwable ignored) {
        }
        copy(value);
        return false;
    }

    private static boolean openWithUrlHandler(String url) {
        try {
            String windows = System.getenv("WINDIR");
            Path rundll32 = Path.of(windows == null || windows.isBlank() ? "C:\\Windows" : windows, "System32", "rundll32.exe");
            new ProcessBuilder(Files.isRegularFile(rundll32) ? rundll32.toString() : "rundll32.exe",
                    "url.dll,FileProtocolHandler", url).start();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean openWithStartCommand(String url) {
        try {
            new ProcessBuilder("cmd.exe", "/c", "start", "\"\"", url).start();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void copy(String value) {
        try {
            if (value != null && !value.isBlank()) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value), null);
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
