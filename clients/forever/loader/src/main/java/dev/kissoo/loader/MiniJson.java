package dev.kissoo.loader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MiniJson {
    private MiniJson() {
    }

    static Object parse(String text) {
        return new Parser(text == null ? "" : text).parse();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> obj(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of();
    }

    @SuppressWarnings("unchecked")
    static List<Object> arr(Object value) {
        return value instanceof List<?> ? (List<Object>) value : List.of();
    }

    static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    static Object get(Map<String, Object> map, String... path) {
        Object cur = map;
        for (String p : path) {
            if (!(cur instanceof Map<?, ?> m)) return null;
            cur = m.get(p);
        }
        return cur;
    }

    private static final class Parser {
        private final String s;
        private int i;

        private Parser(String s) {
            this.s = s;
        }

        private Object parse() {
            ws();
            Object v = value();
            ws();
            return v;
        }

        private Object value() {
            ws();
            if (i >= s.length()) return null;
            char c = s.charAt(i);
            if (c == '"') return string();
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == 't' && s.startsWith("true", i)) {
                i += 4;
                return Boolean.TRUE;
            }
            if (c == 'f' && s.startsWith("false", i)) {
                i += 5;
                return Boolean.FALSE;
            }
            if (c == 'n' && s.startsWith("null", i)) {
                i += 4;
                return null;
            }
            return number();
        }

        private Map<String, Object> object() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            i++;
            ws();
            if (eat('}')) return out;
            while (i < s.length()) {
                String k = string();
                ws();
                eat(':');
                Object v = value();
                out.put(k, v);
                ws();
                if (eat('}')) break;
                eat(',');
            }
            return out;
        }

        private List<Object> array() {
            ArrayList<Object> out = new ArrayList<>();
            i++;
            ws();
            if (eat(']')) return out;
            while (i < s.length()) {
                out.add(value());
                ws();
                if (eat(']')) break;
                eat(',');
            }
            return out;
        }

        private String string() {
            StringBuilder sb = new StringBuilder();
            if (!eat('"')) return "";
            boolean esc = false;
            while (i < s.length()) {
                char c = s.charAt(i++);
                if (esc) {
                    switch (c) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (i + 4 <= s.length()) {
                                try {
                                    sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                                } catch (Throwable ignored) {
                                }
                                i += 4;
                            }
                        }
                        default -> sb.append(c);
                    }
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

        private Object number() {
            int start = i;
            while (i < s.length()) {
                char c = s.charAt(i);
                if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                    i++;
                } else {
                    break;
                }
            }
            String n = s.substring(start, i);
            try {
                return n.contains(".") || n.contains("e") || n.contains("E") ? Double.parseDouble(n) : Long.parseLong(n);
            } catch (Throwable ignored) {
                return 0L;
            }
        }

        private boolean eat(char c) {
            ws();
            if (i < s.length() && s.charAt(i) == c) {
                i++;
                return true;
            }
            return false;
        }

        private void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }
    }
}
