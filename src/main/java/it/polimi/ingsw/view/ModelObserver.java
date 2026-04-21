package it.polimi.ingsw.view;

import java.util.List;

/**
 * Interfaccia Observer lato client.
 * Implementata da CLIView (e in futuro da GUIView).
 * Corrisponde a ModelObserver nell'esempio dei prof.
 */
public interface ModelObserver {

    void onLoginAccepted(String nickname, int expectedPlayers);

    void onPlayerJoined(String nickname, int currentCount, int expected);

    void onGameStarting(List<String> playerNicknames);

    void onError(String message);
}
