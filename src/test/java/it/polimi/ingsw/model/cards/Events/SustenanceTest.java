package it.polimi.ingsw.model.cards.Events;

import it.polimi.ingsw.model.cards.Characters.Artist;
import it.polimi.ingsw.model.cards.Characters.Collector;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SustenanceTest {

    @Test
    void playerPaysFoodForEachCharacter_whenEnoughFood() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));
        player.addFood(5);

        // 2 personaggi → costo 2
        player.addCharacterCard(new Artist(1, Age.Era_I, 2));
        player.addCharacterCard(new Artist(2, Age.Era_I, 2));

        Sustenance sustenance = new Sustenance(1, Age.Era_I, 2); // prestige malus = 2

        sustenance.resolve(List.of(player));

        assertEquals(3, player.getFood());
        assertEquals(0, player.getPrestige());
    }

    @Test
    void playerLosesPrestige_whenNotEnoughFood() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));
        player.addFood(1);

        // 2 personaggi → servono 2 cibi, ma ne ha 1
        player.addCharacterCard(new Artist(1, Age.Era_I, 2));
        player.addCharacterCard(new Artist(2, Age.Era_I, 2));

        Sustenance sustenance = new Sustenance(1, Age.Era_I, 3); // malus = 3

        sustenance.resolve(List.of(player));

        // usa tutto il cibo
        assertEquals(0, player.getFood());

        // manca 1 cibo → perde 3 prestigio
        assertEquals(-3, player.getPrestige());
    }

    @Test
    void collectorsReduceFoodCost() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));
        player.addFood(1);

        // 2 personaggi → costo 2
        player.addCharacterCard(new Artist(1, Age.Era_I, 2));
        player.addCharacterCard(new Artist(2, Age.Era_I, 2));

        // 1 collector → sconto 3 → costo diventa 0
        player.addCharacterCard(new Collector(3, Age.Era_I, 2));

        Sustenance sustenance = new Sustenance(1, Age.Era_I, 2);

        sustenance.resolve(List.of(player));

        // non paga niente
        assertEquals(1, player.getFood());

        // nessuna penalità
        assertEquals(0, player.getPrestige());
    }
}