package it.polimi.ingsw.model.player;

import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.cards.Buildings.BuildingEachTurn;
import it.polimi.ingsw.model.cards.Buildings.BuildingEnd;
import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.cards.Characters.Builder;
import it.polimi.ingsw.model.cards.Characters.Hunter;
import it.polimi.ingsw.model.cards.Characters.Inventor;
import it.polimi.ingsw.model.cards.Characters.Shaman;
import it.polimi.ingsw.model.enums.BuildingEachTurnType;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.enums.InventionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.EnumMap;
import java.util.Map;

/**
 * Represents a player in the game, holding their resources, cards, flags, and identity.
 *
 * <p>All card additions go through {@link #addCharacterCard} or {@link #addBuildingCard},
 * which fire the card's side-effects. For persistence restoration, use
 * {@link #restoreState} instead to bypass side-effects.</p>
 */
public class Player {
    private final String nickname;
    private final Totem myTotem;

    private int nuggets;
    private int prestige;

    private boolean inTurn;

    private List<CharacterCard> myCharacterCards;
    private List<BuildingCard> myBuildingCards;

    private List<InventionType> myInventions;

    private boolean doublePointForBuilder = false;

    public boolean doublePointForRituals = false;
    public boolean noMalusForRituals = false;
    public boolean extraThreeStars = false;

    private boolean extraFoodOnTurnOrder = false;
    private boolean extraCard = false;

    private boolean setToCheck = false;
    private int setNumberForExtraFood = 0;

    private boolean inventorsToCheck = false;

    private int foodDiscountFromBuildings;

    /**
     * Creates a new player with the given nickname and assigned totem.
     * All counters start at zero and all bonus flags default to {@code false}.
     *
     * @param nickname the unique display name for this player
     * @param myTotem  the totem piece assigned to this player
     */
    public Player(String nickname, Totem myTotem) {
        this.nickname = nickname;
        this.myTotem = myTotem;
        this.nuggets = 0;
        this.prestige = 0;
        this.inTurn = false;
        this.myCharacterCards = new ArrayList<>();
        this.myBuildingCards = new ArrayList<>();
        this.myInventions = new ArrayList<>();
    }

    /**
     * Returns this player's unique nickname.
     *
     * @return the player's nickname
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Returns the totem piece assigned to this player.
     *
     * @return this player's {@link Totem}
     */
    public Totem getTotem() {
        return myTotem;
    }

    /**
     * Returns the current amount of food (nuggets) this player has.
     *
     * @return the food count
     */
    public int getFood() {
        return nuggets;
    }

    /**
     * Returns the current prestige (score) of this player.
     *
     * @return the prestige points
     */
    public int getPrestige() {
        return prestige;
    }

    /**
     * Returns whether it is currently this player's turn.
     *
     * @return {@code true} if it is this player's turn
     */
    public boolean isInTurn() {
        return inTurn;
    }

    /**
     * Sets or clears the in-turn flag for this player.
     *
     * @param inTurn {@code true} to mark this player as the active player
     */
    public void setInTurn(boolean inTurn) {
        this.inTurn = inTurn;
    }

    /**
     * Adds food (nuggets) to this player's supply.
     *
     * @param food the amount of food to add
     */
    public void addFood(int food) {
        this.nuggets += food;
    }

    /**
     * Removes food (nuggets) from this player's supply.
     *
     * @param food the amount of food to remove
     */
    public void removeFood(int food) {
        this.nuggets -= food;
    }

    /**
     * Adds prestige points to this player's score.
     *
     * @param prestige the amount of prestige to add
     */
    public void addPrestige(int prestige) {
        this.prestige += prestige;
    }

    /**
     * Removes prestige points from this player's score.
     *
     * @param prestige the amount of prestige to remove
     */
    public void removePrestige(int prestige) {
        this.prestige -= prestige;
    }

    /**
     * Enables the double-point bonus for Builder cards for this player.
     */
    public void enableDoublePointForBuilder() {
        doublePointForBuilder = true;
    }

    /**
     * Enables the double-point bonus for Ritual events for this player.
     */
    public void enableDoublePointForRituals() {
        doublePointForRituals = true;
    }

    /**
     * Enables the no-malus flag for Ritual events for this player.
     */
    public void enableNoMalusForRituals() {
        noMalusForRituals = true;
    }

    /**
     * Enables the extra-three-stars bonus for this player.
     */
    public void enableExtraThreeStars() {
        extraThreeStars = true;
    }

