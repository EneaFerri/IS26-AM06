package it.polimi.ingsw.view;

import it.polimi.ingsw.model.enums.Age;
import java.util.List;

/**
 * Interfaccia Observer lato client.
 * Implementata da CLIView (e in futuro GUIView).
 * Riceve notifiche da ClientModel dopo che il messaggio è arrivato via rete.
 */
public interface ModelObserver {

    // --- LOBBY & SETUP ---
    void onLoginAccepted(String nickname, int expectedPlayers);
    void onPlayerJoined(String nickname, int currentCount, int expected);
    void onGameStarting(List<String> playerNicknames);
    void onError(String message);

    // --- FASE 1: PIAZZAMENTO TOTEM ---
    void onTotemPlaced(String nickname, String boardSpaceId);
    void onInvalidAction(String nicknameTarget, String errorMessage);

    // --- FASE 2: SELEZIONE CARTE ---
    void onCardTaken(String nickname, String cardId);
    void onPlayerUpdated(String nickname);

    // --- FINE TURNO GIOCATORE ---
    void onTurnOrderUpdated(List<String> newOrderedNicknames);

    // --- FINE ROUND & EVENTI ---
    void onEventResolved(String eventName, String resultDetails);
    void onBoardUpdated();
    void onNewEraStarted(Age newEra);

    // --- FINE PARTITA ---
    void onGameOver();
}