package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.BuildingEffectType;
import it.polimi.ingsw.model.enums.BuildingEffectTime;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.cards.EventCard;

public class BuildingCard extends Card {
    private final int foodCost;
    private final int prestigePoint;
    private final BuildingEffectType effectType;
    private final BuildingEffectTime effectTiming;

    public BuildingCard(int cardID, Age cardAge, int foodCost, int prestigePoint,
                        BuildingEffectType effectType, BuildingEffectTime effectTiming) {
        super(cardID, cardAge);
        this.foodCost = foodCost;
        this.prestigePoint = prestigePoint;
        this.effectType = effectType;
        this.effectTiming = effectTiming;
    }

    public int getFoodCost() {
        return foodCost;
    }

    public int getPrestigePoint() {
        return prestigePoint;
    }

    public BuildingEffectType getEffectType() {
        return effectType;
    }

    public BuildingEffectTime getEffectTiming() {
        return effectTiming;
    }
    public void applyEffect(Game game, Player player, EventCard event) {
        if (effectTiming == null || effectType == null) {
            return;
        }

        switch (effectTiming) {
            case IMMEDIATE:
                switch (effectType) {
                    case EXTRA_NUGGETS:
                        // TODO: define the exact amount according to the building card.
                        break;
                    case PRESTIGE_BONUS:
                        // TODO: define the exact amount according to the building card.
                        break;
                    default:
                        break;
                }
                break;

            case EVENT:
                if (event == null) {
                    return;
                }

                switch (effectType) {
                    case EXTRA_NUGGETS:
                        // TODO
                        break;
                    case PRESTIGE_BONUS:
                        // TODO
                        break;
                    case BUILDING_DISCOUNT:
                        // TODO
                        break;
                    case ARTIST_BONUS:
                        // TODO
                        break;
                    default:
                        break;
                }
                break;

            case ENDOFTURN:
                switch (effectType) {
                    case NUGGET_DISCOUNT:
                        // TODO
                        break;
                    case BUILDING_DISCOUNT:
                        // TODO

                        break;
                        //TODO: FARE COSÍ PER TUTTI GLI EFFETTI
                    default:
                        break;
                }
                break;

            case ENDOFGAME:
                switch (effectType) {
                    case PRESTIGE_BONUS:
                        // TODO
                        break;
                    default:
                        break;
                }
                break;

            default:
                break;
        }
    }
}
