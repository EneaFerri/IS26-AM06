package it.polimi.ingsw.model.cards.Buildings;

import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.BuildingEachTurnType;
import it.polimi.ingsw.model.player.Player;

/**
 * A building card that grants a persistent per-turn or permanent bonus effect.
 *
 * <p>When added to a player's collection, this card enables one of several
 * recurring abilities on the player, such as doubling prestige from Builders,
 * gaining extra food, or drawing an additional card each turn.</p>
 */
public class BuildingEachTurn extends BuildingCard {

    private BuildingEachTurnType bType;

    /**
     * Creates a new per-turn building card.
     *
     * @param cardID        the unique identifier for this card
     * @param cardAge       the era this card belongs to
     * @param foodCost      the food cost to purchase this building
     * @param prestigePoint the prestige points granted on purchase
     * @param bType         the type of recurring effect this building provides
     */
    public BuildingEachTurn(int cardID, Age cardAge, int foodCost, int prestigePoint, BuildingEachTurnType bType) {
        super(cardID, cardAge, foodCost, prestigePoint);
        this.bType = bType;
    }

    /**
     * Enables the corresponding recurring ability on the player when this building is purchased.
     *
     * @param player the player who purchased this building
     */
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

    /**
     * Returns the recurring effect type of this building.
     *
     * @return the {@link BuildingEachTurnType}
     */
    public  BuildingEachTurnType getBType() {
        return bType;
    }

    /**
     * Returns a formatted display string describing this building's effect for the UI.
     *
     * @return the display string
     */
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

    /**
     * Returns a string representation of this building card.
     *
     * @return a string containing the building type and base card info
     */
    public String toString() {
        return " {BUILDING_" + bType + ", " +  super.toString()+  "} ";
    }
}
