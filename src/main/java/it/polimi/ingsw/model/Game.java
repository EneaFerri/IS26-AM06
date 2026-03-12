package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.cards.Deck;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.model.player.Player;

import java.util.List;

public class Game {
    int gameID;

    List<Player> players;
    Player playerInTurn;
    int numberOfPlayers;

    GameState gameState;
    Age currentAge;

    Deck mainDeck;
    Deck Deck1;
    Deck Deck2;
    Deck Deck3;

    Board gameBoard;

}
