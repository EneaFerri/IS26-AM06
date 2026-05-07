package it.polimi.ingsw.view.cli;

import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.network.GameServerProxy;
import it.polimi.ingsw.view.ModelObserver;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * CLI view — transport-agnostic.
 *
 * Holds a {@link GameServerProxy} reference that abstracts away whether the
 * underlying connection is RMI or Socket.  All login UI and lobby selection
 * logic lives here (it was previously spread across RmiClient callbacks).
 *
 * ── Input/output architecture ───────────────────────────────────────────────
 * Server callbacks arrive on background threads (RMI pool or socket reader).
 * User input blocks the thread — to avoid blocking a callback thread,
 * onYourTurn spawns a dedicated input thread.
 *
 * Synchronisation via LinkedBlockingQueue<String> (capacity 1):
 *   input thread   → sends action, then calls actionResultQueue.take()
 *   onTotemPlaced  → puts "OK"          → unblocks loop (action accepted)
 *   onCardTaken    → puts "OK"          → unblocks loop
 *   onInvalidAction→ puts "RETRY:<msg>" → unblocks loop (retry)
 */
public class CLIView implements ModelObserver {

    /** Transport-agnostic server proxy — set by ClientLauncher before doLoginCli(). */
    private GameServerProxy server;

    /** Single shared Scanner — never create a second one on System.in. */
    private final Scanner scanner = new Scanner(System.in);

    /** "OK" = action accepted | "RETRY:<msg>" = action rejected */
    private final LinkedBlockingQueue<String> actionResultQueue = new LinkedBlockingQueue<>(1);

    private volatile String  myNick      = "";
    // === SPECTATOR ===
    private volatile boolean isSpectator = false;
    // === END SPECTATOR ===

    // ─────────────────────────────────────────────────────────────────────

    /** Called by ClientLauncher once the network adapter is ready. */
    public void setServer(GameServerProxy server) {
        this.server = server;
    }

    /** Exposes the shared scanner so no second Scanner is ever opened on System.in. */
    public Scanner getScanner() { return scanner; }

    // ─────────────────────────────────────────────────────────────────────
    //  LOGIN CLI  (called by ClientLauncher after setServer)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Interactive login flow. Runs on the main thread before the event loop starts.
     * Asks for nickname and whether to create or join a lobby, then sends the
     * appropriate request to the server via the GameServerProxy.
     */
    public void doLoginCli() {
        System.out.print("Inserisci il tuo nickname: ");
        myNick = scanner.nextLine().trim();

        System.out.print("Vuoi creare una nuova lobby? (s/n): ");
        if (scanner.nextLine().trim().equalsIgnoreCase("s")) {
            int numPlayers = askNumPlayers();
            try { server.loginFirstPlayer(myNick, numPlayers); }
            catch (Exception e) { System.err.println("  ✗ Errore connessione: " + e.getMessage()); }
        } else {
            try { server.requestLobbyList(); }
            catch (Exception e) { System.err.println("  ✗ Errore connessione: " + e.getMessage()); }
        }
    }

