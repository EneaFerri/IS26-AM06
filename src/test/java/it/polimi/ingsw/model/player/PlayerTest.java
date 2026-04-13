package it.polimi.ingsw.model.player;

import it.polimi.ingsw.model.cards.Buildings.BuildingEnd;
import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.cards.Characters.*;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.enums.InventionType;
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

    @Test
    void collectorsGiveOnlyFoodDiscountAndNoImmediateFood() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));

        player.addCharacterCard(new Collector(1, Age.Era_I, 2));
        player.addCharacterCard(new Collector(2, Age.Era_I, 2));

        // Regola: ogni Raccoglitore dà -3 cibo sul Sostentamento
        assertEquals(6, player.getCollectorsFoodDiscount());

        // Regola: i Raccoglitori non danno cibo direttamente
        assertEquals(0, player.getFood());
    }

    @Test
    void inventorsCountDistinctInventionsForFinalScoring() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));

        player.addCharacterCard(new Inventor(1, Age.Era_I, 2, InventionType.CUP));
        player.addCharacterCard(new Inventor(2, Age.Era_I, 2, InventionType.ARROW));
        player.addCharacterCard(new Inventor(3, Age.Era_I, 2, InventionType.CUP));

        assertEquals(3, player.getNumInventors());
        assertEquals(2, player.getNumInventions());
    }

    @Test
    void shamanStarsAreSummedCorrectly() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));

        player.addCharacterCard(new Shaman(1, Age.Era_I, 2, 1));
        player.addCharacterCard(new Shaman(2, Age.Era_I, 2, 3));

        assertEquals(4, player.getStarsFromShamans());
    }

    @Test
    void totalPointsPreEffectIncludesArtistsBuildersAndInventors() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));

        // 2 Artist -> 1 coppia -> 10 punti
        player.addCharacterCard(new Artist(1, Age.Era_I, 2));
        player.addCharacterCard(new Artist(2, Age.Era_I, 2));

        // Builder: 3 punti
        player.addCharacterCard(new Builder(3, Age.Era_I, 2, 3, 1));

        // 2 Inventori con 2 invenzioni diverse -> 2 * 2 = 4 punti
        player.addCharacterCard(new Inventor(4, Age.Era_I, 2, InventionType.BREAD));
        player.addCharacterCard(new Inventor(5, Age.Era_I, 2, InventionType.LEATHER));

        // totale atteso: 10 + 3 + 4 = 17
        assertEquals(17, player.getTotalPointsPreEffect());
    }

    @Test
    void builderDoublePointsAffectsPrestigeContribution() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));

        player.enableDoublePointForBuilder();
        player.addCharacterCard(new Builder(1, Age.Era_I, 2, 3, 1));

        // Builder da 3, raddoppiato -> 6
        assertEquals(6, player.getTotalPointsPreEffect());
    }

    @Test
    void endBuildingPointsAreSummedCorrectly() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));

        // 2 Hunter
        player.addCharacterCard(new Hunter(1, Age.Era_I, 2, 0));
        player.addCharacterCard(new Hunter(2, Age.Era_I, 2, 0));

        // edificio finale: 5 punti per ogni Hunter
        player.addBuildingCard(new BuildingEnd(10, Age.Era_I, 2, 1, CharacterType.HUNTER, 5));

        assertEquals(10, player.getPointsFromEndEffect());
    }

    /*
    @Test
    void fixedEndBuildingPointsAreReturnedCorrectly() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));

        // edificio finale fisso da 25 punti
        player.addBuildingCard(new BuildingEnd(11, Age.Era_I, 2, 1, CharacterType.NONE, 25));

        assertEquals(25, player.getPointsFromEndEffect());
    }
    */

    @Test
    void totalPointsIncludesPreEffectAndEndEffectPoints() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));

        // 2 Artist -> 10 punti
        player.addCharacterCard(new Artist(1, Age.Era_I, 2));
        player.addCharacterCard(new Artist(2, Age.Era_I, 2));

        // 1 Hunter
        player.addCharacterCard(new Hunter(3, Age.Era_I, 2, 0));

        // edificio finale: 5 punti per ogni Hunter
        player.addBuildingCard(new BuildingEnd(10, Age.Era_I, 2, 1, CharacterType.HUNTER, 5));

        // preEffect = 10
        // endEffect = 5
        //punto base edificio +1
        // totale = 15
        assertEquals(16, player.getTotalPoints());
    }
}