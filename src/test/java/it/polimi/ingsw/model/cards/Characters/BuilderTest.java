package it.polimi.ingsw.model.cards.Characters;

import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests that Builder correctly stores its prestige value and building discount alongside inherited fields. */
class BuilderTest {

    private Builder testBuilder;

    @BeforeEach
    void setUp() {
        testBuilder = new Builder(602, Age.Era_I, 4, 3, 1);
    }

    @Test
    void constructorShouldInitializeFieldsAndPassValuesToSuper() {
        assertEquals(3, testBuilder.getPrestigeValue(), "Il valore di prestigio non è stato inizializzato correttamente.");
        assertEquals(1, testBuilder.getDiscountForBuildings(), "Lo sconto di costruzione non è stato inizializzato correttamente.");

        assertEquals(602, testBuilder.getID(), "L'ID non è stato inizializzato correttamente.");
        assertEquals(Age.Era_I, testBuilder.getAge(), "L'Age non è stata inizializzata correttamente.");
        assertEquals(4, testBuilder.getTag(), "Il tag non è stato inizializzato correttamente.");
        assertEquals(CharacterType.BUILDER, testBuilder.getCharacterType(), "Il CharacterType non è stato impostato automaticamente su BUILDER.");
    }
}