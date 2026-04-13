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

public class Player {
    private final String nickname;
    private final Totem myTotem;

    private int nuggets;
    private int prestige;

    private boolean inTurn;

    private List<CharacterCard> myCharacterCards;
    private List<BuildingCard> myBuildingCards;

    //tengo traccia delle diverse "invenzioni"
    private List<InventionType> myInventions;

    //VARIABILI D'APPOGGIO PER GESTIONE EFFETTI BUILDING, quelli che vanno considerati una volta pescata la carta
    private boolean doublePointForBuilder = false; //gestione building che raddoppia punti dei builder

    public boolean doublePointForRituals = false; //gestione building che raddoppia punti per evento rituale
    public boolean noMalusForRituals = false; // gestione building che rimuove malus per evento rituale
    public boolean extraThreeStars = false; //gestione building che aggiunge 3 stelle

    private boolean extraFoodOnTurnOrder = false;   // per il building che permette di ottenere scibo in piu rispetto alla posizione del totem (lo metto in turnOrder)
    private boolean extraCard = false; // building che permette di pescare una carta in più dalla riga sopra ma solo prima della fine del turno, prima che
                                        // in game->advanceNextPlayer si passi al game state di risoluzione eventi

    private boolean setToCheck = false; // gestione building con extrafood per ogni set
    private int setNumberForExtraFood = 0;

    private boolean inventorsToCheck = false;

    private int foodDiscountFromBuildings; //variabile comoda per tenere sconti di cibo dalle building: si ma usiamo
    // private e metodo

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
        //in poche parole controllo se è gia presente un inventore con la stessa invenzione di quello nuobo in myCharacterCards
        //se si, allora ho un doppione  e aggiungo +3 di cibo
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

    public boolean hasExtraFoodOnTurnOrder() { //getters per extraFoodOnTurnOrder e extraCard
        return extraFoodOnTurnOrder;}

    public boolean hasExtraCard() {
        return extraCard; }

    // L'ho aggiunto per calcolare los conto totale di cibo durante il gioco in base al numero di raccoglitori che si hanno
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

    //funzione d'appoggio per maggiore chiarimento di come calcolare sconto totale
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

    //punti totali pre effetti finali delle buildings
    public int getTotalPointsPreEffect() {
        int currPre = prestige;

        int sum = 0;

        for (CharacterCard card : myCharacterCards) {
            sum += card.getPrestigeContribution(this);
        }

        //ARTISTI
        int nArtistCouple = getNumArtists()/2;
        sum = sum + nArtistCouple*10;

        //INVENTORI
        int fromInventors = getNumInventors() * getNumInventions();
        sum = sum + fromInventors;

        //PUNTI BASE DA BUILDING (no effetto, solo prestigio standard)
        for (BuildingCard card : myBuildingCards) {
            sum += card.getPrestigePoint();
        }

        return currPre + sum;
    }

   // public int pointsFromEndEffect = 0; NON SERVE secondo me

    public int countSet() {
        //TODO
        return 0;
    }

    public int getPointsFromEndEffect() {
        // azzera il contatore all'inizio, xche se chiami questo metodo due volte
        // per sbaglio, il giocatore fa il doppio dei punti
        int pointsFromEndEffect = 0;

        for (BuildingCard bCard : myBuildingCards) {
            pointsFromEndEffect += bCard.getEndEffectPoints(this);
        }

        return pointsFromEndEffect;
    }

    public int getTotalPoints() {
        // NON sovrascrivere prestige — restituisce solo il calcolo
        return getTotalPointsPreEffect() + getPointsFromEndEffect();
    }


}
