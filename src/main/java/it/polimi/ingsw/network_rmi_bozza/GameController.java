package it.polimi.ingsw.network_rmi_bozza;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;
import it.polimi.ingsw.model.enums.TotemColor;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller lato server.
 * Implementa IGameServer (interfaccia RMI) e agisce sul Model (Game).
 * Gestisce la fase di login e l'avvio della partita.
 */
public class GameController extends UnicastRemoteObject implements IGameServer {

    private final Game game;

    /** Mappa nickname -> stub RMI del client, per le callback */
    private final List<IGameClient> connectedClients = new ArrayList<>();
    private final List<String>      connectedNicknames = new ArrayList<>();

    /** Numero di giocatori scelto dal primo client */
    private int expectedPlayers = -1;

    /** Colori totem assegnati in ordine di ingresso */
    private static final TotemColor[] TOTEM_COLORS = TotemColor.values();

    public GameController(Game game) throws RemoteException {
        super();
        this.game = game;
    }

    // ------------------------------------------------------------------ //
    //  IGameServer — metodi chiamati via RMI dai client                   //
    // ------------------------------------------------------------------ //

    @Override
    public synchronized void loginFirstPlayer(String nickname, int numPlayers, IGameClient clientStub)
            throws RemoteException {

        System.out.println("[Server] Primo giocatore: " + nickname
                + " | Giocatori richiesti: " + numPlayers);

        // Validazione
        if (numPlayers < 2 || numPlayers > 5) {
            clientStub.onError("Numero di giocatori non valido (2-5).");
            return;
        }
        if (!connectedNicknames.isEmpty()) {
            clientStub.onError("La lobby è già stata creata.");
            return;
        }

        expectedPlayers = numPlayers;
        registerPlayer(nickname, clientStub);
        clientStub.onLoginAccepted(nickname, expectedPlayers);

        broadcastPlayerJoined(nickname);
        checkAndStartIfReady();
    }

    @Override
    public synchronized void login(String nickname, IGameClient clientStub)
            throws RemoteException {

        System.out.println("[Server] Nuovo client: " + nickname);

        // Validazione
        if (expectedPlayers == -1) {
            clientStub.onError("Nessuna lobby attiva. Attendere il primo giocatore.");
            return;
        }
        if (connectedNicknames.size() >= expectedPlayers) {
            clientStub.onError("Lobby piena (" + expectedPlayers + "/" + expectedPlayers + ").");
            return;
        }
        if (connectedNicknames.contains(nickname)) {
            clientStub.onError("Nickname '" + nickname + "' già in uso.");
            return;
        }

        registerPlayer(nickname, clientStub);
        clientStub.onLoginAccepted(nickname, expectedPlayers);

        broadcastPlayerJoined(nickname);
        checkAndStartIfReady();
    }

    // ------------------------------------------------------------------ //
    //  Logica privata                                                      //
    // ------------------------------------------------------------------ //

    private void registerPlayer(String nickname, IGameClient clientStub) {
        TotemColor color = TOTEM_COLORS[connectedNicknames.size() % TOTEM_COLORS.length];
        Player player = new Player(nickname, new Totem(color));
        game.addPlayer(player);

        connectedNicknames.add(nickname);
        connectedClients.add(clientStub);

        System.out.println("[Server] Registrato: " + nickname
                + " (totem: " + color + ") — in lobby: "
                + connectedNicknames.size() + "/" + expectedPlayers);
    }

    /**
     * Notifica tutti i client già in lobby che un nuovo giocatore si è aggiunto.
     * (Il client appena entrato la riceve insieme agli altri.)
     */
    private void broadcastPlayerJoined(String newNickname) {
        int current  = connectedNicknames.size();
        int expected = expectedPlayers;

        for (int i = 0; i < connectedClients.size(); i++) {
            try {
                connectedClients.get(i).onPlayerJoined(newNickname, current, expected);
            } catch (RemoteException e) {
                System.err.println("[Server] Errore callback su "
                        + connectedNicknames.get(i) + ": " + e.getMessage());
            }
        }
    }

    /** Avvia la partita se il numero di giocatori atteso è stato raggiunto. */
    private void checkAndStartIfReady() {
        if (connectedNicknames.size() < expectedPlayers) return;

        System.out.println("\n=== INIZIO PARTITA ===");
        System.out.println("Giocatori: " + connectedNicknames);

        List<String> nicknames = new ArrayList<>(connectedNicknames);

        for (int i = 0; i < connectedClients.size(); i++) {
            try {
                connectedClients.get(i).onGameStarting(nicknames);
            } catch (RemoteException e) {
                System.err.println("[Server] Errore notifica avvio su "
                        + connectedNicknames.get(i) + ": " + e.getMessage());
            }
        }

        // Avvia il model — da qui in poi il gioco procede
        game.startGame();
        System.out.println("[Server] Model avviato. GameState: " + game.getStatus());
    }
}
