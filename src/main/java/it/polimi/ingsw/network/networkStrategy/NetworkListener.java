package it.polimi.ingsw.network.networkStrategy;

import it.polimi.ingsw.network.message.Message;


public interface NetworkListener {

    void onMessage(Message message);

    void onDisconnected();
}