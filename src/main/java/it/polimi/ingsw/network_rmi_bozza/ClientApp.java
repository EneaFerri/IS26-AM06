package it.polimi.ingsw.network_rmi_bozza;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

/**
 * Entry point del client.
 * Si connette all'RMI registry del server, crea la VirtualView (stub callback),
 * poi guida l'utente nella fase di login via CLI.
 *
 * Uso:
 *   java ClientApp [host]
 *   Se host è omesso, usa "localhost".
 */
public class ClientApp {

    public static void main(String[] args) {
        String host = (args.length > 0) ? args[0] : "localhost";

        CliView view = new CliView();
        Scanner scanner = new Scanner(System.in);

        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║     Connessione al server...      ║");
        System.out.println("╚══════════════════════════════════╝");

        try {
            // 1. Recupera lo stub del server dal registry
            Registry registry = LocateRegistry.getRegistry(host, GameServer.RMI_PORT);
            IGameServer serverStub = (IGameServer) registry.lookup(GameServer.SERVICE_NAME);
            System.out.println("Connesso al server: " + host + ":" + GameServer.RMI_PORT);

            // 2. Crea la VirtualView (oggetto RMI locale che riceve le callback)
            VirtualView virtualView = new VirtualView(view);

            // 3. Crea il controller locale
            ClientController controller = new ClientController(serverStub, virtualView);

            // 4. Login via CLI
            String nickname = view.askNickname(scanner);

            // Determiniamo se siamo il primo client chiedendo al server
            // (il server risponderà con errore se la lobby esiste già: gestito in onError)
            System.out.print("Sei il primo giocatore della lobby? (s/n): ");
            String answer = scanner.nextLine().trim().toLowerCase();

            if (answer.equals("s")) {
                int numPlayers = view.askNumberOfPlayers(scanner);
                controller.loginAsFirst(nickname, numPlayers);
            } else {
                controller.login(nickname);
            }

            // 5. Rimani in ascolto delle callback RMI (il thread RMI le gestisce autonomamente)
            System.out.println("\n[In attesa di aggiornamenti dal server...]");
            // Il thread principale aspetta — le callback arrivano sul thread RMI
            Thread.currentThread().join();

        } catch (InterruptedException e) {
            System.out.println("Client terminato.");
        } catch (Exception e) {
            System.err.println("[Client] Errore: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
