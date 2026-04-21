package it.polimi.ingsw.network_rmi_bozza;

import java.util.List;

/**
 * View CLI lato client.
 * Riceve aggiornamenti da VirtualView e li stampa su terminale.
 * Non conosce nulla della rete.
 */
public class CliView {

    public void showLoginAccepted(String nickname, int expectedPlayers) {
        System.out.println("\n✓ Login accettato! Benvenuto, " + nickname);
        System.out.println("  In attesa di " + expectedPlayers + " giocatori per iniziare...");
    }

    public void showPlayerJoined(String nickname, int currentCount, int expected) {
        System.out.println("  [Lobby " + currentCount + "/" + expected + "] "
                + nickname + " si è unito.");
    }

    public void showGameStarting(List<String> playerNicknames) {
        System.out.println("\n╔══════════════════════════════════╗");
        System.out.println("║        INIZIO PARTITA...         ║");
        System.out.println("╠══════════════════════════════════╣");
        System.out.println("║ Giocatori:                        ║");
        for (String name : playerNicknames) {
            System.out.printf("║   - %-29s ║%n", name);
        }
        System.out.println("╚══════════════════════════════════╝");
    }

    public void showError(String message) {
        System.err.println("\n[ERRORE] " + message);
    }

    /**
     * Chiede all'utente di inserire il nickname via stdin.
     */
    public String askNickname(java.util.Scanner scanner) {
        System.out.print("Inserisci il tuo nickname: ");
        return scanner.nextLine().trim();
    }

    /**
     * Chiede quanti giocatori si vogliono per la partita (solo al primo client).
     */
    public int askNumberOfPlayers(java.util.Scanner scanner) {
        System.out.print("Sei il primo giocatore. Quanti giocatori vuoi? (2-5): ");
        while (true) {
            try {
                int n = Integer.parseInt(scanner.nextLine().trim());
                if (n >= 2 && n <= 5) return n;
                System.out.print("Inserisci un numero tra 2 e 5: ");
            } catch (NumberFormatException e) {
                System.out.print("Numero non valido. Riprova: ");
            }
        }
    }
}
