package com.ayoub.netsight.services;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Probes a list of ports on a given IP by attempting a TCP connection.
 *
 * @author Ayoub Elhatab
 * LinkedIn: <a href="https://www.linkedin.com/in/ayoub-elhatab/">Ayoub Elhatab</a>
 */
public class PortScanner {

    /** most commonly encountered ports on a LAN. */
    public static final List<Integer> COMMON_PORTS = List.of(
            21,
            22,   // SSH
            23,   // Telnet (legacy)
            25,   // SMTP
            53,   // DNS
            80,   // HTTP
            110,  // POP3
            135,  // Windows RPC
            139,  // NetBIOS
            143,  // IMAP
            443,  // HTTPS
            445,  // SMB (file sharing)
            3306, // MySQL
            3389, // RDP (Windows Remote Desktop)
            5432, // PostgreSQL
            5900, // VNC
            8080, // HTTP alt / app servers
            8443  // HTTPS alt
    );

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
        List<Integer> open = new ArrayList<>();
        for (int port : ports) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(ip, port), timeoutMs);
                // connection accepted = port open
                open.add(port);
            } catch (IOException e) {
                // refused or timeout = closed
            }
        }
        return open;
    }
}
