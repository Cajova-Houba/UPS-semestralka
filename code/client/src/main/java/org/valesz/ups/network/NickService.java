package org.valesz.ups.network;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.valesz.ups.common.message.Message;
import org.valesz.ups.common.message.MessageType;

import java.io.DataOutputStream;

/**
 * Service will handle sending nick to server and receiving confirmation.
 *
 * @author Zdenek Vales
 */
public class NickService extends Service<Object>{

    private String nick;
    private DataOutputStream outToServer;

    public NickService() {
    }


    public void setOutToServer(DataOutputStream outToServer) {
        this.outToServer = outToServer;
    }


    public void setNick(String nick) {
        this.nick = nick;
    }

    @Override
    protected Task<Object> createTask() {
        char nickLen = Integer.toString(nick.length()).charAt(0);
        nick = nickLen + nick;
        return new MessageSender(new Message(MessageType.CMD, nick), outToServer);
    }
}
