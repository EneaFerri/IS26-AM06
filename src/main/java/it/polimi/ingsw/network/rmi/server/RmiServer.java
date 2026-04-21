package it.polimi.ingsw.network.rmi.server;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;
import it.polimi.ingsw.network.common.GameView;
import it.polimi.ingsw.network.rmi.client.VirtualServerRmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class RmiServer extends UnicastRemoteObject implements VirtualServerRmi {

    private final it.polimi.ingsw.network.rmi.server.GameController controller;
    private final Game game;

    private final Map<String, VirtualViewRmi> clients = new HashMap<>();

    public RmiServer() throws RemoteException {
        this.game = new Game(1);
        this.controller = new it.polimi.ingsw.network.rmi.server.GameController(game);
    }

    public static void main(String[] args) throws Exception {
        Registry registry = LocateRegistry.createRegistry(1234);
        registry.rebind("GameServer", new RmiServer());
        System.out.println("Server ready");
    }

    @Override
    public void connect(VirtualViewRmi client) throws RemoteException {

    }

    @Override
    public void placeTotem(String player, int pos) throws RemoteException {

    }

    @Override
    public void pickCard(String player, int index) throws RemoteException {

    }

    @Override
    public void connectClient(VirtualViewRmi client) {
        // gestisci dopo login
    }


    @Override
    public void connect(String nickname, TotemColor col) {
        game.addPlayer(new Player(nickname, new Totem(col))); // TODO adattare costruttore
    }

    @Override
    public void startGame() {
        controller.startGame();
        broadcast();
    }

    @Override
    public void placeTotem(String nickname, char boardSpace) {
        try {
            controller.placeTotem(nickname, boardSpace);
            broadcast();
        } catch (Exception e) {
            sendError(nickname, e.getMessage());
        }
    }

    private void broadcast() {
        GameView view = buildView();

        for (VirtualViewRmi client : clients.values()) {
            try {
                client.showGameState(view);
            } catch (Exception ignored) {}
        }
    }

    private void sendError(String nickname, String msg) {
        try {
            clients.get(nickname).showError(msg);
        } catch (Exception ignored) {}
    }

    private GameView buildView() {
        return new GameView(
                game.getStatus(),
                game.getCurrentPlayer() != null ? game.getCurrentPlayer().getNickname() : null,
                game.getPlayers().stream().map(Player::getNickname).toList(),
                game.getBoard().getTopRowTribe().stream().map(Object::toString).toList(),
                game.getBoard().getLowRowTribe().stream().map(Object::toString).toList()
        );
    }
}