package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.board.BoardSpace;
import it.polimi.ingsw.model.board.TurnOrder;
import it.polimi.ingsw.model.cards.*;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.EventType;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;

import java.util.*;

public class Game implements GameActions {
    private final int gameID;

    private final List<Player> players;
    private Player playerInTurn;
    private final TurnOrder turnOrder;
    private int numberOfPlayers;

    private GameState gameState;
    private Age currentAge;

    private final Deck mainDeck;
    private final Deck deck1;
    private final Deck deck2;
    private final Deck deck3;

    private final Board gameBoard;

    public Game(int gameID, Board gameBoard, TurnOrder turn, Deck mainDeck, Deck deck1, Deck deck2, Deck deck3) {
        this.gameID = gameID;
        this.players = new ArrayList<>();
        this.playerInTurn = null;
        this.numberOfPlayers = 0;
        this.gameState = GameState.LOGIN;
        this.currentAge = null;
        this.gameBoard = gameBoard;
        this.turnOrder = turn;
        this.mainDeck = mainDeck;
        this.deck1 = deck1;
        this.deck2 = deck2;
        this.deck3 = deck3;
    }

    public int getGameID() {
        return gameID;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public TurnOrder getTurnOrder() {
        return turnOrder;
    }

    public Player getCurrentPlayer() {
        return playerInTurn;
    }

    public int getNumberOfPlayers() {
        return numberOfPlayers;
    }

    public GameState getStatus() {
        return gameState;
    }

    public Age getCurrentAge() {
        return currentAge;
    }

    public Board getBoard() {
        return gameBoard;
    }

    public Deck getMainDeck() {
        return mainDeck;
    }

    public Deck getDeck1() {
        return deck1;
    }

    public Deck getDeck2() {
        return deck2;
    }

    public Deck getDeck3() {
        return deck3;
    }


    public void addPlayer(Player player) {
        if (gameState != GameState.LOGIN) {
            throw new IllegalStateException("Cannot add players after game has started");
        }
        if (players.stream().anyMatch(p -> p.getNickname().equals(player.getNickname()))) {
            throw new IllegalArgumentException("Nickname already taken: " + player.getNickname());
        }
        players.add(player);
        numberOfPlayers++;
    }

    public void setUpFirstRound() {
        //TODO
    }

    public void placeTotemOnOfferSpace(Player player, BoardSpace boardSpace) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(boardSpace, "boardSpace cannot be null");

        if (gameState != GameState.OFFER_SPACE_CHOOSE) {
            throw new IllegalStateException("Cannot place totem: wrong game state");
        }
        if (!boardSpace.isFree()) {
            throw new IllegalStateException("BoardSpace " + boardSpace.getLetter() + " is already occupied");
        }

        gameBoard.placeTotem(player.getTotem(), boardSpace);

        // se tutti i giocatori hanno piazzato, avanza alla risoluzione
        boolean allPlaced = players.stream()
                .allMatch(p -> p.getTotem().getPosition() != null);
        if (allPlaced) {
            gameState = GameState.RESOLVING;
            // il primo a risolvere è quello più a sinistra (lettera minore)
            playerInTurn = gameBoard.getFreeBoardSpaces().isEmpty()
                    ? turnOrder.getOrder().get(0)
                    : findLeftmostPlayer();
        }
    }

    private Player findLeftmostPlayer() {
        // gli spazi sono ordinati per lettera nel tracciato offerte
        for (BoardSpace space : gameBoard.getOfferField()) {
            if (!space.isFree()) {
                Totem t = space.getTotem();
                for (Player p : players) {
                    if (p.getTotem() == t) return p;
                }
            }
        }
        return players.get(0);
    }

    public void startGame() { //forse meglio qui quella che da il via? non so discutiamone
        if (players.isEmpty()) {
            throw new IllegalStateException("Cannot start a game without players");
        }

        gameState = GameState.START;
        currentAge = Age.Era_I;
        setUpFirstRound();
        gameState = GameState.START;
    }

    public void pickTribeCard(Player player, TribeCard card, boolean fromTopRow) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(card, "card cannot be null");

        if (card instanceof EventCard) {
            throw new IllegalArgumentException("Event cards cannot be picked by players");
        }

