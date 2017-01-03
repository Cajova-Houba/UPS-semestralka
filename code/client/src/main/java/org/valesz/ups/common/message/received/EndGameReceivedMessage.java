package org.valesz.ups.common.message.received;

import org.valesz.ups.common.message.MessageType;

/**
 * @author Zdenek Vales
 */
public class EndGameReceivedMessage extends AbstractReceivedMessage<String> {

    @Override
    public String getContent() {
        return "END_GAME";
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.INF;
    }
}
