package it.polimi.ingsw;

import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.network.rmi.server.RmiServer;
import it.polimi.ingsw.network.utils.NetworkUtils;
import it.polimi.ingsw.network.socket.server.SocketServer;
import it.polimi.ingsw.database.DatabaseManager;

import java.rmi.RemoteException;

/**
 * Unified server entry point.
 *
 * Starts both the RMI and the Socket servers sharing a single {@link LobbyManager},
 * so players connecting via different transports can join the same games.
 *
 * ── Ports ──────────────────────────────────────────────────────────────────
 *   RMI registry : 1099  (default)
 *   Socket TCP   : 12345
 */
public class CombinedServer {

    public static void main(String[] args) {
        String ip = NetworkUtils.resolveLocalIp();
        System.setProperty("java.rmi.server.hostname", ip);

        LobbyManager sharedLobbyManager = new LobbyManager();

        // FA1: close DB connection on server shutdown (Ctrl+C / SIGTERM)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { DatabaseManager.getInstance().close(); } catch (Exception ignored) {}
        }, "db-shutdown"));

        // ── RMI server ────────────────────────────────────────────────────
        try {
            RmiServer rmi = new RmiServer(sharedLobbyManager);
            rmi.start();
            //System.out.println("[CombinedServer] RMI server avviato.");
        } catch (RemoteException e) {
            System.err.println("[CombinedServer] RMI startup failed: " + e.getMessage());
        }

        // ── Socket server (blocking — runs on main thread) ─────────────────
        SocketServer socket = new SocketServer(sharedLobbyManager);
        try {

            socket.start();
            //System.out.println("[CombinedServer] Socket server avviato.");

        } catch (Exception e) {
            System.err.println("[CombinedServer] Socket startup failed: " + e.getMessage());
        }
    }
}