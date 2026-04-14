package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardTest {

    private Card testCard;

    @BeforeEach
    void setUp() {
        testCard = new Card(101, Age.Era_I) {
            @Override
            public void pick(Player player, Game game) {
                // non serve fare nulla
            }
        };
    }

    @Test
    void markAsDrawedShouldSetDrawedToTrue() {

        testCard.markAsDrawed();
        assertTrue(testCard.isDrawed(), "Il metodo markAsDrawed dovrebbe impostare lo stato 'drawed' a true.");
    }

    @Test
    void isBuildingShouldReturnFalseByDefault() {
        assertFalse(testCard.isBuilding(), "Il metodo isBuilding dovrebbe ritornare false di default nella classe base Card.");
    }

    @Test
    void isTribeShouldReturnFalseByDefault() {
        assertFalse(testCard.isTribe(), "Il metodo isTribe dovrebbe ritornare false di default nella classe base Card.");
    }
}