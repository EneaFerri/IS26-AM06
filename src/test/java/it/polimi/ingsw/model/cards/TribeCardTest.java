package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TribeCardTest {

    private TribeCard testTribeCard;

    @BeforeEach
    void setUp() {
        testTribeCard = new TribeCard(301, Age.Era_I) {
            @Override
            public void pick(Player player, Game game) {
                // nessuna logica necessaria per questo test
            }
        };
    }

    @Test
    void constructorShouldPassValuesToSuperClass() {

        assertEquals(301, testTribeCard.getID(), "L'ID della carta non è stato passato/inizializzato correttamente.");
        assertEquals(Age.Era_I, testTribeCard.getAge(), "L'Age della carta non è stata passata/inizializzata correttamente.");
    }
}