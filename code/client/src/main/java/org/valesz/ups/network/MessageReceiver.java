package org.valesz.ups.network;

import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.Socket;

/**
 * A task which will wait for a message from the server.
 *
 * @author Zdenek Vales
 */
public class MessageReceiver extends Task<Object> {

    public static final Logger logger = LogManager.getLogger(MessageSender.class);

    private Socket socket;

    public MessageReceiver(Socket socket) {
        logger.trace(String.format("Creating new receiver task with for socket: %s", socket.toString()));
        this.socket = socket;
    }

    @Override
    protected Object call() throws Exception {
        return null;
    }
}
