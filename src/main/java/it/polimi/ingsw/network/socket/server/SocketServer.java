package it.polimi.ingsw.network.socket.server;

import it.polimi.ingsw.CombinedServer;
import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.network.utils.NetworkUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * TCP Socket server.
 *
 * Accepts incoming connections and spawns a {@link SocketClientHandler} per client.
 * Shares the same {@link LobbyManager} as {@link it.polimi.ingsw.network.rmi.server.RmiServer}
 * so players using different transports can participate in the same game.
 *
 * Started by {@link CombinedServer}.
 */
public class SocketServer {

    public static final int SOCKET_PORT = 12345;

    private final LobbyManager  lobbyManager;
    private ServerSocket        serverSocket;
    private volatile boolean    running = true;

    public SocketServer(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    /**
     * Blocking accept loop. Call on a dedicated thread.
     * Spawns one daemon thread per accepted connection.
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(SOCKET_PORT);
        System.out.printf("╔══════════════════════════════════╗%n");
        System.out.printf("║  SocketServer avviato su porta %-4d║%n", SOCKET_PORT);
        System.out.printf("║  IP: %-28s║%n", NetworkUtils.resolveLocalIp());
        System.out.printf("╚══════════════════════════════════╝%n");

        while (running) {
            try {
                Socket client = serverSocket.accept();
                SocketClientHandler handler = new SocketClientHandler(client, lobbyManager);
                Thread t = new Thread(handler,
                        "socket-client-" + client.getRemoteSocketAddress());
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                if (running) System.err.println("[SocketServer] Accept error: " + e.getMessage());
            }
        }
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
    }
}