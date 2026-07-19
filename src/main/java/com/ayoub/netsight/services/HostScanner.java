package com.ayoub.netsight.services;


import com.ayoub.netsight.model.HostInfo;
import lombok.RequiredArgsConstructor;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * A {@link Callable} that fully probes a single IP address in three steps:
 * <ol>
 *   <li>Ping via {@link InetAddress#isReachable(int)}</li>
 *   <li>Reverse-DNS hostname lookup via {@link InetAddress#getCanonicalHostName()}</li>
 *   <li>TCP port scan via {@link PortScanner#scan(String, List, int)}</li>
 * </ol>
 * Designed to be submitted to an {@link java.util.concurrent.ExecutorService} — one task per IP.
 *
 * @author Ayoub Elhatab
 * LinkedIn: <a href="https://www.linkedin.com/in/ayoub-elhatab/">Ayoub Elhatab</a>
 */
@RequiredArgsConstructor
public class HostScanner implements Callable<HostInfo> {

    private final String ip;
    private final int timeoutMs;
    private final List<Integer> ports;

    /**
     * Executes the full probe sequence: ping → hostname → port scan.
     *
     * @return a {@link HostInfo} populated with results, or an empty one with {@code alive = false} if the host is unreachable
     */
    @Override
    public HostInfo call() {
        HostInfo info = new HostInfo(ip);
        try {
            InetAddress addr  = InetAddress.getByName(ip);
            long start = System.currentTimeMillis();

            if (addr.isReachable(timeoutMs)) {
                info.setAlive(true);
                info.setPingMs(System.currentTimeMillis() - start);

                // Reverse-DNS — falls back to IP string if no PTR record
                String host = addr.getCanonicalHostName();
                info.setHostname(host.equals(ip) ? "Unknown" : host);

                // TCP port scan on common ports
                info.setOpenPorts(PortScanner.scan(ip, ports, timeoutMs));
            }
        } catch (Exception e) {
            // host unreachable or DNS error — leave alive = false
        }
        return info;
    }
}
