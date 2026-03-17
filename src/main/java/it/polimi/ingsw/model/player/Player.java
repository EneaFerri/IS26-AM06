package it.polimi.ingsw.model.player;

import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.enums.CharacterType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Player {
    private final String nickname;
    private final Totem myTotem;

    private int nuggets;
    private int prestige;

    private boolean inTurn;

    private List<CharacterCard> myCharacterCards;
    private List<BuildingCard> myBuildingCards;

    public Player(String nickname, Totem myTotem) {
        this.nickname = nickname;
        this.myTotem = myTotem;
        this.nuggets = 0;
        this.prestige = 0;
        this.inTurn = false;
        this.myCharacterCards = new ArrayList<>();
        this.myBuildingCards = new ArrayList<>();
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

    public void addCharacterCard(CharacterCard card) {
        myCharacterCards.add(card);
        card.markAsDrawed();
    }

    public void addBuildingCard(BuildingCard card) {
        myBuildingCards.add(card);
        card.markAsDrawed();
    }

    public List<CharacterCard> getCharacterCards() {
        return Collections.unmodifiableList(myCharacterCards);
    }

    public List<BuildingCard> getBuildingCards() {
        return Collections.unmodifiableList(myBuildingCards);
    }

    // L'ho aggiunto per calcolare los conto totale di cibo durante il gioco in base al numero di raccoglitori che si hanno
    public int getTotalFoodDiscount() {
        int collectors = 0;
        for (CharacterCard card : myCharacterCards) {
            if (card.getCharacterType() == CharacterType.COLLECTOR) {
                collectors++;
            }
        }
        return collectors * 3;
    }

    // l'ho messa per calcolare il punteggio totale però la rivediamo domani, penso possa essere utile
    // (però bisogna aggiungere anche gli altri casi)
    public int getTotalPoints() {
        int total = prestige;

        for (BuildingCard card : myBuildingCards) {
            total += card.getPrestigePoint();
        }

        return total;
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

    public int getStarsFromShamans() {
        int counter = 0;
        for (CharacterCard card : myCharacterCards) {
            //TODO semplicemente contare stelle degli shamani
        }

        return counter;
    }
}
