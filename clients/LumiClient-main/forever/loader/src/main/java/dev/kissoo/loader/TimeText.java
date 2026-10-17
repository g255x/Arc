package dev.kissoo.loader;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

final class TimeText {
    static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private TimeText() {
    }
}
