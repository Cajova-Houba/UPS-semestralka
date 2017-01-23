package org.valesz.ups.common.message.received;

import java.lang.reflect.Type;

/**
 * Utility class for checking the received message type.
 *
 * @author Zdenek Vales
 */
public class ReceivedMessageTypeResolver {


    /**
     * Checks if the type of the message is same as provided type.
     * @param message
     * @param type
     */
    public static boolean isMessageType(AbstractReceivedMessage message, Class type) {
        return message != null && (message.getClass() == type);
    }

    /**
     * Checks if the message is EndGameReceivedMessage and if the type is same,
     * returns it. Otherwise null is returned.
     * @param message
     * @return
     */
    public static EndGameReceivedMessage isEndGame(AbstractReceivedMessage message) {
        if(isMessageType(message, EndGameReceivedMessage.class)) {
            return (EndGameReceivedMessage)message;
        }
        return null;
    }

    /**
     * Checks if the message is ErrorReceivedMessage and if the type is same,
     * returns it. Otherwise null is returned.
     * @param message
     * @return
     */
    public static ErrorReceivedMessage isError(AbstractReceivedMessage message) {
        if(isMessageType(message, ErrorReceivedMessage.class)) {
            return (ErrorReceivedMessage)message;
        }
        return null;
    }

    /**
     * Checks if the message is OkReceivedMessage and if the type is same,
     * returns it. Otherwise null is returned.
     * @param message
     * @return
     */
    public static OkReceivedMessage isOk(AbstractReceivedMessage message) {
        if(isMessageType(message, OkReceivedMessage.class)) {
            return (OkReceivedMessage)message;
        }
        return null;
    }

    /**
     * Checks if the message is StartGameReceivedMessage and if the type is same,
     * returns it. Otherwise null is returned.
     * @param message
     * @return
     */
    public static StartGameReceivedMessage isStartGame(AbstractReceivedMessage message) {
        if(isMessageType(message, StartGameReceivedMessage.class)) {
            return (StartGameReceivedMessage)message;
        }
        return null;
    }

    /**
     * Checks if the message is StartTurnReceivedMessage and if the type is same,
     * returns it. Otherwise null is returned.
     * @param message
     * @return
     */
    public static StartTurnReceivedMessage isStartTurn(AbstractReceivedMessage message) {
        if(isMessageType(message, StartTurnReceivedMessage.class)) {
            return (StartTurnReceivedMessage)message;
        }

        return null;
    }

    /**
     * Checks if the message is WaitingForPlayerReceivedMessage and if the type is same,
     * returns it. Otherwise null is returned.
     * @param message
     * @return
     */
    public static WaitingForPlayerReceivedMessage isWaitingForPlayer(AbstractReceivedMessage message) {
        if(isMessageType(message, WaitingForPlayerReceivedMessage.class)) {
            return (WaitingForPlayerReceivedMessage)message;
        }

        return null;
    }

    /**
     * Checks if the message is AliveReceivedMessage and if the type is same,
     * returns it. Otherwise null is returned.
     * @param message
     * @return
     */
    public static AliveReceivedMessage isAliveMessage(AbstractReceivedMessage message) {
        if(isMessageType(message, AliveReceivedMessage.class)) {
            return (AliveReceivedMessage)message;
        }

        return null;
    }

}
