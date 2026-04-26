package it.polimi.ingsw.view.cli;

import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.network.rmi.client.VirtualServerRmi;
import it.polimi.ingsw.network.rmi.client.RmiClient;
import it.polimi.ingsw.view.ModelObserver;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * View CLI lato client.
 *
 * ARCHITETTURA INPUT/OUTPUT
 * ─────────────────────────
 * Le callback RMI arrivano su thread separati (pool RMI).
 * L'input utente blocca il thread — per non bloccare il thread RMI,
 * ogni onYourTurn lancia un thread di input dedicato.
 *
 * Sincronizzazione tramite LinkedBlockingQueue<String> (capacità 1):
 *   • thread di input  → invia al server, poi chiama actionResultQueue.take()
 *   • onTotemPlaced    → pone "OK"           → sblocca il loop (azione OK)
 *   • onCardTaken      → pone "OK"           → sblocca il loop (azione OK)
 *   • onInvalidAction  → pone "RETRY:<msg>"  → sblocca il loop (riprova)
 */
public class CLIView implements ModelObserver {

    private VirtualServerRmi server;
    private RmiClient        client;

    private final Scanner scanner = new Scanner(System.in);

    /** "OK" = azione accettata | "RETRY:<msg>" = azione rifiutata */
    private final LinkedBlockingQueue<String> actionResultQueue = new LinkedBlockingQueue<>(1);

    private volatile String myNick = "";

    // ─────────────────────────────────────────────────────────────────────

