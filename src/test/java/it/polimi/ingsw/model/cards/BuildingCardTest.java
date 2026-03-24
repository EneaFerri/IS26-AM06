package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.enums.Age;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BuildingCardTest {

    private BuildingCard testBuildingCard;

    @BeforeEach
    void setUp() {
        testBuildingCard = new BuildingCard(201, Age.Era_I, 2, 5) {};
    }

    @Test
    void constructorShouldInitializeFieldsCorrectly() {

        assertEquals(2, testBuildingCard.getFoodCost(), "Il costo in cibo (foodCost) non è stato inizializzato correttamente.");
        assertEquals(5, testBuildingCard.getPrestigePoint(), "I punti prestigio (prestigePoint) non sono stati inizializzati correttamente.");

        assertEquals(201, testBuildingCard.getID(), "L'ID ereditato dalla classe Card non è corretto.");
        assertEquals(Age.Era_I, testBuildingCard.getAge(), "L'Age ereditata dalla classe Card non è corretta.");
    }
}