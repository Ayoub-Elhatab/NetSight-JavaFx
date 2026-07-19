package com.ayoub.netsight.services;

import com.ayoub.netsight.model.HostInfo;
import javafx.application.Platform;
import java.util.List;
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
     * Launches a concurrent subnet scan over the given IP range.
     * Submits one {@link HostScanner} task per IP to a fixed thread pool.
     * Results are dispatched back to the JavaFX Application Thread via {@code Platform.runLater()}.
     *
     * @param subnet      the subnet prefix, e.g. {@code "192.168.1."}
     * @param rangeStart  the first host octet to scan, e.g. {@code 1}
     * @param rangeEnd    the last host octet to scan, e.g. {@code 254}
     * @param threads     the thread pool size — 50 to 100 works well for LAN
     * @param timeoutMs   the per-host connection timeout in milliseconds
     * @param ports       the list of ports to probe on each alive host
     * @param onFound     callback invoked on the FX thread for every alive host discovered
     * @param onProgress  callback invoked on the FX thread with (hostsScanned, totalHosts)
     */
    public void scan(String subnet,
                     int rangeStart, int rangeEnd,
                     int threads,   int timeoutMs,
                     List<Integer> ports,
                     Consumer<HostInfo>    onFound,
                     BiConsumer<Integer, Integer> onProgress) {

        int total = rangeEnd - rangeStart + 1;
        AtomicInteger done  = new AtomicInteger(0);

        executor = Executors.newFixedThreadPool(threads);

        for (int i = rangeStart; i <= rangeEnd; i++) {
            final String ip = subnet + i;

            executor.submit(() -> {
                HostInfo info = new HostScanner(ip, timeoutMs, ports).call();
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

    /**
     * Immediately cancels all pending scans (user clicked Stop).
     **/
    public void stop() {
        if (executor != null) executor.shutdownNow();
    }
}
