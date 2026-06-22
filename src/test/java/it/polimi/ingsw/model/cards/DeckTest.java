package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.enums.Age;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests Deck methods that produce game-ready card lists from JSON data:
 * - getAllCards: returns a non-empty combined list.
 * - prepareTribeCards: validates player count bounds; returns only cards of the requested era
 *   containing both character and event cards.
 * - takeBuldingInGame: returns the correct per-era building count for a given player count.
 * - getFinalsEvents: returns exactly the Last_Event age cards.
 */
class DeckTest {

    private Deck deck;

    @BeforeEach
    void setUp() {
        assertDoesNotThrow(() -> {
            deck = new Deck();
        }, "Deck constructor should not throw if JSON files are present in src/main/resources.");
    }

    @Test
    void getAllCardsShouldCombineAllLists() {
        List<Card> allCards = deck.getAllCards();

        assertNotNull(allCards);
        assertFalse(allCards.isEmpty(), "The combined card list must not be empty.");
    }

    @Test
    void prepareTribeCardsShouldThrowExceptionForInvalidPlayerCount() {
        IllegalArgumentException exceptionLow = assertThrows(IllegalArgumentException.class,
                () -> deck.prepareTribeCards(1, Age.Era_I));
        assertEquals("Number of players must be between 2 and 5", exceptionLow.getMessage());

        IllegalArgumentException exceptionHigh = assertThrows(IllegalArgumentException.class,
                () -> deck.prepareTribeCards(6, Age.Era_I));
        assertEquals("Number of players must be between 2 and 5", exceptionHigh.getMessage());
    }

    @Test
    void takeBuldingInGameShouldReturnExactAmountsPerAgeBasedOnPlayers() {
        int players = 3;
        List<BuildingCard> buildingsInGame = deck.takeBuldingInGame(players);

        assertNotNull(buildingsInGame);

        long era1Count = buildingsInGame.stream().filter(c -> c.getAge() == Age.Era_I).count();
        long era2Count = buildingsInGame.stream().filter(c -> c.getAge() == Age.Era_II).count();
        long era3Count = buildingsInGame.stream().filter(c -> c.getAge() == Age.Era_III).count();

        assertEquals(2, era1Count, "For 3 players there should be 2 Era I building cards.");
        assertEquals(2, era2Count, "For 3 players there should be 2 Era II building cards.");
        assertEquals(4, era3Count, "For 3 players there should be 4 Era III building cards.");
    }

    @Test
    void getFinalsEventsShouldReturnOnlyLastEvents() {
        List<EventCard> finalEvents = deck.getFinalsEvents();

        assertNotNull(finalEvents);
        assertFalse(finalEvents.isEmpty(), "There should be at least one final event.");

        for (EventCard event : finalEvents) {
            assertEquals(Age.Last_Event, event.getAge(), "Final events must have Age.Last_Event.");
        }

        assertEquals(2, finalEvents.size(), "There should be exactly 2 final events in the deck.");
    }

    @Test
    void prepareTribeCardsShouldReturnCardsOfCorrectEraWithBothTypologies() {
        int players = 3;
        List<TribeCard> era1Tribes = deck.prepareTribeCards(players, Age.Era_I);

        assertNotNull(era1Tribes);
        assertFalse(era1Tribes.isEmpty(), "The Era I tribe deck must not be empty.");

        for (TribeCard card : era1Tribes) {
            assertEquals(Age.Era_I, card.getAge(), "All returned cards must belong to Era I.");
        }

        boolean hasCharacter = era1Tribes.stream().anyMatch(TribeCard::isCharacter);
        boolean hasEvent     = era1Tribes.stream().anyMatch(TribeCard::isEvent);
        assertTrue(hasCharacter, "The deck must contain at least one character card.");
        assertTrue(hasEvent,     "The deck must contain at least one event card.");
    }
}
