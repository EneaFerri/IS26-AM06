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
    //  SCHERMATA DI GIOCO (step 3 — TODO)
    // ─────────────────────────────────────────────────────────────────────

    private GameScreen gameScreen;

    public void showGameScreen() {
        // Ricava gli spazi attivi dal boardSummary (arriva presto)
        // Per ora usiamo la lista players per dedurre gli spazi attivi
        List<String> activeSpaces = getActiveSpaces(players.size());
        gameScreen = new GameScreen(server, model, nick, players, activeSpaces);
        Scene scene = gameScreen.build(stage);
        stage.setScene(scene);
        stage.setResizable(true);
    }

    private List<String> getActiveSpaces(int numPlayers) {
        // Specchio esatto di Board.prepareGameBoardSpace
        List<String> spaces = new ArrayList<>(List.of("A","B","C","D","E","F","G"));
        if (numPlayers <= 4) spaces.remove("A");
        if (numPlayers <= 3) spaces.remove("G");
        if (numPlayers <= 2) spaces.remove("D");
        return spaces;
    }

    // Parsa card IDs dopo un marker tipo ##HAS_TOP## o ##HAS_BOT##
    private List<Integer> parseCardIds(String text, String marker) {
        List<Integer> ids = new ArrayList<>();
        int markerIdx = text.indexOf(marker);
        if (markerIdx < 0) return ids;
        // Prende la sezione dopo il marker fino al prossimo marker o fine
        String section = text.substring(markerIdx + marker.length());
        int nextMarker = section.indexOf("##");
        if (nextMarker >= 0) section = section.substring(0, nextMarker);
        // Cerca "CardId: N"
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("CardId:\\s*(\\d+)").matcher(section);
        while (m.find()) ids.add(Integer.parseInt(m.group(1)));
        return ids;
    }

    // Parsa card IDs con tag tipo [T] o [B] (per boardSummary watchers)
    private List<Integer> parseCardIdsTag(String text, String tag) {
        List<Integer> ids = new ArrayList<>();
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(tag) +
                        ".*?CardId:\\s*(\\d+)").matcher(text);
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
        Platform.runLater(this::showGameScreen);
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
            lobbyListBox.getChildren().clear();
            if (lobbies.isEmpty()) {
                lobbyStatusLabel.setText("Nessuna lobby disponibile.");
                return;
            }
            lobbyStatusLabel.setText("Lobby disponibili — scegli una o creane una nuova:");
            for (LobbyManager.LobbyInfo l : lobbies) {
                lobbyListBox.getChildren().add(lobbyCard(l));
            }
        });
    }

    @Override
    public void onError(String message) {
        Platform.runLater(() -> {
            if (lobbyStatusLabel != null)
                lobbyStatusLabel.setText("⚠  " + message);
        });
    }

    @Override
    public void onYourTurn(String nickname, GameState phase, String extraInfo) {
        Platform.runLater(() -> {
            if (gameScreen == null) return;
            gameScreen.updatePhase(phase, nickname);
            parseAndUpdateStats(extraInfo);
            parseAndUpdateAllStats(extraInfo);
            List<Integer> top = parseCardIds(extraInfo, "##HAS_TOP##");
            List<Integer> bot = parseCardIds(extraInfo, "##HAS_BOT##");
            gameScreen.updateBoardCards(top, bot, true);
        });
    }

    @Override
    public void onTurnSnapshot(String currentPlayerNick, String boardSummary) {
        Platform.runLater(() -> {
            if (gameScreen == null) return;
            // Non è il mio turno — aggiorna fase e carte
            gameScreen.setOtherPlayerTurn(currentPlayerNick);
            List<Integer> top = parseCardIdsTag(boardSummary, "[T]");
            List<Integer> bot = parseCardIdsTag(boardSummary, "[B]");
            gameScreen.updateBoardCards(top, bot, false);
            parseAndUpdateAllStats(boardSummary);
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
            // cardId è il toString() della carta: "CardId: 54, Age: Era_I, ..."
            java.util.regex.Matcher m =
                    java.util.regex.Pattern.compile("CardId:\\s*(\\d+)").matcher(cardId);
            if (m.find()) {
                int id = Integer.parseInt(m.group(1));
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
            gameScreen.showEventNotification(eventName);
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
    @Override public void onPlayerDisconnected(String nickname) {}

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

    private HBox lobbyCard(LobbyManager.LobbyInfo lobby) {
        Label info = new Label("Lobby #" + lobby.id() +
                "   —   " + lobby.currentPlayers() +
                " / " + lobby.expectedPlayers() + " giocatori");
        info.setStyle("-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:14;-fx-text-fill:white;");
        HBox.setHgrow(info, Priority.ALWAYS);

        Button joinBtn = new Button("Unisciti");
        joinBtn.setPrefHeight(36);
        joinBtn.setStyle(styleSecondaryButton());
        joinBtn.setOnAction(e -> {
            joinBtn.setDisable(true);
            joinBtn.setText("…");
            new Thread(() -> {
                try { server.login(nick); }
                catch (Exception ex) {
                    Platform.runLater(() -> {
                        joinBtn.setDisable(false);
                        joinBtn.setText("Unisciti");
                    });
                }
            }, "gui-join").start();
        });

        HBox row = new HBox(16, info, joinBtn);
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
}