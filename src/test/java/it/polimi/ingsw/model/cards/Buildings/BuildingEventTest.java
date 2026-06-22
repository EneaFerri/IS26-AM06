package it.polimi.ingsw.model.cards.Buildings;

import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.enums.EventType;
import it.polimi.ingsw.model.player.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests BuildingEvent.applyEventEffect:
 * - No-op when the event type does not match the card's trigger.
 * - SUSTENANCE trigger: adds a food discount equal to the count of matching characters.
 * - PICTURES trigger: adds food equal to the player's artist count.
 * - HUNT trigger: adds food equal to the player's hunter count.
 */
class BuildingEventTest {

    @Test
    void applyEventEffectShouldDoNothingIfEventDoesNotMatch() {
        BuildingEvent card = new BuildingEvent(100, Age.Era_I, 2, 3, EventType.HUNT, CharacterType.HUNTER);

        // Passiamo un evento diverso da HUNT (es. SUSTENANCE)
        assertDoesNotThrow(() -> card.applyEventEffect(EventType.SUSTENANCE, new Player("Test", null)),
                "Se l'evento non combacia, il metodo dovrebbe fermarsi subito senza errori.");
    }

    @Test
    void applyEventEffectSustenanceShouldCalculateDiscountProperly() {
        BuildingEvent card = new BuildingEvent(101, Age.Era_I, 2, 3, EventType.SUSTENANCE, CharacterType.ARTIST);
        final int[] discountAdded = {0};

        Player fakePlayer = new Player("Test", null) {
            @Override
            public List<CharacterCard> getCharacterCards() {
                // Simuliamo che il giocatore abbia 2 ARTISTI e 1 HUNTER
                CharacterCard artist1 = new CharacterCard(1, Age.Era_I, 2, CharacterType.ARTIST) {};
                CharacterCard artist2 = new CharacterCard(2, Age.Era_I, 2, CharacterType.ARTIST) {};
                CharacterCard hunter = new CharacterCard(3, Age.Era_I, 2, CharacterType.HUNTER) {};
                return List.of(artist1, artist2, hunter);
            }

            @Override
            public void addBuildingFoodDiscount(int moreDiscount) {
                discountAdded[0] = moreDiscount;
            }
        };

        card.applyEventEffect(EventType.SUSTENANCE, fakePlayer);

        // Visto che abbiamo impostato la carta per reagire agli ARTISTI, e il fakePlayer ne ha 2, ci aspettiamo 2 di sconto
        assertEquals(2, discountAdded[0], "Dovrebbe aggiungere uno sconto pari al numero di personaggi del tipo richiesto.");
    }

    @Test
    void applyEventEffectPicturesShouldAddFoodBasedOnArtists() {
        BuildingEvent card = new BuildingEvent(102, Age.Era_I, 2, 3, EventType.PICTURES, null);
        final int[] foodAdded = {0};

        Player fakePlayer = new Player("Test", null) {
            @Override
            public int getNumArtists() { return 4; } // Simuliamo che il giocatore abbia 4 artisti

            @Override
            public void addFood(int amount) { foodAdded[0] = amount; }
        };

        card.applyEventEffect(EventType.PICTURES, fakePlayer);
        assertEquals(4, foodAdded[0], "Dovrebbe aggiungere tanto cibo quanti sono gli artisti.");
    }

    @Test
    void applyEventEffectHuntShouldAddFoodBasedOnHunters() {
        BuildingEvent card = new BuildingEvent(103, Age.Era_I, 2, 3, EventType.HUNT, null);
        final int[] foodAdded = {0};

        Player fakePlayer = new Player("Test", null) {
            @Override
            public int getNumHunters() { return 3; } // Simuliamo che il giocatore abbia 3 cacciatori

            @Override
            public void addFood(int amount) { foodAdded[0] = amount; }
        };

        card.applyEventEffect(EventType.HUNT, fakePlayer);
        assertEquals(3, foodAdded[0], "Dovrebbe aggiungere tanto cibo quanti sono i cacciatori.");
    }
}