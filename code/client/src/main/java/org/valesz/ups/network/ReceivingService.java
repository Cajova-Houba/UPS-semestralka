package org.valesz.ups.network;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.valesz.ups.common.message.received.AbstractReceivedMessage;

import java.io.DataInputStream;


/**
 * A service class for receiving response from server.
 *
 * @author Zdenek Vales
 */
public class ReceivingService extends Service<AbstractReceivedMessage> {

    private DataInputStream inFromServer;

    private boolean waitForTask = false;

    /**
     * If the thread will periodically check for response.
     * @param wait
     */
    public void setWaitForTask(boolean wait) {
        this.waitForTask = wait;
    }


    public void setInFromServer(DataInputStream inFromServer) {
        this.inFromServer = inFromServer;
    }

    @Override
    protected Task<AbstractReceivedMessage> createTask() {
        return new MessageReceiver(inFromServer, waitForTask);
    }
}
