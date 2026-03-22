package it.polimi.ingsw.model.cards.Buildings;

import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.BuildingEachTurnType;
import it.polimi.ingsw.model.player.Player;

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
    public void applyEachTurnEffect(Player player) {

        // Qui andrà la logica tipo: player.addFood(1) se l'edificio produce cibo

        //HO PENSATO CHE INVECE DI USARE QUESTO METODO, PER QUESTI TIPI DI BUILDING CONVIENE USARE ATTRIBUTI E METODI DIRETTAMENTE IN PLAYER
        //USANDO GETTYPE() e agendo di conseguenza al caso specifico (nell'apposito spazio)
    }

    public  BuildingEachTurnType getBType() {
        return bType;
    }
}