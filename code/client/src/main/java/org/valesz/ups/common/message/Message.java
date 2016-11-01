package org.valesz.ups.common.message;

/**
 * Message which will be sent (and received) to server.
 *
 * @author Zdenek Vales
 */
public class Message {

    private final MessageType messageType;
    private final String content;

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

    public int getMessageLength() {
        return 3 + content.length();
    }

    @Override
    public String toString() {
        return messageType.name()+content;
    }
}
