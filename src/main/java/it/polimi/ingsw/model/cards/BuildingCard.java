package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.EventType;
import it.polimi.ingsw.model.player.Player;
// import it.polimi.ingsw.model.enums.*; non più necessaria

public abstract class BuildingCard extends Card {
    private final int foodCost;
    private final int prestigePoint;

    public BuildingCard(int cardID, Age cardAge, int foodCost, int prestigePoint) {
        super(cardID, cardAge);
        this.foodCost = foodCost;
        this.prestigePoint = prestigePoint;
    }

    public int getFoodCost() {
        return this.foodCost;
    }

    public int getPrestigePoint() {
        return this.prestigePoint;
    }

    public void applyEventEffect(EventType eventType,  Player player) {
        //non fa nulla qui dentro, serve solo per override
    }

    public void onAddedToPlayer(Player player) {
        // default: NON FA NIENTE
    }

    public int getEndEffectPoints(Player player) {
        return 0;
    }

    @Override
    public void pick(Player player, Game game) {
        game.pickBuildingCard(player, this);
    }

    @Override
    public boolean isBuilding() {
        return true;
    }

    public String toString(){
        return super.toString() + ", food cost: " + foodCost + ", prestige point: " + prestigePoint;
    }
}
