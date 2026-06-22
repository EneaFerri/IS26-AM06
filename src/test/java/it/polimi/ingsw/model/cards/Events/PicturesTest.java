package it.polimi.ingsw.model.cards.Events;

import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.cards.Characters.Artist;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.EventType;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests Pictures.resolve:
 * - Players meeting the minimum artist count receive the prestige bonus.
 * - Players below the threshold receive the prestige malus.
 * - Each player is evaluated independently.
 * - Building applyEventEffect is triggered for each player.
 */
class PicturesTest {

    @Test
    void playerWithEnoughArtistsGetsPrestigeBonus() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));

        player.addCharacterCard(new Artist(1, Age.Era_I, 2));
        player.addCharacterCard(new Artist(2, Age.Era_I, 2));

        Pictures pictures = new Pictures(1, Age.Era_I, 2, 3, 4);

        pictures.resolve(List.of(player));

        assertEquals(8, player.getPrestige());
    }

    @Test
    void playerWithoutEnoughArtistsGetsPrestigeMalus() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));

        player.addCharacterCard(new Artist(1, Age.Era_I, 2));

        Pictures pictures = new Pictures(1, Age.Era_I, 2, 3, 2);

        pictures.resolve(List.of(player));

        assertEquals(- 3, player.getPrestige());
    }

    @Test
    void eachPlayerIsEvaluatedIndependently() {
        Player p1 = new Player("Mario", new Totem(TotemColor.RED));
        Player p2 = new Player("Luigi", new Totem(TotemColor.BLUE));

        p1.addCharacterCard(new Artist(1, Age.Era_I, 2));
        p1.addCharacterCard(new Artist(2, Age.Era_I, 2));

        p2.addCharacterCard(new Artist(3, Age.Era_I, 2));

        Pictures pictures = new Pictures(1, Age.Era_I, 2, 3, 2);

        pictures.resolve(List.of(p1, p2));

        assertEquals(4, p1.getPrestige());
        assertEquals(- 3, p2.getPrestige());
    }

    @Test
    void gettersShouldReturnCorrectValues() {
        Pictures pictures = new Pictures(201, Age.Era_I, 2, 3, 4);
        assertEquals(2, pictures.getMinimumArtists());
        assertEquals(3, pictures.getPrestigeMalus());
        assertEquals(4, pictures.getPrestigeBonus());
    }

    @Test
    void resolveShouldTriggerBuildingEffects() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));
        final boolean[] effectTriggered = {false};

        BuildingCard fakeBuilding = new BuildingCard(100, Age.Era_I, 0, 0) {
            @Override
            public void applyEventEffect(EventType eventType, Player p) {
                if(eventType == EventType.PICTURES) {
                    effectTriggered[0] = true;
                }
            }
        };

        player.addBuildingCard(fakeBuilding);

        Pictures pictures = new Pictures(201, Age.Era_I, 2, 3, 4);
        pictures.resolve(List.of(player));

        assertTrue(effectTriggered[0], "L'evento PICTURES dovrebbe innescare applyEventEffect sugli edifici del giocatore.");
    }
}