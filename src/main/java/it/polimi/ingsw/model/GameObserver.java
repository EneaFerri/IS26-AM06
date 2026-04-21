package it.polimi.ingsw.model;

import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;

import java.util.List;

/**
 * Observer del model (lato server).
 * Il GameController implementa questa interfaccia e riceve le notifiche da Game.
 * Le traduce poi in chiamate su VirtualView verso i client.
 */
public interface GameObserver {

    // --- LOBBY & SETUP ---
    void onPlayerJoined(String nickname);
    void onPlayerError(String message);
    void onGameStarted();

    // --- TURNO ---
    /**
     * Notifica che è il turno di un giocatore specifico.
     * Il GameController userà questa callback per chiamare onYourTurn sul client corretto.
     */
    void onTurnStarted(String nickname, GameState phase);

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