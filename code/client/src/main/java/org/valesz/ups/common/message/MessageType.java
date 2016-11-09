package org.valesz.ups.common.message;

/**
 * Possible message types.
 *
 * @author Zdenek Vales
 */
public enum MessageType {

    ERR,
    INF,
    CMD;

    /**
     * Parses the message type and returns null if the string isn't
     * the name of any message type.
     * @param messageType
     * @return
     */
    public static MessageType parseMessageType(String messageType) {
        MessageType mt = null;
        for(MessageType messageType1 : values()) {
            if(messageType1.name().equals(messageType)) {
                mt = messageType1;
            }
        }

        return mt;
    }

}
