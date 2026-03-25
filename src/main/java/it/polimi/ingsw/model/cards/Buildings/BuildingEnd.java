package it.polimi.ingsw.model.cards.Buildings;

import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.cards.CharacterCard;



public class BuildingEnd extends BuildingCard {
    private final CharacterType characterToConsider;
    private final int prestigeEndEffect;
    private final boolean bonusOnCompleteSet;

    public BuildingEnd(int cardID, Age cardAge, int foodCost, int prestigePoint,
                       CharacterType characterToConsider, int prestigeEndEffect, boolean bonusOnCompleteSet) {
        super(cardID, cardAge, foodCost, prestigePoint);
        this.characterToConsider = characterToConsider;
        this.prestigeEndEffect = prestigeEndEffect;
        this.bonusOnCompleteSet = bonusOnCompleteSet;
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

        //caso speciale building da 25 punti
        if(characterToConsider == CharacterType.NONE){
            player.pointsFromEndEffect += prestigeEndEffect; //cioè +25
            return;
        }

        // Per la carta che da 6 punti se hai tutti i 6 tipi di personaggi
        // la logica di controllo della condizione è dentro player
        if (bonusOnCompleteSet) {
            int nSets = player.countCompleteCharacterSets();
            player.pointsFromEndEffect += nSets * prestigeEndEffect; // se prestigeEndEffect=6 => +6 per ogni set

            return;
        }

        int count = 0;

        // Conto quanti personaggi di quel tipo ha la tribù
        for (CharacterCard c : player.getCharacterCards()) {
            if (c.getCharacterType() == this.characterToConsider) {
                count++;
            }
        }

        int prestigeToAdd = count * this.prestigeEndEffect;

        player.pointsFromEndEffect += prestigeToAdd;

    }
}
