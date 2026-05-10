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
 * Unified client entry point — unico launcher per TUI e GUI.
 *
 * Avvio da terminale (IntelliJ o riga di comando):
 *   1 → TUI : tutta l'interazione rimane nel terminale (CLIView).
 *   2 → GUI : avvia JavaFX tramite Application.launch() e apre la finestra grafica.
 *
 * ── Note architetturali ────────────────────────────────────────────────────
 *  • Lo Scanner viene ottenuto da CLIView (getScanner()) in modo che esista
 *    UN SOLO Scanner su System.in per tutta la vita del processo TUI.
 *  • Application.launch() blocca il thread main fino alla chiusura della
 *    finestra JavaFX — comportamento corretto e atteso.
 *  • ClientLauncherGUI mantiene il proprio main() per poter essere avviato
 *    anche in standalone (es. run configuration JavaFX dedicata).
 */
public class ClientLauncher {

    public static void main(String[] args) throws Exception {

        // ── Scelta interfaccia ────────────────────────────────────────────
        //    Usiamo lo scanner di CLIView per non aprire mai due Scanner su
        //    System.in contemporaneamente nel percorso TUI.
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

        // ── Percorso GUI ──────────────────────────────────────────────────
        if (uiChoice.equals("2")) {
            // Application.launch() deve essere chiamato dal thread main ed è
            // bloccante: ritorna solo quando la finestra JavaFX viene chiusa.
            Application.launch(ClientLauncherGUI.class, args);
            return;
        }

        // ── Percorso TUI ──────────────────────────────────────────────────
        ClientModel model = new ClientModel();
        model.registerObserver(view);

        // Scelta trasporto
        System.out.println();
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   Seleziona tipo di connessione:     ║");
        System.out.println("║   1 → RMI                            ║");
        System.out.println("║   2 → Socket                         ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.print("Scelta: ");
        String netChoice = scanner.nextLine().trim();

        // IP server
        System.out.print("IP del server (INVIO = localhost): ");
        String input = scanner.nextLine().trim();
        String host  = input.isEmpty() ? "localhost" : input;

        // Wire-up
        if (netChoice.equals("1")) {
            // Percorso RMI
            System.setProperty("java.rmi.server.hostname", resolveLocalIp());
            RmiClient rmiClient = RmiClient.connect(host, model);
            view.setServer(rmiClient);
            view.doLoginCli();
        } else {
            // Percorso Socket (default)
            SocketClient      socketClient = new SocketClient(model);
            SocketServerProxy proxy        = socketClient.connect(host);
            view.setServer(proxy);
            view.doLoginCli();
        }

        // Il thread main rimane vivo; tutta l'interazione successiva è event-driven.
    }
}