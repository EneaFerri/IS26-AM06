package it.polimi.ingsw.model.cards.Buildings;

import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.player.Player;

public class BuildingEachTurn extends BuildingCard {

    public BuildingEachTurn(int cardID, Age cardAge, int foodCost, int prestigePoint) {
        super(cardID, cardAge, foodCost, prestigePoint);
    }

    // TODO: da capire bene con il prof in quale momento esatto del turno si attiva
    // (all'inizio quando si pescano le carte o alla fine prima di passare il turno?)
    public void applyEachTurnEffect(Player player) {

        // Qui andrà la logica tipo: player.addFood(1) se l'edificio produce cibo

    }
}