package org.valesz.ups.network;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.valesz.ups.common.message.Message;
import org.valesz.ups.common.message.MessageType;

import java.io.DataOutputStream;

/**
 * A service which will send information about the end of turn
 * to server.
 *
 * @author Zdenek Vales
 */
public class TurnService extends Service<Object> {

    private byte[] firstTurnWord;
    private byte[] secondTurnWord;

    private DataOutputStream outToServer;

    public void setFirstTurnWord(int[] firstTurnWord) {
        this.firstTurnWord = new byte[firstTurnWord.length];
        for (int i = 0; i < firstTurnWord.length; i++) {
            this.firstTurnWord[i] = (byte) firstTurnWord[i];
        }
    }

    public void setSecondTurnWord(int[] secondTurnWord) {
        this.secondTurnWord = new byte[secondTurnWord.length];
        for (int i = 0; i < secondTurnWord.length; i++) {
            this.secondTurnWord[i] = (byte) secondTurnWord[i];
        }
    }

    public void setTurnWord(byte[] turnWord) {
        this.firstTurnWord = turnWord;
    }

    public void setOutToServer(DataOutputStream outToServer) {
        this.outToServer = outToServer;
    }

    @Override
    protected Task<Object> createTask() {
        return new MessageSender(new Message(MessageType.INF,
                new String(firstTurnWord)+new String(secondTurnWord)), outToServer);
    }
}
