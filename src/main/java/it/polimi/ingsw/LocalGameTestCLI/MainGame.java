package it.polimi.ingsw.LocalGameTestCLI;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;
import java.util.Scanner;

public class MainGame {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        Game game = new Game(0);

        System.out.println("GAME STATUS: " + game.getStatus());

        int numberOfPlayersInt=5;

        while (game.getStatus()== GameState.LOGIN && game.getPlayers().size() < numberOfPlayersInt){

            System.out.println("Inserisci nickname: ");
            String nickname = input.nextLine();

            System.out.println("Scegli colore totem");
            System.out.println("Colori rimasti: (BLUE, RED, YELLOW, BLACK, WHITE)"); //da implementare effettivi colori rimasti
            TotemColor totemColor = TotemColor.valueOf(input.nextLine());

            Player player = new Player(nickname, new Totem(totemColor));
            game.addPlayer(player);

            if(game.getPlayers().size() == 1){
                System.out.println("Inserisci il numero di giocatori: ");
                numberOfPlayersInt = Integer.parseInt(input.nextLine());
            }
        }

        System.out.println("Players: ");
        for(Player player : game.getPlayers()){
            System.out.println(player.getNickname() + " " + player.getTotem().getColor());
        }

        game.startGame();

        System.out.println("\n--------------------------primo round-------------------------\n");
        System.out.println("players info:");
        for(Player player : game.getPlayers()){
            System.out.println("\n" + player.toString());
        }

        System.out.println("\ngame board info:");
        System.out.println(game.getBoard().toString());

        System.out.println("GAME STATUS: " + game.getStatus());

    }
}
