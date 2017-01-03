package org.valesz.ups.common.message.received;

import org.valesz.ups.common.message.MessageType;

/**
 * @author Zdenek Vales
 */
public class OkReceivedMessage extends AbstractReceivedMessage<String>{

    @Override
    public String getContent() {
        return "OK";
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.INF;
    }
}
