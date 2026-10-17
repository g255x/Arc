package dev.kissoo.mod.music;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LrcParser {
    private static final Pattern TAG = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?]");

    private LrcParser() {}

    public static List<LrcLine> parse(String lrc) {
        if (lrc == null || lrc.isBlank()) return List.of();

        ArrayList<LrcLine> out = new ArrayList<>();
        String[] lines = lrc.split("\\r?\\n");
        for (String raw : lines) {
            if (raw == null) continue;
            String line = raw.trim();
            if (line.isEmpty()) continue;

            Matcher m = TAG.matcher(line);
            int lastEnd = 0;
            ArrayList<Long> times = new ArrayList<>();
            while (m.find()) {
                lastEnd = m.end();
                int mm = Integer.parseInt(m.group(1));
                int ss = Integer.parseInt(m.group(2));
                String msStr = m.group(3);
                int ms = 0;
                if (msStr != null && !msStr.isEmpty()) {
                    if (msStr.length() == 1) ms = Integer.parseInt(msStr) * 100;
                    else if (msStr.length() == 2) ms = Integer.parseInt(msStr) * 10;
                    else ms = Integer.parseInt(msStr.substring(0, 3));
                }
                times.add(mm * 60_000L + ss * 1_000L + ms);
            }

            if (times.isEmpty()) continue;
            String text = lastEnd < line.length() ? line.substring(lastEnd).trim() : "";
            if (text.isEmpty()) text = " ";

            for (Long t : times) {
                out.add(new LrcLine(t, text));
            }
        }

        out.sort(Comparator.comparingLong(LrcLine::timeMs));
        return out;
    }

    public static int findLineIndex(List<LrcLine> lines, long timeMs) {
        if (lines == null || lines.isEmpty()) return -1;
        int lo = 0, hi = lines.size() - 1;
        int ans = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            long t = lines.get(mid).timeMs();
            if (t <= timeMs) {
                ans = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return ans;
    }
}

