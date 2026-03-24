package it.polimi.ingsw.model.cards.Events;

import it.polimi.ingsw.model.cards.Characters.Hunter;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HuntTest {

    @Test
    void playerGetsFoodAndPrestigeForEachHunter() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));

        // 2 hunter
        player.addCharacterCard(new Hunter(1, Age.Era_I, 2,0));
        player.addCharacterCard(new Hunter(2, Age.Era_I, 2,0));

        Hunt hunt = new Hunt(1, Age.Era_I, 3); // prestige = 3 per hunter

        hunt.resolve(List.of(player));

        // 1 food per hunter
        assertEquals(2, player.getFood());

        // 2 hunter → 2 * 3 = 6 prestige
        assertEquals(6, player.getPrestige());
    }

    @Test
    void playerWithNoHunterGetsNothing() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));

        Hunt hunt = new Hunt(1, Age.Era_I, 3);

        hunt.resolve(List.of(player));

        assertEquals(0, player.getFood());
        assertEquals(0, player.getPrestige());
    }

    @Test
    void huntEventCountsHuntersWithAndWithoutNugget() {
        Player player = new Player("Mario", new Totem(TotemColor.RED));

        // uno con icona, uno senza
        player.addCharacterCard(new Hunter(1, Age.Era_I, 2, 0));
        player.addCharacterCard(new Hunter(2, Age.Era_I, 2, 0));

        Hunt hunt = new Hunt(1, Age.Era_I, 2);

        hunt.resolve(List.of(player));

        // 2 hunter totali → 2 cibi
        assertEquals(2, player.getFood());

        // 2 hunter * 2 prestigio = 4
        assertEquals(4, player.getPrestige());
    }

}