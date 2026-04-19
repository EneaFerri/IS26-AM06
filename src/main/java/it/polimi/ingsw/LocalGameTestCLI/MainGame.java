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

    //Classe per testare il gioco in locale da terminale --> i commenti con freccia indicano appunti sulle chiamate in client/server
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        Game game = new Game(0); // --> server fa partire il gioco con id casuale quano parte programma

        System.out.println("GAME STATUS: " + game.getStatus());

        int numberOfPlayersInt=5;

        while (game.getStatus()== GameState.LOGIN && game.getPlayers().size() < numberOfPlayersInt){ // --> model in fase di LOGIN in attesa del primo player

            // --> quando si attiva il collegamento SOCKET/RMI con client viene richiesto nickname
            System.out.println("Inserisci nickname: "); // --> inviato da Server a Client
            String nickname = input.nextLine(); //inviato da Client a Server

            TotemColor totemColor = askTotemColor(input, game); //--> inviato da Server a Client richiesta di colore Totem, e ricevuta risposta

            // --> Server manda a tutti i client il nuovo player aggiunto (nickname e colore totem)

            Player player = new Player(nickname, new Totem(totemColor)); // model effettivamente aggiunge player al gioco
            game.addPlayer(player);

            if(game.getPlayers().size() == 1){
                System.out.println("Inserisci il numero di giocatori: "); // --> se primo player aggiunto, richiesto numero giocatori
                numberOfPlayersInt = Integer.parseInt(input.nextLine()); //--> ricevuta risposta, ora Model sa effettivamente quanti giocatori attendere, per ognuno richiede nickname e colore totem
            }
        }

        //check players e totems
        System.out.println("Players: ");
        for(Player player : game.getPlayers()){
            System.out.println(player.getNickname() + " " + player.getTotem().getColor());
        }

        game.startGame(); //--> finita la fase di LOGIN, il model lancia startGame() e lo comunica a tutti i client
        // --> in questa fase viene anche lanciata la grafica del gameBoard a tutti i client


        //simulazione round
        for(int i = 1; i < 11; i++){

            if(game.getStatus()== GameState.END){ //check extra
                break;
            }

            // --> ogni inizio round Server manda determinate informazioni a tutti i client: Numero round, Era??
            if(game.getCurrentAge()== Age.Last_Event){
                System.out.println("\n--------------------------ROUND FINALE-------------------------\n");
            }else{
                System.out.println("\n--------------------------round: " + i + "-------------------------\n");
            }

            //qua le metto come stringhe, in realtà le player info e le game Board Info saranno poi nella view di ogni client, e andranno aggiornato per "ogni azione"
            System.out.println("players info:");
            for(Player player : game.getPlayers()){
                System.out.println("\n" + player.toString());
            }
            System.out.println("\ngame board info:");
            System.out.println(game.getBoard().toString());


            System.out.println("\nGAME STATUS: " + game.getStatus()); //gamestate = OFFER_SPACE_CHOOSE

            while(game.getStatus() == GameState.OFFER_SPACE_CHOOSE){

                System.out.println("\nPlayer " + game.getCurrentPlayer().getNickname() + " scegli il posto di offerte "); // --> Server manda a Client del player in turno la richiesta di scelta di posizione board

                System.out.println("Inserisci lettera del posto: ");
                char letter = input.nextLine().charAt(0);  // --> ricevuta risposta, Server sa il posto scelto dal client

                boolean found = false;
                // --> Server cerca il posto scelto e lo mette in offerto, se non lo trova, ritorna errore
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

                // --> fino a che tutti i client si sono posizionati il server manda richiesta e rimane in attesa del player in turno, intanto altri client devono rimanere "fermi"
                // --> però per ogni scelta fatta da un player (client) è importante che il server la comunichi a tutti gli altri client (scelta strategica di posizionamento)
            }


            System.out.println("\ngame board info:");
            System.out.println(game.getBoard().toString());

            System.out.println("\nGAME STATUS: " + game.getStatus()); //gamestate = PICKING_CARD

            while(game.getStatus() == GameState.PICKING_CARD){ //--> la logica è la stessa per la scleta del board space: server manda richiesta al client in turno, client risponde con le varie pick card, anche tutti gli altri client vengono aggiornati (devono sapere che carte sono state prese)
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

                // --> attenziona ad aggiornare corrente la view del gameBoard per ogni client, per ogni carta pescata
            }

            System.out.println("players info:");
            for(Player player : game.getPlayers()){
                System.out.println("\n" + player.toString());
            }


            System.out.println("\nGAME STATUS: " + game.getStatus()); //gamestate = EVENTS?

            game.resolveEvents();

            System.out.println("\nEVENTI RISOLTI");

        }

        if(game.getStatus() == GameState.END){ //--> comunicazione a tutti i client dei risultati, particolare comunicazione per il vincitore

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