    /**
     * Enables the bonus that grants extra food when the player's totem is placed on the turn-order track.
     */
    public void enableExtraFoodOnTurnOrder() {
        extraFoodOnTurnOrder = true;
    }

    /**
     * Enables the bonus that grants an extra card pick for this player.
     */
    public void enableExtraCard() {
        extraCard = true;
    }

    /**
     * Enables the set-completion bonus tracking for this player.
     */
    public void enableSetBonus() {
        setToCheck = true;
    }

    /**
     * Enables the Inventor synergy bonus tracking for this player.
     */
    public void enableInventorBonus() {
        inventorsToCheck = true;
    }

    /**
     * Adds a character card to this player's hand and fires its on-add side-effects.
     * If the set-completion bonus is active, also checks for completed sets.
     *
     * @param card the {@link CharacterCard} to add
     */
    public void addCharacterCard(CharacterCard card) {
        myCharacterCards.add(card);
        card.markAsDrawed();

        card.onAddedToPlayer(this);

        if(setToCheck) {
            setCountAndCheck();
        }
    }

    private void setCountAndCheck() {
        // count how many cards the player has for each of the 6 character types
        // a complete set requires at least 1 card of each type
        List<CharacterType> typesFound = new ArrayList<>();

        int n = 0;
        for(CharacterCard card : myCharacterCards) {
            if(!typesFound.contains(card.getCharacterType())){
                typesFound.add(card.getCharacterType());
            }

            if(typesFound.size() == 6) { // completed a set
                n++;
                typesFound.clear();
            }
        }

        if(n > setNumberForExtraFood) {
            this.addFood(5); // extra food from the set-bonus card
            setNumberForExtraFood = n; // update the set counter
        }

    }

    /**
     * Checks Inventor synergies when a new Inventor card is added.
     * If another Inventor with the same invention type is already owned, grants bonus food.
     *
     * @param newInventor the newly added {@link Inventor} card
     */
    public void inventorsCountAndCheck(Inventor newInventor) {

        InventionType type = newInventor.getInvention();

        if (!myInventions.contains(type)) {
            myInventions.add(type);
        }

        if (inventorsToCheck) {
            for (CharacterCard card : myCharacterCards) {
                if (card != newInventor && card.hasInvention(type)) {
                    this.addFood(3);
                }
            }
        }
    }

    /**
     * Adds a building card to this player's hand and fires its on-add side-effects.
     *
     * @param card the {@link BuildingCard} to add
     */
    public void addBuildingCard(BuildingCard card) {
        myBuildingCards.add(card);
        card.markAsDrawed();
        card.onAddedToPlayer(this);
    }

    /**
     * Calculates the total food discount this player receives when buying building cards,
     * summing discounts from all character cards.
     *
     * @return the total food discount for building purchases
     */
    public int foodDiscountToBuyBuildings(){
        int discount = 0;
        for(CharacterCard card : myCharacterCards) {
            discount = discount + card.getDiscountForBuildings();
        }
        return discount;
    }

    /**
     * Returns whether the double-point-for-Builder bonus is active.
     *
     * @return {@code true} if the double-point bonus for Builder cards is enabled
     */
    public boolean hasDoublePointForBuilder() {
        return doublePointForBuilder;
    }

    /**
     * Returns an unmodifiable view of this player's character cards.
     *
     * @return an unmodifiable list of {@link CharacterCard}
     */
    public List<CharacterCard> getCharacterCards() {
        return Collections.unmodifiableList(myCharacterCards);
    }

    /**
     * Returns an unmodifiable view of this player's building cards.
     *
     * @return an unmodifiable list of {@link BuildingCard}
     */
    public List<BuildingCard> getBuildingCards() {
        return Collections.unmodifiableList(myBuildingCards);
    }

    /**
     * Returns whether this player receives extra food when their totem is placed on the turn-order track.
     *
     * @return {@code true} if the extra-food-on-turn-order bonus is active
     */
    public boolean hasExtraFoodOnTurnOrder() {
        return extraFoodOnTurnOrder;}

    /**
     * Returns whether this player has an extra card pick bonus.
     *
     * @return {@code true} if the extra-card bonus is active
     */
    public boolean hasExtraCard() {
        return extraCard;
    }

    /**
     * Returns an unmodifiable view of the invention types this player has discovered.
     *
     * @return an unmodifiable list of {@link InventionType}
     */
    public List<InventionType> getMyInventions()     { return Collections.unmodifiableList(myInventions); }

