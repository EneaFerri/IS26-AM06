package it.polimi.ingsw.view.cli;

import it.polimi.ingsw.view.ModelObserver;

import java.util.List;

/**
 * View CLI lato client.
 * Implementa ModelObserver: viene notificata dal ClientModel ad ogni
 * aggiornamento e stampa le informazioni su terminale.
 *
 * Non conosce nulla di RMI né di rete.
 * Corrisponde a CLIView nell'esempio dei prof.
 */
public class CLIView implements ModelObserver {

    @Override
    public void onLoginAccepted(String nickname, int expectedPlayers) {
        System.out.println("\n✓ Login accettato! Benvenuto, " + nickname);
        System.out.println("  In attesa di " + expectedPlayers + " giocatori per iniziare...");
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
        System.out.println("║ Giocatori:                        ║");
        for (String name : playerNicknames) {
            System.out.printf("║   - %-29s ║%n", name);
        }
        System.out.println("╚══════════════════════════════════╝");
    }

    @Override
    public void onError(String message) {
        System.err.println("\n[ERRORE] " + message);
    }
}
