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
import javafx.scene.SnapshotParameters;
import javafx.scene.effect.GaussianBlur;
import javafx.beans.binding.Bindings;


import java.util.*;

/*
  Schermata principale di gioco.
  Separata da GUIView per tenere il codice gestibile.
  GUIView la chiama via  showGameScreen() e poi aggiorna
  i binding tramite i metodi pubblici update().
 */
public class GameScreen {

    // ── Costanti layout ───────────────────────────────────────────────────
    private static final double BOARD_TILE_W  = 110;
    private static final double BOARD_TILE_H  = 155;
    private static final double BLOCK_W       = 88;
    private static final double BLOCK_H       = 125;
    private static final double CARD_W = 95;
    private static final double CARD_H = 134;

    private AnimatedBackground animatedBg;

    private StackPane rootWrapper;
    private final Label toastLabel = new Label();
    private javafx.animation.PauseTransition toastPause;


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

    private boolean canPickTop = false;
    private boolean canPickBot = false;

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

    private final Map<String, Circle> playerTotemDots     = new HashMap<>();
    private final Map<String, TotemColor> playerTotemColors = new HashMap<>();

    private final Map<String, List<Integer>> playerOwnedCards = new HashMap<>(); //per mostrare le carte di ogni player

    // ── Spazi offerta in ordine ───────────────────────────────────────────
    private static final List<String> SPACES = List.of("A","B","C","D","E","F","G");

    private final List<String> activeSpaces;

    // elementi per ANIMAZIONE EVENTI
    private BorderPane mainRoot;

    private final Queue<EventAnimationRequest> eventQueue = new ArrayDeque<>();
    private boolean eventAnimationRunning = false;

    private final StackPane eventOverlay = new StackPane();
    private final Rectangle eventFlash = new Rectangle();
    private final Rectangle eventScrim = new Rectangle();
    private final ImageView eventHeroCard = new ImageView();
    private final Label eventHeroTitle = new Label();

    private ScrollPane centerScrollPane;


    private final javafx.scene.effect.GaussianBlur eventBlur =
            new javafx.scene.effect.GaussianBlur(0);

    // overlay separato per il dettaglio carta. lo tengo distinto da quello degli eventi così non si pestano i piedi
    private final StackPane cardDetailOverlay = new StackPane();
    private final Rectangle cardDetailScrim = new Rectangle();

    private final StackPane cardDetailGlass = new StackPane();
    private final LiquidGlassPane cardDetailLiquidGlass = new LiquidGlassPane();

    private final VBox cardDetailPanel = new VBox(16);
    private final ImageView cardDetailImage = new ImageView();
    private final Label cardDetailTitle = new Label("DETTAGLIO CARTA");
    private final Label cardDetailSubtitle = new Label();
    private final Label cardDetailHint = new Label(
            "Click fuori, premi ESC o usa il bottone qui sotto per tornare alla partita."
    );

    // PER IL POP-UP DELLE CARTE DEL SINGOLO GIOCATORE
    private final StackPane playerCardsOverlay = new StackPane();
    private final Rectangle playerCardsScrim = new Rectangle();
    private final StackPane playerCardsGlass = new StackPane();
    private final LiquidGlassPane playerCardsLiquidGlass = new LiquidGlassPane();
    private final VBox playerCardsPanel = new VBox(10);
    private final Label playerCardsTitle = new Label("CARTE GIOCATORE");
    private final Label playerCardsSubtitle = new Label();
    private final Label playerCardsHint = new Label(
            "Scorri per vedere tutte le carte. Click su una carta per aprirne il dettaglio."
    );
    private final HBox playerCardsRow = new HBox(8);
    private final ScrollPane playerCardsScroller = new ScrollPane(playerCardsRow);
    private final javafx.scene.effect.GaussianBlur playerCardsBlur =
            new javafx.scene.effect.GaussianBlur(0);

    private final javafx.scene.effect.GaussianBlur cardDetailBlur =
            new javafx.scene.effect.GaussianBlur(0);


    private static final class LiquidGlassPane extends Pane {
        private final javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas();
        private final javafx.animation.AnimationTimer timer;
        private double t = 0.0;

        LiquidGlassPane() {
            setMouseTransparent(true);
            setOpacity(0.92);
            getChildren().add(canvas);

            canvas.widthProperty().bind(widthProperty());
            canvas.heightProperty().bind(heightProperty());

            widthProperty().addListener((obs, oldVal, newVal) -> paint());
            heightProperty().addListener((obs, oldVal, newVal) -> paint());

            timer = new javafx.animation.AnimationTimer() {
                @Override
                public void handle(long now) {
                    t += 0.012;
                    paint();
                }
            };
            timer.start();
        }

        private void paint() {
            double w = getWidth();
            double h = getHeight();
            if (w <= 0 || h <= 0) return;

            javafx.scene.canvas.GraphicsContext g = canvas.getGraphicsContext2D();
            g.clearRect(0, 0, w, h);

            g.setGlobalAlpha(0.55);
            g.setFill(new RadialGradient(
                    0, 0,
                    0.24 + 0.04 * Math.sin(t * 0.42),
                    0.16 + 0.03 * Math.cos(t * 0.34),
                    0.92,
                    true,
                    CycleMethod.NO_CYCLE,
                    new Stop(0.00, Color.rgb(255, 255, 255, 0.86)),
                    new Stop(0.36, Color.rgb(244, 247, 255, 0.48)),
                    new Stop(0.72, Color.rgb(204, 218, 238, 0.18)),
                    new Stop(1.00, Color.rgb(255, 255, 255, 0.00))
            ));
            g.fillRect(0, 0, w, h);

            g.setGlobalAlpha(0.28);
            g.setFill(new LinearGradient(
                    0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0.00, Color.rgb(255, 255, 255, 0.54)),
                    new Stop(0.18, Color.rgb(255, 255, 255, 0.18)),
                    new Stop(0.58, Color.rgb(205, 218, 235, 0.10)),
                    new Stop(1.00, Color.rgb(117, 136, 160, 0.12))
            ));
            g.fillRoundRect(0, 0, w, h, 56, 56);

            g.setGlobalAlpha(0.44);
            g.setFill(new LinearGradient(
                    0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0.00, Color.rgb(255, 255, 255, 0.78)),
                    new Stop(0.10, Color.rgb(255, 255, 255, 0.38)),
                    new Stop(0.34, Color.rgb(255, 255, 255, 0.06)),
                    new Stop(1.00, Color.rgb(255, 255, 255, 0.00))
            ));
            g.save();
            g.translate(Math.sin(t * 0.22) * w * 0.008, Math.cos(t * 0.16) * h * 0.006);
            g.rotate(-9.5);
            g.fillRoundRect(w * -0.04, h * 0.00, w * 0.72, h * 0.28, 54, 54);
            g.restore();

            g.setGlobalAlpha(0.18);
            g.setFill(new RadialGradient(
                    0, 0,
                    0.68 + 0.02 * Math.cos(t * 0.28),
                    0.44 + 0.02 * Math.sin(t * 0.24),
                    0.52,
                    true,
                    CycleMethod.NO_CYCLE,
                    new Stop(0.00, Color.rgb(255, 255, 255, 0.30)),
                    new Stop(0.55, Color.rgb(220, 228, 242, 0.08)),
                    new Stop(1.00, Color.rgb(255, 255, 255, 0.00))
            ));
            g.fillOval(w * 0.40, h * 0.16, w * 0.54, h * 0.66);

