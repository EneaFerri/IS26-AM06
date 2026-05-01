package it.polimi.ingsw.view.gui;

import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.GameServerProxy;
import it.polimi.ingsw.view.ClientModel;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;

import java.util.*;

/**
 * Schermata principale di gioco.
 * Separata da GUIView per tenere il codice gestibile.
 * GUIView la chiama via  showGameScreen() e poi aggiorna
 * i binding tramite i metodi pubblici update*().
 */
public class GameScreen {

    // ── Costanti layout ───────────────────────────────────────────────────
    private static final double BOARD_TILE_W  = 110;
    private static final double BOARD_TILE_H  = 155;
    private static final double BLOCK_W       = 88;
    private static final double BLOCK_H       = 125;
    private static final double CARD_W        = 72;
    private static final double CARD_H        = 102;

    // Colori Pantone dei totem (da pedine_specs.pdf)
    private static final Map<TotemColor, String> TOTEM_HEX = Map.of(
            TotemColor.RED,    "#E8472A",
            TotemColor.YELLOW, "#F5C518",
            TotemColor.BLUE,   "#00A8C6",
            TotemColor.BLACK,  "#3D0C2A",
            TotemColor.WHITE,  "#E8E8E8"
    );

    // ── Riferimenti UI aggiornabili ───────────────────────────────────────
    private final Label       eraLabel        = new Label("Era I");
    private final Label       phaseLabel      = new Label();
    private final Label       turnLabel       = new Label();
    private final Label       foodLabel       = new Label("🍖 —");
    private final Label       prestigeLabel   = new Label("★ —");
    private final HBox        handBox         = new HBox(8);
    private final VBox        playersPanel    = new VBox(12);

    private final HBox topRowBox = new HBox(8);
    private final HBox botRowBox = new HBox(8);
    private boolean canPickCards = false;

    // spazio → lista ImageView dei totem sovrapposti
    private final Map<String, VBox> spaceTotemSlots = new HashMap<>();
    // spazio → il StackPane cliccabile
    private final Map<String, StackPane> spacePanes   = new HashMap<>();

    private final VBox turnOrderTotemSlots = new VBox(2);
    private StackPane turnOrderPane;

    private final GameServerProxy server;
    private final ClientModel     model;
    private final String          myNick;
    private final List<String>    players;
    private       GameState       currentPhase = GameState.OFFER_SPACE_CHOOSE;
    private       boolean         isMyTurn     = false;

    private final Map<String, String> playerOnSpace = new HashMap<>();
    // nickname → lettera spazio (es. "fil" → "E")

    private List<Integer> lastTopIds = new ArrayList<>();
    private List<Integer> lastBotIds = new ArrayList<>();

    private final Map<String, Label> playerPrestigeLabels = new HashMap<>();
    private final Map<String, Label> playerFoodLabels     = new HashMap<>();

    // ── Spazi offerta in ordine ───────────────────────────────────────────
    private static final List<String> SPACES = List.of("A","B","C","D","E","F","G");

