package it.polimi.ingsw.LocalGameTestCLI;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.board.BoardSpace;
import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.cards.Card;
import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.cards.TribeCard;
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

        //simulazione 3 round
        for(int i = 0; i < 3; i++){
            System.out.println("\n--------------------------round: " + i + "-------------------------\n");
            System.out.println("players info:");
            for(Player player : game.getPlayers()){
                System.out.println("\n" + player.toString());
            }

            System.out.println("\ngame board info:");
            System.out.println(game.getBoard().toString());

            System.out.println("\nGAME STATUS: " + game.getStatus()); //gamestate = OFFER_SPACE_CHOOSE

            while(game.getStatus() == GameState.OFFER_SPACE_CHOOSE){
                System.out.println("\nPlayer " + game.getCurrentPlayer().getNickname() + " scegli il posto di offerte ");

                System.out.println("Inserisci lettera del posto: ");
                char letter = input.nextLine().charAt(0);

                boolean found = false;
                for(BoardSpace bs : game.getBoard().getFreeBoardSpaces()){
                    if(bs.getLetter() == letter){
                        game.placeTotemOnOfferSpace(game.getCurrentPlayer(), bs);
                        found = true;
                        break;
                    }
                }

                if(!found){
                    System.out.println("Posto non disponibile");
                }

            }

            System.out.println("\ngame board info:");
            System.out.println(game.getBoard().toString());

            System.out.println("\nGAME STATUS: " + game.getStatus()); //gamestate = PICKING_CARD

            while(game.getStatus() == GameState.PICKING_CARD){
                String playerNickname = game.getCurrentPlayer().getNickname();

                System.out.println("\nPlayer " + playerNickname + " scegli carta (inserisci id):");
                int idcardchoose = Integer.parseInt(input.nextLine());
                for(TribeCard c: game.getBoard().getAvailableUpperTribeCards()){
                    if(c.getID() == idcardchoose){
                        game.pickCard(game.getCurrentPlayer(), c);
                        System.out.println("carta selezionata");
                    }
                }

                for(TribeCard c: game.getBoard().getAvailableBottomTribeCards()){
                    if(c.getID() == idcardchoose){
                        game.pickCard(game.getCurrentPlayer(), c);
                        System.out.println("carta selezionata");
                    }
                }

                for(BuildingCard c: game.getBoard().getAvailableUpperBuildingCards()){
                    if(c.getID() == idcardchoose){
                        game.pickCard(game.getCurrentPlayer(), c);
                        System.out.println("carta selezionata");
                    }
                }

                for(BuildingCard c: game.getBoard().getAvailableBottomBuildingCards()){
                    if(c.getID() == idcardchoose){
                        game.pickCard(game.getCurrentPlayer(), c);
                        System.out.println("carta selezionata");
                    }
                }

                System.out.println("prossima carta");


            }

            System.out.println("players info:");
            for(Player player : game.getPlayers()){
                System.out.println("\n" + player.toString());
            }


            System.out.println("\nGAME STATUS: " + game.getStatus()); //gamestate = EVENTS?

            game.resolveEvents();
            System.out.println("\nEVENTI RISOLTI");

        }






    }
}
