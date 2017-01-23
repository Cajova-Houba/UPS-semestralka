package org.valesz.ups.network;

import org.junit.Test;
import org.valesz.ups.common.error.EndOfStreamReached;
import org.valesz.ups.common.error.MaxAttemptsReached;
import org.valesz.ups.common.message.received.AbstractReceivedMessage;
import org.valesz.ups.common.message.received.EndGameReceivedMessage;
import org.valesz.ups.common.message.received.ReceivedMessageTypeResolver;
import org.valesz.ups.common.message.received.StartTurnReceivedMessage;

import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * @author Zdenek Vales
 */
public class PostStartReceiverTest {

    /**
     * Receive START_TURN
     */
    @Test
    public void testReceiveStartTurn() throws IOException, MaxAttemptsReached, EndOfStreamReached {
        String serverResponse = "\nasfCMd01030507090204060810\n";
        DataInputStream inFromServer = new DataInputStream(new ByteArrayInputStream(serverResponse.getBytes()));
        DataOutputStream outToServer = new DataOutputStream(new ByteArrayOutputStream());
        PostStartReceiver psr = prepareStartTurnReceiver();

        AbstractReceivedMessage receivedMessage = psr.waitForMessage(inFromServer, outToServer);
        assertNotNull("Null returned!", receivedMessage);
        StartTurnReceivedMessage startTurn = ReceivedMessageTypeResolver.isStartTurn(receivedMessage);
        assertNotNull("Wrong message received! "+receivedMessage, startTurn);

        int pos1 = 1;
        int pos2 = 2;

        for (int i = 0; i < startTurn.getFirstPlayerStones().length; i++) {
            assertEquals("Wrong p1 stone position!", pos1, startTurn.getFirstPlayerStones()[i]);
            assertEquals("Wrong p2 stone position!", pos2, startTurn.getSecondPlayerStones()[i]);

            pos1 += 2;
            pos2 += 2;
        }

    }

    /**
     * Receive end game even if the start turn is expected.
     */
    @Test
    public void testReceiveStartTurnEndGame() throws IOException, MaxAttemptsReached, EndOfStreamReached {
        String serverResponse = "\nasfInFEND_GAMEvalesz;";
        DataInputStream inFromServer = new DataInputStream(new ByteArrayInputStream(serverResponse.getBytes()));
        DataOutputStream outToServer = new DataOutputStream(new ByteArrayOutputStream());
        PostStartReceiver psr = prepareStartTurnReceiver();

        AbstractReceivedMessage receivedMessage = psr.waitForMessage(inFromServer, outToServer);
        assertNotNull("Null returned!", receivedMessage);
        EndGameReceivedMessage endGame = ReceivedMessageTypeResolver.isEndGame(receivedMessage);
        assertNotNull("Wrong message received! "+receivedMessage, endGame);
        assertEquals("Wrong winner nick received!", "valesz", endGame.getContent());
    }

    private PostStartReceiver prepareStartTurnReceiver() {
        return new PostStartReceiver(null, message -> {
            if(message == null) {
                return false;
            }

            return ReceivedMessageTypeResolver.isStartTurn(message) != null;
        }, TcpClient.MAX_TIMEOUT, TcpClient.MAX_ATTEMPTS);
    }
}
