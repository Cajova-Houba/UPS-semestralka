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
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * An abstract receiver class.
 * Only contains method for receiving and parsing messages.
 *
 * @author Zdenek Vales
 */
public abstract class AbstractReceiver extends Task<AbstractReceivedMessage>{

    protected static final Logger logger = LogManager.getLogger(AbstractReceiver.class);

    /**
     * Max timeout for message to be received in waiting mode.
     * In milliseconds.
     */
    public static final int MAX_WAITING_TIMEOUT = 1000;

    /**
     * Tries to receive and parse a message type from the tcp stack. Throws exception if
     * some gibberish text is received, message type cannot be parsed or some other error occurs.
     * @param inFromServer
     * @return
     */
    protected MessageType receiveMessageType(DataInputStream inFromServer) throws IOException, ReceivingException {
        final int msgTypeLen = MessageType.getMessageTypeLen();
        byte[] buffer = new byte[msgTypeLen];
        int i = 0;
        int j = 0;
        int msgStaus = 0;
        boolean charOk = false;
        MessageType mt = null;

        // get message type, by byte
        while (i < msgTypeLen) {
            msgStaus = inFromServer.read(buffer, i, 1);

            if(msgStaus < 0) {
                logger.error("Error while receiving message type");
                throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE));
            }

            // filter out white space chars at the beginning of the message
            if(((char)buffer[i]) == '\n' || ((char)buffer[i]) == ' ') {
                continue;
            }

            if (mt == null) {
                // message type not determined yet
                if(i > 0) {
                    // message type should be known at this point
                    throw new ReceivingException(Error.BAD_MSG_TYPE());
                } else {
                    switch (buffer[i]) {
                        case 'C':
                        case 'c':
                            mt = MessageType.CMD;
                            break;
                        case 'I':
                        case 'i':
                            mt = MessageType.INF;
                            break;
                        case 'E':
                        case 'e':
                            mt = MessageType.ERR;
                            break;
                    }
                }
            } else {
                // receive the rest of the message type
                // and make sure it's correct
                char[] possibleChars = MessageType.getPossibleCharsAtPos(mt, i);
                charOk = false;
                for(j = 0; j < possibleChars.length; j++) {
                    if(((char)buffer[i]) == possibleChars[j]) {
                        charOk = true;
                        break;
                    }
                }
                if(!charOk) {
                    throw new ReceivingException(Error.BAD_MSG_TYPE());
                }
            }

            i++;
        }


        return mt;
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

    /**
     * Receives the rest of the INF message.
     * @return If everything is fine, message object is returned, otherwise Error is returned.
     */
    private AbstractReceivedMessage receiveInfMsg(DataInputStream inFromServer) throws IOException, ReceivingException {
        /*
        * Info message can either be START_GAME, END_GAME or OK.
        */
        byte[] buffer = new byte[50];
        Message m;
        int received = 0;
        int i = 0;
        int minLen = 0;
        String expectedMessage = "";

        received = inFromServer.read(buffer,0,1);
        if (received != 1) {
            logger.error("Error receiving INF message.");
            throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE));
        }
        char firstChar = (char)buffer[0];
        switch (firstChar) {
            case 'O':
            case 'o':
                //INFOK message
                received = inFromServer.read(buffer,1,1);
                if(received != 1) {
                    logger.error("Error while receiving OK message");
                    throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE));
                } else if (((char)buffer[1]) != 'K' && ((char)buffer[1]) != 'k') {
                    logger.error(String.format("Wrong message received: %c%c.", (char)buffer[0], (char)buffer[1]));
                    throw new ReceivingException(Error.BAD_MSG_CONTENT());
                }

                logger.trace("Received: OK");
                return new OkReceivedMessage();

            case 'S':
            case 's':
                //INFSTART_GAME message
                expectedMessage = "START_GAME";
                minLen = expectedMessage.length();         //length of TART_GAME
                i = 1;
                while(i < minLen) {
                    char c = expectedMessage.charAt(i);
                    received = inFromServer.read(buffer,i,1);
                    if(received != 1) {
                        logger.error("Error while receiving START_GAME message.");
                        throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE));
                    }

                    if(((char)buffer[i]) != c && ((char)buffer[i]) != Character.toLowerCase(c)) {
                        logger.error(String.format("Bad character %c on position %d while receiving START_GAME message.", (char)buffer[i], i));
                        throw new ReceivingException(Error.BAD_MSG_CONTENT());
                    }

                    i++;
                }

                // START_GAME string is received, now receive the rest of the message
                String firstNick = receiveNick(inFromServer, ',');
                if(firstNick == null || !Pattern.matches(Constraits.NICKNAME_REGEXP, firstNick)) {
                    logger.error("Error while receiving the 1st nick from START_GAME message");
                    throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE));
                }
                logger.trace("1st nick received: "+firstNick);

                String secondNick = receiveNick(inFromServer, ';');
                if(secondNick == null || !Pattern.matches(Constraits.NICKNAME_REGEXP, firstNick)) {
                    logger.error("Error while receiving the 1st nick from START_GAME message");
                    throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE));
                }
                logger.trace("2nd nick received: "+secondNick   );
                logger.trace("Received: START_GAME, p1="+firstNick+", p2="+secondNick);
                return new StartGameReceivedMessage(firstNick, secondNick);

            case 'E':
            case 'e':
                //receive ND_GAME
                expectedMessage = "END_GAME";
                i = 1;
                minLen = expectedMessage.length();
                while (i < minLen) {
                    char c = expectedMessage.charAt(i);
                    received = inFromServer.read(buffer,i,1);
                    if(received != 1) {
                        logger.error("Error while receiving END_GAME message.");
                        throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE));
                    }

                    if(((char)buffer[i]) != c && ((char)buffer[i]) != Character.toLowerCase(c)) {
                        logger.error(String.format("Bad character %c on position %d while receiving END_GAME message.", (char)buffer[i], i));
                        throw new ReceivingException(Error.BAD_MSG_CONTENT());
                    }
                    i++;
                }

                // receive winner's nick
                String winner = receiveNick(inFromServer, ';');
                if(winner == null) {
                    logger.error("Empty nick received!");
                    throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE));
                }

                logger.trace("Received: END_GAME");
                return new EndGameReceivedMessage(winner);



