package it.polimi.ingsw.model.cards.Characters;

import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.enums.InventionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventorTest {

    private Inventor testInventor;

    @BeforeEach
    void setUp() {
        testInventor = new Inventor(604, Age.Era_I, 3, InventionType.BREAD);
    }

    @Test
    void constructorShouldInitializeFieldsAndPassValuesToSuper() {
        assertEquals(InventionType.BREAD, testInventor.getInvention(), "L'invenzione non è stata inizializzata correttamente.");

        assertEquals(604, testInventor.getID(), "L'ID non è stato inizializzato correttamente.");
        assertEquals(Age.Era_I, testInventor.getAge(), "L'Age non è stata inizializzata correttamente.");
        assertEquals(3, testInventor.getTag(), "Il tag non è stato inizializzato correttamente.");
        assertEquals(CharacterType.INVENTOR, testInventor.getCharacterType(), "Il CharacterType non è stato impostato automaticamente su INVENTOR.");
    }
}