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


    public static int getMessageTypeLen() {
        return 3;
    }

    /**
     * Returns an array containing possible chars at a certain position
     * of message type name.
     *
     * For example getPossibleCharsAtPos(CMD,2) would return ['D','d']
     *
     * @param mt Message type.
     * @param pos Position.
     * @return
     */
    public static char[] getPossibleCharsAtPos(MessageType mt, int pos) {
        char[] possibleChars = new char[2];
        char c = '\0';
        if(pos < 0 || pos >= getMessageTypeLen() || mt == null) {
            return possibleChars;
        }

        c = mt.name().charAt(pos);
        possibleChars = new char[] {c, Character.toLowerCase(c)};

        return possibleChars;
    }
}
