package it.polimi.ingsw.network.socket.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.network.utils.PingPongManager;
import it.polimi.ingsw.network.utils.message.MessageType;
import it.polimi.ingsw.network.utils.message.NetworkMessage;
import it.polimi.ingsw.persistence.RankingEntry;
import it.polimi.ingsw.view.ClientModel;

import java.io.*;
import java.net.Socket;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Client-side Socket connection manager.
 *
 * ── Responsibilities ────────────────────────────────────────────────────────
 *  1. Connects to the SocketServer.
 *  2. Creates a SocketServerProxy and registers it with CLIView so the
 *     view can send actions without knowing the transport.
 *  3. Starts a background reader thread that deserialises incoming JSON lines
 *     and calls the corresponding ClientModel.onXxx() methods.
 *  4. Manages a HeartbeatManager: responds to PING with PONG; sends its own
 *     PING to detect server unavailability.
 *  5. On disconnection, notifies ClientModel so CLIView can show the error.
 */
public class SocketClient {

    public static final int SOCKET_PORT = 12345;

    private static final int RETRY_INTERVAL_MS = 5_000;
    private static final int MAX_RETRY_MS      = 60_000;

    private final ClientModel     model;
    private final ObjectMapper    mapper    = new ObjectMapper();
    private final PingPongManager heartbeat = new PingPongManager();

    private SocketServerProxy proxy;
    private volatile boolean  running = true;
    private String            host;
    private volatile String   savedNickname;

    private final AtomicBoolean disconnected = new AtomicBoolean(false);
    private final AtomicLong    connGen      = new AtomicLong(0);

    public SocketClient(ClientModel model) {
        this.model = model;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  CONNECT
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Connects to the server, wires the proxy into CLIView, then starts
     * the background read loop.
     *
     * The caller (ClientLauncher) is responsible for calling
     * view.doLoginCli() after this method returns, and then blocking the
     * main thread with Thread.currentThread().join().
     *
     * @return the SocketServerProxy that CLIView should use as its GameServerProxy
     */
    public SocketServerProxy connect(String host) throws IOException {
        this.host = host;
        Socket socket = new Socket(host, SOCKET_PORT);
        System.out.println("[SocketClient] Connected to " + host + ":" + SOCKET_PORT);

        PrintWriter out = new PrintWriter(
                new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8")), true);
        BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), "UTF-8"));

        proxy = new SocketServerProxy(out);

        // Heartbeat: we send PINGs to detect server disappearance.
        heartbeat.start(
                () -> { try { proxy.ping(); } catch (Exception e) { handleServerDisconnect(); } },
                ()  -> handleServerDisconnect()
        );

        // Background reader thread.
        final long myGen = connGen.get();
        Thread reader = new Thread(() -> readLoop(in, socket, myGen), "socket-reader");
        reader.setDaemon(true);
        reader.start();

        return proxy;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  READ LOOP
    // ─────────────────────────────────────────────────────────────────────

