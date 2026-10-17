package dev.kissoo.loader;

import java.util.List;

final class JsonUtil {
    private JsonUtil() {
    }

    static String esc(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    static String array(List<String> values) {
        if (values == null || values.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (String v : values) {
            if (v == null || v.isBlank()) continue;
            if (!first) sb.append(',');
            sb.append('"').append(esc(v)).append('"');
            first = false;
        }
        return sb.append(']').toString();
    }

    static String field(String json, String key) {
        if (json == null || key == null) return "";
        String marker = "\"" + key + "\"";
        int idx = json.indexOf(marker);
        if (idx < 0) return "";
        int colon = json.indexOf(':', idx + marker.length());
        if (colon < 0) return "";
        int start = json.indexOf('"', colon + 1);
        if (start < 0) {
            int p = colon + 1;
            while (p < json.length() && Character.isWhitespace(json.charAt(p))) p++;
            int e = p;
            while (e < json.length() && "0123456789".indexOf(json.charAt(e)) >= 0) e++;
            return e > p ? json.substring(p, e) : "";
        }
        StringBuilder sb = new StringBuilder();
        boolean esc = false;
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (esc) {
                sb.append(c == 'n' ? '\n' : c == 'r' ? '\r' : c == 't' ? '\t' : c);
                esc = false;
            } else if (c == '\\') {
                esc = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
