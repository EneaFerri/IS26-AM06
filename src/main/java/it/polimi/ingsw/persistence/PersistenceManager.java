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
 * Gestisce il salvataggio e il ripristino delle partite su disco (FA Persistenza).
 *
 * Ogni partita viene salvata come saves/game_{id}.json usando Jackson.
 * Il salvataggio avviene dopo ogni transizione di turno; il file viene eliminato
 * quando la partita termina normalmente.
 *
 * Al riavvio del server, loadAll() carica tutti i file presenti e restore()
 * ricostruisce il grafo di oggetti completo usando il catalogo carte di un Deck
 * fresco (già presente nel Game dopo new Game(id)).
 */
public class PersistenceManager {

    private static final String SAVE_DIR = "saves";
    private static PersistenceManager instance;

    private final ObjectMapper mapper = new ObjectMapper();

    private PersistenceManager() {
        File dir = new File(SAVE_DIR);
        if (!dir.exists()) dir.mkdirs();
    }

    public static synchronized PersistenceManager getInstance() {
        if (instance == null) instance = new PersistenceManager();
        return instance;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SAVE
    // ─────────────────────────────────────────────────────────────────────────

    /** Serializza lo stato corrente della partita su disco. */
    public void save(Game game) {
        try {
            GameSnapshot snap = toSnapshot(game);
            File file = saveFile(game.getGameID());
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, snap);
        } catch (Exception e) {
            System.err.println("[PersistenceManager] save game " + game.getGameID() + " failed: " + e.getMessage());
        }
    }

    /** Elimina il file di salvataggio (chiamato quando la partita finisce normalmente). */
    public void delete(int gameId) {
        File file = saveFile(gameId);
        if (file.exists() && !file.delete()) {
            System.err.println("[PersistenceManager] impossibile eliminare " + file.getName());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LOAD + RESTORE
    // ─────────────────────────────────────────────────────────────────────────

    /** Carica tutti i file saves/game_*.json e li deserializza in snapshot. */
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
     * Ricostruisce un oggetto Game completo a partire da un GameSnapshot.
     *
     * Strategia:
     *  1. Crea new Game(id) → il costruttore crea un mainDeck fresco con tutte le carte
     *  2. Costruisce un catalogo cardId → Card da mainDeck.getAllCards()
     *  3. Ricostruisce i Player (con le loro carte, senza side-effect)
     *  4. Ricostruisce le righe board, i totem sugli spazi e sul TurnOrder
     *  5. Chiama game.restorePersistedState() che imposta tutti i campi non-final
     */
    public Game restore(GameSnapshot snap) {
        Game game = new Game(snap.gameId());

        // Catalogo carte: id → oggetto
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

        // ── Deck residui ───────────────────────────────────────────────────
        List<TribeCard>    deckI     = resolveIds(snap.deckEraI(),         catalog, TribeCard.class);
        List<TribeCard>    deckII    = resolveIds(snap.deckEraII(),        catalog, TribeCard.class);
        List<TribeCard>    deckIII   = resolveIds(snap.deckEraIII(),       catalog, TribeCard.class);
        List<EventCard>    finalEvts = resolveIds(snap.finalEventIds(),    catalog, EventCard.class);
        List<BuildingCard> buildings = resolveIds(snap.buildingsInGameIds(), catalog, BuildingCard.class);

        // ── Totem su board spaces: lettera → TotemColor name ──────────────
        Map<Character, String> spaceTotemColors = new HashMap<>();
        for (BoardSnapshot.SpaceSnap ss : snap.board().spaces()) {
            if (ss.totemColor() != null) {
                spaceTotemColors.put(ss.letter(), ss.totemColor());
            }
        }

        // ── Enum da stringa ────────────────────────────────────────────────
        GameState state = GameState.valueOf(snap.gameState());
        Age age         = Age.valueOf(snap.currentAge());

        // ── Costruisci il board ripristinato (con removeFromOfferField per il numero giocatori) ─
        // Il board fresco ha TUTTI gli spazi (A-G); ricalcoliamo quelli in gioco
        game.getBoard().prepareGameBoardSpace(snap.numberOfPlayers());

        // ── Richiama restorePersistedState sul Game ────────────────────────
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
    //  CONVERSIONE Game → GameSnapshot
    // ─────────────────────────────────────────────────────────────────────────

    private GameSnapshot toSnapshot(Game game) {
        // Players
        List<PlayerSnapshot> playerSnaps = game.getPlayers().stream()
                .map(this::playerToSnapshot)
                .collect(Collectors.toList());

        // Board
        BoardSnapshot boardSnap = boardToSnapshot(game);

        // TurnOrder
        TurnOrderSnapshot toSnap = turnOrderToSnapshot(game);

        // Deck residui
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
                game.getTopPicks(), game.getBottomPicks()
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
    //  RIPRISTINO PLAYER
    // ─────────────────────────────────────────────────────────────────────────

    private Player restorePlayer(PlayerSnapshot ps, Map<Integer, Card> catalog) {
        TotemColor color = TotemColor.valueOf(ps.totemColor());
        Player p = new Player(ps.nickname(), new Totem(color));

        List<CharacterCard> chars = ps.characterCardIds().stream()
                .map(id -> (CharacterCard) catalog.get(id))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        // Imposta drawed sulle carte personaggio
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

    /** Risolve una lista di CardRef in oggetti Card del tipo richiesto, impostando il flag drawed. */
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

    /** Risolve una lista di ID in oggetti Card del tipo richiesto. */
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
