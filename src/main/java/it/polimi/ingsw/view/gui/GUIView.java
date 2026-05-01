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
        Background bg = darkBackground();

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

        StackPane root = new StackPane(card);
        root.setBackground(bg);
        root.setPadding(new Insets(60));

        stage.setScene(new Scene(root, 600, 720));
    }

    // ─────────────────────────────────────────────────────────────────────
    //  SCHERMATA ATTESA (dopo login / join)
    // ─────────────────────────────────────────────────────────────────────

    private void showWaitingScreen(int current, int expected) {
        Background bg = darkBackground();

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

        StackPane root = new StackPane(card);
        root.setBackground(bg);
        root.setPadding(new Insets(60));

        stage.setScene(new Scene(root, 560, 400));
    }

    // ─────────────────────────────────────────────────────────────────────
    //  SCHERMATA DI GIOCO (step 3 — TODO)
    // ─────────────────────────────────────────────────────────────────────

    private GameScreen gameScreen;

    public void showGameScreen() {
        gameScreen = new GameScreen(server, model, nick,
                model.getLobbyPlayers());
        Scene scene = gameScreen.build(stage);
        stage.setScene(scene);
        stage.setResizable(true);
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
            System.out.println("[GUI] onCardTaken: " + nickname + " → " + cardId);
            if (nickname.equals(nick)) {
                try {
                    gameScreen.addCardToHand(Integer.parseInt(cardId.trim()));
                } catch (NumberFormatException ex) {
                    System.err.println("[GUI] cardId non parsabile: " + cardId);
                }
            }
        });
    }

    @Override public void onPlayerUpdated(String nickname) {}
    @Override
    public void onTurnOrderUpdated(List<String> newOrderedNicknames) {
        Platform.runLater(() -> {
            if (gameScreen != null)
                gameScreen.updateTurnOrder(newOrderedNicknames, totemColors);
        });
    }
    @Override public void onEventResolved(String eventName, String resultDetails) {}

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
        // Cerca righe tipo: │  fil                 3       0
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile(
                                "│\\s+(\\S+)\\s+(\\d+)\\s+(\\d+)")
                        .matcher(text);
        while (m.find()) {
            String pNick   = m.group(1);
            int food       = Integer.parseInt(m.group(2));
            int prestige   = Integer.parseInt(m.group(3));
            gameScreen.updatePlayerStats(pNick, food, prestige);
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

    private Background darkBackground() {
        return new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#0d0d1a")),
                        new Stop(1, Color.web("#1a0d2e"))
                ), CornerRadii.EMPTY, Insets.EMPTY));
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