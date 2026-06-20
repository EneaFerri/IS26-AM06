package it.polimi.ingsw.model.cards.Buildings;

import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.EventType;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.cards.CharacterCard;

public class BuildingEvent extends BuildingCard {
    private final EventType eventToRespond;
    private final CharacterType characterToConsider;

    public BuildingEvent(int cardID, Age cardAge, int foodCost, int prestigePoint,
                         EventType eventToRespond, CharacterType characterToConsider) {
        super(cardID, cardAge, foodCost, prestigePoint);
        this.eventToRespond = eventToRespond;
        this.characterToConsider = characterToConsider;
    }

    @Override
    public void applyEventEffect(EventType event, Player player) {

        if (this.eventToRespond == null || this.eventToRespond != event) return;

        if (event == EventType.SUSTENANCE) {
            int moreDiscount = 0;
            for (CharacterCard c : player.getCharacterCards()) {
                if (c.getCharacterType().equals(characterToConsider)) {
                    moreDiscount++;
                }
            }
            player.addBuildingFoodDiscount(moreDiscount);
        }

        if (event == EventType.PICTURES) {
            int n = player.getNumArtists();
            player.addFood(n);
        }

        if (event == EventType.HUNT) {
            int n = player.getNumHunters();
            player.addFood(n);
            player.addPrestige(n);
        }


    }

    public EventType getEventToRespond()          { return eventToRespond; }
    public CharacterType getCharacterToConsider() { return characterToConsider; }

    @Override
    public String toDisplayString() {
        String eventLabel = switch (eventToRespond) {
            case HUNT       -> "Caccia";
            case PICTURES   -> "Pitture Rupestri";
            case RITUAL     -> "Rituale Sciamanico";
            case SUSTENANCE -> "Sostentamento";
        };
        String charLabel = switch (characterToConsider) {
            case ARTIST      -> "Artisti";
            case BUILDER     -> "Costruttori";
            case COLLECTOR   -> "Raccoglitori";
            case HUNTER      -> "Cacciatori";
            case INVENTOR    -> "Inventori";
            case SHAMAN      -> "Sciamani";
            case SET_OF_CHAR -> "set completi";
        };
        return "Edificio (" + ageLabel() + ")  [" + eventLabel + ": bonus su "
                + charLabel + " | costo: " + getFoodCost() + " | PP: " + getPrestigePoint() + "]";
    }

    public String toString(){
        return " { BUILDING_EVENT, " + super.toString() + ", event to respond: " + eventToRespond + ", character to consider: " + characterToConsider + "} ";
    }
}

