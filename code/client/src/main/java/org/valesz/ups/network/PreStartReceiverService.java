package org.valesz.ups.network;


import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.valesz.ups.common.message.received.AbstractReceivedMessage;

import java.io.DataInputStream;

/**
 * @author Zdenek Vales
 */
public class PreStartReceiverService extends Service<AbstractReceivedMessage> {

    private DataInputStream inFromServer;

    public void setInFromServer(DataInputStream inFromServer) {
        this.inFromServer = inFromServer;
    }

    @Override
    protected Task<AbstractReceivedMessage> createTask() {
        return null;
    }
}
