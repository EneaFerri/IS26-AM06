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

/**
 * Core game model implementing all game-flow logic for a single match of Mesos.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Maintains the authoritative game state (players, board, decks, turn order).</li>
 *   <li>Implements {@link GameActions}: validates and applies all player actions.</li>
 *   <li>Notifies registered {@link GameObserver}s after every state change so that
 *       {@link it.polimi.ingsw.controller.GameController} can relay updates to clients.</li>
 * </ul>
 * </p>
 */
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

    // helper variables for card picking
    private int topPicks = 0;
    private int bottomPicks = 0;

    /**
     * Creates a new game with the given identifier.
     * Initialises all decks, the board, and sets state to {@link GameState#LOGIN}.
     *
     * @param gameID the unique identifier for this game session
     */
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

    /**
     * Registers a {@link GameObserver} that will receive all model notifications.
     *
     * @param observer the observer to register
     */
    public void addObserver(GameObserver observer) {
        this.observers.add(observer);
    }

    /** @return the unique identifier of this game session */
    public int getGameID()             { return gameID; }
    /** @return the list of players in this game */
    public List<Player> getPlayers()   { return players; }
    /** @return the current turn-order track */
    public TurnOrder getTurnOrder()    { return turnOrder; }
    /** @return the player whose turn it currently is, or {@code null} between turns */
    public Player getCurrentPlayer()   { return playerInTurn; }
    /** @return the total number of players in this game */
    public int getNumberOfPlayers()    { return numberOfPlayers; }
    /** @return the current {@link GameState} */
    public GameState getStatus()       { return gameState; }
    /** @return the current {@link Age} era */
    public Age getCurrentAge()         { return currentAge; }
    /** @return the game board */
    public Board getBoard()            { return gameBoard; }
    /** @return the main card deck used as a catalogue */
    public Deck getMainDeck()          { return mainDeck; }

    // Additional getters for persistence — do not modify state
    /** @return the remaining Era I tribe cards */
    public List<TribeCard>   getDeckEraI()          { return deck_ERA_I; }
    /** @return the remaining Era II tribe cards */
    public List<TribeCard>   getDeckEraII()         { return deck_ERA_II; }
    /** @return the remaining Era III tribe cards */
    public List<TribeCard>   getDeckEraIII()        { return deck_ERA_III; }
    /** @return the remaining final event cards */
    public List<EventCard>   getFinalEvents()       { return finalEvents; }
    /** @return the building cards currently in the game */
    public List<BuildingCard> getBuildingsInGame()  { return buldingsInGame; }
    /** @return the number of top-row picks already used by the current player this turn */
    public int getTopPicks()                        { return topPicks; }
    /** @return the number of bottom-row picks already used by the current player this turn */
    public int getBottomPicks()                     { return bottomPicks; }
    /** @return the current round's player order */
    public List<Player>      getCurrentRoundOrder() { return currentRoundOrder; }

    /**
     * Returns the total number of tribe cards remaining across all era decks.
     * The GUI uses this value for the central-deck counter, which reflects the
     * overall remaining supply rather than just the active era.
     *
     * @return the sum of remaining tribe cards in Era I, II, and III decks
     */
    public int getRemainingCardsInTotalDeck() {
        return deck_ERA_I.size() + deck_ERA_II.size() + deck_ERA_III.size();
    }

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

        // notify the first player whose turn it is
        notifyCurrentPlayerTurn();
    }

    private void setUpGameCards() {
        gameBoard.prepareGameBoardSpace(numberOfPlayers);
        turnOrder = new TurnOrder(numberOfPlayers-1);

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
            turnOrder.placeTotemFirstFreeFirstR(player);
        }

        for (int i = 0; i < shuffled.size(); i++) {

            if( i == 0 ){
                shuffled.get(i).addFood(2);
            } else if (i == 1 || i == 2) {
                shuffled.get(i).addFood(3);
            } else if ( i == 3 || i == 4) {
                shuffled.get(i).addFood(4);
            }


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
    //  PHASE 1: TOTEM PLACEMENT
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

            // handle space A (5-player games only)
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

        // notify the next player (whether still in OFFER_SPACE_CHOOSE or already in PICKING_CARD)
        if (playerInTurn != null) {
            notifyCurrentPlayerTurn();
        }
    }

    // ────────────────────────────────────────────────
    //  PHASE 2: CARD SELECTION
    // ────────────────────────────────────────────────

    /**
     * Returns the number of top-row picks the given player still has available this turn.
     *
     * @param player the player to check
     * @return the number of remaining top-row picks
     */
    public int getRemainingTopPicks(Player player) {
        BoardSpace space = player.getTotem().getPosition();
        return space.getTopCardsNumber() - topPicks;
    }

    /**
     * Returns the number of bottom-row picks the given player still has available this turn.
     *
     * @param player the player to check
     * @return the number of remaining bottom-row picks
     */
    public int getRemainingBottomPicks(Player player) {
        BoardSpace space = player.getTotem().getPosition();
        return space.getBottomCardsNumber() - bottomPicks;
    }

    /**
     * Picks a card from the board and applies it to the given player.
     * Automatically advances the turn when all picks are exhausted.
     *
     * @param player the player picking the card
     * @param card   the card to pick
     * @throws IllegalStateException if the game is not in the {@code PICKING_CARD} state,
     *                               or the player has no remaining picks for the chosen row
     */
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

        // if the player has used all their picks, advance to the next player
        if (getRemainingTopPicks(player) == 0 && getRemainingBottomPicks(player) == 0) {
            returnTotemToTurnOrder(player);
            advanceNextPlayer();

            // if we transitioned to EVENTS state, resolve them automatically
            if (gameState == GameState.EVENTS) {
                resolveEvents();
                return;
            }

            // otherwise notify the next player
            if (playerInTurn != null) {
                notifyCurrentPlayerTurn();
            }
        } else {
            // the player still has remaining picks: re-notify them
            notifyCurrentPlayerTurn();
        }
    }

    /**
     * Picks a character card from the board and adds it to the player's hand.
     *
     * @param player the player picking the card
     * @param card   the {@link CharacterCard} to pick
     */
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

    /**
     * Picks a building card from the board, deducts its food cost, and adds it to the player's hand.
     *
     * @param player the player picking the card
     * @param card   the {@link BuildingCard} to pick
     * @throws IllegalStateException if the player does not have enough food to afford the card
     */
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
    //  TURN ADVANCEMENT
    // ────────────────────────────────────────────────

    /**
     * Advances the active player to the next in the current round order.
     * Resets per-turn pick counters and sets state to {@link GameState#EVENTS}
     * when all players have taken their turn.
     */
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

    // Notifies the current player via observer, communicating the active phase.
    // GameController translates this into onYourTurn on the correct client.
    // This also resolves the freeze bug that occurred when no cards remained to pick.

    private static final String AUTO_ADVANCE_MESSAGE =
            "Non ci sono più carte da pescare. Avanzamento automatico.";

    private boolean hasPickableCardsInRow(Player player, boolean topRow) {
        List<TribeCard> tribeCards = topRow
                ? gameBoard.getAvailableUpperTribeCards()
                : gameBoard.getAvailableBottomTribeCards();

        for (TribeCard card : tribeCards) {
            if (card.isCharacter()) {
                return true;
            }
        }

        List<BuildingCard> buildingCards = topRow
                ? gameBoard.getAvailableUpperBuildingCards()
                : gameBoard.getAvailableBottomBuildingCards();

        int discount = player.foodDiscountToBuyBuildings();
        for (BuildingCard card : buildingCards) {
            int actualCost = Math.max(0, card.getFoodCost() - discount);
            if (player.getFood() >= actualCost) {
                return true;
            }
        }

        return false;
    }

    private boolean consumeUnavailableRowPicks(Player player) {
        if (player == null || gameState != GameState.PICKING_CARD) return false;

        boolean consumed = false;

        if (getRemainingTopPicks(player) > 0 && !hasPickableCardsInRow(player, true)) {
            topPicks = player.getTotem().getPosition().getTopCardsNumber();
            consumed = true;
        }

        if (getRemainingBottomPicks(player) > 0 && !hasPickableCardsInRow(player, false)) {
            bottomPicks = player.getTotem().getPosition().getBottomCardsNumber();
            consumed = true;
        }

        if (consumed) {
            for (GameObserver obs : observers) {
                obs.onPlayerError(AUTO_ADVANCE_MESSAGE);
            }
        }

        return consumed;
    }


    private void notifyCurrentPlayerTurn() {
        while (playerInTurn != null && gameState == GameState.PICKING_CARD) {
            consumeUnavailableRowPicks(playerInTurn);

            if (getRemainingTopPicks(playerInTurn) == 0
                    && getRemainingBottomPicks(playerInTurn) == 0) {
                returnTotemToTurnOrder(playerInTurn);
                advanceNextPlayer();

                if (gameState == GameState.EVENTS) {
                    resolveEvents();
                    return;
                }

                continue;
            }

            break;
        }

        if (playerInTurn == null) return;

        for (GameObserver obs : observers) {
            obs.onTurnStarted(playerInTurn.getNickname(), gameState);
        }
    }



    // ────────────────────────────────────────────────
    //  END OF ROUND: EVENTS (called automatically)
    // ────────────────────────────────────────────────

    @Override
    public void resolveEvents() {
        if (gameState != GameState.EVENTS) {
            throw new IllegalStateException("Cannot resolve events: wrong game state");
        }

        List<EventCard> events = gameBoard.getLowRowEvents();

        // Sustenance is always last — explicit rule
        events.sort(Comparator.comparingInt(e ->
                e.getType() == EventType.SUSTENANCE ? 1 : 0));

        for (EventCard event : events) {
            event.resolve(players);
            for (GameObserver obs : observers) {
                obs.onEventResolved(event.getType().toString(), "cardId=" + event.getID());
            }
        }

        for (Player p : players) {
            for (GameObserver obs : observers) obs.onPlayerUpdated(p.getNickname());
        }

        if (currentAge != Age.Last_Event) {
            nextRound();
        } else {
            // last event: also resolve the top row (final events)
            List<EventCard> lastEvents = gameBoard.getUpRowEvents();
            lastEvents.sort(Comparator.comparingInt(e ->
                    e.getType() == EventType.SUSTENANCE ? 1 : 0));

            for (EventCard event : lastEvents) {
                event.resolve(players);
                for (GameObserver obs : observers) {
                    obs.onEventResolved(event.getType().toString(), "cardId=" + event.getID());
                }
            }


            gameState = GameState.END;
            endGame();
        }
    }

    // ────────────────────────────────────────────────
    //  NEXT ROUND (called automatically by resolveEvents)
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

        // update state and turn order for the next round
        gameState = GameState.OFFER_SPACE_CHOOSE;
        currentRoundOrder = new ArrayList<>(turnOrder.getOrder(players));
        playerInTurn = currentRoundOrder.get(0);
        playerInTurn.setInTurn(true);

        for (GameObserver obs : observers) {
            obs.onBoardUpdated();
        }

        // notify the first player of the new round
        notifyCurrentPlayerTurn();
    }

    /**
     * Handles the transition to a new era: shifts building card rows, deals new building
     * cards for the current era, and notifies observers.
     */
    public void updatedAge() {
        gameBoard.shiftRowsBuildings();
        gameBoard.setTopBuildingCards(buldingsInGame, currentAge);

        for (GameObserver obs : observers) {
            obs.onNewEraStarted(currentAge);
            obs.onBoardUpdated();
        }
    }

    // ────────────────────────────────────────────────
    //  END OF GAME (called automatically by resolveEvents)
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

    // ────────────────────────────────────────────────
    //  RESTORE FROM SNAPSHOT (called only by PersistenceManager)
    // ────────────────────────────────────────────────

    /**
     * Rebuilds the internal state of this Game from a previously saved snapshot.
     * Called exactly once by {@code PersistenceManager.restore()} immediately after
     * creating the Game with {@code new Game(gameId)}.
     *
     * <p>Does not touch {@code mainDeck} (used as a card catalogue for lookups)
     * or observers. Non-final lists and references are set directly.</p>
     *
     * @param restoredPlayers       players already rebuilt with their cards and flags
     * @param playerInTurnNick      nickname of the current player, or {@code null} if none
     * @param roundOrderNicks       current round order as a list of nicknames
     * @param state                 game state to restore
     * @param age                   current era
     * @param numPlayers            number of players
     * @param deckI                 remaining Era I tribe cards
     * @param deckII                remaining Era II tribe cards
     * @param deckIII               remaining Era III tribe cards
     * @param finalEvts             remaining final event cards
     * @param buildings             building cards in the game
     * @param topPicks              current top-row pick counter
     * @param bottomPicks           current bottom-row pick counter
     * @param boardTopTribe         top tribe card row of the board
     * @param boardBottomTribe      bottom tribe card row of the board
     * @param boardTopBuild         top building card row of the board
     * @param boardBottomBuild      bottom building card row of the board
     * @param spaceTotemColors      map from space letter to TotemColor name ({@code null} means empty)
     * @param turnOrderTag          configuration tag for {@link TurnOrder}
     * @param blockTotemColors      totem color for each order block ({@code null} means empty)
     */
    public void restorePersistedState(
            List<Player> restoredPlayers,
            String playerInTurnNick,
            List<String> roundOrderNicks,
            GameState state, Age age, int numPlayers,
            List<TribeCard> deckI, List<TribeCard> deckII, List<TribeCard> deckIII,
            List<EventCard> finalEvts, List<BuildingCard> buildings,
            int topPicks, int bottomPicks,
            List<TribeCard> boardTopTribe, List<TribeCard> boardBottomTribe,
            List<BuildingCard> boardTopBuild, List<BuildingCard> boardBottomBuild,
            java.util.Map<Character, String> spaceTotemColors,
            int turnOrderTag, List<String> blockTotemColors) {

        // ── Players ──────────────────────────────────────────────────────────
        this.players.clear();
        this.players.addAll(restoredPlayers);
        this.numberOfPlayers = numPlayers;

        this.playerInTurn = playerInTurnNick == null ? null :
                restoredPlayers.stream()
                        .filter(p -> p.getNickname().equals(playerInTurnNick))
                        .findFirst().orElse(null);

        this.currentRoundOrder = roundOrderNicks.stream()
                .map(nick -> restoredPlayers.stream()
                        .filter(p -> p.getNickname().equals(nick))
                        .findFirst().orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());

        // ── Game state ───────────────────────────────────────────────────────
        this.gameState   = state;
        this.currentAge  = age;
        this.deck_ERA_I  = new ArrayList<>(deckI);
        this.deck_ERA_II = new ArrayList<>(deckII);
        this.deck_ERA_III = new ArrayList<>(deckIII);
        this.finalEvents  = new ArrayList<>(finalEvts);
        this.buldingsInGame = new ArrayList<>(buildings);
        this.topPicks    = topPicks;
        this.bottomPicks = bottomPicks;

        // ── Board card rows ───────────────────────────────────────────────────
        gameBoard.restoreCardRows(boardTopTribe, boardBottomTribe, boardTopBuild, boardBottomBuild);

        // ── Totems on board spaces ────────────────────────────────────────────
        // first clear all spaces (Board comes from new Game() with empty spaces)
        gameBoard.clearBoardSpaces();
        for (it.polimi.ingsw.model.board.BoardSpace space : gameBoard.getOfferField()) {
            String colorName = spaceTotemColors.get(space.getLetter());
            if (colorName != null) {
                restoredPlayers.stream()
                        .filter(p -> p.getTotem().getColor().name().equals(colorName))
                        .findFirst()
                        .ifPresent(p -> gameBoard.placeTotem(p.getTotem(), space));
            }
        }

        // ── TurnOrder ────────────────────────────────────────────────────────
        TurnOrder to = new TurnOrder(turnOrderTag);
        List<it.polimi.ingsw.model.board.OrderBlock> blocks = to.getOrderBlocks();
        for (int i = 0; i < blocks.size() && i < blockTotemColors.size(); i++) {
            String colorName = blockTotemColors.get(i);
            if (colorName != null) {
                final it.polimi.ingsw.model.board.OrderBlock block = blocks.get(i);
                restoredPlayers.stream()
                        .filter(p -> p.getTotem().getColor().name().equals(colorName))
                        .findFirst()
                        .ifPresent(p -> {
                            block.setTotem(p.getTotem());
                            p.getTotem().remove(); // totem.position=null when on the TurnOrder track
                        });
            }
        }
        this.turnOrder = to;

        // ── inTurn flag ───────────────────────────────────────────────────────
        for (Player p : this.players) {
            p.setInTurn(p == this.playerInTurn);
        }
    }

    /**
     * Returns the list of winners at the end of the game.
     * Winners are players with the highest prestige; ties are broken by food count.
     *
     * @return a list of winning {@link Player}s (may contain more than one in case of a tie)
     */
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
