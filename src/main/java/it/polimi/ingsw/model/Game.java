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

    private final Board gameBoard;

    private int topPicks = 0;
    private int bottomPicks = 0;

    public Game(int gameID, Board gameBoard, TurnOrder turn) {
        this.gameID = gameID;
        this.players = new ArrayList<>();
        this.playerInTurn = null;
        this.numberOfPlayers = 0;
        this.gameState = GameState.LOGIN;
        this.currentAge = Age.Era_I;
        this.gameBoard = gameBoard;
        this.turnOrder = turn;

        this.mainDeck = new Deck();

        this.deck_ERA_I = new ArrayList<>();
        this.deck_ERA_II = new ArrayList<>();
        this.deck_ERA_III = new ArrayList<>();

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

    public void startGame() { //forse meglio qui quella che da il via? non so discutiamone
        if (gameState != GameState.LOGIN) {
            throw new IllegalStateException("Cannot start game after game has started");
        }
        if (players.isEmpty()) {
            throw new IllegalStateException("Cannot start a game without players");
        }

        setUpFirstRound(); //per comodità set up first round in altro metodo, privato tanto viene chiamato solo qua
    }

    private void setUpFirstRound() {
        //random order for the first round
        List<Player> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);

        currentRoundOrder = shuffled;

        for (Player player : shuffled) {
            turnOrder.placeTotemFirstFree(player);
        }

        //starting food based on the order
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


        //setup tribeCard
        deck_ERA_I = mainDeck.prepareTribeCards(numberOfPlayers, Age.Era_I);
        deck_ERA_II = mainDeck.prepareTribeCards(numberOfPlayers, Age.Era_II);
        deck_ERA_III = mainDeck.prepareTribeCards(numberOfPlayers, Age.Era_III);

        //setup board first turn


        //bottomCards
        int nBottomCards = numberOfPlayers +1;
        TribeCard temp = null;
        while (nBottomCards >0) {
            int i=1;
            for (i=1; i <= deck_ERA_I.size(); i++) {
                if(deck_ERA_I.get(deck_ERA_I.size()-i).isCharacter()) {
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

        //NON ANDREBBE CONTROLLATO L'ORDINE DALLA TESSERE TURN ORDER?
        //CIOE SE EFFETTIVAMENTE TOCCAVA A player del metodo a piazzare il totem?
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

        if (fromTopRow && getRemainingTopPicks(player) <= 0) {
            throw new IllegalStateException("No more top row picks allowed");
        }
        if (fromBottomRow && getRemainingBottomPicks(player) <= 0) {
            throw new IllegalStateException("No more bottom row picks allowed");
        }

        card.pick(player, this);

        if (fromTopRow) topPicks++;
        else bottomPicks++;

        if (getRemainingTopPicks(player) == 0 && getRemainingBottomPicks(player) == 0) {
            returnTotemToTurnOrder(player);
            advanceNextPlayer();
        }
    }

    public void pickCharacterCard(Player player, CharacterCard card) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(card, "card cannot be null");

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

    public void returnTotemToTurnOrder(Player player) {
        Objects.requireNonNull(player, "player cannot be null");

        turnOrder.placeTotemFirstFree(player);

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
            playerInTurn = null;
            gameState = GameState.EVENTS;
        }
    }


    public void resolveEvents() {

        if (gameState != GameState.EVENTS) {
            throw new IllegalStateException("Cannot resolve lower events after EVENTS has started");
        }

        List<EventCard> events = gameBoard.getLowRowEvents();

        for (EventCard event : events) {
            event.resolve(players);
        }

        if(currentAge != Age.Last_Round) {
            nextRound();
        }else {
            List<EventCard> LastEvents = gameBoard.getUpRowEvents();

            for (EventCard event : LastEvents) {
                event.resolve(players);
            }

            gameState = GameState.END;
            //TODO DECISIONALE : oppure direttamnte chiama EndGame() ?????
        }

    }

    public void nextRound() {
        topPicks = 0;
        bottomPicks = 0;
        // refresh tabellone
        gameBoard.shiftRows();
        gameBoard.clearBoardSpaces();



        //pseudo:
        //pescare carte da lista dell'era corrente (deck_ERA_I, ecc...) e metterle in topTribe del board
        //se la lista dell'era corrente finisce chiamare update era (da implementare)
        //continuare eventuale pescaggio da successivo deck
        //se finito anche deck_ERA_III allora pescare le 2 carte evento finale (dopo aver risolto quel turno mettere gamestate in END)
        //

        int cardNumberToDraw = numberOfPlayers + 4;
        TribeCard tempCard = null;

        if(currentAge == Age.Era_I) {
            while(cardNumberToDraw > 0){

                if(deck_ERA_I.isEmpty()){
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

        if(currentAge == Age.Era_II) {
            while(cardNumberToDraw > 0){

                if(deck_ERA_II.isEmpty()){
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

        if(currentAge == Age.Era_III) {
            while(cardNumberToDraw > 0){

                if(deck_ERA_III.isEmpty()){
                    currentAge = Age.Last_Round;
                    break;
                }

                tempCard = deck_ERA_III.get(deck_ERA_III.size() - 1);
                gameBoard.addTopTribeCards(tempCard);
                deck_ERA_III.remove(deck_ERA_III.size() - 1);
                cardNumberToDraw--;
            }
        }

        if(currentAge == Age.Last_Round) {
             //TODO: aggiungi i 2 eventi finali nella TopTribe

            //poi si fa il check (nel metodo ResolveLowerEvents) dopo aver risolto gli eventi se si è raggiunta l'era Last_Round
            //in tal caso si risolvono anche gli eventi "sopra" (compresi i 2 finali) e si può andare in gamestate.END
            //non verrà piu chiamato nextRound se l'era corrente è Last_Round...

            // da decidere se il metodo EndGame() verrà chiamato dal controller (tipo bottone finisci partita oppure calcola punteggio finale cliccabile da ogni player)
            //oppure se chiamare EndGame() "internamente" dando a tutti player i risultati finali

        }

        if(cardNumberToDraw > 0){
            throw new IllegalStateException("Error, not enough cards to draw");
        }

        // aggiorna stato e ordine turno per il prossimo round
        gameState = GameState.OFFER_SPACE_CHOOSE;
        playerInTurn = turnOrder.getOrder().get(0);
    }

    public void updatedAge() {

        gameBoard.shiftRowsBuildings(); //toglie eventuali buildings sotto, e sposta da sopra a sotto quelle sopra

        gameBoard.setTopBuildingCards(buldingsInGame, currentAge); //aggiunge sopra le building dell'era nuova (currentAge già aggiornata)
    }


    public void endGame() {

        if (gameState != GameState.END) {
            throw new IllegalStateException("Cannot end game after END has started");
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



