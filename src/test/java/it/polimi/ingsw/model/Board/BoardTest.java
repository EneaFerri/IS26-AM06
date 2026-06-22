package it.polimi.ingsw.model.Board;

import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.board.BoardSpace;
import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.cards.EventCard;
import it.polimi.ingsw.model.cards.Buildings.BuildingEnd;
import it.polimi.ingsw.model.cards.Characters.Artist;
import it.polimi.ingsw.model.cards.Characters.Hunter;
import it.polimi.ingsw.model.cards.Events.Hunt;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests all non-getter public methods of Board:
 * - prepareGameBoardSpace: removes the correct spaces for 2-5 players.
 * - addTopTribeCards / addBottomTribeCardsFirstTurn: availability filtering by drawed flag.
 * - setTopBuildingCards / getAvailableUpperBuildingCards / getAvailableBottomBuildingCards.
 * - removeCard: removes from every card list; no-op when card is absent.
 * - shiftRows / shiftRowsBuildings: top row replaces bottom, top is cleared.
 * - placeTotem / clearBoardSpaces: totem placement and bulk removal.
 * - getFreeBoardSpaces: count decreases as totems are placed.
 * - getPlayerInOfferOrder: returns players in offerField list order.
 * - getLowRowEvents / getUpRowEvents: returns only EventCard instances.
 * - restoreCardRows: sets all four card lists at once.
 * - getBoardSpace: returns null for an unknown letter.
 */
class BoardTest {

    // =========================================================
    // HELPER: replace offerField with controlled spaces
    // =========================================================

    private Board createBoardWithSpaces(BoardSpace... spaces) {
        Board board = new Board();
        try {
            Field f = Board.class.getDeclaredField("offerField");
            f.setAccessible(true);
            f.set(board, new ArrayList<>(List.of(spaces)));
        } catch (Exception e) {
            fail("Could not inject offerField: " + e.getMessage());
        }
        return board;
    }

    @SuppressWarnings("unchecked")
    private List<BuildingCard> getBottomBuildingCards(Board board) {
        try {
            Field f = Board.class.getDeclaredField("bottomBuildingCards");
            f.setAccessible(true);
            return (List<BuildingCard>) f.get(board);
        } catch (Exception e) {
            fail("Could not access bottomBuildingCards: " + e.getMessage());
            return null;
        }
    }

    // =========================================================
    // Group A: prepareGameBoardSpace
    // =========================================================

    @Test
    void prepareGameBoardSpace_fivePlayers_keepsAllSevenSpaces() {
        Board board = new Board();
        board.prepareGameBoardSpace(5);
        assertEquals(7, board.getOfferField().size());
    }

    @Test
    void prepareGameBoardSpace_fourPlayers_removesSpaceA() {
        Board board = new Board();
        board.prepareGameBoardSpace(4);
        assertEquals(6, board.getOfferField().size());
        assertTrue(board.getOfferField().stream().noneMatch(s -> s.getLetter() == 'A'));
    }

    @Test
    void prepareGameBoardSpace_threePlayers_removesSpacesAAndG() {
        Board board = new Board();
        board.prepareGameBoardSpace(3);
        assertEquals(5, board.getOfferField().size());
        assertTrue(board.getOfferField().stream().noneMatch(s -> s.getLetter() == 'A'));
        assertTrue(board.getOfferField().stream().noneMatch(s -> s.getLetter() == 'G'));
    }

    @Test
    void prepareGameBoardSpace_twoPlayers_removesSpacesADG() {
        Board board = new Board();
        board.prepareGameBoardSpace(2);
        assertEquals(4, board.getOfferField().size());
        assertTrue(board.getOfferField().stream().noneMatch(s -> s.getLetter() == 'A'));
        assertTrue(board.getOfferField().stream().noneMatch(s -> s.getLetter() == 'D'));
        assertTrue(board.getOfferField().stream().noneMatch(s -> s.getLetter() == 'G'));
    }

    // =========================================================
    // Group B: top/bottom tribe card rows
    // =========================================================

    @Test
    void addTopTribeCards_nonDrawed_appearsInAvailableUpper() {
        Board board = new Board();
        Artist artist = new Artist(1, Age.Era_I, 2);
        board.addTopTribeCards(artist);
        List<?> available = board.getAvailableUpperTribeCards();
        assertEquals(1, available.size());
        assertTrue(available.contains(artist));
    }

    @Test
    void addTopTribeCards_drawed_excludedFromAvailableUpper() {
        Board board = new Board();
        Artist artist = new Artist(1, Age.Era_I, 2);
        artist.markAsDrawed();
        board.addTopTribeCards(artist);
        assertTrue(board.getAvailableUpperTribeCards().isEmpty());
    }

    @Test
    void addBottomTribeCardsFirstTurn_nonDrawed_appearsInAvailableBottom() {
        Board board = new Board();
        Hunter hunter = new Hunter(2, Age.Era_I, 2, 0);
        board.addBottomTribeCardsFirstTurn(hunter);
        List<?> available = board.getAvailableBottomTribeCards();
        assertEquals(1, available.size());
        assertTrue(available.contains(hunter));
    }

