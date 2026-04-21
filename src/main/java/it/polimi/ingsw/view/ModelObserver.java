package it.polimi.ingsw.view;

import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;

import java.util.List;

/**
 * Interfaccia Observer lato client.
 * Implementata da CLIView (e in futuro GUIView).
 * Riceve notifiche da ClientModel.
 */
public interface ModelObserver {

    // --- LOBBY & SETUP ---
    void onLoginAccepted(String nickname, int expectedPlayers);
    void onPlayerJoined(String nickname, int currentCount, int expected);
    void onGameStarting(List<String> playerNicknames);
    void onError(String message);

    // --- TURNO ---
    /**
     * Chiamato SOLO sul client del giocatore che deve agire.
     * @param nickname  il giocatore che deve agire (= this client)
     * @param phase     la fase corrente
     * @param extraInfo riepilogo leggibile delle opzioni (spazi liberi / carte disponibili)
     */
    void onYourTurn(String nickname, GameState phase, String extraInfo);

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
    void onGameOver(String results);
}