package it.polimi.ingsw.model.cards.Buildings;

import it.polimi.ingsw.model.cards.BuildingCard;
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

    @Override
    public int getEndEffectPoints(Player player) {
        // caso speciale: edificio che dà un bonus fisso finale (es. +25)
        if (characterToConsider == CharacterType.NONE) {
            return prestigeEndEffect;
        }

        int count = 0;

        // conto quanti personaggi del tipo richiesto ha il player
        for (CharacterCard c : player.getCharacterCards()) {
            if (c.getCharacterType() == this.characterToConsider) {
                count++;
            }
        }

        return count * this.prestigeEndEffect;
    }

    /*
    // Raga occhio: questo metodo va chiamato SOLO alla fine della partita (GamePhase.END o simile)
    // altrimenti il giocatore si farma punti gratis a ogni turno
    public void applyEndEffect(Player player) {

        //caso speciale building da 25 punti
        if(characterToConsider == CharacterType.NONE){
            player.pointsFromEndEffect += prestigeEndEffect; //cioè +25
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

        //TODO: manca gestione edificio "6 punti per set a fine partita", FORSE MEGLIO GESTIRE DIRETTAMENTE IN PLAYER???
    }
    */
}
