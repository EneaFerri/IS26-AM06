package it.polimi.ingsw.model.cards.Buildings;

import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.BuildingEachTurnType;
import it.polimi.ingsw.model.player.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests BuildingEachTurn.onAddedToPlayer for all seven BuildingEachTurnType values,
 * verifying that each type activates the correct player flag/ability.
 */
class BuildingEachTurnTest {

    @Test
    void onAddedToPlayerShouldEnableCorrectPlayerFlag() {
        // Creiamo un array per registrare quale metodo del Player viene effettivamente chiamato
        final boolean[] flagsTriggered = new boolean[7];

        Player fakePlayer = new Player("Test", null) {
            @Override public void enableDoublePointForBuilder() { flagsTriggered[0] = true; }
            @Override public void enableDoublePointForRituals() { flagsTriggered[1] = true; }
            @Override public void enableExtraThreeStars()       { flagsTriggered[2] = true; }
            @Override public void enableNoMalusForRituals()     { flagsTriggered[3] = true; }
            @Override public void enableSetBonus()              { flagsTriggered[4] = true; }
            @Override public void enableInventorBonus()         { flagsTriggered[5] = true; }
            @Override public void enableExtraFoodOnTurnOrder()  { flagsTriggered[6] = true; }
        };

        // Testiamo tutti e 7 i tipi in sequenza usando lo stesso fakePlayer

        new BuildingEachTurn(1, Age.Era_I, 1, 1, BuildingEachTurnType.BUILDER_DOUBLEPOINTS).onAddedToPlayer(fakePlayer);
        assertTrue(flagsTriggered[0], "Non ha attivato la flag per BUILDER_DOUBLEPOINTS");

        new BuildingEachTurn(2, Age.Era_I, 1, 1, BuildingEachTurnType.RITUAL_DOUBLEPOINTS).onAddedToPlayer(fakePlayer);
        assertTrue(flagsTriggered[1], "Non ha attivato la flag per RITUAL_DOUBLEPOINTS");

        new BuildingEachTurn(3, Age.Era_I, 1, 1, BuildingEachTurnType.RITUAL_THREEEXTRASTARS).onAddedToPlayer(fakePlayer);
        assertTrue(flagsTriggered[2], "Non ha attivato la flag per RITUAL_THREEEXTRASTARS");

        new BuildingEachTurn(4, Age.Era_I, 1, 1, BuildingEachTurnType.RITUAL_NOMALUS).onAddedToPlayer(fakePlayer);
        assertTrue(flagsTriggered[3], "Non ha attivato la flag per RITUAL_NOMALUS");

        new BuildingEachTurn(5, Age.Era_I, 1, 1, BuildingEachTurnType.EXTRAFOOD_SET).onAddedToPlayer(fakePlayer);
        assertTrue(flagsTriggered[4], "Non ha attivato la flag per EXTRAFOOD_SET");

        new BuildingEachTurn(6, Age.Era_I, 1, 1, BuildingEachTurnType.EXTRAFOOD_INVENTORS).onAddedToPlayer(fakePlayer);
        assertTrue(flagsTriggered[5], "Non ha attivato la flag per EXTRAFOOD_INVENTORS");

        new BuildingEachTurn(7, Age.Era_I, 1, 1, BuildingEachTurnType.EXTRAFOOD_TURNORDER).onAddedToPlayer(fakePlayer);
        assertTrue(flagsTriggered[6], "Non ha attivato la flag per EXTRAFOOD_TURNORDER");
    }
}