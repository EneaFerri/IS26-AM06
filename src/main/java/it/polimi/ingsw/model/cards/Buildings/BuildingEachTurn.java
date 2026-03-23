package it.polimi.ingsw.model.cards.Buildings;

import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.BuildingEachTurnType;

public class BuildingEachTurn extends BuildingCard {

    private BuildingEachTurnType bType;

    public BuildingEachTurn(int cardID, Age cardAge, int foodCost, int prestigePoint, BuildingEachTurnType bType) {
        super(cardID, cardAge, foodCost, prestigePoint);
        this.bType = bType;
    }

    // TODO: da capire bene con il prof in quale momento esatto del turno si attiva
    // (all'inizio quando si pescano le carte o alla fine prima di passare il turno?)

    //NO, in reatà si tratta di quelle bulding che dal momento della pescata, "attivano" degli effetti da controlare sempre
    //eachtTurn è simbolico e indica che valgono tutti i turni fino alla fine. poi in base al tipo di effetto si attiverà in un determianto momento

    public  BuildingEachTurnType getBType() {
        return bType;
    }
}