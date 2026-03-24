package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.enums.Age;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardTest {

    private Card testCard;

    @BeforeEach
    void setUp() {

        testCard = new Card(101, Age.Era_I) {};
    }

    @Test
    void markAsDrawedShouldSetDrawedToTrue() {

        testCard.markAsDrawed();
        assertTrue(testCard.isDrawed(), "Il metodo markAsDrawed dovrebbe impostare lo stato 'drawed' a true.");
    }
}