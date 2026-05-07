package it.polimi.ingsw.network.socket.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.VirtualView;
import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.network.utils.PingPongManager;
import it.polimi.ingsw.network.utils.message.MessageType;
import it.polimi.ingsw.network.utils.message.NetworkMessage;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles one Socket client connection on the server side.
 *
 * ── Responsibilities ────────────────────────────────────────────────────────
 *  1. Read loop: deserialises incoming JSON lines and dispatches them
 *     to LobbyManager (same interface used by RmiServer).
 *  2. VirtualView: serialises all server-to-client callbacks to JSON
 *     and writes them over the socket.
 *  3. Heartbeat: sends PING every 10 s; if no PONG arrives, disconnects.
 *  4. Disconnection: on any IOException or heartbeat timeout, notifies
 *     LobbyManager so the other players in the lobby are informed.
 *
 * ── Thread safety ───────────────────────────────────────────────────────────
 *  send() serialises JSON outside the lock, then sendRaw() acquires it to write.
 *  out/nickname/running are volatile for cross-thread visibility.
 *  handleDisconnect() is idempotent (guarded by `running`).
 */
public class SocketClientHandler implements VirtualView, Runnable {

    private final Socket           socket;
    private final LobbyManager     lobbyManager;
    private final ObjectMapper     mapper    = new ObjectMapper();
    private final PingPongManager heartbeat = new PingPongManager();

    private volatile PrintWriter  out;
    private volatile String       nickname;       // set on successful login
    private volatile boolean      running = true;

