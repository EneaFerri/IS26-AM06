package it.polimi.ingsw.model.cards.Characters;

import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShamanTest {

    private Shaman testShaman;

    @BeforeEach
    void setUp() {
        testShaman = new Shaman(605, Age.Era_I, 3, 5);
    }

    @Test
    void constructorShouldInitializeFieldsAndPassValuesToSuper() {
        assertEquals(5, testShaman.getStars(), "Il numero di stelle non è stato inizializzato correttamente.");

        assertEquals(605, testShaman.getID(), "L'ID non è stato inizializzato correttamente.");
        assertEquals(Age.Era_I, testShaman.getAge(), "L'Age non è stata inizializzata correttamente.");
        assertEquals(3, testShaman.getTag(), "Il tag non è stato inizializzato correttamente.");
        assertEquals(CharacterType.SHAMAN, testShaman.getCharacterType(), "Il CharacterType non è stato impostato automaticamente su SHAMAN.");
    }
}