            g.setGlobalAlpha(0.42);
            g.setFill(new LinearGradient(
                    0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0.00, Color.rgb(255, 255, 255, 0.00)),
                    new Stop(0.18, Color.rgb(255, 120, 120, 0.16)),
                    new Stop(0.38, Color.rgb(255, 214, 120, 0.24)),
                    new Stop(0.58, Color.rgb(170, 255, 214, 0.22)),
                    new Stop(0.78, Color.rgb(130, 190, 255, 0.18)),
                    new Stop(1.00, Color.rgb(255, 255, 255, 0.00))
            ));
            g.fillRoundRect(w * 0.34, h * 0.952, w * 0.30, Math.max(2.0, h * 0.012), 12, 12);

            g.setGlobalAlpha(0.12);
            g.setStroke(Color.rgb(120, 130, 150, 0.85));
            g.setLineWidth(1.0);
            g.strokeRoundRect(1.0, 1.0, w - 2.0, h - 2.0, 56, 56);

            g.setGlobalAlpha(1.0);
        }
    }

    private static final class EventAnimationRequest {
        final String eventName;
        final int cardId;
        final Map<String, int[]> stats;

        EventAnimationRequest(String eventName, int cardId, Map<String, int[]> stats) {
            this.eventName = eventName;
            this.cardId = cardId;
            this.stats = stats;
        }
    }


    public GameScreen(GameServerProxy server, ClientModel model,
                      String myNick, List<String> players, List<String> activeSpaces) {
        this.server       = server;
        this.model        = model;
        this.myNick       = myNick;
        this.players      = players;
        this.activeSpaces = activeSpaces;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  BUILD SCENE
    // ─────────────────────────────────────────────────────────────────────

    public Scene build(Stage stage) {
        BorderPane root = new BorderPane();
        mainRoot = root;
        root.setBackground(Background.EMPTY);

        root.setTop(buildHeader());
        root.setCenter(buildCenter());
        root.setRight(buildPlayersPanel());
        root.setBottom(buildHandPanel());

        animatedBg = new AnimatedBackground();

        rootWrapper = new StackPane(animatedBg, root);
        StackPane.setAlignment(root, Pos.TOP_LEFT);

        root.prefWidthProperty().bind(rootWrapper.widthProperty());
        root.prefHeightProperty().bind(rootWrapper.heightProperty());

        toastLabel.setVisible(false);
        toastLabel.setMouseTransparent(true);
        toastLabel.setWrapText(true);
        toastLabel.setMaxWidth(460);
        toastLabel.setAlignment(Pos.CENTER);
        toastLabel.setStyle("-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:white;" +
                "-fx-background-color:rgba(0,0,0,0.78);" +
                "-fx-background-radius:16;" +
                "-fx-border-color:rgba(255,255,255,0.14);" +
                "-fx-border-radius:16;" +
                "-fx-padding:12 20 12 20;");

        rootWrapper.getChildren().add(toastLabel);
        StackPane.setAlignment(toastLabel, Pos.TOP_CENTER);
        StackPane.setMargin(toastLabel, new Insets(86, 0, 0, 0));

        eventScrim.setFill(Color.rgb(8, 10, 18, 0.18));
        eventScrim.setOpacity(0.0);
        eventScrim.widthProperty().bind(rootWrapper.widthProperty());
        eventScrim.heightProperty().bind(rootWrapper.heightProperty());
        eventScrim.setMouseTransparent(true);

        eventFlash.setFill(Color.web("#FFD54A"));
        eventFlash.setOpacity(0.0);
        eventFlash.widthProperty().bind(rootWrapper.widthProperty());
        eventFlash.heightProperty().bind(rootWrapper.heightProperty());
        eventFlash.setMouseTransparent(true);

        eventHeroCard.setPreserveRatio(true);
        eventHeroCard.setFitWidth(CARD_W);
        eventHeroCard.setFitHeight(CARD_H);

        eventHeroCard.setSmooth(true);
        eventHeroCard.setCache(true);
        eventHeroCard.setPickOnBounds(false);

        DropShadow glow = new DropShadow();
        glow.setRadius(70);
        glow.setSpread(0.22);
        glow.setColor(Color.web("#FFD54A"));

        DropShadow shadow = new DropShadow();
        shadow.setRadius(24);
        shadow.setOffsetY(10);
        shadow.setColor(Color.rgb(0, 0, 0, 0.55));
        glow.setInput(shadow);

        eventHeroCard.setEffect(glow);


        eventHeroTitle.setStyle("-fx-font-family:'SF Pro Display','Helvetica Neue',Arial;" +
                "-fx-font-size:32;-fx-font-weight:bold;-fx-text-fill:white;" +
                "-fx-background-color:rgba(0,0,0,0.72);" +
                "-fx-background-radius:20;" +
                "-fx-border-color:rgba(255,213,74,0.55);" +
                "-fx-border-radius:20;" +
                "-fx-padding:14 28 14 28;");
        eventHeroTitle.setEffect(new DropShadow(28, Color.rgb(0, 0, 0, 0.85)));


        eventHeroTitle.setStyle("-fx-font-family:'SF Pro Display','Helvetica Neue',Arial;" +
                "-fx-font-size:32;-fx-font-weight:bold;-fx-text-fill:white;" +
                "-fx-background-color:rgba(0,0,0,0.78);" +
                "-fx-background-radius:20;" +
                "-fx-border-color:rgba(255,213,74,0.55);" +
                "-fx-border-radius:20;" +
                "-fx-padding:14 28 14 28;");
        eventHeroTitle.setEffect(new DropShadow(28, Color.rgb(0, 0, 0, 0.85)));

        StackPane.setAlignment(eventHeroTitle, Pos.TOP_CENTER);
        StackPane.setMargin(eventHeroTitle, new Insets(72, 0, 0, 0));

        StackPane.setAlignment(eventHeroCard, Pos.CENTER);
        eventHeroCard.setTranslateY(60);

        eventOverlay.getChildren().addAll(eventScrim, eventFlash, eventHeroCard, eventHeroTitle);

        eventOverlay.setVisible(false);
        eventOverlay.setMouseTransparent(true);
        eventOverlay.setOpacity(1.0);

        rootWrapper.getChildren().add(eventOverlay);
        StackPane.setAlignment(eventOverlay, Pos.CENTER);

        rootWrapper.getChildren().add(buildPlayerCardsOverlay());
        StackPane.setAlignment(playerCardsOverlay, Pos.CENTER);

        // effetto per vedere i dettagli della carta
        rootWrapper.getChildren().add(buildCardDetailOverlay());
        StackPane.setAlignment(cardDetailOverlay, Pos.CENTER);

        Scene scene = new Scene(rootWrapper, 1400, 860);

        scene.setOnKeyPressed(e -> {
            // esc cosi si puo anche non smenare il mouse
            if (e.getCode() != javafx.scene.input.KeyCode.ESCAPE) return;
            if (cardDetailOverlay.isVisible()) {
                hideCardDetail();
            } else if (playerCardsOverlay.isVisible()) {
                hidePlayerCardsOverlay();
            }
        });

        return scene;
    }


    //HELPER

    private String eventDisplayName(String eventName) {
        return switch (eventName) {
            case "HUNT" -> "CACCIA";
            case "PICTURES" -> "PITTURE RUPESTRI";
            case "RITUAL" -> "RITUALE SCIAMANICO";
            case "SUSTENANCE" -> "SOSTENTAMENTO";
            default -> eventName.replace("_", " ");
        };
    }

    private StackPane findCardNode(int cardId) {
        for (javafx.scene.Node n : topRowBox.getChildren()) {
            if (n instanceof StackPane sp && Integer.valueOf(cardId).equals(sp.getUserData())) {
                return sp;
            }
        }
        for (javafx.scene.Node n : botRowBox.getChildren()) {
            if (n instanceof StackPane sp && Integer.valueOf(cardId).equals(sp.getUserData())) {
                return sp;
            }
        }
        return null;
    }

    private Image buildEventAnimationImage(int cardId) {
        var url = getClass().getResource("/gui/cards/card_" + cardId + ".png");
        return url != null ? new Image(url.toExternalForm()) : null;
    }

    // da solo un po difficile
    private StackPane buildCardDetailOverlay() {
        cardDetailScrim.setFill(Color.rgb(10, 14, 20, 0.22));
        cardDetailScrim.setOpacity(0.0);
        cardDetailScrim.widthProperty().bind(rootWrapper.widthProperty());
        cardDetailScrim.heightProperty().bind(rootWrapper.heightProperty());

        cardDetailImage.setPreserveRatio(true);
        cardDetailImage.fitWidthProperty().bind(Bindings.min(
                rootWrapper.widthProperty().multiply(0.40),
                rootWrapper.heightProperty().multiply(0.58 * CARD_W / CARD_H)
        ));
        cardDetailImage.fitHeightProperty().bind(Bindings.min(
                rootWrapper.heightProperty().multiply(0.58),
                rootWrapper.widthProperty().multiply(0.40 * CARD_H / CARD_W)
        ));
        cardDetailImage.setSmooth(true);
        cardDetailImage.setCache(true);
        cardDetailImage.setEffect(new DropShadow(30, Color.rgb(0, 0, 0, 0.50)));

        cardDetailTitle.setStyle("-fx-font-family:'SF Pro Display','Helvetica Neue',Arial;" +
                "-fx-font-size:30;-fx-font-weight:bold;-fx-text-fill:white;");

        cardDetailSubtitle.setWrapText(true);
        cardDetailSubtitle.setAlignment(Pos.CENTER);
        cardDetailSubtitle.setTextAlignment(TextAlignment.CENTER);
        cardDetailSubtitle.setStyle("-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:13;-fx-text-fill:rgba(255,255,255,0.72);");

        cardDetailHint.setWrapText(true);
        cardDetailHint.setAlignment(Pos.CENTER);
        cardDetailHint.setTextAlignment(TextAlignment.CENTER);
        cardDetailHint.setStyle("-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:12;-fx-text-fill:rgba(255,255,255,0.42);");

        Button closeBtn = new Button("Chiudi");
        closeBtn.setPrefHeight(40);
        closeBtn.setPrefWidth(150);
        closeBtn.setStyle("-fx-background-color:linear-gradient(to bottom, rgba(255,255,255,0.34), rgba(255,255,255,0.14));" +
                "-fx-background-radius:18;" +
                "-fx-text-fill:white;" +
                "-fx-font-size:14;" +
                "-fx-font-weight:bold;" +
                "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-border-color:rgba(255,255,255,0.36);" +
                "-fx-border-radius:18;" +
                "-fx-border-width:1.1;" +
                "-fx-cursor:hand;");
        closeBtn.setOnAction(e -> hideCardDetail());

        cardDetailPanel.setAlignment(Pos.CENTER);
        cardDetailPanel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        cardDetailPanel.setPadding(new Insets(26, 28, 24, 28));
        cardDetailPanel.setStyle("-fx-background-color:transparent;");

        // consumo il click sul pannello così il click fuori chiude, quello dentro no
        cardDetailGlass.setOnMouseClicked(e -> e.consume());

        cardDetailPanel.getChildren().setAll(
                cardDetailTitle,
                cardDetailSubtitle,
                cardDetailImage,
                cardDetailHint,
                closeBtn
        );

        Rectangle glassClip = new Rectangle();
        glassClip.setArcWidth(64);
        glassClip.setArcHeight(64);
        glassClip.widthProperty().bind(cardDetailGlass.widthProperty());
        glassClip.heightProperty().bind(cardDetailGlass.heightProperty());

        Rectangle glassWash = new Rectangle();
        glassWash.setArcWidth(64);
        glassWash.setArcHeight(64);
        glassWash.widthProperty().bind(cardDetailGlass.widthProperty());
        glassWash.heightProperty().bind(cardDetailGlass.heightProperty());
        glassWash.setFill(new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(255, 255, 255, 0.44)),
                new Stop(0.24, Color.rgb(250, 252, 255, 0.18)),
                new Stop(0.62, Color.rgb(196, 210, 228, 0.11)),
                new Stop(1.00, Color.rgb(255, 255, 255, 0.16))
        ));

        Rectangle topGlint = new Rectangle();
        topGlint.setHeight(2.0);
        topGlint.setArcWidth(60);
        topGlint.setArcHeight(60);
        topGlint.widthProperty().bind(cardDetailGlass.widthProperty().multiply(0.88));
        topGlint.setFill(new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(255, 255, 255, 0.00)),
                new Stop(0.5, Color.rgb(255, 255, 255, 0.92)),
                new Stop(1.0, Color.rgb(255, 255, 255, 0.00))
        ));
        topGlint.setMouseTransparent(true);

        cardDetailLiquidGlass.prefWidthProperty().bind(cardDetailGlass.widthProperty());
        cardDetailLiquidGlass.prefHeightProperty().bind(cardDetailGlass.heightProperty());

        cardDetailGlass.prefWidthProperty().bind(Bindings.min(
                rootWrapper.widthProperty().multiply(0.56),
                760
        ));
        cardDetailGlass.maxWidthProperty().bind(cardDetailGlass.prefWidthProperty());
        cardDetailGlass.prefHeightProperty().bind(Bindings.min(
                rootWrapper.heightProperty().multiply(0.84),
                760
        ));
        cardDetailGlass.maxHeightProperty().bind(cardDetailGlass.prefHeightProperty());
        cardDetailGlass.setMinSize(340, 430);
        cardDetailGlass.setClip(glassClip);
        cardDetailGlass.getChildren().setAll(glassWash, cardDetailLiquidGlass, cardDetailPanel, topGlint);
        StackPane.setAlignment(topGlint, Pos.TOP_CENTER);
        StackPane.setMargin(topGlint, new Insets(16, 0, 0, 0));
        cardDetailGlass.setEffect(new DropShadow(54, Color.rgb(36, 48, 68, 0.24)));

        StackPane panelHolder = new StackPane(cardDetailGlass);
        panelHolder.setPadding(new Insets(28));
        panelHolder.setPickOnBounds(false);

        cardDetailOverlay.getChildren().setAll(cardDetailScrim, panelHolder);
        cardDetailOverlay.setVisible(false);
        cardDetailOverlay.setMouseTransparent(true);
        cardDetailOverlay.setOpacity(1.0);

        // click sullo sfondo = chiusura rapida del dettaglio
        cardDetailOverlay.setOnMouseClicked(e -> hideCardDetail());

        return cardDetailOverlay;
    }

    private Button buildGlassCloseButton(Runnable onClose) {
        Button closeBtn = new Button("Chiudi");
        closeBtn.setPrefHeight(40);
        closeBtn.setPrefWidth(150);
        closeBtn.setStyle("-fx-background-color:linear-gradient(to bottom, rgba(255,255,255,0.34), rgba(255,255,255,0.14));" +
                "-fx-background-radius:18;" +
                "-fx-text-fill:white;" +
                "-fx-font-size:14;" +
                "-fx-font-weight:bold;" +
                "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-border-color:rgba(255,255,255,0.36);" +
                "-fx-border-radius:18;" +
                "-fx-border-width:1.1;" +
                "-fx-cursor:hand;");
        closeBtn.setOnAction(e -> onClose.run());
        return closeBtn;
    }

    private StackPane buildPlayerCardsOverlay() {
        playerCardsScrim.setFill(Color.rgb(10, 14, 20, 0.22));
        playerCardsScrim.setOpacity(0.0);
        playerCardsScrim.widthProperty().bind(rootWrapper.widthProperty());
        playerCardsScrim.heightProperty().bind(rootWrapper.heightProperty());

        playerCardsTitle.setStyle("-fx-font-family:'SF Pro Display','Helvetica Neue',Arial;" +
                "-fx-font-size:17;-fx-font-weight:bold;-fx-text-fill:white;");

        playerCardsSubtitle.setWrapText(true);
        playerCardsSubtitle.setAlignment(Pos.CENTER_LEFT);
        playerCardsSubtitle.setTextAlignment(TextAlignment.LEFT);
        playerCardsSubtitle.setStyle("-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:11;-fx-text-fill:rgba(255,255,255,0.70);");

        playerCardsHint.setWrapText(true);
        playerCardsHint.setAlignment(Pos.CENTER_LEFT);
        playerCardsHint.setTextAlignment(TextAlignment.LEFT);
        playerCardsHint.setStyle("-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:10;-fx-text-fill:rgba(255,255,255,0.36);");

        playerCardsRow.setAlignment(Pos.CENTER_LEFT);
        playerCardsRow.setPadding(new Insets(2, 6, 10, 6));

        playerCardsScroller.setContent(playerCardsRow);
        playerCardsScroller.setFitToHeight(true);
        playerCardsScroller.setPannable(true);
        playerCardsScroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        playerCardsScroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        playerCardsScroller.setStyle("-fx-background:transparent;-fx-background-color:transparent;" +
                "-fx-border-color:transparent;");
        playerCardsScroller.setMinHeight(CARD_H * 1.5);
        playerCardsScroller.setPrefViewportHeight(CARD_H * 1.55);
        playerCardsScroller.setPrefViewportWidth(CARD_W * 5.0);
        playerCardsScroller.viewportBoundsProperty().addListener((obs, oldVal, newVal) ->
                playerCardsRow.setMinHeight(newVal.getHeight()));

        Button closeBtn = buildGlassCloseButton(this::hidePlayerCardsOverlay);

        closeBtn.setPrefHeight(34);
        closeBtn.setPrefWidth(120);

        playerCardsPanel.setAlignment(Pos.CENTER_LEFT);
        playerCardsPanel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        playerCardsPanel.setPadding(new Insets(18, 22, 14, 22));
        playerCardsPanel.setStyle("-fx-background-color:transparent;");
        playerCardsGlass.setOnMouseClicked(e -> e.consume());

        playerCardsPanel.getChildren().setAll(
                playerCardsTitle,
                playerCardsSubtitle,
                playerCardsScroller,
                playerCardsHint,
                closeBtn
        );

        Rectangle glassClip = new Rectangle();
        glassClip.setArcWidth(64);
        glassClip.setArcHeight(64);
        glassClip.widthProperty().bind(playerCardsGlass.widthProperty());
        glassClip.heightProperty().bind(playerCardsGlass.heightProperty());

        Rectangle glassWash = new Rectangle();
        glassWash.setArcWidth(64);
        glassWash.setArcHeight(64);
        glassWash.widthProperty().bind(playerCardsGlass.widthProperty());
        glassWash.heightProperty().bind(playerCardsGlass.heightProperty());
        glassWash.setFill(new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(255, 255, 255, 0.44)),
                new Stop(0.24, Color.rgb(250, 252, 255, 0.18)),
                new Stop(0.62, Color.rgb(196, 210, 228, 0.11)),
                new Stop(1.00, Color.rgb(255, 255, 255, 0.16))
        ));

        Rectangle topGlint = new Rectangle();
        topGlint.setHeight(2.0);
        topGlint.setArcWidth(60);
        topGlint.setArcHeight(60);
        topGlint.widthProperty().bind(playerCardsGlass.widthProperty().multiply(0.88));
        topGlint.setFill(new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(255, 255, 255, 0.00)),
                new Stop(0.5, Color.rgb(255, 255, 255, 0.92)),
                new Stop(1.0, Color.rgb(255, 255, 255, 0.00))
        ));
        topGlint.setMouseTransparent(true);

        playerCardsLiquidGlass.prefWidthProperty().bind(playerCardsGlass.widthProperty());
        playerCardsLiquidGlass.prefHeightProperty().bind(playerCardsGlass.heightProperty());

        playerCardsGlass.prefWidthProperty().bind(Bindings.min(
                rootWrapper.widthProperty().multiply(0.60),
                820
        ));
        playerCardsGlass.maxWidthProperty().bind(playerCardsGlass.prefWidthProperty());
        playerCardsGlass.prefHeightProperty().bind(Bindings.min(
                rootWrapper.heightProperty().multiply(0.50),
                360
        ));
        playerCardsGlass.maxHeightProperty().bind(playerCardsGlass.prefHeightProperty());
        playerCardsGlass.setMinSize(380, 230);
        playerCardsGlass.setClip(glassClip);
        playerCardsGlass.getChildren().setAll(glassWash, playerCardsLiquidGlass, playerCardsPanel, topGlint);
        StackPane.setAlignment(topGlint, Pos.TOP_CENTER);
        StackPane.setMargin(topGlint, new Insets(16, 0, 0, 0));
        playerCardsGlass.setEffect(new DropShadow(54, Color.rgb(36, 48, 68, 0.24)));

        StackPane panelHolder = new StackPane(playerCardsGlass);
        panelHolder.setPadding(new Insets(28));
        panelHolder.setPickOnBounds(false);

        playerCardsOverlay.getChildren().setAll(playerCardsScrim, panelHolder);
        playerCardsOverlay.setVisible(false);
        playerCardsOverlay.setMouseTransparent(true);
        playerCardsOverlay.setOpacity(1.0);
        playerCardsOverlay.setOnMouseClicked(e -> hidePlayerCardsOverlay());

        return playerCardsOverlay;
    }

    private Image buildCardDetailImage(int cardId) {
        var url = getClass().getResource("/gui/cards/card_" + cardId + ".png");
        return url != null ? new Image(url.toExternalForm()) : null;
    }

    private String cardTypeLabel(int cardId) {
        if (cardId >= 200) return "Carta evento";
        if (cardId >= 100) return "Carta edificio";
        return "Carta personaggio";
    }

    //helper per pin totem sulla destra
    private void applyPlayerTotemColor(String nick, TotemColor color) {
        if (nick == null) return;

        Circle dot = playerTotemDots.get(nick);
        if (dot == null) return;

        if (color == null) {
            playerTotemColors.remove(nick);
            dot.setFill(Color.TRANSPARENT);
            dot.setStroke(Color.rgb(255, 255, 255, 0.18));
            dot.setStrokeWidth(0.8);
            return;
        }

        playerTotemColors.put(nick, color);

        String hex = TOTEM_HEX.getOrDefault(color, "#888");
        dot.setFill(Color.web(hex));
        dot.setStroke(color == TotemColor.WHITE
                ? Color.rgb(255, 255, 255, 0.92)
                : Color.rgb(255, 255, 255, 0.28));
        dot.setStrokeWidth(color == TotemColor.WHITE ? 1.5 : 0.9);
    }

    private StackPane buildOwnedCardPreview(int cardId) {
        Image img = buildCardDetailImage(cardId);
        ImageView view = new ImageView(img);
        view.setPreserveRatio(true);
        view.setFitWidth(CARD_W * 1.25);
        view.setFitHeight(CARD_H * 1.25);
        view.setSmooth(true);

        StackPane slot = new StackPane(view);
        slot.setPadding(new Insets(3));
        slot.setStyle("-fx-background-color:rgba(255,255,255,0.06);" +
                "-fx-background-radius:14;" +
                "-fx-border-color:rgba(255,255,255,0.12);" +
                "-fx-border-radius:18;" +
                "-fx-border-width:1;" +
                "-fx-cursor:hand;");
        slot.setEffect(new DropShadow(12, Color.rgb(0, 0, 0, 0.22)));
        slot.setOnMouseEntered(e -> slot.setEffect(new DropShadow(20, Color.rgb(255, 255, 255, 0.20))));
        slot.setOnMouseExited(e -> slot.setEffect(new DropShadow(18, Color.rgb(0, 0, 0, 0.22))));
        slot.setOnMouseClicked(e -> {
            e.consume();
            showCardDetail(cardId);
        });
        return slot;
    }

    private void showPlayerCardsOverlay(String playerNick) {
        List<Integer> cardIds = playerOwnedCards.getOrDefault(playerNick, List.of());
        playerCardsRow.getChildren().clear();

        for (Integer cardId : cardIds) {
            if (cardId != null) {
                playerCardsRow.getChildren().add(buildOwnedCardPreview(cardId));
            }
        }

        if (playerCardsRow.getChildren().isEmpty()) {
            Label empty = new Label("Questo giocatore non ha ancora carte.");
            empty.setStyle("-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                    "-fx-font-size:14;-fx-text-fill:rgba(255,255,255,0.58);");
            empty.setMinHeight(CARD_H * 1.25);
            playerCardsRow.getChildren().add(empty);
        }

        playerCardsTitle.setText("CARTE DI " + playerNick.toUpperCase());
        playerCardsSubtitle.setText(cardIds.isEmpty()
                ? "Nessuna carta raccolta al momento."
                : cardIds.size() == 1
                  ? "1 carta posseduta"
                  : cardIds.size() + " carte possedute");

        playerCardsOverlay.setVisible(true);
        playerCardsOverlay.setMouseTransparent(false);
        playerCardsOverlay.toFront();

        playerCardsGlass.setOpacity(0.0);
        playerCardsGlass.setScaleX(0.94);
        playerCardsGlass.setScaleY(0.94);
        playerCardsGlass.setTranslateY(24);
        playerCardsScrim.setOpacity(0.0);
        playerCardsScroller.setHvalue(0.0);

        mainRoot.setEffect(playerCardsBlur);
        playerCardsBlur.setRadius(0.0);

        javafx.animation.FadeTransition scrimFade =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(220), playerCardsScrim);
        scrimFade.setFromValue(0.0);
        scrimFade.setToValue(1.0);

        javafx.animation.FadeTransition panelFade =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(220), playerCardsGlass);
        panelFade.setFromValue(0.0);
        panelFade.setToValue(1.0);

        javafx.animation.ScaleTransition panelScale =
                new javafx.animation.ScaleTransition(javafx.util.Duration.millis(260), playerCardsGlass);
        panelScale.setFromX(0.94);
        panelScale.setFromY(0.94);
        panelScale.setToX(1.0);
        panelScale.setToY(1.0);

        javafx.animation.TranslateTransition panelSlide =
                new javafx.animation.TranslateTransition(javafx.util.Duration.millis(260), playerCardsGlass);
        panelSlide.setFromY(24);
        panelSlide.setToY(0);

        javafx.animation.Timeline blurIn = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                        javafx.util.Duration.millis(260),
                        new javafx.animation.KeyValue(
                                playerCardsBlur.radiusProperty(), 22.0, javafx.animation.Interpolator.EASE_BOTH
                        )
                )
        );

        new javafx.animation.ParallelTransition(
                scrimFade, panelFade, panelScale, panelSlide, blurIn
        ).play();
    }

    private void hidePlayerCardsOverlay() {
        if (!playerCardsOverlay.isVisible()) return;

        javafx.animation.FadeTransition scrimFade =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(180), playerCardsScrim);
        scrimFade.setFromValue(playerCardsScrim.getOpacity());
        scrimFade.setToValue(0.0);

        javafx.animation.FadeTransition panelFade =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(180), playerCardsGlass);
        panelFade.setFromValue(playerCardsGlass.getOpacity());
        panelFade.setToValue(0.0);

        javafx.animation.ScaleTransition panelScale =
                new javafx.animation.ScaleTransition(javafx.util.Duration.millis(180), playerCardsGlass);
        panelScale.setFromX(playerCardsGlass.getScaleX());
        panelScale.setFromY(playerCardsGlass.getScaleY());
        panelScale.setToX(0.96);
        panelScale.setToY(0.96);

        javafx.animation.TranslateTransition panelSlide =
                new javafx.animation.TranslateTransition(javafx.util.Duration.millis(180), playerCardsGlass);
        panelSlide.setFromY(playerCardsGlass.getTranslateY());
        panelSlide.setToY(18);

        javafx.animation.Timeline blurOut = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                        javafx.util.Duration.millis(180),
                        new javafx.animation.KeyValue(
                                playerCardsBlur.radiusProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH
                        )
                )
        );

        javafx.animation.ParallelTransition close = new javafx.animation.ParallelTransition(
                scrimFade, panelFade, panelScale, panelSlide, blurOut
        );

        close.setOnFinished(e -> {
            playerCardsOverlay.setVisible(false);
            playerCardsOverlay.setMouseTransparent(true);
            playerCardsRow.getChildren().clear();
            mainRoot.setEffect(null);
        });

        close.play();
    }

    private void showCardDetail(int cardId) {
        Image img = buildCardDetailImage(cardId);

        if (img == null) {
            // meglio dare feedback esplicito che lasciare un click morto
            showErrorMessage("Immagine non trovata per la carta " + cardId);
            return;
        }

        cardDetailImage.setImage(img);
        cardDetailTitle.setText("DETTAGLIO CARTA");
        cardDetailSubtitle.setText(cardTypeLabel(cardId) + "  •  ID " + cardId);

        cardDetailOverlay.setVisible(true);
        cardDetailOverlay.setMouseTransparent(false);
        cardDetailOverlay.setOpacity(1.0);
        cardDetailOverlay.toFront();

        cardDetailGlass.setOpacity(0.0);
        cardDetailGlass.setScaleX(0.92);
        cardDetailGlass.setScaleY(0.92);
        cardDetailGlass.setTranslateY(24);

        cardDetailScrim.setOpacity(0.0);

        mainRoot.setEffect(cardDetailBlur);
        cardDetailBlur.setRadius(0.0);

        javafx.animation.FadeTransition scrimFade =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(220), cardDetailScrim);
        scrimFade.setFromValue(0.0);
        scrimFade.setToValue(1.0);

        javafx.animation.FadeTransition panelFade =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(220), cardDetailGlass);
        panelFade.setFromValue(0.0);
        panelFade.setToValue(1.0);

        javafx.animation.ScaleTransition panelScale =
                new javafx.animation.ScaleTransition(javafx.util.Duration.millis(260), cardDetailGlass);
        panelScale.setFromX(0.92);
        panelScale.setFromY(0.92);
        panelScale.setToX(1.0);
        panelScale.setToY(1.0);

        javafx.animation.TranslateTransition panelSlide =
                new javafx.animation.TranslateTransition(javafx.util.Duration.millis(260), cardDetailGlass);
        panelSlide.setFromY(24);
        panelSlide.setToY(0);

        javafx.animation.Timeline blurIn = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                        javafx.util.Duration.millis(260),
                        new javafx.animation.KeyValue(
                                cardDetailBlur.radiusProperty(), 22.0, javafx.animation.Interpolator.EASE_BOTH
                        )
                )
        );

        new javafx.animation.ParallelTransition(
                scrimFade, panelFade, panelScale, panelSlide, blurIn
        ).play();
    }

    private void hideCardDetail() {
        if (!cardDetailOverlay.isVisible()) return;

        javafx.animation.FadeTransition scrimFade =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(180), cardDetailScrim);
        scrimFade.setFromValue(cardDetailScrim.getOpacity());
        scrimFade.setToValue(0.0);

        javafx.animation.FadeTransition panelFade =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(180), cardDetailGlass);
        panelFade.setFromValue(cardDetailGlass.getOpacity());
        panelFade.setToValue(0.0);

        javafx.animation.ScaleTransition panelScale =
                new javafx.animation.ScaleTransition(javafx.util.Duration.millis(180), cardDetailGlass);
        panelScale.setFromX(cardDetailGlass.getScaleX());
        panelScale.setFromY(cardDetailGlass.getScaleY());
        panelScale.setToX(0.95);
        panelScale.setToY(0.95);

        javafx.animation.TranslateTransition panelSlide =
                new javafx.animation.TranslateTransition(javafx.util.Duration.millis(180), cardDetailGlass);
        panelSlide.setFromY(cardDetailGlass.getTranslateY());
        panelSlide.setToY(18);

        javafx.animation.Timeline blurOut = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                        javafx.util.Duration.millis(180),
                        new javafx.animation.KeyValue(
                                cardDetailBlur.radiusProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH
                        )
                )
        );

        javafx.animation.ParallelTransition close =
                new javafx.animation.ParallelTransition(
                        scrimFade, panelFade, panelScale, panelSlide, blurOut
                );

        close.setOnFinished(e -> {
            cardDetailOverlay.setVisible(false);
            cardDetailOverlay.setMouseTransparent(true);
            cardDetailImage.setImage(null);
            mainRoot.setEffect(null);
        });

        close.play();
    }



    private int extractLabelNumber(Label label) {
        String digits = label.getText().replaceAll("[^0-9-]", "");
        if (digits.isEmpty() || digits.equals("-")) return 0;
        return Integer.parseInt(digits);
    }

    private void animateNumber(Label label, String prefix, int target) {
        int start = extractLabelNumber(label);

        javafx.animation.Timeline tl = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                        javafx.util.Duration.ZERO,
                        new javafx.animation.KeyValue(new javafx.beans.property.SimpleIntegerProperty(start), start)
                )
        );

        javafx.beans.property.IntegerProperty animatedValue =
                new javafx.beans.property.SimpleIntegerProperty(start);

        animatedValue.addListener((obs, oldVal, newVal) ->
                label.setText(prefix + newVal.intValue()));

        tl.getKeyFrames().setAll(
                new javafx.animation.KeyFrame(
                        javafx.util.Duration.seconds(1.2),
                        new javafx.animation.KeyValue(animatedValue, target, javafx.animation.Interpolator.EASE_BOTH)
                )
        );
        tl.play();
    }

    private void animateMyStats(int food, int prestige) {
        animateNumber(foodLabel, "", food);
        animateNumber(prestigeLabel, "", prestige);
    }

    private void animatePlayerStats(String nick, int food, int prestige) {
        Label fl = playerFoodLabels.get(nick);
        Label pl = playerPrestigeLabels.get(nick);

        if (fl != null) animateNumber(fl, "🍖 ", food);
        if (pl != null) animateNumber(pl, "★ ", prestige);
    }

    private void applyAnimatedEventStats(Map<String, int[]> stats) {
        for (Map.Entry<String, int[]> entry : stats.entrySet()) {
            String nick = entry.getKey();
            int[] values = entry.getValue();

            animatePlayerStats(nick, values[0], values[1]);
            if (nick.equals(myNick)) {
                animateMyStats(values[0], values[1]);
            }
        }
    }

    public void showEventResolution(String eventName, int cardId, Map<String, int[]> stats) {
        eventQueue.offer(new EventAnimationRequest(eventName, cardId, stats));
        if (!eventAnimationRunning) {
            playNextEventAnimation();
        }
    }

    private void playNextEventAnimation() {
        EventAnimationRequest req = eventQueue.poll();
        if (req == null) {
            eventAnimationRunning = false;
            return;
        }

        eventAnimationRunning = true;

        Image img = buildEventAnimationImage(req.cardId);
        if (img != null) {
            eventHeroCard.setImage(img);
        }

        eventHeroTitle.setText("RISOLUZIONE EVENTO " + eventDisplayName(req.eventName));
        eventOverlay.setVisible(true);
        eventOverlay.setOpacity(1.0);
        eventOverlay.toFront();

        StackPane source = findCardNode(req.cardId);
        double startDx = 0;
        double startDy = 0;

        if (source != null && source.getScene() != null) {
            Bounds sceneBounds = source.localToScene(source.getBoundsInLocal());
            Bounds localBounds = rootWrapper.sceneToLocal(sceneBounds);

            double sourceCx = localBounds.getMinX() + localBounds.getWidth() / 2.0;
            double sourceCy = localBounds.getMinY() + localBounds.getHeight() / 2.0;
            double rootCx = rootWrapper.getWidth() / 2.0;
            double rootCy = rootWrapper.getHeight() / 2.0;

            startDx = sourceCx - rootCx;
            startDy = sourceCy - rootCy;
        }

        eventHeroCard.setTranslateX(startDx+40);
        eventHeroCard.setTranslateY(startDy);
        eventHeroCard.setScaleX(1.0);
        eventHeroCard.setScaleY(1.0);
        eventHeroCard.setRotate(0.0);

        eventHeroTitle.setOpacity(0.0);
        eventHeroTitle.setTranslateY(24);

        eventScrim.setOpacity(0.0);
        eventFlash.setOpacity(0.0);

        centerScrollPane.setEffect(eventBlur);

        javafx.animation.Timeline intro = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                        new javafx.animation.KeyValue(eventHeroCard.translateXProperty(), startDx),
                        new javafx.animation.KeyValue(eventHeroCard.translateYProperty(), startDy),
                        new javafx.animation.KeyValue(eventHeroCard.scaleXProperty(), 1.0),
                        new javafx.animation.KeyValue(eventHeroCard.scaleYProperty(), 1.0),
                        new javafx.animation.KeyValue(eventHeroCard.rotateProperty(), 0.0),
                        new javafx.animation.KeyValue(eventHeroTitle.opacityProperty(), 0.0),
                        new javafx.animation.KeyValue(eventHeroTitle.translateYProperty(), 24),
                        new javafx.animation.KeyValue(eventScrim.opacityProperty(), 0.0),
                        new javafx.animation.KeyValue(eventFlash.opacityProperty(), 0.0),
                        new javafx.animation.KeyValue(eventBlur.radiusProperty(), 0.0)
                ),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(180),
                        new javafx.animation.KeyValue(eventFlash.opacityProperty(), 0.92, javafx.animation.Interpolator.EASE_OUT)
                ),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(320),
                        new javafx.animation.KeyValue(eventScrim.opacityProperty(), 0.55, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(eventBlur.radiusProperty(), 10.0, javafx.animation.Interpolator.EASE_BOTH)
                ),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(1250),
                        new javafx.animation.KeyValue(eventHeroCard.translateXProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(eventHeroCard.translateYProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(eventHeroCard.scaleXProperty(), 4.55, javafx.animation.Interpolator.SPLINE(0.18, 0.9, 0.24, 1.0)),
                        new javafx.animation.KeyValue(eventHeroCard.scaleYProperty(), 4.55, javafx.animation.Interpolator.SPLINE(0.18, 0.9, 0.24, 1.0)),
                        new javafx.animation.KeyValue(eventHeroCard.rotateProperty(), 720.0, javafx.animation.Interpolator.EASE_OUT),
                        new javafx.animation.KeyValue(eventHeroTitle.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(eventHeroTitle.translateYProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(eventFlash.opacityProperty(), 0.20, javafx.animation.Interpolator.EASE_BOTH)
                ),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(1500),
                        new javafx.animation.KeyValue(eventHeroCard.scaleXProperty(), 4.35, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(eventHeroCard.scaleYProperty(), 4.35, javafx.animation.Interpolator.EASE_BOTH)
                )
        );

        javafx.animation.PauseTransition statsDelay =
                new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.7));
        statsDelay.setOnFinished(e -> applyAnimatedEventStats(req.stats));

        javafx.animation.PauseTransition hold =
                new javafx.animation.PauseTransition(javafx.util.Duration.seconds(6));

        javafx.animation.Timeline outro = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(450),
                        new javafx.animation.KeyValue(eventOverlay.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(eventScrim.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(eventFlash.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(eventBlur.radiusProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(eventHeroCard.scaleXProperty(), 4.05, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(eventHeroCard.scaleYProperty(), 4.05, javafx.animation.Interpolator.EASE_BOTH)
                )
        );

        javafx.animation.SequentialTransition seq =
                new javafx.animation.SequentialTransition(
                        intro,
                        new javafx.animation.ParallelTransition(statsDelay, hold),
                        outro
                );

        seq.setOnFinished(e -> {
            centerScrollPane.setEffect(null);
            eventOverlay.setVisible(false);
            eventOverlay.setOpacity(1.0);
            eventHeroCard.setImage(null);
            playNextEventAnimation();
        });

        seq.play();
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
        topRowBox.setMaxWidth(Region.USE_PREF_SIZE);

        botRowBox.setAlignment(Pos.CENTER);
        botRowBox.setMinHeight(CARD_H + 16);
        botRowBox.setPadding(new Insets(8, 0, 8, 0));
        botRowBox.setMaxWidth(Region.USE_PREF_SIZE);

        HBox boardRow = new HBox(10);
        boardRow.setAlignment(Pos.CENTER);
        boardRow.setFillHeight(false);
        boardRow.getChildren().add(buildTurnOrderTrack());
        for (String letter : activeSpaces)
            boardRow.getChildren().add(buildBoardSpace(letter));

        VBox centerContent = new VBox(10, topRowBox, boardRow, botRowBox);
        centerContent.setAlignment(Pos.CENTER);
        centerContent.setFillWidth(false);
        centerContent.setPadding(new Insets(12, 16, 12, 16));
        centerContent.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane centeredWrapper = new StackPane(centerContent);
        centeredWrapper.setAlignment(Pos.CENTER);
        centeredWrapper.setPadding(Insets.EMPTY);

        centerScrollPane = new ScrollPane(centeredWrapper);
        centerScrollPane.setFitToWidth(true);
        centerScrollPane.setFitToHeight(true);
        centerScrollPane.setPannable(true);
        centerScrollPane.setStyle("-fx-background:transparent;-fx-background-color:transparent;" +
                "-fx-border-color:transparent;");

        centerScrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            centeredWrapper.setMinWidth(newBounds.getWidth());
            centeredWrapper.setMinHeight(newBounds.getHeight());
        });

        return centerScrollPane;
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

    private StackPane buildCardSlot(Integer cardId, boolean isUpper) {
        StackPane slot = new StackPane();
        slot.setPrefSize(CARD_W, CARD_H);
        slot.setStyle("-fx-background-radius:6;-fx-cursor:hand;");
        slot.setEffect(new DropShadow(6, Color.rgb(0, 0, 0, 0.5)));
        slot.setUserData(cardId != null ? cardId : -1); // ← traccia l'ID

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
        TotemColor color = playerTotemColors.get(nick);

        // Indicatore colore
        Circle dot = new Circle(5);
        playerTotemDots.put(nick, dot);
        applyPlayerTotemColor(nick, color);

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
                "-fx-border-radius:12;-fx-border-width:1;" +
                "-fx-cursor:hand;");
        card.setOnMouseClicked(e -> showPlayerCardsOverlay(nick));
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
        VBox myStats = new VBox(6,
                styledStat("🍖", foodLabel, "#F5C518"),
                styledStat("★",  prestigeLabel, "#FFD700")
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

    private HBox styledStat(String icon, Label valueLabel, String color) {
        Label iconLbl = new Label(icon);
        iconLbl.setStyle(labelStyle(18, color));
        valueLabel.setStyle("-fx-font-family:'SF Pro Display','Helvetica Neue',Arial;" +
                "-fx-font-size:22;-fx-font-weight:bold;-fx-text-fill:white;");
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

    public void updatePlayerOwnedCards(Map<String, List<Integer>> cardsByPlayer) {
        playerOwnedCards.clear();
        for (String player : players) {
            List<Integer> cards = cardsByPlayer.getOrDefault(player, List.of());
            playerOwnedCards.put(player, new ArrayList<>(cards));
        }
    }

    public void placeTotemOnSpace(String letter, TotemColor color, String nickname) {
        VBox slot = spaceTotemSlots.get(letter);
        if (slot == null) return;

        playerOnSpace.put(nickname, letter); // ← tieni traccia
        applyPlayerTotemColor(nickname, color);

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
            applyPlayerTotemColor(nick, color);
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
        // Svuota SOLO i totem dagli spazi — le carte rimangono sempre visibili
        spaceTotemSlots.values().forEach(v -> v.getChildren().clear());
        playerOnSpace.clear();
    }

    public void showErrorMessage(String msg) {
        turnLabel.setText("⚠ " + msg);
        turnLabel.setStyle(labelStyle(13, "#FF453A"));
    }

    public void updateBoardCards(List<Integer> topIds, List<Integer> botIds,
                                 boolean canPick, boolean pickFromTop) {
        // Aggiorna solo se arrivano dati non vuoti
        if (!topIds.isEmpty()) lastTopIds = new ArrayList<>(topIds);
        if (!botIds.isEmpty()) lastBotIds = new ArrayList<>(botIds);

        this.canPickCards = canPick;
        this.canPickTop   = canPick && pickFromTop;
        this.canPickBot   = canPick && !pickFromTop;

        rebuildCardRow(topRowBox, lastTopIds, true);
        rebuildCardRow(botRowBox, lastBotIds, false);

        topRowBox.setStyle(canPickTop
                ? "-fx-border-color:#34C759;-fx-border-width:2;-fx-border-radius:10;-fx-padding:6;"
                : "-fx-border-color:transparent;-fx-padding:6;");
        botRowBox.setStyle(canPickBot
                ? "-fx-border-color:#34C759;-fx-border-width:2;-fx-border-radius:10;-fx-padding:6;"
                : "-fx-border-color:transparent;-fx-padding:6;");
        topRowBox.setOpacity(canPickBot ? 0.55 : 1.0);
        botRowBox.setOpacity(canPickTop ? 0.55 : 1.0);
    }



    public void removeCardFromBoard(int cardId) {
        boolean changed = false;

        // Cerca e rimuove dalla top row
        if (lastTopIds.remove(Integer.valueOf(cardId))) {
            changed = true;
            // Trova il nodo nella UI e fai fade-out prima di ricostruire
            fadeOutCard(topRowBox, cardId, () -> rebuildCardRow(topRowBox, lastTopIds, true));
        }

        // Cerca e rimuove dalla bot row
        if (lastBotIds.remove(Integer.valueOf(cardId))) {
            changed = true;
            fadeOutCard(botRowBox, cardId, () -> rebuildCardRow(botRowBox, lastBotIds, false));
        }
    }

    private void fadeOutCard(HBox row, int cardId, Runnable onComplete) {
        // Trova il StackPane che contiene la carta con quell'ID
        row.getChildren().stream()
                .filter(n -> n instanceof StackPane)
                .filter(n -> cardId == (int) n.getUserData())
                .findFirst()
                .ifPresentOrElse(node -> {
                    javafx.animation.FadeTransition ft =
                            new javafx.animation.FadeTransition(
                                    javafx.util.Duration.millis(3000), node);
                    ft.setFromValue(1.0);
                    ft.setToValue(0.0);
                    ft.setOnFinished(e -> onComplete.run());
                    ft.play();
                }, onComplete); // se non trova il nodo, ricostruisce direttamente
    }

    private void rebuildCardRow(HBox row, List<Integer> ids, boolean fromTop) {
        row.getChildren().clear();
        boolean rowPickable = fromTop ? canPickTop : canPickBot;

        for (int i = 0; i < ids.size(); i++) {
            int cardId = ids.get(i);
            int index  = i;
            StackPane card = buildCardSlot(cardId, fromTop);

            if (isEventCard(cardId)) {
                card.setOpacity(0.65);
                card.setStyle("-fx-background-radius:6;" +
                        "-fx-border-color:#FFD700;-fx-border-width:2;" +
                        "-fx-border-radius:6;");
                card.setCursor(javafx.scene.Cursor.HAND);

                // visto che le carte evento non sono mai pescabili qui il click apre solo il dettaglio
                card.setOnMouseClicked(e -> showCardDetail(cardId));
            } else if (rowPickable && isMyTurn) {
                card.setOnMouseEntered(e ->
                        card.setEffect(new DropShadow(16, Color.web("#34C759"))));
                card.setOnMouseExited(e -> card.setEffect(null));

                card.setOnMouseClicked(e -> {
                    // tasto destro per il dettaglio rapido cosi non interferisce con la pick vera
                    if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                        showCardDetail(cardId);
                        return;
                    }

                    if (e.getButton() != javafx.scene.input.MouseButton.PRIMARY) return;

                    card.setOpacity(0.5);
                    card.setDisable(true);

                    new Thread(() -> {
                        try {
                            server.pickCard(myNick, index, fromTop);
                        } catch (Exception ex) {
                            javafx.application.Platform.runLater(() -> {
                                card.setOpacity(1.0);
                                card.setDisable(false);
                                showErrorMessage("Errore: " + ex.getMessage());
                            });
                        }
                    }, "gui-pick").start();
                });
            } else {
                card.setCursor(javafx.scene.Cursor.HAND);

                // quando non sono in una fase di pick il click serve a ispezionare la carta
                card.setOnMouseClicked(e -> showCardDetail(cardId));
            }

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

    public void showEventNotification(String eventName) { //TODO: DA ELIMINARE IN MY OPINION AVENDO AGGIUNTO L'ANIMAZIONE
        // Banner animato in cima
        String emoji = switch (eventName) {
            case "HUNT"       -> "🏹";
            case "PICTURES"   -> "🎨";
            case "RITUAL"     -> "🔮";
            case "SUSTENANCE" -> "🍖";
            default           -> "⚡";
        };

        Label banner = new Label(emoji + "  " + eventName.replace("_", " "));
        banner.setStyle("-fx-font-family:'SF Pro Display','Helvetica Neue',Arial;" +
                "-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:white;" +
                "-fx-background-color:rgba(255,200,0,0.25);" +
                "-fx-background-radius:20;-fx-padding:6 20 6 20;" +
                "-fx-border-color:rgba(255,200,0,0.5);" +
                "-fx-border-radius:20;-fx-border-width:1;");

        // Mostralo nell'header al posto della fase
        phaseLabel.setText(emoji + " " + eventName.replace("_", " "));
        phaseLabel.setStyle(labelStyle(13, "#FFD700"));

        // Sparisce dopo 2.5 secondi
        javafx.animation.PauseTransition pause =
                new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2.5));
        pause.setOnFinished(e -> phaseLabel.setStyle(labelStyle(12, "rgba(255,255,255,0.55)")));
        pause.play();
    }

    public void showToast(String message) {
        toastLabel.setText(message);
        toastLabel.setOpacity(1.0);
        toastLabel.setVisible(true);
        toastLabel.toFront();

        if (toastPause != null) {
            toastPause.stop();
        }

        toastPause = new javafx.animation.PauseTransition(
                javafx.util.Duration.seconds(5));
        toastPause.setOnFinished(e -> {
            javafx.animation.FadeTransition fade =
                    new javafx.animation.FadeTransition(
                            javafx.util.Duration.millis(250), toastLabel);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(ev -> {
                toastLabel.setVisible(false);
                toastLabel.setOpacity(1.0);
            });
            fade.play();
        });
        toastPause.playFromStart();
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

 //   private void showCardDetail(int cardId) {
        // TODO step 4 — modal con immagine grande + effetto frosted glass
 //   }

    private void showError(String msg) {
        turnLabel.setText("⚠ " + msg);
        turnLabel.setStyle(labelStyle(13, "#FF453A"));
    }

    // ─────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────



    private static boolean isEventCard(int cardId) {
        return cardId >= 200;
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