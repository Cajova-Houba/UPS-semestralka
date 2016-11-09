package org.valesz.ups.network;

import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.valesz.ups.common.error.Error;
import org.valesz.ups.common.error.ErrorCode;
import org.valesz.ups.common.error.ErrorMessages;
import org.valesz.ups.common.message.Message;
import org.valesz.ups.common.message.MessageType;

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
    private final DataInputStream inFromServer;

    public MessageSender(Message message, DataOutputStream outToServer, DataInputStream inFromServer) {
        this.message = message;
        this.outToServer = outToServer;
        this.inFromServer = inFromServer;
    }

    @Override
    protected Object call() throws Exception {

        Object response = null;

        sendMessage(message);

        // wait for response
        response = getResponse();

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

    public Object getResponse() throws IOException {

        /* first, receive 3 bytes indicating message type*/
        byte[] buffer = new byte[3];

        int received = inFromServer.read(buffer);
        if(received != 3) {
            logger.error("Error while receiving message type");
            return Error.GENERAL_ERROR(ErrorMessages.WAITING_FOR_RESPONSE);
        }

        String msgType = new String(buffer);
        MessageType messageType = MessageType.parseMessageType(msgType);
        if (messageType == null) {
            logger.error("Error, wrong message type received: "+msgType);
            return Error.BAD_MSG_TYPE();
        }

        /* now receive the rest of the message */
        switch (messageType) {
            case INF:
                return receiveInfMsg();
            case ERR:
                return receiveErrMsg();
            default:
                return Error.BAD_MSG_TYPE();
        }
    }

    /**
     * Receives the rest of the INF message.
     * @return If everything is fine, message object is returned, otherwise Error is returned.
     */
    private Object receiveInfMsg() throws IOException {

        /*
        * Info message can either be START_TURN, END_TURN or OK.
        */
        byte[] buffer = new byte[1];
        int received = inFromServer.read(buffer);
        Message m;
        if (received != 1) {
            logger.error("Error receiving INF message.");
            return Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE);
        }
        char firstChar = (char)buffer[0];
        switch (firstChar) {
            case 'O':
                //receive K
                received = inFromServer.read(buffer);
                if(received != 1) {
                    logger.error("Error while receiving OK message");
                    return Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE);
                }

                m = new Message(MessageType.INF, "OK");
                logger.trace("Received: "+m.toString());
                return m;

            case 'S':
                //receive TART_TURN
                buffer = new byte[9];
                received = inFromServer.read(buffer);
                if(received != buffer.length) {
                    logger.error("Error while receiving START_TURN message.");
                    return Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE);
                }

                m = new Message(MessageType.INF, "START_TURN");
                logger.trace("Received: "+m.toString());
                return m;

            case 'E':
                //receive ND_TURN
                buffer = new byte[7];
                received = inFromServer.read(buffer);
                if(received != buffer.length) {
                    logger.error("Error while receiving END_TURN message.");
                    return Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE);
                }

                m = new Message(MessageType.INF, "END_TURN");
                logger.trace("Received: "+m.toString());
                return m;

            default:
                return Error.BAD_MSG_CONTENT();
        }
    }

    /**
     * Receives the rest of the CMD message.
     * @return
     */
    private Message receiveCmdMsg() {
        return  null;
    }

    /**
     * Receives the rest of the ERR message.
     * @return
     */
    private Error receiveErrMsg() throws IOException {

        /* ERR message consists of just 1 byte with error code */
        byte[] buffer = new byte[1];
        int received = inFromServer.read(buffer);
        if (received != 1) {
            logger.error("Error receiving ERR message.");
            return Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE);
        }

        ErrorCode ec = ErrorCode.getCodeByInt(buffer[0]);
        if (ec == ErrorCode.NO_ERROR) {
            logger.error("Error, wrong error code received.");
            return Error.GENERAL_ERROR(ErrorMessages.UNRECOGNIZED_ERROR);
        }

        return new Error(ec, "");
    }
}
