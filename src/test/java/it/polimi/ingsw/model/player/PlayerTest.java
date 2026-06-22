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

/**
 * Tests all non-getter Player methods:
 * - Food/prestige arithmetic: addFood, removeFood, addPrestige, removePrestige.
 * - Card management: addCharacterCard (with side-effects per card type), addBuildingCard.
 * - Scoring helpers: getCollectorsFoodDiscount, foodDiscountToBuyBuildings,
 *   addBuildingFoodDiscount/resetBuildingFoodDiscount, getTotalFoodDiscount,
 *   getTotalPointsPreEffect, getPointsFromEndEffect, getTotalPoints.
 * - Special flags: enableDoublePointForBuilder, countSet.
 */
class PlayerTest {

    @Test
    void addFood_increasesFood() {
        Player player = new Player("Alice", new Totem(TotemColor.RED));
        player.addFood(5);
        assertEquals(5, player.getFood());
    }

    @Test
    void removeFood_decreasesFood() {
        Player player = new Player("Alice", new Totem(TotemColor.RED));
        player.addFood(5);
        player.removeFood(2);
        assertEquals(3, player.getFood());
    }

    @Test
    void addPrestige_increasesPrestige() {
        Player player = new Player("Alice", new Totem(TotemColor.RED));
        player.addPrestige(4);
        assertEquals(4, player.getPrestige());
    }

    @Test
    void removePrestige_decreasesPrestige() {
        Player player = new Player("Alice", new Totem(TotemColor.RED));
        player.addPrestige(10);
        player.removePrestige(3);
        assertEquals(7, player.getPrestige());
    }

    @Test
    void addCharacterCard_addsCardToPlayer() {
        Player player = new Player("Alice", new Totem(TotemColor.RED));
        Artist card = new Artist(1, Age.Era_I, 2);
        player.addCharacterCard(card);
        assertEquals(1, player.getCharacterCards().size());
        assertTrue(player.getCharacterCards().contains(card));
    }

    @Test
    void collectorsGiveCorrectFoodDiscount() {
        Player player = new Player("Alice", new Totem(TotemColor.RED));
        player.addCharacterCard(new Collector(1, Age.Era_I, 2));
        player.addCharacterCard(new Collector(2, Age.Era_I, 2));
        assertEquals(6, player.getCollectorsFoodDiscount());
    }

    @Test
    void hunterWithNuggetGivesFoodForEachOwnedHunter() {
        Player player = new Player("Alice", new Totem(TotemColor.RED));
        player.addCharacterCard(new Hunter(1, Age.Era_I, 2, 1));
        assertEquals(1, player.getFood());
        player.addCharacterCard(new Hunter(2, Age.Era_I, 2, 1));
        assertEquals(3, player.getFood());
    }

    @Test
    void hunterWithoutNuggetGivesNoImmediateFood() {
        Player player = new Player("Alice", new Totem(TotemColor.RED));
        player.addCharacterCard(new Hunter(1, Age.Era_I, 2, 0));
        assertEquals(0, player.getFood());
    }

    @Test
    void collectorsGiveOnlyFoodDiscountAndNoImmediateFood() {
        Player player = new Player("Alice", new Totem(TotemColor.RED));
        player.addCharacterCard(new Collector(1, Age.Era_I, 2));
        player.addCharacterCard(new Collector(2, Age.Era_I, 2));
        assertEquals(6, player.getCollectorsFoodDiscount());
        assertEquals(0, player.getFood());
    }

    @Test
    void inventorsCountDistinctInventionsForFinalScoring() {
        Player player = new Player("Alice", new Totem(TotemColor.RED));
        player.addCharacterCard(new Inventor(1, Age.Era_I, 2, InventionType.CUP));
        player.addCharacterCard(new Inventor(2, Age.Era_I, 2, InventionType.ARROW));
        player.addCharacterCard(new Inventor(3, Age.Era_I, 2, InventionType.CUP));
        assertEquals(3, player.getNumInventors());
        assertEquals(2, player.getNumInventions());
    }

    @Test
    void shamanStarsAreSummedCorrectly() {
        Player player = new Player("Alice", new Totem(TotemColor.RED));
        player.addCharacterCard(new Shaman(1, Age.Era_I, 2, 1));
        player.addCharacterCard(new Shaman(2, Age.Era_I, 2, 3));
        assertEquals(4, player.getStarsFromShamans());
    }

    @Test
    void totalPointsPreEffectIncludesArtistsBuildersAndInventors() {
        Player player = new Player("Alice", new Totem(TotemColor.RED));
        player.addCharacterCard(new Artist(1, Age.Era_I, 2));
        player.addCharacterCard(new Artist(2, Age.Era_I, 2));
        player.addCharacterCard(new Builder(3, Age.Era_I, 2, 3, 1));
        player.addCharacterCard(new Inventor(4, Age.Era_I, 2, InventionType.BREAD));
        player.addCharacterCard(new Inventor(5, Age.Era_I, 2, InventionType.LEATHER));
        // 2 artists = 1 couple = 10 pts; builder = 3 pts; 2 inventors * 2 inventions = 4 pts → total 17
        assertEquals(17, player.getTotalPointsPreEffect());
    }

