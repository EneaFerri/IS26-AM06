package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests CharacterCard non-getter methods:
 * - pick: delegates to Game.pickCharacterCard.
 * - isAvailableForPlayers: returns true only when the player count meets the card tag threshold.
 * - Default scoring overrides (getShamanStars, getPrestigeContribution, etc.) return zero/false.
 */
class CharacterCardTest {

    private CharacterCard testCharacterCard;

    @BeforeEach
    void setUp() {
        // Uso l'ID 36 reale dal characters.json (Era_I, Tag 2, HUNTER)
        testCharacterCard = new CharacterCard(36, Age.Era_I, 2, CharacterType.HUNTER) {};
    }

    @Test
    void constructorShouldInitializeFieldsAndPassValuesToSuper() {
        assertEquals(2, testCharacterCard.getTag(), "Il tag non è stato inizializzato correttamente.");
        assertEquals(CharacterType.HUNTER, testCharacterCard.getCharacterType(), "Il CharacterType non è stato inizializzato correttamente.");
        assertEquals(36, testCharacterCard.getID(), "L'ID ereditato non è corretto.");
        assertEquals(Age.Era_I, testCharacterCard.getAge(), "L'Age ereditata non è corretta.");
    }

    @Test
    void defaultMethodsShouldReturnCorrectValues() {
        assertDoesNotThrow(() -> testCharacterCard.onAddedToPlayer(null));

        assertEquals(0, testCharacterCard.getShamanStars());
        assertEquals(0, testCharacterCard.getPrestigeContribution(null));
        assertFalse(testCharacterCard.hasInvention(null));
        assertEquals(0, testCharacterCard.getDiscountForBuildings());
        assertTrue(testCharacterCard.isCharacter());
    }

    @Test
    void isAvailableForPlayersShouldCompareTagCorrectly() {
        // Il tag è impostato a 2 nel setUp
        assertTrue(testCharacterCard.isAvailableForPlayers(2), "Dovrebbe essere disponibile per 2 giocatori");
        assertTrue(testCharacterCard.isAvailableForPlayers(3), "Dovrebbe essere disponibile per 3 giocatori");
        assertFalse(testCharacterCard.isAvailableForPlayers(1), "Non dovrebbe essere disponibile per 1 giocatore (tag 2 > 1)");
    }

    @Test
    void pickShouldCallGamePickCharacterCard() {

        final boolean[] methodCalled = {false};

        Game fakeGame = new Game(1) {
            @Override
            public void pickCharacterCard(Player player, CharacterCard card) {

                methodCalled[0] = true;
            }
        };

        Player fakePlayer = new Player("TestPlayer", null);

        testCharacterCard.pick(fakePlayer, fakeGame);

        assertTrue(methodCalled[0], "Il metodo pickCharacterCard del Game NON è stato chiamato dalla carta!");
    }
}