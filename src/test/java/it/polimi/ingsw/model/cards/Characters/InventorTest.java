package it.polimi.ingsw.model.cards.Characters;

import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.enums.InventionType;
import it.polimi.ingsw.model.player.Player;
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

    @Test
    void hasInventionShouldReturnTrueForMatchingInventionAndFalseOtherwise() {
        // testInventor ha InventionType.BREAD (impostato nel setUp)

        assertTrue(testInventor.hasInvention(InventionType.BREAD),
                "Dovrebbe ritornare true se l'invenzione passata corrisponde a quella della carta.");

        assertFalse(testInventor.hasInvention(InventionType.BOAT),
                "Dovrebbe ritornare false se l'invenzione passata è diversa da quella della carta.");
    }

    @Test
    void onAddedToPlayerShouldCallPlayerMethod() {
        // 1. Prepariamo il nostro solito flag per "spiare" l'invocazione
        final boolean[] methodCalled = {false};

        // 2. Creiamo un "Fake Player" sovrascrivendo SOLO il metodo che ci interessa
        // Usiamo null per il Totem visto che per questo test non ci serve
        Player fakePlayer = new Player("TestInventor", null) {
            @Override
            public void inventorsCountAndCheck(Inventor inventor) {
                methodCalled[0] = true;
            }
        };

        // 3. Eseguiamo il metodo da testare
        testInventor.onAddedToPlayer(fakePlayer);

        // 4. Verifichiamo che il metodo del giocatore sia stato effettivamente chiamato
        assertTrue(methodCalled[0], "Il metodo onAddedToPlayer DEVE chiamare inventorsCountAndCheck() sul giocatore!");
    }
}