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
    // --- LOBBY ---
    void addPlayer(Player player);
    void startGame();

    // --- SETUP ---
    void setUpFirstRound();

    // --- FASE 1: piazzamento totem (era TotemPositionStartRound) ---
    // rinominato per chiarezza, aggiunto boardSpace come parametro esplicito
    void placeTotemOnOfferSpace(Player player, BoardSpace boardSpace);

    // --- FASE 2: selezione carte (era CardSelectionPhase — troppo vago) ---
    // il controller invoca questi quando il giocatore sceglie
    void pickTribeCard(Player player, TribeCard card, boolean fromTopRow);
    void pickBuildingCard(Player player, BuildingCard card, boolean fromTopRow);

    // --- FINE TURNO GIOCATORE: ritorno totem (era TotemPositionEndRound) ---
    // aggiunto position perché ha effetti immediati (cibo/penalità)
    void returnTotemToTurnOrder(Player player);

    // --- FINE ROUND ---
    void resolveLowerEvents();
    void updateAge();
    void nextTurn();

    // --- FINE PARTITA ---
    void endGame();
}