//            case 'W':
//                //receive 'AITING'
//                buffer = new byte[6];
//                received = inFromServer.read(buffer);
//                if(received != buffer.length || !Pattern.matches("AITING", new String(buffer))) {
//                    logger.error("Error while receiving WAITING message.");
//                    throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE));
//                }
//
//                // receive the nick of the player the game is waiting for
//                String nick = receiveNick(inFromServer, ';');
//                if(nick == null) {
//                    logger.error("Empty nick received!");
//                    throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE));
//                }
//
//                return new WaitingForPlayerReceivedMessage(nick);

            default:
                throw new ReceivingException(Error.BAD_MSG_CONTENT());
        }
    }

    /**
     * Receives the rest of the ERR message.
     * @return
     */
    private ErrorReceivedMessage receiveErrMsg(DataInputStream inFromServer) throws IOException, ReceivingException {

        /* ERR message consists of just 2 bytes with error code */
        byte[] buffer = new byte[2];
        int minLen = 2;
        int i = 0;
        while (i < minLen) {
            int received = inFromServer.read(buffer, i, 1);

            if(received != 1) {
                logger.error("Error while receiving error message.");
                throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE));
            }

            if(((char)buffer[i]) < '0' || ((char)buffer[i]) > '9') {
                logger.error(String.format("Bad character %c on position %d while receiving error message.", ((char)buffer[i]), i));
                throw new ReceivingException(Error.BAD_MSG_CONTENT());
            }
        }

        ErrorCode ec = ErrorCode.getCodeByInt(buffer[0]);
        if (ec == ErrorCode.NO_ERROR) {
            logger.error("Error, wrong error code received.");
            throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.UNRECOGNIZED_ERROR));
        }

        return new ErrorReceivedMessage(ec);
    }

    /**
     * Receives 10 bytes from tcp stack and tries to convert them to 5 in values
     * which represent turn word.
     * @param inFromServer
     * @param player
     * @return
     */
    private int[] receivePlayerStones(DataInputStream inFromServer, int player) throws IOException, ReceivingException {
        final int len = Constraits.MAX_NUMBER_OF_STONES;
        int[] playerStones = new int[len];
        byte[] buffer = new byte[len*2];
        int i = 0;
        int cntr = 0;
        int stonePos = 0;
        int received = 0;

        while(i < len) {
            received = inFromServer.read(buffer, i, 1);
            if(received != 1) {
                logger.error("Error while receiving player "+player+" turn word of start turn message.");
                throw new ReceivingException(Error.GENERAL_ERROR(ErrorMessages.RECEIVING_RESPONSE));
            }

            if(((char)buffer[i]) < '0' || ((char)buffer[i]) > '9') {
                logger.error(String.format("Bad character %c on position %d while receiving start turn message.", ((char)buffer[i]), i));
                throw new ReceivingException(Error.BAD_MSG_CONTENT());
            }

            if(i % 2 == 0) {
                // first digit of the 2-digit number
                stonePos = 10*Character.getNumericValue(((char)buffer[i]));
            } else {
                // second digit of the 2-digit number
                stonePos += Character.getNumericValue(((char)buffer[i]));
                playerStones[cntr] = stonePos;
                cntr++;
            }

            i++;
        }

        return playerStones;
    }

    /**
     * Receives the rest of the CMD message.
     * @return
     */
    private AbstractReceivedMessage receiveCmdMsg(DataInputStream inFromServer) throws IOException, ReceivingException {

        /*
            Command message can be only start turn
            20 bytes = both turn words
         */
        logger.debug("Receiving CMD message.");
        int[] firstPlayerStones = receivePlayerStones(inFromServer, 1);
        int[] secondPlayerStones = receivePlayerStones(inFromServer, 2);

        return new StartTurnReceivedMessage(firstPlayerStones, secondPlayerStones);
    }

    /**
     * Tries to receive and parse message from the tcp stack. Throws exception if
     * some gibberish text is received or message cannot be parsed.
     * @param inFromServer Input stream.
     * @return
     */
    protected AbstractReceivedMessage receiveMessage(DataInputStream inFromServer) throws IOException, ReceivingException {

        MessageType messageType = null;

        // receive message type
        messageType = receiveMessageType(inFromServer);


        /* now receive the rest of the message */
        switch (messageType) {
            case INF:
                return receiveInfMsg(inFromServer);
            case ERR:
                return receiveErrMsg(inFromServer);
            case CMD:
                return receiveCmdMsg(inFromServer);
            default:
                throw new ReceivingException(Error.BAD_MSG_TYPE());
        }
    }
}
