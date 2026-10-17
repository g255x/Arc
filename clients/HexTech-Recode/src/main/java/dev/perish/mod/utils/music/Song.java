package dev.perish.mod.utils.music;

public class Song {
    public final String image;
    public final String name;
    public final String singer;
    public final long id;
    public final long duration;

    public Song(String image, String name, String singer, long id, long duration) {
        this.image = image;
        this.name = name;
        this.singer = singer;
        this.id = id;
        this.duration = duration;
    }
}
