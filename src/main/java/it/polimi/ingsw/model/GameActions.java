package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.BoardSpace;
import it.polimi.ingsw.model.cards.Buildings.BuildingCard;
import it.polimi.ingsw.model.cards.Card;
import it.polimi.ingsw.model.cards.TribeCard;
import it.polimi.ingsw.model.player.Player;

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
    void pickCard(Player player, Card card);
    void pickTribeCard(Player player, TribeCard card);
    void pickBuildingCard(Player player, BuildingCard card);

    // --- FINE TURNO GIOCATORE: ritorno totem (era TotemPositionEndRound) ---
    // aggiunto position perché ha effetti immediati (cibo/penalità)
    void returnTotemToTurnOrder(Player player);

    // --- FINE ROUND ---
    void resolveLowerEvents();
    void updateAge();
    void nextRound();

    // --- FINE PARTITA ---
    void endGame();
}

