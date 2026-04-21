package it.polimi.ingsw.network.rmi_v0.common;

import it.polimi.ingsw.model.enums.GameState;
import java.io.Serializable;
import java.util.List;

public class GameView implements Serializable {

    private GameState state;
    private String currentPlayer;
    private List<String> players;
    private List<String> topCards;
    private List<String> bottomCards;

    public GameView(GameState state, String currentPlayer,
                    List<String> players,
                    List<String> topCards,
                    List<String> bottomCards) {
        this.state = state;
        this.currentPlayer = currentPlayer;
        this.players = players;
        this.topCards = topCards;
        this.bottomCards = bottomCards;
    }

    public GameState getState() { return state; }
    public String getCurrentPlayer() { return currentPlayer; }
    public List<String> getPlayers() { return players; }
    public List<String> getTopCards() { return topCards; }
    public List<String> getBottomCards() { return bottomCards; }
}