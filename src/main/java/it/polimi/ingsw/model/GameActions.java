package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.board.BoardSpace;
import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.cards.EventCard;
import it.polimi.ingsw.model.cards.TribeCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.model.player.Player;

import java.util.List;

public interface GameActions {
    void addPlayer(Player player);
    void startGame();
    void setUpFirstRound();
    void TotemPositionStartRound(Player player, BoardSpace boardSpace);
    void CardSelectionPhase();
    void resolveLowerEvents();
    void updateAge();
    void TotemPositionEndRound();
    void nextTurn();
    void endGame();
    /*
    Player getPlayerInTurn();
    List<Player> getPlayers();
    GameState getStatus();
    Age getCurrentAge();
    Board getBoard();
    List<BoardSpace> getFreeBoardSpaces();
    List<TribeCard> getAvailableTribeCards();
    List<BuildingCard> getAvailableBuildingCards();
    List<EventCard> getLowRowEvents();
    */
}

