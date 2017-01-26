package org.valesz.ups.common.message.received;


import org.valesz.ups.common.message.MessageType;

/**
 * A base class for all responses received from server.
 *
 * T defines the data type of response content. Usually just a String.
 *
 * @author Zdenek Vales
 */
public abstract class   AbstractReceivedMessage<T> {

    /**
     * Returns a message content.
     * @return
     */
    public abstract T getContent();

    /**
     * Returns a type of response.
     * @return
     */
    public abstract MessageType getMessageType();

    @Override
    public String toString() {
        String cnt = getContent() == null ? "null" : getContent().toString();
        String msgType = getMessageType() == null ? "null" : getMessageType().toString();
        return String.format("Content: %s, MessageType: %s.", cnt, msgType);
    }
}
