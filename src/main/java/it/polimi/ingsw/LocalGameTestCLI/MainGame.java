package it.polimi.ingsw.LocalGameTestCLI;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.board.BoardSpace;
import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.cards.Card;
import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.cards.TribeCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;
import java.util.Scanner;
import it.polimi.ingsw.model.board.BoardSpace;


public class MainGame {

    //Classe per testare il gioco in locale da terminale!!!!!!!!!!!!!!!!!
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        Game game = new Game(0);

        System.out.println("GAME STATUS: " + game.getStatus());

        int numberOfPlayersInt=5;

        while (game.getStatus()== GameState.LOGIN && game.getPlayers().size() < numberOfPlayersInt){

            System.out.println("Inserisci nickname: ");
            String nickname = input.nextLine();

            TotemColor totemColor = askTotemColor(input, game); // sistemata scelta dei colori: un colore può essere scelto solo da uno, e questo viene mostrato agli altri

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

        //simulazione round
        for(int i = 1; i < 20; i++){

            if(game.getStatus()== GameState.END){
                break;
            }

            if(game.getCurrentAge()== Age.Last_Event){
                System.out.println("\n--------------------------ROUND FINALE-------------------------\n");
            }else{
                System.out.println("\n--------------------------round: " + i + "-------------------------\n");
            }


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

        if(game.getStatus() == GameState.END){

            System.out.println("\n\nPartita terminata, ecco i risultati:\n");

            System.out.println("players info:");
            for(Player player : game.getPlayers()){
                System.out.println("\n" + player.toString());
            }

            System.out.println("\nWINNER: " + game.getWinners().get(0).getNickname() + ", con ben " + game.getWinners().get(0).getPrestige() + " punti");
        }


    }





    private static TotemColor askTotemColor(Scanner input, Game game) { // colore x singolo giocatore protetto, visione colore altri, input anche in minuscolo o misto
        while (true) {
            System.out.println("Scegli colore totem");
            printTotemColors(game);

            String rawColor = input.nextLine().trim().toUpperCase();

            try {
                TotemColor chosenColor = TotemColor.valueOf(rawColor);

                if (isColorAlreadyTaken(game, chosenColor)) {
                    System.out.println("Colore già scelto da un altro giocatore, riprova");
                    continue;
                }

                return chosenColor;

            } catch (IllegalArgumentException e) {
                System.out.println("Colore non valido, riprova scrivendo uno dei colori mostrati");
            }
        }
    }

    private static void printTotemColors(Game game) {
        System.out.println("Colori disponibili / occupati:");

        for (TotemColor color : TotemColor.values()) {
            String owner = "";

            for (Player player : game.getPlayers()) {
                if (player.getTotem().getColor() == color) {
                    owner = " (" + player.getNickname() + ")";
                    break;
                }
            }

            System.out.println("- " + color + owner);
        }
    }

    private static boolean isColorAlreadyTaken(Game game, TotemColor color) {
        for (Player player : game.getPlayers()) {
            if (player.getTotem().getColor() == color) {
                return true;
            }
        }
        return false;
    }

}
