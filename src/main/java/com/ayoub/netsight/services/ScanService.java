package com.ayoub.netsight.services;

import com.ayoub.netsight.model.HostInfo;
import javafx.application.Platform;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 *<p> Orchestrates a full subnet scan.</p>
 *
 * Creates a fixed thread pool, submits one HostScanner per IP.
 * Callbacks are dispatched on the JavaFX Application Thread
 * via Platform.runLater() so the UI can be updated safely.
 *
 * @author Ayoub Elhatab
 * LinkedIn: <a href="https://www.linkedin.com/in/ayoub-elhatab/">Ayoub Elhatab</a>
 */
public class ScanService {

    private ExecutorService executor;

    /**
     * @param subnet     e.g. "192.168.1."
     * @param rangeStart first host octet, e.g. 1
     * @param rangeEnd   last  host octet, e.g. 254
     * @param threads    thread pool size (50–100 works well for LAN)
     * @param timeoutMs  per-host timeout in milliseconds
     * @param onFound    called on FX thread for every alive host
     * @param onProgress called on FX thread with (done, total)
     */
    public void scan(String subnet,
                     int rangeStart, int rangeEnd,
                     int threads,   int timeoutMs,
                     Consumer<HostInfo>    onFound,
                     BiConsumer<Integer, Integer> onProgress) {

        int total = rangeEnd - rangeStart + 1;
        AtomicInteger done  = new AtomicInteger(0);

        executor = Executors.newFixedThreadPool(threads);

        for (int i = rangeStart; i <= rangeEnd; i++) {
            final String ip = subnet + i;

            executor.submit(() -> {
                HostInfo info = new HostScanner(ip, timeoutMs).call();
                int d = done.incrementAndGet();

                // All UI updates MUST happen on the FX thread
                Platform.runLater(() -> {
                    if (info.isAlive()) onFound.accept(info);
                    onProgress.accept(d, total);
                });
            });
        }

        executor.shutdown();
    }

    /** Immediately cancels all pending scans (user clicked Stop). */
    public void stop() {
        if (executor != null) executor.shutdownNow();
    }
}
