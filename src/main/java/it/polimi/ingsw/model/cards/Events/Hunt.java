package it.polimi.ingsw.model.cards.Events;

import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.cards.EventCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.EventType;
import it.polimi.ingsw.model.player.Player;

import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.enums.CharacterType;

import java.util.List;

public class Hunt extends EventCard {
// RAGASSUOLI QUI HO CAMBIATO NOMI PERCHÉ NOI AVEVAMO MESSO TUTTO CON IL NOME "PRICE" MA IN REALTA
    // NON SI PAGA NIENTE DURANTE L'EVENTO DI CACCIA, QUINDI HO MESSO BONUS (sbaglio?)
    private static final int FOOD_BONUS = 1;
    private final int prestigeBonus;

    public Hunt(int cardID, Age cardAge, int prestigeBonus) {
        super(cardID, cardAge, EventType.HUNT);
        this.prestigeBonus = prestigeBonus;
    }


    public int getPrestigeBonus() {
        return prestigeBonus;
    }

    @Override
    public void resolve(List<Player> players) {
        for (Player player : players) {
            int hunters = 0;

            for(BuildingCard bCard : player.getBuildingCards()){
                bCard.applyEventEffect(EventType.HUNT, player);
            }

            for (CharacterCard card : player.getCharacterCards()) {
                if (card.getCharacterType() == CharacterType.HUNTER) {
                    hunters++;
                }
            }

            player.addFood(hunters * FOOD_BONUS);
            player.addPrestige(hunters * prestigeBonus);
        }
    }
}
