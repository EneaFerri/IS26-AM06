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

public class Deck
{
    private List<CharacterCard> characterCards = new ArrayList<>();
    private List<BuildingCard> buildingCards = new ArrayList<>();
    private List<EventCard> eventCards = new ArrayList<>();

    private List<Card> cards;

    //se servisse risalire all'intero mazzo di carte
    public List<Card> getAllCards()
    {
        cards = new ArrayList<>();
        cards.addAll(characterCards);
        cards.addAll(buildingCards);
        cards.addAll(eventCards);

        return cards;
    }

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

    private void loadCharacters(ObjectMapper mapper) throws IOException {
        JsonNode root = mapper.readTree(
                getClass().getResourceAsStream("/characters.json")
        );
        for (JsonNode node : root) {
            characterCards.add(DeckConfiguration.createCharacterCard(node));
        }
    }

    private void loadBuildings(ObjectMapper mapper) throws IOException {
        JsonNode root = mapper.readTree(
                getClass().getResourceAsStream("/buildings.json")
        );
        for (JsonNode node : root) {
            buildingCards.add(DeckConfiguration.createBuildingCard(node));
        }
    }

    private void loadEvents(ObjectMapper mapper) throws IOException {
        JsonNode root = mapper.readTree(
                getClass().getResourceAsStream("/events.json")
        );
        for (JsonNode node : root) {
            eventCards.add(DeckConfiguration.createEventCard(node));
        }
    }


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

    private List<BuildingCard> buildingsOrderedWithERA(List<BuildingCard> buildingCards, int from_ERA_I, int from_ERA_II, int from_ERA_III){

        List<BuildingCard> buildingCardsOrdered = new ArrayList<>();

        if(from_ERA_I==0 || from_ERA_II==0 || from_ERA_III==0){
            throw new IllegalArgumentException("Error in buildings ordering");
        }

        //mischio tutte le buildings
        Collections.shuffle(buildingCards);

        //ERA_I
        for(BuildingCard bCard : buildingCards){
            if(from_ERA_I==0){
                break;
            }
            if(bCard.getAge() == Age.Era_I){
                from_ERA_I--;
                buildingCardsOrdered.add(bCard);
            }
        }

        //ERA_II
        for(BuildingCard bCard : buildingCards){
            if(from_ERA_II==0){
                break;
            }
            if(bCard.getAge() == Age.Era_II){
                from_ERA_II--;
                buildingCardsOrdered.add(bCard);
            }
        }

        //ERA_III
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
