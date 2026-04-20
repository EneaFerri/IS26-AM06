package it.polimi.ingsw.model.board;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.model.cards.*;
import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Board {



    private List<TribeCard> topTribeCards;
    private List<TribeCard> bottomTribeCards;

    private List<BuildingCard> topBuildingCards;
    private List<BuildingCard> bottomBuildingCards;

    private List<BoardSpace> offerField;


    public Board() {
        topTribeCards = new ArrayList<>();
        bottomTribeCards = new ArrayList<>();
        topBuildingCards = new ArrayList<>();
        bottomBuildingCards = new ArrayList<>();

        offerField = new ArrayList<>();
        configureOfferField();
    }

    /*
    public Board(List<TribeCard> topTribeCards, List<TribeCard> bottomTribeCards,
                 List<BuildingCard> topBuildingCards, List<BuildingCard> bottomBuildingCards) {


        this.topTribeCards = topTribeCards;
        this.bottomTribeCards = bottomTribeCards;
        this.topBuildingCards = topBuildingCards;
        this.bottomBuildingCards = bottomBuildingCards;

        this.offerField = null;
        configureOfferField();
    }
    */

    //aggiunge tutte le carte boardspace nella lista di offerfield prendendole direttamente da json file
    private void configureOfferField() {

        ObjectMapper mapper = new ObjectMapper();

        try{
            JsonNode root = mapper.readTree(
                    getClass().getResourceAsStream("/boardSpaces.json")
            );
            for (JsonNode node : root) {
                offerField.add(BoardConfiguration.createBoardSpace(node));
            }
        }catch(IOException e){
            throw new RuntimeException("errore caricamento da JSON", e);
        }
    }

    public void prepareGameBoardSpace(int numberOfPlayers) {
        //rimuove le carte dalla lista offerfield se inutili in base al numero player

        if (numberOfPlayers == 5) {
            return;
        }else if (numberOfPlayers == 4) {
            //rimuovi lettera A
            removeFromOfferField('A');
        }else if (numberOfPlayers == 3) {
            //rimuovi lettera G
            removeFromOfferField('G');
            removeFromOfferField('A');
        }else if (numberOfPlayers == 2) {
            //riumuovi lettera D
            removeFromOfferField('D');
            removeFromOfferField('G');
            removeFromOfferField('A');
        }

    }

    private void removeFromOfferField(char letter){
        offerField.removeIf(boardSpace -> boardSpace.getLetter() == letter);
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
                    if (player.getTotem() == space.getTotem()) {
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

    public String toString(){
        return "Top row: " + topTribeCards + ", Top row buildings: " + topBuildingCards +
                "\n" +  "OfferSPace: " + printBoardSpaces() +
                "\n" +  " Bottom row: " + bottomTribeCards + " Bottom row buildings: " + bottomBuildingCards;
    }

    private String printBoardSpaces(){
        String boardSpaces = "";
        for (BoardSpace space : offerField) {
            boardSpaces += space.toString() + " ";
        }
        return boardSpaces;
    }

}
