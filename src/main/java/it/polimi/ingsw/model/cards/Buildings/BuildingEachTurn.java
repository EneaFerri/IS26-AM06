package it.polimi.ingsw.model.cards.Buildings;

import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.BuildingEachTurnType;
import it.polimi.ingsw.model.player.Player;

public class BuildingEachTurn extends BuildingCard {

    private BuildingEachTurnType bType;

    public BuildingEachTurn(int cardID, Age cardAge, int foodCost, int prestigePoint, BuildingEachTurnType bType) {
        super(cardID, cardAge, foodCost, prestigePoint);
        this.bType = bType;
    }

    @Override
    public void onAddedToPlayer(Player player) {
        if (getBType() == BuildingEachTurnType.BUILDER_DOUBLEPOINTS) {
            player.enableDoublePointForBuilder();
        } else if (getBType() == BuildingEachTurnType.RITUAL_DOUBLEPOINTS) {
            player.enableDoublePointForRituals();
        } else if (getBType() == BuildingEachTurnType.RITUAL_THREEEXTRASTARS) {
            player.enableExtraThreeStars();
        } else if (getBType() == BuildingEachTurnType.RITUAL_NOMALUS) {
            player.enableNoMalusForRituals();
        } else if (getBType() == BuildingEachTurnType.EXTRAFOOD_SET) {
            player.enableSetBonus();
        } else if (getBType() == BuildingEachTurnType.EXTRAFOOD_INVENTORS) {
            player.enableInventorBonus();
        } else if (getBType() == BuildingEachTurnType.EXTRAFOOD_TURNORDER) {
            player.enableExtraFoodOnTurnOrder();
        } else if (getBType() == BuildingEachTurnType.EXTRACARD) {
            player.enableExtraCard();
        }
    }


    public  BuildingEachTurnType getBType() {
        return bType;
    }

    @Override
    public String toDisplayString() {
        String effect = switch (bType) {
            case BUILDER_DOUBLEPOINTS   -> "×2 PP Costruttori (fin. partita)";
            case RITUAL_DOUBLEPOINTS    -> "×2 PP al Rituale Sciamanico";
            case RITUAL_THREEEXTRASTARS -> "+3 stelle extra al Rituale";
            case RITUAL_NOMALUS         -> "Nessun malus al Rituale";
            case EXTRAFOOD_SET          -> "+5 Cibo per set completo di 6 tipi";
            case EXTRAFOOD_INVENTORS    -> "+3 Cibo per coppia Inventori uguale";
            case EXTRAFOOD_TURNORDER    -> "+1 Cibo extra nelle posizioni bonus turno";
            case EXTRACARD              -> "Carta extra dalla fila superiore dopo i turni";
        };
        return "Edificio (" + ageLabel() + ")  [" + effect + " | costo: " + getFoodCost() + " | PP: " + getPrestigePoint() + "]";
    }

    public String toString() {
        return " {BUILDING_" + bType + ", " +  super.toString()+  "} ";
    }
}