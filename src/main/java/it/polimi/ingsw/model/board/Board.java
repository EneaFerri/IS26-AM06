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

/**
 * Represents the game board, holding the card rows (tribe and building),
 * the offer field (totem placement spaces), and utility methods for game flow.
 */
public class Board {

    private List<TribeCard> topTribeCards;
    private List<TribeCard> bottomTribeCards;

    private List<BuildingCard> topBuildingCards;
    private List<BuildingCard> bottomBuildingCards;

    private List<BoardSpace> offerField;

    /**
     * Creates a new Board and configures the offer field from the JSON resource.
     */
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
            throw new RuntimeException("error loading from JSON", e);
        }
    }

    /**
     * Removes offer-field spaces that are not used for the given player count.
     *
     * @param numberOfPlayers the number of players in the game (2–5)
     */
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

    /**
     * Returns the top row of tribe cards.
     *
     * @return list of tribe cards in the top row
     */
    public List<TribeCard> getTopRowTribe() {
        return topTribeCards;
    }

    /**
     * Returns the bottom row of tribe cards.
     *
     * @return list of tribe cards in the bottom row
     */
    public List<TribeCard> getLowRowTribe() {
        return bottomTribeCards;
    }

    /**
     * Returns the top row of building cards.
     *
     * @return list of building cards in the top row
     */
    public List<BuildingCard> getTopRowBuild() {
        return topBuildingCards;
    }

    /**
     * Returns the bottom row of building cards.
     *
     * @return list of building cards in the bottom row
     */
    public List<BuildingCard> getLowRowBuild() {
        return bottomBuildingCards;
    }

    /**
     * Returns all offer-field spaces (active spaces for totem placement).
     *
     * @return list of board spaces in the offer field
     */
    public List<BoardSpace> getOfferField() {
        return offerField;
    }

    /**
     * Places a totem on the given board space.
     *
     * @param totem the totem to place
     * @param space the target board space
     */
    public void placeTotem(Totem totem, BoardSpace space) {
        space.setTotem(totem);
        totem.place(space);
    }

    /**
     * Adds a tribe card to the bottom row (used during first-turn setup).
     *
     * @param card the tribe card to add
     */
    public void addBottomTribeCardsFirstTurn(TribeCard card) {
        this.bottomTribeCards.add(card);
    }

    /**
     * Adds a tribe card to the top row.
     *
     * @param card the tribe card to add
     */
    public void addTopTribeCards(TribeCard card) {
        this.topTribeCards.add(card);
    }

    /**
     * Replaces the top building card row with cards belonging to the given age.
     *
     * @param cards the full list of building cards to filter
     * @param ERA   the age whose cards should be placed in the top row
     */
    public void setTopBuildingCards(List<BuildingCard> cards, Age ERA) {
        List<BuildingCard> filtered = new ArrayList<>();
        for (BuildingCard card : cards) {
            if (card.getAge() == ERA) {
                filtered.add(card);
            }
        }
        this.topBuildingCards = filtered;
    }

    /**
     * Returns the board space identified by the given letter, or {@code null} if not found.
     *
     * @param letter the letter identifier of the space
     * @return the matching {@link BoardSpace}, or {@code null}
     */
    public BoardSpace getBoardSpace(char letter){
        for (BoardSpace space : offerField) {
            if (space.getLetter() == letter) {
                return space;
            }
        }
        return null;
    }

    /**
     * Returns all offer-field spaces that are currently unoccupied.
     *
     * @return list of free board spaces
     */
    public List<BoardSpace> getFreeBoardSpaces(){
        List<BoardSpace> freeSpaces = new ArrayList<>();
        for (BoardSpace space : offerField) {
            if (space.isFree()) {
                freeSpaces.add(space);
            }
        }
        return freeSpaces;
    }

    /**
     * Returns the players ordered by their totem position on the offer field.
     *
     * @param players all players in the game
     * @return players sorted by offer-field placement order
     */
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

    /**
     * Returns tribe cards in the top row that have not yet been drawn.
     *
     * @return list of available top-row tribe cards
     */
    public List<TribeCard> getAvailableUpperTribeCards(){
        List<TribeCard> availableTribeUp = new ArrayList<>();
        for (TribeCard card : topTribeCards) {
            if (!card.isDrawed()) {
                availableTribeUp.add(card);
            }
        }
        return availableTribeUp;
    }

    /**
     * Returns tribe cards in the bottom row that have not yet been drawn.
     *
     * @return list of available bottom-row tribe cards
     */
    public List<TribeCard> getAvailableBottomTribeCards(){
        List<TribeCard> availableTribeBo = new ArrayList<>();
        for (TribeCard card : bottomTribeCards) {
            if (!card.isDrawed()) {
                availableTribeBo.add(card);
            }
        }
        return availableTribeBo;
    }

    /**
     * Returns building cards in the top row that have not yet been drawn.
     *
     * @return list of available top-row building cards
     */
    public List<BuildingCard> getAvailableUpperBuildingCards() {
        List<BuildingCard> availableBuildUp = new ArrayList<>();
        for (BuildingCard card : topBuildingCards ) {
            if (!card.isDrawed()) {
                availableBuildUp.add(card);
            }
        }
        return availableBuildUp;
    }

    /**
     * Returns building cards in the bottom row that have not yet been drawn.
     *
     * @return list of available bottom-row building cards
     */
    public List<BuildingCard> getAvailableBottomBuildingCards(){
        List<BuildingCard> availableBuildBo = new ArrayList<>();
        for (BuildingCard card : bottomBuildingCards ) {
            if (!card.isDrawed()) {
                availableBuildBo.add(card);
            }
        }
        return availableBuildBo;
    }

    /**
     * Removes the given card from whichever row it currently belongs to.
     *
     * @param card the card to remove
     */
    public void removeCard(Card card) {
        if (topTribeCards.remove(card)) return;
        if (bottomTribeCards.remove(card)) return;
        if (topBuildingCards.remove(card)) return;
        bottomBuildingCards.remove(card);
    }

    /**
     * Shifts the tribe card rows: the current top row becomes the new bottom row and the top row is cleared.
     */
    public void shiftRows() {

        bottomTribeCards.clear();
        bottomTribeCards.addAll(topTribeCards);
        topTribeCards.clear();

    }

    /**
     * Shifts the building card rows: the current top row becomes the new bottom row and the top row is cleared.
     */
    public void shiftRowsBuildings() {

        bottomBuildingCards.clear();
        bottomBuildingCards.addAll(topBuildingCards);
        topBuildingCards.clear();

    }

    /**
     * Overwrites all four card rows — used only by {@code PersistenceManager} during game restoration.
     *
     * @param topTribe    tribe cards to restore in the top row
     * @param bottomTribe tribe cards to restore in the bottom row
     * @param topBuild    building cards to restore in the top row
     * @param bottomBuild building cards to restore in the bottom row
     */
    public void restoreCardRows(List<TribeCard> topTribe, List<TribeCard> bottomTribe,
                                List<BuildingCard> topBuild, List<BuildingCard> bottomBuild) {
        this.topTribeCards    = new ArrayList<>(topTribe);
        this.bottomTribeCards = new ArrayList<>(bottomTribe);
        this.topBuildingCards = new ArrayList<>(topBuild);
        this.bottomBuildingCards = new ArrayList<>(bottomBuild);
    }

    /**
     * Removes all totems from every offer-field space.
     */
    public void clearBoardSpaces(){
        for (BoardSpace space : offerField) {
            space.removeTotem();
        }
    }

    /**
     * Returns the event cards currently in the bottom tribe row, preserving their order.
     *
     * @return list of event cards in the bottom row
     */
    public List<EventCard> getLowRowEvents() {

        List<EventCard> lowRowEvents = new ArrayList<>();

        for (TribeCard card : bottomTribeCards) { // preserving card order
            if (card.isEvent()) {
                lowRowEvents.add((EventCard) card);
            }
        }

        return lowRowEvents;
    }

    /**
     * Returns the event cards currently in the top tribe row.
     *
     * @return list of event cards in the top row
     */
    public List<EventCard> getUpRowEvents() {

        List<EventCard> upRowEvents = new ArrayList<>();

        for (TribeCard card : topTribeCards) {
            if (card.isEvent()) {
                upRowEvents.add((EventCard) card);
            }
        }

        return upRowEvents;
    }

    /**
     * Returns a string representation of the board showing all card rows and offer spaces.
     *
     * @return string describing the board state
     */
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
