package com.ayoub.netsight.services;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Probes a list of ports on a given IP by attempting a TCP connection.
 *
 * @author Ayoub Elhatab
 * LinkedIn: <a href="https://www.linkedin.com/in/ayoub-elhatab/">Ayoub Elhatab</a>
 */
public class PortScanner {


    public static final Map<Integer, String> SERVICE_NAMES = Map.ofEntries(
            Map.entry(21,   "FTP"),
            Map.entry(22,   "SSH"),
            Map.entry(23,   "Telnet"),
            Map.entry(25,   "SMTP"),
            Map.entry(53,   "DNS"),
            Map.entry(80,   "HTTP"),
            Map.entry(443,  "HTTPS"),
            Map.entry(445,  "SMB"),
            Map.entry(3306, "MySQL"),
            Map.entry(3389, "RDP"),
            Map.entry(5432, "PostgreSQL"),
            Map.entry(5900, "VNC"),
            Map.entry(8080, "HTTP-alt"),
            Map.entry(8443, "HTTPS-alt")
    );

    /**
     * Scans every port in {@code ports} on {@code ip}.
     * @return list of ports that accepted a connection
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
