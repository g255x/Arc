package dev.kissoo.loader;

final class DownloadPause {
    private static final Object LOCK = new Object();
    private static boolean paused;

    private DownloadPause() {
    }

    static void pause() {
        synchronized (LOCK) {
            paused = true;
        }
    }

    static void resume() {
        synchronized (LOCK) {
            paused = false;
            LOCK.notifyAll();
        }
    }

    static boolean isPaused() {
        synchronized (LOCK) {
            return paused;
        }
    }

    static void awaitIfPaused() throws InterruptedException {
        synchronized (LOCK) {
            while (paused) {
                LOCK.wait(350L);
            }
        }
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Download interrupted");
        }
    }
}
