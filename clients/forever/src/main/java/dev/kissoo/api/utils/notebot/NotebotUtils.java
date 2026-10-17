package dev.kissoo.api.utils.notebot;

import dev.kissoo.api.utils.notebot.song.Note;
import net.minecraft.block.BlockState;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.enums.NoteBlockInstrument;

public class NotebotUtils {

    public static Note getNoteFromNoteBlock(BlockState noteBlock, NotebotMode mode) {
        NoteBlockInstrument instrument = null;
        int level = noteBlock.get(NoteBlock.NOTE);

        if (mode == NotebotMode.ExactInstruments) {
            instrument = noteBlock.get(NoteBlock.INSTRUMENT);
        }

        return new Note(instrument, level);
    }

    public static int calcNumberOfHits(int from, int to) {
        if (from > to) {
            return (25 - from) + to;
        } else {
            return to - from;
        }
    }

    public enum NotebotMode {
        AnyInstrument, ExactInstruments
    }
}
