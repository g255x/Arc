package dev.kissoo.api.utils.notebot.decoder;

import dev.kissoo.api.utils.notebot.song.Note;
import dev.kissoo.api.utils.notebot.song.Song;
import net.minecraft.block.enums.NoteBlockInstrument;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses Note Block Studio (.nbs) files.
 * Format reference: https://opennbs.org/nbs
 */
public class NBSSongDecoder extends SongDecoder {
    public static final int NOTE_OFFSET = 33;

    @Override
    public Song parse(File songFile) throws Exception {
        return parse(new FileInputStream(songFile), songFile.getName());
    }

    private Song parse(InputStream inputStream, String fileName) throws Exception {
        Map<Integer, java.util.List<Note>> notesMap = new HashMap<>();

        DataInputStream in = new DataInputStream(inputStream);
        short length = readShort(in);
        int nbsVersion = 0;
        if (length == 0) {
            nbsVersion = in.readByte();
            in.readByte(); // first custom instrument
            if (nbsVersion >= 3) {
                length = readShort(in);
            }
        }

        readShort(in); // song height
        String title = readString(in);
        readString(in); // author
        readString(in); // original author
        readString(in); // description
        float speed = readShort(in) / 100f;
        in.readBoolean(); // auto-save
        in.readByte(); // auto-save duration
        in.readByte(); // time signature
        readInt(in); // minutes spent
        readInt(in); // left clicks
        readInt(in); // right clicks
        readInt(in); // blocks added
        readInt(in); // blocks removed
        readString(in); // .mid/.schematic file name
        if (nbsVersion >= 4) {
            in.readByte(); // loop on/off
            in.readByte(); // max loop count
            readShort(in); // loop start tick
        }

        Song song = new Song(notesMap, title == null || title.isEmpty() ? fileName : title);

        double tick = -1;
        while (true) {
            short jumpTicks = readShort(in);
            if (jumpTicks == 0) break;
            tick += jumpTicks * (20f / speed);

            while (true) {
                short jumpLayers = readShort(in);
                if (jumpLayers == 0) break;

                byte instrument = in.readByte();
                byte key = in.readByte();
                if (nbsVersion >= 4) {
                    in.readUnsignedByte(); // velocity
                    in.readUnsignedByte(); // panning
                    readShort(in); // pitch
                }

                NoteBlockInstrument inst = fromNBSInstrument(instrument);
                if (inst == null) continue;

                song.put((int) Math.round(tick), new Note(inst, key - NOTE_OFFSET));
            }
        }

        return song;
    }

    private static short readShort(DataInputStream in) throws IOException {
        int b1 = in.readUnsignedByte();
        int b2 = in.readUnsignedByte();
        return (short) (b1 + (b2 << 8));
    }

    private static int readInt(DataInputStream in) throws IOException {
        int b1 = in.readUnsignedByte();
        int b2 = in.readUnsignedByte();
        int b3 = in.readUnsignedByte();
        int b4 = in.readUnsignedByte();
        return b1 + (b2 << 8) + (b3 << 16) + (b4 << 24);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = readInt(in);
        if (length < 0) throw new EOFException("Length can't be negative! Length: " + length);
        if (length > in.available()) {
            throw new EOFException("Can't read string larger than buffer! Length: " + length + " Available: " + in.available());
        }

        StringBuilder sb = new StringBuilder(length);
        for (; length > 0; --length) {
            char c = (char) in.readByte();
            if (c == (char) 0x0D) c = ' ';
            sb.append(c);
        }
        return sb.toString();
    }

    private static NoteBlockInstrument fromNBSInstrument(int instrument) {
        return switch (instrument) {
            case 0 -> NoteBlockInstrument.HARP;
            case 1 -> NoteBlockInstrument.BASS;
            case 2 -> NoteBlockInstrument.BASEDRUM;
            case 3 -> NoteBlockInstrument.SNARE;
            case 4 -> NoteBlockInstrument.HAT;
            case 5 -> NoteBlockInstrument.GUITAR;
            case 6 -> NoteBlockInstrument.FLUTE;
            case 7 -> NoteBlockInstrument.BELL;
            case 8 -> NoteBlockInstrument.CHIME;
            case 9 -> NoteBlockInstrument.XYLOPHONE;
            case 10 -> NoteBlockInstrument.IRON_XYLOPHONE;
            case 11 -> NoteBlockInstrument.COW_BELL;
            case 12 -> NoteBlockInstrument.DIDGERIDOO;
            case 13 -> NoteBlockInstrument.BIT;
            case 14 -> NoteBlockInstrument.BANJO;
            case 15 -> NoteBlockInstrument.PLING;
            default -> null;
        };
    }
}
