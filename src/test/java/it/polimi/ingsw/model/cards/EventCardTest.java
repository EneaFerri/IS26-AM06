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

        testEventCard = new EventCard(401, Age.Era_I, EventType.HUNT) {
            @Override
            public void resolve(List<Player> players) {
                // Implementazione dummy: non testiamo questo metodo qui perché è astratto. Il vero test del resolve verrà fatto classe per classe
            }
        };
    }

    @Test
    void constructorShouldInitializeTypeAndPassValuesToSuper() {
        assertEquals(EventType.HUNT, testEventCard.getType(), "L'EventType non è stato inizializzato correttamente.");

        assertEquals(401, testEventCard.getID(), "L'ID ereditato non è corretto.");
        assertEquals(Age.Era_I, testEventCard.getAge(), "L'Age ereditata non è corretta.");
    }
}