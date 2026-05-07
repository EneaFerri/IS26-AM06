package it.polimi.ingsw.view.gui;

import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.GameServerProxy;
import it.polimi.ingsw.view.ClientModel;
import it.polimi.ingsw.view.ModelObserver;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GUIView implements ModelObserver {

    private final Stage       stage;
    private final ClientModel model;
    private GameServerProxy   server;
    private String            nick;

    private List<String> players = new ArrayList<>();

    // Riferimenti agli elementi lobby che aggiorniamo live
    private VBox lobbyListBox;
    private Label lobbyStatusLabel;

    private final Map<String, TotemColor> totemColors = new java.util.HashMap<>();

    // === SPECTATOR ===
    private volatile boolean isSpectator = false;
    // === END SPECTATOR ===

    public GUIView(Stage stage, ClientModel model) {
        this.stage = stage;
        this.model = model;
    }

    public void setServer(GameServerProxy server) { this.server = server; }
    public void setNick(String nick)              { this.nick   = nick;   }

    // ─────────────────────────────────────────────────────────────────────
    //  SCHERMATA LOBBY
    // ─────────────────────────────────────────────────────────────────────

    public void showLobbyScreen() {
        AnimatedBackground animBg = new AnimatedBackground();

        // ── Header ────────────────────────────────────────────────────────
        Label title = new Label("Lobby");
        title.setStyle(styleTitle());

        Label welcome = new Label("Benvenuto, " + nick + " 👋");
        welcome.setStyle("-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:14;-fx-text-fill:rgba(255,255,255,0.50);");

        VBox header = new VBox(4, title, welcome);
        header.setAlignment(Pos.CENTER_LEFT);

        // ── Lista lobby ───────────────────────────────────────────────────
        lobbyStatusLabel = new Label("Ricerca lobby in corso…");
        lobbyStatusLabel.setStyle("-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:13;-fx-text-fill:rgba(255,255,255,0.45);");

        lobbyListBox = new VBox(10);
        lobbyListBox.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(lobbyListBox);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(220);
        scroll.setStyle("-fx-background:transparent;-fx-background-color:transparent;" +
                "-fx-border-color:transparent;");

        // ── Separatore ────────────────────────────────────────────────────
        Region sep = separator();

        // ── Crea nuova lobby ──────────────────────────────────────────────
        Label createLabel = new Label("CREA NUOVA LOBBY");
        createLabel.setStyle(styleSmallCaps());

        Label playersLabel = new Label("Numero giocatori");
        playersLabel.setStyle("-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:13;-fx-text-fill:rgba(255,255,255,0.70);");

        ToggleGroup playersTG = new ToggleGroup();
        HBox playersRow = new HBox(10);
        playersRow.setAlignment(Pos.CENTER_LEFT);
        for (int n = 2; n <= 5; n++) {
            ToggleButton tb = playerToggleButton(String.valueOf(n), playersTG, n == 2);
            playersRow.getChildren().add(tb);
        }

        Button createBtn = new Button("Crea e aspetta");
        createBtn.setPrefHeight(48);
        createBtn.setMaxWidth(Double.MAX_VALUE);
        createBtn.setStyle(stylePrimaryButton());
        createBtn.setOnAction(e -> {
            ToggleButton sel = (ToggleButton) playersTG.getSelectedToggle();
            if (sel == null) return;
            int numPlayers = Integer.parseInt(sel.getText());
            createBtn.setDisable(true);
            createBtn.setText("Creazione…");
            new Thread(() -> {
                try {
                    server.loginFirstPlayer(nick, numPlayers);
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        createBtn.setDisable(false);
                        createBtn.setText("Crea e aspetta");
                        lobbyStatusLabel.setText("⚠  " + ex.getMessage());
                    });
                }
            }, "gui-create-lobby").start();
        });

        // ── Card contenitore ──────────────────────────────────────────────
        VBox card = new VBox(20,
                header,
                separator(),
                lobbyStatusLabel, scroll,
                sep,
                createLabel, playersLabel, playersRow,
                createBtn
        );
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(44, 40, 44, 40));
        card.setMaxWidth(480);
        card.setStyle(styleGlassCard());

        StackPane cardWrapper = new StackPane(card);
        cardWrapper.setPadding(new Insets(60));

        StackPane root = new StackPane(animBg, cardWrapper);
        animBg.prefWidthProperty().bind(root.widthProperty());
        animBg.prefHeightProperty().bind(root.heightProperty());

        stage.setScene(new Scene(root, 600, 720));
    }

    // ─────────────────────────────────────────────────────────────────────
    //  SCHERMATA ATTESA (dopo login / join)
    // ─────────────────────────────────────────────────────────────────────

    private void showWaitingScreen(int current, int expected) {
        AnimatedBackground animBg = new AnimatedBackground();

        Label title = new Label("In attesa…");
        title.setStyle(styleTitle());

        Label info = new Label("Giocatori connessi: " + current + " / " + expected);
        info.setStyle("-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:16;-fx-text-fill:rgba(255,255,255,0.75);");

        // Indicatore visivo giocatori
        HBox dots = new HBox(12);
        dots.setAlignment(Pos.CENTER);
        for (int i = 0; i < expected; i++) {
            Region dot = new Region();
            dot.setPrefSize(14, 14);
            dot.setStyle("-fx-background-radius:7;" +
                    "-fx-background-color:" + (i < current ? "#34C759" : "rgba(255,255,255,0.15)") + ";");
            dots.getChildren().add(dot);
        }

        VBox card = new VBox(24, title, info, dots);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(60));
        card.setMaxWidth(400);
        card.setStyle(styleGlassCard());

        StackPane cardWrapper = new StackPane(card);
        cardWrapper.setPadding(new Insets(60));

        StackPane root = new StackPane(animBg, cardWrapper);
        animBg.prefWidthProperty().bind(root.widthProperty());
        animBg.prefHeightProperty().bind(root.heightProperty());

        stage.setScene(new Scene(root, 560, 400));
    }

    // ─────────────────────────────────────────────────────────────────────
    //  SCHERMATA DI GIOCO
    // ─────────────────────────────────────────────────────────────────────

    private GameScreen gameScreen;

    public void showGameScreen() {
        showGameScreen(false);
    }

    // === SPECTATOR ===
    private void showGameScreen(boolean spectator) {
        List<String> activeSpaces = getActiveSpaces(players.size());
        gameScreen = new GameScreen(server, model, nick, players, activeSpaces, spectator, this);
        Scene scene = gameScreen.build(stage);
        stage.setScene(scene);
        stage.setResizable(true);
    }

    /**
     * Called by GameScreen's "Torna alla Lobby" button when in spectator mode.
     * Leaves spectator mode and requests a fresh lobby list.
     */
    public void leaveSpectatorView() {
        // This runs on the FX thread (button action), so showLobbyScreen() is safe here.
        // We rebuild the lobby UI immediately so that when onLobbyList arrives from the
        // server it finds lobbyListBox already initialized and just updates the list.
        isSpectator = false;
        gameScreen = null;
        showLobbyScreen();
        new Thread(() -> {
            try {
                server.leaveSpectator(nick);
            } catch (Exception e) {
                System.err.println("[GUIView] leaveSpectator error: " + e.getMessage());
            }
        }, "gui-leave-spectator").start();
    }
    // === END SPECTATOR ===

    private List<String> getActiveSpaces(int numPlayers) {
        // Specchio esatto di Board.prepareGameBoardSpace
        List<String> spaces = new ArrayList<>(List.of("A","B","C","D","E","F","G"));
        if (numPlayers <= 4) spaces.remove("A");
        if (numPlayers <= 3) spaces.remove("G");
        if (numPlayers <= 2) spaces.remove("D");
        return spaces;
    }

    // il metodo restituisce gli id delle carte nella riga del marker passato (e si limita a leggere quelle della riga E NON anche quelle pescate)
    private List<Integer> parseCardIds(String text, String marker) {
        List<Integer> ids = new ArrayList<>();
        int markerIdx = text.indexOf(marker);
        if (markerIdx < 0) return ids;

        String section = text.substring(markerIdx + marker.length());

        int end = section.length();

        int nextMarker = section.indexOf("##");
        if (nextMarker >= 0) end = Math.min(end, nextMarker);

        int playersIdx = section.indexOf("┌── Stato giocatori");
        if (playersIdx >= 0) end = Math.min(end, playersIdx);

        int handIdx = section.indexOf("┌── Le tue carte");
        if (handIdx >= 0) end = Math.min(end, handIdx);

        section = section.substring(0, end);

        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("CardId:\\s*(\\d+)").matcher(section);
        while (m.find()) {
            ids.add(Integer.parseInt(m.group(1)));
        }

        return ids;
    }


    private List<Integer> parseTopCards(String text) {
        return parseCardsBetween(text, "Riga SUPERIORE", "Riga INFERIORE");
    }

    private List<Integer> parseBotCards(String text) {
        return parseCardsBetween(text, "Riga INFERIORE", "Spazi Offerta");
    }

    private List<Integer> parseCardsBetween(String text, String startMarker, String endMarker) {
        List<Integer> ids = new ArrayList<>();
        int start = text.indexOf(startMarker);
        if (start < 0) return ids;
        int end = text.indexOf(endMarker, start + startMarker.length());
        String section = end >= 0 ? text.substring(start, end) : text.substring(start);
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("CardId:\\s*(\\d+)").matcher(section);
        while (m.find()) ids.add(Integer.parseInt(m.group(1)));
        return ids;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  ModelObserver
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void onLoginAccepted(String nickname, int expectedPlayers) {
        Platform.runLater(() -> showWaitingScreen(1, expectedPlayers));
    }

    @Override
    public void onPlayerJoined(String nickname, int currentCount, int expected) {
        Platform.runLater(() -> showWaitingScreen(currentCount, expected));
    }

    @Override
    public void onGameStarting(List<String> playerNicknames) {
        this.players = new ArrayList<>(playerNicknames);
        it.polimi.ingsw.model.enums.TotemColor[] colors =
                it.polimi.ingsw.model.enums.TotemColor.values();
        for (int i = 0; i < playerNicknames.size(); i++)
            totemColors.put(playerNicknames.get(i), colors[i % colors.length]);
        Platform.runLater(() -> showGameScreen(false));
    }

    @Override
    public void onNoLobbyAvailable() {
        Platform.runLater(() -> {
            lobbyListBox.getChildren().clear();
            lobbyStatusLabel.setText("Nessuna lobby aperta — creane una nuova qui sotto.");
        });
    }

    @Override
    public void onLobbyList(List<LobbyManager.LobbyInfo> lobbies) {
        Platform.runLater(() -> {
            // === SPECTATOR: returning from spectator mode — rebuild lobby screen first ===
            if (lobbyListBox == null) {
                showLobbyScreen();
            }
            // === END SPECTATOR ===

            lobbyListBox.getChildren().clear();

            List<LobbyManager.LobbyInfo> open = lobbies.stream()
                    .filter(l -> !l.inProgress()).collect(java.util.stream.Collectors.toList());
            List<LobbyManager.LobbyInfo> inProgress = lobbies.stream()
                    .filter(LobbyManager.LobbyInfo::inProgress).collect(java.util.stream.Collectors.toList());

            if (open.isEmpty() && inProgress.isEmpty()) {
                lobbyStatusLabel.setText("Nessuna lobby disponibile.");
                return;
            }

            if (!open.isEmpty()) {
                lobbyStatusLabel.setText("Lobby disponibili — scegli una o creane una nuova:");
                for (LobbyManager.LobbyInfo l : open) {
                    lobbyListBox.getChildren().add(lobbyCard(l, false));
                }
            } else {
                lobbyStatusLabel.setText("Nessuna lobby aperta.");
            }

            // === SPECTATOR: show in-progress lobbies ===
            if (!inProgress.isEmpty()) {
                Label inProgressLabel = new Label("PARTITE IN CORSO");
                inProgressLabel.setStyle(styleSmallCaps());
                inProgressLabel.setPadding(new Insets(12, 0, 4, 0));
                lobbyListBox.getChildren().add(inProgressLabel);
                for (LobbyManager.LobbyInfo l : inProgress) {
                    lobbyListBox.getChildren().add(lobbyCard(l, true));
                }
            }
            // === END SPECTATOR ===
        });
    }

    @Override
    public void onError(String message) {
        Platform.runLater(() -> {
            if (gameScreen != null) {
                gameScreen.showToast(message);
                return;
            }

            if (lobbyStatusLabel != null) {
                lobbyStatusLabel.setText("⚠  " + message);
            }
        });
    }



    @Override
    public void onYourTurn(String nickname, GameState phase, String extraInfo) {
        Platform.runLater(() -> {
            if (gameScreen == null) return;
            gameScreen.updatePhase(phase, nickname);
            parseAndUpdateStats(extraInfo);
            parseAndUpdateAllStats(extraInfo);
            gameScreen.updatePlayerOwnedCards(parsePlayerOwnedCards(extraInfo));

            boolean myTurn  = nick.equals(nickname);
            boolean canPick = myTurn && phase == GameState.PICKING_CARD;

            List<Integer> top;
            List<Integer> bot;
            boolean pickTop;

            if (phase == GameState.PICKING_CARD) {
                // Carte con marker ##HAS_TOP## / ##HAS_BOT##
                top     = parseCardIds(extraInfo, "##HAS_TOP##");
                bot     = parseCardIds(extraInfo, "##HAS_BOT##");
                pickTop = !top.isEmpty(); // può pescare dall'alto se ci sono carte lì
            } else {
                top     = parseTopCards(extraInfo);
                bot     = parseBotCards(extraInfo);
                pickTop = false;
            }

            gameScreen.updateBoardCards(top, bot, canPick, pickTop);
        });
    }


    @Override
    public void onTurnSnapshot(String currentPlayerNick, String boardSummary) {
        Platform.runLater(() -> {
            if (gameScreen == null) return;
            gameScreen.setOtherPlayerTurn(currentPlayerNick);
            List<Integer> top = parseTopCards(boardSummary);
            List<Integer> bot = parseBotCards(boardSummary);
            // Passa sempre — updateBoardCards ignora le liste vuote internamente
            gameScreen.updateBoardCards(top, bot, false, false);
            parseAndUpdateAllStats(boardSummary);
            gameScreen.updatePlayerOwnedCards(parsePlayerOwnedCards(boardSummary));
        });
    }

    @Override
    public void onTotemPlaced(String nickname, String boardSpaceId) {
        Platform.runLater(() -> {
            if (gameScreen == null) return;
            TotemColor color = totemColors.getOrDefault(nickname, TotemColor.BLUE);
            gameScreen.placeTotemOnSpace(boardSpaceId, color, nickname);
        });
    }

    @Override public void onInvalidAction(String nicknameTarget, String errorMessage) {
        Platform.runLater(() -> {
            if (gameScreen != null) gameScreen.showErrorMessage(errorMessage);
        });
    }

    @Override
    public void onCardTaken(String nickname, String cardId) {
        Platform.runLater(() -> {
            if (gameScreen == null) return;
            java.util.regex.Matcher m =
                    java.util.regex.Pattern.compile("CardId:\\s*(\\d+)").matcher(cardId);
            if (m.find()) {
                int id = Integer.parseInt(m.group(1));
                // Rimuovi la carta dal board per tutti (anche se la prende un altro)
                gameScreen.removeCardFromBoard(id);
                // Aggiungila alla mano solo se è mia
                if (nickname.equals(nick))
                    gameScreen.addCardToHand(id);
            }
        });
    }

    @Override
    public void onPlayerUpdated(String nickname) {
        // Non abbiamo lo stato aggiornato qui, arriverà con il prossimo onYourTurn/onTurnSnapshot
        // Ma possiamo forzare un aggiornamento se riceviamo boardSummary nell'extraInfo
        Platform.runLater(() -> System.out.println("[GUI] playerUpdated: " + nickname));
    }

    @Override
    public void onTurnOrderUpdated(List<String> newOrderedNicknames) {
        Platform.runLater(() -> {
            if (gameScreen != null)
                gameScreen.updateTurnOrder(newOrderedNicknames, totemColors);
        });
    }

    @Override
    public void onEventResolved(String eventName, String resultDetails) {
        Platform.runLater(() -> {
            if (gameScreen == null) return;

            int cardId = parseEventCardId(resultDetails);
            Map<String, int[]> stats = parseEventStats(resultDetails);

            gameScreen.showEventResolution(eventName, cardId, stats);
        });
    }


    @Override public void onBoardUpdated() {
        Platform.runLater(() -> { if (gameScreen != null) gameScreen.clearBoard(); });
    }

    @Override public void onNewEraStarted(Age newEra) {
        Platform.runLater(() -> { if (gameScreen != null) gameScreen.updateEra(newEra); });
    }
    @Override public void onGameOver(String results) {
        Platform.runLater(() -> System.out.println("[GUI] Fine partita: " + results));
    }
    // === TASK F: disconnection banner ===
    @Override
    public void onPlayerDisconnected(String nickname) {
        Platform.runLater(() -> {
            if (gameScreen != null) {
                gameScreen.showToast("⚠  " + nickname + " si è disconnesso.");
            } else if (lobbyStatusLabel != null) {
                lobbyStatusLabel.setText("⚠  " + nickname + " si è disconnesso.");
            }
        });
    }
    // === END TASK F ===

    // === SPECTATOR ===
    @Override
    public void onSpectatorJoined(String currentPlayerNick, String boardSummary) {
        // Parse player names before entering the FX thread.
        List<String> parsedPlayers = parsePlayerNamesFromSummary(boardSummary);

        Platform.runLater(() -> {
            isSpectator = true;

            // Spectators never receive onGameStarting, so players/totemColors are empty.
            // Populate them now from the board summary.
            if (!parsedPlayers.isEmpty()) {
                this.players = new ArrayList<>(parsedPlayers);
                it.polimi.ingsw.model.enums.TotemColor[] colors =
                        it.polimi.ingsw.model.enums.TotemColor.values();
                for (int i = 0; i < parsedPlayers.size(); i++) {
                    totemColors.put(parsedPlayers.get(i), colors[i % colors.length]);
                }
            }

            showGameScreen(true); // now builds with correct players list + spaces

            // Apply the board snapshot the server sent at entry time.
            if (gameScreen != null && boardSummary != null) {
                gameScreen.setOtherPlayerTurn(currentPlayerNick);
                List<Integer> top = parseTopCards(boardSummary);
                List<Integer> bot = parseBotCards(boardSummary);
                gameScreen.updateBoardCards(top, bot, false, false);
                parseAndUpdateAllStats(boardSummary);
                gameScreen.updatePlayerOwnedCards(parsePlayerOwnedCards(boardSummary));
            }
        });
    }

    /**
     * Parses ordered player nicknames from the ##PLAYER_CARDS_BEGIN## block.
     * Spectators use this because they never receive onGameStarting().
     */
    private List<String> parsePlayerNamesFromSummary(String boardSummary) {
        List<String> names = new ArrayList<>();
        if (boardSummary == null) return names;
        for (String line : boardSummary.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("PLAYER=")) {
                names.add(trimmed.substring("PLAYER=".length()).trim());
            }
        }
        return names;
    }
