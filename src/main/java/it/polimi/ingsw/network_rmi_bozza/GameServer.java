package it.polimi.ingsw.network_rmi_bozza;

import it.polimi.ingsw.model.Game;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Entry point del server.
 * Crea il model (Game), istanzia il GameController e lo registra nell'RMI registry.
 */
public class GameServer {

    public static final String SERVICE_NAME = "GameService";
    public static final int    RMI_PORT     = 1099;

    public static void main(String[] args) {
        try {
            Game game = new Game(1);

            GameController controller = new GameController(game);

            Registry registry = LocateRegistry.createRegistry(RMI_PORT);
            registry.rebind(SERVICE_NAME, controller);

            System.out.println("╔══════════════════════════════════╗");
            System.out.println("║  Server avviato su porta " + RMI_PORT + "    ║");
            System.out.println("║  In attesa di connessioni...      ║");
            System.out.println("╚══════════════════════════════════╝");

        } catch (Exception e) {
            System.err.println("[Server] Errore avvio: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
