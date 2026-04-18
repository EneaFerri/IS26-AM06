package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.board.BoardSpace;
import it.polimi.ingsw.model.board.OrderBlock;
import it.polimi.ingsw.model.board.TurnOrder;
import it.polimi.ingsw.model.cards.*;
import it.polimi.ingsw.model.cards.Buildings.BuildingEnd;
import it.polimi.ingsw.model.cards.Characters.Artist;
import it.polimi.ingsw.model.cards.Characters.Collector;
import it.polimi.ingsw.model.cards.Characters.Hunter;
import it.polimi.ingsw.model.cards.Events.Hunt;
import it.polimi.ingsw.model.cards.Events.Sustenance;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {

    // =========================================================
    // HELPERS
    // =========================================================

    private Player createPlayer(String nickname, TotemColor color) {
        return new Player(nickname, new Totem(color));
    }

    /**
     * Crea una TurnOrder con n blocchi vuoti (nessun bonus/malus)
     * usando reflection per impostare la lista interna.
     */
    private TurnOrder createTurnOrderWithBlocks(int n) {
        TurnOrder turnOrder = new TurnOrder(n);
        List<OrderBlock> blocks = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            blocks.add(new OrderBlock(0, 0));
        }
        try {
            Field field = TurnOrder.class.getDeclaredField("orderBlocks");
            field.setAccessible(true);
            field.set(turnOrder, blocks);
        } catch (Exception e) {
            fail("Impossibile inizializzare i blocchi di TurnOrder: " + e.getMessage());
        }
        return turnOrder;
    }

    /**
     * Crea un Board minimale per 2 giocatori senza caricare il Deck da JSON.
     * Gli spazi offerta: B (1 top, 0 bottom), C (0 top, 1 bottom).
     * Solo 2 spazi perché in una partita a 2 giocatori ci sono 2 giocatori
     * che piazzano totem — usiamo solo gli spazi necessari.
     */
    private Board createEmptyBoard() {
        List<BoardSpace> offerField = new ArrayList<>();
        offerField.add(new BoardSpace('B', 1, 0, 0));
        offerField.add(new BoardSpace('C', 0, 1, 0));

        return new Board(

                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );
    }

    /**
     * Crea un Game in stato LOGIN con board e turnOrder già configurati,
     * senza caricare il Deck da JSON (usiamo reflection per bypassare startGame).
     */
    private Game createGameInLoginState() {
        Board board = createEmptyBoard();
        TurnOrder turnOrder = createTurnOrderWithBlocks(2);
        return new Game(1);
    }

    /**
     * Porta il Game direttamente in stato OFFER_SPACE_CHOOSE
     * bypassando setUpFirstRound (che carica JSON) via reflection.
     */
    private Game createGameReadyToPlay(Player p1, Player p2) {
        Board board = createEmptyBoard();
        TurnOrder turnOrder = createTurnOrderWithBlocks(2);
        Game game = new Game(1);

        // aggiungi giocatori manualmente
        try {
            Field playersField = Game.class.getDeclaredField("players");
            playersField.setAccessible(true);
            List<Player> players = (List<Player>) playersField.get(game);
            players.add(p1);
            players.add(p2);

            Field numField = Game.class.getDeclaredField("numberOfPlayers");
            numField.setAccessible(true);
            numField.set(game, 2);

            Field stateField = Game.class.getDeclaredField("gameState");
            stateField.setAccessible(true);
            stateField.set(game, GameState.OFFER_SPACE_CHOOSE);

            Field orderField = Game.class.getDeclaredField("currentRoundOrder");
            orderField.setAccessible(true);
            orderField.set(game, new ArrayList<>(List.of(p1, p2)));

            Field playerInTurnField = Game.class.getDeclaredField("playerInTurn");
            playerInTurnField.setAccessible(true);
            playerInTurnField.set(game, p1);

            // posiziona i totem sulla turnOrder
            /*
            turnOrder.placeTotemFirstFree(p1);
            turnOrder.placeTotemFirstFree(p2);

             */

        } catch (Exception e) {
            fail("Errore setup game: " + e.getMessage());
        }

        return game;
    }

    // helper da aggiungere
    private void setGameStateAndAge(Game game, GameState state, Age age) {
        try {
            Field stateField = Game.class.getDeclaredField("gameState");
            stateField.setAccessible(true);
            stateField.set(game, state);

            Field ageField = Game.class.getDeclaredField("currentAge");
            ageField.setAccessible(true);
            ageField.set(game, age);
        } catch (Exception e) {
            fail("Errore setup: " + e.getMessage());
        }
    }

    // =========================================================
    // TEST: addPlayer
    // =========================================================

    @Test
    void addPlayer_addsPlayerCorrectly() {
        Game game = createGameInLoginState();
        Player p1 = createPlayer("Mario", TotemColor.RED);

        game.addPlayer(p1);

        assertEquals(1, game.getPlayers().size());
        assertEquals("Mario", game.getPlayers().get(0).getNickname());
    }

    @Test
    void addPlayer_withDuplicateNickname_throwsException() {
        Game game = createGameInLoginState();
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Mario", TotemColor.BLUE);

        game.addPlayer(p1);

        assertThrows(IllegalArgumentException.class, () -> game.addPlayer(p2));
    }

    @Test
    void addPlayer_afterGameStarted_throwsException() {
        Game game = createGameInLoginState();
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);

        game.addPlayer(p1);
        game.addPlayer(p2);

        // porta in stato diverso da LOGIN via reflection
        try {
            Field stateField = Game.class.getDeclaredField("gameState");
            stateField.setAccessible(true);
            stateField.set(game, GameState.OFFER_SPACE_CHOOSE);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertThrows(IllegalStateException.class,
                () -> game.addPlayer(createPlayer("Terzo", TotemColor.BLACK)));
    }

    // =========================================================
    // TEST: placeTotemOnOfferSpace
    // =========================================================

    @Test
    void placeTotemOnOfferSpace_playerPlacesCorrectly() {
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);
        Game game = createGameReadyToPlay(p1, p2);

        BoardSpace b = game.getBoard().getBoardSpace('B');
        game.placeTotemOnOfferSpace(p1, b);

        assertFalse(b.isFree());
        assertEquals(p1.getTotem(), b.getTotem());
    }

    @Test
    void placeTotemOnOfferSpace_onOccupiedSpace_throwsException() {
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);
        Game game = createGameReadyToPlay(p1, p2);

        BoardSpace b = game.getBoard().getBoardSpace('B');
        game.placeTotemOnOfferSpace(p1, b);

        // p2 cerca di piazzarsi sullo stesso spazio
        assertThrows(IllegalStateException.class,
                () -> game.placeTotemOnOfferSpace(p2, b));
    }

    @Test
    void placeTotemOnOfferSpace_whenAllPlaced_switchesToPickingCard() {
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);
        Game game = createGameReadyToPlay(p1, p2);

        BoardSpace b = game.getBoard().getBoardSpace('B');
        BoardSpace c = game.getBoard().getBoardSpace('C');

        game.placeTotemOnOfferSpace(p1, b);
        assertEquals(GameState.OFFER_SPACE_CHOOSE, game.getStatus());

        game.placeTotemOnOfferSpace(p2, c);
        assertEquals(GameState.PICKING_CARD, game.getStatus());
        assertNotNull(game.getCurrentPlayer());
    }

    @Test
    void placeTotemOnOfferSpace_leftmostPlayerGoesFirst() {
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);
        Game game = createGameReadyToPlay(p1, p2);

        // p1 va in C (lettera maggiore), p2 va in B (lettera minore)
        BoardSpace b = game.getBoard().getBoardSpace('B');
        BoardSpace c = game.getBoard().getBoardSpace('C');

        game.placeTotemOnOfferSpace(p1, c);
        game.placeTotemOnOfferSpace(p2, b);

        // il primo a risolvere è chi è più a sinistra = B = p2
        assertEquals(p2, game.getCurrentPlayer());
    }

    // =========================================================
    // TEST: pickBuildingCard
    // =========================================================

    @Test
    void pickBuildingCard_removesFoodAndAddsCardToPlayer() {
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);
        Game game = createGameReadyToPlay(p1, p2);

        // crea una BuildingEnd concreta con costo 2
        BuildingEnd building = new BuildingEnd(100, Age.Era_I, 2, 1,
                CharacterType.COLLECTOR, 25);
        game.getBoard().getLowRowBuild().add(building);

        p1.addFood(5);

        game.pickBuildingCard(p1, building);

        assertEquals(3, p1.getFood()); // 5 - 2 = 3
        assertTrue(p1.getBuildingCards().contains(building));
        assertFalse(game.getBoard().getLowRowBuild().contains(building));
    }

    @Test
    void pickBuildingCard_withInsufficientFood_throwsException() {
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);
        Game game = createGameReadyToPlay(p1, p2);

        BuildingEnd building = new BuildingEnd(100, Age.Era_I, 5, 1,
                CharacterType.ARTIST, 25);
        game.getBoard().getLowRowBuild().add(building);

        p1.addFood(2); // non abbastanza

        assertThrows(IllegalStateException.class,
                () -> game.pickBuildingCard(p1, building));
    }

    // =========================================================
    // TEST: pickCharacterCard
    // =========================================================

    @Test
    void pickCharacterCard_addsCardToPlayerAndRemovesFromBoard() {
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);
        Game game = createGameReadyToPlay(p1, p2);

        Artist artist = new Artist(200, Age.Era_I, 2);
        game.getBoard().getLowRowTribe().add(artist);

        game.pickCharacterCard(p1, artist);

        assertTrue(p1.getCharacterCards().contains(artist));
        assertFalse(game.getBoard().getLowRowTribe().contains(artist));
    }

    // =========================================================
    // TEST: returnTotemToTurnOrder
    // =========================================================

    @Test
    void returnTotemToTurnOrder_totemRemovedFromBoard() {
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);
        Game game = createGameReadyToPlay(p1, p2);

        BoardSpace b = game.getBoard().getBoardSpace('B');
        game.placeTotemOnOfferSpace(p1, b);
        game.placeTotemOnOfferSpace(p2, game.getBoard().getBoardSpace('C'));

        // stato è PICKING_CARD, ritorniamo il totem di p2 (leftmost = p2 su B? no,
        // p1 è su B quindi p1 è leftmost)
        game.returnTotemToTurnOrder(p1);

        // il totem di p1 non è più sul board
        assertNull(p1.getTotem().getPosition());
    }

    // =========================================================
    // TEST: advanceNextPlayer
    // =========================================================

    @Test
    void advanceNextPlayer_movesToNextPlayer() {
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);
        Game game = createGameReadyToPlay(p1, p2);

        BoardSpace b = game.getBoard().getBoardSpace('B');
        BoardSpace c = game.getBoard().getBoardSpace('C');

        game.placeTotemOnOfferSpace(p1, b);
        game.placeTotemOnOfferSpace(p2, c);

        // p1 è leftmost, tocca a lui
        assertEquals(p1, game.getCurrentPlayer());

        game.advanceNextPlayer();

        assertEquals(p2, game.getCurrentPlayer());
    }

    @Test
    void advanceNextPlayer_afterLastPlayer_switchesToEvents() {
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);
        Game game = createGameReadyToPlay(p1, p2);

        BoardSpace b = game.getBoard().getBoardSpace('B');
        BoardSpace c = game.getBoard().getBoardSpace('C');

        game.placeTotemOnOfferSpace(p1, b);
        game.placeTotemOnOfferSpace(p2, c);

        game.advanceNextPlayer(); // passa a p2
        game.advanceNextPlayer(); // finiti tutti

        assertEquals(GameState.EVENTS, game.getStatus());
        assertNull(game.getCurrentPlayer());
    }

    // =========================================================
    // TEST: getWinners
    // =========================================================

    @Test
    void getWinners_returnsPlayerWithHighestPrestige() {
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);
        Game game = createGameInLoginState();
        game.addPlayer(p1);
        game.addPlayer(p2);

        p1.addPrestige(10);
        p2.addPrestige(5);

        List<Player> winners = game.getWinners();

        assertEquals(1, winners.size());
        assertEquals("Mario", winners.get(0).getNickname());
    }

    @Test
    void getWinners_tieBreaker_returnsPlayerWithMoreFood() {
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);
        Game game = createGameInLoginState();
        game.addPlayer(p1);
        game.addPlayer(p2);

        p1.addPrestige(10);
        p2.addPrestige(10);
        p1.addFood(3);
        p2.addFood(7);

        List<Player> winners = game.getWinners();

        assertEquals(1, winners.size());
        assertEquals("Luigi", winners.get(0).getNickname());
    }

    @Test
    void getWinners_fullTie_returnsBothPlayers() {
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);
        Game game = createGameInLoginState();
        game.addPlayer(p1);
        game.addPlayer(p2);

        p1.addPrestige(10);
        p2.addPrestige(10);
        p1.addFood(5);
        p2.addFood(5);

        List<Player> winners = game.getWinners();

        assertEquals(2, winners.size());
    }

    // =========================================================
