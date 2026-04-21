package it.polimi.ingsw;

import java.util.List;

/**
 * Interfaccia technology-agnostic che definisce i metodi usati dal server
 * per notificare i client (callback / aggiornamenti view).
 *
 * Implementazioni concrete: VirtualViewRmi (RMI), VirtualViewSocket (Socket — futuro)
 */
public interface VirtualView {

    void onLoginAccepted(String nickname, int expectedPlayers) throws Exception;

    void onPlayerJoined(String nickname, int currentCount, int expected) throws Exception;

    void onGameStarting(List<String> playerNicknames) throws Exception;

    void onError(String message) throws Exception;
}
