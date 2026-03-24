package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CharacterCardTest {

    private CharacterCard testCharacterCard;

    @BeforeEach
    void setUp() {
        testCharacterCard = new CharacterCard(501, Age.Era_I, 3, CharacterType.HUNTER) {};
    }

    @Test
    void constructorShouldInitializeFieldsAndPassValuesToSuper() {
        assertEquals(3, testCharacterCard.getTag(), "Il tag (numero di giocatori) non è stato inizializzato correttamente.");
        assertEquals(CharacterType.HUNTER, testCharacterCard.getCharacterType(), "Il CharacterType non è stato inizializzato correttamente.");

        assertEquals(501, testCharacterCard.getID(), "L'ID ereditato non è corretto.");
        assertEquals(Age.Era_I, testCharacterCard.getAge(), "L'Age ereditata non è corretta.");
    }
}