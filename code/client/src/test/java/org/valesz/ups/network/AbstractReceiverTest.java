package org.valesz.ups.network;

import org.junit.Test;
import org.valesz.ups.common.error.*;
import org.valesz.ups.common.message.received.AbstractReceivedMessage;
import org.valesz.ups.common.message.received.ErrorReceivedMessage;
import org.valesz.ups.common.message.received.ReceivedMessageTypeResolver;
import org.valesz.ups.common.message.received.StartGameReceivedMessage;

import java.io.*;

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

    @Test
    public void testReceiveStartGameMessage() throws IOException, ReceivingException {
        String startGameMsg = "\nInFStaRt_GaMenick1,nick2;";
        AbstractReceiver receiver = new AbstractReceiver() {
            @Override
            protected AbstractReceivedMessage call() throws Exception {
                return null;
            }
        };

        InputStream is = new ByteArrayInputStream(startGameMsg.getBytes());
        AbstractReceivedMessage receivedMessage = receiver.receiveMessage(new DataInputStream(is));
        assertNotNull("Null returned!",receivedMessage);
        StartGameReceivedMessage message = ReceivedMessageTypeResolver.isStartGame(receivedMessage);
        assertNotNull("Wrong message received! "+receivedMessage.toString(), message);
        assertEquals("Wrong first nick received!", "nick1", message.getFirstNickname());
        assertEquals("Wrong second nick received!", "nick2", message.getSecondNickname());
    }

    @Test
    public void testReceiveBadStartGameMessage() {
        String badMsg1 = "InFStart_GAmfnick1,nick2;";       // bad start_game
        String badMsg2 = "InFStart_GaMEnc,nick2;";          // first nick too short
        String badMsg3 = "InFStart_Gamenic;nck;";           // bad delimiter
        String badMsg4 = "infstart_gameasdasdasdasdasda";   // bad nick format
        String badMsg5 = "infstart_game";                   // no nick
        String badMsg6 = "infstart_gamenick1,nick2";        // no delimiter for nick 2
        String badMsg7 = "infstart_gamenick1,ni;";          // nick 2 too short
        String[] badNickMessages = new String[] {badMsg2, badMsg3, badMsg4, badMsg5, badMsg6, badMsg7};
        AbstractReceiver receiver = new AbstractReceiver() {
            @Override
            protected AbstractReceivedMessage call() throws Exception {
                return null;
            }
        };
        InputStream is = null;

        // first bad message should throw bad content exception
        is = new ByteArrayInputStream(badMsg1.getBytes());
        try {
            receiver.receiveMessage(new DataInputStream(is));
            fail("Bad message content exception expected!");
        } catch (BadMsgContentException ex) {
            // ok
        } catch (Exception ex) {
            fail("Unexpected exception: "+ex.getMessage());
        }

        // rest of the messages should throw bad nick exceptions
        for(int i = 0; i < badNickMessages.length; i++) {
            is = new ByteArrayInputStream(badNickMessages[i].getBytes());

            try {
                receiver.receiveMessage(new DataInputStream(is));
                fail("Bad nick format exception expected!");
            } catch (BadNickFormatException ex) {
                // ok
            } catch (Exception ex) {
                fail("Unexpected exception: "+ex.getMessage());
            }
        }
    }

    /**
     * Receive malformed start turn.
     */
    @Test
    public void testReceiveStartTurnFail(){
        String badMsg1 = "cmdStaRt_URN";                            // bad START_TURN
        String badMsg2 = "cmdstart_turna10300507090204060810";      // bad first player turn word
        String badMsg3 = "cmdstart_turn01030050709g204060810";      // bad second player turn word
        String badMsg4 = "cmdstart_turn010300507090";               // bad turn word length
        String[] badMsgs = new String[] {
                badMsg1, badMsg2, badMsg3, badMsg4
        };
        AbstractReceiver receiver = new AbstractReceiver() {
            @Override
            protected AbstractReceivedMessage call() throws Exception {
                return null;
            }
        };
        for (int i = 0; i < badMsgs.length; i++) {
            try {
                DataInputStream inToServer = new DataInputStream(new ByteArrayInputStream(badMsgs[i].getBytes()));
                receiver.receiveMessage(inToServer);
            } catch (BadMsgContentException ex) {
                // ok
            } catch (Exception e) {
                fail("Unexpected exception: "+e.getMessage());
            }
        }
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
