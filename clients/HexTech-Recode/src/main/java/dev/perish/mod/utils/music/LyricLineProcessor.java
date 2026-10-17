package dev.perish.mod.utils.music;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricLineProcessor {
    private static final Pattern LRC_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)");

    public static List<LyricLine> parseLyricLine(String text) {
        List<LyricLine> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        for (String rawLine : text.split("\n")) {
            Matcher matcher = LRC_PATTERN.matcher(rawLine.trim());
            if (matcher.find()) {
                try {
                    int min = Integer.parseInt(matcher.group(1));
                    int sec = Integer.parseInt(matcher.group(2));
                    String msStr = matcher.group(3);
                    long ms = msStr.length() == 2 ? Long.parseLong(msStr) * 10 : Long.parseLong(msStr);
                    long timeMs = min * 60L * 1000L + sec * 1000L + ms;
                    String lyricText = matcher.group(4).trim();
                    lines.add(new LyricLine(lyricText, timeMs));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        lines.sort(Comparator.comparingLong(l -> l.timeMs));
        return lines;
    }

    public static int findCurrentIndex(List<LyricLine> lyrics, long timeMs) {
        if (lyrics == null || lyrics.isEmpty()) return -1;
        int index = -1;
        for (int i = 0; i < lyrics.size(); i++) {
            if (lyrics.get(i).timeMs <= timeMs) {
                index = i;
            } else {
                break;
            }
        }
        return index;
    }
}