    private int askNumPlayers() {
        int n = 0;
        while (n < 2 || n > 5) {
            System.out.print("  Quanti giocatori? (2-5): ");
            try { n = Integer.parseInt(scanner.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("  Numero non valido."); }
        }
        return n;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  LOBBY & SETUP callbacks
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
        // Runs on a callback thread — spawn a new thread for blocking scanner input.
        new Thread(() -> {
            System.out.println("\n  Nessuna lobby disponibile.");
            System.out.print("  Vuoi crearne una nuova? (s/n): ");
            if (scanner.nextLine().trim().equalsIgnoreCase("s")) {
                int n = askNumPlayers();
                try { server.loginFirstPlayer(myNick, n); }
                catch (Exception e) { System.err.println("  ✗ " + e.getMessage()); }
            } else {
                System.out.println("  Arrivederci.");
                System.exit(0);
            }
        }, "lobby-create-thread").start();
    }

    @Override
    public void onLobbyList(List<LobbyManager.LobbyInfo> lobbies) {
        // Runs on a callback thread — spawn a new thread for blocking scanner input.
        new Thread(() -> {
            // === SPECTATOR: if returning from spectator mode, just show the updated list ===
            // The spectator-exit thread has already cleared isSpectator.
            // This list arrives as the server's reply to leaveSpectator().
            // Fall through to normal display.
            // === END SPECTATOR ===

            List<LobbyManager.LobbyInfo> open = lobbies.stream()
                    .filter(l -> !l.inProgress()).collect(Collectors.toList());
            List<LobbyManager.LobbyInfo> inProgress = lobbies.stream()
                    .filter(LobbyManager.LobbyInfo::inProgress).collect(Collectors.toList());

            if (open.isEmpty() && inProgress.isEmpty()) {
                onNoLobbyAvailable();
                return;
            }

            System.out.println();
            if (!open.isEmpty()) {
                System.out.println("  Lobby aperte:");
                for (int i = 0; i < open.size(); i++) {
                    System.out.printf("    [%d] %s%n", i + 1, open.get(i));
                }
            } else {
                System.out.println("  Nessuna lobby aperta.");
            }

            // === SPECTATOR ===
            if (!inProgress.isEmpty()) {
                System.out.println("  Partite in corso (solo spettatori):");
                for (int i = 0; i < inProgress.size(); i++) {
                    System.out.printf("    [s%d] %s%n", i + 1, inProgress.get(i));
                }
            }
            // === END SPECTATOR ===

            System.out.println("  Digita: numero per entrare, s+numero per guardare, 'nuova' per creare.");
            String choice = scanner.nextLine().trim().toLowerCase();
            try {
                if (choice.equals("nuova")) {
                    int n = askNumPlayers();
                    server.loginFirstPlayer(myNick, n);
                // === SPECTATOR ===
                } else if (choice.startsWith("s") && choice.length() > 1) {
                    try {
                        int sIdx = Integer.parseInt(choice.substring(1)) - 1;
                        if (sIdx >= 0 && sIdx < inProgress.size()) {
                            server.joinAsSpectator(myNick, inProgress.get(sIdx).id());
                        } else {
                            System.out.println("  Scelta non valida.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("  Formato non valido. Usa: s1, s2, ecc.");
                    }
                // === END SPECTATOR ===
                } else {
                    try {
                        int idx = Integer.parseInt(choice) - 1;
                        if (idx >= 0 && idx < open.size()) {
                            server.loginToLobby(myNick, open.get(idx).id());
                        } else if (!open.isEmpty()) {
                            server.loginToLobby(myNick, open.get(0).id());
                        } else {
                            System.out.println("  Nessuna lobby aperta disponibile.");
                        }
                    } catch (NumberFormatException e) {
                        // INVIO senza input → prima lobby aperta
                        if (!open.isEmpty()) {
                            server.loginToLobby(myNick, open.get(0).id());
                        }
                    }
                }
            } catch (Exception e) { System.err.println("  ✗ " + e.getMessage()); }
        }, "lobby-join-thread").start();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  TURNO
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Received by ALL players at the start of every turn.
     * Waiting players see the current board state + whose turn it is.
     * The active player ignores this (they receive the richer onYourTurn instead).
     */
    @Override
    public void onTurnSnapshot(String currentPlayerNick, String boardSummary) {
        if (currentPlayerNick.equals(myNick)) return; // active player: skip, onYourTurn handles it
        System.out.println();
        printBanner("⟳ Turno di " + currentPlayerNick.toUpperCase());
        System.out.println(boardSummary);
        System.out.println("  (In attesa che " + currentPlayerNick + " concluda il suo turno...)");
        printLine();
    }

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
            } catch (Exception e) {
                System.err.println("  Errore di rete: " + e.getMessage() + " — riprova.");
                continue;
            }

            // Aspetta la risposta del server (timeout 30s per evitare blocco permanente)
            String result = actionResultQueue.poll(30, TimeUnit.SECONDS);
            if (result == null) {
                System.out.println("  ✗ Nessuna risposta dal server (timeout). Riprova.");
                continue;
            }
            if (result.equals("OK")) return;

            // RETRY:<messaggio>
            System.out.println("  ✗ " + (result.startsWith("RETRY:") ? result.substring(6) : result));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  LOOP FASE 2 — selezione carte
    // ─────────────────────────────────────────────────────────────────────

    /**
     * The extraInfo string uses two machine-readable markers injected by the server:
     *   ##HAS_TOP##   — player has remaining top-row picks
     *   ##HAS_BOT##   — player has remaining bottom-row picks
     * These are appended by GameController.buildExtraInfo() and stripped before display.
     */
    private void runPickCardLoop(String nickname, String extraInfo) throws InterruptedException {
        boolean hasTop = extraInfo.contains("##HAS_TOP##");
        boolean hasBot = extraInfo.contains("##HAS_BOT##");

        // Strip machine markers before printing
        String display = extraInfo.replace("##HAS_TOP##", "").replace("##HAS_BOT##", "");
        System.out.println(display);

        while (true) {
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
            } catch (Exception e) {
                System.err.println("  Errore di rete: " + e.getMessage() + " — riprova.");
                continue;
            }

            String result = actionResultQueue.poll(30, TimeUnit.SECONDS);
            if (result == null) {
                System.out.println("  ✗ Nessuna risposta dal server (timeout). Riprova.");
                continue;
            }
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

    /** Invia il segnale alla queue senza mai bloccare il callback thread. */
    private void signal(String value) {
        actionResultQueue.clear();   // scarica eventuali segnali stale
        actionResultQueue.offer(value); // non-blocking: se piena (caso anomalo) scarta silenziosamente
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

    @Override
    public void onPlayerDisconnected(String nickname) {
        System.out.println();
        printBanner("⚠ DISCONNESSIONE: " + nickname.toUpperCase());
        System.out.println("  Il giocatore " + nickname + " si è disconnesso.");
        System.out.println("  La partita potrebbe non poter continuare.");
        printLine();
    }

    // === SPECTATOR ===

    @Override
    public void onSpectatorJoined(String currentPlayerNick, String boardSummary) {
        isSpectator = true;
        printBanner("MODALITÀ SPETTATORE");
        System.out.println("  Stai guardando la partita in sola lettura. Nessuna azione disponibile.");
        System.out.println("  Turno corrente: " + currentPlayerNick);
        System.out.println(boardSummary);
        printLine();
        System.out.println("  Digita 'esci' per tornare alla schermata lobby.");

        // Listens for "esci" on a separate thread so normal callbacks keep printing
        Thread exitThread = new Thread(() -> {
            while (isSpectator) {
                String input = scanner.nextLine().trim().toLowerCase();
                if (input.equals("esci")) {
                    isSpectator = false;
                    try {
                        server.leaveSpectator(myNick);
                        // Server replies with onLobbyList, which triggers the normal lobby flow
                    } catch (Exception e) {
                        System.err.println("  ✗ Errore uscita spettatore: " + e.getMessage());
                    }
                    return;
                }
                System.out.println("  (Solo spettatore — digita 'esci' per tornare alla lobby.)");
            }
        }, "spectator-exit-thread");
        exitThread.setDaemon(true);
        exitThread.start();
    }

    // === END SPECTATOR ===

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