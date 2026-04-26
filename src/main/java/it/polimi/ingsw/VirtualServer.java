package it.polimi.ingsw;

public interface VirtualServer<V extends VirtualView> {

    // --- LOBBY ---
    void loginFirstPlayer(String nickname, int numPlayers, V clientView) throws Exception;
    void login(String nickname, V clientView)                             throws Exception;
    /** Richiede la lista delle lobby aperte prima di scegliere dove entrare */
    void requestLobbyList(V clientView)                                   throws Exception;

    // --- FASE 1 ---
    void placeTotem(String nickname, char boardSpaceLetter)               throws Exception;

    // --- FASE 2 ---
    void pickCard(String nickname, int cardIndex, boolean fromTop)        throws Exception;
}