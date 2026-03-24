package it.polimi.ingsw.model.cards.Events;

import it.polimi.ingsw.model.cards.Characters.Shaman;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RitualTest {

    @Test
    void playerWithMostStarsGetsPrestigeBonus() {
        Player p1 = new Player("Mario", new Totem(TotemColor.RED));
        Player p2 = new Player("Luigi", new Totem(TotemColor.BLUE));

        // p1 = 3 stelle totali
        p1.addCharacterCard(new Shaman(1, Age.Era_I, 2, 1));
        p1.addCharacterCard(new Shaman(2, Age.Era_I, 2, 2));

        // p2 = 1 stella totale
        p2.addCharacterCard(new Shaman(3, Age.Era_I, 2, 1));

        int p1PrestigeBefore = p1.getPrestige();
        int p2PrestigeBefore = p2.getPrestige();

        Ritual ritual = new Ritual(1, Age.Era_I, 4, 2);
        // maxBonus = 4
        // maxMalus = 2

        ritual.resolve(List.of(p1, p2));

        assertEquals(p1PrestigeBefore + 4, p1.getPrestige());
        assertEquals(p2PrestigeBefore - 2, p2.getPrestige());
    }

    @Test
    void playersWithSameMaxStarsBothGetBonus() {
        Player p1 = new Player("Mario", new Totem(TotemColor.RED));
        Player p2 = new Player("Luigi", new Totem(TotemColor.BLUE));
        Player p3 = new Player("Peach", new Totem(TotemColor.YELLOW));

        // p1 = 2 stelle
        p1.addCharacterCard(new Shaman(1, Age.Era_I, 2, 2));

        // p2 = 2 stelle
        p2.addCharacterCard(new Shaman(2, Age.Era_I, 2, 2));

        // p3 = 1 stella
        p3.addCharacterCard(new Shaman(3, Age.Era_I, 2, 1));

        int p1PrestigeBefore = p1.getPrestige();
        int p2PrestigeBefore = p2.getPrestige();
        int p3PrestigeBefore = p3.getPrestige();

        Ritual ritual = new Ritual(1, Age.Era_I, 3, 1);

        ritual.resolve(List.of(p1, p2, p3));

        assertEquals(p1PrestigeBefore + 3, p1.getPrestige());
        assertEquals(p2PrestigeBefore + 3, p2.getPrestige());
        assertEquals(p3PrestigeBefore - 1, p3.getPrestige());
    }

    @Test
    void playersWithSameMinStarsBothGetMalus() {
        Player p1 = new Player("Mario", new Totem(TotemColor.RED));
        Player p2 = new Player("Luigi", new Totem(TotemColor.BLUE));
        Player p3 = new Player("Peach", new Totem(TotemColor.YELLOW));

        // p1 = 3 stelle
        p1.addCharacterCard(new Shaman(1, Age.Era_I, 2, 3));

        // p2 = 1 stella
        p2.addCharacterCard(new Shaman(2, Age.Era_I, 2, 1));

        // p3 = 1 stella
        p3.addCharacterCard(new Shaman(3, Age.Era_I, 2, 1));

        int p1PrestigeBefore = p1.getPrestige();
        int p2PrestigeBefore = p2.getPrestige();
        int p3PrestigeBefore = p3.getPrestige();

        Ritual ritual = new Ritual(1, Age.Era_I, 2, 4);

        ritual.resolve(List.of(p1, p2, p3));

        assertEquals(p1PrestigeBefore + 2, p1.getPrestige());
        assertEquals(p2PrestigeBefore - 4, p2.getPrestige());
        assertEquals(p3PrestigeBefore - 4, p3.getPrestige());
    }

    @Test
    void singlePlayerWithOnlyOneValueGetsBothBonusAndMalusAccordingToCurrentImplementation() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));

        player.addCharacterCard(new Shaman(1, Age.Era_I, 2, 2));

        int prestigeBefore = player.getPrestige();

        Ritual ritual = new Ritual(1, Age.Era_I, 5, 3);

        ritual.resolve(List.of(player));

        // Con l'implementazione attuale: maxStars == minStars,
        // quindi il player riceve sia bonus che malus.
        assertEquals(prestigeBefore + 2, player.getPrestige());
    }
}