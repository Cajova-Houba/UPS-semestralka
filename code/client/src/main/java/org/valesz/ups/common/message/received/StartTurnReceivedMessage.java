package org.valesz.ups.common.message.received;

import org.valesz.ups.common.message.MessageType;

/**
 * Message from server that indicates beginning of the new turn.
 *
 * @author Zdenek Vales
 */
public class StartTurnReceivedMessage extends AbstractReceivedMessage<int[]> {

    private int[] firstPlayerStones;
    private int[] secondPlayerStones;

    public StartTurnReceivedMessage(int[] firstPlayerStones, int[] secondPlayerStones) {
        this.firstPlayerStones = firstPlayerStones;
        this.secondPlayerStones = secondPlayerStones;
    }

    public int[] getFirstPlayerStones() {
        return firstPlayerStones;
    }

    public int[] getSecondPlayerStones() {
        return secondPlayerStones;
    }

    /**
     * Returns both sets of stones.
     * @return
     */
    @Override
    public int[] getContent() {
        int[] both = new int[firstPlayerStones.length + secondPlayerStones.length];
        for (int i = 0; i < firstPlayerStones.length; i++) {
            both[i] = firstPlayerStones[i];
            both[i+5] = secondPlayerStones[i];
        }

        return both;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.CMD;
    }
}
