package it.polimi.ingsw;

/**
 * Interfaccia technology-agnostic che definisce le azioni che il client
 * può invocare sul server.
 *
 * Implementazioni concrete:
 *  - VirtualServerRmi  (RMI)
 *  - VirtualServerSocket (Socket — futuro)
 */
public interface VirtualServer<V extends VirtualView> {

    // --- LOBBY ---
    void loginFirstPlayer(String nickname, int numPlayers, V clientView) throws Exception;
    void login(String nickname, V clientView) throws Exception;

    // --- FASE 1: PIAZZAMENTO TOTEM ---
    /**
     * Il giocatore sceglie su quale BoardSpace piazzare il proprio totem.
     * @param nickname     il giocatore che agisce
     * @param boardSpaceLetter  la lettera dello spazio (es. 'B')
     */
    void placeTotem(String nickname, char boardSpaceLetter) throws Exception;

    // --- FASE 2: SELEZIONE CARTA ---
    /**
     * Il giocatore sceglie una carta da prendere.
     * @param nickname  il giocatore che agisce
     * @param cardIndex indice della carta nella lista mostrata dalla CLI (0-based)
     * @param fromTop   true = riga superiore, false = riga inferiore
     */
    void pickCard(String nickname, int cardIndex, boolean fromTop) throws Exception;
}