    /**
     * Returns whether the set-completion bonus is being tracked for this player.
     *
     * @return {@code true} if set tracking is enabled
     */
    public boolean hasSetToCheck()                   { return setToCheck; }

    /**
     * Returns the number of complete character-card sets this player has assembled so far.
     *
     * @return the current set count used for the extra-food bonus
     */
    public int     getSetNumberForExtraFood()        { return setNumberForExtraFood; }

    /**
     * Returns whether Inventor synergy bonuses are being tracked for this player.
     *
     * @return {@code true} if Inventor tracking is enabled
     */
    public boolean hasInventorsToCheck()             { return inventorsToCheck; }

    /**
     * Calculates the food discount granted by Collector character cards.
     * Each Collector provides a discount of 3 food.
     *
     * @return the total food discount from Collector cards
     */
    public int getCollectorsFoodDiscount() {
        int numcollectors = 0;
        for (CharacterCard card : myCharacterCards) {
            if (card.getCharacterType() == CharacterType.COLLECTOR) {
                numcollectors++;
            }
        }

        return numcollectors*3;
    }

    /**
     * Returns the food discount accumulated from building cards.
     *
     * @return the food discount from buildings
     */
    public int getBuildingFoodDiscount() {
        return foodDiscountFromBuildings;
    }

    /**
     * Adds to the food discount provided by building cards.
     *
     * @param morediscount the additional food discount to apply
     */
    public void addBuildingFoodDiscount(int morediscount) {
        foodDiscountFromBuildings += morediscount;
    }

    /**
     * Resets the food discount from building cards to zero.
     */
    public void resetBuildingFoodDiscount() {
        this.foodDiscountFromBuildings = 0;
    }

    /**
     * Returns the total food discount this player has from all sources (Collectors + buildings).
     *
     * @return the combined food discount
     */
    public int getTotalFoodDiscount() {
        int fromCollectors = getCollectorsFoodDiscount();
        int fromBuildings = getBuildingFoodDiscount();

        return fromCollectors+fromBuildings;
    }

    /**
     * Counts the number of Artist character cards this player owns.
     *
     * @return the number of Artist cards
     */
    public int getNumArtists() {
        int counter = 0;
        for (CharacterCard card : myCharacterCards) {
            if (card.getCharacterType() == CharacterType.ARTIST) {
                counter++;
            }
        }
        return counter;
    }

    /**
     * Counts the number of Hunter character cards this player owns.
     *
     * @return the number of Hunter cards
     */
    public int getNumHunters() {
        int counter = 0;
        for (CharacterCard card : myCharacterCards) {
            if (card.getCharacterType() == CharacterType.HUNTER) {
                counter++;
            }
        }
        return counter;
    }

    /**
     * Counts the number of Inventor character cards this player owns.
     *
     * @return the number of Inventor cards
     */
    public int getNumInventors() {
        int counter = 0;
        for (CharacterCard card : myCharacterCards) {
            if (card.getCharacterType() == CharacterType.INVENTOR) {
                counter++;
            }
        }
        return counter;
    }

    /**
     * Counts the number of distinct invention types this player has discovered.
     *
     * @return the number of unique inventions
     */
    public int getNumInventions() {
        int counter = 0;
        for (InventionType invention : myInventions) {
            counter++;
        }
        return counter;
    }

    /**
     * Calculates the total shaman stars contributed by all Shaman character cards.
     *
     * @return the total shaman star count
     */
    public int getStarsFromShamans() {
        int counter = 0;
        for (CharacterCard card : myCharacterCards) {
            counter += card.getShamanStars();
        }
        return counter;
    }

    /**
     * Calculates total prestige points before end-of-game building effects are applied.
     * Includes base prestige, character card contributions, Artist pairs, Inventor combos,
     * and building card points.
     *
     * @return prestige total before end-game building effects
     */
    public int getTotalPointsPreEffect() {
        int currPre = prestige;

        int sum = 0;

        for (CharacterCard card : myCharacterCards) {
            sum += card.getPrestigeContribution(this);
        }

        int nArtistCouple = getNumArtists()/2;
        sum = sum + nArtistCouple*10;

        int fromInventors = getNumInventors() * getNumInventions();
        sum = sum + fromInventors;

        for (BuildingCard card : myBuildingCards) {
            sum += card.getPrestigePoint();
        }

        return currPre + sum;
    }

