package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests TribeCard default method implementations on the abstract base class:
 * - isCharacter / isEvent: both false by default.
 * - isAvailableForPlayers: true by default.
 * - isBuilding / isTribe: false and true respectively (inherited overrides from Card).
 */
class TribeCardTest {

    private TribeCard testTribeCard;

    @BeforeEach
    void setUp() {
        testTribeCard = new TribeCard(0, Age.Era_I) {
            @Override
            public void pick(Player player, Game game) {}
        };
    }

    @Test
    void constructorShouldPassValuesToSuperClass() {

        assertEquals(0, testTribeCard.getID(), "L'ID della carta non è stato passato/inizializzato correttamente.");
        assertEquals(Age.Era_I, testTribeCard.getAge(), "L'Age della carta non è stata passata/inizializzata correttamente.");
    }

    @Test
    void defaultBooleanMethodsShouldReturnCorrectValues() {
        assertFalse(testTribeCard.isCharacter(), "isCharacter dovrebbe essere false di default in TribeCard");
        assertFalse(testTribeCard.isEvent(), "isEvent dovrebbe essere false di default in TribeCard");
        assertTrue(testTribeCard.isAvailableForPlayers(3), "isAvailableForPlayers dovrebbe essere true di default in TribeCard");
        assertFalse(testTribeCard.isBuilding(), "isBuilding dovrebbe essere false (override da Card)");
        assertTrue(testTribeCard.isTribe(), "isTribe dovrebbe essere true (override da Card)");
    }
}