    public GameScreen(GameServerProxy server, ClientModel model,
                      String myNick, List<String> players) {
        this.server  = server;
        this.model   = model;
        this.myNick  = myNick;
        this.players = players;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  BUILD SCENE
    // ─────────────────────────────────────────────────────────────────────

    public Scene build(Stage stage) {
        // Root
        BorderPane root = new BorderPane();
        root.setBackground(darkBg());

        root.setTop(buildHeader());
        root.setCenter(buildCenter());
        root.setRight(buildPlayersPanel());
        root.setBottom(buildHandPanel());

        BorderPane.setMargin(root.getTop(),    new Insets(0));
        BorderPane.setMargin(root.getRight(),  new Insets(0));
        BorderPane.setMargin(root.getBottom(), new Insets(0));

        return new Scene(root, 1400, 860);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  HEADER
    // ─────────────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        // Era pill
        eraLabel.setStyle("-fx-font-family:'SF Pro Display','Helvetica Neue',Arial;" +
                "-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:white;" +
                "-fx-background-color:#007AFF;-fx-background-radius:20;" +
                "-fx-padding:4 14 4 14;");

        // Fase
        phaseLabel.setText("Piazzamento totem");
        phaseLabel.setStyle(labelStyle(12, "rgba(255,255,255,0.55)"));

        // Turno
        turnLabel.setText("In attesa…");
        turnLabel.setStyle(labelStyle(13, "white"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Cibo e Prestigio
        foodLabel.setStyle(labelStyle(13, "#F5C518"));
        prestigeLabel.setStyle(labelStyle(13, "#34C759"));

        HBox header = new HBox(16, eraLabel, separator_v(), phaseLabel,
                separator_v(), turnLabel, spacer,
                foodLabel, prestigeLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 24, 14, 24));
        header.setStyle("-fx-background-color:rgba(0,0,0,0.45);" +
                "-fx-border-color:rgba(255,255,255,0.08);" +
                "-fx-border-width:0 0 1 0;");
        return header;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  CENTER  (turn order + board spaces + card rows)
    // ─────────────────────────────────────────────────────────────────────

    private ScrollPane buildCenter() {
        topRowBox.setAlignment(Pos.CENTER);
        topRowBox.setMinHeight(CARD_H + 16);
        topRowBox.setPadding(new Insets(8, 0, 8, 0));

        botRowBox.setAlignment(Pos.CENTER);
        botRowBox.setMinHeight(CARD_H + 16);
        botRowBox.setPadding(new Insets(8, 0, 8, 0));

        HBox boardRow = new HBox(10);
        boardRow.setAlignment(Pos.CENTER);
        boardRow.getChildren().add(buildTurnOrderTrack());
        for (String letter : SPACES)
            boardRow.getChildren().add(buildBoardSpace(letter));

        BorderPane center = new BorderPane();
        center.setTop(topRowBox);
        center.setCenter(boardRow);
        center.setBottom(botRowBox);
        center.setPadding(new Insets(12, 16, 12, 16));

        ScrollPane sp = new ScrollPane(center);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.setStyle("-fx-background:transparent;-fx-background-color:transparent;" +
                "-fx-border-color:transparent;");
        return sp;
    }

    // ── Turn order track ─────────────────────────────────────────────────

    private VBox buildTurnOrderTrack() {
        int blockNum = Math.min(players.size() - 1, 4); // 2p→1, 3p→2, 4p→3, 5p→4
        blockNum = Math.max(blockNum, 1);

        ImageView img = loadImage("/gui/board/block" + blockNum + ".png",
                BLOCK_W, BLOCK_H);

        turnOrderTotemSlots.setAlignment(Pos.TOP_CENTER);
        turnOrderTotemSlots.setPadding(new Insets(8, 0, 0, 0));
        turnOrderTotemSlots.setMaxWidth(BLOCK_W - 8);

        turnOrderPane = new StackPane(img, turnOrderTotemSlots);
        StackPane.setAlignment(turnOrderTotemSlots, Pos.TOP_CENTER);
        turnOrderPane.setPrefSize(BLOCK_W, BLOCK_H);

        Label lbl = new Label("ORDINE");
        lbl.setStyle(labelStyle(9, "rgba(255,255,255,0.40)"));

        VBox track = new VBox(4, lbl, turnOrderPane);
        track.setAlignment(Pos.BOTTOM_CENTER);
        return track;
    }

    private StackPane buildBlockTile(int blockNum) {
        ImageView img = loadImage("/gui/board/block" + blockNum + ".png",
                BLOCK_W, BLOCK_H);
        StackPane sp = new StackPane(img);
        sp.setPrefSize(BLOCK_W, BLOCK_H);
        return sp;
    }

    // ── Singolo spazio offerta ────────────────────────────────────────────

    private VBox buildBoardSpace(String letter) {
        ImageView bg = loadImage("/gui/board/board_" + letter + ".png",
                BOARD_TILE_W, BOARD_TILE_H);

        VBox totemSlot = new VBox(2);
        totemSlot.setAlignment(Pos.BOTTOM_CENTER);
        totemSlot.setMaxWidth(BOARD_TILE_W - 8);
        spaceTotemSlots.put(letter, totemSlot);

        Label letterLbl = new Label(letter);
        letterLbl.setStyle("-fx-font-family:'SF Pro Display','Helvetica Neue',Arial;" +
                "-fx-font-size:11;-fx-font-weight:bold;" +
                "-fx-text-fill:rgba(255,255,255,0.70);");

        StackPane tile = new StackPane(bg, totemSlot, letterLbl);
        StackPane.setAlignment(letterLbl, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(letterLbl, new Insets(0, 4, 4, 0));
        StackPane.setAlignment(totemSlot, Pos.BOTTOM_CENTER);
        StackPane.setMargin(totemSlot, new Insets(0, 0, 6, 0));

        tile.setPrefSize(BOARD_TILE_W, BOARD_TILE_H);
        tile.setCursor(javafx.scene.Cursor.HAND);
        tile.setStyle("-fx-background-radius:12;-fx-border-radius:12;" +
                "-fx-border-color:rgba(255,255,255,0.10);-fx-border-width:1;");
        tile.setOnMouseEntered(e -> {
            if (isMyTurn && currentPhase == GameState.OFFER_SPACE_CHOOSE)
                tile.setEffect(new DropShadow(16, Color.web("#007AFF")));
        });
        tile.setOnMouseExited(e -> tile.setEffect(null));
        tile.setOnMouseClicked(e -> onSpaceClicked(letter));
        spacePanes.put(letter, tile);

        VBox col = new VBox(tile);
        col.setAlignment(Pos.BOTTOM_CENTER);
        return col;
    }

    // ── Riga di carte (upper / lower) ────────────────────────────────────

    private HBox buildCardRow(String letter, boolean upper) {
        int count = upper ? numCardsUpper(letter) : numCardsLower(letter);
        HBox row = new HBox(4);
        row.setAlignment(Pos.CENTER);
        row.setMinHeight(count > 0 ? CARD_H + 4 : 0);

        for (int i = 0; i < count; i++) {
            // Placeholder carta coperta (back generico)
            StackPane cardSlot = buildCardSlot(null, upper);
            row.getChildren().add(cardSlot);
        }
        return row;
    }

    private StackPane buildCardSlot(Integer cardId, boolean isUpper) {
        StackPane slot = new StackPane();
        slot.setPrefSize(CARD_W, CARD_H);
        slot.setStyle("-fx-background-radius:6;-fx-cursor:hand;");
        slot.setEffect(new DropShadow(6, Color.rgb(0, 0, 0, 0.5)));

        if (cardId != null) {
            ImageView front = loadImage("/gui/cards/card_" + cardId + ".png",
                    CARD_W, CARD_H);
            if (front.getImage() != null && !front.getImage().isError()) {
                slot.getChildren().add(front);
                return slot;
            }
        }
        // Fallback: back
        String backPath = isUpper ? "/gui/cards/backs/back_tribe1.png"
                : "/gui/cards/backs/back_build1.png";
        slot.getChildren().add(loadImage(backPath, CARD_W, CARD_H));
        return slot;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PANNELLO GIOCATORI (right)
    // ─────────────────────────────────────────────────────────────────────

    private ScrollPane buildPlayersPanel() {
        Label title = new Label("GIOCATORI");
        title.setStyle(labelStyle(10, "rgba(255,255,255,0.40)"));
        title.setPadding(new Insets(0, 0, 8, 0));

        playersPanel.getChildren().add(title);
        for (String nick : players) {
            playersPanel.getChildren().add(buildPlayerCard(nick, nick.equals(myNick)));
        }
        playersPanel.setPadding(new Insets(20, 16, 20, 12));
        playersPanel.setStyle("-fx-background-color:rgba(0,0,0,0.30);" +
                "-fx-border-color:rgba(255,255,255,0.07);" +
                "-fx-border-width:0 0 0 1;");
        playersPanel.setPrefWidth(200);

        ScrollPane sp = new ScrollPane(playersPanel);
        sp.setFitToWidth(true);
        sp.setPrefWidth(200);
        sp.setStyle("-fx-background:transparent;-fx-background-color:transparent;" +
                "-fx-border-color:transparent;");
        return sp;
    }

    private VBox buildPlayerCard(String nick, boolean isMe) {
        // Colore totem del giocatore (placeholder finché non arriva dal server)
        TotemColor color = assignTotemColor(nick);
        String hex = TOTEM_HEX.getOrDefault(color, "#888");

        // Indicatore colore
        Circle dot = new Circle(5, Color.web(hex));

        Label nickLbl = new Label(nick + (isMe ? " (tu)" : ""));
        nickLbl.setStyle("-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:13;" +
                "-fx-font-weight:" + (isMe ? "bold" : "normal") + ";" +
                "-fx-text-fill:white;");
        nickLbl.setMaxWidth(140);
        HBox nameRow = new HBox(8, dot, nickLbl);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        Label prestigeLbl = new Label("★ 0");
        prestigeLbl.setStyle(labelStyle(11, "#34C759"));
        Label foodLbl = new Label("🍖 0");
        foodLbl.setStyle(labelStyle(11, "#F5C518"));

        playerPrestigeLabels.put(nick, prestigeLbl);
        playerFoodLabels.put(nick, foodLbl);

        HBox stats = new HBox(10, prestigeLbl, foodLbl);

        VBox card = new VBox(6, nameRow, stats);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setStyle("-fx-background-color:" + (isMe
                ? "rgba(0,122,255,0.15)"
                : "rgba(255,255,255,0.05)") + ";" +
                "-fx-background-radius:12;" +
                "-fx-border-color:" + (isMe
                ? "rgba(0,122,255,0.35)"
                : "rgba(255,255,255,0.08)") + ";" +
                "-fx-border-radius:12;-fx-border-width:1;");
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PANNELLO MANO (bottom)
    // ─────────────────────────────────────────────────────────────────────

    private HBox buildHandPanel() {
        Label title = new Label("LA TUA TRIBÙ");
        title.setStyle(labelStyle(10, "rgba(255,255,255,0.40)"));

        handBox.setAlignment(Pos.CENTER_LEFT);
        handBox.setPadding(new Insets(0));

        // Placeholder iniziale
        Label empty = new Label("Le carte che raccogli appariranno qui");
        empty.setStyle(labelStyle(12, "rgba(255,255,255,0.25)"));
        handBox.getChildren().add(empty);

        ScrollPane scrollHand = new ScrollPane(handBox);
        scrollHand.setFitToHeight(true);
        scrollHand.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollHand.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollHand.setPrefHeight(CARD_H + 24);
        scrollHand.setStyle("-fx-background:transparent;-fx-background-color:transparent;" +
                "-fx-border-color:transparent;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Food + Prestige grandi
        VBox myStats = new VBox(4,
                styledStat("🍖", foodLabel),
                styledStat("★",  prestigeLabel)
        );
        myStats.setAlignment(Pos.CENTER_RIGHT);
        myStats.setPadding(new Insets(0, 8, 0, 16));

        HBox bottom = new HBox(16, title, scrollHand, spacer, myStats);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(12, 20, 12, 20));
        bottom.setPrefHeight(CARD_H + 48);
        bottom.setStyle("-fx-background-color:rgba(0,0,0,0.40);" +
                "-fx-border-color:rgba(255,255,255,0.08);" +
                "-fx-border-width:1 0 0 0;");
        return bottom;
    }

    private HBox styledStat(String icon, Label valueLabel) {
        Label iconLbl = new Label(icon);
        iconLbl.setStyle(labelStyle(16, "white"));
        valueLabel.setStyle(labelStyle(16, "white"));
        HBox row = new HBox(6, iconLbl, valueLabel);
        row.setAlignment(Pos.CENTER_RIGHT);
        return row;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  UPDATE methods  (chiamati da GUIView su Platform.runLater)
    // ─────────────────────────────────────────────────────────────────────

    public void updateEra(Age era) {
        String text = switch (era) {
            case Era_I   -> "Era I";
            case Era_II  -> "Era II";
            case Era_III -> "Era III";
            default      -> "Fine";
        };
        eraLabel.setText(text);
    }

    public void updatePhase(GameState phase, String currentPlayer) {
        this.currentPhase = phase;
        this.isMyTurn     = myNick.equals(currentPlayer);

        phaseLabel.setText(switch (phase) {
            case OFFER_SPACE_CHOOSE -> "Piazzamento totem";
            case PICKING_CARD       -> "Scegli una carta";
            case EXTRA_CARD         -> "Carta extra";
            case EVENTS             -> "Risoluzione eventi";
            case END                -> "Fine partita";
            default                 -> phase.name();
        });

        if (isMyTurn) {
            turnLabel.setText("⬤ Il tuo turno");
            turnLabel.setStyle(labelStyle(13, "#34C759"));
        } else {
            turnLabel.setText("Turno di " + currentPlayer);
            turnLabel.setStyle(labelStyle(13, "rgba(255,255,255,0.60)"));
        }

        boolean active = isMyTurn && phase == GameState.OFFER_SPACE_CHOOSE;
        spacePanes.forEach((l, p) -> p.setOpacity(active ? 1.0 : 0.85));

        // Aggiorna cliccabilità carte
        canPickCards = isMyTurn && phase == GameState.PICKING_CARD;
        rebuildCardRow(topRowBox, lastTopIds, true);
        rebuildCardRow(botRowBox, lastBotIds, false);
    }

    public void updateMyStats(int food, int prestige) {
        foodLabel.setText(String.valueOf(food));
        prestigeLabel.setText(String.valueOf(prestige));
    }

    public void updatePlayerStats(String nick, int food, int prestige) {
        Label fl = playerFoodLabels.get(nick);
        Label pl = playerPrestigeLabels.get(nick);
        if (fl != null) fl.setText("🍖 " + food);
        if (pl != null) pl.setText("★ " + prestige);
    }

    public void placeTotemOnSpace(String letter, TotemColor color, String nickname) {
        VBox slot = spaceTotemSlots.get(letter);
        if (slot == null) return;

        playerOnSpace.put(nickname, letter); // ← tieni traccia

        String hex = TOTEM_HEX.getOrDefault(color, "#888");
        ImageView totemImg = loadImage(
                "/gui/totems/totem_" + color.name().toLowerCase() + ".png", 28, 36);

        if (totemImg.getImage() == null || totemImg.getImage().isError()) {
            Circle c = new Circle(9, Color.web(hex));
            c.setStroke(Color.web("rgba(255,255,255,0.4)"));
            c.setStrokeWidth(1);
            slot.getChildren().add(c);
        } else {
            slot.getChildren().add(totemImg);
        }
    }

    public void updateTurnOrder(List<String> orderedNicknames,
                                Map<String, TotemColor> totemColors) {
        // Trova chi è appena stato aggiunto alla tessera ordine
        // confrontando con chi era già lì
        for (String nick : orderedNicknames) {
            String spaceId = playerOnSpace.remove(nick); // era su questo spazio
            if (spaceId != null) {
                VBox slot = spaceTotemSlots.get(spaceId);
                if (slot != null) slot.getChildren().clear(); // rimuove solo il suo totem
            }
        }

        // Aggiorna la tessera ordine turno
        turnOrderTotemSlots.getChildren().clear();
        for (String nick : orderedNicknames) {
            TotemColor color = totemColors.getOrDefault(nick, TotemColor.BLUE);
            ImageView tv = loadImage(
                    "/gui/totems/totem_" + color.name().toLowerCase() + ".png", 22, 28);
            turnOrderTotemSlots.getChildren().add(tv);
        }
    }

    public void addCardToHand(int cardId) {
        // Rimuove il placeholder se c'è
        handBox.getChildren().removeIf(n -> n instanceof Label);
        StackPane card = buildCardSlot(cardId, true);
        // Click sulla carta della mano → mostra dettaglio (TODO)
        card.setOnMouseClicked(e -> showCardDetail(cardId));
        handBox.getChildren().add(card);
    }

    public void clearBoard() {
        spaceTotemSlots.values().forEach(slot -> slot.getChildren().clear());
    }

    public void showErrorMessage(String msg) {
        turnLabel.setText("⚠ " + msg);
        turnLabel.setStyle(labelStyle(13, "#FF453A"));
    }

    public void updateBoardCards(List<Integer> topIds, List<Integer> botIds, boolean canPick) {
        lastTopIds = new ArrayList<>(topIds);
        lastBotIds = new ArrayList<>(botIds);
        this.canPickCards = canPick;

        rebuildCardRow(topRowBox, topIds, true);
        rebuildCardRow(botRowBox, botIds, false);
    }

    private void rebuildCardRow(HBox row, List<Integer> ids, boolean fromTop) {
        row.getChildren().clear();
        for (int i = 0; i < ids.size(); i++) {
            int cardId = ids.get(i);
            int index  = i;
            StackPane card = buildCardSlot(cardId, fromTop);

            // Click → pickCard
            card.setOnMouseClicked(e -> {
                if (!canPickCards || !isMyTurn) return;
                // Effetto visivo immediato
                card.setOpacity(0.5);
                card.setDisable(true);
                new Thread(() -> {
                    try { server.pickCard(myNick, index, fromTop); }
                    catch (Exception ex) {
                        javafx.application.Platform.runLater(() -> {
                            card.setOpacity(1.0);
                            card.setDisable(false);
                            showErrorMessage("Errore: " + ex.getMessage());
                        });
                    }
                }, "gui-pick").start();
            });

            // Hover glow quando cliccabile
            card.setOnMouseEntered(e -> {
                if (canPickCards && isMyTurn)
                    card.setEffect(new DropShadow(14, Color.web("#34C759")));
            });
            card.setOnMouseExited(e -> card.setEffect(null));

            // Opacità ridotta se non cliccabile
            card.setOpacity(canPickCards && isMyTurn ? 1.0 : 0.80);

            row.getChildren().add(card);
        }
    }

    public void setOtherPlayerTurn(String currentPlayer) {
        this.isMyTurn = false;
        turnLabel.setText("Turno di " + currentPlayer);
        turnLabel.setStyle(labelStyle(13, "rgba(255,255,255,0.60)"));
        phaseLabel.setText("In attesa…");
        spacePanes.forEach((l, p) -> p.setOpacity(0.75));
    }

    // ─────────────────────────────────────────────────────────────────────
    //  AZIONI UTENTE
    // ─────────────────────────────────────────────────────────────────────

    private void onSpaceClicked(String letter) {
        if (!isMyTurn || currentPhase != GameState.OFFER_SPACE_CHOOSE) return;
        new Thread(() -> {
            try { server.placeTotem(myNick, letter.charAt(0)); }
            catch (Exception ex) {
                javafx.application.Platform.runLater(() ->
                        showError("Errore piazzamento: " + ex.getMessage()));
            }
        }, "gui-place-totem").start();
    }

    private void showCardDetail(int cardId) {
        // TODO step 4 — modal con immagine grande + effetto frosted glass
    }

    private void showError(String msg) {
        turnLabel.setText("⚠ " + msg);
        turnLabel.setStyle(labelStyle(13, "#FF453A"));
    }

    // ─────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────

    private static int numCardsUpper(String letter) {
        return switch (letter) {
            case "C" -> 1;
            case "E" -> 1;
            case "F" -> 2;
            case "G" -> 2;
            default  -> 0;
        };
    }

    private static int numCardsLower(String letter) {
        return switch (letter) {
            case "B" -> 1;
            case "D" -> 2;
            case "E" -> 1;
            case "G" -> 1;
            default  -> 0;
        };
    }

    private ImageView loadImage(String path, double w, double h) {
        ImageView iv = new ImageView();
        try {
            var url = getClass().getResource(path);
            if (url != null) {
                Image img = new Image(url.toExternalForm(), w, h, true, true);
                iv.setImage(img);
            }
        } catch (Exception ignored) {}
        iv.setFitWidth(w);
        iv.setFitHeight(h);
        iv.setPreserveRatio(true);
        return iv;
    }

    private TotemColor assignTotemColor(String nick) {
        TotemColor[] colors = TotemColor.values();
        return colors[Math.abs(nick.hashCode()) % colors.length];
    }

    private Background darkBg() {
        return new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#0d0d1a")),
                        new Stop(1, Color.web("#1a0d2e"))),
                CornerRadii.EMPTY, Insets.EMPTY));
    }

    private String labelStyle(int size, String color) {
        return "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:" + size + ";-fx-text-fill:" + color + ";";
    }

    private Region separator_v() {
        Region r = new Region();
        r.setPrefSize(1, 16);
        r.setStyle("-fx-background-color:rgba(255,255,255,0.15);");
        return r;
    }
}