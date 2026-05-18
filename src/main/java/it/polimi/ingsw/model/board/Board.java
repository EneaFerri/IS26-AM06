package it.polimi.ingsw.model.board;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.model.cards.*;
import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Board of the game.
 * Contains the cards and the spaces on the board.
 * Every game has exactly one board.
 */

public class Board implements Serializable {
    private static final long serialVersionUID = 1L;

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

    //add all the boardspace cards in the offerfield list by reading them from the json file
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

    //remuve cards from offerfield if not needed based on number of players
    public void prepareGameBoardSpace(int numberOfPlayers) {

        if (numberOfPlayers == 5) {
            return;
        }else if (numberOfPlayers == 4) {
            removeFromOfferField('A');

        }else if (numberOfPlayers == 3) {

            removeFromOfferField('G');
            removeFromOfferField('A');

        }else if (numberOfPlayers == 2) {


            removeFromOfferField('D');
            removeFromOfferField('G');
            removeFromOfferField('A');
        }

    }

    private void removeFromOfferField(char letter){
        offerField.removeIf(boardSpace -> boardSpace.getLetter() == letter);
    }


    public List<BoardSpace> getOfferField() {
        return offerField;
    }

    public void placeTotem(Totem totem, BoardSpace space) {
        space.setTotem(totem);
        totem.place(space);
    }

    //only for the first turn of the game
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
        this.topBuildingCards = filtered;
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

        // delete all the tribe cards bottom row (no buildings)
        bottomTribeCards.clear();

        // move tribe cards top row --> to bottom row (no buildings)
        bottomTribeCards.addAll(topTribeCards);
        topTribeCards.clear();


    }

    // buildings shifts is managed based on the era change
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

        for (TribeCard card : bottomTribeCards) {
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