    public void setServer(VirtualServerRmi server, RmiClient client) {
        this.server = server;
        this.client = client;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  LOBBY & SETUP
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void onLoginAccepted(String nickname, int expectedPlayers) {
        myNick = nickname;
        printBanner("LOGIN ACCETTATO!");
        System.out.printf("  Benvenuto, %s%n", nickname);
        System.out.printf("  In attesa di %d giocatori...%n", expectedPlayers);
        printLine();
    }

    @Override
    public void onPlayerJoined(String nickname, int currentCount, int expected) {
        System.out.printf("  [Lobby %d/%d]  %s si è unito.%n", currentCount, expected, nickname);
    }

    @Override
    public void onGameStarting(List<String> playerNicknames) {
        printBanner("INIZIO PARTITA!");
        for (String name : playerNicknames) System.out.printf("   ▸ %s%n", name);
        printLine();
    }

    @Override
    public void onError(String message) {
        System.err.println("  ✗ ERRORE: " + message);
    }

    @Override
    public void onNoLobbyAvailable() {

    }

    @Override
    public void onLobbyList(List<LobbyManager.LobbyInfo> lobbies) {

    }

    // ─────────────────────────────────────────────────────────────────────
    //  TURNO
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void onYourTurn(String nickname, GameState phase, String extraInfo) {
        System.out.println();
        printBanner("★ TOCCA A TE, " + nickname.toUpperCase() + "!");

        Thread t = new Thread(() -> {
            try {
                if (phase == GameState.OFFER_SPACE_CHOOSE) {
                    runPlaceTotemLoop(nickname, extraInfo);
                } else if (phase == GameState.PICKING_CARD) {
                    runPickCardLoop(nickname, extraInfo);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "input-thread");
        t.setDaemon(true);
        t.start();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  LOOP FASE 1 — piazzamento totem
    // ─────────────────────────────────────────────────────────────────────

    private void runPlaceTotemLoop(String nickname, String extraInfo) throws InterruptedException {
        System.out.println(extraInfo);

        while (true) {
            System.out.print("\n  Lettera spazio [es. B]: ");
            String raw = scanner.nextLine().trim().toUpperCase();

            if (raw.length() != 1 || !Character.isLetter(raw.charAt(0))) {
                System.out.println("  Inserisci una singola lettera.");
                continue;
            }

            try {
                server.placeTotem(nickname, raw.charAt(0));
            } catch (RemoteException e) {
                System.err.println("  Errore di rete: " + e.getMessage() + " — riprova.");
                continue;
            }

            // Aspetta la risposta del server (bloccante — arriva da onTotemPlaced o onInvalidAction)
            String result = actionResultQueue.take();
            if (result.equals("OK")) return;

            // RETRY:<messaggio>
            System.out.println("  ✗ " + (result.startsWith("RETRY:") ? result.substring(6) : result));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  LOOP FASE 2 — selezione carte
    // ─────────────────────────────────────────────────────────────────────

    private void runPickCardLoop(String nickname, String extraInfo) throws InterruptedException {
        System.out.println(extraInfo);

        while (true) {
            boolean hasTop = extraInfo.contains("Riga SUPERIORE:");
            boolean hasBot = extraInfo.contains("Riga INFERIORE:");

            if (!hasTop && !hasBot) {
                System.out.println("  Nessuna pescata disponibile — passaggio automatico.");
                return;
            }

            boolean fromTop;
            if (hasTop && hasBot) {
                System.out.print("  Riga [S=superiore / I=inferiore]: ");
                String r = scanner.nextLine().trim().toUpperCase();
                if      (r.equals("S")) fromTop = true;
                else if (r.equals("I")) fromTop = false;
                else { System.out.println("  Digita S o I."); continue; }
            } else {
                fromTop = hasTop;
                System.out.println("  Riga " + (hasTop ? "SUPERIORE" : "INFERIORE") + " disponibile.");
            }

            System.out.print("  Indice carta: ");
            int index;
            try {
                index = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("  Inserisci un numero intero.");
                continue;
            }

            try {
                server.pickCard(nickname, index, fromTop);
            } catch (RemoteException e) {
                System.err.println("  Errore di rete: " + e.getMessage() + " — riprova.");
                continue;
            }

            String result = actionResultQueue.take();
            if (result.equals("OK")) return;
            System.out.println("  ✗ " + (result.startsWith("RETRY:") ? result.substring(6) : result));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  CALLBACK — segnalano al loop il risultato dell'azione
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void onTotemPlaced(String nickname, String boardSpaceId) {
        System.out.printf("%n  [Totem] %-15s  ▸  spazio %s%n", nickname, boardSpaceId);
        if (nickname.equals(myNick)) signal("OK");
    }

    @Override
    public void onInvalidAction(String nicknameTarget, String errorMessage) {
        if (nicknameTarget.equals(myNick)) {
            signal("RETRY:" + errorMessage);
        } else {
            System.out.printf("  [Errore] %s: %s%n", nicknameTarget, errorMessage);
        }
    }

    @Override
    public void onCardTaken(String nickname, String cardId) {
        System.out.printf("  [Carta] %-15s  ha preso: %s%n", nickname, cardId);
        if (nickname.equals(myNick)) signal("OK");
    }

    /** Invia il segnale alla queue senza bloccare (timeout 200ms). */
    private void signal(String value) {
        try {
            actionResultQueue.clear(); // scarica eventuali segnali stale
            actionResultQueue.put(value);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  ALTRI CALLBACK
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void onPlayerUpdated(String nickname) {
        // silenzioso: frequente, dettaglio visualizzato in onYourTurn
    }

    @Override
    public void onTurnOrderUpdated(List<String> ordered) {
        System.out.println();
        System.out.println("  ┌── Ordine turno ─────────────────────────────┐");
        for (int i = 0; i < ordered.size(); i++) {
            System.out.printf("  │  %d. %-43s│%n", i + 1, ordered.get(i));
        }
        System.out.println("  └─────────────────────────────────────────────┘");
    }

    @Override
    public void onEventResolved(String eventName, String resultDetails) {
        System.out.printf("  [Evento] %-22s  → %s%n", eventName, resultDetails);
    }

    @Override
    public void onBoardUpdated() {
        System.out.println("  ─── Tabellone aggiornato ───");
    }

    @Override
    public void onNewEraStarted(Age newEra) {
        System.out.println();
        printBanner("✦ NUOVA ERA: " + newEra);
    }

    @Override
    public void onGameOver(String results) {
        System.out.println();
        printBanner("PARTITA TERMINATA!");
        System.out.println("  ┌── Classifica finale ───────────────────────┐");
        if (results != null && !results.isEmpty()) {
            String[] medals = {"🥇", "🥈", "🥉"};
            String[] entries = results.split(",");
            for (int i = 0; i < entries.length; i++) {
                String[] p = entries[i].split(":");
                String name = p.length > 0 ? p[0] : "?";
                String pts  = p.length > 1 ? p[1] : "?";
                String m    = i < medals.length ? medals[i] : "  ";
                System.out.printf("  │  %s %-22s %5s pt         │%n", m, name, pts);
            }
        }
        System.out.println("  └─────────────────────────────────────────────┘");
    }

    // ─────────────────────────────────────────────────────────────────────
    //  UTILITY
    // ─────────────────────────────────────────────────────────────────────

    private void printBanner(String title) {
        String line = "═".repeat(46);
        System.out.println("╔" + line + "╗");
        System.out.printf("║  %-44s  ║%n", title);
        System.out.println("╚" + line + "╝");
    }

    private void printLine() {
        System.out.println("─".repeat(48));
    }
}