        // verifica che la carta sia disponibile nella riga richiesta
        List<TribeCard> available = fromTopRow
                ? gameBoard.getAvailableUpperTribeCards()
                : gameBoard.getAvailableBottomTribeCards();

        if (!available.contains(card)) {
            throw new IllegalArgumentException("Card not available in the specified row");
        }

        gameBoard.removeCard(card);
        player.addCharacterCard((CharacterCard) card);
    }

    public void pickBuildingCard(Player player, BuildingCard card, boolean fromTopRow) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(card, "card cannot be null");

        int actualCost = Math.max(0, card.getFoodCost() - player.getTotalFoodDiscount());

        if (player.getFood() < actualCost) {
            throw new IllegalStateException("Not enough food to pick building card");
        }

        // verifica che la carta sia disponibile nella riga richiesta
        List<BuildingCard> available = fromTopRow
                ? gameBoard.getAvailableUpperBuildingCards()
                : gameBoard.getAvailableBottomBuildingCards();

        if (!available.contains(card)) {
            throw new IllegalArgumentException("Building card not available in the specified row");
        }

        player.removeFood(actualCost);
        gameBoard.removeCard(card);
        player.addBuildingCard(card);

        // effetto immediato ON_ACQUIRE
        card.applyEffect(this, player, null);
    }

    public void returnTotemToTurnOrder(Player player) {
        Objects.requireNonNull(player, "player cannot be null");

        turnOrder.placeTotemFirstFree(player);

        // se tutti sono tornati sulla tessera ordine, avanza agli eventi
        boolean allReturned = players.stream()
                .allMatch(p -> p.getTotem().getPosition() == null);
        if (allReturned) {
            gameState = GameState.EVENTS;
        }
    }

    public void resolveLowerEvents() {
        // TODO: completare però potrebbe essere un buon inizio
        List<EventCard> events = gameBoard.getLowRowEvents();

        for (EventCard event : events) {
            event.resolve(players);
            for (Player player : players) {
                for (BuildingCard building : player.getBuildingCards()) {
                    building.applyEffect(this, player, event);
                }
            }
        }
    }

    public void updateAge() {
        //TODO
    }

    public void TotemPositionEndRound() {
        // TODO: salvare pos finale totem
    }

    public void nextTurn() {
        // refresh tabellone
        gameBoard.shiftRows();
        gameBoard.clearBoardSpaces();
        turnOrder.clearAll();

        // controlla cambio era
        updateAge();

        // ripesca (numPlayers + 4) carte per la nuova fila superiore
        List<TribeCard> newTopRow = mainDeck.draw(numberOfPlayers + 4);
        gameBoard.setTopTribeCards(newTopRow);

        // aggiorna stato e ordine turno per il prossimo round
        gameState = GameState.OFFER_SPACE_CHOOSE;
        playerInTurn = turnOrder.getOrder().get(0);
    }

    public void endGame() {
        gameState = GameState.END;

        // nell'ultimo round si risolvono anche gli eventi in fila superiore
        List<EventCard> finalEvents = new ArrayList<>(gameBoard.getLowRowEvents());
        for (TribeCard card : gameBoard.getTopRowTribe()) {
            if (card instanceof EventCard) {
                finalEvents.add((EventCard) card);
            }
        }

        // Sostentamento sempre per ultimo
        finalEvents.sort(Comparator.comparingInt(e ->
                e.getType() == EventType.SUSTENANCE ? 1 : 0));

        for (EventCard event : finalEvents) {
            event.resolve(players);
        }

        // punteggi finali
        for (Player p : players) {
            p.getTotalPoints();
        }
    }

    public List<Player> getWinners() {
        int maxScore = players.stream()
                .mapToInt(Player::getPrestige)
                .max()
                .orElse(0);

        List<Player> winners = players.stream()
                .filter(p -> p.getPrestige() == maxScore)
                .toList();

        // tiebreaker: più cibo
        if (winners.size() > 1) {
            int maxFood = winners.stream()
                    .mapToInt(Player::getFood)
                    .max()
                    .orElse(0);
            winners = winners.stream()
                    .filter(p -> p.getFood() == maxFood)
                    .toList();
        }

        return winners;
    }
}



