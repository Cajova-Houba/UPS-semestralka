package org.valesz.ups.network;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.valesz.ups.common.error.Error;
import org.valesz.ups.common.message.Message;
import org.valesz.ups.common.message.MessageType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

/**
 * Service will handle sending nick to server and receiving confirmation.
 *
 * @author Zdenek Vales
 */
public class NickService extends Service<Object>{

    private String nick;
    private DataInputStream inFromServer;
    private DataOutputStream outToServer;

    public NickService() {
    }

    public DataInputStream getInFromServer() {
        return inFromServer;
    }

    public void setInFromServer(DataInputStream inFromServer) {
        this.inFromServer = inFromServer;
    }

    public DataOutputStream getOutToServer() {
        return outToServer;
    }

    public void setOutToServer(DataOutputStream outToServer) {
        this.outToServer = outToServer;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    @Override
    protected Task<Object> createTask() {
        byte nickLen = (byte)nick.length();
        nick = ((char)nickLen) + nick;
        return new MessageSender(new Message(MessageType.CMD, nick), outToServer, inFromServer);
    }
}