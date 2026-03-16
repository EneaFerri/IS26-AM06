package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.board.BoardSpace;
import it.polimi.ingsw.model.cards.Deck;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.model.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Game implements GameActions {
    private final int gameID;

    private final List<Player> players;
    private Player playerInTurn;
    private int numberOfPlayers;

    private GameState gameState;
    private Age currentAge;

    private final Deck mainDeck;
    private final Deck deck1;
    private final Deck deck2;
    private final Deck deck3;

    private final Board gameBoard;

    public Game(int gameID, Board gameBoard, Deck mainDeck, Deck deck1, Deck deck2, Deck deck3) {
        this.gameID = gameID;
        this.players = new ArrayList<>();
        this.playerInTurn = null;
        this.numberOfPlayers = 0;
        this.gameState = GameState.LOGIN;
        this.currentAge = null;
        this.gameBoard = gameBoard;
        this.mainDeck = mainDeck;
        this.deck1 = deck1;
        this.deck2 = deck2;
        this.deck3 = deck3;
    }

    public int getGameID() {
        return gameID;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Player getCurrentPlayer() {
        return playerInTurn;
    }

    public int getNumberOfPlayers() {
        return numberOfPlayers;
    }

    public GameState getStatus() {
        return gameState;
    }

    public Age getCurrentAge() {
        return currentAge;
    }

    public Board getBoard() {
        return gameBoard;
    }

    public Deck getMainDeck() {
        return mainDeck;
    }

    public Deck getDeck1() {
        return deck1;
    }

    public Deck getDeck2() {
        return deck2;
    }

    public Deck getDeck3() {
        return deck3;
    }

    public void addPlayer(Player player) {
        //TODO
    }

    public void setUpFirstTurn() {
        //TODO
    }

    public void startGame() { //forse meglio qui quella che da il via? non so discutiamone
        if (players.isEmpty()) {
            throw new IllegalStateException("Cannot start a game without players");
        }

        gameState = GameState.START;
        currentAge = Age.Era_I;
        setUpFirstTurn();
        gameState = GameState.START;
    }

    public void TotemPositionStartRound(Player player, BoardSpace boardSpace) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(boardSpace, "boardSpace cannot be null");

        if (gameBoard == null) {
            throw new IllegalStateException("Board not initialized");
        }

        gameBoard.placeTotem(player.getTotem(), boardSpace);
    }

    public void CardSelectionPhase() {
        // TODO: implementare la fase di pescaggio delle carte
    }

    public void resolveLowerEvents() {
        // TODO
    }

    public void updateAge() {
        // TODO: aggiornare l'era quando il gioco raggiunhge quella successiva
    }

    public void TotemPositionEndRound() {
        // TODO: salvare pos finale totem
    }

    public void nextTurn() {
        //TODO
    }

    public void endGame() {
        gameState = GameState.END;
    }
}
