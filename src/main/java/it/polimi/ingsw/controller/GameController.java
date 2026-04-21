package it.polimi.ingsw.controller;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.board.BoardSpace;
import it.polimi.ingsw.model.cards.Card;
import it.polimi.ingsw.model.player.Player;

public class GameController {

    private final Game game;

    public GameController(Game game) {
        this.game = game;
    }

    public void placeTotem(String nickname, char spaceLetter) {
        Player p = getPlayer(nickname);
        BoardSpace space = game.getBoard().getBoardSpace(spaceLetter);

        game.placeTotemOnOfferSpace(p, space);
    }

    public void pickCard(String nickname, int cardId) {
        Player p = getPlayer(nickname);
        Card c = getCard(cardId);

        game.pickCard(p,c);

    }

    public void startGame() {
        game.startGame();
    }

    //internal methods
    private Player getPlayer(String nickname) {
        return game.getPlayers().stream()
                .filter(p -> p.getNickname().equals(nickname))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
    }

    private Card getCard(int cardId){
        return game.getMainDeck().getAllCards().stream()
                .filter(c-> c.getID() == cardId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
    }
}
