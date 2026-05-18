package it.polimi.ingsw.view.gui;

import it.polimi.ingsw.network.GameServerProxy;
import it.polimi.ingsw.network.rmi.client.RmiClient;
import it.polimi.ingsw.network.socket.client.SocketClient;
import it.polimi.ingsw.view.ClientModel;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import java.awt.Taskbar;
import java.awt.Toolkit;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.stage.Stage;
import javafx.animation.AnimationTimer;
import javafx.scene.shape.Rectangle;

import static it.polimi.ingsw.network.utils.NetworkUtils.resolveLocalIp;

public class ClientLauncherGUI extends Application {

    private Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Mesos");
        stage.setResizable(false);

        // Icona finestra
        stage.getIcons().add(new Image(
                getClass().getResourceAsStream("/icon.png")
        ));

        // Icona dock macOS
        if (Taskbar.isTaskbarSupported()) {
            var taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                taskbar.setIconImage(
                        Toolkit.getDefaultToolkit().getImage(
                                getClass().getResource("/icon.png")
                        )
                );
            }
        }

        MusicPlayer.start();
        stage.setOnCloseRequest(e -> MusicPlayer.stop());

        showConnectionScreen();
    }

    // ── Schermata di connessione ───────────────────────────────────────────

    private void showConnectionScreen() {
        AnimatedBackground animBg = new AnimatedBackground();

        // Glass card centrale
        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(44, 40, 44, 40));
        card.setMaxWidth(400);
        card.setStyle(styleGlassCard());

        // Titolo
        Label title = new Label("MESOS");
        title.setStyle("-fx-font-family:'SF Pro Display','Helvetica Neue',Arial;" +
                "-fx-font-size:42;-fx-font-weight:bold;-fx-text-fill:white;");

        Label subtitle = new Label("La tua tribù ti aspetta");
        subtitle.setStyle("-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:13;-fx-text-fill:rgba(255,255,255,0.45);");

        // Separatore
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color:rgba(255,255,255,0.10);");

        // Trasporto
        Label transportLabel = fieldLabel("Trasporto");
        ToggleGroup tg = new ToggleGroup();
        RadioButton rmiBtn    = styledRadio("RMI",    tg, true);
        RadioButton socketBtn = styledRadio("Socket", tg, false);
        HBox transportRow = new HBox(16, rmiBtn, socketBtn);
        transportRow.setAlignment(Pos.CENTER_LEFT);

        // IP
        Label ipLabel   = fieldLabel("Indirizzo server");
        TextField ipField = glassTextField("localhost");

        // Nickname
        Label nickLabel   = fieldLabel("Nickname");
        TextField nickField = glassTextField("Il tuo nome tribale");

        // Errore
        Label errorLabel = new Label();
        errorLabel.setWrapText(true);
        errorLabel.setStyle("-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:12;-fx-text-fill:#FF453A;");

        // Bottone connetti
        Button connectBtn = new Button("Connetti");
        connectBtn.setMaxWidth(Double.MAX_VALUE);
        connectBtn.setPrefHeight(48);
        connectBtn.setStyle(stylePrimaryButton());
        connectBtn.setDisable(true);
        nickField.textProperty().addListener((obs, oldVal, newVal) ->
                connectBtn.setDisable(newVal.trim().isEmpty())
        );

        connectBtn.setOnAction(e -> onConnect(
                nickField.getText().trim(),
                ipField.getText().trim().isEmpty() ? "localhost" : ipField.getText().trim(),
                rmiBtn.isSelected(),
                connectBtn, errorLabel
        ));

        card.getChildren().addAll(
                title, subtitle, sep,
                transportLabel, transportRow,
                ipLabel, ipField,
                nickLabel, nickField,
                errorLabel, connectBtn
        );

        StackPane cardWrapper = new StackPane(card);
        cardWrapper.setPadding(new Insets(60));

        StackPane root = new StackPane(animBg, cardWrapper);
        animBg.prefWidthProperty().bind(root.widthProperty());
        animBg.prefHeightProperty().bind(root.heightProperty());

        primaryStage.setScene(new Scene(root, 580, 660));
        primaryStage.show();
    }

    // ── Logica di connessione (thread separato) ────────────────────────────

    private void onConnect(String nick, String host, boolean useRmi,
                           Button btn, Label errorLabel) {
        btn.setDisable(true);
        btn.setText("Connessione…");
        errorLabel.setText("");

        new Thread(() -> {
            try {
                ClientModel model = new ClientModel();
                GUIView gui = new GUIView(primaryStage, model);
                model.registerObserver(gui);

                GameServerProxy proxy;
                if (useRmi) {
                    System.setProperty("java.rmi.server.hostname", resolveLocalIp());
                    proxy = RmiClient.connect(host, model);
                } else {
                    proxy = new SocketClient(model).connect(host);
                }

                gui.setServer(proxy);
                gui.setNick(nick);

                Platform.runLater(() -> {
                    gui.showLobbyScreen();
                    // Solo dopo che la UI è pronta, chiedi la lista
                    new Thread(() -> {
                        try { proxy.requestLobbyList(); }
                        catch (Exception ex) { ex.printStackTrace(); }
                    }, "gui-lobbylist").start();
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    errorLabel.setText("⚠  " + ex.getMessage());
                    btn.setDisable(false);
                    btn.setText("Connetti");
                });
            }
        }, "gui-connect").start();
    }

    // ── Style helpers ──────────────────────────────────────────────────────

    private String styleGlassCard() {
        return "-fx-background-color:rgba(255,255,255,0.07);" +
                "-fx-background-radius:24;" +
                "-fx-border-color:rgba(255,255,255,0.13);" +
                "-fx-border-radius:24;" +
                "-fx-border-width:1;";
    }

    private String stylePrimaryButton() {
        return "-fx-background-color:#007AFF;" +
                "-fx-background-radius:14;" +
                "-fx-text-fill:white;" +
                "-fx-font-size:15;" +
                "-fx-font-weight:bold;" +
                "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-cursor:hand;";
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text.toUpperCase());
        l.setStyle("-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:11;-fx-font-weight:bold;" +
                "-fx-text-fill:rgba(255,255,255,0.50);");
        return l;
    }

    private TextField glassTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefHeight(44);
        tf.setStyle("-fx-background-color:rgba(255,255,255,0.08);" +
                "-fx-background-radius:12;" +
                "-fx-border-color:rgba(255,255,255,0.11);" +
                "-fx-border-radius:12;" +
                "-fx-border-width:1;" +
                "-fx-text-fill:white;" +
                "-fx-font-size:15;" +
                "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-prompt-text-fill:rgba(255,255,255,0.28);" +
                "-fx-padding:0 16 0 16;");
        return tf;
    }

    private RadioButton styledRadio(String text, ToggleGroup group, boolean selected) {
        RadioButton rb = new RadioButton(text);
        rb.setToggleGroup(group);
        rb.setSelected(selected);
        rb.setStyle("-fx-text-fill:rgba(255,255,255,0.85);" +
                "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:14;");
        return rb;
    }
}

