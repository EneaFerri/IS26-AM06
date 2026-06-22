package it.polimi.ingsw.model.cards.Buildings;

import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.EventType;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.cards.CharacterCard;

/**
 * A building card that reacts to a specific event type during event resolution.
 *
 * <p>When the corresponding event occurs, this building applies a bonus to its
 * owner based on the number of matching character cards they hold.</p>
 */
public class BuildingEvent extends BuildingCard {
    private final EventType eventToRespond;
    private final CharacterType characterToConsider;

    /**
     * Creates a new event-reactive building card.
     *
     * @param cardID              the unique identifier for this card
     * @param cardAge             the era this card belongs to
     * @param foodCost            the food cost to purchase this building
     * @param prestigePoint       the prestige points granted on purchase
     * @param eventToRespond      the event type that triggers this building's effect
     * @param characterToConsider the character type whose count determines the bonus magnitude
     */
    public BuildingEvent(int cardID, Age cardAge, int foodCost, int prestigePoint,
                         EventType eventToRespond, CharacterType characterToConsider) {
        super(cardID, cardAge, foodCost, prestigePoint);
        this.eventToRespond = eventToRespond;
        this.characterToConsider = characterToConsider;
    }

    /**
     * Applies this building's bonus effect when its trigger event occurs.
     *
     * @param event  the event type being resolved
     * @param player the player who owns this building
     */
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

    /**
     * Returns the event type that triggers this building's effect.
     *
     * @return the trigger {@link EventType}
     */
    public EventType getEventToRespond()          { return eventToRespond; }

    /**
     * Returns the character type used to calculate the bonus magnitude.
     *
     * @return the {@link CharacterType}
     */
    public CharacterType getCharacterToConsider() { return characterToConsider; }

    /**
     * Returns a formatted display string describing this building's event reaction for the UI.
     *
     * @return the display string
     */
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

    /**
     * Returns a string representation of this building card.
     *
     * @return a string containing the building type, base card info, event type, and character type
     */
    public String toString(){
        return " { BUILDING_EVENT, " + super.toString() + ", event to respond: " + eventToRespond + ", character to consider: " + characterToConsider + "} ";
    }
}
