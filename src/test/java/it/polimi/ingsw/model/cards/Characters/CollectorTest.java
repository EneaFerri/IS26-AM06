package it.polimi.ingsw.model.cards.Characters;

import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CollectorTest {

    private Collector testCollector;

    @BeforeEach
    void setUp() {
        testCollector = new Collector(606, Age.Era_I, 3);
    }

    @Test
    void constructorShouldInitializeFieldsAndPassValuesToSuper() {
        assertEquals(606, testCollector.getID(), "L'ID non è stato inizializzato correttamente.");
        assertEquals(Age.Era_I, testCollector.getAge(), "L'Age non è stata inizializzata correttamente.");
        assertEquals(3, testCollector.getTag(), "Il tag non è stato inizializzato correttamente.");
        assertEquals(CharacterType.COLLECTOR, testCollector.getCharacterType(), "Il CharacterType non è stato impostato automaticamente su COLLECTOR.");
    }
}