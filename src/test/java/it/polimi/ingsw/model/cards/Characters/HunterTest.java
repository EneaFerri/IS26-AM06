package it.polimi.ingsw.model.cards.Characters;

import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests that Hunter correctly stores the nugget count and sets CharacterType.HUNTER. */
class HunterTest {

    private Hunter testHunter;

    @BeforeEach
    void setUp() {
        testHunter = new Hunter(603, Age.Era_I, 3, 2);
    }

    @Test
    void constructorShouldInitializeFieldsAndPassValuesToSuper() {
        assertEquals(2, testHunter.getNuggets(), "Il numero di pepite non è stato inizializzato correttamente.");

        assertEquals(603, testHunter.getID(), "L'ID non è stato inizializzato correttamente.");
        assertEquals(Age.Era_I, testHunter.getAge(), "L'Age non è stata inizializzata correttamente.");
        assertEquals(3, testHunter.getTag(), "Il tag non è stato inizializzato correttamente.");
        assertEquals(CharacterType.HUNTER, testHunter.getCharacterType(), "Il CharacterType non è stato impostato automaticamente su HUNTER.");
    }
}