    @Test
    void builderDoublePointsAffectsPrestigeContribution() {
        Player player = new Player("Alice", new Totem(TotemColor.RED));
        player.enableDoublePointForBuilder();
        player.addCharacterCard(new Builder(1, Age.Era_I, 2, 3, 1));
        assertEquals(6, player.getTotalPointsPreEffect());
    }

    @Test
    void endBuildingPointsAreSummedCorrectly() {
        Player player = new Player("Alice", new Totem(TotemColor.RED));
        player.addCharacterCard(new Hunter(1, Age.Era_I, 2, 0));
        player.addCharacterCard(new Hunter(2, Age.Era_I, 2, 0));
        player.addBuildingCard(new BuildingEnd(10, Age.Era_I, 2, 1, CharacterType.HUNTER, 5));
        assertEquals(10, player.getPointsFromEndEffect());
    }

    @Test
    void totalPointsIncludesPreEffectAndEndEffectPoints() {
        Player player = new Player("Alice", new Totem(TotemColor.RED));
        player.addCharacterCard(new Artist(1, Age.Era_I, 2));
        player.addCharacterCard(new Artist(2, Age.Era_I, 2));
        player.addCharacterCard(new Hunter(3, Age.Era_I, 2, 0));
        player.addBuildingCard(new BuildingEnd(10, Age.Era_I, 2, 1, CharacterType.HUNTER, 5));
        // preEffect = 10 (artists) + 1 (building base); endEffect = 5 (1 hunter * 5)
        assertEquals(16, player.getTotalPoints());
    }

    @Test
    void foodDiscountToBuyBuildings_sumsBuilderDiscounts() {
        Player player = new Player("Alice", new Totem(TotemColor.RED));
        player.addCharacterCard(new Builder(1, Age.Era_I, 2, 3, 2));
        player.addCharacterCard(new Builder(2, Age.Era_I, 2, 3, 3));
        player.addCharacterCard(new Artist(3, Age.Era_I, 2));
        assertEquals(5, player.foodDiscountToBuyBuildings());
    }

    @Test
    void addBuildingFoodDiscount_accumulatesAndResetClearsIt() {
        Player player = new Player("Alice", new Totem(TotemColor.RED));
        player.addBuildingFoodDiscount(3);
        player.addBuildingFoodDiscount(2);
        assertEquals(5, player.getBuildingFoodDiscount());
        player.resetBuildingFoodDiscount();
        assertEquals(0, player.getBuildingFoodDiscount());
    }

    @Test
    void getTotalFoodDiscount_sumsCollectorsAndBuildingDiscounts() {
        Player player = new Player("Alice", new Totem(TotemColor.RED));
        player.addCharacterCard(new Collector(1, Age.Era_I, 2));
        player.addBuildingFoodDiscount(4);
        // 3 (from 1 collector) + 4 (from building) = 7
        assertEquals(7, player.getTotalFoodDiscount());
    }

    @Test
    void countSet_returnsNumberOfCompleteSets() {
        Player player = new Player("Alice", new Totem(TotemColor.RED));
        player.addCharacterCard(new Artist(1, Age.Era_I, 2));
        player.addCharacterCard(new Builder(2, Age.Era_I, 2, 3, 1));
        player.addCharacterCard(new Collector(3, Age.Era_I, 2));
        player.addCharacterCard(new Hunter(4, Age.Era_I, 2, 0));
        player.addCharacterCard(new Inventor(5, Age.Era_I, 2, InventionType.ARROW));
        player.addCharacterCard(new Shaman(6, Age.Era_I, 2, 1));
        player.addCharacterCard(new Artist(7, Age.Era_I, 2));
        player.addCharacterCard(new Builder(8, Age.Era_I, 2, 3, 1));
        player.addCharacterCard(new Collector(9, Age.Era_I, 2));
        player.addCharacterCard(new Hunter(10, Age.Era_I, 2, 0));
        player.addCharacterCard(new Inventor(11, Age.Era_I, 2, InventionType.BREAD));
        player.addCharacterCard(new Shaman(12, Age.Era_I, 2, 2));
        assertEquals(2, player.countSet());
    }

    @Test
    void countSet_doesNotCountIncompleteSet() {
        Player player = new Player("Alice", new Totem(TotemColor.RED));
        player.addCharacterCard(new Artist(1, Age.Era_I, 2));
        player.addCharacterCard(new Builder(2, Age.Era_I, 2, 3, 1));
        player.addCharacterCard(new Collector(3, Age.Era_I, 2));
        player.addCharacterCard(new Hunter(4, Age.Era_I, 2, 0));
        player.addCharacterCard(new Inventor(5, Age.Era_I, 2, InventionType.ARROW));
        player.addCharacterCard(new Shaman(6, Age.Era_I, 2, 1));
        // second cycle: missing Shaman
        player.addCharacterCard(new Artist(7, Age.Era_I, 2));
        player.addCharacterCard(new Builder(8, Age.Era_I, 2, 3, 1));
        player.addCharacterCard(new Collector(9, Age.Era_I, 2));
        player.addCharacterCard(new Hunter(10, Age.Era_I, 2, 0));
        player.addCharacterCard(new Inventor(11, Age.Era_I, 2, InventionType.BREAD));
        assertEquals(1, player.countSet());
    }
}