    @Test
    void addBottomTribeCardsFirstTurn_drawed_excludedFromAvailableBottom() {
        Board board = new Board();
        Hunter hunter = new Hunter(2, Age.Era_I, 2, 0);
        hunter.markAsDrawed();
        board.addBottomTribeCardsFirstTurn(hunter);
        assertTrue(board.getAvailableBottomTribeCards().isEmpty());
    }

    // =========================================================
    // Group C: building card rows
    // =========================================================

    @Test
    void setTopBuildingCards_filtersCorrectlyByAge() {
        Board board = new Board();
        BuildingEnd b1 = new BuildingEnd(10, Age.Era_I,  2, 1, CharacterType.HUNTER, 5);
        BuildingEnd b2 = new BuildingEnd(11, Age.Era_II, 2, 1, CharacterType.HUNTER, 5);
        board.setTopBuildingCards(List.of(b1, b2), Age.Era_I);
        assertEquals(1, board.getTopRowBuild().size());
        assertTrue(board.getTopRowBuild().contains(b1));
    }

    @Test
    void getAvailableUpperBuildingCards_excludesDrawedCards() {
        Board board = new Board();
        BuildingEnd b = new BuildingEnd(10, Age.Era_I, 2, 1, CharacterType.HUNTER, 5);
        board.setTopBuildingCards(List.of(b), Age.Era_I);
        b.markAsDrawed();
        assertTrue(board.getAvailableUpperBuildingCards().isEmpty());
    }

    @Test
    void getAvailableBottomBuildingCards_returnsNonDrawedCards() {
        Board board = new Board();
        BuildingEnd b = new BuildingEnd(10, Age.Era_I, 2, 1, CharacterType.HUNTER, 5);
        getBottomBuildingCards(board).add(b);
        assertEquals(1, board.getAvailableBottomBuildingCards().size());
        b.markAsDrawed();
        assertTrue(board.getAvailableBottomBuildingCards().isEmpty());
    }

    // =========================================================
    // Group D: removeCard
    // =========================================================

    @Test
    void removeCard_removesFromTopTribeCards() {
        Board board = new Board();
        Artist a = new Artist(1, Age.Era_I, 2);
        board.addTopTribeCards(a);
        board.removeCard(a);
        assertTrue(board.getTopRowTribe().isEmpty());
    }

    @Test
    void removeCard_removesFromBottomTribeCards() {
        Board board = new Board();
        Artist a = new Artist(1, Age.Era_I, 2);
        board.addBottomTribeCardsFirstTurn(a);
        board.removeCard(a);
        assertTrue(board.getLowRowTribe().isEmpty());
    }

    @Test
    void removeCard_removesFromTopBuildingCards() {
        Board board = new Board();
        BuildingEnd b = new BuildingEnd(10, Age.Era_I, 2, 1, CharacterType.HUNTER, 5);
        board.setTopBuildingCards(List.of(b), Age.Era_I);
        board.removeCard(b);
        assertTrue(board.getTopRowBuild().isEmpty());
    }

    @Test
    void removeCard_removesFromBottomBuildingCards() {
        Board board = new Board();
        BuildingEnd b = new BuildingEnd(10, Age.Era_I, 2, 1, CharacterType.HUNTER, 5);
        getBottomBuildingCards(board).add(b);
        board.removeCard(b);
        assertTrue(board.getLowRowBuild().isEmpty());
    }

    @Test
    void removeCard_cardNotPresent_doesNotThrow() {
        Board board = new Board();
        Artist a = new Artist(99, Age.Era_I, 2);
        assertDoesNotThrow(() -> board.removeCard(a));
    }

    // =========================================================
    // Group E: shiftRows / shiftRowsBuildings
    // =========================================================

    @Test
    void shiftRows_movesTopTribeToBottom_clearsTop() {
        Board board = new Board();
        board.addTopTribeCards(new Artist(1, Age.Era_I, 2));
        board.addTopTribeCards(new Artist(2, Age.Era_I, 2));
        board.shiftRows();
        assertEquals(2, board.getLowRowTribe().size());
        assertTrue(board.getTopRowTribe().isEmpty());
    }

    @Test
    void shiftRows_overwritesExistingBottomRow() {
        Board board = new Board();
        Hunter oldBottom = new Hunter(1, Age.Era_I, 2, 0);
        Artist newTop    = new Artist(2, Age.Era_I, 2);
        board.addBottomTribeCardsFirstTurn(oldBottom);
        board.addTopTribeCards(newTop);
        board.shiftRows();
        assertEquals(1, board.getLowRowTribe().size());
        assertTrue(board.getLowRowTribe().contains(newTop));
        assertFalse(board.getLowRowTribe().contains(oldBottom));
    }

    @Test
    void shiftRowsBuildings_movesTopBuildToBottom_clearsTop() {
        Board board = new Board();
        BuildingEnd b = new BuildingEnd(10, Age.Era_I, 2, 1, CharacterType.HUNTER, 5);
        board.setTopBuildingCards(List.of(b), Age.Era_I);
        board.shiftRowsBuildings();
        assertEquals(1, board.getLowRowBuild().size());
        assertTrue(board.getTopRowBuild().isEmpty());
    }