    /**
     * Counts how many complete sets of all 6 character types this player has assembled.
     *
     * @return the number of complete sets
     */
    public int countSet() {
        // count how many cards the player has for each of the 6 character types
        // a complete set requires at least 1 card of each type
        Map<CharacterType, Integer> counts = new EnumMap<>(CharacterType.class);

        counts.put(CharacterType.ARTIST, 0);
        counts.put(CharacterType.BUILDER, 0);
        counts.put(CharacterType.COLLECTOR, 0);
        counts.put(CharacterType.HUNTER, 0);
        counts.put(CharacterType.INVENTOR, 0);
        counts.put(CharacterType.SHAMAN, 0);

        for (CharacterCard card : myCharacterCards) {
            CharacterType type = card.getCharacterType();

            if (counts.containsKey(type)) {
                counts.put(type, counts.get(type) + 1);
            }
        }

        return counts.values().stream()
                .min(Integer::compareTo)
                .orElse(0);

    }

    /**
     * Calculates prestige points from end-of-game building card effects.
     *
     * @return total points from building end-game effects
     */
    public int getPointsFromEndEffect() {

        int pointsFromEndEffect = 0;

        for (BuildingCard bCard : myBuildingCards) {
            pointsFromEndEffect += bCard.getEndEffectPoints(this);
        }

        return pointsFromEndEffect;
    }

    /**
     * Calculates this player's total prestige score, including all effects.
     *
     * @return the final total prestige score
     */
    public int getTotalPoints() {
        return getTotalPointsPreEffect() + getPointsFromEndEffect();
    }

    /**
     * Directly restores all state fields from a persisted snapshot.
     * Does NOT call {@code onAddedToPlayer} or any other side-effects;
     * all flags are already in their final form inside the snapshot.
     *
     * @param nuggets                 food count to restore
     * @param prestige                prestige points to restore
     * @param chars                   character cards to restore
     * @param buildings               building cards to restore
     * @param inventions              invention types to restore
     * @param doublePointForBuilder   double-point-for-Builder flag
     * @param doublePointForRituals   double-point-for-Rituals flag
     * @param noMalusForRituals       no-malus-for-Rituals flag
     * @param extraThreeStars         extra-three-stars flag
     * @param extraFoodOnTurnOrder    extra-food-on-turn-order flag
     * @param extraCard               extra-card flag
     * @param setToCheck              set-completion tracking flag
     * @param setNumberForExtraFood   number of complete sets already counted
     * @param inventorsToCheck        Inventor synergy tracking flag
     * @param foodDiscountFromBuildings accumulated building food discount
     */
    public void restoreState(int nuggets, int prestige,
                             List<CharacterCard> chars, List<BuildingCard> buildings,
                             List<InventionType> inventions,
                             boolean doublePointForBuilder, boolean doublePointForRituals,
                             boolean noMalusForRituals, boolean extraThreeStars,
                             boolean extraFoodOnTurnOrder, boolean extraCard,
                             boolean setToCheck, int setNumberForExtraFood,
                             boolean inventorsToCheck, int foodDiscountFromBuildings) {
        this.nuggets = nuggets;
        this.prestige = prestige;
        this.myCharacterCards = new ArrayList<>(chars);
        this.myBuildingCards  = new ArrayList<>(buildings);
        this.myInventions     = new ArrayList<>(inventions);
        this.doublePointForBuilder   = doublePointForBuilder;
        this.doublePointForRituals   = doublePointForRituals;
        this.noMalusForRituals       = noMalusForRituals;
        this.extraThreeStars         = extraThreeStars;
        this.extraFoodOnTurnOrder    = extraFoodOnTurnOrder;
        this.extraCard               = extraCard;
        this.setToCheck              = setToCheck;
        this.setNumberForExtraFood   = setNumberForExtraFood;
        this.inventorsToCheck        = inventorsToCheck;
        this.foodDiscountFromBuildings = foodDiscountFromBuildings;
    }

    /**
     * Returns a string representation of this player for debugging purposes.
     *
     * @return a string containing nickname, totem, food, prestige, turn status, and cards
     */
    public String toString() {
        return "Player{" +
                "nickname='" + nickname + '\'' +
                ", myTotem= " + myTotem.toString() +
                ", nuggets=" + nuggets +
                ", prestige=" + prestige +
                ", inTurn=" + inTurn +
                ", myCharacterCards=" + myCharacterCards +
                ", myBuildingCards=" + myBuildingCards +
                ", myInventions=" + myInventions +
                '}';
    }
}
