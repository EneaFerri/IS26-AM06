package it.polimi.ingsw.model.cards;

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

    public void addCard(Card c){
        if (c instanceof TribeCard ) {
            tribeCards.add((TribeCard) c);
        } else {
            buildingCards.add((BuildingCard) c);
        }
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


}
