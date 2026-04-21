package it.polimi.ingsw.network_rmi_bozza;

import java.rmi.RemoteException;

/**
 * Controller lato client.
 * Riceve le azioni dall'utente (tramite View) e le inoltra al server via RMI stub.
 * Separa la View dalla conoscenza della rete.
 */
public class ClientController {

    /** Stub RMI del server — il client lo usa come fosse un oggetto locale */
    private final IGameServer virtualServer;

    /** Stub RMI di questo client — passato al server per le callback */
    private final VirtualView virtualView;

    private String nickname;

    public ClientController(IGameServer virtualServer, VirtualView virtualView) {
        this.virtualServer = virtualServer;
        this.virtualView   = virtualView;
    }

    /**
     * Primo giocatore: login con scelta del numero di giocatori.
     */
    public void loginAsFirst(String nickname, int numPlayers) {
        this.nickname = nickname;
        try {
            virtualServer.loginFirstPlayer(nickname, numPlayers, virtualView);
        } catch (RemoteException e) {
            System.err.println("[Client] Errore RMI loginFirstPlayer: " + e.getMessage());
        }
    }

    /**
     * Giocatori successivi: semplice login.
     */
    public void login(String nickname) {
        this.nickname = nickname;
        try {
            virtualServer.login(nickname, virtualView);
        } catch (RemoteException e) {
            System.err.println("[Client] Errore RMI login: " + e.getMessage());
        }
    }

    public String getNickname() {
        return nickname;
    }
}
