package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.EventType;
import it.polimi.ingsw.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BuildingCardTest {

    private BuildingCard testBuildingCard;

    @BeforeEach
    void setUp() {
        // Uso l'ID 100 reale dal buildings.json (Era_I, foodCost 5, prestigePoints 3)
        testBuildingCard = new BuildingCard(100, Age.Era_I, 5, 3) {};
    }

    @Test
    void constructorShouldInitializeFieldsCorrectly() {
        assertEquals(5, testBuildingCard.getFoodCost());
        assertEquals(3, testBuildingCard.getPrestigePoint());
        assertEquals(100, testBuildingCard.getID());
        assertEquals(Age.Era_I, testBuildingCard.getAge());
    }

    @Test
    void defaultMethodsShouldCoverEmptyAndDefaultReturns() {
        assertDoesNotThrow(() -> testBuildingCard.applyEventEffect(EventType.HUNT, null));
        assertDoesNotThrow(() -> testBuildingCard.onAddedToPlayer(null));

        assertEquals(0, testBuildingCard.getEndEffectPoints(null));
        assertTrue(testBuildingCard.isBuilding());
    }

    @Test
    void pickShouldCallGamePickBuildingCard() {
        // 1. Flag per registrare la chiamata
        final boolean[] methodCalled = {false};

        // 2. Creiamo un mock manuale di Game
        Game fakeGame = new Game(1) {
            @Override
            public void pickBuildingCard(Player player, BuildingCard card) {
                methodCalled[0] = true;
            }
        };

        // 3. Creiamo un giocatore fittizio
        Player fakePlayer = new Player("TestPlayer", null);

        // 4. Testiamo il metodo
        testBuildingCard.pick(fakePlayer, fakeGame);

        // 5. Verifichiamo il flag
        assertTrue(methodCalled[0], "Il metodo pickBuildingCard del Game NON è stato chiamato dalla carta!");
    }
}