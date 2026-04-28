package it.polimi.ingsw.network.rmi.server;

import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.network.rmi.client.VirtualServerRmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Server RMI. Possiede un LobbyManager (invece del singolo GameController).
 * Tutta la logica di instradamento è nel LobbyManager.
 */
public class RmiServer extends UnicastRemoteObject implements VirtualServerRmi {

    private static final String SERVER_NAME = "GameServer";
    private static final int    RMI_PORT    = 1099;

    private final LobbyManager lobbyManager = new LobbyManager();

    public RmiServer() throws RemoteException { super(); }

    public static void main(String[] args) throws RemoteException {
        // Auto-rileva l'IP della LAN (non localhost) e lo dichiara a RMI
        String serverIp = resolveLocalIp();
        System.setProperty("java.rmi.server.hostname", serverIp);

        VirtualServerRmi server = new RmiServer();
        Registry registry = LocateRegistry.createRegistry(RMI_PORT);
        registry.rebind(SERVER_NAME, server);

        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║  Server avviato su porta " + RMI_PORT + "    ║");
        System.out.println("║  IP: " + serverIp + "              ║");
        System.out.println("║  Comunica questo IP ai client!   ║");
        System.out.println("╚══════════════════════════════════╝");
    }

    /**
     * Trova il primo IP non-loopback della macchina (es. 192.168.x.x).
     * Fallback a localhost se non trovato.
     */
    private static String resolveLocalIp() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> ifaces =
                    java.net.NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                java.net.NetworkInterface iface = ifaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) continue;
                java.util.Enumeration<java.net.InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    java.net.InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (java.net.SocketException e) {
            System.err.println("Impossibile rilevare IP: " + e.getMessage());
        }
        return "localhost";
    }

    // --- LOGIN ---

    @Override
    public synchronized void loginFirstPlayer(String nickname, int numPlayers,
                                              VirtualViewRmi clientView) throws RemoteException {
        System.out.println("[RmiServer] loginFirstPlayer: " + nickname);
        lobbyManager.createLobby(nickname, numPlayers, clientView);
    }

    @Override
    public synchronized void login(String nickname,
                                   VirtualViewRmi clientView) throws RemoteException {
        System.out.println("[RmiServer] login: " + nickname);
        lobbyManager.joinLobby(nickname, clientView);
    }

    @Override
    public synchronized void requestLobbyList(VirtualViewRmi clientView) throws RemoteException {
        try {
            clientView.onLobbyList(lobbyManager.getOpenLobbies());
        } catch (Exception e) {
            System.err.println("[RmiServer] requestLobbyList: " + e.getMessage());
        }
    }

    // --- GIOCO ---

    @Override
    public synchronized void placeTotem(String nickname,
                                        char boardSpaceLetter) throws RemoteException {
        System.out.println("[RmiServer] placeTotem: " + nickname + " → " + boardSpaceLetter);
        lobbyManager.placeTotem(nickname, boardSpaceLetter);
    }

    @Override
    public synchronized void pickCard(String nickname, int cardIndex,
                                      boolean fromTop) throws RemoteException {
        System.out.println("[RmiServer] pickCard: " + nickname
                + " idx=" + cardIndex + " top=" + fromTop);
        lobbyManager.pickCard(nickname, cardIndex, fromTop);
    }
}