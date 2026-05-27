package it.polimi.ingsw.persistence;

import java.util.List;

/**
 * Snapshot JSON-serializzabile dell'intera partita (Game).
 * Aggrega tutti i sotto-snapshot; non contiene riferimenti a oggetti del model.
 */
public record GameSnapshot(
        int    gameId,
        int    numberOfPlayers,
        String gameState,                    // GameState.name()
        String currentAge,                   // Age.name()
        String playerInTurnNick,             // null se nessuno in turno
        List<String> currentRoundOrderNicks,

        List<PlayerSnapshot> players,
        BoardSnapshot        board,
        TurnOrderSnapshot    turnOrder,

        List<Integer> deckEraI,              // cardId residui nel mazzo Era I
        List<Integer> deckEraII,
        List<Integer> deckEraIII,
        List<Integer> finalEventIds,         // cardId eventi finali residui
        List<Integer> buildingsInGameIds,    // cardId edifici in partita

        int topPicks,
        int bottomPicks,
        List<String> botNicknames            // nickname dei player sostituiti da bot al momento del salvataggio
) {}
