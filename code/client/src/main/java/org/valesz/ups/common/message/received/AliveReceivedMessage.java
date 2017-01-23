package org.valesz.ups.common.message.received;

import org.valesz.ups.common.message.MessageType;

/**
 * @author Zdenek Vales
 */
public class AliveReceivedMessage extends AbstractReceivedMessage {

    @Override
    public Object getContent() {
        return "ALIVE";
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.INF;
    }
}
