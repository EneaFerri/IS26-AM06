package it.polimi.ingsw.model.cards.Buildings;

import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.cards.CharacterCard;

public class BuildingEnd extends BuildingCard {
    private final CharacterType characterToConsider;
    private final int prestigeEndEffect;

    public BuildingEnd(int cardID, Age cardAge, int foodCost, int prestigePoint,
                       CharacterType characterToConsider, int prestigeEndEffect) {
        super(cardID, cardAge, foodCost, prestigePoint);
        this.characterToConsider = characterToConsider;
        this.prestigeEndEffect = prestigeEndEffect;
    }

    public CharacterType getCharacterToConsider() {
        return characterToConsider;
    }

    public int getPrestigeEndEffect() {
        return prestigeEndEffect;
    }

    // Raga occhio: questo metodo va chiamato SOLO alla fine della partita (GamePhase.END o simile)
    // altrimenti il giocatore si farma punti gratis a ogni turno
    public void applyEndEffect(Player player) {
        int count = 0;

        // Conto quanti personaggi di quel tipo ha la tribù
        for (CharacterCard c : player.getCharacterCards()) {
            if (c.getCharacterType() == this.characterToConsider) {
                count++;
            }
        }

        int prestigeToAdd = count * this.prestigeEndEffect;

        // TODO: se implementiamo un metodo addEndGamePoints nel Player è più pulito per l'information hiding
        player.pointsFromEndEffect += prestigeToAdd;
    }
}
