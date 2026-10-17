package dev.perish.mod.utils.music;

public class Playlist {
    public final long id;
    public final String name;
    public final String coverUrl;
    public final long playCount;

    public Playlist(long id, String name, String coverUrl, long playCount) {
        this.id = id;
        this.name = name;
        this.coverUrl = coverUrl;
        this.playCount = playCount;
    }
}
