package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.BoardSpace;
import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.cards.Card;
import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.cards.TribeCard;
import it.polimi.ingsw.model.player.Player;

/**
 * Defines all valid game actions that mutate the model.
 * Implemented by {@link Game} and called by
 * {@link it.polimi.ingsw.controller.GameController}.
 */
public interface GameActions {

    // --- LOBBY ---

    /**
     * Adds a player to the game during the login phase.
     *
     * @param player the {@link Player} to add
     * @throws IllegalStateException    if the game has already started
     * @throws IllegalArgumentException if the nickname is already taken
     */
    void addPlayer(Player player);

    // --- SETUP ---

    /**
     * Starts the game, deals cards, and sets up the first round.
     *
     * @throws IllegalStateException if the game has already started or the player count is invalid
     */
    void startGame();

    // --- PHASE 1: TOTEM PLACEMENT ---
    // Renamed for clarity; boardSpace is now an explicit parameter.

    /**
     * Places the player's totem on the chosen offer space.
     *
     * @param player     the player placing their totem
     * @param boardSpace the offer space to occupy
     * @throws IllegalStateException if the game is not in the {@code OFFER_SPACE_CHOOSE} state
     *                               or the space is already occupied
     */
    void placeTotemOnOfferSpace(Player player, BoardSpace boardSpace);

    // --- PHASE 2: CARD SELECTION ---
    // The controller calls these when the player makes a choice.

    /**
     * Picks a card from the board and applies it to the player.
     *
     * @param player the player picking the card
     * @param card   the card to pick
     * @throws IllegalStateException if the game is not in the {@code PICKING_CARD} state
     *                               or the player has no remaining picks for that row
     */
    void pickCard(Player player, Card card);

    // --- END OF PLAYER TURN: TOTEM RETURN ---
    // Totem return is triggered automatically after all picks are exhausted.

    /**
     * Returns the player's totem to the turn-order track after they finish picking cards.
     *
     * @param player the player whose totem should be returned
     */
    void returnTotemToTurnOrder(Player player);

    // --- END OF ROUND ---

    /**
     * Resolves all event cards on the board for the current round, in turn order.
     *
     * @throws IllegalStateException if the game is not in the {@code EVENTS} state
     */
    void resolveEvents();

    /**
     * Advances to the next round: shifts card rows, deals new cards, and resets turn order.
     */
    void nextRound();

    // --- END OF GAME ---

    /**
     * Ends the game and triggers final score computation.
     *
     * @throws IllegalStateException if the game is not in the {@code END} state
     */
    void endGame();
}
