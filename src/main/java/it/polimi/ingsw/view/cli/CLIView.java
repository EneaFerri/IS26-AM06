package it.polimi.ingsw.view.cli;

import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.view.ModelObserver;
import java.util.List;

/**
 * View CLI lato client.
 * Implementa ModelObserver: stampa a terminale ogni evento di gioco.
 * Non conosce nulla di RMI né di rete.
 */
public class CLIView implements ModelObserver {

    // --- LOBBY & SETUP ---

    @Override
    public void onLoginAccepted(String nickname, int expectedPlayers) {
        System.out.println("\n✓ Login accettato! Benvenuto, " + nickname);
        System.out.println("  In attesa di " + expectedPlayers + " giocatori...");
    }

    @Override
    public void onPlayerJoined(String nickname, int currentCount, int expected) {
        System.out.println("  [Lobby " + currentCount + "/" + expected + "] "
                + nickname + " si è unito.");
    }

    @Override
    public void onGameStarting(List<String> playerNicknames) {
        System.out.println("\n╔══════════════════════════════════╗");
        System.out.println("║        INIZIO PARTITA...         ║");
        System.out.println("╠══════════════════════════════════╣");
        for (String name : playerNicknames) {
            System.out.printf("║   - %-29s ║%n", name);
        }
        System.out.println("╚══════════════════════════════════╝");
    }

    @Override
    public void onError(String message) {
        System.err.println("\n[ERRORE] " + message);
    }

    // --- FASE 1: PIAZZAMENTO TOTEM ---

    @Override
    public void onTotemPlaced(String nickname, String boardSpaceId) {
        System.out.println("[Totem] " + nickname + " ha piazzato il totem su " + boardSpaceId);
    }

    @Override
    public void onInvalidAction(String nicknameTarget, String errorMessage) {
        System.err.println("[Azione non valida] " + nicknameTarget + ": " + errorMessage);
    }

    // --- FASE 2: SELEZIONE CARTE ---

    @Override
    public void onCardTaken(String nickname, String cardId) {
        System.out.println("[Carta] " + nickname + " ha preso la carta " + cardId);
    }

    @Override
    public void onPlayerUpdated(String nickname) {
        System.out.println("[Aggiornamento] Stato di " + nickname + " aggiornato.");
    }

    // --- FINE TURNO GIOCATORE ---

    @Override
    public void onTurnOrderUpdated(List<String> newOrderedNicknames) {
        System.out.println("[Ordine turno] " + newOrderedNicknames);
    }

    // --- FINE ROUND & EVENTI ---

    @Override
    public void onEventResolved(String eventName, String resultDetails) {
        System.out.println("[Evento] " + eventName + " — " + resultDetails);
    }

    @Override
    public void onBoardUpdated() {
        System.out.println("[Board] Tabellone aggiornato.");
    }

    @Override
    public void onNewEraStarted(Age newEra) {
        System.out.println("\n>>> Nuova era: " + newEra + " <<<");
    }

    // --- FINE PARTITA ---

    @Override
    public void onGameOver() {
        System.out.println("\n╔══════════════════════════════════╗");
        System.out.println("║         PARTITA TERMINATA        ║");
        System.out.println("╚══════════════════════════════════╝");
    }
}