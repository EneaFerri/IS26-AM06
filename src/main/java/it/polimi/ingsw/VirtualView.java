package it.polimi.ingsw;

import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;

import java.util.List;

/**
 * Interfaccia technology-agnostic che il server usa per notificare i client.
 * Il server la implementa tramite VirtualViewRmi (RMI) o VirtualViewSocket (Socket — futuro).
 */
public interface VirtualView {

    // --- LOBBY & SETUP ---
    void onLoginAccepted(String nickname, int expectedPlayers)           throws Exception;
    void onPlayerJoined(String nickname, int currentCount, int expected) throws Exception;
    void onGameStarting(List<String> playerNicknames)                    throws Exception;
    void onError(String message)                                         throws Exception;

    // --- TURNO: notifica al giocatore di turno cosa deve fare ---
    /**
     * Inviato SOLO al giocatore che deve agire adesso.
     * @param nickname  il giocatore che deve agire
     * @param phase     la fase corrente (OFFER_SPACE_CHOOSE o PICKING_CARD)
     * @param extraInfo info aggiuntiva serializzata (es. lista spazi liberi, lista carte disponibili)
     */
    void onYourTurn(String nickname, GameState phase, String extraInfo)  throws Exception;

    // --- FASE 1: PIAZZAMENTO TOTEM ---
    void onTotemPlaced(String nickname, String boardSpaceId)             throws Exception;
    void onInvalidAction(String nicknameTarget, String errorMessage)     throws Exception;

    // --- FASE 2: SELEZIONE CARTE ---
    void onCardTaken(String nickname, String cardId)                     throws Exception;
    void onPlayerUpdated(String nickname)                                throws Exception;

    // --- FINE TURNO GIOCATORE ---
    void onTurnOrderUpdated(List<String> newOrderedNicknames)            throws Exception;

    // --- FINE ROUND & EVENTI ---
    void onEventResolved(String eventName, String resultDetails)         throws Exception;
    void onBoardUpdated()                                                throws Exception;
    void onNewEraStarted(Age newEra)                                     throws Exception;

    // --- FINE PARTITA ---
    /**
     * @param results  classifica finale serializzata: "nick1:punti1,nick2:punti2,..."
     */
    void onGameOver(String results)                                      throws Exception;
}