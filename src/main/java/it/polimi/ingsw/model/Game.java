package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.board.BoardSpace;
import it.polimi.ingsw.model.board.TurnOrder;
import it.polimi.ingsw.model.cards.*;
import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.EventType;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.model.player.Player;

import java.util.*;

public class Game implements GameActions {
    private final int gameID;

    private final List<Player> players;
    private Player playerInTurn;
    private final TurnOrder turnOrder;
    private List<Player> currentRoundOrder;

    private int numberOfPlayers;

    private GameState gameState;
    private Age currentAge;

    private final Deck mainDeck;

    private List<TribeCard> deck_ERA_I ;
    private List<TribeCard> deck_ERA_II;
    private List<TribeCard> deck_ERA_III;
    private List<EventCard> finalEvents;

    private List<BuildingCard> buldingsInGame;

    private List<Player> extraCardPlayers; // per tenere traccia giocatore (possibili altri giocatori con espansioni di carte) con carta building extra card
    private int extraCardPlayerIndex;

    private final Board gameBoard;

    private int topPicks = 0;
    private int bottomPicks = 0;

    public Game(int gameID, Board gameBoard, TurnOrder turn) {
        this.gameID = gameID;
        this.players = new ArrayList<>();
        this.playerInTurn = null;
        this.numberOfPlayers = 0;
        this.gameState = GameState.LOGIN;
        this.currentAge = null;
        this.gameBoard = gameBoard;
        this.turnOrder = turn;

        this.mainDeck = new Deck();

        this.deck_ERA_I = new ArrayList<>();
        this.deck_ERA_II = new ArrayList<>();
        this.deck_ERA_III = new ArrayList<>();

        this.extraCardPlayers = new ArrayList<>();
        this.extraCardPlayerIndex = 0;


        this.buldingsInGame = new ArrayList<>();
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

    public List<TribeCard> getDeck1() {
        return deck_ERA_I;
    }

    public List<TribeCard> getDeck2() {
        return deck_ERA_II;
    }

    public List<TribeCard> getDeck3() {
        return deck_ERA_III;
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
        if (gameState != GameState.START) {
            throw new IllegalStateException("Cannot set up first round after game has started");
        }



        List<Player> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);

        currentRoundOrder = shuffled;

        for (Player player : shuffled) {
            turnOrder.placeTotemFirstFree(player);
        }

        for (int i = 0; i < shuffled.size(); i++) {
            int food = switch (i) {
                case 0 -> 2;
                case 1, 2 -> 3;
                default -> 4;
            };
            shuffled.get(i).addFood(food);
        }


        //setup buildings
        buldingsInGame = mainDeck.takeBuldingInGame(numberOfPlayers);
        gameBoard.setTopBuildingCards(buldingsInGame, Age.Era_I);
        //TODO: LOGICA DI UPDATE ERA E SOPSOTAMENTO shiftbuilding

        //setup tribeCard
        deck_ERA_I = mainDeck.prepareTribeCards(numberOfPlayers, Age.Era_I);
        deck_ERA_II = mainDeck.prepareTribeCards(numberOfPlayers, Age.Era_II);
        deck_ERA_III = mainDeck.prepareTribeCards(numberOfPlayers, Age.Era_II);

        //setup board first turn
        //TODO: CHECK

        //bottomCards
        int nBottomCards = numberOfPlayers +1;
        TribeCard temp = null;
        while (nBottomCards >0) {
            int i=1;
            for (i=1; i <= deck_ERA_I.size(); i++) {
                if (deck_ERA_I.get(deck_ERA_I.size() - i) instanceof EventCard) {

                } else if(deck_ERA_I.get(deck_ERA_I.size()-i) instanceof CharacterCard) {
                    temp = deck_ERA_I.get(deck_ERA_I.size() - i);
                    break;
                }
            }
            deck_ERA_I.remove(deck_ERA_I.size() - i);
            gameBoard.addBottomTribeCardsFirstTurn(temp);
            nBottomCards--;
        }

        //topcards
        int nTopCards = numberOfPlayers + 4;
        TribeCard temptop = null;
        while (nTopCards >0) {
            temptop = deck_ERA_I.get(deck_ERA_I.size() - 1);
            gameBoard.addTopTribeCards(temptop);
            deck_ERA_I.remove(deck_ERA_I.size() - 1);
            nTopCards--;
        }



        gameState = GameState.OFFER_SPACE_CHOOSE;
        playerInTurn = currentRoundOrder.get(0);

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
            gameState = GameState.PICKING_CARD;
            currentRoundOrder = gameBoard.getPlayerInOfferOrder(players);
            playerInTurn = currentRoundOrder.get(0);
        }
    }

    public void advanceNextPlayer() {
        topPicks = 0;
        bottomPicks = 0;

        int currentPlayerIndex = currentRoundOrder.indexOf(playerInTurn);

        if (playerInTurn != null) {
            playerInTurn.setInTurn(false);
        }

        if (currentPlayerIndex < currentRoundOrder.size() - 1) {
            playerInTurn = currentRoundOrder.get(currentPlayerIndex + 1);
            playerInTurn.setInTurn(true);
        } else {
            extraCardPlayers = getPlayersWithExtraCard(); // controllo per possessore building extra card
            extraCardPlayerIndex = 0;

            if (!extraCardPlayers.isEmpty()) {
                gameState = GameState.EXTRA_CARD;
                playerInTurn = extraCardPlayers.get(0);
                playerInTurn.setInTurn(true);
            } else {
                playerInTurn = null;
                gameState = GameState.EVENTS;
            }
        }
    }

    public List<Player> getPlayersWithExtraCard() {
        List<Player> playersWithExtraCard = new ArrayList<>();

        // uso l'ordine di turno, perché la carta si attiva quando tutti
        // i totem sono tornati sulla tessera ordine di turno
        for (Player player : turnOrder.getOrder()) {
            if (player.hasExtraCard()) {
                playersWithExtraCard.add(player);
            }
        }

        return playersWithExtraCard;
    }


    public void startGame() { //forse meglio qui quella che da il via? non so discutiamone
        if (gameState != GameState.LOGIN) {
            throw new IllegalStateException("Cannot start game after game has started");
        }
        if (players.isEmpty()) {
            throw new IllegalStateException("Cannot start a game without players");
        }

        gameState = GameState.START;
        currentAge = Age.Era_I;
        setUpFirstRound();
    }

    //HO PROVATO A FARLA MA LASCIO UN PICCLO TODO PERCHÉ NON É SEMPLICISSIMA ANCHE SE PENSO DI ESSERCI
    public int getRemainingTopPicks(Player player) {
        BoardSpace space = player.getTotem().getPosition();
        return space.getTopCardsNumber() - topPicks;
    }

    public int getRemainingBottomPicks(Player player) {
        BoardSpace space = player.getTotem().getPosition();
        return space.getBottomCardsNumber() - bottomPicks;
    }

    public void pickCard(Player player, Card card) {

        if(gameState != GameState.PICKING_CARD) {
            throw new IllegalStateException("Cannot pick card after PICKING_CARD has started");
        }

        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(card, "card cannot be null");

        BoardSpace boardSpace = player.getTotem().getPosition();
        // gestione spazio A (solo partite a 5 giocatori)
        if (numberOfPlayers == 5 && boardSpace.getLetter() == 'A') {
            player.addFood(3);
            returnTotemToTurnOrder(player);
            advanceNextPlayer();
            return;
        }

        boolean fromTopRow = gameBoard.getAvailableUpperTribeCards().contains(card) ||
                gameBoard.getAvailableUpperBuildingCards().contains(card);

        boolean fromBottomRow = gameBoard.getAvailableBottomTribeCards().contains(card) ||
                gameBoard.getAvailableBottomBuildingCards().contains(card);

        if (!fromTopRow || getRemainingTopPicks(player) <= 0) {
            throw new IllegalStateException("No more top row picks allowed");
        }
        if (!fromBottomRow || getRemainingBottomPicks(player) <= 0) {
            throw new IllegalStateException("No more bottom row picks allowed");
        }

        if (card instanceof BuildingCard) {
            pickBuildingCard(player, (BuildingCard) card);
        } else if (card instanceof CharacterCard) {
            pickTribeCard(player, (TribeCard) card);
        }

        if (fromTopRow) topPicks++;
        else bottomPicks++;

        if (getRemainingTopPicks(player) == 0 && getRemainingBottomPicks(player) == 0) {
            returnTotemToTurnOrder(player);
            advanceNextPlayer();
        }
    }

    public void pickTribeCard(Player player, TribeCard card) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(card, "card cannot be null");

        if (card instanceof EventCard) {
            throw new IllegalArgumentException("Event cards cannot be picked by players");
        }

        player.addCharacterCard((CharacterCard) card);
        gameBoard.removeCard(card);
    }

    public void pickBuildingCard(Player player, BuildingCard card) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(card, "card cannot be null");

        int actualCost = Math.max(0, card.getFoodCost() - player.getBuildingFoodDiscount());

        if (player.getFood() < actualCost) {
            throw new IllegalStateException("Not enough food to pick building card");
        }

        player.removeFood(actualCost);
        gameBoard.removeCard(card);
        player.addBuildingCard(card);

    }

    public void pickExtraCard(Player player, Card card) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(card, "card cannot be null");

        if (gameState != GameState.EXTRA_CARD) { // inizia solo dopo che in advanceNextPlayer tutti hanno fnito di pescare
                                                 // ed i totem tornano nella tabella posizioni
            throw new IllegalStateException("Cannot pick extra card outside EXTRA_CARD phase");
        }

        if (player != playerInTurn) {
            throw new IllegalStateException("It is not this player's extra-card turn");
        }

        boolean fromTopRow = gameBoard.getAvailableUpperTribeCards().contains(card) ||
                gameBoard.getAvailableUpperBuildingCards().contains(card);

        if (!fromTopRow) {
            throw new IllegalArgumentException("Extra card can be picked only from top row");
        }

        if (card instanceof BuildingCard) {
            pickBuildingCard(player, (BuildingCard) card);
        } else if (card instanceof CharacterCard) {
            pickTribeCard(player, (TribeCard) card);
        } else {
            throw new IllegalArgumentException("Event cards cannot be picked");
        }

        advanceExtraCardPlayer();
    }

    private void advanceExtraCardPlayer() { // passo all'ipotetico altro possessore della building di questo tipo
        if (playerInTurn != null) {
            playerInTurn.setInTurn(false);
        }

        extraCardPlayerIndex++;

        if (extraCardPlayerIndex < extraCardPlayers.size()) {
            playerInTurn = extraCardPlayers.get(extraCardPlayerIndex);
            playerInTurn.setInTurn(true);
        } else {
            playerInTurn = null;
            gameState = GameState.EVENTS;
        }
    }


    public void returnTotemToTurnOrder(Player player) {
        Objects.requireNonNull(player, "player cannot be null");

        turnOrder.placeTotemFirstFree(player);
        /*
        // se tutti sono tornati sulla tessera ordine, avanza agli eventi
        boolean allReturned = players.stream()
                .allMatch(p -> p.getTotem().getPosition() == null);
        if (allReturned) {
            gameState = GameState.EVENTS;
        } */
    }

    public void resolveLowerEvents() {

        if (gameState != GameState.EVENTS) {
            throw new IllegalStateException("Cannot resolve lower events after EVENTS has started");
        }
        // TODO: completare però potrebbe essere un buon inizio
        List<EventCard> events = gameBoard.getLowRowEvents();

        for (EventCard event : events) {
            event.resolve(players);
        }

        //eventi risolti ok e come passo a "next round"?
    }

    public void updateAge() {
        //TODO
    }

    public void nextRound() {
        topPicks = 0;
        bottomPicks = 0;
        // refresh tabellone
        gameBoard.shiftRows();
        gameBoard.clearBoardSpaces();


        //TODO: CON NUOVO DECK

        //pseudo:
        //pescare carte da lista dell'era corrente (deck_ERA_I, ecc...) e metterle in topTribe del board
        //se la lista dell'era corrente finisce chiamare update era (da implementare)
        //continuare eventuale pescaggio da successivo deck
        //se finito anche deck_ERA_III allora pescare le 2 carte evento finale (dopo aver risolto quel turno mettere gamestate in END)
        //

        /* old version: (con draw)
        // ripesca (numPlayers + 4) carte per la nuova fila superiore
        List<TribeCard> newTopRow = mainDeck.draw(numberOfPlayers + 4);
        gameBoard.setTopTribeCards(newTopRow);

        // controlla cambio era
        updateAge();
        */



        // aggiorna stato e ordine turno per il prossimo round
        gameState = GameState.OFFER_SPACE_CHOOSE;
        playerInTurn = turnOrder.getOrder().get(0);
    }

    public void endGame() {

        if (gameState != GameState.END) {
            throw new IllegalStateException("Cannot end game after END has started");
        }


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



