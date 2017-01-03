package org.valesz.ups.common.message;

import org.valesz.ups.common.error.Error;
import org.valesz.ups.common.error.ErrorCode;

/**
 * Message which will be sent to server.
 *
 * @author Zdenek Vales
 */
public class Message {

    private final MessageType messageType;
    private final String content;

    /**
     * Parses a message. If the parsing fails, exception is thrown.
     *
     * @param message
     * @return
     */
    public static Message parseMessage(String message) throws MessageParsingException {

        /* message type parsing */
        MessageType mt = null;
        if (message == null || message.length() < 3) {
            throw new MessageParsingException(MessageParsingException.BAD_MESSAGE_TYPE);
        }
        for(MessageType messageType : MessageType.values()) {
            if (message.startsWith(messageType.name()) || message.startsWith(messageType.name().toLowerCase())) {
                mt = messageType;
                break;
            }
        }
        if(mt == null) {
            throw new MessageParsingException(MessageParsingException.BAD_MESSAGE_TYPE);
        }


        /* content parsing */
        String content = "";
        if(message.length() == 3) {
            /* type is right but the message doesn't contain anything */
            throw new MessageParsingException(MessageParsingException.BAD_MESSAGE_CONTENT);
        }
        content = message.substring(3);

        /* if the message is error, content should be 1 byte containing error number */
        if(mt == MessageType.ERR) {
            if (content.length() > 1) {
                throw new MessageParsingException(MessageParsingException.BAD_MESSAGE_CONTENT);
            }
            content = Integer.toString(content.getBytes()[0]);
        }

        return new Message(mt, content);
    }

    /**
     * Creates a message of type CMD with content where the
     * first byte is length of the nick and the rest of the content is the nick
     * itself.
     * @param nick
     * @return
     */
    public static Message createNickMessage(String nick) {
        byte nickLen = (byte)nick.length();
        nick = ((char)nickLen) + nick;
        return new Message(MessageType.CMD, nick);
    }

    public Message(MessageType messageType, String content) {
        this.messageType = messageType;
        this.content = content;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getContent() {
        return content;
    }

    /**
     * Returns the length of the message as array of four bytes.
     * Example:
     *  length = 14
     *  return = 014 000 000 000
     * @return
     */
    public byte[] getMessageLengthBytes() {
        int l = getMessageLength();

        byte res[] = new byte[4];

        res[0] = (byte)(l >> 24);
        res[1] = (byte)((l << 8) >> 24);
        res[2] = (byte)((l << 16) >> 24);
        res[3] = (byte)((l << 24) >> 24);

        return res;
    }

    public int getMessageLength() {
        return content.length();
    }

    @Override
    public String toString() {
        return "Message{" +
                "messageType=" + messageType +
                ", content='" + content + '\'' +
                '}';
    }

    /**
     * Returns true if the message type is error.
     * @return
     */
    public boolean isError() {
        return messageType == MessageType.ERR;
    }

    /**
     * Returns true if the message is INFOK.
     * @return
     */
    public boolean isOk() {
        return messageType == MessageType.INF && content.equals("OK");
    }

    /**
     * Returns this message as an array of bytes.
     * First four bytes are length of the whole message.
     * @return
     */
    public byte[] toBytes() {
        String msg = messageType.name() + content;
        byte[] res = new byte[msg.length()];

        for (int i = 0; i < msg.length(); i++) {
            res[i] = (byte)msg.charAt(i);
        }

        return res;
    }

    /**
     * If the isError() returns true, Error object will be returned.
     * Otherwise null is returned.
     * @return
     */
    public Error toError() {
        return new Error(ErrorCode.getCodeByInt(Integer.parseInt(content)), "");
    }
}
