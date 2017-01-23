package org.valesz.ups.network;

import org.junit.Test;
import org.valesz.ups.common.error.EndOfStreamReached;
import org.valesz.ups.common.error.ErrorCode;
import org.valesz.ups.common.error.MaxAttemptsReached;
import org.valesz.ups.common.message.received.AbstractReceivedMessage;
import org.valesz.ups.common.message.received.ErrorReceivedMessage;
import org.valesz.ups.common.message.received.ReceivedMessageTypeResolver;

import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Zdenek Vales
 */
public class PreStartReceiverTest {

    @Test
    public void testWaitForNickConfirm() throws IOException, MaxAttemptsReached, EndOfStreamReached {
        String serverResponse = "\nInFoK";
        AbstractReceivedMessage receivedMessage = null;
        DataInputStream inFromServer = new DataInputStream(new ByteArrayInputStream(serverResponse.getBytes()));
        DataOutputStream outToServer = new DataOutputStream(new ByteArrayOutputStream());
        PreStartReceiver psr = prepareOkErrReceiver();

        receivedMessage = psr.waitForMessage(inFromServer, outToServer);
        assertNotNull("Null received!", receivedMessage);
        assertNotNull("Wrong message received! "+receivedMessage.toString(), ReceivedMessageTypeResolver.isOk(receivedMessage));
    }

    @Test
    public void testWaitForNickConfirmAlmostFail() throws IOException, MaxAttemptsReached, EndOfStreamReached {
        String serverResponse = "\nasdfghjklinfok";         // MAX_ATTEMPTS-1 of bad characters, then ok message
        AbstractReceivedMessage receivedMessage = null;
        DataInputStream inFromServer = new DataInputStream(new ByteArrayInputStream(serverResponse.getBytes()));
        DataOutputStream outToServer = new DataOutputStream(new ByteArrayOutputStream());
        PreStartReceiver psr = prepareOkErrReceiver();

        receivedMessage = psr.waitForMessage(inFromServer, outToServer);
        assertNotNull("Null received!", receivedMessage);
        assertNotNull("Wrong message received! "+receivedMessage.toString(), ReceivedMessageTypeResolver.isOk(receivedMessage));
    }

    @Test
    public void testWaitForNickError() throws IOException, MaxAttemptsReached, EndOfStreamReached {
        String serverResponse = "\neRr"+ ErrorCode.BAD_NICKNAME.code;
        AbstractReceivedMessage receivedMessage = null;
        DataInputStream inFromServer = new DataInputStream(new ByteArrayInputStream(serverResponse.getBytes()));
        DataOutputStream outToServer = new DataOutputStream(new ByteArrayOutputStream());
        PreStartReceiver psr = prepareOkErrReceiver();

        receivedMessage = psr.waitForMessage(inFromServer, outToServer);
        assertNotNull("Null received!", receivedMessage);
        ErrorReceivedMessage err = ReceivedMessageTypeResolver.isError(receivedMessage);
        assertNotNull("Wrong message received! "+receivedMessage.toString(), err);
        assertEquals("Wrong error code received!", ErrorCode.BAD_NICKNAME, err.getContent().code);
    }

    @Test(expected = MaxAttemptsReached.class)
    public void testWaitForNickResponseFail1() throws IOException, MaxAttemptsReached, EndOfStreamReached {
        String serverResponse = "\nasdfghjklq";     // send gibberish message
        AbstractReceivedMessage receivedMessage = null;
        DataInputStream inFromServer = new DataInputStream(new ByteArrayInputStream(serverResponse.getBytes()));
        DataOutputStream outToServer = new DataOutputStream(new ByteArrayOutputStream());
        PreStartReceiver psr = prepareOkErrReceiver();

        psr.waitForMessage(inFromServer, outToServer);
    }

    private PreStartReceiver prepareOkErrReceiver() {
        return new PreStartReceiver(null, message -> {
            if(message == null) {
                return false;
            }

            return ReceivedMessageTypeResolver.isOk(message) != null || ReceivedMessageTypeResolver.isError(message) != null;
        }, TcpClient.MAX_TIMEOUT, TcpClient.MAX_ATTEMPTS);
    }
}
