package it.polimi.ingsw;

import it.polimi.ingsw.model.enums.Age;
import java.util.List;

/**
 * Interfaccia technology-agnostic che il server usa per notificare i client.
 * Incorpora tutti i metodi di GameObserver con firme adatte alla rete:
 * - alcuni metodi di GameObserver sono arricchiti (es. onPlayerJoined riceve
 *   anche currentCount/expected per aggiornare la lobby)
 * - VirtualViewRmi la specializza aggiungendo Remote + RemoteException
 */
public interface VirtualView {

    // --- LOBBY & SETUP ---
    void onLoginAccepted(String nickname, int expectedPlayers)          throws Exception;
    void onPlayerJoined(String nickname, int currentCount, int expected) throws Exception;
    void onGameStarting(List<String> playerNicknames)                   throws Exception;
    void onError(String message)                                         throws Exception;

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
    void onGameOver()                                                    throws Exception;
}