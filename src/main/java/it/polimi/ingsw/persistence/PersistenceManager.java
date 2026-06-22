package it.polimi.ingsw.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.cards.*;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.model.enums.InventionType;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages saving and restoring games to/from disk (Persistence feature).
 *
 * <p>Each game is saved as {@code saves/game_{id}.json} using Jackson.
 * Saving occurs after every turn transition; the file is deleted when the game
 * ends normally.
 *
 * <p>On server restart, {@link #loadAll()} reads all existing files and
 * {@link #restore(GameSnapshot)} reconstructs the full object graph using the
 * card catalogue from a fresh {@link it.polimi.ingsw.model.cards.Deck}
 * (already present in the {@link Game} after {@code new Game(id)}).
 */
public class PersistenceManager {

    private static final String SAVE_DIR = "saves";
    private static PersistenceManager instance;

    private final ObjectMapper mapper = new ObjectMapper();

    private PersistenceManager() {
        File dir = new File(SAVE_DIR);
        if (!dir.exists()) dir.mkdirs();
    }

    /**
     * Returns the singleton instance, creating it on first call.
     *
     * @return the singleton {@code PersistenceManager}
     */
    public static synchronized PersistenceManager getInstance() {
        if (instance == null) instance = new PersistenceManager();
        return instance;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SAVE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Serialises the current game state to disk, including the nicknames of active bots.
     *
     * @param game         the game to save
     * @param botNicknames nicknames of players currently replaced by bots
     */
    public void save(Game game, Collection<String> botNicknames) {
        try {
            GameSnapshot snap = toSnapshot(game, botNicknames);
            File file = saveFile(game.getGameID());
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, snap);
        } catch (Exception e) {
            System.err.println("[PersistenceManager] save game " + game.getGameID() + " failed: " + e.getMessage());
        }
    }

    /**
     * Deletes the save file for the given game (called when the game ends normally).
     *
     * @param gameId ID of the game whose save file should be removed
     */
    public void delete(int gameId) {
        File file = saveFile(gameId);
        if (file.exists() && !file.delete()) {
            System.err.println("[PersistenceManager] impossibile eliminare " + file.getName());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LOAD + RESTORE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Loads all {@code saves/game_*.json} files and deserialises them into snapshots.
     *
     * @return list of all successfully loaded {@link GameSnapshot} objects
     */
    public List<GameSnapshot> loadAll() {
        List<GameSnapshot> result = new ArrayList<>();
        Path dir = Paths.get(SAVE_DIR);
        if (!Files.isDirectory(dir)) return result;

        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().matches("game_\\d+\\.json"))
                 .forEach(p -> {
                     try {
                         result.add(mapper.readValue(p.toFile(), GameSnapshot.class));
                         System.out.println("[PersistenceManager] Caricato snapshot: " + p.getFileName());
                     } catch (Exception e) {
                         System.err.println("[PersistenceManager] Impossibile leggere " + p + ": " + e.getMessage());
                     }
                 });
        } catch (Exception e) {
            System.err.println("[PersistenceManager] loadAll failed: " + e.getMessage());
        }
        return result;
    }

    /**
     * Reconstructs a complete {@link Game} object from a {@link GameSnapshot}.
     *
     * <p>Strategy:
     * <ol>
     *   <li>Creates {@code new Game(id)} — the constructor produces a fresh main deck with all cards.</li>
     *   <li>Builds a {@code cardId → Card} catalogue from {@code mainDeck.getAllCards()}.</li>
     *   <li>Reconstructs each {@link Player} (with their cards, without side-effects).</li>
     *   <li>Reconstructs the board rows, totems on spaces, and the TurnOrder.</li>
     *   <li>Calls {@code game.restorePersistedState()} to set all non-final fields.</li>
     * </ol>
     *
     * @param snap the snapshot to restore from
     * @return a fully initialised {@link Game} reflecting the saved state
     */
    public Game restore(GameSnapshot snap) {
        Game game = new Game(snap.gameId());

        // Card catalogue: id → object
        Map<Integer, Card> catalog = game.getMainDeck().getAllCards().stream()
                .collect(Collectors.toMap(Card::getID, c -> c));

        // ── Players ────────────────────────────────────────────────────────
        List<Player> players = new ArrayList<>();
        for (PlayerSnapshot ps : snap.players()) {
            players.add(restorePlayer(ps, catalog));
        }

        // ── Board card rows ────────────────────────────────────────────────
        List<TribeCard>    boardTopTribe    = resolveCardRefs(snap.board().topTribeCards(),    catalog, TribeCard.class);
        List<TribeCard>    boardBottomTribe = resolveCardRefs(snap.board().bottomTribeCards(), catalog, TribeCard.class);
        List<BuildingCard> boardTopBuild    = resolveCardRefs(snap.board().topBuildingCards(), catalog, BuildingCard.class);
        List<BuildingCard> boardBottomBuild = resolveCardRefs(snap.board().bottomBuildingCards(), catalog, BuildingCard.class);

        // ── Remaining decks ────────────────────────────────────────────────
        List<TribeCard>    deckI     = resolveIds(snap.deckEraI(),         catalog, TribeCard.class);
        List<TribeCard>    deckII    = resolveIds(snap.deckEraII(),        catalog, TribeCard.class);
        List<TribeCard>    deckIII   = resolveIds(snap.deckEraIII(),       catalog, TribeCard.class);
        List<EventCard>    finalEvts = resolveIds(snap.finalEventIds(),    catalog, EventCard.class);
        List<BuildingCard> buildings = resolveIds(snap.buildingsInGameIds(), catalog, BuildingCard.class);

        // ── Totem on board spaces: letter → TotemColor name ───────────────
        Map<Character, String> spaceTotemColors = new HashMap<>();
        for (BoardSnapshot.SpaceSnap ss : snap.board().spaces()) {
            if (ss.totemColor() != null) {
                spaceTotemColors.put(ss.letter(), ss.totemColor());
            }
        }

        // ── Enum from string ───────────────────────────────────────────────
        GameState state = GameState.valueOf(snap.gameState());
        Age age         = Age.valueOf(snap.currentAge());

        // ── Rebuild restored board (recalculate active spaces for player count) ─
        // The fresh board has ALL spaces (A-G); recalculate which are in play
        game.getBoard().prepareGameBoardSpace(snap.numberOfPlayers());

        // ── Call restorePersistedState on Game ─────────────────────────────
        game.restorePersistedState(
                players,
                snap.playerInTurnNick(),
                snap.currentRoundOrderNicks(),
                state, age, snap.numberOfPlayers(),
                deckI, deckII, deckIII,
                finalEvts, buildings,
                snap.topPicks(), snap.bottomPicks(),
                boardTopTribe, boardBottomTribe,
                boardTopBuild, boardBottomBuild,
                spaceTotemColors,
                snap.turnOrder().tag(),
                snap.turnOrder().blockTotemColors()
        );

        return game;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Game → GameSnapshot CONVERSION
    // ─────────────────────────────────────────────────────────────────────────

    private GameSnapshot toSnapshot(Game game, Collection<String> botNicknames) {
        List<PlayerSnapshot> playerSnaps = game.getPlayers().stream()
                .map(this::playerToSnapshot)
                .collect(Collectors.toList());

        BoardSnapshot boardSnap = boardToSnapshot(game);
        TurnOrderSnapshot toSnap = turnOrderToSnapshot(game);

        List<Integer> deckI   = toIdList(game.getDeckEraI());
        List<Integer> deckII  = toIdList(game.getDeckEraII());
        List<Integer> deckIII = toIdList(game.getDeckEraIII());
        List<Integer> finals  = toIdList(game.getFinalEvents());
        List<Integer> bldgs   = toIdList(game.getBuildingsInGame());

        String playerInTurnNick = game.getCurrentPlayer() != null
                ? game.getCurrentPlayer().getNickname() : null;

        List<String> roundOrderNicks = game.getCurrentRoundOrder() == null
                ? game.getPlayers().stream().map(Player::getNickname).collect(Collectors.toList())
                : game.getCurrentRoundOrder().stream().map(Player::getNickname).collect(Collectors.toList());

        return new GameSnapshot(
                game.getGameID(),
                game.getNumberOfPlayers(),
                game.getStatus().name(),
                game.getCurrentAge().name(),
                playerInTurnNick,
                roundOrderNicks,
                playerSnaps, boardSnap, toSnap,
                deckI, deckII, deckIII, finals, bldgs,
                game.getTopPicks(), game.getBottomPicks(),
                new ArrayList<>(botNicknames)
        );
    }

    private PlayerSnapshot playerToSnapshot(Player p) {
        return new PlayerSnapshot(
                p.getNickname(),
                p.getTotem().getColor().name(),
                p.getFood(),
                p.getPrestige(),
                p.getCharacterCards().stream().map(Card::getID).collect(Collectors.toList()),
                p.getBuildingCards().stream().map(Card::getID).collect(Collectors.toList()),
                p.getMyInventions().stream().map(Enum::name).collect(Collectors.toList()),
                p.hasDoublePointForBuilder(),
                p.doublePointForRituals,
                p.noMalusForRituals,
                p.extraThreeStars,
                p.hasExtraFoodOnTurnOrder(),
                p.hasExtraCard(),
                p.hasSetToCheck(),
                p.getSetNumberForExtraFood(),
                p.hasInventorsToCheck(),
                p.getBuildingFoodDiscount()
        );
    }

    private BoardSnapshot boardToSnapshot(Game game) {
        List<CardRef> topTribe    = toCardRefList(game.getBoard().getTopRowTribe());
        List<CardRef> bottomTribe = toCardRefList(game.getBoard().getLowRowTribe());
        List<CardRef> topBuild    = toCardRefList(game.getBoard().getTopRowBuild());
        List<CardRef> bottomBuild = toCardRefList(game.getBoard().getLowRowBuild());

        List<BoardSnapshot.SpaceSnap> spaces = game.getBoard().getOfferField().stream()
                .map(s -> new BoardSnapshot.SpaceSnap(
                        s.getLetter(),
                        s.isFree() ? null : s.getTotem().getColor().name()))
                .collect(Collectors.toList());

        return new BoardSnapshot(topTribe, bottomTribe, topBuild, bottomBuild, spaces);
    }

    private TurnOrderSnapshot turnOrderToSnapshot(Game game) {
        if (game.getTurnOrder() == null) {
            return new TurnOrderSnapshot(game.getNumberOfPlayers() - 1, Collections.emptyList());
        }
        int tag = game.getTurnOrder().getTag();
        List<String> blockColors = game.getTurnOrder().getOrderBlocks().stream()
                .map(b -> b.isFree() ? null : b.getTotemOn().getColor().name())
                .collect(Collectors.toList());
        return new TurnOrderSnapshot(tag, blockColors);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PLAYER RESTORE
    // ─────────────────────────────────────────────────────────────────────────

    private Player restorePlayer(PlayerSnapshot ps, Map<Integer, Card> catalog) {
        TotemColor color = TotemColor.valueOf(ps.totemColor());
        Player p = new Player(ps.nickname(), new Totem(color));

        List<CharacterCard> chars = ps.characterCardIds().stream()
                .map(id -> (CharacterCard) catalog.get(id))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        // Mark character cards as drawn
        chars.forEach(Card::markAsDrawed);

        List<BuildingCard> buildings = ps.buildingCardIds().stream()
                .map(id -> (BuildingCard) catalog.get(id))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        buildings.forEach(Card::markAsDrawed);

        List<InventionType> inventions = ps.inventions().stream()
                .map(InventionType::valueOf)
                .collect(Collectors.toList());

        p.restoreState(
                ps.nuggets(), ps.prestige(),
                chars, buildings, inventions,
                ps.doublePointForBuilder(), ps.doublePointForRituals(),
                ps.noMalusForRituals(), ps.extraThreeStars(),
                ps.extraFoodOnTurnOrder(), ps.extraCard(),
                ps.setToCheck(), ps.setNumberForExtraFood(),
                ps.inventorsToCheck(), ps.foodDiscountFromBuildings()
        );
        return p;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UTILITY
    // ─────────────────────────────────────────────────────────────────────────

    private File saveFile(int gameId) {
        return new File(SAVE_DIR + "/game_" + gameId + ".json");
    }

    private List<Integer> toIdList(List<? extends Card> cards) {
        return cards.stream().map(Card::getID).collect(Collectors.toList());
    }

    private List<CardRef> toCardRefList(List<? extends Card> cards) {
        return cards.stream()
                .map(c -> new CardRef(c.getID(), c.isDrawed()))
                .collect(Collectors.toList());
    }

    /**
     * Resolves a list of {@link CardRef} objects into typed {@link Card} instances,
     * restoring the {@code drawed} flag on each resolved card.
     *
     * @param <T>     the expected card subtype
     * @param refs    list of card references to resolve
     * @param catalog mapping from card ID to card instance
     * @param type    the expected runtime type
     * @return list of resolved, typed card instances
     */
    @SuppressWarnings("unchecked")
    private <T extends Card> List<T> resolveCardRefs(List<CardRef> refs, Map<Integer, Card> catalog, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (CardRef ref : refs) {
            Card c = catalog.get(ref.cardId());
            if (c != null && type.isInstance(c)) {
                if (ref.drawed()) c.markAsDrawed();
                result.add((T) c);
            }
        }
        return result;
    }

    /**
     * Resolves a list of card IDs into typed {@link Card} instances.
     *
     * @param <T>     the expected card subtype
     * @param ids     list of card IDs to resolve
     * @param catalog mapping from card ID to card instance
     * @param type    the expected runtime type
     * @return list of resolved, typed card instances
     */
    @SuppressWarnings("unchecked")
    private <T extends Card> List<T> resolveIds(List<Integer> ids, Map<Integer, Card> catalog, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (int id : ids) {
            Card c = catalog.get(id);
            if (c != null && type.isInstance(c)) result.add((T) c);
        }
        return result;
    }
}
