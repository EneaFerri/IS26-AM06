package it.polimi.ingsw.model.cards;

import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.model.cards.Characters.*;
import it.polimi.ingsw.model.cards.Events.*;
import it.polimi.ingsw.model.cards.Buildings.*;
import it.polimi.ingsw.model.enums.*;
import jdk.jfr.Event;

import java.util.Collection;
import java.util.List;

public abstract class DeckConfiguration {

    public static CharacterCard createCharacterCard(JsonNode node){

        int id = node.get("id").asInt();
        Age era = Age.valueOf(node.get("era").asText());
        int tag = node.get("tag").asInt();
        CharacterType type = CharacterType.valueOf(node.get("type").asText());

        if(type.equals(CharacterType.ARTIST)){
            return new Artist(id, era, tag);

        } else if (type.equals(CharacterType.BUILDER)) {
            int pValue = node.get("prestigeValue").asInt();
            int bDiscount = node.get("buildingDiscount").asInt();
            return new Builder(id, era, tag, pValue, bDiscount);

        } else if (type.equals(CharacterType.COLLECTOR)) {
            return new Collector(id, era, tag);

        } else if (type.equals(CharacterType.HUNTER)) {
            int nuggetFlag = node.get("nugget").asInt();
            return new Hunter(id, era, tag, nuggetFlag);

        } else if (type.equals(CharacterType.INVENTOR)) {
            InventionType inventionType = InventionType.valueOf(node.get("inventionType").asText());
            return new Inventor(id, era, tag, inventionType);

        } else if (type.equals(CharacterType.SHAMAN)) {
            int stars = node.get("stars").asInt();
            return new Shaman(id, era, tag, stars);

        }else {
            System.out.println("Errore character type");
            return null;
        }

    }

    public static BuildingCard createBuildingCard(JsonNode node){

        int id = node.get("id").asInt();
        Age era = Age.valueOf(node.get("era").asText());
        int foodCost = node.get("foodCost").asInt();
        int prestigePoints = node.get("prestigePoints").asInt();

        String buildingType = node.get("buildingType").asText();

        if(buildingType.equals("eachTurn")){
            BuildingEachTurnType dettailedType = BuildingEachTurnType.valueOf(node.get("eachTurnType").asText());
            return new BuildingEachTurn(id, era, foodCost, prestigePoints, dettailedType);

        }else if(buildingType.equals("end")){
            CharacterType charType = CharacterType.valueOf(node.get("charType").asText());
            int prestigeEndEffect = node.get("prestigeEndEffect").asInt();
            return new BuildingEnd(id, era, foodCost, prestigePoints, charType, prestigeEndEffect);

        } else if (buildingType.equals("event")) {
            EventType eventType = EventType.valueOf(node.get("eventType").asText());
            CharacterType charType = CharacterType.valueOf(node.get("charType").asText());
            return new BuildingEvent(id, era, foodCost, prestigePoints, eventType, charType);

        }else{
            System.out.println("Errore building type");
            return null;
        }
    }

    public  static EventCard createEventCard(JsonNode node){

        int id = node.get("id").asInt();
        Age era = Age.valueOf(node.get("era").asText());
        EventType eventType = EventType.valueOf(node.get("eventType").asText());

        if(eventType.equals(EventType.HUNT)){
            int bonus = node.get("prestigeBonus").asInt();
            return new Hunt(id, era, bonus);

        } else if (eventType.equals(EventType.PICTURES)) {
            int min = node.get("minArtists").asInt();
            int bonus = node.get("prestigeBonus").asInt();
            int malus = node.get("prestigeMalus").asInt();
            return new Pictures(id, era, min, malus, bonus);

        } else if (eventType.equals(EventType.RITUAL)) {
            int maxBonus = node.get("prestigeBonus").asInt();
            int maxMalus = node.get("prestigeMalus").asInt();
            return new Ritual(id, era, maxBonus, maxMalus);

        } else if (eventType.equals(EventType.SUSTENANCE)) {
            int prestigeMalus = node.get("prestigeMalus").asInt();
            return new Sustenance(id, era, prestigeMalus);

        }else  {
            System.out.println("Errore event type");
            return null;
        }

    }


}