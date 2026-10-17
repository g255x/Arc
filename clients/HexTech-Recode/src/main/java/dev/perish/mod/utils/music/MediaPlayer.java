package dev.perish.mod.utils.music;

import com.goxr3plus.streamplayer.enums.Status;
import com.goxr3plus.streamplayer.stream.StreamPlayer;
import com.goxr3plus.streamplayer.stream.StreamPlayerEvent;
import com.goxr3plus.streamplayer.stream.StreamPlayerListener;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MediaPlayer implements StreamPlayerListener {
    public static final MediaPlayer INSTANCE = new MediaPlayer();

    public final StreamPlayer player = new StreamPlayer();
    public volatile boolean isPlaying = false;
    public volatile Song currentSong = null;
    public volatile int tick = 0;
    public volatile long totalDuration = 0L;
    public final List<LyricLine> lyric = new ArrayList<>();
    public final List<Song> playlist = new ArrayList<>();
    public volatile int currentIndex = 0;
    public volatile float volume = 1.0f;

    private volatile boolean isChangingSong = false;
    private volatile int lastPlayedTick = 0;
    private volatile long seekOffset = 0L;
    private volatile boolean isSeeking = false;
    private volatile long totalBytes = 0L;
    private volatile boolean manualStop = false;
    private volatile boolean hasStartedPlaying = false;
    private volatile long playStartTime = 0L;
    private volatile String currentSongId = "";
    private File songFile = null;

    private MediaPlayer() {
        player.addStreamPlayerListener(this);
    }

    public void playSound(File file, Song song) {
        new Thread(() -> {
            manualStop = true;
            isChangingSong = false;
            lastPlayedTick = 0;
            seekOffset = 0L;
            hasStartedPlaying = false;
            playStartTime = System.currentTimeMillis();
            currentSongId = String.valueOf(song.id);
            songFile = file;
            try {
                player.stop();
            } catch (Exception ignored) {}
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
            manualStop = false;
            try {
                player.open(file);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            MediaPlayer.this.currentSong = song;
            isPlaying = true;
            totalDuration = song.duration > 0 ? song.duration : calculateDuration(file);
            try {
                player.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private long calculateDuration(File file) {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(file)) {
            long frames = ais.getFrameLength();
            float rate = ais.getFormat().getFrameRate();
            return (long) (frames / rate * 1000);
        } catch (Exception e) {
            return 0L;
        }
    }

    public void play() {
        new Thread(() -> {
            try { player.resume(); } catch (Exception e) { e.printStackTrace(); }
            isPlaying = true;
        }).start();
    }

    public void pause() {
        new Thread(() -> {
            try { player.pause(); } catch (Exception e) { e.printStackTrace(); }
            isPlaying = false;
        }).start();
    }

    public void toggle() {
        if (isPlaying) pause();
        else play();
    }

    public void stop() {
        new Thread(() -> {
            manualStop = true;
            try { player.stop(); } catch (Exception e) { e.printStackTrace(); }
            isPlaying = false;
            tick = 0;
            seekOffset = 0L;
        }).start();
    }

    public void seekTo(long positionMs) {
        if (songFile == null || totalDuration <= 0) return;
        isSeeking = true;
        seekOffset = positionMs;
        tick = (int) positionMs;
        lastPlayedTick = (int) positionMs;
        new Thread(() -> {
            try {
                long targetBytes = (long) ((positionMs / (double) totalDuration) * totalBytes);
                player.seekBytes(targetBytes);
                Thread.sleep(150);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isSeeking = false;
            }
        }).start();
    }

    public void seekToPercent(float percent) {
        seekTo((long) (totalDuration * percent));
    }

    public void setPlaylist(List<Song> songs, int startIndex) {
        synchronized (playlist) {
            playlist.clear();
            playlist.addAll(songs);
            currentIndex = startIndex;
        }
    }

    public void playNext() {
        synchronized (playlist) {
            if (playlist.isEmpty() || isChangingSong) return;
            isChangingSong = true;
            currentIndex = (currentIndex + 1) % playlist.size();
            int idx = currentIndex;
            new Thread(() -> {
                try {
                    NeteaseCloudApi.INSTANCE.playSong(playlist.get(idx));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    isChangingSong = false;
                }
            }).start();
        }
    }

    public void playPrevious() {
        synchronized (playlist) {
            if (playlist.isEmpty() || isChangingSong) return;
            isChangingSong = true;
            currentIndex = currentIndex > 0 ? currentIndex - 1 : playlist.size() - 1;
            int idx = currentIndex;
            new Thread(() -> {
                try {
                    NeteaseCloudApi.INSTANCE.playSong(playlist.get(idx));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    isChangingSong = false;
                }
            }).start();
        }
    }

    public String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public void setVolume(float value) {
        volume = Math.max(0f, Math.min(1f, value));
        try {
            player.setGain(volume);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void opened(Object dataSource, Map<String, Object> properties) {
        Object len = properties.get("audio.length.bytes");
        if (len instanceof Number) {
            totalBytes = ((Number) len).longValue();
        }
    }

    @Override
    public void progress(int nEncodedBytes, long microsecondPosition, byte[] pcmData, Map<String, Object> properties) {
        if (isSeeking) return;
        long relativeMs = microsecondPosition / 1000;
        long currentMs = seekOffset + relativeMs;
        tick = (int) currentMs;
        lastPlayedTick = (int) currentMs;
    }

    @Override
    public void statusUpdated(StreamPlayerEvent event) {
        if (event.getPlayerStatus() == Status.PLAYING || event.getPlayerStatus() == Status.RESUMED) {
            isPlaying = true;
            hasStartedPlaying = true;
        }
        if (event.getPlayerStatus() == Status.PAUSED) {
            if (isSeeking) return;
            isPlaying = false;
        }
        if (event.getPlayerStatus() == Status.STOPPED) {
            if (isSeeking || manualStop) return;
            isPlaying = false;
            if (!hasStartedPlaying) return;
            boolean playedEnough = totalDuration > 0 && lastPlayedTick >= (totalDuration * 0.85);
            if (!playlist.isEmpty() && playedEnough && !isChangingSong) {
                playNext();
            }
        }
    }
}
