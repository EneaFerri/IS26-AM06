package it.polimi.ingsw;

import it.polimi.ingsw.network.rmi.client.RmiClient;

/**
 * Interfaccia technology-agnostic che definisce le azioni che il client
 * può invocare sul server. Corrisponde alle azioni di gioco (GameActions)
 * esposte verso la rete.
 *
 * Implementazioni concrete: VirtualServerRmi (RMI), VirtualServerSocket (Socket — futuro)
 */
public interface VirtualServer {

    void loginFirstPlayer(String nickname, int numPlayers, RmiClient rmiClient) throws Exception;

    void login(String nickname, RmiClient rmiClient) throws Exception;
}