// TEST: pickCard — gestione contatori e deleghe
// =========================================================

    @Test
    void pickCard_fromTopRow_decrementsTopPicks() {
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);
        Game game = createGameReadyToPlay(p1, p2);

        // p1 su B (1 top, 0 bottom), p2 su C
        BoardSpace b = game.getBoard().getBoardSpace('B');
        BoardSpace c = game.getBoard().getBoardSpace('C');
        game.placeTotemOnOfferSpace(p1, b);
        game.placeTotemOnOfferSpace(p2, c);

        // aggiungi una carta nella fila superiore
        Artist artist = new Artist(200, Age.Era_I, 2);
        game.getBoard().getTopRowTribe().add(artist);

        // p1 è leftmost (B < C)
        assertEquals(p1, game.getCurrentPlayer());
        assertEquals(1, game.getRemainingTopPicks(p1));

        game.pickCard(p1, artist);

        assertTrue(p1.getCharacterCards().contains(artist));
    }

    @Test
    void pickCard_exceedingTopPicks_throwsException() {
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);
        Game game = createGameReadyToPlay(p1, p2);

        BoardSpace b = game.getBoard().getBoardSpace('B');
        BoardSpace c = game.getBoard().getBoardSpace('C');
        game.placeTotemOnOfferSpace(p1, b);
        game.placeTotemOnOfferSpace(p2, c);

        Artist artist1 = new Artist(200, Age.Era_I, 2);
        Artist artist2 = new Artist(201, Age.Era_I, 2);
        game.getBoard().getTopRowTribe().add(artist1);
        game.getBoard().getTopRowTribe().add(artist2);

        // p1 pesca la sua unica carta top → avanza automaticamente a p2
        game.pickCard(p1, artist1);

        // ora tocca a p2 su C (0 top, 1 bottom)
        // p2 non può pescare dalla top row — getRemainingTopPicks = 0
        assertThrows(IllegalStateException.class, () -> game.pickCard(p2, artist2));
    }

    @Test
    void pickCard_fromBottomRow_decrementsBottomPicks() {
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);
        Game game = createGameReadyToPlay(p1, p2);

        // p1 su B (1 top, 0 bottom), p2 su C (0 top, 1 bottom)
        BoardSpace b = game.getBoard().getBoardSpace('B');
        BoardSpace c = game.getBoard().getBoardSpace('C');
        game.placeTotemOnOfferSpace(p1, b);
        game.placeTotemOnOfferSpace(p2, c);

        Artist artist = new Artist(200, Age.Era_I, 2);
        game.getBoard().getLowRowTribe().add(artist);

        // p1 è leftmost ma ha 0 bottom picks — p2 ha 1 bottom pick
        // avanziamo a p2
        game.advanceNextPlayer();

        assertEquals(1, game.getRemainingBottomPicks(p2));
        game.pickCard(p2, artist);

        assertTrue(p2.getCharacterCards().contains(artist));
    }

