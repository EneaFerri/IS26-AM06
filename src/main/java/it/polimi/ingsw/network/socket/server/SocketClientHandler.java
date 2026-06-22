package it.polimi.ingsw.network.socket.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.VirtualView;
import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.network.utils.PingPongManager;
import it.polimi.ingsw.network.utils.message.MessageType;
import it.polimi.ingsw.network.utils.message.NetworkMessage;
import it.polimi.ingsw.database.RankingEntry;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Handles one Socket client connection on the server side.
 *
 * <p><b>Responsibilities:</b></p>
 * <ol>
 *   <li>Read loop: deserialises incoming JSON lines and dispatches them to {@link LobbyManager}.</li>
 *   <li>VirtualView: serialises all server-to-client callbacks to JSON and writes them over the socket.</li>
 *   <li>Heartbeat: sends PING every 10 s; if no PONG arrives, disconnects.</li>
 *   <li>Disconnection: on any IOException or heartbeat timeout, notifies LobbyManager.</li>
 * </ol>
 *
 * <p><b>Thread safety:</b> {@code send()} serialises JSON and enqueues it (non-blocking);
 * {@code senderLoop()} writes to the socket on a dedicated daemon thread without holding any game lock.
 * {@code out}, {@code nickname}, and {@code running} are {@code volatile} for cross-thread visibility.
 * {@code handleDisconnect()} is idempotent (guarded by {@code running}).</p>
 */
public class SocketClientHandler implements VirtualView, Runnable {

    private final Socket           socket;
    private final LobbyManager     lobbyManager;
    private final ObjectMapper     mapper    = new ObjectMapper();
    private final PingPongManager heartbeat = new PingPongManager();

    private volatile PrintWriter  out;
    private volatile String       nickname;       // set on successful login
    private volatile boolean      running = true;

    // Per-client async send queue: VirtualView methods enqueue JSON and return immediately.
    // The senderThread drains the queue and writes to the socket without holding any game lock.
    private final BlockingQueue<String> sendQueue = new LinkedBlockingQueue<>(256);
    private volatile Thread senderThread;