    private void readLoop(BufferedReader in, Socket socket, long myGen) {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                try {
                    dispatch(mapper.readValue(line, NetworkMessage.class));
                } catch (Exception e) {
                    System.err.println("[SocketClient] Parse error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (running) System.err.println("[SocketClient] Read error: " + e.getMessage());
        } finally {
            // Only the reader for the CURRENT connection may trigger a disconnect.
            // Old readers blocked on readLine() until TCP timeout must not disrupt a
            // newer connection that was already established successfully.
            if (connGen.get() == myGen) {
                handleServerDisconnect();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  MESSAGE DISPATCH  (Server → Client)
    // ─────────────────────────────────────────────────────────────────────

    private void dispatch(NetworkMessage msg) {
        MessageType type;
        try {
            type = MessageType.valueOf(msg.getType());
        } catch (IllegalArgumentException e) {
            System.err.println("[SocketClient] Unknown message type: " + msg.getType());
            return;
        }

        switch (type) {

            // ── Login & setup ──────────────────────────────────────────────
            case ON_LOGIN_ACCEPTED -> {
                savedNickname = msg.str("nickname");
                model.onLoginAccepted(savedNickname, msg.num("expectedPlayers"));
            }

            case ON_PLAYER_JOINED ->
                    model.onPlayerJoined(msg.str("nickname"),
                            msg.num("currentCount"), msg.num("expected"));

            case ON_GAME_STARTING ->
                    model.onGameStarting(msg.strList("playerNicknames"));

            case ON_ERROR ->
                    model.onError(msg.str("message"));

            case ON_NO_LOBBY_AVAILABLE ->
                    model.onNoLobbyAvailable();

            case ON_LOBBY_LIST -> {
                List<LobbyManager.LobbyInfo> lobbies = msg.mapList("lobbies").stream()
                        .map(m -> new LobbyManager.LobbyInfo(
                                ((Number) m.get("id")).intValue(),
                                ((Number) m.get("currentPlayers")).intValue(),
                                ((Number) m.get("expectedPlayers")).intValue(),
                                Boolean.TRUE.equals(m.get("inProgress"))))
                        .collect(Collectors.toList());
                model.onLobbyList(lobbies);
            }

            // ── Turn ──────────────────────────────────────────────────────
            case ON_TURN_SNAPSHOT ->
                    model.onTurnSnapshot(msg.str("currentPlayerNick"), msg.str("boardSummary"));

            case ON_YOUR_TURN -> {
                GameState phase;
                try {
                    phase = GameState.valueOf(msg.str("phase"));
                } catch (Exception e) {
                    System.err.println("[SocketClient] Unknown GameState: " + msg.str("phase"));
                    return;
                }
                model.onYourTurn(msg.str("nickname"), phase, msg.str("extraInfo"));
            }

            // ── Phase 1 ───────────────────────────────────────────────────
            case ON_TOTEM_PLACED ->
                    model.onTotemPlaced(msg.str("nickname"), msg.str("boardSpaceId"));

            case ON_INVALID_ACTION ->
                    model.onInvalidAction(msg.str("nicknameTarget"), msg.str("errorMessage"));

            // ── Phase 2 ───────────────────────────────────────────────────
            case ON_CARD_TAKEN ->
                    model.onCardTaken(msg.str("nickname"), msg.str("cardId"));

            case ON_PLAYER_UPDATED ->
                    model.onPlayerUpdated(msg.str("nickname"));

            // ── End of player turn ────────────────────────────────────────
            case ON_TURN_ORDER_UPDATED ->
                    model.onTurnOrderUpdated(msg.strList("ordered"));

            // ── End of round ──────────────────────────────────────────────
            case ON_EVENT_RESOLVED ->
                    model.onEventResolved(msg.str("eventName"), msg.str("details"));

            case ON_BOARD_UPDATED ->
                    model.onBoardUpdated();

            case ON_NEW_ERA_STARTED -> {
                try {
                    model.onNewEraStarted(Age.valueOf(msg.str("era")));
                } catch (Exception e) {
                    System.err.println("[SocketClient] Unknown Age: " + msg.str("era"));
                }
            }

            // ── End of game ───────────────────────────────────────────────
            case ON_GAME_OVER ->
                    model.onGameOver(msg.str("results"));

            // ── Database ranking ──────────────────────────────────────────
            case ON_RANKING_DATA -> {
                int myRank       = msg.num("myRank");
                int totalEntries = msg.num("totalEntries");
                List<RankingEntry> ranking = msg.mapList("ranking").stream()
                        .map(m -> new RankingEntry(
                                ((Number) m.get("rank")).intValue(),
                                (String)  m.get("nickname"),
                                ((Number) m.get("score")).intValue(),
                                LocalDate.parse((String) m.get("date")),
                                ((Number) m.get("numPlayers")).intValue()
                        ))
                        .collect(Collectors.toList());
                model.onRankingData(myRank, totalEntries, ranking);
            }

            // ── Disconnection ─────────────────────────────────────────────
            case ON_PLAYER_DISCONNECTED ->
                    model.onPlayerDisconnected(msg.str("nickname"));

            // === SPECTATOR ===
            case ON_SPECTATOR_JOINED ->
                    model.onSpectatorJoined(msg.str("currentPlayerNick"), msg.str("boardSummary"));
            // === END SPECTATOR ===

            // ── Heartbeat ─────────────────────────────────────────────────
            case PING -> {
                try { proxy.pong(); } catch (Exception e) { handleServerDisconnect(); }
            }
            case PONG -> heartbeat.receivedPong();

            default -> System.err.println("[SocketClient] Unhandled type: " + type);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  DISCONNECTION
    // ─────────────────────────────────────────────────────────────────────

    private void handleServerDisconnect() {
        if (!disconnected.compareAndSet(false, true)) return;
        running = false;
        heartbeat.stop();
        System.err.println("\n[SocketClient] Server connection lost.");
        model.onError("SERVER_DOWN");
        startReconnectLoop();
    }

    private void startReconnectLoop() {
        Thread t = new Thread(() -> {
            long deadline = System.currentTimeMillis() + MAX_RETRY_MS;
            while (System.currentTimeMillis() < deadline) {
                try {
                    Thread.sleep(RETRY_INTERVAL_MS);
                    Socket socket = new Socket(host, SOCKET_PORT);
                    PrintWriter out = new PrintWriter(
                            new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8")), true);
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), "UTF-8"));

                    long newGen = connGen.incrementAndGet();
                    proxy.setWriter(out);
                    running = true;
                    disconnected.set(false);

                    heartbeat.stop();
                    heartbeat.start(
                            () -> { try { proxy.ping(); } catch (Exception e) { handleServerDisconnect(); } },
                            () -> handleServerDisconnect()
                    );

                    final long capturedGen = newGen;
                    Thread reader = new Thread(() -> readLoop(in, socket, capturedGen), "socket-reader");
                    reader.setDaemon(true);
                    reader.start();

                    if (savedNickname != null) {
                        proxy.login(savedNickname);
                    }
                    System.out.println("[SocketClient] Reconnected as " + savedNickname);
                    return;
                } catch (Exception e) {
                    System.out.println("[SocketClient] Server not available yet, retrying...");
                }
            }
            model.onError("RECONNECT_TIMEOUT");
        }, "socket-reconnect");
        t.setDaemon(true);
        t.start();
    }
}