// =========================================================
// TEST: resolveEvents — Sustenance
// =========================================================

    @Test
    void resolveEvents_sustenance_playerPaysFood() {
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);
        Game game = createGameReadyToPlay(p1, p2);

        setGameStateAndAge(game, GameState.EVENTS, Age.Last_Event);

        // p1 ha 2 personaggi e 3 cibo
        p1.addCharacterCard(new Artist(1, Age.Era_I, 2));
        p1.addCharacterCard(new Artist(2, Age.Era_I, 2));
        p1.addFood(3);

        // aggiungi evento Sustenance nella fila inferiore
        Sustenance sustenance = new Sustenance(203, Age.Era_I, 2);
        game.getBoard().getLowRowTribe().add(sustenance);

        game.resolveEvents();

        // paga 2 cibo (1 per personaggio), ne restano 1
        assertEquals(1, p1.getFood());
    }

    @Test
    void resolveEvents_sustenance_playerLosesPrestigeWhenNotEnoughFood() {
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);
        Game game = createGameReadyToPlay(p1, p2);

        setGameStateAndAge(game, GameState.EVENTS, Age.Last_Event);

        // p1 ha 3 personaggi e solo 1 cibo
        p1.addCharacterCard(new Artist(1, Age.Era_I, 2));
        p1.addCharacterCard(new Artist(2, Age.Era_I, 2));
        p1.addCharacterCard(new Artist(3, Age.Era_I, 2));
        p1.addFood(1);

        Sustenance sustenance = new Sustenance(203, Age.Era_I, 2);
        game.getBoard().getLowRowTribe().add(sustenance);

        game.resolveEvents();

        // paga 1 cibo, mancano 2 -> perde 2*2 = 4 PP
        assertEquals(0, p1.getFood());
        assertEquals(-4, p1.getPrestige());
    }

    @Test
    void resolveEvents_hunt_playerGainsFoodAndPrestige() {
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);
        Game game = createGameReadyToPlay(p1, p2);

        setGameStateAndAge(game, GameState.EVENTS, Age.Last_Event);

        // p1 ha 2 cacciatori
        p1.addCharacterCard(new Hunter(1, Age.Era_I, 2, 0));
        p1.addCharacterCard(new Hunter(2, Age.Era_I, 2, 0));

        Hunt hunt = new Hunt(200, Age.Era_I, 2); // 2 PP per cacciatore
        game.getBoard().getLowRowTribe().add(hunt);

        game.resolveEvents();

        // 2 cacciatori: +2 cibo, +4 PP
        assertEquals(2, p1.getFood());
        assertEquals(4, p1.getPrestige());
    }

    @Test
    void resolveEvents_sustenance_resolvedLast() {
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);
        Game game = createGameReadyToPlay(p1, p2);

        setGameStateAndAge(game, GameState.EVENTS, Age.Last_Event);

        // p1 ha 1 cacciatore e 0 personaggi extra
        p1.addCharacterCard(new Hunter(1, Age.Era_I, 2, 0));

        // Hunt dà 1 cibo prima, poi Sustenance lo consuma
        Hunt hunt = new Hunt(200, Age.Era_I, 1);
        Sustenance sustenance = new Sustenance(203, Age.Era_I, 2);

        // aggiungo Sustenance prima di Hunt — deve comunque essere risolto per ultimo
        game.getBoard().getLowRowTribe().add(sustenance);
        game.getBoard().getLowRowTribe().add(hunt);

        game.resolveEvents();

        // Hunt dà 1 cibo → totale 1 cibo
        // Sustenance: 1 personaggio → paga 1 cibo → resta 0
        assertEquals(0, p1.getFood());
        assertEquals(1, p1.getPrestige()); // 1 PP da Hunt
    }

