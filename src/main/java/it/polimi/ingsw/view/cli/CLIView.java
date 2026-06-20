package it.polimi.ingsw.view.cli;

import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.network.GameServerProxy;
import it.polimi.ingsw.database.RankingEntry;
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

    private volatile String  myNick           = "";
    private volatile Thread  inputThread      = null;
    private          int     savedNumPlayers  = 2;
    // === SPECTATOR ===
    private volatile boolean isSpectator = false;
    // === END SPECTATOR ===

    private volatile int  cachedWidth   = -1;
    private volatile long widthCachedAt = 0;

    // ── Colori ANSI (disabilitati — testo plain) ────────────────────────────
    private static final String RST  = "";
    private static final String CYN  = "";
    private static final String BCYN = "";
    private static final String YLW  = "";
    private static final String BYLW = "";
    private static final String GRN  = "";
    private static final String BGRN = "";
    private static final String MGT  = "";
    private static final String GRY  = "";

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
        savedNumPlayers = expectedPlayers;
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
        System.out.println(stripMachineBlock(boardSummary));
        System.out.println("  (In attesa che " + currentPlayerNick + " concluda il suo turno...)");
        printLine();
    }

    @Override
    public void onYourTurn(String nickname, GameState phase, String extraInfo) {
        System.out.println();
        printBanner("★ TOCCA A TE, " + nickname.toUpperCase() + "!");

        // Interrompe l'eventuale thread di input del turno precedente
        Thread prev = inputThread;
        if (prev != null) prev.interrupt();

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
        inputThread = t;
        t.start();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  LOOP FASE 1 — piazzamento totem
    // ─────────────────────────────────────────────────────────────────────

    private void runPlaceTotemLoop(String nickname, String extraInfo) throws InterruptedException {
        System.out.println(stripMachineBlock(extraInfo));

        while (true) {
            System.out.print("\n  Lettera spazio [es. B]: ");
            String raw;
            try { raw = scanner.nextLine().trim().toUpperCase(); }
            catch (Exception e) { Thread.currentThread().interrupt(); return; }

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
        display = stripMachineBlock(display);
        System.out.println(display);

        while (true) {
            if (!hasTop && !hasBot) {
                System.out.println("  Nessuna pescata disponibile — passaggio automatico.");
                return;
            }

            boolean fromTop;
            if (hasTop && hasBot) {
                System.out.print("  Riga [S=superiore / I=inferiore]: ");
                String r;
                try { r = scanner.nextLine().trim().toUpperCase(); }
                catch (Exception e) { Thread.currentThread().interrupt(); return; }
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
                String rawIdx = scanner.nextLine().trim();
                index = Integer.parseInt(rawIdx);
            } catch (NumberFormatException e) {
                System.out.println("  Inserisci un numero intero.");
                continue;
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                return;
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
        int w = terminalWidth();
        System.out.println();
        System.out.println(boxTop(w, "── ⟳ Ordine turno "));
        for (int i = 0; i < ordered.size(); i++) {
            System.out.println(boxLine(w, (i + 1) + ". " + ordered.get(i)));
        }
        System.out.println(boxBottom(w));
    }

    @Override
    public void onEventResolved(String eventName, String resultDetails) {
        System.out.printf("  [Evento] %-22s  → %s%n", eventName, resultDetails);
    }

    @Override
    public void onBoardUpdated() {
        int w = terminalWidth();
        String label = " ◉ Tabellone aggiornato ";
        int half = (w - label.length()) / 2;
        int rest = Math.max(0, w - half - label.length());
        System.out.println(CYN + "─".repeat(half) + RST + YLW + label + RST + CYN + "─".repeat(rest) + RST);
    }

    @Override
    public void onNewEraStarted(Age newEra) {
        System.out.println();
        printBanner("✦ NUOVA ERA: " + newEra);
    }

    @Override
    public void onGameOver(String results) {
        int w = terminalWidth();
        System.out.println();
        printBanner("PARTITA TERMINATA!");
        System.out.println(boxTop(w, "── ✦ Classifica finale "));
        if (results != null && !results.isEmpty()) {
            String[] medals = {"🥇", "🥈", "🥉"};
            String[] entries = results.split(",");
            for (int i = 0; i < entries.length; i++) {
                String[] p = entries[i].split(":");
                String name = p.length > 0 ? p[0] : "?";
                String pts  = p.length > 1 ? p[1] : "?";
                String m    = i < medals.length ? medals[i] : "  ";
                System.out.println(boxLine(w, String.format("%s %-22s %5s pt", m, name, pts)));
            }
        }
        System.out.println(boxBottom(w));
    }

    @Override
    public void onRankingData(int myRank, int totalEntries, List<RankingEntry> fullRanking) {
        int w = terminalWidth();
        // Totale riga = 34 + nickWidth; la colonna nickname si espande con il terminale
        int nickWidth = Math.max(24, w - 34);
        int nickCol   = nickWidth + 2; // dashes separatore = nick + 2 spazi
        System.out.println();
        int np = fullRanking.isEmpty() ? 0 : fullRanking.get(0).numPlayers();
        printBanner("CLASSIFICA GLOBALE (" + np + " GIOCATORI)");
        System.out.printf("  La tua posizione: #%d su %d partite storiche%n", myRank, totalEntries);
        System.out.println(CYN + "  ┌─────┬" + "─".repeat(nickCol) + "┬────────┬────────────┐" + RST);
        System.out.printf(        "  │ " + YLW + "Pos" + RST + " │ " + YLW + "%-" + nickWidth + "s" + RST + " │ " + YLW + "Punti  " + RST + "│ " + YLW + "Data       " + RST + "│%n", "Nickname");
        System.out.println(CYN + "  ├─────┼" + "─".repeat(nickCol) + "┼────────┼────────────┤" + RST);
        for (RankingEntry e : fullRanking) {
            boolean isMe = e.nickname().equals(myNick);
            String marker = isMe ? BYLW + " ◄" + RST : "  ";
            String nick   = isMe ? BYLW + String.format("%-" + nickWidth + "s", e.nickname()) + RST
                                 : String.format("%-" + nickWidth + "s", e.nickname());
            System.out.printf("  │ %3d │ %s │ %6d │ %s%s%n",
                    e.rank(), nick, e.score(), e.date(), marker);
        }
        System.out.println(CYN + "  └─────┴" + "─".repeat(nickCol) + "┴────────┴────────────┘" + RST);
        printLine();
    }

    @Override
    public void onPlayerDisconnected(String nickname) {
        System.out.println();
        printBanner("⚠ DISCONNESSIONE: " + nickname.toUpperCase());
        System.out.println("  Il giocatore " + nickname + " si è disconnesso.");
        printLine();
    }

    @Override
    public void onPlayerReplacedByBot(String nickname) {
        System.out.println();
        printBanner("🤖 BOT ATTIVATO: " + nickname.toUpperCase());
        System.out.println("  Il giocatore '" + nickname + "' è stato disconnesso.");
        System.out.println("  Un bot continuerà la partita al suo posto.");
        printLine();
    }

    // --- SERVER CRASH & RECONNECT ---

    @Override
    public void onWaitingForServer(String message) {
        System.out.println();
        printBanner("SERVER NON RAGGIUNGIBILE");
        System.out.println("  ⚠  " + message);
        printLine();
    }

    @Override
    public void onServerReconnected() {
        printBanner("SERVER RICONNESSO!");
        System.out.println("  ✓  Rientro nella partita...");
        printLine();
        try { server.loginFirstPlayer(myNick, savedNumPlayers); }
        catch (Exception e) { System.err.println("  ✗ Errore rientro: " + e.getMessage()); }
    }

    // === SPECTATOR ===

    @Override
    public void onSpectatorJoined(String currentPlayerNick, String boardSummary) {
        isSpectator = true;
        printBanner("MODALITÀ SPETTATORE");
        System.out.println("  Stai guardando la partita in sola lettura. Nessuna azione disponibile.");
        System.out.println("  Turno corrente: " + currentPlayerNick);
        System.out.println(stripMachineBlock(boardSummary));
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

    private String stripMachineBlock(String text) {
        if (text == null) return "";
        text = stripBlock(text, "##DECK_STATUS_BEGIN##", "##DECK_STATUS_END##");
        text = stripBlock(text, "##PLAYER_CARDS_BEGIN##", "##PLAYER_CARDS_END##");
        return fitToTerminal(text);
    }

    private String stripBlock(String text, String begin, String end) {
        int start = text.indexOf(begin);
        int stop  = text.indexOf(end);
        if (start < 0 || stop < start) return text;
        return text.substring(0, start) + text.substring(stop + end.length());
    }

    private String fitToTerminal(String text) {
        int width = terminalWidth();
        StringBuilder out = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            out.append(adaptLine(line, width)).append("\n");
        }
        return out.toString();
    }

    private String adaptLine(String line, int width) {
        // Top border "  ┌── LABEL ─────┐" — espande + colora
        if (line.endsWith("┐") && line.contains("┌")) {
            int idx    = line.indexOf('┌');
            String indent = line.substring(0, idx);
            String inner  = line.substring(idx + 1, line.length() - 1);
            int lastNonDash = inner.length() - 1;
            while (lastNonDash >= 0 && inner.charAt(lastNonDash) == '─') lastNonDash--;
            String label = addSectionIcon(inner.substring(0, lastNonDash + 1));
            int targetInner = Math.max(2, width - indent.length() - 2);
            int dashes = Math.max(1, targetInner - label.length());
            return CYN + indent + "┌" + YLW + label + CYN + "─".repeat(dashes) + "┐" + RST;
        }
        // Bottom border "  └──────────────┘" — espande + colora
        if (line.endsWith("┘") && line.contains("└")) {
            int idx = line.indexOf('└');
            String indent = line.substring(0, idx);
            int targetInner = Math.max(1, width - indent.length() - 2);
            return CYN + indent + "└" + "─".repeat(targetInner) + "┘" + RST;
        }
        // Righe carte: "  │  [T]", "  │  [B]", "  │  [0]" ecc.
        if (line.length() > 7 && line.startsWith("  │  [") && line.charAt(7) == ']') {
            return colorCardLine(line);
        }
        // Intestazioni categoria personaggi/edifici
        if (line.startsWith("  │  [Personaggi]")) {
            boolean empty = line.contains("nessuna carta");
            return CYN + "  │  " + RST + (empty ? GRY : BGRN)
                   + "⚔ Personaggi" + (empty ? " — nessuna carta" : "") + RST;
        }
        if (line.startsWith("  │  [Edifici]")) {
            boolean empty = line.contains("nessuna carta");
            return CYN + "  │  " + RST + (empty ? GRY : BYLW)
                   + "▦ Edifici" + (empty ? " — nessuna carta" : "") + RST;
        }
        // Bullet carte in mano: "  │    • Artista..."
        if (line.startsWith("  │    • ")) {
            String cardName = line.substring(9);
            String c = cardName.contains("⚑") ? MGT
                     : cardName.startsWith("Edificio") ? BYLW : GRN;
            return CYN + "  │    • " + RST + c + cardName + RST;
        }
        // Altre righe con │ bordo
        if (line.startsWith("  │")) {
            return CYN + "  │" + RST + line.substring(3);
        }
        // Riga troppo lunga: tronca
        if (line.length() > width) return line.substring(0, width - 1) + "…";
        return line;
    }

    private String colorCardLine(String line) {
        int cardIdStart = line.lastIndexOf("(CardId:");
        String beforeId = cardIdStart >= 0 ? line.substring(0, cardIdStart) : line;
        String idPart   = cardIdStart >= 0 ? line.substring(cardIdStart)   : "";

        String cardColor;
        if (beforeId.contains("⚑"))       cardColor = MGT;   // evento (⚑)
        else if (beforeId.contains("[B]"))     cardColor = BYLW;  // edificio
        else                                   cardColor = GRN;   // personaggio

        if (beforeId.length() < 8)
            return CYN + "  │" + RST + beforeId.substring(3) + GRY + idPart + RST;

        String boxPfx  = beforeId.substring(0, 5);  // "  │  "
        String typeTag = beforeId.substring(5, 8);   // "[T]", "[B]", "[0]"…
        String rest    = beforeId.substring(8);      // " Inventore…" (con spazi di padding)

        return CYN + boxPfx + RST + YLW + typeTag + RST + cardColor + rest + RST
             + GRY + idPart + RST;
    }

    private String addSectionIcon(String label) {
        if (label.contains("SUPERIORE"))       return label.replace("SUPERIORE",       "↑ SUPERIORE");
        if (label.contains("INFERIORE"))       return label.replace("INFERIORE",       "↓ INFERIORE");
        if (label.contains("Spazi Offerta"))   return label.replace("Spazi Offerta",   "◉ Spazi Offerta");
        if (label.contains("Stato giocatori")) return label.replace("Stato giocatori", "◈ Stato giocatori");
        if (label.contains("Le tue carte"))    return label.replace("Le tue carte",    "◆ Le tue carte");
        return label;
    }

    // Larghezza terminale reale: stty size legge /dev/tty anche da subprocess (cache 5s)
    private int terminalWidth() {
        long now = System.currentTimeMillis();
        if (cachedWidth > 0 && now - widthCachedAt < 5_000) return cachedWidth;
        // 1) variabile d'ambiente (export COLUMNS=... la imposta esplicitamente)
        String cols = System.getenv("COLUMNS");
        if (cols != null) {
            try { return cacheWidth(Math.max(40, Integer.parseInt(cols.trim())), now); }
            catch (NumberFormatException ignored) {}
        }
        // 2) stty size </dev/tty → stampa "rows cols" leggendo il terminale reale
        try {
            Process p = new ProcessBuilder("sh", "-c", "stty size </dev/tty 2>/dev/null")
                    .redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            p.destroyForcibly();
            String[] parts = out.split("\\s+");
            if (parts.length >= 2) {
                int w = Integer.parseInt(parts[1]);
                if (w > 0) return cacheWidth(Math.max(40, w), now);
            }
        } catch (Exception ignored) {}
        // 3) tput cols come ulteriore fallback
        try {
            Process p = new ProcessBuilder("sh", "-c", "tput cols 2>/dev/null")
                    .redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            p.destroyForcibly();
            int w = Integer.parseInt(out);
            if (w > 0) return cacheWidth(Math.max(40, w), now);
        } catch (Exception ignored) {}
        return cacheWidth(80, now);
    }

    private int cacheWidth(int w, long t) { cachedWidth = w; widthCachedAt = t; return w; }

    // "  ┌{label}{dashes}┐" colorata — larghezza totale = width
    private String boxTop(int width, String label) {
        int dashes = Math.max(1, width - 4 - label.length());
        return CYN + "  ┌" + YLW + label + CYN + "─".repeat(dashes) + "┐" + RST;
    }

    // "  └{dashes}┘" colorata — larghezza totale = width
    private String boxBottom(int width) {
        return CYN + "  └" + "─".repeat(Math.max(1, width - 4)) + "┘" + RST;
    }

    // "  │  {content paddato}│" colorata — larghezza totale = width
    private String boxLine(int width, String content) {
        int fieldWidth = Math.max(1, width - 6);
        if (content.length() > fieldWidth) content = content.substring(0, fieldWidth);
        return CYN + "  │  " + RST + String.format("%-" + fieldWidth + "s", content)
             + CYN + "│" + RST;
    }

    private void printBanner(String title) {
        int w  = terminalWidth();
        int cw = Math.max(1, w - 6);
        String inner = "═".repeat(Math.max(2, w - 2));
        System.out.println(BCYN + "╔" + inner + "╗" + RST);
        System.out.println(BCYN + "║" + RST + BYLW + "  "
                + String.format("%-" + cw + "s", title) + "  " + RST + BCYN + "║" + RST);
        System.out.println(BCYN + "╚" + inner + "╝" + RST);
    }

    private void printLine() {
        System.out.println(CYN + "─".repeat(terminalWidth()) + RST);
    }
}