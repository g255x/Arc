package dev.kissoo.mod.music;

import javazoom.jl.decoder.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

import java.io.InputStream;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Minimal MP3 streaming player using OpenAL queueing.
 * Adapted from CraftGR's AudioPlayer implementation.
 */
public final class Mp3OpenALPlayer {
    private final Decoder decoder = new Decoder();
    private final Bitstream bitstream;

    private IntBuffer source;
    private IntBuffer buffer;

    private float gain = 1.0f;
    private volatile boolean playing;

    public Mp3OpenALPlayer(InputStream stream) {
        this.bitstream = new Bitstream(stream);
    }

    public void play(boolean fadeIn) throws JavaLayerException {
        source = BufferUtils.createIntBuffer(1);
        AL10.alGenSources(source);
        alCheck();

        AL10.alSourcei(source.get(0), AL10.AL_LOOPING, AL10.AL_FALSE);
        AL10.alSourcef(source.get(0), AL10.AL_PITCH, 1.0f);
        applyGain(fadeIn ? 0.0f : 1.0f);
        alCheck();

        playing = true;
        long fadeStart = System.currentTimeMillis();

        while (playing) {
            if (fadeIn) {
                float p = Math.min(1.0f, (System.currentTimeMillis() - fadeStart) / 1200.0f);
                applyGain(p);
                if (p >= 1.0f) fadeIn = false;
            }

            try {
                if (!decodeAndQueueFrame()) break;
            } catch (JavaLayerException e) {
                close();
                throw e;
            }
        }

        close();
    }

    public void stop() {
        playing = false;
        if (source != null) {
            try {
                AL10.alSourceStop(source.get(0));
                alCheck();
            } catch (Throwable ignored) {}
        }
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setGain(float gain) {
        this.gain = Math.max(0.0f, Math.min(1.0f, gain));
        if (source != null) {
            applyGain(1.0f);
        }
    }

    private boolean decodeAndQueueFrame() throws BitstreamException, DecoderException {
        Header header;
        try {
            header = bitstream.readFrame();
        } catch (BitstreamException e) {
            if (e.getErrorCode() == BitstreamErrors.STREAM_ERROR
                    || e.getErrorCode() == BitstreamErrors.UNEXPECTED_EOF
                    || e.getErrorCode() == BitstreamErrors.STREAM_EOF) {
                return false;
            }
            throw e;
        }

        if (header == null) return false;

        SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);
        short[] samples = output.getBuffer();

        if (buffer == null) {
            buffer = BufferUtils.createIntBuffer(1);
        } else {
            int processed = AL10.alGetSourcei(source.get(0), AL10.AL_BUFFERS_PROCESSED);
            alCheck();
            while (processed-- > 0) {
                AL10.alSourceUnqueueBuffers(source.get(0), buffer);
            }
        }

        AL10.alGenBuffers(buffer);
        ShortBuffer shortBuffer = BufferUtils.createShortBuffer(output.getBufferLength()).put(samples, 0, output.getBufferLength());
        ShortBuffer data = (ShortBuffer) ((Buffer) shortBuffer).flip();
        int format = (output.getChannelCount() > 1) ? AL10.AL_FORMAT_STEREO16 : AL10.AL_FORMAT_MONO16;
        AL10.alBufferData(buffer.get(0), format, data, output.getSampleFrequency());
        AL10.alSourceQueueBuffers(source.get(0), buffer);
        alCheck();

        int state = AL10.alGetSourcei(source.get(0), AL10.AL_SOURCE_STATE);
        if (playing && state != AL10.AL_PLAYING) {
            AL10.alSourcePlay(source.get(0));
        }
        alCheck();

        bitstream.closeFrame();
        return true;
    }

    private void applyGain(float multiplier) {
        if (source == null) return;
        float m = Math.max(0.0f, Math.min(1.0f, multiplier));
        float value = (float) ((Math.exp(m * gain) - 1) / (Math.E - 1));
        AL10.alSourcef(source.get(0), AL10.AL_GAIN, value);
    }

    private void alCheck() {
        int err = AL10.alGetError();
        // Silent: avoid crashing the client UI; errors will just result in no audio.
        if (err != AL10.AL_NO_ERROR) {
            // no-op
        }
    }

    public void close() {
        try {
            if (source != null) {
                AL10.alSourceStop(source.get(0));
                AL10.alDeleteSources(source);
                source = null;
            }
            if (buffer != null) {
                AL10.alDeleteBuffers(buffer);
                buffer = null;
            }
        } catch (Throwable ignored) {
        }

        try {
            bitstream.close();
        } catch (Throwable ignored) {
        }
    }
}

