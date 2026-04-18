package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.EventType;
import it.polimi.ingsw.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventCardTest {

    private EventCard testEventCard;

    @BeforeEach
    void setUp() {
        testEventCard = new EventCard(200, Age.Era_I, EventType.HUNT) {
            @Override
            public void resolve(List<Player> players) {
                // Implementazione dummy
            }
        };
    }

    @Test
    void constructorShouldInitializeTypeAndPassValuesToSuper() {
        assertEquals(EventType.HUNT, testEventCard.getType(), "L'EventType non è stato inizializzato correttamente.");
        assertEquals(200, testEventCard.getID(), "L'ID ereditato non è corretto.");
        assertEquals(Age.Era_I, testEventCard.getAge(), "L'Age ereditata non è corretta.");
    }

    @Test
    void pickShouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> testEventCard.pick(null, null)
        );
        assertEquals("Event cards cannot be picked by players", exception.getMessage());
    }

    @Test
    void booleanTypeMethodsShouldReturnCorrectValues() {
        assertFalse(testEventCard.isCharacter(), "isCharacter dovrebbe essere false in EventCard");
        assertTrue(testEventCard.isEvent(), "isEvent dovrebbe essere true in EventCard");
    }
}