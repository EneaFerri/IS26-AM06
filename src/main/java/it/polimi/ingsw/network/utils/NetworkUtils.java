package it.polimi.ingsw.network.utils;

import java.net.*;
import java.util.Enumeration;

/**
 * Shared network utilities.
 *
 * Replaces the identical resolveLocalIp() method that was copy-pasted
 * into both RmiServer and RmiClient.
 */
public final class NetworkUtils {

    private NetworkUtils() {}

    /**
     * Returns the first non-loopback IPv4 address found on this machine.
     * Falls back to "localhost" if none is found.
     *
     * Suitable for setting java.rmi.server.hostname and for displaying
     * connection instructions to the user.
     */
    public static String resolveLocalIp() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) continue;
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("[NetworkUtils] Cannot resolve local IP: " + e.getMessage());
        }
        return "localhost";
    }
}