    /**
     * Creates a handler for a newly accepted client connection.
     *
     * @param socket       the accepted TCP socket
     * @param lobbyManager the shared lobby manager
     */
    public SocketClientHandler(Socket socket, LobbyManager lobbyManager) {
        this.socket      = socket;
        this.lobbyManager = lobbyManager;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  MAIN READ LOOP
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Main entry point for the per-client thread: opens streams, starts the sender thread
     * and heartbeat, then runs the read loop until the connection closes.
     */
    @Override
    public void run() {
        try {
            out = new PrintWriter(
                    new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8")), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), "UTF-8"));

            System.out.println("[SocketHandler] Client connected: " + socket.getRemoteSocketAddress());

            senderThread = new Thread(this::senderLoop,
                    "sender-" + socket.getRemoteSocketAddress());
            senderThread.setDaemon(true);
            senderThread.start();

            heartbeat.start(
                    () -> send(new NetworkMessage(MessageType.PING)),
                    () -> handleDisconnect("heartbeat timeout")
            );

            String line;
            while (running && (line = in.readLine()) != null) {
                try {
                    dispatch(mapper.readValue(line, NetworkMessage.class));
                } catch (Exception e) {
                    System.err.println("[SocketHandler] Parse error: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            if (running) {
                System.err.println("[SocketHandler] Connection lost for "
                        + (nickname != null ? nickname : "unknown") + ": " + e.getMessage());
            }
        } finally {
            handleDisconnect("connection closed");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  ASYNC SENDER LOOP
    // ─────────────────────────────────────────────────────────────────────

    /** Drains the send queue and writes each JSON line to the socket. Runs on a dedicated daemon thread. */
    private void senderLoop() {
        try {
            while (running) {
                String json = sendQueue.poll(5, TimeUnit.SECONDS);
                if (json == null) continue; // idle timeout — re-check running
                out.println(json);
                if (out.checkError()) throw new IOException("PrintWriter error");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("[SocketHandler] Sender error for "
                    + (nickname != null ? nickname : "unknown") + ": " + e.getMessage());
            handleDisconnect("send failure");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  MESSAGE DISPATCH  (Client → Server)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Routes an incoming client message to the appropriate {@link LobbyManager} method.
     *
     * @param msg the deserialized message received from the client
     */
    private void dispatch(NetworkMessage msg) {
        MessageType type;
        try {
            type = MessageType.valueOf(msg.getType());
        } catch (IllegalArgumentException e) {
            System.err.println("[SocketHandler] Unknown message type: " + msg.getType());
            return;
        }

        switch (type) {
            case LOGIN_FIRST -> lobbyManager.createLobby(
                    msg.str("nickname"), msg.num("numPlayers"), this);

            case LOGIN_TO_LOBBY -> lobbyManager.joinSpecificLobby(
                    msg.str("nickname"), msg.num("lobbyId"), this);

            case REQUEST_LOBBY_LIST -> {
                lobbyManager.requestLobbyList(this);
            }
            /*
            // === SPECTATOR ===
            case JOIN_AS_SPECTATOR -> {
                String spectatorNick = msg.str("nickname");
                this.nickname = spectatorNick; // capture so handleDisconnect works
                lobbyManager.joinAsSpectator(spectatorNick, msg.num("lobbyId"), this);
            }

            case LEAVE_SPECTATOR -> {
                if (nickname != null) {
                    lobbyManager.leaveSpectator(nickname, this);
                    this.nickname = null; // no longer associated with a game
                }
            }
            // === END SPECTATOR ===

             */

            case PLACE_TOTEM -> {
                String letter = msg.str("letter");
                if (letter != null && !letter.isEmpty() && nickname != null) {
                    lobbyManager.placeTotem(nickname, letter.charAt(0));
                }
            }

            case PICK_CARD -> {
                if (nickname != null) {
                    lobbyManager.pickCard(nickname, msg.num("cardIndex"), msg.bool("fromTop"));
                }
            }

            case PONG -> heartbeat.receivedPong();

            case PING -> heartbeat.receivedPing(() -> send(new NetworkMessage(MessageType.PONG)));

            default -> System.err.println("[SocketHandler] Unhandled type: " + type);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  VirtualView implementation  (Server → Client callbacks)
    // ─────────────────────────────────────────────────────────────────────

    /** {@inheritDoc} */
    @Override
    public void onLoginAccepted(String nick, int expectedPlayers) throws Exception {
        this.nickname = nick;   // capture so PLACE_TOTEM / PICK_CARD can be routed
        send(new NetworkMessage(MessageType.ON_LOGIN_ACCEPTED,
                Map.of("nickname", nick, "expectedPlayers", expectedPlayers)));
    }

    /** {@inheritDoc} */
    @Override
    public void onPlayerJoined(String nick, int currentCount, int expected) throws Exception {
        send(new NetworkMessage(MessageType.ON_PLAYER_JOINED,
                Map.of("nickname", nick, "currentCount", currentCount, "expected", expected)));
    }

    /** {@inheritDoc} */
    @Override
    public void onGameStarting(List<String> playerNicknames) throws Exception {
        send(new NetworkMessage(MessageType.ON_GAME_STARTING,
                Map.of("playerNicknames", playerNicknames)));
    }

    /** {@inheritDoc} */
    @Override
    public void onError(String message) throws Exception {
        send(new NetworkMessage(MessageType.ON_ERROR, Map.of("message", message)));
    }

    /** {@inheritDoc} */
    @Override
    public void onNoLobbyAvailable() throws Exception {
        send(new NetworkMessage(MessageType.ON_NO_LOBBY_AVAILABLE));
    }

    /** {@inheritDoc} */
    @Override
    public void onLobbyList(List<LobbyManager.LobbyInfo> lobbies) throws Exception {
        List<Map<String, Object>> list = lobbies.stream()
                .map(l -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", l.id());
                    m.put("currentPlayers", l.currentPlayers());
                    m.put("expectedPlayers", l.expectedPlayers());
                    m.put("inProgress", l.inProgress());
                    return m;
                })
                .toList();
        send(new NetworkMessage(MessageType.ON_LOBBY_LIST, Map.of("lobbies", list)));
    }

    /** {@inheritDoc} */
    @Override
    public void onTurnSnapshot(String currentPlayerNick, String boardSummary) throws Exception {
        send(new NetworkMessage(MessageType.ON_TURN_SNAPSHOT,
                Map.of("currentPlayerNick", currentPlayerNick, "boardSummary", boardSummary)));
    }

    /** {@inheritDoc} */
    @Override
    public void onYourTurn(String nick, GameState phase, String extraInfo) throws Exception {
        send(new NetworkMessage(MessageType.ON_YOUR_TURN,
                Map.of("nickname", nick, "phase", phase.name(), "extraInfo", extraInfo)));
    }

    /** {@inheritDoc} */
    @Override
    public void onTotemPlaced(String nick, String boardSpaceId) throws Exception {
        send(new NetworkMessage(MessageType.ON_TOTEM_PLACED,
                Map.of("nickname", nick, "boardSpaceId", boardSpaceId)));
    }

    /** {@inheritDoc} */
    @Override
    public void onInvalidAction(String nicknameTarget, String errorMessage) throws Exception {
        send(new NetworkMessage(MessageType.ON_INVALID_ACTION,
                Map.of("nicknameTarget", nicknameTarget, "errorMessage", errorMessage)));
    }

    /** {@inheritDoc} */
    @Override
    public void onCardTaken(String nick, String cardId) throws Exception {
        send(new NetworkMessage(MessageType.ON_CARD_TAKEN,
                Map.of("nickname", nick, "cardId", cardId)));
    }

    /** {@inheritDoc} */
    @Override
    public void onPlayerUpdated(String nick) throws Exception {
        send(new NetworkMessage(MessageType.ON_PLAYER_UPDATED, Map.of("nickname", nick)));
    }

    /** {@inheritDoc} */
    @Override
    public void onTurnOrderUpdated(List<String> ordered) throws Exception {
        send(new NetworkMessage(MessageType.ON_TURN_ORDER_UPDATED, Map.of("ordered", ordered)));
    }

    /** {@inheritDoc} */
    @Override
    public void onEventResolved(String eventName, String details) throws Exception {
        send(new NetworkMessage(MessageType.ON_EVENT_RESOLVED,
                Map.of("eventName", eventName, "details", details)));
    }

    /** {@inheritDoc} */
    @Override
    public void onBoardUpdated() throws Exception {
        send(new NetworkMessage(MessageType.ON_BOARD_UPDATED));
    }

    /** {@inheritDoc} */
    @Override
    public void onNewEraStarted(Age newEra) throws Exception {
        send(new NetworkMessage(MessageType.ON_NEW_ERA_STARTED,
                Map.of("era", newEra.name())));
    }

    /** {@inheritDoc} */
    @Override
    public void onGameOver(String results) throws Exception {
        send(new NetworkMessage(MessageType.ON_GAME_OVER, Map.of("results", results)));
    }

    /** {@inheritDoc} */
    @Override
    public void onRankingData(int myRank, int totalEntries,
                              List<RankingEntry> fullRanking) throws Exception {
        List<Map<String, Object>> list = fullRanking.stream()
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("rank",       e.rank());
                    m.put("nickname",   e.nickname());
                    m.put("score",      e.score());
                    m.put("date",       e.date().toString()); // ISO-8601: "2025-05-14"
                    m.put("numPlayers", e.numPlayers());
                    return m;
                })
                .collect(Collectors.toList());
        send(new NetworkMessage(MessageType.ON_RANKING_DATA,
                Map.of("myRank", myRank, "totalEntries", totalEntries, "ranking", list)));
    }

    /** {@inheritDoc} */
    @Override
    public void onPlayerDisconnected(String disconnectedNick) throws Exception {
        send(new NetworkMessage(MessageType.ON_PLAYER_DISCONNECTED,
                Map.of("nickname", disconnectedNick)));
    }

    /** {@inheritDoc} */
    @Override
    public void onPlayerReplacedByBot(String nickname) throws Exception {
        send(new NetworkMessage(MessageType.ON_PLAYER_REPLACED_BY_BOT,
                Map.of("nickname", nickname)));
    }
    /*
    // === SPECTATOR ===

    /** {@inheritDoc}
    @Override
    public void onSpectatorJoined(String currentPlayerNick, String boardSummary) throws Exception {
        send(new NetworkMessage(MessageType.ON_SPECTATOR_JOINED,
                Map.of("currentPlayerNick", currentPlayerNick, "boardSummary", boardSummary)));
    }
    // === END SPECTATOR ===

     */


    // ─────────────────────────────────────────────────────────────────────
    //  SEND  (non-blocking — enqueues JSON for the senderLoop thread)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Serialises {@code msg} to JSON and enqueues it for the sender thread (non-blocking).
     * Drops the message if the queue is full and logs a warning.
     *
     * @param msg the message to enqueue
     */
    private void send(NetworkMessage msg) {
        try {
            String json = mapper.writeValueAsString(msg);
            if (running && !sendQueue.offer(json)) {
                System.err.println("[SocketHandler] Send queue full for "
                        + (nickname != null ? nickname : "unknown") + " — dropping message");
            }
        } catch (Exception e) {
            System.err.println("[SocketHandler] Serialization error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  DISCONNECTION (idempotent)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Idempotent disconnect handler: stops the heartbeat, closes the socket, clears the send queue,
     * and notifies LobbyManager.
     *
     * @param reason human-readable explanation of why the connection is being closed
     */
    private void handleDisconnect(String reason) {
        String nick;
        synchronized (this) {
            if (!running) return;
            running = false;
            heartbeat.stop();
            try { socket.close(); } catch (IOException ignored) {}
            // Interrupt the sender thread so it exits its poll() immediately
            // instead of waiting up to 5 seconds for the next idle timeout.
            if (senderThread != null) senderThread.interrupt();
            sendQueue.clear();
            System.out.println("[SocketHandler] Disconnected: "
                    + (nickname != null ? nickname : "unknown") + " — " + reason);
            nick = nickname;
        }
        // LobbyManager is called outside the lock to avoid a deadlock cycle:
        // senderLoop thread: SocketClientHandler → LobbyManager → GameController
        // read loop thread:  LobbyManager → GameController → SocketClientHandler
        // Releasing the lock before calling LobbyManager breaks the cycle.
        if (nick != null) {
            lobbyManager.handleDisconnect(nick);
        }
    }
}