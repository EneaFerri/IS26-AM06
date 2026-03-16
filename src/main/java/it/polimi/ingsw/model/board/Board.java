package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.cards.Card;
import it.polimi.ingsw.model.cards.Deck;
import it.polimi.ingsw.model.cards.TribeCard;
import it.polimi.ingsw.model.player.Totem;

import java.util.List;

public class Board {

    private Deck currentDeck;

    private List<TribeCard> topTribeCards;
    private List<TribeCard> bottomTribeCards;

    private List<BuildingCard> topBuildingCards;
    private List<BuildingCard> bottomBuildingCards;

    private List<BoardSpace> offerField;

    public Board(Deck deck, List<TribeCard> topTribeCards, List<TribeCard> bottomTribeCards,
                 List<BuildingCard> topBuildingCards, List<BuildingCard> bottomBuildingCards,
                 List<BoardSpace> offerField) {

        this.currentDeck = deck;
        this.topTribeCards = topTribeCards;
        this.bottomTribeCards = bottomTribeCards;
        this.topBuildingCards = topBuildingCards;
        this.bottomBuildingCards = bottomBuildingCards;
        this.offerField = offerField;
    }

    public List<TribeCard> getTopRowTribe() {
        return topTribeCards;
    }

    public List<TribeCard> getLowRowTribe() {
        return bottomTribeCards;
    }

    public List<BuildingCard> getTopRowBuild() {
        return topBuildingCards;
    }

    public List<BuildingCard> getLowRowBuild() {
        return bottomBuildingCards;
    }

    public List<BoardSpace> getOfferField() {
        return offerField;
    }

    public void placeTotem(Totem totem, BoardSpace space) {
        space.setTotem(totem);
        totem.place(space);
    }

    public BoardSpace getBoardSpace(char letter){
        //TODO
    }
    public List<BoardSpace> getFreeBoardSpaces(){
        //TODO
    }
    public List<TribeCard> getAvailableTribeCards(){
        //TODO
    }
    public List<BuildingCard> getAvailableBuildingCards(){
        //TODO
    }
    public void removeCard(Card card){
        //TODO
    }
    public void shiftRows(){
        //TODO
    }
    public void clearBoardSpaces(){
        //TODO
    }





}
