package it.polimi.ingsw.persistence;

import java.util.List;

/**
 * JSON-serializable snapshot of the entire game (Game).
 * Aggregates all sub-snapshots; contains no references to model objects.
 */
public record GameSnapshot(
        int    gameId,
        int    numberOfPlayers,
        String gameState,                    // GameState.name()
        String currentAge,                   // Age.name()
        String playerInTurnNick,             // null if no player is currently in turn
        List<String> currentRoundOrderNicks,

        List<PlayerSnapshot> players,
        BoardSnapshot        board,
        TurnOrderSnapshot    turnOrder,

        List<Integer> deckEraI,              // cardIds remaining in Era I deck
        List<Integer> deckEraII,
        List<Integer> deckEraIII,
        List<Integer> finalEventIds,         // cardIds of remaining final event cards
        List<Integer> buildingsInGameIds,    // cardIds of buildings in this game

        int topPicks,
        int bottomPicks,
        List<String> botNicknames            // nicknames of players replaced by bots at save time
) {}
