package it.polimi.ingsw.model.cards.Buildings;

import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.player.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests BuildingEnd.getEndEffectPoints:
 * - SET_OF_CHAR type: multiplies the player's complete-set count by the per-set bonus.
 * - Specific CharacterType: counts matching character cards owned by the player.
 */
class BuildingEndTest {

    @Test
    void gettersShouldReturnCorrectValues() {
        BuildingEnd card = new BuildingEnd(110, Age.Era_I, 5, 2, CharacterType.ARTIST, 3);
        assertEquals(CharacterType.ARTIST, card.getCharacterToConsider());
        assertEquals(3, card.getPrestigeEndEffect());
    }

    @Test
    void getEndEffectPointsShouldMultiplySetsCorrectly() {
        // Carta che dà 5 punti per ogni set
        BuildingEnd card = new BuildingEnd(111, Age.Era_I, 5, 2, CharacterType.SET_OF_CHAR, 5);

        Player fakePlayer = new Player("Test", null) {
            @Override
            public int countSet() {
                return 3; // Simuliamo che il giocatore abbia 3 set completi
            }
        };

        // 3 set * 5 punti l'uno = 15
        assertEquals(15, card.getEndEffectPoints(fakePlayer), "Dovrebbe moltiplicare il numero di set per i punti previsti dalla carta.");
    }

    @Test
    void getEndEffectPointsShouldCountSpecificCharacters() {
        // Carta che dà 4 punti per ogni BUILDER
        BuildingEnd card = new BuildingEnd(112, Age.Era_I, 5, 2, CharacterType.BUILDER, 4);

        Player fakePlayer = new Player("Test", null) {
            @Override
            public List<CharacterCard> getCharacterCards() {
                // Simuliamo che il giocatore abbia 2 BUILDER e 1 SHAMAN
                CharacterCard builder1 = new CharacterCard(1, Age.Era_I, 2, CharacterType.BUILDER) {};
                CharacterCard builder2 = new CharacterCard(2, Age.Era_I, 2, CharacterType.BUILDER) {};
                CharacterCard shaman = new CharacterCard(3, Age.Era_I, 2, CharacterType.SHAMAN) {};
                return List.of(builder1, builder2, shaman);
            }
        };

        // 2 builder * 4 punti l'uno = 8
        assertEquals(8, card.getEndEffectPoints(fakePlayer), "Dovrebbe contare quanti personaggi del tipo richiesto possiede il giocatore e moltiplicarli per l'effetto.");
    }
}