    public SocketClientHandler(Socket socket, LobbyManager lobbyManager) {
        this.socket      = socket;
        this.lobbyManager = lobbyManager;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  MAIN READ LOOP
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void run() {
        try {
            out = new PrintWriter(
                    new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8")), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), "UTF-8"));

            System.out.println("[SocketHandler] Client connected: " + socket.getRemoteSocketAddress());

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
    //  MESSAGE DISPATCH  (Client → Server)
    // ─────────────────────────────────────────────────────────────────────

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

            case LOGIN -> lobbyManager.joinLobby(
                    msg.str("nickname"), this);

            case LOGIN_TO_LOBBY -> lobbyManager.joinSpecificLobby(
                    msg.str("nickname"), msg.num("lobbyId"), this);

            case REQUEST_LOBBY_LIST -> {
                try { onLobbyList(lobbyManager.getActiveLobbies()); }
                catch (Exception e) {
                    System.err.println("[SocketHandler] requestLobbyList: " + e.getMessage());
                }
            }

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

    @Override
    public void onLoginAccepted(String nick, int expectedPlayers) throws Exception {
        this.nickname = nick;   // capture so PLACE_TOTEM / PICK_CARD can be routed
        send(new NetworkMessage(MessageType.ON_LOGIN_ACCEPTED,
                Map.of("nickname", nick, "expectedPlayers", expectedPlayers)));
    }

    @Override
    public void onPlayerJoined(String nick, int currentCount, int expected) throws Exception {
        send(new NetworkMessage(MessageType.ON_PLAYER_JOINED,
                Map.of("nickname", nick, "currentCount", currentCount, "expected", expected)));
    }

    @Override
    public void onGameStarting(List<String> playerNicknames) throws Exception {
        send(new NetworkMessage(MessageType.ON_GAME_STARTING,
                Map.of("playerNicknames", playerNicknames)));
    }

    @Override
    public void onError(String message) throws Exception {
        send(new NetworkMessage(MessageType.ON_ERROR, Map.of("message", message)));
    }

    @Override
    public void onNoLobbyAvailable() throws Exception {
        send(new NetworkMessage(MessageType.ON_NO_LOBBY_AVAILABLE));
    }

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

    @Override
    public void onTurnSnapshot(String currentPlayerNick, String boardSummary) throws Exception {
        send(new NetworkMessage(MessageType.ON_TURN_SNAPSHOT,
                Map.of("currentPlayerNick", currentPlayerNick, "boardSummary", boardSummary)));
    }

    @Override
    public void onYourTurn(String nick, GameState phase, String extraInfo) throws Exception {
        send(new NetworkMessage(MessageType.ON_YOUR_TURN,
                Map.of("nickname", nick, "phase", phase.name(), "extraInfo", extraInfo)));
    }

    @Override
    public void onTotemPlaced(String nick, String boardSpaceId) throws Exception {
        send(new NetworkMessage(MessageType.ON_TOTEM_PLACED,
                Map.of("nickname", nick, "boardSpaceId", boardSpaceId)));
    }

    @Override
    public void onInvalidAction(String nicknameTarget, String errorMessage) throws Exception {
        send(new NetworkMessage(MessageType.ON_INVALID_ACTION,
                Map.of("nicknameTarget", nicknameTarget, "errorMessage", errorMessage)));
    }

    @Override
    public void onCardTaken(String nick, String cardId) throws Exception {
        send(new NetworkMessage(MessageType.ON_CARD_TAKEN,
                Map.of("nickname", nick, "cardId", cardId)));
    }

    @Override
    public void onPlayerUpdated(String nick) throws Exception {
        send(new NetworkMessage(MessageType.ON_PLAYER_UPDATED, Map.of("nickname", nick)));
    }

    @Override
    public void onTurnOrderUpdated(List<String> ordered) throws Exception {
        send(new NetworkMessage(MessageType.ON_TURN_ORDER_UPDATED, Map.of("ordered", ordered)));
    }

    @Override
    public void onEventResolved(String eventName, String details) throws Exception {
        send(new NetworkMessage(MessageType.ON_EVENT_RESOLVED,
                Map.of("eventName", eventName, "details", details)));
    }

    @Override
    public void onBoardUpdated() throws Exception {
        send(new NetworkMessage(MessageType.ON_BOARD_UPDATED));
    }

    @Override
    public void onNewEraStarted(Age newEra) throws Exception {
        send(new NetworkMessage(MessageType.ON_NEW_ERA_STARTED,
                Map.of("era", newEra.name())));
    }

    @Override
    public void onGameOver(String results) throws Exception {
        send(new NetworkMessage(MessageType.ON_GAME_OVER, Map.of("results", results)));
    }

    @Override
    public void onPlayerDisconnected(String disconnectedNick) throws Exception {
        send(new NetworkMessage(MessageType.ON_PLAYER_DISCONNECTED,
                Map.of("nickname", disconnectedNick)));
    }

    // === SPECTATOR ===
    @Override
    public void onSpectatorJoined(String currentPlayerNick, String boardSummary) throws Exception {
        send(new NetworkMessage(MessageType.ON_SPECTATOR_JOINED,
                Map.of("currentPlayerNick", currentPlayerNick, "boardSummary", boardSummary)));
    }
    // === END SPECTATOR ===

    // ─────────────────────────────────────────────────────────────────────
    //  SEND  (synchronised — multiple server threads may call VirtualView)
    // ─────────────────────────────────────────────────────────────────────

    private void send(NetworkMessage msg) {
        String json;
        try {
            json = mapper.writeValueAsString(msg); // serializzazione fuori dal lock
        } catch (Exception e) {
            System.err.println("[SocketHandler] Serialization error: " + e.getMessage());
            return;
        }
        sendRaw(json);
    }

    private synchronized void sendRaw(String json) {
        if (!running || out == null) return;
        try {
            out.println(json);
            if (out.checkError()) throw new IOException("PrintWriter error");
        } catch (Exception e) {
            System.err.println("[SocketHandler] Send error to "
                    + (nickname != null ? nickname : "unknown") + ": " + e.getMessage());
            handleDisconnect("send failure");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  DISCONNECTION (idempotent)
    // ─────────────────────────────────────────────────────────────────────

    private void handleDisconnect(String reason) {
        String nick;
        synchronized (this) {
            if (!running) return;
            running = false;
            heartbeat.stop();
            try { socket.close(); } catch (IOException ignored) {}
            System.out.println("[SocketHandler] Disconnected: "
                    + (nickname != null ? nickname : "unknown") + " — " + reason);
            nick = nickname;
        }
        // LobbyManager chiamato fuori dal lock per evitare deadlock:
        // heartbeat thread: SocketClientHandler → LobbyManager → GameController
        // read loop thread: LobbyManager → GameController → SocketClientHandler
        // rilasciando il lock prima di chiamare LobbyManager si spezza il ciclo.
        if (nick != null) {
            lobbyManager.handleDisconnect(nick);
        }
    }
}