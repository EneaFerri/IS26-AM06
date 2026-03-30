package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.board.BoardSpace;
import it.polimi.ingsw.model.board.OrderBlock;
import it.polimi.ingsw.model.board.TurnOrder;
import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.cards.Card;
import it.polimi.ingsw.model.cards.Deck;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {

    private Player createPlayer(String nickname, TotemColor color) {
        return new Player(nickname, new Totem(color));
    }

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

    private Board createBoardForTwoPlayers() {
        List<BoardSpace> offerField = new ArrayList<>();
        offerField.add(new BoardSpace('A', 1, 0, 0));
        offerField.add(new BoardSpace('B', 1, 0, 0));
        offerField.add(new BoardSpace('C', 0, 1, 0));
        offerField.add(new BoardSpace('D', 0, 1, 0));

        return new Board(
                new Deck(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                offerField
        );
    }

    private Game createGameForTwoPlayers() {
        Board board = createBoardForTwoPlayers();
        TurnOrder turnOrder = createTurnOrderWithBlocks(2);

        return new Game(1, board, turnOrder);
    }

    @Test
    void addPlayer_withDuplicateNickname_throwsException() {
        Game game = createGameForTwoPlayers();
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Mario", TotemColor.BLUE);

        game.addPlayer(p1);

        assertThrows(IllegalArgumentException.class, () -> game.addPlayer(p2));
    }
/*
    @Test
    void startGame_withValidPlayers_setsEraAndOfferState() {
        Game game = createGameForTwoPlayers();
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);

        game.addPlayer(p1);
        game.addPlayer(p2);
 //TODO: ULTIMARE CARD.JASON PER FAR ANDARE IL TEST
        game.startGame();

        assertEquals(Age.Era_I, game.getCurrentAge());
        assertEquals(GameState.OFFER_SPACE_CHOOSE, game.getStatus());
        assertNotNull(game.getCurrentPlayer());

        // con 2 giocatori: 2 cibi al primo, 3 al secondo
        assertEquals(5, p1.getFood() + p2.getFood());
    }

    @Test
    void placeTotemOnOfferSpace_whenAllPlayersPlaced_switchesToPickingCard() {
        Game game = createGameForTwoPlayers();
        Player p1 = createPlayer("Mario", TotemColor.RED);
        Player p2 = createPlayer("Luigi", TotemColor.BLUE);

        game.addPlayer(p1);
        game.addPlayer(p2);
        game.startGame();

        BoardSpace a = game.getBoard().getBoardSpace('A');
        BoardSpace b = game.getBoard().getBoardSpace('B');

        game.placeTotemOnOfferSpace(p1, b);
        assertEquals(GameState.OFFER_SPACE_CHOOSE, game.getStatus());

        game.placeTotemOnOfferSpace(p2, a);

        assertEquals(GameState.PICKING_CARD, game.getStatus());
        assertNotNull(game.getCurrentPlayer());
        assertFalse(a.isFree());
        assertFalse(b.isFree());
    }
*/
    @Test
    void pickBuildingCard_removesFoodRemovesCardFromBoardAndAddsCardToPlayer() {
        Game game = createGameForTwoPlayers();
        Player player = createPlayer("Mario", TotemColor.RED);

        game.addPlayer(player);

        BuildingCard building = new BuildingCard(100, Age.Era_I, 2, 1) {};
        game.getBoard().getLowRowBuild().add(building);

        player.addFood(5);

        game.pickBuildingCard(player, building);

        assertEquals(3, player.getFood());
        assertTrue(player.getBuildingCards().contains(building));
        assertFalse(game.getBoard().getLowRowBuild().contains(building));
    }
}
