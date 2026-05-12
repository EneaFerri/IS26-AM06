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
     * Try to catch che ip og tailscale
     * Falls back to "localhost" if none is found.
     *
     * Suitable for setting java.rmi.server.hostname and for displaying
     * connection instructions to the user.
     */
    public static String resolveLocalIp() {
        String fallback = null;
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) continue;
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr.isLoopbackAddress() || !(addr instanceof Inet4Address)) continue;
                    if (isTailscaleIp(addr)) return addr.getHostAddress(); // preferisci Tailscale
                    if (fallback == null) fallback = addr.getHostAddress();
                }
            }
        } catch (SocketException e) {
            System.err.println("[NetworkUtils] Cannot resolve local IP: " + e.getMessage());
        }
        return fallback != null ? fallback : "localhost";
    }

    /* Tailscale usa il range CGNAT 100.64.0.0/10 (100.64.x.x – 100.127.x.x) */
    private static boolean isTailscaleIp(InetAddress addr) {
        byte[] b = addr.getAddress();
        int first  = b[0] & 0xFF;
        int second = b[1] & 0xFF;
        return first == 100 && second >= 64 && second <= 127;
    }
}