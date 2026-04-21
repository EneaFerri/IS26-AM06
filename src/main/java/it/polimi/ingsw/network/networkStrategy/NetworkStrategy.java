package it.polimi.ingsw.network.networkStrategy;
import it.polimi.ingsw.network.networkStrategy.NetworkListener;

import java.io.IOException;


public interface NetworkStrategy {

    void setListener(NetworkListener listener);

    void connect(String host, int port) throws IOException;



    void login(String nickname);


    void drawCard(int cardId);


    void placeTotem(char letter);


    void disconnect();
}