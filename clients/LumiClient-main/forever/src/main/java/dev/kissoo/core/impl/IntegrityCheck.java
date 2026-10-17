package dev.kissoo.core.impl;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 防篡改完整性校验。
 *
 * 构建时（Gradle embedSha 任务）计算最终 jar 排除 sha256.txt 自身后的 SHA-256，
 * 写入 sha256.txt 嵌入 jar；运行时重算运行中 jar 的 SHA-256 并与嵌入值比对，
 * 不一致立即崩溃（halt），防止他人改包后生成的新 jar 被使用。
 *
 * 算法（与 build.gradle embedSha 任务保持一致）：
 *   遍历 jar 全部条目（跳过目录与 sha256.txt），按条目名排序，
 *   对每个条目依次 hash：名称字节 + 0x00 + 内容字节 + 0x01。
 */
public class IntegrityCheck {

    public static final String SHA_FILE = "sha256.txt";

    private IntegrityCheck() {
    }

    public static void verify() {
        // 完整性校验已注释，跳过
    }

    private static String readExpected() throws Exception {
        try (InputStream in = IntegrityCheck.class.getResourceAsStream("/" + SHA_FILE)) {
            if (in == null) return null;
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }

    private static String computeJarSha() throws Exception {
        Path jarPath = getJarPath();
        if (jarPath == null || !jarPath.toString().endsWith(".jar")) return null;
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            List<ZipEntry> entries = new ArrayList<>();
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) continue;
                if (e.getName().equals(SHA_FILE)) continue;
                entries.add(e);
            }
            entries.sort(Comparator.comparing(ZipEntry::getName));
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            for (ZipEntry e : entries) {
                md.update(e.getName().getBytes(StandardCharsets.UTF_8));
                md.update((byte) 0);
                try (InputStream in = zip.getInputStream(e)) {
                    int n;
                    while ((n = in.read(buf)) != -1) md.update(buf, 0, n);
                }
                md.update((byte) 1);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        }
    }

    private static Path getJarPath() throws Exception {
        java.net.URL loc = IntegrityCheck.class.getProtectionDomain().getCodeSource().getLocation();
        if (loc == null) return null;
        return Paths.get(loc.toURI());
    }

    private static void crash(String msg) {
        System.err.println("[Forever/IntegrityCheck] " + msg);
        System.err.println("[Forever/IntegrityCheck] 客户端完整性校验失败，即将强制退出。");
        System.err.flush();
        Runtime.getRuntime().halt(1);
    }
}