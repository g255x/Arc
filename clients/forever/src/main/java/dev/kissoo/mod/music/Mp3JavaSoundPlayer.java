package dev.kissoo.mod.music;

import javazoom.jl.decoder.*;

import javax.sound.sampled.*;
import java.io.InputStream;

public final class Mp3JavaSoundPlayer {
    private final Decoder decoder = new Decoder();
    private final Bitstream bitstream;
    private volatile boolean playing;
    private volatile float volume01 = 0.8f;
    private final long startAtMs;
    private volatile long playedMs;

    private SourceDataLine line;
    private FloatControl gainControl;

    public Mp3JavaSoundPlayer(InputStream stream) {
        this(stream, 0);
    }

    public Mp3JavaSoundPlayer(InputStream stream, long startAtMs) {
        this.bitstream = new Bitstream(stream);
        this.startAtMs = Math.max(0, startAtMs);
    }

    public void setVolume01(float v) {
        volume01 = Math.max(0f, Math.min(1f, v));
        applyVolume();
    }

    public void stop() {
        playing = false;
        try {
            if (line != null) line.stop();
        } catch (Throwable ignored) {
        }
    }

    public long getPlayedMs() {
        return playedMs;
    }

    public void play() throws Exception {
        playing = true;

        byte[] pcmOut = new byte[1152 * 4]; // enough for one MP3 frame typical
        long ms = 0;

        while (playing) {
            Header header;
            try {
                header = bitstream.readFrame();
            } catch (BitstreamException e) {
                break;
            }
            if (header == null) break;

            SampleBuffer output;
            try {
                output = (SampleBuffer) decoder.decodeFrame(header, bitstream);
            } catch (DecoderException e) {
                break;
            }

            long frameMs = Math.max(1, (long) header.ms_per_frame());
            // Seek by decoding+discarding until we reach target time.
            if (ms + frameMs < startAtMs) {
                ms += frameMs;
                playedMs = ms;
                bitstream.closeFrame();
                continue;
            }

            if (line == null) {
                openLine(output.getSampleFrequency(), output.getChannelCount());
                applyVolume();
            }

            short[] samples = output.getBuffer();
            int sampleCount = output.getBufferLength();

            int bytesNeeded = sampleCount * 2;
            if (pcmOut.length < bytesNeeded) {
                pcmOut = new byte[Math.max(bytesNeeded, pcmOut.length * 2)];
            }

            // little-endian 16-bit PCM
            int bi = 0;
            for (int i = 0; i < sampleCount; i++) {
                short s = samples[i];
                pcmOut[bi++] = (byte) (s & 0xFF);
                pcmOut[bi++] = (byte) ((s >> 8) & 0xFF);
            }

            line.write(pcmOut, 0, bytesNeeded);
            ms += frameMs;
            playedMs = ms;
            bitstream.closeFrame();
        }

        close();
    }

    private void openLine(int sampleRate, int channels) throws LineUnavailableException {
        AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format, sampleRate);
        line.start();
        if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        }
    }

    private void applyVolume() {
        if (gainControl == null) return;
        float v = Math.max(0.0001f, volume01);
        float dB = (float) (20.0 * Math.log10(v));
        dB = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB));
        try {
            gainControl.setValue(dB);
        } catch (Throwable ignored) {
        }
    }

    public void close() {
        try {
            bitstream.close();
        } catch (Throwable ignored) {
        }
        try {
            if (line != null) {
                line.drain();
                line.stop();
                line.close();
            }
        } catch (Throwable ignored) {
        }
        line = null;
        gainControl = null;
        playing = false;
    }
}

