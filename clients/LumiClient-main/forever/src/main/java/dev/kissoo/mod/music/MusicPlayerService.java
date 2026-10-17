package dev.kissoo.mod.music;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public final class MusicPlayerService {
    public enum PlaybackState {IDLE, LOADING, PLAYING, ERROR}
    public enum PlayMode {QUEUE, LOOP_ONE}

    private final NeteaseApiClient api = new NeteaseApiClient();
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AlienV4-MusicPlayer");
        t.setDaemon(true);
        return t;
    });

    private volatile Mp3JavaSoundPlayer currentPlayer;
    private volatile Future<?> currentTask;
    private volatile NeteaseSong currentSong;
    private volatile long songStartMs;
    private volatile long basePositionMs;
    private volatile boolean paused;
    private volatile float volume01 = 0.8f;
    private volatile List<LrcLine> lrcLines = List.of();
    private volatile PlaybackState playbackState = PlaybackState.IDLE;
    private volatile String lastError = "";
    private final AlbumArtManager albumArt = new AlbumArtManager();
    private volatile net.minecraft.util.Identifier currentAlbumTex;
    private volatile PlayMode playMode = PlayMode.QUEUE;

    private final List<NeteaseSong> queue = new CopyOnWriteArrayList<>();
    private volatile int queueIndex = -1;

    private final AtomicLong sessionCounter = new AtomicLong(0);
    private volatile boolean isPlayFromRunning = false;

    public MusicPlayerService() {
        NeteaseLoginManager.getInstance().setOnCookieChangeListener(cookies -> {
            if (cookies != null) {
                api.setAuthCookies(cookies);
            } else {
                api.clearAuthCookies();
            }
        });
        if (NeteaseLoginManager.getInstance().isLoggedIn()) {
            api.setAuthCookies(NeteaseLoginManager.getInstance().getLoginCookies());
        }
    }

    public NeteaseApiClient api() {
        return api;
    }

    public AlbumArtManager getAlbumArtManager() {
        return albumArt;
    }

    public synchronized void stop() {
        stop(true);
    }

    private synchronized void stop(boolean clearQueue) {
        sessionCounter.incrementAndGet(); // 使所有进行中的任务失效

        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
        if (currentPlayer != null) {
            currentPlayer.stop();
            currentPlayer.close();
            currentPlayer = null;
        }
        currentSong = null;
        lrcLines = List.of();
        songStartMs = 0;
        basePositionMs = 0;
        paused = false;
        playbackState = PlaybackState.IDLE;
        lastError = "";
        currentAlbumTex = null;
        isPlayFromRunning = false;

        if (clearQueue) {
            queue.clear();
            queueIndex = -1;
        }
    }

    public synchronized void play(NeteaseSong song, boolean fadeIn) {
        Objects.requireNonNull(song, "song");
        stop(true);
        queue.clear();
        queueIndex = -1;
        playFromInternal(song, 0);
    }

    public synchronized void pause() {
        if (currentSong == null || paused) return;
        paused = true;
        sessionCounter.incrementAndGet();

        if (currentPlayer != null) {
            basePositionMs = currentPlayer.getPlayedMs();
            currentPlayer.stop();
        } else {
            basePositionMs = getSongPositionMs();
        }
        playbackState = PlaybackState.IDLE;
    }

    public synchronized void resume() {
        if (currentSong == null || !paused) return;
        paused = false;
        playFromInternal(currentSong, basePositionMs);
    }

    public synchronized void seekTo(long positionMs) {
        if (currentSong == null) return;
        basePositionMs = Math.max(0, positionMs);
        paused = false;
        playFromInternal(currentSong, basePositionMs);
    }

    private void playFromInternal(NeteaseSong song, long positionMs) {
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
        if (currentPlayer != null) {
            currentPlayer.stop();
            currentPlayer.close();
            currentPlayer = null;
        }

        currentSong = song;
        basePositionMs = Math.max(0, positionMs);
        songStartMs = System.currentTimeMillis();
        playbackState = PlaybackState.LOADING;
        lastError = "";
        currentAlbumTex = albumArt.getOrRequest(song.id(), api.resolvePicUrl(song));

        final long sessionId = sessionCounter.incrementAndGet();
        final NeteaseSong songToPlay = song;
        final long startPosition = basePositionMs;

        currentTask = io.submit(() -> {
            Mp3JavaSoundPlayer player = null;
            try {
                if (sessionCounter.get() != sessionId) return;

                URI audioUri;
                try {
                    audioUri = api.streamUrl(songToPlay);
                } catch (Throwable t) {
                    playbackState = PlaybackState.ERROR;
                    lastError = "获取音频地址失败: " + t.getMessage();
                    scheduleNextOnError();
                    return;
                }
                try (InputStream in = openStreamFollowRedirects(audioUri)) {
                    if (sessionCounter.get() != sessionId) return;
                    try {
                        String lrc = api.lyric(songToPlay);
                        if (sessionCounter.get() == sessionId) {
                            lrcLines = LrcParser.parse(lrc);
                        }
                    } catch (Throwable ignored) {
                        if (sessionCounter.get() == sessionId) {
                            lrcLines = List.of();
                        }
                    }

                    BufferedInputStream bin = new BufferedInputStream(in);
                    if (!looksLikeMp3(bin)) {
                        if (sessionCounter.get() == sessionId) {
                            playbackState = PlaybackState.ERROR;
                            lastError = "Stream is not MP3 (possibly VIP/blocked).";
                            scheduleNextOnError();
                        }
                        return;
                    }

                    if (sessionCounter.get() != sessionId) return;

                    playbackState = PlaybackState.PLAYING;
                    player = new Mp3JavaSoundPlayer(bin, startPosition);

                    synchronized (MusicPlayerService.this) {
                        if (sessionCounter.get() == sessionId) {
                            currentPlayer = player;
                        } else {
                            player.close();
                            return;
                        }
                    }

                    player.setVolume01(volume01);
                    player.play();
                    if (sessionCounter.get() == sessionId) {
                        synchronized (MusicPlayerService.this) {
                            basePositionMs = player.getPlayedMs();
                            playbackState = PlaybackState.IDLE;
                        }
                        scheduleNextSong();
                    }
                }
            } catch (Throwable t) {
                if (sessionCounter.get() == sessionId) {
                    synchronized (MusicPlayerService.this) {
                        playbackState = PlaybackState.ERROR;
                        lastError = t.getClass().getSimpleName() +
                                (t.getMessage() == null ? "" : (": " + t.getMessage()));
                    }

                    if (isVipOrBlockedError(t) || lastError.contains("403") || lastError.contains("404")) {
                        System.out.println("[MusicPlayer] VIP/Error song skipped: " + songToPlay.name() + " - " + lastError);
                        scheduleNextOnError();
                    } else {
                        scheduleNextSong();
                    }
                }
            } finally {
                synchronized (MusicPlayerService.this) {
                    if (currentPlayer == player && player != null) {
                        player.stop();
                        player.close();
                        currentPlayer = null;
                    }
                    currentTask = null;
                }
            }
        });
    }

    private void scheduleNextSong() {
        io.submit(() -> {
            synchronized (MusicPlayerService.this) {
                playNextInQueueIfAny();
            }
        });
    }

    private void scheduleNextOnError() {
        io.submit(() -> {
            synchronized (MusicPlayerService.this) {
                playNextInQueueAfterError();
            }
        });
    }

    private synchronized void playNextInQueueAfterError() {
        if (queue.isEmpty()) return;

        boolean wasLoopOne = (playMode == PlayMode.LOOP_ONE);
        int next = queueIndex + 1;
        if (next >= 0 && next < queue.size()) {
            queueIndex = next;
            NeteaseSong nextSong = queue.get(queueIndex);
            basePositionMs = 0;
            paused = false;
            System.out.println("[MusicPlayer] Skipping to next: " + nextSong.name());
            playFromInternal(nextSong, 0);
        } else if (queue.size() > 0) {
            queueIndex = 0;
            NeteaseSong nextSong = queue.get(queueIndex);
            basePositionMs = 0;
            paused = false;
            playFromInternal(nextSong, 0);
        }
    }

    private boolean isVipOrBlockedError(Throwable t) {
        String msg = t.getMessage();
        if (msg == null) return false;
        return msg.contains("403") ||
                msg.contains("404") ||
                msg.contains("付费") ||
                msg.contains("VIP") ||
                msg.contains("版权") ||
                msg.contains("blocked") ||
                msg.contains("denied") ||
                msg.contains("unavailable");
    }

    public synchronized void setQueue(List<NeteaseSong> songs, int startIndex) {
        stop(false);
        queue.clear();
        if (songs != null) queue.addAll(songs);
        queueIndex = Math.max(0, Math.min(startIndex, queue.size() - 1));
        if (queueIndex >= 0 && queueIndex < queue.size()) {
            NeteaseSong first = queue.get(queueIndex);
            paused = false;
            playFromInternal(first, 0);
        }
    }

    private synchronized void playNextInQueueIfAny() {
        if (playMode == PlayMode.LOOP_ONE && currentSong != null) {
            if (lastError != null && (lastError.contains("403") || lastError.contains("404"))) {
                playNextInQueueAfterError();
                return;
            }
            paused = false;
            basePositionMs = 0;
            playFromInternal(currentSong, 0);
            return;
        }

        if (queue.isEmpty()) return;

        int next = queueIndex + 1;
        if (next >= 0 && next < queue.size()) {
            queueIndex = next;
            NeteaseSong nextSong = queue.get(queueIndex);
            basePositionMs = 0;
            paused = false;
            playFromInternal(nextSong, 0);
        }
        // 队列播放完毕，不自动循环
    }

    public synchronized void setVolume01(float v) {
        volume01 = Math.max(0.0f, Math.min(1.0f, v));
        if (currentPlayer != null) currentPlayer.setVolume01(volume01);
    }

    public float getVolume01() {
        return volume01;
    }

    public PlaybackState getPlaybackState() {
        return playbackState;
    }

    public String getLastError() {
        return lastError;
    }

    public boolean isPaused() {
        return paused;
    }

    public net.minecraft.util.Identifier getCurrentAlbumTex() {
        NeteaseSong s = currentSong;
        if (s != null) {
            net.minecraft.util.Identifier id = albumArt.getOrRequest(s.id(), api.resolvePicUrl(s));
            if (id != null) currentAlbumTex = id;
        }
        return currentAlbumTex;
    }

    public net.minecraft.util.Identifier getAlbumTexFor(NeteaseSong song) {
        if (song == null) return null;
        String picUrl = song == currentSong ? api.resolvePicUrl(song) : song.picUrl();
        net.minecraft.util.Identifier id = albumArt.getOrRequest(song.id(), picUrl);
        if (song == currentSong && id != null) currentAlbumTex = id;
        return id;
    }

    public String getAlbumArtDebug() {
        return albumArt.getLastDebug();
    }

    public void setPlayMode(PlayMode mode) {
        if (mode != null) playMode = mode;
    }

    public PlayMode getPlayMode() {
        return playMode;
    }

    public synchronized boolean next() {
        if (queue.isEmpty()) return false;
        int next = queueIndex + 1;
        if (next < 0 || next >= queue.size()) return false;
        queueIndex = next;
        paused = false;
        basePositionMs = 0;
        playFromInternal(queue.get(queueIndex), 0);
        return true;
    }

    public synchronized boolean prev() {
        if (queue.isEmpty()) return false;
        int prev = queueIndex - 1;
        if (prev < 0 || prev >= queue.size()) return false;
        queueIndex = prev;
        paused = false;
        basePositionMs = 0;
        playFromInternal(queue.get(queueIndex), 0);
        return true;
    }

    public NeteaseSong getCurrentSong() {
        return currentSong;
    }

    public long getSongPositionMs() {
        if (paused) return Math.max(0, basePositionMs);
        if (currentPlayer != null) {
            return Math.max(0, currentPlayer.getPlayedMs());
        }
        return Math.max(0, basePositionMs);
    }

    public List<LrcLine> getLrcLines() {
        return lrcLines;
    }

    public String getLyricAt(long timeMs) {
        List<LrcLine> lines = lrcLines;
        int idx = LrcParser.findLineIndex(lines, timeMs);
        if (idx < 0 || idx >= lines.size()) return "";
        return lines.get(idx).text();
    }

    public String getNextLyricAt(long timeMs) {
        List<LrcLine> lines = lrcLines;
        int idx = LrcParser.findLineIndex(lines, timeMs);
        int next = idx + 1;
        if (next < 0 || next >= lines.size()) return "";
        return lines.get(next).text();
    }

    public void cleanup() {
        stop(true);
        albumArt.clearAll();
    }

    private static InputStream openStreamFollowRedirects(URI uri) throws IOException {
        URL url = uri.toURL();
        for (int i = 0; i < 5; i++) {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(20_000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Referer", "https://music.163.com/");
            int code = conn.getResponseCode();
            if (code >= 300 && code < 400) {
                String loc = conn.getHeaderField("Location");
                if (loc == null || loc.isBlank()) throw new IOException("redirect without Location");
                try {
                    url = url.toURI().resolve(loc).toURL();
                } catch (Exception e) {
                    url = URI.create(loc).toURL();
                }
                continue;
            }
            if (code != 200) throw new IOException("http " + code);
            return conn.getInputStream();
        }
        throw new IOException("too many redirects");
    }

    private static boolean looksLikeMp3(BufferedInputStream in) throws IOException {
        in.mark(64);
        byte[] b = new byte[16];
        int n = in.read(b);
        in.reset();
        if (n < 4) return false;
        if (b[0] == 'I' && b[1] == 'D' && b[2] == '3') return true;
        int b0 = b[0] & 0xFF;
        int b1 = b[1] & 0xFF;
        return b0 == 0xFF && (b1 & 0xE0) == 0xE0;
    }
}
