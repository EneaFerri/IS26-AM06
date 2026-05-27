package it.polimi.ingsw.view;

import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.database.RankingEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Model lato client.
 * Riceve le callback da RmiClient (via VirtualViewRmi) e notifica
 * gli observer (CLIView) tramite il pattern Observer.
 * Non conosce nulla di RMI.
 */
public class ClientModel {

    private String       myNickname;
    private int          expectedPlayers;
    private List<String> lobbyPlayers = new ArrayList<>();

    private final List<ModelObserver> observers = new ArrayList<>();

    public void registerObserver(ModelObserver observer) {
        observers.add(observer);
    }

    // --- LOBBY & SETUP ---

    public void onLoginAccepted(String nickname, int expected) {
        this.myNickname = nickname;
        this.expectedPlayers = expected;
        observers.forEach(o -> o.onLoginAccepted(nickname, expected));
    }

    public void onPlayerJoined(String nickname, int currentCount, int expected) {
        if (!lobbyPlayers.contains(nickname)) lobbyPlayers.add(nickname);
        observers.forEach(o -> o.onPlayerJoined(nickname, currentCount, expected));
    }

    public void onGameStarting(List<String> playerNicknames) {
        this.lobbyPlayers = new ArrayList<>(playerNicknames);
        observers.forEach(o -> o.onGameStarting(playerNicknames));
    }

    public void onError(String message) {
        observers.forEach(o -> o.onError(message));
    }

    public void onNoLobbyAvailable()                               { observers.forEach(o -> o.onNoLobbyAvailable()); }
    public void onLobbyList(List<LobbyManager.LobbyInfo> lobbies)  { observers.forEach(o -> o.onLobbyList(lobbies)); }

    // --- TURNO ---

    public void onTurnSnapshot(String currentPlayerNick, String boardSummary) {
        observers.forEach(o -> o.onTurnSnapshot(currentPlayerNick, boardSummary));
    }

    public void onYourTurn(String nickname, GameState phase, String extraInfo) {
        observers.forEach(o -> o.onYourTurn(nickname, phase, extraInfo));
    }

    // --- FASE 1: PIAZZAMENTO TOTEM ---

    public void onTotemPlaced(String nickname, String boardSpaceId) {
        observers.forEach(o -> o.onTotemPlaced(nickname, boardSpaceId));
    }

    public void onInvalidAction(String nicknameTarget, String errorMessage) {
        observers.forEach(o -> o.onInvalidAction(nicknameTarget, errorMessage));
    }

    // --- FASE 2: SELEZIONE CARTE ---

    public void onCardTaken(String nickname, String cardId) {
        observers.forEach(o -> o.onCardTaken(nickname, cardId));
    }

    public void onPlayerUpdated(String nickname) {
        observers.forEach(o -> o.onPlayerUpdated(nickname));
    }

    // --- FINE TURNO GIOCATORE ---

    public void onTurnOrderUpdated(List<String> newOrderedNicknames) {
        observers.forEach(o -> o.onTurnOrderUpdated(newOrderedNicknames));
    }

    // --- FINE ROUND & EVENTI ---

    public void onEventResolved(String eventName, String resultDetails) {
        observers.forEach(o -> o.onEventResolved(eventName, resultDetails));
    }

    public void onBoardUpdated() {
        observers.forEach(o -> o.onBoardUpdated());
    }

    public void onNewEraStarted(Age newEra) {
        observers.forEach(o -> o.onNewEraStarted(newEra));
    }

    // --- FINE PARTITA ---

    public void onGameOver(String results) {
        observers.forEach(o -> o.onGameOver(results));
    }

    // --- CLASSIFICA DB ---

    public void onRankingData(int myRank, int totalEntries, List<RankingEntry> fullRanking) {
        observers.forEach(o -> o.onRankingData(myRank, totalEntries, fullRanking));
    }

    // --- DISCONNESSIONE ---

    public void onPlayerDisconnected(String nickname) {
        observers.forEach(o -> o.onPlayerDisconnected(nickname));
    }

    public void onPlayerReplacedByBot(String nickname) {
        observers.forEach(o -> o.onPlayerReplacedByBot(nickname));
    }

    // === SPECTATOR ===
    public void onSpectatorJoined(String currentPlayerNick, String boardSummary) {
        observers.forEach(o -> o.onSpectatorJoined(currentPlayerNick, boardSummary));
    }
    // === END SPECTATOR ===

    // --- SERVER CRASH & RECONNECT ---
    public void onWaitingForServer(String message) {
        observers.forEach(o -> o.onWaitingForServer(message));
    }
    public void onServerReconnected() {
        observers.forEach(o -> o.onServerReconnected());
    }

    // --- Getters ---
    public String       getMyNickname()      { return myNickname; }
    public int          getExpectedPlayers() { return expectedPlayers; }
    public List<String> getLobbyPlayers()    { return List.copyOf(lobbyPlayers); }
}