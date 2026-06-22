package it.polimi.ingsw;

import it.polimi.ingsw.network.rmi.client.RmiClient;
import it.polimi.ingsw.network.socket.client.SocketClient;
import it.polimi.ingsw.network.socket.client.SocketServerProxy;
import it.polimi.ingsw.view.ClientModel;
import it.polimi.ingsw.view.cli.CLIView;
import it.polimi.ingsw.view.gui.ClientLauncherGUI;
import javafx.application.Application;

import java.util.Scanner;

import static it.polimi.ingsw.network.utils.NetworkUtils.resolveLocalIp;

/**
 * Unified client entry point — single launcher for both TUI and GUI.
 *
 * <p>Launch from the terminal (IntelliJ or command line):
 * <ul>
 *   <li>1 → TUI : all interaction stays in the terminal ({@link CLIView}).</li>
 *   <li>2 → GUI : starts JavaFX via {@code Application.launch()} and opens the graphical window.</li>
 * </ul>
 *
 * <p>Architectural notes:
 * <ul>
 *   <li>The {@link Scanner} is obtained from {@link CLIView#getScanner()} so that exactly one
 *       Scanner exists on {@code System.in} for the entire lifetime of the TUI process.</li>
 *   <li>{@code Application.launch()} blocks the main thread until the JavaFX window is closed —
 *       the expected and correct behavior.</li>
 *   <li>{@link ClientLauncherGUI} keeps its own {@code main()} so it can also be launched
 *       standalone (e.g. a dedicated JavaFX run configuration).</li>
 * </ul>
 */
public class ClientLauncher {

    /**
     * Application entry point. Prompts the user to choose TUI or GUI and the network transport,
     * then wires up the client components and starts the chosen interface.
     *
     * @param args command-line arguments (not used)
     * @throws Exception if the connection or JavaFX launch fails
     */
    public static void main(String[] args) throws Exception {

        System.setProperty("apple.awt.application.name", "Mesos");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Mesos");

        // ── Interface selection ───────────────────────────────────────────
        //    Use CLIView's Scanner so we never open two Scanners on
        //    System.in simultaneously on the TUI path.
        CLIView     view    = new CLIView();
        Scanner     scanner = view.getScanner();

        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║        Benvenuto in  M E S O S       ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║   Seleziona interfaccia di gioco:    ║");
        System.out.println("║   1 → TUI  (solo terminale)          ║");
        System.out.println("║   2 → GUI  (finestra grafica JavaFX) ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.print("Scelta: ");
        String uiChoice = scanner.nextLine().trim();

        // ── GUI path ──────────────────────────────────────────────────────
        if (uiChoice.equals("2")) {
            // Application.launch() must be called from the main thread and is
            // blocking: it returns only when the JavaFX window is closed.
            Application.launch(ClientLauncherGUI.class, args);
            return;
        }

        // ── TUI path ──────────────────────────────────────────────────────
        ClientModel model = new ClientModel();
        model.registerObserver(view);

        // Transport selection
        System.out.println();
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   Seleziona tipo di connessione:     ║");
        System.out.println("║   1 → RMI                            ║");
        System.out.println("║   2 → Socket                         ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.print("Scelta: ");
        String netChoice = scanner.nextLine().trim();

        // Server IP
        System.out.print("IP del server (INVIO = localhost): ");
        String input = scanner.nextLine().trim();
        String host  = input.isEmpty() ? "localhost" : input;

        // Wire-up
        if (netChoice.equals("1")) {
            // RMI path
            System.setProperty("java.rmi.server.hostname", resolveLocalIp());
            RmiClient rmiClient = RmiClient.connect(host, model);
            view.setServer(rmiClient);
            view.doLoginCli();
        } else {
            // Socket path (default)
            SocketClient      socketClient = new SocketClient(model);
            SocketServerProxy proxy        = socketClient.connect(host);
            view.setServer(proxy);
            view.doLoginCli();
        }

        // The main thread stays alive; all subsequent interaction is event-driven.
        try { Thread.currentThread().join(); }
        catch (InterruptedException e) { System.out.println("Client terminato."); }
    }
}