class AnimatedBackground extends Pane {

    private final AnimationTimer timer;
    private double t = 0;

    public AnimatedBackground() {
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                t += 0.004;
                repaint();
            }
        };
        timer.start();
    }

    private void repaint() {
        double w = getWidth();
        double h = getHeight();
        if (w == 0 || h == 0) return;

        // Tre colori che ciclano: giallo → rosso → viola → rosso → giallo
        double r1 = 0.5 + 0.5 * Math.sin(t);
        double r2 = 0.5 + 0.5 * Math.sin(t + Math.PI * 2.0 / 3.0);
        double r3 = 0.5 + 0.5 * Math.sin(t + Math.PI * 4.0 / 3.0);

        // Colori: giallo(255,200,0), rosso(200,20,20), viola(80,0,120)
        double red   = 200 * r1 + 200 * r2 + 80  * r3;
        double green = 200 * r1 + 20  * r2 + 0   * r3;
        double blue  = 0   * r1 + 20  * r2 + 120 * r3;

        double sum = r1 + r2 + r3;
        red   = Math.min(255, red   / sum);
        green = Math.min(255, green / sum);
        blue  = Math.min(255, blue  / sum);

        // Punto caldo che si muove
        double cx = w * (0.5 + 0.35 * Math.sin(t * 0.7));
        double cy = h * (0.5 + 0.35 * Math.cos(t * 0.5));

        Color hotColor = Color.rgb(
                (int) Math.min(255, red   * 1.4),
                (int) Math.min(255, green * 0.6),
                (int) Math.min(255, blue  * 1.2)
        );
        Color darkColor = Color.rgb(8, 4, 18); // quasi nero viola

        RadialGradient grad = new RadialGradient(
                0, 0,
                cx / w, cy / h,
                0.75,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0.0, hotColor),
                new Stop(0.5, Color.rgb(
                        (int) Math.min(255, red   * 0.5),
                        (int) Math.min(255, green * 0.15),
                        (int) Math.min(255, blue  * 0.6)
                )),
                new Stop(1.0, darkColor)
        );

        setBackground(new Background(new BackgroundFill(
                grad, CornerRadii.EMPTY, Insets.EMPTY)));
    }

    public void stop() { timer.stop(); }
}