package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.enums.Age;
import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck
{
    private List<Card> cards = new ArrayList<>();

    public Deck() {
        Gson gson = new Gson();
        try (Reader reader = new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("cards.json"))) {


            DeckConfiguration config = gson.fromJson(reader, DeckConfiguration.class);

            if (config.hunters != null) cards.addAll(config.hunters);
            if (config.builders != null) cards.addAll(config.builders);
            if (config.collectors != null) cards.addAll(config.collectors);
            if (config.artists != null) cards.addAll(config.artists);
            if (config.inventors != null) cards.addAll(config.inventors);
            if (config.shamans != null) cards.addAll(config.shamans);
            if (config.hunts != null) cards.addAll(config.hunts);
            if (config.pictures != null) cards.addAll(config.pictures);
            if (config.rituals != null) cards.addAll(config.rituals);
            if (config.sustenances != null) cards.addAll(config.sustenances);
            if (config.buildingEnds != null) cards.addAll(config.buildingEnds);
            if (config.buildingEachTurns != null) cards.addAll(config.buildingEachTurns);
            if (config.buildingEvents != null) cards.addAll(config.buildingEvents);

        } catch (Exception e) {
            System.err.println("Errore durante la lettura del mazzo: " + e.getMessage());
        }
    }

    public void addCard(Card c){
        cards.add(c);
    }

    public boolean isEmpty(){
        return cards.isEmpty();
    }

    public int size(){
        return cards.size();
    }

    public List<BuildingCard> takeBuldingInGame(int numberOfPlayers){

        List<BuildingCard> buildingCards = new ArrayList<>();

        for(Card card : cards){
            if(card instanceof BuildingCard){
                buildingCards.add((BuildingCard) card);
            }
        }

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
        List<TribeCard> tribeCards = new ArrayList<>();
        if(numberOfPlayers<2 || numberOfPlayers>5){
            throw new IllegalArgumentException("Number of players must be between 2 and 5");
        }

        for(Card card : cards){
            if(card instanceof TribeCard tribeCard){
                if(tribeCard.getAge() == ERA){
                    tribeCards.add(tribeCard);
                }
            }
        }

        for(TribeCard tribeCard : tribeCards){
            if(tribeCard instanceof CharacterCard characterCard){
                if(characterCard.getTag() > numberOfPlayers ){
                    tribeCards.remove(characterCard);
                }
            }
        }

        return tribeCards;
    }

}
