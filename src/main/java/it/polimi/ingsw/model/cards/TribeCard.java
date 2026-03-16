package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.TribeType;

public abstract class TribeCard extends Card {
    /*
    private TribeType typeCardTribe; //0->Character, 1->Event

    É DAVVERO NECESSARIO? SECONDO ME NO, ci ho ragionato ora e non ha senso duplicare il tipo di carta:
    semplicemente abbiamo le due sottoclassi charachter e event, e nel momento in cui chiameremo una delle due
    sottoclassi, conosceremo già il tipo, quindi forse inutile

    NEL CASO TOGLIAMO ANCHE L'ENUM
     */

    public TribeCard(int cardID, Age cardAge) {
        super(cardID, cardAge);
    }

    /*
    public TribeType getType() {
        return typeCardTribe;
    }
     */

}
