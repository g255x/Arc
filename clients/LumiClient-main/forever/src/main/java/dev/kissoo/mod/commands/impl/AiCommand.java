package dev.kissoo.mod.commands.impl;

import dev.kissoo.auth.KeyAuth;
import dev.kissoo.core.Manager;
import dev.kissoo.core.impl.CommandManager;
import dev.kissoo.mod.commands.Command;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class AiCommand extends Command {

    private static final int    TIMEOUT_MS   = 30_000;
    private static final int    MSG_ID       = 0xA1_C0_DE;
    /** 发给 Claude 的上下文窗口（最近 N 轮） */
    private static final int    SEND_ROUNDS  = 5;

    /** 全量历史（内存），持久化到文件 */
    private static final Deque<String[]> history = new ArrayDeque<>();
    private static final String HISTORY_FILE = "ai-history.json";

    public AiCommand() {
        super("ai", "<消息> | clear");
        loadHistory();
    }

    @Override
    public void runCommand(String[] parameters) {
        if (parameters.length == 0) { sendUsage(); return; }

        if (parameters.length == 1 && parameters[0].equalsIgnoreCase("clear")) {
            synchronized (history) { history.clear(); }
            saveHistory();
            sendChatMessage("§7[§dAI§7] §a对话历史已清除");
            return;
        }

        String message = String.join(" ", parameters);
        CommandManager.sendMessageId("§7[§dAI§7] §8思考中...", MSG_ID);

        String token = "token";
        String hwid  = "hwid";
        if (token == null || token.isEmpty()) {
            CommandManager.sendMessageId("§7[§dAI§7] §c请先登录", MSG_ID);
            return;
        }

        // 只把最近 SEND_ROUNDS 轮发给服务端（减少 token 消耗），但本地保存全部
        String historyJson = buildRecentHistoryJson(SEND_ROUNDS);

        Thread t = new Thread(() -> {
            try {
                String body = "{\"token\":\"" + esc(token) + "\","
                        + "\"hwid\":\""    + esc(hwid)    + "\","
                        + "\"message\":\"" + esc(message) + "\","
                        + "\"history\":"   + historyJson  + "}";

                String url = "https://52.175.50.86/api/index.php?route=/ai-chat";

                HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                conn.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream out = conn.getOutputStream()) { out.write(bytes); }

                int code = conn.getResponseCode();
                InputStream in = (code >= 400) ? conn.getErrorStream() : conn.getInputStream();
                String resp = in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
                conn.disconnect();

                if (code == 200) {
                    String reply = extractString(resp, "reply");
                    if (reply != null && !reply.isEmpty()) {
                        synchronized (history) {
                            history.addLast(new String[]{"user",      message});
                            history.addLast(new String[]{"assistant", reply});
                            List<String[]> compacted = compactHistory(new ArrayList<>(history));
                            history.clear();
                            for (String[] entry : compacted) history.addLast(entry);
                        }
                        saveHistory();
                        CommandManager.sendMessageId("§7[§dAI§7] §f" + reply, MSG_ID);
                    } else {
                        CommandManager.sendMessageId("§7[§dAI§7] §c返回内容为空", MSG_ID);
                    }
                } else {
                    String err = extractString(resp, "error");
                    CommandManager.sendMessageId("§7[§dAI§7] §c" + (err != null ? err : "HTTP " + code), MSG_ID);
                }
            } catch (Exception e) {
                CommandManager.sendMessageId("§7[§dAI§7] §c请求失败: " + e.getMessage(), MSG_ID);
            }
        }, "Forever-AiCommand");
        t.setDaemon(true);
        t.start();
    }

    // ── 持久化 ────────────────────────────────────────────────────────────────

    private static void saveHistory() {
        try {
            File file = Manager.getFile(HISTORY_FILE);
            List<String[]> snapshot;
            synchronized (history) { snapshot = new ArrayList<>(history); }
            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < snapshot.size(); i++) {
                String[] e = snapshot.get(i);
                sb.append("  {\"role\":\"").append(esc(e[0]))
                  .append("\",\"content\":\"").append(esc(e[1])).append("\"}");
                if (i < snapshot.size() - 1) sb.append(',');
                sb.append('\n');
            }
            sb.append(']');
            Files.writeString(file.toPath(), sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }

    private static void loadHistory() {
        try {
            File file = Manager.getFile(HISTORY_FILE);
            if (!file.exists()) return;
            String raw = Files.readString(file.toPath(), StandardCharsets.UTF_8).trim();
            if (raw.isEmpty() || raw.charAt(0) != '[') return;

            List<String[]> loaded = parseHistoryJson(raw);
            synchronized (history) {
                history.clear();
                for (String[] e : compactHistory(loaded)) history.addLast(e);
            }
        } catch (Exception ignored) {}
    }

    /** 极简 JSON 数组解析：[{"role":"...","content":"..."},...] */
    private static List<String[]> parseHistoryJson(String json) {
        List<String[]> result = new ArrayList<>();
        int i = 0;
        while (i < json.length()) {
            int objStart = json.indexOf('{', i);
            if (objStart < 0) break;
            int objEnd = json.indexOf('}', objStart);
            if (objEnd < 0) break;
            String obj = json.substring(objStart, objEnd + 1);
            String role    = extractString(obj, "role");
            String content = extractString(obj, "content");
            if (role != null && content != null
                    && (role.equals("user") || role.equals("assistant"))) {
                result.add(new String[]{role, content});
            }
            i = objEnd + 1;
        }
        return result;
    }

    // ── 工具方法 ──────────────────────────────────────────────────────────────

    /** 构建最近 rounds 轮（user+assistant 各算半轮）的历史 JSON */
    private static synchronized String buildRecentHistoryJson(int rounds) {
        List<String[]> all = compactHistory(new ArrayList<>(history));
        int start = Math.max(0, all.size() - rounds * 2);
        StringBuilder sb = new StringBuilder("[");
        for (int i = start; i < all.size(); i++) {
            if (i > start) sb.append(',');
            String[] e = all.get(i);
            sb.append("{\"role\":\"").append(esc(e[0]))
              .append("\",\"content\":\"").append(esc(e[1])).append("\"}");
        }
        return sb.append(']').toString();
    }

    private static List<String[]> compactHistory(List<String[]> input) {
        List<String[]> output = new ArrayList<>();
        String lastAssistantKey = "";
        for (String[] entry : input) {
            if (entry == null || entry.length < 2) continue;
            String role = entry[0];
            String content = entry[1];
            if (role == null || content == null || content.isBlank()) continue;
            if (!role.equals("user") && !role.equals("assistant")) continue;
            if (role.equals("assistant")) {
                String key = normalizeForRepeatCheck(content);
                if (!key.isEmpty() && key.equals(lastAssistantKey)) continue;
                lastAssistantKey = key;
            }
            output.add(new String[]{role, content});
        }
        return output;
    }

    private static String normalizeForRepeatCheck(String value) {
        if (value == null) return "";
        return value.replaceAll("\\s+", " ").trim();
    }

    private static String extractString(String json, String key) {
        if (json == null || key == null) return null;
        String marker = "\"" + key + "\"";
        int idx = json.indexOf(marker);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + marker.length());
        if (colon < 0) return null;
        int start = json.indexOf('"', colon + 1);
        if (start < 0) return null;
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    default  -> value.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                value.append(c);
            }
        }
        return value.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    @Override
    public String[] getAutocorrect(int count, List<String> separated) {
        return null;
    }
}
