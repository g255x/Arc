package dev.kissoo.api.utils.notebot.decoder;

import dev.kissoo.api.utils.notebot.song.Song;

import java.io.File;

public abstract class SongDecoder {
    public abstract Song parse(File file) throws Exception;
}