// === END SPECTATOR ===

    // ─────────────────────────────────────────────────────────────────────
    //  UI helpers
    // ─────────────────────────────────────────────────────────────────────

    private void parseAndUpdateAllStats(String text) {
        if (gameScreen == null || text == null) return;
        // Formato tabella: │  nickname<spaces>food<spaces>prestige<spaces>turno
        String[] lines = text.split("\n");
        for (String line : lines) {
            if (!line.contains("│")) continue;
            // Rimuove il prefisso "│  " e splitta per 2+ spazi
            String content = line.replaceFirst(".*?│\\s{2}", "").trim();
            String[] parts = content.split("\\s{2,}");
            if (parts.length < 3) continue;
            String pNick = parts[0].trim();
            // Salta righe header
            if (pNick.equals("Nome") || pNick.isEmpty()) continue;
            try {
                int food     = Integer.parseInt(parts[1].trim());
                int prestige = Integer.parseInt(parts[2].trim());
                System.out.println("[GUI] stats: " + pNick +
                        " cibo=" + food + " prestige=" + prestige);
                gameScreen.updatePlayerStats(pNick, food, prestige);
                if (pNick.equals(nick))
                    gameScreen.updateMyStats(food, prestige);
            } catch (NumberFormatException ignored) {}
        }
    }

    //Parso le carte possedute da ogni player per mostrarle nel pop_up
    private Map<String, List<Integer>> parsePlayerOwnedCards(String text) {
        Map<String, List<Integer>> cardsByPlayer = new java.util.HashMap<>();
        if (text == null) return cardsByPlayer;

        int start = text.indexOf("##PLAYER_CARDS_BEGIN##");
        int end = text.indexOf("##PLAYER_CARDS_END##");
        if (start < 0 || end < start) return cardsByPlayer;

        String[] lines = text.substring(start, end).split("\n");
        String currentPlayer = null;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.startsWith("PLAYER=")) {
                currentPlayer = line.substring("PLAYER=".length()).trim();
                cardsByPlayer.putIfAbsent(currentPlayer, new ArrayList<>());
            } else if (line.startsWith("CARD=") && currentPlayer != null) {
                try {
                    cardsByPlayer.get(currentPlayer).add(
                            Integer.parseInt(line.substring("CARD=".length()).trim())
                    );
                } catch (NumberFormatException ignored) {}
            } else if (line.equals("END_PLAYER")) {
                currentPlayer = null;
            }
        }

        return cardsByPlayer;
    }

    private HBox lobbyCard(LobbyManager.LobbyInfo lobby, boolean spectate) {
        Label info = new Label("Lobby #" + lobby.id() +
                "   —   " + lobby.currentPlayers() +
                " / " + lobby.expectedPlayers() + " giocatori");
        info.setStyle("-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:14;-fx-text-fill:white;");
        HBox.setHgrow(info, Priority.ALWAYS);

        String btnLabel = spectate ? "Guarda" : "Unisciti";
        Button actionBtn = new Button(btnLabel);
        actionBtn.setPrefHeight(36);
        actionBtn.setStyle(styleSecondaryButton());
        actionBtn.setOnAction(e -> {
            actionBtn.setDisable(true);
            actionBtn.setText("…");
            new Thread(() -> {
                try {
                    // === SPECTATOR ===
                    if (spectate) {
                        server.joinAsSpectator(nick, lobby.id());
                    } else {
                        server.loginToLobby(nick, lobby.id());
                    }
                    // === END SPECTATOR ===
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        actionBtn.setDisable(false);
                        actionBtn.setText(btnLabel);
                    });
                }
            }, spectate ? "gui-spectate" : "gui-join").start();
        });

        HBox row = new HBox(16, info, actionBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16, 20, 16, 20));
        row.setStyle("-fx-background-color:rgba(255,255,255,0.06);" +
                "-fx-background-radius:14;" +
                "-fx-border-color:rgba(255,255,255,0.10);" +
                "-fx-border-radius:14;-fx-border-width:1;");
        return row;
    }

    private ToggleButton playerToggleButton(String text, ToggleGroup tg, boolean selected) {
        ToggleButton tb = new ToggleButton(text);
        tb.setToggleGroup(tg);
        tb.setSelected(selected);
        tb.setPrefSize(52, 44);
        tb.setStyle("-fx-background-color:rgba(255,255,255,0.08);" +
                "-fx-background-radius:12;" +
                "-fx-text-fill:white;-fx-font-size:15;" +
                "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-cursor:hand;");
        tb.selectedProperty().addListener((obs, wasSelected, isSelected) ->
                tb.setStyle("-fx-background-color:" + (isSelected ? "#007AFF" : "rgba(255,255,255,0.08)") + ";" +
                        "-fx-background-radius:12;" +
                        "-fx-text-fill:white;-fx-font-size:15;" +
                        "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                        "-fx-cursor:hand;")
        );
        return tb;
    }


    private Region separator() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setMaxWidth(Double.MAX_VALUE);
        r.setStyle("-fx-background-color:rgba(255,255,255,0.10);");
        return r;
    }

    private String styleTitle() {
        return "-fx-font-family:'SF Pro Display','Helvetica Neue',Arial;" +
                "-fx-font-size:32;-fx-font-weight:bold;-fx-text-fill:white;";
    }

    private String styleSmallCaps() {
        return "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:11;-fx-font-weight:bold;" +
                "-fx-text-fill:rgba(255,255,255,0.45);";
    }

    private String styleGlassCard() {
        return "-fx-background-color:rgba(255,255,255,0.07);" +
                "-fx-background-radius:24;" +
                "-fx-border-color:rgba(255,255,255,0.13);" +
                "-fx-border-radius:24;-fx-border-width:1;";
    }

    private String stylePrimaryButton() {
        return "-fx-background-color:#007AFF;-fx-background-radius:14;" +
                "-fx-text-fill:white;-fx-font-size:15;-fx-font-weight:bold;" +
                "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;-fx-cursor:hand;";
    }

    private String styleSecondaryButton() {
        return "-fx-background-color:rgba(255,255,255,0.12);-fx-background-radius:10;" +
                "-fx-text-fill:white;-fx-font-size:13;" +
                "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;-fx-cursor:hand;";
    }

    private void parseAndUpdateStats(String extraInfo) {
        if (gameScreen == null || extraInfo == null) return;
        try {
            java.util.regex.Matcher m =
                    java.util.regex.Pattern.compile(
                                    "Il tuo cibo:\\s*(\\d+).*?Prestige:\\s*(\\d+)")
                            .matcher(extraInfo);
            if (m.find()) {
                int food     = Integer.parseInt(m.group(1));
                int prestige = Integer.parseInt(m.group(2));
                gameScreen.updateMyStats(food, prestige);
            }
        } catch (Exception ignored) {}
    }

    private int parseEventCardId(String payload) {
        for (String line : payload.split("\n")) {
            if (line.startsWith("cardId=")) {
                try {
                    return Integer.parseInt(line.substring("cardId=".length()).trim());
                } catch (NumberFormatException ignored) { }
            }
        }
        return -1;
    }

    private Map<String, int[]> parseEventStats(String payload) {
        Map<String, int[]> stats = new java.util.HashMap<>();

        for (String line : payload.split("\n")) {
            if (!line.startsWith("player=")) continue;

            String[] parts = line.split(";");
            String pNick = null;
            Integer food = null;
            Integer prestige = null;

            for (String part : parts) {
                if (part.startsWith("player=")) {
                    pNick = part.substring("player=".length()).trim();
                } else if (part.startsWith("food=")) {
                    food = Integer.parseInt(part.substring("food=".length()).trim());
                } else if (part.startsWith("prestige=")) {
                    prestige = Integer.parseInt(part.substring("prestige=".length()).trim());
                }
            }

            if (pNick != null && food != null && prestige != null) {
                stats.put(pNick, new int[]{food, prestige});
            }
        }

        return stats;
    }

}