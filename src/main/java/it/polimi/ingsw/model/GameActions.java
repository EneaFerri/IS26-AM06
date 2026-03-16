package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.BoardSpace;
import it.polimi.ingsw.model.player.Player;

public interface GameActions {
    void addPlayer(Player player);
    void startGame();
    void setUpFirstTurn();
    void TotemPositionStartRound(Player player, BoardSpace boardSpace);
    void CardSelectionPhase();
    void resolveLowerEvents();
    void updateAge();
    void TotemPositionEndRound();
    void nextTurn();
    void endGame();
}
