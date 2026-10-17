package dev.kissoo.loader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

final class LocalEnvironmentGuard {
    private static final Map<String, String> PROCESS_RULES = buildRules();
    private static final int MAX_FINDINGS = 12;
    private static volatile GuardReport lastReport;

    private LocalEnvironmentGuard() {
    }

    static GuardReport inspect() {
        LinkedHashMap<String, GuardFinding> findings = new LinkedHashMap<>();
        if (!GuardMesh.crossCheck()) {
            add(findings, block("Integrity", "local guard mesh", "请重新下载 Loader，不要修改启动器文件。"));
        }
        inspectProcesses(findings);
        inspectVirtualMachineFiles(findings);
        GuardReport report = new GuardReport(List.copyOf(findings.values()));
        lastReport = report;
        return report;
    }

    static boolean networkTunnelDetected() {
        GuardReport report = lastReport;
        if (report == null) report = inspect();
        boolean processDetected = report.findings() != null && report.findings().stream()
                .anyMatch(f -> isNetworkNotice(f.category()));
        if (processDetected) return true;
        return hasText(System.getProperty("http.proxyHost"))
                || hasText(System.getProperty("https.proxyHost"))
                || hasText(System.getenv("HTTP_PROXY"))
                || hasText(System.getenv("HTTPS_PROXY"))
                || hasText(System.getenv("ALL_PROXY"));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static void inspectProcesses(Map<String, GuardFinding> findings) {
        try {
            ProcessHandle.allProcesses().forEach(handle -> {
                if (findings.size() >= MAX_FINDINGS) return;
                ProcessHandle.Info info = handle.info();
                String raw = (info.command().orElse("") + " " + info.commandLine().orElse("")).toLowerCase(Locale.ROOT);
                String normalized = raw.replace('\\', '/');
                if (normalized.isBlank()) return;
                for (Map.Entry<String, String> rule : PROCESS_RULES.entrySet()) {
                    if (!matches(normalized, rule.getKey())) continue;
                    add(findings, findingForRule(rule.getValue(), displayName(normalized)));
                    break;
                }
            });
        } catch (Throwable ignored) {
        }
        if (isWindows()) inspectTaskList(findings);
    }

    private static void inspectTaskList(Map<String, GuardFinding> findings) {
        try {
            Process p = new ProcessBuilder("tasklist", "/fo", "csv", "/nh").redirectErrorStream(true).start();
            String text = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
            p.waitFor();
            for (String line : text.split("\\R")) {
                if (findings.size() >= MAX_FINDINGS) return;
                for (Map.Entry<String, String> rule : PROCESS_RULES.entrySet()) {
                    if (!matches(line, rule.getKey())) continue;
                    add(findings, findingForRule(rule.getValue(), line.replace("\"", "").split(",", 2)[0]));
                    break;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void inspectVirtualMachineFiles(Map<String, GuardFinding> findings) {
        if (!isWindows()) return;
        String windir = System.getenv("WINDIR");
        Path drivers = java.nio.file.Paths.get(windir == null ? "C:\\Windows" : windir, "System32", "drivers");
        String[] vmDrivers = {"vmmouse.sys", "vmhgfs.sys", "vm3dmp.sys", "VBoxMouse.sys", "VBoxGuest.sys", "VBoxSF.sys", "qemu-ga.sys", "prl_fs.sys"};
        for (String driver : vmDrivers) {
            if (Files.exists(drivers.resolve(driver))) {
                add(findings, block("Virtual machine", driver, "请在真实本机环境启动。"));
                if (findings.size() >= MAX_FINDINGS) return;
            }
        }
    }

    private static GuardFinding findingForRule(String category, String name) {
        if (isNetworkNotice(category)) {
            return warn(category, name, "已检测到 VPN/代理/加速器。不会阻止启动，但频繁切换节点导致 IP 频繁变化，可能触发风控或误封。");
        }
        return block(category, name, "请关闭后重新打开 Loader。");
    }

    private static boolean isNetworkNotice(String category) {
        return "VPN / Proxy".equals(category) || "Network accelerator".equals(category);
    }

    private static GuardFinding block(String category, String name, String advice) {
        return new GuardFinding(category, name, advice, GuardSeverity.BLOCK);
    }

    private static GuardFinding warn(String category, String name, String advice) {
        return new GuardFinding(category, name, advice, GuardSeverity.WARN);
    }

    private static boolean matches(String raw, String needle) {
        String n = needle.toLowerCase(Locale.ROOT);
        if (n.length() <= 3) {
            return raw.matches(".*(^|[/\\\\\\s\"'])" + Pattern.quote(n) + "(\\.exe|[/\\\\\\s\"',]|$).*");
        }
        return raw.contains(n);
    }

    private static String displayName(String raw) {
        String[] parts = raw.split("\\s+");
        String best = parts.length == 0 ? raw : parts[0];
        int slash = Math.max(best.lastIndexOf('/'), best.lastIndexOf('\\'));
        return slash >= 0 ? best.substring(slash + 1) : best;
    }

    private static void add(Map<String, GuardFinding> findings, GuardFinding finding) {
        findings.putIfAbsent(finding.category() + ":" + finding.name(), finding);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static Map<String, String> buildRules() {
        LinkedHashMap<String, String> rules = new LinkedHashMap<>();
        for (String s : List.of("clash", "mihomo", "v2ray", "xray", "sing-box", "shadowsocks", "nekoray", "netch", "wireguard", "openvpn", "outline", "protonvpn", "nordvpn", "expressvpn", "surfshark", "tailscale", "zerotier", "hiddify", "tun2socks")) {
            rules.put(s, "VPN / Proxy");
        }
        for (String s : List.of("uu.exe", "uuaccelerator", "leigod", "xunyou", "qiyou", "biubiu", "golink", "迅游", "雷神", "奇游", "网易uu")) {
            rules.put(s, "Network accelerator");
        }
        for (String s : List.of("amidewin", "volumeid", "macshift", "tmactrl", "smac", "hwidspoofer", "hwid_spoofer", "serialchecker", "diskid", "dmiedit", "dmi_edit")) {
            rules.put(s, "HWID tool");
        }
        for (String s : List.of("vmtoolsd", "vmwaretray", "vmwareuser", "vboxservice", "vboxtray", "qemu-ga", "prl_tools", "prl_cc", "xenservice", "vmsrvc", "vmusrvc")) {
            rules.put(s, "Virtual machine");
        }
        return rules;
    }
}
