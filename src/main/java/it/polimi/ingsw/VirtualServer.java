package it.polimi.ingsw;

/**
 * Interfaccia technology-agnostic che definisce le azioni che il client
 * può invocare sul server. Corrisponde alle azioni di gioco (GameActions)
 * esposte verso la rete.
 *
 * Implementazioni concrete: VirtualServerRmi (RMI), VirtualServerSocket (Socket — futuro)
 */
public interface VirtualServer {

    void loginFirstPlayer(String nickname, int numPlayers) throws Exception;

    void login(String nickname) throws Exception;
}
