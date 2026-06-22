package it.polimi.ingsw.model.cards;

import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.model.enums.Age;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The main card deck for the game.
 *
 * <p>Loads all character, building, and event cards from JSON resource files at
 * construction time. Provides methods to select the correct subsets of cards
 * for a given number of players and era.</p>
 */
public class Deck
{
    private List<CharacterCard> characterCards = new ArrayList<>();
    private List<BuildingCard> buildingCards = new ArrayList<>();
    private List<EventCard> eventCards = new ArrayList<>();

    private List<Card> cards;

    /**
     * Returns all cards in the deck (character, building, and event cards combined).
     *
     * @return a new list containing all cards
     */
    public List<Card> getAllCards()
    {
        cards = new ArrayList<>();
        cards.addAll(characterCards);
        cards.addAll(buildingCards);
        cards.addAll(eventCards);

        return cards;
    }

    /**
     * Creates a new deck by loading all cards from the JSON resource files.
     *
     * @throws RuntimeException if the JSON files cannot be loaded
     */
    public Deck() {
        ObjectMapper mapper = new ObjectMapper();

        try{
            loadCharacters(mapper);
            loadBuildings(mapper);
            loadEvents(mapper);
        }catch(IOException e){
            throw new RuntimeException("errore caricamento da JSON", e);
        }
    }

    /**
     * Loads character cards from the characters.json resource file.
     *
     * @param mapper the Jackson ObjectMapper to use for parsing
     * @throws IOException if the resource file cannot be read
     */
    private void loadCharacters(ObjectMapper mapper) throws IOException {
        JsonNode root = mapper.readTree(
                getClass().getResourceAsStream("/characters.json")
        );
        for (JsonNode node : root) {
            characterCards.add(DeckConfiguration.createCharacterCard(node));
        }
    }

    /**
     * Loads building cards from the buildings.json resource file.
     *
     * @param mapper the Jackson ObjectMapper to use for parsing
     * @throws IOException if the resource file cannot be read
     */
    private void loadBuildings(ObjectMapper mapper) throws IOException {
        JsonNode root = mapper.readTree(
                getClass().getResourceAsStream("/buildings.json")
        );
        for (JsonNode node : root) {
            buildingCards.add(DeckConfiguration.createBuildingCard(node));
        }
    }

    /**
     * Loads event cards from the events.json resource file.
     *
     * @param mapper the Jackson ObjectMapper to use for parsing
     * @throws IOException if the resource file cannot be read
     */
    private void loadEvents(ObjectMapper mapper) throws IOException {
        JsonNode root = mapper.readTree(
                getClass().getResourceAsStream("/events.json")
        );
        for (JsonNode node : root) {
            eventCards.add(DeckConfiguration.createEventCard(node));
        }
    }


    /**
     * Selects and returns the building cards to be used in a game with the given number of players.
     *
     * <p>The selection picks a player-count-specific number of cards from each era,
     * shuffled randomly.</p>
     *
     * @param numberOfPlayers the number of players in the game (must be between 2 and 5)
     * @return the list of building cards to use, ordered by era
     * @throws IllegalArgumentException if the number of players is not between 2 and 5
     */
    public List<BuildingCard> takeBuldingInGame(int numberOfPlayers){

        int from_ERA_I = 0, from_ERA_II = 0, from_ERA_III = 0;

        if(numberOfPlayers<2 || numberOfPlayers>5){
            throw new IllegalArgumentException("Number of players must be between 2 and 5");
        }

        if(numberOfPlayers==2){
            from_ERA_I = 1;
            from_ERA_II = 2;
            from_ERA_III = 3;

        }else if(numberOfPlayers==3){
            from_ERA_I = 2;
            from_ERA_II = 2;
            from_ERA_III = 4;

        }else if(numberOfPlayers==4){
            from_ERA_I = 2;
            from_ERA_II = 3;
            from_ERA_III = 4;

        }else if(numberOfPlayers==5){
            from_ERA_I = 2;
            from_ERA_II = 3;
            from_ERA_III = 5;

        }

        return buildingsOrderedWithERA(buildingCards, from_ERA_I, from_ERA_II, from_ERA_III) ;
    }

    /**
     * Shuffles and selects building cards in era order according to the specified counts.
     *
     * @param buildingCards the full list of building cards to select from
     * @param from_ERA_I    number of Era I buildings to include
     * @param from_ERA_II   number of Era II buildings to include
     * @param from_ERA_III  number of Era III buildings to include
     * @return the selected building cards ordered by era
     * @throws IllegalArgumentException if any era count is zero
     */
    private List<BuildingCard> buildingsOrderedWithERA(List<BuildingCard> buildingCards, int from_ERA_I, int from_ERA_II, int from_ERA_III){

        List<BuildingCard> buildingCardsOrdered = new ArrayList<>();

        if(from_ERA_I==0 || from_ERA_II==0 || from_ERA_III==0){
            throw new IllegalArgumentException("Error in buildings ordering");
        }

        Collections.shuffle(buildingCards);

        for(BuildingCard bCard : buildingCards){
            if(from_ERA_I==0){
                break;
            }
            if(bCard.getAge() == Age.Era_I){
                from_ERA_I--;
                buildingCardsOrdered.add(bCard);
            }
        }

        for(BuildingCard bCard : buildingCards){
            if(from_ERA_II==0){
                break;
            }
            if(bCard.getAge() == Age.Era_II){
                from_ERA_II--;
                buildingCardsOrdered.add(bCard);
            }
        }

        for(BuildingCard bCard : buildingCards){
            if(from_ERA_III==0){
                break;
            }
            if(bCard.getAge() == Age.Era_III){
                from_ERA_III--;
                buildingCardsOrdered.add(bCard);
            }
        }

        return buildingCardsOrdered;

    }

    /**
     * Prepares and returns the shuffled list of tribe cards (characters and events) for the given era.
     *
     * @param numberOfPlayers the number of players in the game (must be between 2 and 5)
     * @param ERA             the era for which to prepare cards
     * @return a shuffled list of tribe cards available for the specified era and player count
     * @throws IllegalArgumentException if the number of players is not between 2 and 5
     */
    public List<TribeCard> prepareTribeCards(int numberOfPlayers, Age ERA){
        if(numberOfPlayers < 2 || numberOfPlayers > 5){
            throw new IllegalArgumentException("Number of players must be between 2 and 5");
        }

        List<TribeCard> tribeCards = new ArrayList<>();

        for(CharacterCard card : characterCards){
            if(card.getAge() == ERA && card.isAvailableForPlayers(numberOfPlayers)){
                tribeCards.add(card);
            }
        }

        for(EventCard card : eventCards){
            if(card.getAge() == ERA){
                tribeCards.add(card);
            }
        }

        Collections.shuffle(tribeCards);

        return tribeCards;
    }

    /**
     * Returns all final-era event cards (those belonging to {@link Age#Last_Event}).
     *
     * @return a list of final-era event cards
     */
    public List<EventCard> getFinalsEvents() {
        List<EventCard> finalEvents = new ArrayList<>();

        for(EventCard card : eventCards){
            if(card.getAge() == Age.Last_Event){
                finalEvents.add(card);
            }
        }
        return finalEvents;
    }
}
