package org.valesz.ups.network;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.valesz.ups.common.message.Message;

import java.io.DataOutputStream;

/**
 * Service that sends ok messages.
 * @author Zdenek Vales
 */
public class OkService extends Service<Object>{

    private DataOutputStream outToServer;

    public void setOutToServer(DataOutputStream outToServer) {
        this.outToServer = outToServer;
    }

    @Override
    protected Task<Object> createTask() {
        return new MessageSender(Message.createOKMessage(), outToServer);
    }
}
