package org.valesz.ups.common.message.received;

import org.valesz.ups.common.message.MessageType;

/**
 * Message containing the nick of the player the game is waiting for to reconnect.
 *
 * @author Zdenek Vales
 */
public class WaitingForPlayerReceivedMessage extends AbstractReceivedMessage<String>{

    private String nick;

    public WaitingForPlayerReceivedMessage(String nick) {
        this.nick = nick;
    }

    @Override
    public String getContent() {
        return nick;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.INF;
    }
}
