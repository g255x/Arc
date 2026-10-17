package dev.kissoo.api.utils.notebot.decoder;

import dev.kissoo.api.utils.notebot.song.Note;
import dev.kissoo.api.utils.notebot.song.Song;
import net.minecraft.block.enums.NoteBlockInstrument;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple text song format: one note per line, "tick:level[:instrumentOrdinal]".
 * Lines starting with "// Name: " set the title. Blank lines are skipped.
 */
public class TextSongDecoder extends SongDecoder {

    @Override
    public Song parse(File file) throws Exception {
        List<String> lines = Files.readAllLines(file.toPath());
        Map<Integer, List<Note>> notesMap = new HashMap<>();
        String title = file.getName();

        for (String line : lines) {
            if (line.startsWith("// Name: ")) {
                title = line.substring(9);
                continue;
            }
            if (line.isEmpty() || line.startsWith("//")) continue;

            String[] parts = line.split(":");
            if (parts.length < 2) continue;

            try {
                int tick = Integer.parseInt(parts[0].trim());
                int level = Integer.parseInt(parts[1].trim());
                int type = parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 0;

                NoteBlockInstrument instrument = NoteBlockInstrument.values()[type];
                notesMap.computeIfAbsent(tick, k -> new java.util.ArrayList<>()).add(new Note(instrument, level));
            } catch (NumberFormatException ignored) {
                // malformed line, skip
            }
        }

        return new Song(notesMap, title);
    }
}
