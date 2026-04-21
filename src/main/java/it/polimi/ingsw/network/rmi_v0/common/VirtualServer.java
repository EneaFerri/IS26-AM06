package it.polimi.ingsw.network.rmi_v0.common;

public interface VirtualServer {

    void connect(String nickname);

    void startGame();

    void placeTotem(String nickname, char boardSpace);

    void pickTopCard(String nickname, int index);

    void pickBottomCard(String nickname, int index);

    void endTurn(String nickname);
}