// =========================================================
// TEST: collectors — sconto Sustenance
// =========================================================

    @Test
    void resolveEvents_sustenance_collectorsReduceCost() {
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);
        Game game = createGameReadyToPlay(p1, p2);

        setGameStateAndAge(game, GameState.EVENTS, Age.Last_Event);

        // p1 ha 4 personaggi e 1 raccoglitore (sconto 3)
        p1.addCharacterCard(new Artist(1, Age.Era_I, 2));
        p1.addCharacterCard(new Artist(2, Age.Era_I, 2));
        p1.addCharacterCard(new Artist(3, Age.Era_I, 2));
        p1.addCharacterCard(new Collector(4, Age.Era_I, 2));
        p1.addFood(5);

        Sustenance sustenance = new Sustenance(203, Age.Era_I, 2);
        game.getBoard().getLowRowTribe().add(sustenance);

        game.resolveEvents();

        // 4 personaggi = 4 cibo, sconto raccoglitore = 3 → paga 1 cibo
        assertEquals(4, p1.getFood());
    }

// =========================================================
// TEST: endGame — punteggi finali
// =========================================================

    @Test
    void endGame_computesFinalScoresCorrectly() {
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);
        Game game = createGameInLoginState();
        game.addPlayer(p1);
        game.addPlayer(p2);

        try {
            Field stateField = Game.class.getDeclaredField("gameState");
            stateField.setAccessible(true);
            stateField.set(game, GameState.END);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        p1.addCharacterCard(new Artist(1, Age.Era_I, 2));
        p1.addCharacterCard(new Artist(2, Age.Era_I, 2));
        p1.addPrestige(5);

        game.endGame();

        // salva il risultato una volta sola
        int totalPoints = p1.getTotalPoints();
        assertEquals(15, totalPoints);
    }
}