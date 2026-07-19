package com.ayoub.netsight.services;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Utility class for scanning open TCP ports on a given host.
 * Uses a fixed thread pool to probe ports concurrently for performance.
 *
 * @author Ayoub Elhatab
 * LinkedIn: <a href="https://www.linkedin.com/in/ayoub-elhatab/">Ayoub Elhatab</a>
 */
public class PortScanner {

    /**
     * Probes a list of ports on the given IP address concurrently
     * by attempting a TCP {@link Socket} connection on each port.
     * A successful connection indicates the port is open.
     *
     * @param ip        the target IP address to scan
     * @param ports     the list of port numbers to probe
     * @param timeoutMs the connection timeout per port in milliseconds
     * @return          a sorted list of port numbers that accepted a connection
     */
    public static List<Integer> scan(String ip, List<Integer> ports, int timeoutMs) {
        List<Integer> open = Collections.synchronizedList(new ArrayList<>());
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(ports.size(), 100));// max 100 threads per host
;
        List<Future<?>> futures = new ArrayList<>();
        for (int port : ports) {
            futures.add(pool.submit(() -> {
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress(ip, port), timeoutMs);
                    open.add(port);
                } catch (IOException ex) {

                }
            }));
        }

        // wait for all ports to finish
        for (Future<?> f : futures) {
            try { f.get(); } catch (Exception ex) {

            }
        }

        pool.shutdown();
        Collections.sort(open);
        return open;
    }
}
