package it.polimi.ingsw;

public interface VirtualServer<V extends VirtualView> {

    // --- LOBBY ---
    void loginFirstPlayer(String nickname, int numPlayers, V clientView) throws Exception;
    void login(String nickname, V clientView)                             throws Exception;
    /** Entra in una lobby specifica per ID (evita di finire nella prima disponibile). */
    void loginToLobby(String nickname, int lobbyId, V clientView)        throws Exception;
    /** Richiede la lista di tutte le lobby attive (aperte + in corso). */
    void requestLobbyList(V clientView)                                   throws Exception;

    // --- FASE 1 ---
    void placeTotem(String nickname, char boardSpaceLetter)               throws Exception;

    // --- FASE 2 ---
    void pickCard(String nickname, int cardIndex, boolean fromTop)        throws Exception;

    // === SPECTATOR ===
    /** Entra in una partita in corso come spettatore (solo lettura, no azioni). */
    void joinAsSpectator(String nickname, int lobbyId, V clientView)     throws Exception;
    /** Lascia la modalità spettatore e torna alla lobby. */
    void leaveSpectator(String nickname, V clientView)                   throws Exception;
    // === END SPECTATOR ===
}