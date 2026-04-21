package it.polimi.ingsw.view;

import java.util.ArrayList;
import java.util.List;

/**
 * Model lato client.
 * Mantiene lo stato locale visibile dalla View (nickname, giocatori in lobby, ecc.)
 * e notifica gli observer (CLIView) ad ogni aggiornamento.
 *
 * Non conosce nulla di RMI: riceve i dati da RmiClient e li propaga via Observer.
 * Corrisponde a ClientModel nell'esempio dei prof.
 */
public class ClientModel {

    private String       myNickname;
    private int          expectedPlayers;
    private List<String> lobbyPlayers = new ArrayList<>();

    private final List<ModelObserver> observers = new ArrayList<>();

    public void registerObserver(ModelObserver observer) {
        observers.add(observer);
    }

    // ------------------------------------------------------------------ //
    //  Metodi chiamati da RmiClient (callback dal server)                 //
    // ------------------------------------------------------------------ //

    public void onLoginAccepted(String nickname, int expected) {
        this.myNickname      = nickname;
        this.expectedPlayers = expected;
        for (ModelObserver o : observers) o.onLoginAccepted(nickname, expected);
    }

    public void onPlayerJoined(String nickname, int currentCount, int expected) {
        if (!lobbyPlayers.contains(nickname)) lobbyPlayers.add(nickname);
        for (ModelObserver o : observers) o.onPlayerJoined(nickname, currentCount, expected);
    }

    public void onGameStarting(List<String> playerNicknames) {
        this.lobbyPlayers = new ArrayList<>(playerNicknames);
        for (ModelObserver o : observers) o.onGameStarting(playerNicknames);
    }

    public void onError(String message) {
        for (ModelObserver o : observers) o.onError(message);
    }

    // Getters
    public String       getMyNickname()     { return myNickname; }
    public int          getExpectedPlayers(){ return expectedPlayers; }
    public List<String> getLobbyPlayers()   { return List.copyOf(lobbyPlayers); }
}