    // =========================================================
    // Group F: placeTotem / clearBoardSpaces
    // =========================================================

    @Test
    void placeTotem_spacePreviouslyFree_becomesOccupied() {
        Board board = createBoardWithSpaces(new BoardSpace('B', 1, 0, 0));
        Totem totem = new Totem(TotemColor.RED);
        BoardSpace space = board.getBoardSpace('B');
        board.placeTotem(totem, space);
        assertFalse(space.isFree());
        assertEquals(totem, space.getTotem());
    }

    @Test
    void clearBoardSpaces_removesAllTotems() {
        Board board = createBoardWithSpaces(
                new BoardSpace('B', 1, 0, 0),
                new BoardSpace('C', 0, 1, 0)
        );
        board.placeTotem(new Totem(TotemColor.RED),  board.getBoardSpace('B'));
        board.placeTotem(new Totem(TotemColor.BLUE), board.getBoardSpace('C'));
        board.clearBoardSpaces();
        assertEquals(2, board.getFreeBoardSpaces().size());
    }

    // =========================================================
    // Group G: getFreeBoardSpaces
    // =========================================================

    @Test
    void getFreeBoardSpaces_noTotems_returnsAllSpaces() {
        Board board = createBoardWithSpaces(
                new BoardSpace('B', 1, 0, 0),
                new BoardSpace('C', 0, 1, 0)
        );
        assertEquals(2, board.getFreeBoardSpaces().size());
    }

    @Test
    void getFreeBoardSpaces_oneTotemPlaced_returnsOneLessSpace() {
        Board board = createBoardWithSpaces(
                new BoardSpace('B', 1, 0, 0),
                new BoardSpace('C', 0, 1, 0)
        );
        board.placeTotem(new Totem(TotemColor.RED), board.getBoardSpace('B'));
        assertEquals(1, board.getFreeBoardSpaces().size());
    }

    // =========================================================
    // Group H: getPlayerInOfferOrder
    // =========================================================

    @Test
    void getPlayerInOfferOrder_returnsPlayersInOfferFieldListOrder() {
        Board board = createBoardWithSpaces(
                new BoardSpace('B', 1, 0, 0),
                new BoardSpace('C', 0, 1, 0)
        );
        Player alice = new Player("Alice", new Totem(TotemColor.RED));
        Player bob   = new Player("Bob",   new Totem(TotemColor.BLUE));
        // Alice on C (second), Bob on B (first)
        board.placeTotem(alice.getTotem(), board.getBoardSpace('C'));
        board.placeTotem(bob.getTotem(),   board.getBoardSpace('B'));

        List<Player> ordered = board.getPlayerInOfferOrder(List.of(alice, bob));

        assertEquals(2, ordered.size());
        assertEquals(bob,   ordered.get(0));
        assertEquals(alice, ordered.get(1));
    }

    // =========================================================
    // Group I: getLowRowEvents / getUpRowEvents
    // =========================================================

    @Test
    void getLowRowEvents_returnsOnlyEventCardsFromBottomRow() {
        Board board = new Board();
        board.addBottomTribeCardsFirstTurn(new Artist(1, Age.Era_I, 2));
        board.addBottomTribeCardsFirstTurn(new Hunt(50, Age.Era_I, 1));
        List<EventCard> events = board.getLowRowEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof Hunt);
    }

    @Test
    void getUpRowEvents_returnsOnlyEventCardsFromTopRow() {
        Board board = new Board();
        board.addTopTribeCards(new Artist(1, Age.Era_I, 2));
        board.addTopTribeCards(new Hunt(50, Age.Era_I, 1));
        List<EventCard> events = board.getUpRowEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof Hunt);
    }

    // =========================================================
    // Group J: restoreCardRows
    // =========================================================

    @Test
    void restoreCardRows_setsAllFourLists() {
        Board board = new Board();
        Artist   topT = new Artist(1, Age.Era_I, 2);
        Hunter   botT = new Hunter(2, Age.Era_I, 2, 0);
        BuildingEnd topB = new BuildingEnd(10, Age.Era_I,  2, 1, CharacterType.HUNTER, 5);
        BuildingEnd botB = new BuildingEnd(11, Age.Era_II, 2, 1, CharacterType.HUNTER, 5);

        board.restoreCardRows(List.of(topT), List.of(botT), List.of(topB), List.of(botB));

        assertEquals(1, board.getTopRowTribe().size());
        assertEquals(1, board.getLowRowTribe().size());
        assertEquals(1, board.getTopRowBuild().size());
        assertEquals(1, board.getLowRowBuild().size());
        assertTrue(board.getTopRowTribe().contains(topT));
        assertTrue(board.getLowRowTribe().contains(botT));
    }

    // =========================================================
    // Group K: getBoardSpace — null path
    // =========================================================

    @Test
    void getBoardSpace_nonExistentLetter_returnsNull() {
        Board board = createBoardWithSpaces(new BoardSpace('B', 1, 0, 0));
        assertNull(board.getBoardSpace('Z'));
    }
}
