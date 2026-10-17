package dev.kissoo.api.utils.notebot.decoder;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SongDecoders {
    private static final Map<String, SongDecoder> decoders = new HashMap<>();

    static {
        decoders.put("nbs", new NBSSongDecoder());
        decoders.put("txt", new TextSongDecoder());
    }

    private static String extensionOf(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }

    public static boolean hasDecoder(File file) {
        return decoders.containsKey(extensionOf(file));
    }

    public static dev.kissoo.api.utils.notebot.song.Song parse(File file) throws Exception {
        SongDecoder decoder = decoders.get(extensionOf(file));
        if (decoder == null) throw new IllegalStateException("Decoder for this file does not exist!");

        var song = decoder.parse(file);
        song.finishLoading();
        return song;
    }
}
