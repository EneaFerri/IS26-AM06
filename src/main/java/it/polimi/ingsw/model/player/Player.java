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

    public String getNickname() {
        return nickname;
    }

    public Totem getTotem() {
        return myTotem;
    }

    public int getFood() {
        return nuggets;
    }

    public int getPrestige() {
        return prestige;
    }

    public boolean isInTurn() {
        return inTurn;
    }

    public void setInTurn(boolean inTurn) {
        this.inTurn = inTurn;
    }

    public void addFood(int food) {
        this.nuggets += food;
    }

    public void removeFood(int food) {
        this.nuggets -= food;
    }

    public void addPrestige(int prestige) {
        this.prestige += prestige;
    }

    public void removePrestige(int prestige) {
        this.prestige -= prestige;
    }

    public void enableDoublePointForBuilder() {
        doublePointForBuilder = true;
    }

    public void enableDoublePointForRituals() {
        doublePointForRituals = true;
    }

    public void enableNoMalusForRituals() {
        noMalusForRituals = true;
    }

    public void enableExtraThreeStars() {
        extraThreeStars = true;
    }

    public void enableExtraFoodOnTurnOrder() {
        extraFoodOnTurnOrder = true;
    }

    public void enableExtraCard() {
        extraCard = true;
    }

    public void enableSetBonus() {
        setToCheck = true;
    }

    public void enableInventorBonus() {
        inventorsToCheck = true;
    }

    public void addCharacterCard(CharacterCard card) {
        myCharacterCards.add(card);
        card.markAsDrawed();

        card.onAddedToPlayer(this);

        if(setToCheck) {
            setCountAndCheck();
        }
    }

    private void setCountAndCheck() {
        List<CharacterType> typesFound = new ArrayList<>();

        int n = 0;
        for(CharacterCard card : myCharacterCards) {
            if(!typesFound.contains(card.getCharacterType())){
                typesFound.add(card.getCharacterType());
            }

            if(typesFound.size() == 6) { //completato un set
                n++;
                typesFound.clear();
            }
        }

        if(n > setNumberForExtraFood) {
            this.addFood(5); //extra food dalla carta
            setNumberForExtraFood = n; //update numero di set
        }

    }

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

    public void addBuildingCard(BuildingCard card) {
        myBuildingCards.add(card);
        card.markAsDrawed();
        card.onAddedToPlayer(this);
    }

    public int foodDiscountToBuyBuildings(){
        int discount = 0;
        for(CharacterCard card : myCharacterCards) {
            discount = discount + card.getDiscountForBuildings();
        }
        return discount;
    }

    public boolean hasDoublePointForBuilder() {
        return doublePointForBuilder;
    }

    public List<CharacterCard> getCharacterCards() {
        return Collections.unmodifiableList(myCharacterCards);
    }

    public List<BuildingCard> getBuildingCards() {
        return Collections.unmodifiableList(myBuildingCards);
    }

    public boolean hasExtraFoodOnTurnOrder() {
        return extraFoodOnTurnOrder;}

    public boolean hasExtraCard() {
        return extraCard;
    }

    public List<InventionType> getMyInventions()     { return Collections.unmodifiableList(myInventions); }
    public boolean hasSetToCheck()                   { return setToCheck; }
    public int     getSetNumberForExtraFood()        { return setNumberForExtraFood; }
    public boolean hasInventorsToCheck()             { return inventorsToCheck; }

    public int getCollectorsFoodDiscount() {
        int numcollectors = 0;
        for (CharacterCard card : myCharacterCards) {
            if (card.getCharacterType() == CharacterType.COLLECTOR) {
                numcollectors++;
            }
        }

        return numcollectors*3;
    }

    public int getBuildingFoodDiscount() {
        return foodDiscountFromBuildings;
    }

    public void addBuildingFoodDiscount(int morediscount) {
        foodDiscountFromBuildings += morediscount;
    }

    public void resetBuildingFoodDiscount() {
        this.foodDiscountFromBuildings = 0;
    }

    public int getTotalFoodDiscount() {
        int fromCollectors = getCollectorsFoodDiscount();
        int fromBuildings = getBuildingFoodDiscount();

        return fromCollectors+fromBuildings;
    }

    public int getNumArtists() {
        int counter = 0;
        for (CharacterCard card : myCharacterCards) {
            if (card.getCharacterType() == CharacterType.ARTIST) {
                counter++;
            }
        }
        return counter;
    }

    public int getNumHunters() {
        int counter = 0;
        for (CharacterCard card : myCharacterCards) {
            if (card.getCharacterType() == CharacterType.HUNTER) {
                counter++;
            }
        }
        return counter;
    }

    public int getNumInventors() {
        int counter = 0;
        for (CharacterCard card : myCharacterCards) {
            if (card.getCharacterType() == CharacterType.INVENTOR) {
                counter++;
            }
        }
        return counter;
    }

    public int getNumInventions() {
        int counter = 0;
        for (InventionType invention : myInventions) {
            counter++;
        }
        return counter;
    }

    public int getStarsFromShamans() {
        int counter = 0;
        for (CharacterCard card : myCharacterCards) {
            counter += card.getShamanStars();
        }
        return counter;
    }

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


    public int countSet() {
        // contiamo quante carte ha il player per ciascuno dei 6 tipi "veri"
        // un set completo richiede almeno 1 carta per ogni tipo
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

    public int getPointsFromEndEffect() {

        int pointsFromEndEffect = 0;

        for (BuildingCard bCard : myBuildingCards) {
            pointsFromEndEffect += bCard.getEndEffectPoints(this);
        }

        return pointsFromEndEffect;
    }

    public int getTotalPoints() {
        return getTotalPointsPreEffect() + getPointsFromEndEffect();
    }

    /**
     * Imposta direttamente tutti i campi di stato dal snapshot persistito.
     * NON chiama onAddedToPlayer né altri side-effect: i flag sono già nella
     * loro forma finale nel snapshot.
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
