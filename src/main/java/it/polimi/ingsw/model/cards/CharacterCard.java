package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;

public class CharacterCard extends TribeCard {
    private final int tag; //indica il numerino, default 2 gg, altrimenti +3,+4,5

    private final CharacterType characterType;

    public CharacterCard(int cardID, Age cardAge, int tag, CharacterType characterType) {
        super(cardID, cardAge);
        this.tag = tag;
        this.characterType = characterType;
    }

    public int getTag() {
        return tag;
    }
    public CharacterType getCharacterType() {
        return characterType;
    }
}
