package it.polimi.ingsw;

import it.polimi.ingsw.network.rmi.client.RmiClient;
import it.polimi.ingsw.network.socket.client.SocketClient;
import it.polimi.ingsw.network.socket.client.SocketServerProxy;
import it.polimi.ingsw.view.ClientModel;
import it.polimi.ingsw.view.cli.CLIView;

import java.util.Scanner;

import static it.polimi.ingsw.network.utils.NetworkUtils.resolveLocalIp;

/**
 * Client entry point.
 *
 * Asks the user which network transport to use (RMI or Socket), then wires
 * up the chosen implementation.  The {@link CLIView} and {@link ClientModel}
 * are identical in both paths — only the network adapter differs.
 *
 * ── Strategy pattern ───────────────────────────────────────────────────────
 *   Both RmiClient and SocketServerProxy implement {@link it.polimi.ingsw.network.GameServerProxy}.
 *   CLIView holds a GameServerProxy reference and never cares about the transport.
 */
public class ClientLauncher {

    public static void main(String[] args) throws Exception {

        // ── Shared components ─────────────────────────────────────────────
        CLIView     view  = new CLIView();
        ClientModel model = new ClientModel();
        model.registerObserver(view);

        Scanner scanner = view.getScanner();   // single scanner on System.in

        // ── Transport choice ──────────────────────────────────────────────
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   Seleziona tipo di connessione:     ║");
        System.out.println("║   1 → RMI                            ║");
        System.out.println("║   2 → Socket                         ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.print("Scelta: ");
        String choice = scanner.nextLine().trim();

        // ── Server IP ─────────────────────────────────────────────────────
        System.out.print("IP del server (INVIO = localhost): ");
        String input = scanner.nextLine().trim();
        String host  = input.isEmpty() ? "localhost" : input;

        // ── Wire-up ───────────────────────────────────────────────────────
        if (choice.equals("1")) {
            // RMI path
            System.setProperty("java.rmi.server.hostname",
                    resolveLocalIp());

            RmiClient rmiClient = RmiClient.connect(host, model); // factory: looks up registry + wraps stub
            view.setServer(rmiClient);
            view.doLoginCli();

        } else {
            // Socket path
            SocketClient socketClient = new SocketClient(model);
            SocketServerProxy proxy   = socketClient.connect(host);
            view.setServer(proxy);                // SocketServerProxy implements GameServerProxy
            view.doLoginCli();
        }

        // Block the main thread — all further interaction is event-driven.
        try { Thread.currentThread().join(); }
        catch (InterruptedException e) { System.out.println("Client terminato."); }
    }
}