package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.cards.Buildings.BuildingCard;

import java.util.*;

public class Deck
{
    private List<Card> cards;
    private List<TribeCard> tribeCards;
    private List<BuildingCard> buildingCards;


    public Deck(){
        tribeCards = new ArrayList<>();
        buildingCards = new ArrayList<>();

    }

    public void addCard(TribeCard c){
        tribeCards.add(c);
    }

    public Card draw(){
        if(tribeCards.isEmpty())
            return null;
        return tribeCards.remove(0);
    }

    public List<TribeCard> draw(int n) {
        List<TribeCard> drawn = new ArrayList<>();
        for (int i = 0; i < n && !tribeCards.isEmpty(); i++) {
            drawn.add(tribeCards.remove(0));
        }
        return drawn;
    }

    public void randomize (){
        Collections.shuffle(tribeCards);

    }

    public boolean isEmpty(){
        return tribeCards.isEmpty();
    }

    public int size(){
        return tribeCards.size();
    }

    //TODO: metodo create deck per age?
}
