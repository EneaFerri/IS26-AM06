package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.cards.*;
import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;

import java.util.ArrayList;
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

    public void addBottomTribeCardsFirstTurn(TribeCard card) {
        this.bottomTribeCards.add(card);
    }
    public void addTopTribeCards(TribeCard card) {
        this.topTribeCards.add(card);
    }

    public void setTopBuildingCards(List<BuildingCard> cards, Age ERA) {
        List<BuildingCard> filtered = new ArrayList<>();
        for (BuildingCard card : cards) {
            if (card.getAge() == ERA) {
                filtered.add(card);
            }
        }
        this.topBuildingCards = filtered; //l'ho modificato perchè quello di prima assegnava tutta la lista con una sola
                                         // corrispondenza hahaha
    }

    public BoardSpace getBoardSpace(char letter){
        for (BoardSpace space : offerField) {
            if (space.getLetter() == letter) {
                return space;
            }
        }
        return null;
    }

    public List<BoardSpace> getFreeBoardSpaces(){
        List<BoardSpace> freeSpaces = new ArrayList<>();
        for (BoardSpace space : offerField) {
            if (space.isFree()) {
                freeSpaces.add(space);
            }
        }
        return freeSpaces;
    }

    public List<Player> getPlayerInOfferOrder(List <Player> players){
        List<Player> ordered = new ArrayList<>();
        for (BoardSpace space : offerField) {
            if (!space.isFree()) {
                for (Player player : players) {
                    if (player.getTotem().equals(space.getTotem())) {
                        ordered.add(player);
                        break;
                    }
                }
            }
        }
        return ordered;
    }

    public List<TribeCard> getAvailableUpperTribeCards(){
        List<TribeCard> availableTribeUp = new ArrayList<>();
        for (TribeCard card : topTribeCards) {
            if (!card.isDrawed()) {
                availableTribeUp.add(card);
            }
        }
        return availableTribeUp;
    }

    public List<TribeCard> getAvailableBottomTribeCards(){
        List<TribeCard> availableTribeBo = new ArrayList<>();
        for (TribeCard card : bottomTribeCards) {
            if (!card.isDrawed()) {
                availableTribeBo.add(card);
            }
        }
        return availableTribeBo;
    }

    public List<BuildingCard> getAvailableUpperBuildingCards() {
        List<BuildingCard> availableBuildUp = new ArrayList<>();
        for (BuildingCard card : topBuildingCards ) {
            if (!card.isDrawed()) {
                availableBuildUp.add(card);
            }
        }
        return availableBuildUp;
    }

    public List<BuildingCard> getAvailableBottomBuildingCards(){
        List<BuildingCard> availableBuildBo = new ArrayList<>();
        for (BuildingCard card : bottomBuildingCards ) {
            if (!card.isDrawed()) {
                availableBuildBo.add(card);
            }
        }
        return availableBuildBo;
    }

    public void removeCard(Card card) {
        if (topTribeCards.remove(card)) return;
        if (bottomTribeCards.remove(card)) return;
        if (topBuildingCards.remove(card)) return;
        bottomBuildingCards.remove(card);
    }

    public void shiftRows() {
        // scarta tutti i TribeCard dalla fila inferiore (le BuildingCard rimangono)
        bottomTribeCards.clear();

        // sposta TribeCard dalla fila superiore alla fila inferiore
        bottomTribeCards.addAll(topTribeCards);
        topTribeCards.clear();

        // le BuildingCard in fila superiore rimangono in fila superiore
        // le BuildingCard in fila inferiore rimangono in fila inferiore
    }

    public void shiftRowsBuildings() {

        bottomBuildingCards.clear();

        bottomBuildingCards.addAll(topBuildingCards);
        topBuildingCards.clear();

    }

    public void clearBoardSpaces(){
        for (BoardSpace space : offerField) {
            space.removeTotem();
        }
    }

    public List<EventCard> getLowRowEvents() {

        List<EventCard> lowRowEvents = new ArrayList<>();

        for (TribeCard card : bottomTribeCards) { //così salviamo anche l'ordine
            if (card.isEvent()) {
                lowRowEvents.add((EventCard) card);
            }
        }

        return lowRowEvents;
    }

    public List<EventCard> getUpRowEvents() {

        List<EventCard> upRowEvents = new ArrayList<>();

        for (TribeCard card : topTribeCards) {
            if (card.isEvent()) {
                upRowEvents.add((EventCard) card);
            }
        }

        return upRowEvents;
    }


}
