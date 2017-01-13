package org.valesz.ups.network;

import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.valesz.ups.common.Constraits;
import org.valesz.ups.common.error.Error;
import org.valesz.ups.common.error.ErrorCode;
import org.valesz.ups.common.error.ErrorMessages;
import org.valesz.ups.common.error.ReceivingException;
import org.valesz.ups.common.message.Message;
import org.valesz.ups.common.message.MessageType;
import org.valesz.ups.common.message.received.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.regex.Pattern;

/**
 * A task which will wait for a message from the server.
 * If the message has wrong type or is incomplete ReceivingException is thrown.
 *
 * @author Zdenek Vales
 */
public class MessageReceiver extends Task<AbstractReceivedMessage> {

    public static final Logger logger = LogManager.getLogger(MessageReceiver.class);
    private final DataInputStream inFromServer;
    private final boolean waitForResponse;

    public MessageReceiver(DataInputStream inFromServer, boolean waitForResponse) {
        this.inFromServer = inFromServer;
        this.waitForResponse = waitForResponse;
    }

    @Override
    protected AbstractReceivedMessage call() throws Exception {

        AbstractReceivedMessage response = getResponse();

        return response;
    }

    public AbstractReceivedMessage getResponse() throws IOException, ReceivingException {

        /* first, receive 3 bytes indicating message type*/
        byte[] buffer = new byte[3];

        int received = 0;
        if(waitForResponse) {
            while( received != 3) {
                received = inFromServer.read(buffer, 0, 3);
            }
        } else {
            received = inFromServer.read(buffer);
            if(received != 3) {
                logger.error("Error while receiving message type");
                throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.WAITING_FOR_RESPONSE));
            }
        }

        String msgType = new String(buffer);
        MessageType messageType = MessageType.parseMessageType(msgType);
        if (messageType == null) {
            logger.error("Error, wrong message type received: "+msgType);
            throw new ReceivingException(Error.BAD_MSG_TYPE());
        }

        /* now receive the rest of the message */
        switch (messageType) {
            case INF:
                return receiveInfMsg();
            case ERR:
                return receiveErrMsg();
            case CMD:
                return receiveCmdMsg();
            default:
                throw new ReceivingException(Error.BAD_MSG_TYPE());
        }
    }

    /**
     * Receives the rest of the INF message.
     * @return If everything is fine, message object is returned, otherwise Error is returned.
     */
    private AbstractReceivedMessage receiveInfMsg() throws IOException, ReceivingException {
        /*
        * Info message can either be START_GAME, END_GAME or OK.
        */
        byte[] buffer = new byte[1];
        int received = inFromServer.read(buffer);
        Message m;
        if (received != 1) {
            logger.error("Error receiving INF message.");
            throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE));
        }
        char firstChar = (char)buffer[0];
        Pattern pattern;
        switch (firstChar) {
            case 'O':
                //INFOK message
                received = inFromServer.read(buffer);
                if(received != 1) {
                    logger.error("Error while receiving OK message");
                    throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE));
                }

                logger.trace("Received: OK");
                return new OkReceivedMessage();

            case 'S':
                //INFSTART_GAME message
                int minLen = 9;         //length of TART_GAME
                buffer = new byte[minLen];
                received = inFromServer.read(buffer);
                if(received < minLen) {
                    logger.error("Error while receiving START_GAME message");
                    throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE));
                }
                String receivedString = new String(buffer);

                if(Pattern.matches("TART_GAME", receivedString)) {
                    // receive both nick names
                    String firstNick = receiveNick(inFromServer, ',');
                    logger.trace("1st nick received: "+firstNick);
                    if(firstNick == null || !Pattern.matches(Constraits.NICKNAME_REGEXP, firstNick)) {
                        logger.error("Error while receiving the 1st nick from START_GAME message");
                        throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE));
                    }
                    String secondNick = receiveNick(inFromServer, ';');
                    logger.trace("2nd nick received: "+secondNick   );
                    if(firstNick == null || !Pattern.matches(Constraits.NICKNAME_REGEXP, firstNick)) {
                        logger.error("Error while receiving the 1st nick from START_GAME message");
                        throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE));
                    }
                    logger.trace("Received: START_GAME, p1="+firstNick+", p2="+secondNick);
                    return new StartGameReceivedMessage(firstNick, secondNick);

                } else {
                    logger.error("Error while receiving START_GAME message");
                    throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE));
                }
            case 'E':
                //receive ND_GAME
                buffer = new byte[7];
                received = inFromServer.read(buffer);
                if(received != buffer.length || !Pattern.matches("ND_GAME", new String(buffer))) {
                    logger.error("Error while receiving END_GAME message.");
                    throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE));
                }

                // receive winner's nick
                String winner = receiveNick(inFromServer, ';');
                if(winner == null) {
                    logger.error("Empty nick received!");
                    throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE));
                }

                logger.trace("Received: END_GAME");
                return new EndGameReceivedMessage(winner);

            default:
                throw new ReceivingException(Error.BAD_MSG_CONTENT());
        }
    }

    /**
     * Receives the rest of the CMD message.
     * @return
     */
    private AbstractReceivedMessage receiveCmdMsg() throws IOException, ReceivingException {

        /*
            Command message can be only start turn
            10 bytes = both turn words
         */
        logger.debug("Receiving CMD message.");
        byte[] buffer = new byte[10];
        int received = inFromServer.read(buffer);
        if(received != buffer.length) {
            logger.error("Error while receiving start turn message");
            throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE));
        } else {
            int[] firstPlayerStones = new int[5];
            int[] secondPlayerStones = new int[5];

            for (int i = 0; i < 5; i++) {
                firstPlayerStones[i] = buffer[i];
                secondPlayerStones[i] = buffer[i+5];
            }

            return new StartTurnReceivedMessage(firstPlayerStones, secondPlayerStones);
        }
    }

    /**
     * Receives the rest of the ERR message.
     * @return
     */
    private ErrorReceivedMessage receiveErrMsg() throws IOException, ReceivingException {

        /* ERR message consists of just 1 byte with error code */
        byte[] buffer = new byte[1];
        int received = inFromServer.read(buffer);
        if (received != 1) {
            logger.error("Error receiving ERR message.");
            throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE));
        }

        ErrorCode ec = ErrorCode.getCodeByInt(buffer[0]);
        if (ec == ErrorCode.NO_ERROR) {
            logger.error("Error, wrong error code received.");
            throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.UNRECOGNIZED_ERROR));
        }

        return new ErrorReceivedMessage(ec);
    }

    /**
     * This method will read from input byte per byte until it reaches delimiter char.
     * If error occurs null is returned.
     *
     * @param input
     * @param delimiter
     * @return
     */
    private String receiveNick(DataInputStream input, char delimiter) throws IOException {
        int i = 0,
                received = 0;
        byte[] buffer = new byte[1];
        StringBuilder nickBuilder = new StringBuilder();

        received = input.read(buffer, 0, 1);
        if(received == 0) {
            return null;
        }

        while (i < Constraits.MAX_NICK_LENGTH && ((char)buffer[0]) != delimiter) {
            nickBuilder.append((char)buffer[0]);

            received = input.read(buffer, 0, 1);
            if(received == 0) {
                return null;
            }

            i++;
        }

        return nickBuilder.toString();
    }
}
