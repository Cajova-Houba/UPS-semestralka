package org.valesz.ups.network;

import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.valesz.ups.common.message.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * A task which will send a message to the server and wait for response.
 *
 * Type of the return object not decided yet.
 *
 * @author Zdenek Vales
 */
public class MessageSender extends Task<Object> {

    public static final Logger logger = LogManager.getLogger(MessageSender.class);

    private final Message message;
    private final Socket socket;

    public MessageSender(Message message, Socket socket) {
        logger.trace(String.format("Creating new task with message: %s to socket: %s", message.toString(), socket.toString()));
        this.message = message;
        this.socket = socket;
    }

    @Override
    protected Object call() throws Exception {

        Object response = null;

        if (!socket.isConnected()) {
            return response;
        }

        logger.trace("Sending message.");
        sendMessage(message);
        logger.trace("Sent.");

        // wait for response
        logger.trace("Waiting for response.");
        response = getResponse();
        logger.trace(String.format("Response received: %s.", response.toString()));

        return response;
    }

    /**
     * Send a message to socket.
     * @param message
     */
    public void sendMessage(Message message) throws IOException {
        DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());

        // send message
        outToServer.write(message.toBytes());

        outToServer.close();
    }

    public Object getResponse() throws IOException {
        DataInputStream inFromServer = new DataInputStream(socket.getInputStream());
        String data = "";

        /* buffer for input stream */
        byte[] byteDataLen = new byte[1];

        /* real length of data to be returned*/
        int dataLen;

        dataLen = inFromServer.read(byteDataLen);
        if(dataLen == -1) {
            logger.error("Error while receiving data.");
        }

        /* byteDataLen now stores the actual length of data to be received, convert it to int */
        for(int i = 0; i< byteDataLen[0]; i++){
            int ch = inFromServer.read();
            data += ch+",";
        }

        return data;
    }
}
