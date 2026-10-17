package dev.kissoo.mod.music;

public record NeteaseSong(long id, String rawId, String name, String artists, String picUrl, long durationMs,
                          String source, String picId, String lyricId) {
    public NeteaseSong(long id, String name, String artists, String picUrl, long durationMs) {
        this(id, String.valueOf(id), name, artists, picUrl, durationMs, "netease", "", String.valueOf(id));
    }

    public NeteaseSong(long id, String name, String artists, String picUrl, long durationMs,
                       String source, String picId, String lyricId) {
        this(id, String.valueOf(id), name, artists, picUrl, durationMs, source, picId, lyricId);
    }
}

