package dev.kissoo.loader;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class QQScanner {
    private static final Pattern QQ = Pattern.compile("^[1-9]\\d{4,11}$");
    private static volatile List<String> cached;

    private QQScanner() {
    }

    static List<String> all() {
        if (cached != null) return cached;
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win")) {
            String appdata = System.getenv("APPDATA");
            if (appdata != null) scan(java.nio.file.Paths.get(appdata, "Tencent", "Users"), out, false);
            for (Path doc : documentPaths()) {
                scan(doc.resolve("Tencent Files"), out, false);
                scan(doc.resolve("Tencent Files (64 Bit)"), out, false);
                scan(doc.resolve("Tencent Files").resolve("nt_qq").resolve("global").resolve("nt_data").resolve("Login"), out, true);
            }
            readPublicTencentIni(out);
            queryRegistry(out);
        }
        cached = List.copyOf(out);
        return cached;
    }

    private static void scan(Path dir, Set<String> qqs, boolean nt) {
        if (!Files.isDirectory(dir)) return;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                if (!Files.isDirectory(p)) continue;
                String name = p.getFileName().toString();
                String value = nt ? name.replaceAll("[^0-9]", "") : name;
                if (QQ.matcher(value).matches()) qqs.add(value);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Set<Path> documentPaths() {
        LinkedHashSet<Path> paths = new LinkedHashSet<>();
        String home = System.getProperty("user.home");
        if (home != null) paths.add(java.nio.file.Paths.get(home, "Documents"));
        String pub = System.getenv("PUBLIC");
        if (pub != null) paths.add(java.nio.file.Paths.get(pub, "Documents"));
        String one = System.getenv("OneDrive");
        if (one != null) paths.add(java.nio.file.Paths.get(one));
        String oneC = System.getenv("OneDriveConsumer");
        if (oneC != null) paths.add(java.nio.file.Paths.get(oneC));
        return paths;
    }

    private static void readPublicTencentIni(Set<String> qqs) {
        String pub = System.getenv("PUBLIC");
        Path ini = java.nio.file.Paths.get(pub == null ? "C:\\Users\\Public" : pub, "Documents", "Tencent", "QQ", "UserDataInfo.ini");
        if (!Files.isRegularFile(ini)) return;
        try (BufferedReader reader = Files.newBufferedReader(ini, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2 && parts[0].trim().equals("UserDataSavePath")) {
                    scan(java.nio.file.Paths.get(parts[1].trim()), qqs, false);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void queryRegistry(Set<String> qqs) {
        String[] keys = {"HKCU\\Software\\Tencent\\QQ", "HKCU\\Software\\Tencent\\QQNT", "HKLM\\Software\\Tencent\\QQ"};
        String[] values = {"LastUin", "UserID", "QQ", "Account", "LastLoginUin"};
        for (String key : keys) for (String value : values) {
            try {
                Process p = new ProcessBuilder("reg", "query", key, "/v", value).redirectErrorStream(true).start();
                String text = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                p.waitFor();
                Matcher m = Pattern.compile(value + "\\s+REG_\\w+\\s+(\\S+)").matcher(text);
                if (m.find() && QQ.matcher(m.group(1)).matches()) qqs.add(m.group(1));
            } catch (Throwable ignored) {
            }
        }
    }
}
