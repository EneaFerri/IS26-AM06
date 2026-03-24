package it.polimi.ingsw.model.player;

import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.cards.Characters.Artist;
import it.polimi.ingsw.model.cards.Characters.Collector;
import it.polimi.ingsw.model.cards.Characters.Hunter;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @Test
    void addFood_increasesFood() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));

        player.addFood(5);

        assertEquals(5, player.getFood());
    }

    @Test
    void removeFood_decreasesFood() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));
        player.addFood(5);

        player.removeFood(2);

        assertEquals(3, player.getFood());
    }

    @Test
    void addPrestige_increasesPrestige() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));

        player.addPrestige(4);

        assertEquals(4, player.getPrestige());
    }

    @Test
    void addCharacterCard_addsCardToPlayer() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));

        Artist card = new Artist(1, Age.Era_I, 2);

        player.addCharacterCard(card);

        assertEquals(1, player.getCharacterCards().size());
        assertTrue(player.getCharacterCards().contains(card));
    }

    @Test
    void collectorsGiveCorrectFoodDiscount() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));

        Collector c1 = new Collector(1, Age.Era_I, 2);
        Collector c2 = new Collector(2, Age.Era_I, 2);

        player.addCharacterCard(c1);
        player.addCharacterCard(c2);

        assertEquals(6, player.getCollectorsFoodDiscount());
    }

    @Test
    void hunterWithNuggetGivesFoodForEachOwnedHunter() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));

        // primo hunter con icona
        player.addCharacterCard(new Hunter(1, Age.Era_I, 2, 1));

        // possiede 1 hunter → prende 1 cibo
        assertEquals(1, player.getFood());

        // secondo hunter con icona
        player.addCharacterCard(new Hunter(2, Age.Era_I, 2, 1));

        // ora possiede 2 hunter → il secondo deve far prendere 2 cibi
        // totale: 1 + 2 = 3
        assertEquals(3, player.getFood());
    }

    @Test
    void hunterWithoutNuggetGivesNoImmediateFood() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));

        player.addCharacterCard(new Hunter(1, Age.Era_I, 2, 0));

        assertEquals(0, player.getFood());
    }
}