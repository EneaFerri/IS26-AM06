package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.enums.Age;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeckTest {

    private Deck deck;

    @BeforeEach
    void setUp() {
        // Istanziare il Deck qui chiama in automatico i metodi loadCharacters, loadBuildings e loadEvents!
        // Questo ci regala una coverage altissima sul costruttore e la lettura JSON.
        // N.B. Assicurati che i file .json siano dentro la cartella "src/main/resources".
        assertDoesNotThrow(() -> {
            deck = new Deck();
        }, "Il costruttore non dovrebbe lanciare eccezioni se i JSON sono posizionati correttamente.");
    }

    @Test
    void getAllCardsShouldCombineAllLists() {
        List<Card> allCards = deck.getAllCards();

        assertNotNull(allCards);
        assertFalse(allCards.isEmpty(), "La lista di tutte le carte non deve essere vuota.");
    }

    @Test
    void prepareTribeCardsShouldThrowExceptionForInvalidPlayerCount() {
        // Verifica del controllo sulle eccezioni per numero di giocatori errato
        IllegalArgumentException exceptionLow = assertThrows(IllegalArgumentException.class,
                () -> deck.prepareTribeCards(1, Age.Era_I));
        assertEquals("Number of players must be between 2 and 5", exceptionLow.getMessage());

        IllegalArgumentException exceptionHigh = assertThrows(IllegalArgumentException.class,
                () -> deck.prepareTribeCards(6, Age.Era_I));
        assertEquals("Number of players must be between 2 and 5", exceptionHigh.getMessage());
    }

    @Test
    void takeBuldingInGameShouldReturnExactAmountsPerAgeBasedOnPlayers() {
        // Simuliamo una partita a 3 giocatori
        int players = 3;
        List<BuildingCard> buildingsInGame = deck.takeBuldingInGame(players);

        assertNotNull(buildingsInGame);

        // Contiamo quante carte ci sono per ogni Era
        long era1Count = buildingsInGame.stream().filter(c -> c.getAge() == Age.Era_I).count();
        long era2Count = buildingsInGame.stream().filter(c -> c.getAge() == Age.Era_II).count();
        long era3Count = buildingsInGame.stream().filter(c -> c.getAge() == Age.Era_III).count();

        // Verifichiamo che i conti tornino esattamente con la logica del tuo Deck.java per 3 giocatori!
        assertEquals(2, era1Count, "Per 3 giocatori dovrebbero esserci 2 carte Edificio per l'Era I.");
        assertEquals(2, era2Count, "Per 3 giocatori dovrebbero esserci 2 carte Edificio per l'Era II.");
        assertEquals(4, era3Count, "Per 3 giocatori dovrebbero esserci 4 carte Edificio per l'Era III.");
    }

    @Test
    void getFinalsEventsShouldReturnOnlyLastEvents() {
        List<EventCard> finalEvents = deck.getFinalsEvents();

        assertNotNull(finalEvents);
        assertFalse(finalEvents.isEmpty(), "Dovrebbe trovare almeno un evento finale.");

        // Nel tuo gioco, gli eventi finali hanno l'enum Age.Last_Event!
        for (EventCard event : finalEvents) {
            assertEquals(Age.Last_Event, event.getAge(), "Gli eventi finali devono avere l'Era impostata su Last_Event.");
        }

        // Visto che sappiamo esserci esattamente 2 eventi finali nel gioco reale, possiamo testare anche quello!
        assertEquals(2, finalEvents.size(), "Dovrebbero esserci esattamente 2 eventi finali nel mazzo.");
    }
/*
    @Test
    void takeTribeInGameShouldExtractCorrectMixOfCharactersAndEvents() {
        // Scegliamo un test base: 3 giocatori in Era I
        int players = 3;
        List<TribeCard> era1Tribes = deck.takeTribeInGame(Age.Era_I, players);

        assertNotNull(era1Tribes);

        // La tua logica dice: numeroCarteTribu = (players * 4) - 1 = (3 * 4) - 1 = 11 personaggi.
        // A questi si sommano 2 eventi. Totale atteso: 13 carte.
        assertEquals(13, era1Tribes.size(), "Per 3 giocatori in Era I dovrebbero essere pescate 11 personaggi e 2 eventi (Totale 13).");

        // Verifichiamo che tutte le carte estratte siano effettivamente dell'Era I
        for (TribeCard card : era1Tribes) {
            assertEquals(Age.Era_I, card.getAge(), "Il mazzo in gioco non deve contenere carte di ere future.");
        }
    }*/
}
