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

    private List<GameObserver> observers = new ArrayList<>();

    private final List<Player> players;
    private Player playerInTurn;
    private TurnOrder turnOrder;
    private List<Player> currentRoundOrder;

    private int numberOfPlayers;

    private GameState gameState;
    private Age currentAge;

    private final Deck mainDeck;

    private List<TribeCard> deck_ERA_I;
    private List<TribeCard> deck_ERA_II;
    private List<TribeCard> deck_ERA_III;
    private List<EventCard> finalEvents;

    private List<BuildingCard> buldingsInGame;

    private final Board gameBoard;

    // variabili d'appoggio per pescare carte
    private int topPicks = 0;
    private int bottomPicks = 0;

    public Game(int gameID) {
        this.gameID = gameID;
        this.players = new ArrayList<>();
        this.playerInTurn = null;
        this.numberOfPlayers = 0;
        this.gameState = GameState.LOGIN;
        this.currentAge = Age.Era_I;

        this.gameBoard = new Board();
        this.turnOrder = null;

        this.mainDeck = new Deck();

        this.deck_ERA_I = new ArrayList<>();
        this.deck_ERA_II = new ArrayList<>();
        this.deck_ERA_III = new ArrayList<>();

        this.buldingsInGame = new ArrayList<>();

        this.finalEvents = mainDeck.getFinalsEvents();
    }

    public void addObserver(GameObserver observer) {
        this.observers.add(observer);
    }

    public int getGameID()             { return gameID; }
    public List<Player> getPlayers()   { return players; }
    public TurnOrder getTurnOrder()    { return turnOrder; }
    public Player getCurrentPlayer()   { return playerInTurn; }
    public int getNumberOfPlayers()    { return numberOfPlayers; }
    public GameState getStatus()       { return gameState; }
    public Age getCurrentAge()         { return currentAge; }
    public Board getBoard()            { return gameBoard; }
    public Deck getMainDeck()          { return mainDeck; }

    // ────────────────────────────────────────────────
    //  LOBBY
    // ────────────────────────────────────────────────

    @Override
    public void addPlayer(Player player) {
        if (gameState != GameState.LOGIN) {
            throw new IllegalStateException("Cannot add players after game has started");
        }
        if (players.stream().anyMatch(p -> p.getNickname().equals(player.getNickname()))) {
            for (GameObserver obs : observers) {
                obs.onPlayerError("Nickname already taken: " + player.getNickname());
            }
            throw new IllegalArgumentException("Nickname already taken");
        }
        players.add(player);
        numberOfPlayers++;
        for (GameObserver obs : observers) {
            obs.onPlayerJoined(player.getNickname());
        }
    }

    // ────────────────────────────────────────────────
    //  SETUP
    // ────────────────────────────────────────────────

    @Override
    public void startGame() {
        if (gameState != GameState.LOGIN) {
            throw new IllegalStateException("Cannot start game after game has started");
        }
        if (players.isEmpty() || players.size() < 2 || players.size() > 5) {
            throw new IllegalStateException("Cannot start a game");
        }

        setUpGameCards();
        setUpFirstRound();

        for (GameObserver obs : observers) {
            obs.onGameStarted();
            obs.onBoardUpdated();
        }

        // Notifica il primo giocatore di turno
        notifyCurrentPlayerTurn();
    }

    private void setUpGameCards() {
        gameBoard.prepareGameBoardSpace(numberOfPlayers);
        turnOrder = new TurnOrder(numberOfPlayers);

        buldingsInGame = mainDeck.takeBuldingInGame(numberOfPlayers);
        gameBoard.setTopBuildingCards(buldingsInGame, Age.Era_I);

        deck_ERA_I   = mainDeck.prepareTribeCards(numberOfPlayers, Age.Era_I);
        deck_ERA_II  = mainDeck.prepareTribeCards(numberOfPlayers, Age.Era_II);
        deck_ERA_III = mainDeck.prepareTribeCards(numberOfPlayers, Age.Era_III);
    }

    private void setUpFirstRound() {
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

        // bottom cards
        int nBottomCards = numberOfPlayers + 1;
        TribeCard temp = null;
        while (nBottomCards > 0) {
            int i = 1;
            for (i = 1; i <= deck_ERA_I.size(); i++) {
                if (deck_ERA_I.get(deck_ERA_I.size() - i).isCharacter()) {
                    temp = deck_ERA_I.get(deck_ERA_I.size() - i);
                    break;
                }
            }
            deck_ERA_I.remove(deck_ERA_I.size() - i);
            gameBoard.addBottomTribeCardsFirstTurn(temp);
            nBottomCards--;
        }

        // top cards
        int nTopCards = numberOfPlayers + 4;
        while (nTopCards > 0) {
            TribeCard temptop = deck_ERA_I.get(deck_ERA_I.size() - 1);
            gameBoard.addTopTribeCards(temptop);
            deck_ERA_I.remove(deck_ERA_I.size() - 1);
            nTopCards--;
        }

        gameState = GameState.OFFER_SPACE_CHOOSE;
        playerInTurn = currentRoundOrder.get(0);
        playerInTurn.setInTurn(true);
    }

    // ────────────────────────────────────────────────
    //  FASE 1: PIAZZAMENTO TOTEM
    // ────────────────────────────────────────────────

    @Override
    public void placeTotemOnOfferSpace(Player player, BoardSpace boardSpace) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(boardSpace, "boardSpace cannot be null");

        if (gameState != GameState.OFFER_SPACE_CHOOSE) {
            throw new IllegalStateException("Cannot place totem: wrong game state");
        }
        if (!boardSpace.isFree()) {
            for (GameObserver obs : observers) {
                obs.onInvalidAction(player.getNickname(), "Space already occupied");
            }
            throw new IllegalStateException("BoardSpace " + boardSpace.getLetter() + " is already occupied");
        }

        turnOrder.clearBlock(player.getTotem());
        gameBoard.placeTotem(player.getTotem(), boardSpace);

        for (GameObserver obs : observers) {
            obs.onTotemPlaced(player.getNickname(), String.valueOf(boardSpace.getLetter()));
        }

        advanceNextPlayer();

        boolean allPlaced = players.stream()
                .allMatch(p -> p.getTotem().getPosition() != null);

        if (allPlaced) {
            gameState = GameState.PICKING_CARD;
            currentRoundOrder = gameBoard.getPlayerInOfferOrder(players);
            playerInTurn = currentRoundOrder.get(0);
            playerInTurn.setInTurn(true);

            // gestione spazio A (solo partite a 5 giocatori)
            if (numberOfPlayers == 5) {
                BoardSpace firstBoardSpace = playerInTurn.getTotem().getPosition();
                if (firstBoardSpace.getLetter() == 'A') {
                    playerInTurn.addFood(3);
                    for (GameObserver obs : observers) obs.onPlayerUpdated(playerInTurn.getNickname());
                    returnTotemToTurnOrder(playerInTurn);
                    advanceNextPlayer();
                }
            }
        }

        // Notifica il prossimo giocatore (che sia ancora in OFFER_SPACE_CHOOSE o già in PICKING_CARD)
        if (playerInTurn != null) {
            notifyCurrentPlayerTurn();
        }
    }

    // ────────────────────────────────────────────────
    //  FASE 2: SELEZIONE CARTE
    // ────────────────────────────────────────────────

    public int getRemainingTopPicks(Player player) {
        BoardSpace space = player.getTotem().getPosition();
        return space.getTopCardsNumber() - topPicks;
    }

    public int getRemainingBottomPicks(Player player) {
        BoardSpace space = player.getTotem().getPosition();
        return space.getBottomCardsNumber() - bottomPicks;
    }

    public void pickCard(Player player, Card card) {
        if (gameState != GameState.PICKING_CARD) {
            throw new IllegalStateException("Cannot pick card: wrong game state");
        }
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(card, "card cannot be null");

        boolean fromTopRow = gameBoard.getAvailableUpperTribeCards().contains(card) ||
                gameBoard.getAvailableUpperBuildingCards().contains(card);
        boolean fromBottomRow = gameBoard.getAvailableBottomTribeCards().contains(card) ||
                gameBoard.getAvailableBottomBuildingCards().contains(card);

        if (fromTopRow && getRemainingTopPicks(player) <= 0) {
            for (GameObserver obs : observers) {
                obs.onInvalidAction(player.getNickname(), "No more top row picks allowed");
            }
            throw new IllegalStateException("No more top row picks allowed");
        }
        if (fromBottomRow && getRemainingBottomPicks(player) <= 0) {
            for (GameObserver obs : observers) {
                obs.onInvalidAction(player.getNickname(), "No more bottom row picks allowed");
            }
            throw new IllegalStateException("No more bottom row picks allowed");
        }

        card.pick(player, this);

        if (fromTopRow) topPicks++;
        else bottomPicks++;

        // Se il giocatore ha esaurito tutte le pescate, passa al prossimo
        if (getRemainingTopPicks(player) == 0 && getRemainingBottomPicks(player) == 0) {
            returnTotemToTurnOrder(player);
            advanceNextPlayer();

            // Se siamo passati a EVENTS, risolvi automaticamente
            if (gameState == GameState.EVENTS) {
                resolveEvents();
                return;
            }

            // Altrimenti notifica il prossimo giocatore
            if (playerInTurn != null) {
                notifyCurrentPlayerTurn();
            }
        } else {
            // Il giocatore ha ancora pescate da fare: ri-notificalo
            notifyCurrentPlayerTurn();
        }
    }

    public void pickCharacterCard(Player player, CharacterCard card) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(card, "card cannot be null");

        player.addCharacterCard(card);
        gameBoard.removeCard(card);

        for (GameObserver obs : observers) {
            obs.onCardTaken(player.getNickname(), card.toString());
            obs.onPlayerUpdated(player.getNickname());
        }
    }

    public void pickBuildingCard(Player player, BuildingCard card) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(card, "card cannot be null");

        int actualCost = Math.max(0, card.getFoodCost() - player.foodDiscountToBuyBuildings());

        if (player.getFood() < actualCost) {
            for (GameObserver obs : observers) {
                obs.onInvalidAction(player.getNickname(), "Not enough food to buy building card");
            }
            throw new IllegalStateException("Not enough food to pick building card");
        }

        player.removeFood(actualCost);
        gameBoard.removeCard(card);
        player.addBuildingCard(card);

        for (GameObserver obs : observers) {
            obs.onCardTaken(player.getNickname(), card.toString());
            obs.onPlayerUpdated(player.getNickname());
        }
    }

    @Override
    public void returnTotemToTurnOrder(Player player) {
        Objects.requireNonNull(player, "player cannot be null");

        turnOrder.placeTotemFirstFree(player);

        List<String> orderedNicks = turnOrder.getOrder(players).stream()
                .map(Player::getNickname).toList();
        for (GameObserver obs : observers) {
            obs.onTurnOrderUpdated(orderedNicks);
            obs.onPlayerUpdated(player.getNickname());
        }
    }

    // ────────────────────────────────────────────────
    //  AVANZAMENTO TURNO (privato)
    // ────────────────────────────────────────────────

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
            playerInTurn = null;
            gameState = GameState.EVENTS;
        }
    }

    /**
     * Notifica via observer il giocatore di turno attuale,
     * comunicando la fase corrente.
     * Il GameController la tradurrà in onYourTurn sul client corretto.
     */
    private void notifyCurrentPlayerTurn() {
        if (playerInTurn == null) return;
        for (GameObserver obs : observers) {
            obs.onTurnStarted(playerInTurn.getNickname(), gameState);
        }
    }

    // ────────────────────────────────────────────────
    //  FINE ROUND: EVENTI (chiamato automaticamente)
    // ────────────────────────────────────────────────

    @Override
    public void resolveEvents() {
        if (gameState != GameState.EVENTS) {
            throw new IllegalStateException("Cannot resolve events: wrong game state");
        }

        List<EventCard> events = gameBoard.getLowRowEvents();

        // Sustenance sempre per ultimo — regola esplicita
        events.sort(Comparator.comparingInt(e ->
                e.getType() == EventType.SUSTENANCE ? 1 : 0));

        for (EventCard event : events) {
            event.resolve(players);
            for (GameObserver obs : observers) {
                obs.onEventResolved(event.getType().toString(), "Resolved");
            }
        }
        for (Player p : players) {
            for (GameObserver obs : observers) obs.onPlayerUpdated(p.getNickname());
        }

        if (currentAge != Age.Last_Event) {
            nextRound();
        } else {
            // Ultimo evento: risolvi anche la riga superiore (eventi finali)
            List<EventCard> lastEvents = gameBoard.getUpRowEvents();
            lastEvents.sort(Comparator.comparingInt(e ->
                    e.getType() == EventType.SUSTENANCE ? 1 : 0));

            for (EventCard event : lastEvents) {
                event.resolve(players);
                for (GameObserver obs : observers) {
                    obs.onEventResolved(event.getType().toString(), "FinalEvent");
                }
            }

            gameState = GameState.END;
            endGame();
        }
    }

    // ────────────────────────────────────────────────
    //  PROSSIMO ROUND (chiamato automaticamente da resolveEvents)
    // ────────────────────────────────────────────────

    @Override
    public void nextRound() {
        topPicks = 0;
        bottomPicks = 0;

        gameBoard.shiftRows();
        gameBoard.clearBoardSpaces();

        int cardNumberToDraw = numberOfPlayers + 4;
        TribeCard tempCard;

        if (currentAge == Age.Era_I) {
            while (cardNumberToDraw > 0) {
                if (deck_ERA_I.isEmpty()) {
                    currentAge = Age.Era_II;
                    updatedAge();
                    break;
                }
                tempCard = deck_ERA_I.get(deck_ERA_I.size() - 1);
                gameBoard.addTopTribeCards(tempCard);
                deck_ERA_I.remove(deck_ERA_I.size() - 1);
                cardNumberToDraw--;
            }
        }

        if (currentAge == Age.Era_II) {
            while (cardNumberToDraw > 0) {
                if (deck_ERA_II.isEmpty()) {
                    currentAge = Age.Era_III;
                    updatedAge();
                    break;
                }
                tempCard = deck_ERA_II.get(deck_ERA_II.size() - 1);
                gameBoard.addTopTribeCards(tempCard);
                deck_ERA_II.remove(deck_ERA_II.size() - 1);
                cardNumberToDraw--;
            }
        }

        if (currentAge == Age.Era_III) {
            while (cardNumberToDraw > 0) {
                if (deck_ERA_III.isEmpty()) {
                    currentAge = Age.Last_Event;
                    break;
                }
                tempCard = deck_ERA_III.get(deck_ERA_III.size() - 1);
                gameBoard.addTopTribeCards(tempCard);
                deck_ERA_III.remove(deck_ERA_III.size() - 1);
                cardNumberToDraw--;
            }
        }

        if (currentAge == Age.Last_Event) {
            while (cardNumberToDraw > 0) {
                if (finalEvents.isEmpty()) break;
                tempCard = finalEvents.get(finalEvents.size() - 1);
                gameBoard.addTopTribeCards(tempCard);
                finalEvents.remove(finalEvents.size() - 1);
                cardNumberToDraw--;
            }
        }

        if (cardNumberToDraw > 0 && currentAge != Age.Last_Event) {
            throw new IllegalStateException("Error, not enough cards to draw");
        }

        // Aggiorna stato e ordine turno per il prossimo round
        gameState = GameState.OFFER_SPACE_CHOOSE;
        currentRoundOrder = new ArrayList<>(turnOrder.getOrder(players));
        playerInTurn = currentRoundOrder.get(0);
        playerInTurn.setInTurn(true);

        for (GameObserver obs : observers) {
            obs.onBoardUpdated();
        }

        // Notifica il primo giocatore del nuovo round
        notifyCurrentPlayerTurn();
    }

    public void updatedAge() {
        gameBoard.shiftRowsBuildings();
        gameBoard.setTopBuildingCards(buldingsInGame, currentAge);

        for (GameObserver obs : observers) {
            obs.onNewEraStarted(currentAge);
            obs.onBoardUpdated();
        }
    }

    // ────────────────────────────────────────────────
    //  FINE PARTITA (chiamato automaticamente da resolveEvents)
    // ────────────────────────────────────────────────

    @Override
    public void endGame() {
        if (gameState != GameState.END) {
            throw new IllegalStateException("Cannot end game: wrong state");
        }

        for (Player p : players) {
            p.getTotalPoints();
        }

        for (GameObserver obs : observers) {
            obs.onGameOver();
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