package org.valesz.ups.network;

import org.junit.Test;
import org.valesz.ups.common.error.BadMsgContentException;
import org.valesz.ups.common.error.BadMsgTypeReceived;
import org.valesz.ups.common.error.ErrorCode;
import org.valesz.ups.common.error.ReceivingException;
import org.valesz.ups.common.message.received.AbstractReceivedMessage;
import org.valesz.ups.common.message.received.ErrorReceivedMessage;
import org.valesz.ups.common.message.received.ReceivedMessageTypeResolver;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Zdenek Vales
 */
public class AbstractReceiverTest {

    @Test
    public void testReceiveOkMessage() throws IOException, ReceivingException {
        String okMsg = "\n\niNfOk";
        AbstractReceiver receiver = new AbstractReceiver() {
            @Override
            protected AbstractReceivedMessage call() throws Exception {
                return null;
            }
        };

        InputStream is = new ByteArrayInputStream(okMsg.getBytes());
        AbstractReceivedMessage receivedMessage = receiver.receiveMessage(new DataInputStream(is));
        assertNotNull("Null returned!",receivedMessage);
        assertNotNull("Wrong message received! "+receivedMessage.toString(), ReceivedMessageTypeResolver.isOk(receivedMessage));
    }

    @Test
    public void testReceiveErrMessage() throws IOException, ReceivingException {
        String errMsg1 = "\n ErR50";
        String errMsg2 = "\n eRR01";
        ErrorReceivedMessage err = null;
        InputStream is = null;
        AbstractReceivedMessage receivedMessage = null;
        AbstractReceiver receiver = new AbstractReceiver() {
            @Override
            protected AbstractReceivedMessage call() throws Exception {
                return null;
            }
        };

        // receive general error
        is = new ByteArrayInputStream(errMsg1.getBytes());
        receivedMessage = receiver.receiveMessage(new DataInputStream(is));
        assertNotNull("Null returned!", receivedMessage);
        err = ReceivedMessageTypeResolver.isError(receivedMessage);
        assertNotNull("Wrong message type received! "+receivedMessage.toString(), err);
        assertEquals("Wrong error code received!", ErrorCode.GENERAL_ERROR, err.getContent().code);

        // receive unrecognized error
        is = new ByteArrayInputStream(errMsg2.getBytes());
        receivedMessage = receiver.receiveMessage(new DataInputStream(is));
        assertNotNull("Null returned!", receivedMessage);
        err = ReceivedMessageTypeResolver.isError(receivedMessage);
        assertNotNull("Wrong message type received! "+receivedMessage.toString(), err);
        assertEquals("Wrong error code received!", ErrorCode.UNRECOGNIZED_ERROR, err.getContent().code);
    }

    @Test
    public void testReceiveAliveMessage() throws IOException, ReceivingException {
        String aliveMsg = "\n\niNfaLiVE";
        AbstractReceiver receiver = new AbstractReceiver() {
            @Override
            protected AbstractReceivedMessage call() throws Exception {
                return null;
            }
        };

        InputStream is = new ByteArrayInputStream(aliveMsg.getBytes());
        AbstractReceivedMessage receivedMessage = receiver.receiveMessage(new DataInputStream(is));
        assertNotNull("Null returned!",receivedMessage);
        assertNotNull("Wrong message received! "+receivedMessage.toString(), ReceivedMessageTypeResolver.isAliveMessage(receivedMessage));
    }

    @Test(expected = BadMsgTypeReceived.class)
    public void testReceiveMessageBadMsgType() throws IOException, ReceivingException {
        String badMsg = "\n \ncMf";
        AbstractReceiver receiver = new AbstractReceiver() {
            @Override
            protected AbstractReceivedMessage call() throws Exception {
                return null;
            }
        };

        InputStream is = new ByteArrayInputStream(badMsg.getBytes());
        receiver.receiveMessage(new DataInputStream(is));
        fail("Bad message type not detected!");
    }

    @Test(expected = BadMsgContentException.class)
    public void testBadMsgContent() throws IOException, ReceivingException {
        String badMsg = "\n \ncMdASDSA";
        AbstractReceiver receiver = new AbstractReceiver() {
            @Override
            protected AbstractReceivedMessage call() throws Exception {
                return null;
            }
        };

        InputStream is = new ByteArrayInputStream(badMsg.getBytes());
        receiver.receiveMessage(new DataInputStream(is));
        fail("Bad message content not detected!");
    }


}
