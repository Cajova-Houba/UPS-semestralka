package org.valesz.ups.common.message.received;

import org.valesz.ups.common.message.MessageType;

/**
 * @author Zdenek Vales
 */
public class EndGameReceivedMessage extends AbstractReceivedMessage<String> {

    private String winner;

    public EndGameReceivedMessage(String winner) {
        this.winner = winner;
    }

    /**
     * Returns the winner of the game.
     * If the returned content is empty, no-one wins and the game
     * has ended because of some error.
     * @return
     */
    @Override
    public String getContent() {
        return winner;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.INF;
    }
}
