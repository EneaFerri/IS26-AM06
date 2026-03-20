package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.enums.*;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.cards.EventCard;

public class BuildingCard extends Card {
    private final int foodCost;
    private final int prestigePoint;
    private final BuildingEffectTime effectTiming;

    private EventType eventToResponde;  //se non è una building con effetto evento sarà null

    private CharacterType characterToConsider; //sia per effetto su eventi che per effetto su endgame

    private int prestigeEndEffect; //se non è una building con effetto endgame sarà null/zero

    public BuildingCard(int cardID, Age cardAge, int foodCost, int prestigePoint,
                        BuildingEffectTime effectTiming, EventType eventToResponde,  CharacterType characterToConsider, int prestigeEndEffect) {
        super(cardID, cardAge);
        this.foodCost = foodCost;
        this.prestigePoint = prestigePoint;
        this.effectTiming = effectTiming;

        this.eventToResponde = eventToResponde;

        this.characterToConsider = characterToConsider;

        this.prestigeEndEffect = prestigeEndEffect;
    }

    public int getFoodCost() {
        return foodCost;
    }

    public int getPrestigePoint() {
        return prestigePoint;
    }

    public BuildingEffectTime getEffectTiming() {
        return effectTiming;
    }


    public void applyEventEffect(EventType event, Player player) {

        if(effectTiming != BuildingEffectTime.EVENT) return; //caso building non event

        if(eventToResponde==null || eventToResponde != event) return; // caso event in corso diverso da quello a cui risponde la building

        if (event == EventType.SUSTENANCE){
            int moreDiscount=0;

            for (CharacterCard c : player.getCharacterCards()){
                if(c.getCharacterType().equals(characterToConsider)){
                    moreDiscount++;
                }
            }

             player.foodDiscountFromBuildings+=moreDiscount;
        }

        if( event == EventType.PICTURES){

            int n = player.getNumArtists();
            player.addFood(n);
        }

        if(event == EventType.RITUAL){
            //TODO: piu complicato rispetto agli altri

        }

        if (event == EventType.HUNT){

            int n = player.getNumHunters();
            player.addFood(n);
            player.addPrestige(n);

        }
    }

    public void applyEachTurnEffect() {
        if(effectTiming != BuildingEffectTime.EACHTURN) return;

        //TODO: ?
    }

    public void applyEndEffect(Player player) { //TODO: per come implementato ora, gestisce solo le buildings con effetto "prestigo x personaggio", per le altre ending ?
        if(effectTiming != BuildingEffectTime.ENDOFGAME) return;

        int count=0;
        for( CharacterCard c : player.getCharacterCards()){
            if(c.getCharacterType().equals(characterToConsider)){
                count++;
            }
        }

        int prestigeToAdd= count*prestigeEndEffect;

        player.pointsFromEndEffect+=prestigeToAdd;
    }
}
