package it.polimi.ingsw.network.rmi_v0.server;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.board.BoardSpace;
import it.polimi.ingsw.model.player.Player;

public class GameController {

    private final Game game;

    public GameController(Game game) {
        this.game = game;
    }

    private Player getPlayer(String nickname) {
        return game.getPlayers().stream()
                .filter(p -> p.getNickname().equals(nickname))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
    }

    public void placeTotem(String nickname, char spaceLetter) {
        Player p = getPlayer(nickname);
        BoardSpace space = game.getBoard().getBoardSpace(spaceLetter);

        game.placeTotemOnOfferSpace(p, space);
    }

    public void pickTopCard(String nickname, int index) {
        Player p = getPlayer(nickname);

        var cards = game.getBoard().getAvailableUpperTribeCards();
        game.pickCard(p, cards.get(index));
    }

    public void pickBottomCard(String nickname, int index) {
        Player p = getPlayer(nickname);

        var cards = game.getBoard().getAvailableBottomTribeCards();
        game.pickCard(p, cards.get(index));
    }

    public void startGame() {
        game.startGame();
    }
}