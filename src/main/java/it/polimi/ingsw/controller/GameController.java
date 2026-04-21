package it.polimi.ingsw.controller;

import it.polimi.ingsw.VirtualView;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;
import it.polimi.ingsw.model.enums.TotemColor;

import java.util.List;

/**
 * Controller MVC lato server.
 * Non conosce RMI né Socket: lavora solo su VirtualView (interfaccia base).
 * La lista dei client viene popolata direttamente dai metodi di login,
 * che ricevono lo stub come parametro in modo atomico.
 */
public class GameController {

    private final Game game;
    private final List<VirtualView> clients = new java.util.ArrayList<>();
    private int expectedPlayers = -1;

    private static final TotemColor[] TOTEM_COLORS = TotemColor.values();

    public GameController(Game game) {
        this.game = game;
    }

    public synchronized void loginFirstPlayer(String nickname, int numPlayers, VirtualView caller) {
        try {
            if (numPlayers < 2 || numPlayers > 5) {
                caller.onError("Numero di giocatori non valido (2-5).");
                return;
            }
            if (expectedPlayers != -1) {
                caller.onError("La lobby è già stata creata.");
                return;
            }

            expectedPlayers = numPlayers;
            clients.add(caller);
            registerPlayer(nickname);

            System.out.println("[Controller] Primo giocatore: " + nickname
                    + " | Attesi: " + numPlayers);

            caller.onLoginAccepted(nickname, expectedPlayers);
            broadcastPlayerJoined(nickname);
            checkAndStartIfReady();

        } catch (Exception e) {
            System.err.println("[Controller] Errore loginFirstPlayer: " + e.getMessage());
        }
    }

    public synchronized void login(String nickname, VirtualView caller) {
        try {
            if (expectedPlayers == -1) {
                caller.onError("Nessuna lobby attiva. Attendere il primo giocatore.");
                return;
            }
            if (game.getNumberOfPlayers() >= expectedPlayers) {
                caller.onError("Lobby piena (" + expectedPlayers + "/" + expectedPlayers + ").");
                return;
            }
            if (game.getPlayers().stream().anyMatch(p -> p.getNickname().equals(nickname))) {
                caller.onError("Nickname '" + nickname + "' già in uso.");
                return;
            }

            clients.add(caller);
            registerPlayer(nickname);

            System.out.println("[Controller] " + nickname
                    + " [" + game.getNumberOfPlayers() + "/" + expectedPlayers + "]");

            caller.onLoginAccepted(nickname, expectedPlayers);
            broadcastPlayerJoined(nickname);
            checkAndStartIfReady();

        } catch (Exception e) {
            System.err.println("[Controller] Errore login: " + e.getMessage());
        }
    }

    private void registerPlayer(String nickname) {
        TotemColor color = TOTEM_COLORS[(game.getNumberOfPlayers()) % TOTEM_COLORS.length];
        Player player = new Player(nickname, new Totem(color));
        game.addPlayer(player);
    }

    private void broadcastPlayerJoined(String newNickname) throws Exception {
        int current  = game.getNumberOfPlayers();
        int expected = expectedPlayers;
        for (VirtualView client : clients) {
            client.onPlayerJoined(newNickname, current, expected);
        }
    }

    private void checkAndStartIfReady() throws Exception {
        if (game.getNumberOfPlayers() < expectedPlayers) return;

        List<String> nicknames = game.getPlayers()
                .stream()
                .map(Player::getNickname)
                .toList();

        System.out.println("\n=== INIZIO PARTITA ===");
        System.out.println("Giocatori: " + nicknames);

        for (VirtualView client : clients) {
            client.onGameStarting(nicknames);
        }

        game.startGame();
        System.out.println("[Server] GameState: " + game.getStatus());
    }
}
