package org.valesz.ups.network;

import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.valesz.ups.common.message.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * A task which will send a message to the server.
 *
 * Type of the return object can be either Message or Error.
 * Message is returned if the response from server is correctly received.
 * Error is returned if some error during receiving occurs.
 *
 * If the message from the server is ERR, Error is returned too.
 *
 * @author Zdenek Vales
 */
public class MessageSender extends Task<Object> {

    public static final Logger logger = LogManager.getLogger(MessageSender.class);

    private final Message message;
    private final DataOutputStream outToServer;

    public MessageSender(Message message, DataOutputStream outToServer) {
        this.message = message;
        this.outToServer = outToServer;
    }

    @Override
    protected Object call() throws Exception {

        Object response = null;

        sendMessage(message);

        return response;
    }

    /**
     * Send a message to socket.
     * @param message
     */
    public void sendMessage(Message message) throws IOException {
        logger.trace("Sending message: "+message.toString());
        // send message
        outToServer.write(message.toBytes());
        logger.trace("Sent.");
    }

}
