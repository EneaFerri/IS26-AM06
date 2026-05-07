package it.polimi.ingsw.network.socket.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.network.GameServerProxy;
import it.polimi.ingsw.network.utils.message.MessageType;
import it.polimi.ingsw.network.utils.message.NetworkMessage;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Client-side proxy that implements {@link GameServerProxy} over a TCP socket.
 *
 * Each method call translates into a JSON {@link NetworkMessage} written
 * to the server's input stream (newline-delimited).
 *
 * Also exposes {@code ping()} and {@code pong()} used by the heartbeat manager.
 */
public class SocketServerProxy implements GameServerProxy {

    private final PrintWriter  out;
    private final ObjectMapper mapper = new ObjectMapper();

    public SocketServerProxy(PrintWriter out) {
        this.out = out;
    }

    // ── Login ──────────────────────────────────────────────────────────────

    @Override
    public void loginFirstPlayer(String nickname, int numPlayers) throws Exception {
        send(new NetworkMessage(MessageType.LOGIN_FIRST,
                Map.of("nickname", nickname, "numPlayers", numPlayers)));
    }

    @Override
    public void login(String nickname) throws Exception {
        send(new NetworkMessage(MessageType.LOGIN, Map.of("nickname", nickname)));
    }

    @Override
    public void loginToLobby(String nickname, int lobbyId) throws Exception {
        send(new NetworkMessage(MessageType.LOGIN_TO_LOBBY,
                Map.of("nickname", nickname, "lobbyId", lobbyId)));
    }

    @Override
    public void requestLobbyList() throws Exception {
        send(new NetworkMessage(MessageType.REQUEST_LOBBY_LIST));
    }

    // ── Game actions ───────────────────────────────────────────────────────

    @Override
    public void placeTotem(String nickname, char boardSpaceLetter) throws Exception {
        send(new NetworkMessage(MessageType.PLACE_TOTEM,
                Map.of("nickname", nickname, "letter", String.valueOf(boardSpaceLetter))));
    }

    @Override
    public void pickCard(String nickname, int cardIndex, boolean fromTop) throws Exception {
        send(new NetworkMessage(MessageType.PICK_CARD,
                Map.of("nickname", nickname, "cardIndex", cardIndex, "fromTop", fromTop)));
    }

    // === SPECTATOR ===

    @Override
    public void joinAsSpectator(String nickname, int lobbyId) throws Exception {
        send(new NetworkMessage(MessageType.JOIN_AS_SPECTATOR,
                Map.of("nickname", nickname, "lobbyId", lobbyId)));
    }

    @Override
    public void leaveSpectator(String nickname) throws Exception {
        send(new NetworkMessage(MessageType.LEAVE_SPECTATOR,
                Map.of("nickname", nickname)));
    }

    // === END SPECTATOR ===

    // ── Heartbeat ─────────────────────────────────────────────────────────

    public void ping() throws Exception {
        send(new NetworkMessage(MessageType.PING));
    }

    public void pong() throws Exception {
        send(new NetworkMessage(MessageType.PONG));
    }

    // ─────────────────────────────────────────────────────────────────────

    private synchronized void send(NetworkMessage msg) throws Exception {
        out.println(mapper.writeValueAsString(msg));
        if (out.checkError()) throw new IOException("Socket write error — connection may be closed");
    }
}