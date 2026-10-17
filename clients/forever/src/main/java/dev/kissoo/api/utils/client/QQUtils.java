package dev.kissoo.api.utils.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QQUtils {
    private static final Pattern QQ_PATTERN = Pattern.compile("^[1-9]\\d{4,11}$");
    private static String cachedQQ;

    private QQUtils() {
    }

    public static String getQQ() {
        if (cachedQQ != null) return cachedQQ.isEmpty() ? null : cachedQQ;
        cachedQQ = detectQQ();
        return cachedQQ.isEmpty() ? null : cachedQQ;
    }

    public static List<String> getAllQQ() {
        Set<String> qqs = new LinkedHashSet<>();
        if (!isWindows()) return new ArrayList<>(qqs);

        String appdata = System.getenv("APPDATA");
        if (appdata != null) {
            scanDirectory(Paths.get(appdata, "Tencent", "Users"), qqs, false);
        }

        Set<Path> docPaths = getDocumentPaths();
        for (Path doc : docPaths) {
            scanDirectory(doc.resolve("Tencent Files"), qqs, false);
            scanDirectory(doc.resolve("Tencent Files (64 Bit)"), qqs, false);
            Path ntLogin = doc.resolve("Tencent Files")
                    .resolve("nt_qq")
                    .resolve("global")
                    .resolve("nt_data")
                    .resolve("Login");
            scanDirectory(ntLogin, qqs, true);
        }

        Path publicDoc = Paths.get(
                System.getenv("PUBLIC") != null ? System.getenv("PUBLIC") : "C:\\Users\\Public",
                "Documents",
                "Tencent",
                "QQ",
                "UserDataInfo.ini"
        );
        scanCustomPath(publicDoc, qqs);

        for (String fromReg : queryRegistry()) {
            qqs.add(fromReg);
        }

        return new ArrayList<>(qqs);
    }

    public static void refresh() {
        cachedQQ = null;
    }

    private static String detectQQ() {
        if (!isWindows()) return "";

        String appdata = System.getenv("APPDATA");
        if (appdata != null) {
            Set<String> qqs = new HashSet<>();
            scanDirectory(Paths.get(appdata, "Tencent", "Users"), qqs, false);
            if (!qqs.isEmpty()) return qqs.iterator().next();
        }

        Set<Path> docPaths = getDocumentPaths();
        for (Path doc : docPaths) {
            Set<String> qqs = new HashSet<>();

            Path ntLogin = doc.resolve("Tencent Files")
                    .resolve("nt_qq")
                    .resolve("global")
                    .resolve("nt_data")
                    .resolve("Login");
            scanDirectory(ntLogin, qqs, true);
            if (!qqs.isEmpty()) return qqs.iterator().next();

            scanDirectory(doc.resolve("Tencent Files"), qqs, false);
            if (!qqs.isEmpty()) return qqs.iterator().next();

            scanDirectory(doc.resolve("Tencent Files (64 Bit)"), qqs, false);
            if (!qqs.isEmpty()) return qqs.iterator().next();
        }

        Path publicDoc = Paths.get(
                System.getenv("PUBLIC") != null ? System.getenv("PUBLIC") : "C:\\Users\\Public",
                "Documents",
                "Tencent",
                "QQ",
                "UserDataInfo.ini"
        );
        Set<String> qqs = new HashSet<>();
        scanCustomPath(publicDoc, qqs);
        if (!qqs.isEmpty()) return qqs.iterator().next();

        for (String reg : queryRegistry()) {
            if (isValidQQ(reg)) return reg;
        }

        return "";
    }

    private static void scanDirectory(Path path, Set<String> qqs, boolean isNtQQ) {
        if (!Files.isDirectory(path)) return;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (!Files.isDirectory(entry)) continue;
                String name = entry.getFileName().toString();
                if (isNtQQ) {
                    String numbers = name.replaceAll("[^0-9]", "");
                    if (numbers.length() >= 5 && numbers.length() <= 12 && isValidQQ(numbers)) {
                        qqs.add(numbers);
                    }
                } else if (isValidQQ(name)) {
                    qqs.add(name.trim());
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void scanCustomPath(Path iniPath, Set<String> qqs) {
        if (!Files.isRegularFile(iniPath)) return;
        try (BufferedReader reader = Files.newBufferedReader(iniPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2 && "UserDataSavePath".equals(parts[0].trim())) {
                    scanDirectory(Paths.get(parts[1].trim()), qqs, false);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static Set<Path> getDocumentPaths() {
        Set<Path> paths = new HashSet<>();
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            paths.add(Paths.get(userHome, "Documents"));
        }
        String publicDocs = System.getenv("PUBLIC");
        if (publicDocs != null) {
            paths.add(Paths.get(publicDocs, "Documents"));
        }
        String oneDrive = System.getenv("OneDrive");
        if (oneDrive != null) {
            paths.add(Paths.get(oneDrive));
        }
        String oneDriveConsumer = System.getenv("OneDriveConsumer");
        if (oneDriveConsumer != null) {
            paths.add(Paths.get(oneDriveConsumer));
        }
        return paths;
    }

    private static List<String> queryRegistry() {
        List<String> result = new ArrayList<>();
        String[] keys = {
                "HKCU\\Software\\Tencent\\QQ",
                "HKCU\\Software\\Tencent\\QQNT",
                "HKLM\\Software\\Tencent\\QQ",
                "HKCU\\Software\\WOW6432Node\\Tencent\\QQ"
        };
        String[] values = {"LastUin", "UserID", "QQ", "Account", "LastLoginUin"};
        for (String key : keys) {
            for (String val : values) {
                String r = regQuery(key, val);
                if (isValidQQ(r) && !result.contains(r)) result.add(r);
            }
        }
        return result;
    }

    private static String regQuery(String key, String value) {
        try {
            ProcessBuilder pb = new ProcessBuilder("reg", "query", key, "/v", value);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line).append('\n');
                output = sb.toString();
            }
            p.waitFor();
            Matcher m = Pattern.compile(value + "\\s+REG_\\w+\\s+(\\S+)").matcher(output);
            if (m.find()) {
                String raw = m.group(1).trim();
                if (isValidQQ(raw)) return raw;
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static boolean isValidQQ(String s) {
        return s != null && QQ_PATTERN.matcher(s.trim()).matches();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
    }
}
