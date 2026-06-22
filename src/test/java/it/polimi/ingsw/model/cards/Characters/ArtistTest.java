package it.polimi.ingsw.model.cards.Characters;

import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests that Artist correctly sets CharacterType.ARTIST and passes ID, age, and tag to the parent. */
class ArtistTest {

    private Artist testArtist;

    @BeforeEach
    void setUp() {
        testArtist = new Artist(601, Age.Era_I, 3);
    }

    @Test
    void constructorShouldInitializeFieldsAndPassValuesToSuper() {
        assertEquals(601, testArtist.getID(), "L'ID non è stato inizializzato correttamente.");
        assertEquals(Age.Era_I, testArtist.getAge(), "L'Age non è stata inizializzata correttamente.");
        assertEquals(3, testArtist.getTag(), "Il tag non è stato inizializzato correttamente.");
        assertEquals(CharacterType.ARTIST, testArtist.getCharacterType(), "Il CharacterType non è stato impostato automaticamente su ARTIST.");
    }
}