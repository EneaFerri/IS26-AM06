package it.polimi.ingsw.LocalGameTestCLI;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;
import java.util.Scanner;
import it.polimi.ingsw.model.board.BoardSpace;


public class MainGame {
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

        System.out.println("\n--------------------------primo round-------------------------\n");
        System.out.println("players info:");
        for(Player player : game.getPlayers()){
            System.out.println("\n" + player.toString());
        }

        System.out.println("\ngame board info:");
        System.out.println(game.getBoard().toString());

        System.out.println("GAME STATUS: " + game.getStatus());

        while (game.getStatus() == GameState.OFFER_SPACE_CHOOSE) { // piazzamento totem
            Player currentPlayer = game.getCurrentPlayer(); // player in turn deciso dal model

            System.out.println("\nTocca a: " + currentPlayer.getNickname());
            System.out.println("Spazi liberi: " + game.getBoard().getFreeBoardSpaces()); // aggiornamento dinamico spezzi disponibili
            System.out.println("Inserisci la lettera dello spazio offerta: ");

            String chosenSpaceInput = input.nextLine().trim().toUpperCase();

            if (chosenSpaceInput.isEmpty()){
                 System.out.println("Input vuoto ,riprova.");
                continue;
            }

            BoardSpace chosenSpace = game.getBoard().getBoardSpace(chosenSpaceInput.charAt(0)); // lettera inserta -> spazio board (verifica se eiste) nel caso reinserimento

            if (chosenSpace == null) {
                System.out.println("Spazio non valido, riprova .");
                continue ;
            }

            try {
                game.placeTotemOnOfferSpace(currentPlayer, chosenSpace); // chiamata al model e verifica correttezza, se ok avanzamento

                System.out.println("\nBoard aggiornato:");
                System.out.println(game.getBoard());
                System.out.println("GAME STATUS: " + game.getStatus());

            } catch (IllegalStateException e) { // messaggi personalizzati per tipologia di errore
                System.out.println("Errore: " + e.getMessage());
            }
        }

        System.out.println("\nFine test fase OFFER_SPACE_CHOOSE.");
        System.out.println("GAME STATUS: " + game.getStatus());

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
