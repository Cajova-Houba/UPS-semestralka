package org.valesz.ups.network;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.valesz.ups.common.message.Message;
import org.valesz.ups.common.message.MessageType;

import java.net.Socket;

/**
 * Service will handle sending nick to server and receiving confirmation.
 *
 * @author Zdenek Vales
 */
public class NickService extends Service<Object>{

    private Socket socket;
    private String nick;

    public NickService() {
    }

    public NickService(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    @Override
    protected Task<Object> createTask() {
        return new MessageSender(new Message(MessageType.CMD, nick), socket);
    }
}
