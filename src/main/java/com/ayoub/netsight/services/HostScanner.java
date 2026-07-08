package com.ayoub.netsight.services;


import com.ayoub.netsight.model.HostInfo;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * A Callable that fully probes a single IP address:
 *   <li> Ping via InetAddress.isReachable()</li>
 *   <li>Reverse-DNS hostname lookup</li>
 *   <Li>Port scan via PortScanner</Li>
 *
 * <p>Designed to be submitted to an ExecutorService — one task per IP.</p>
 *
 * @author Ayoub Elhatab
 * LinkedIn: <a href="https://www.linkedin.com/in/ayoub-elhatab/">Ayoub Elhatab</a>
 */
public class HostScanner implements Callable<HostInfo> {

    private final String ip;
    private final int    timeoutMs;
    private final List<Integer> ports;

    public HostScanner(String ip, int timeoutMs, List<Integer> ports) {
        this.ip        = ip;
        this.timeoutMs = timeoutMs;
        this.ports     = ports;
    }

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
