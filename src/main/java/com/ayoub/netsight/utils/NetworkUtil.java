package com.ayoub.netsight.utils;

import java.net.*;
import java.util.Enumeration;

/**
 * Detects the local non-loopback IPv4 address at startup
 * so the subnet field can be pre-filled automatically.
 *
 * @author Ayoub Elhatab
 * LinkedIn: <a href="https://www.linkedin.com/in/ayoub-elhatab/">Ayoub Elhatab</a>
 */
public class NetworkUtil {

    /**
     * Detects the local non-loopback IPv4 address of this machine.
     *
     * @return local IPv4 address e.g. {@code "192.168.1.42"}, or {@code "192.168.1.1"} if detection fails
     */
    public static String getLocalIp() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();

            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();

                // skip loopback and inactive interfaces
                if (iface.isLoopback() || !iface.isUp()) continue;

                for (InterfaceAddress addr : iface.getInterfaceAddresses()) {
                    InetAddress ia = addr.getAddress();
                    if (ia instanceof Inet4Address) {
                        return ia.getHostAddress(); //ex : "192.168.1.42"
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "192.168.1.1"; // safe fallback
    }

    /**
     * Extracts the subnet prefix from a full IPv4 address.
     * e.g. {@code "192.168.1.42"} → {@code "192.168.1."}
     *
     * @param ip the full IPv4 address
     * @return   the subnet prefix ending with a dot
     */
    public static String toSubnet(String ip) {
        return ip.substring(0, ip.lastIndexOf('.') + 1);
    }
}
