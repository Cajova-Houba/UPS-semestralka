package org.valesz.ups.network;

import org.valesz.ups.common.error.EndOfStreamReached;
import org.valesz.ups.common.error.MaxAttemptsReached;
import org.valesz.ups.common.error.ReceivingException;
import org.valesz.ups.common.message.Message;
import org.valesz.ups.common.message.received.AbstractReceivedMessage;
import org.valesz.ups.common.message.received.ExpectedMessageComparator;
import org.valesz.ups.common.message.received.ReceivedMessageTypeResolver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * This task will handle all incoming messages in the pre-start game state.
 * It will be receiving message until the expected one arrives.
 *
 * Used while waiting for nick confirm and new game start.
 *
 * @author Zdenek Vales
 */
public class PreStartReceiver extends AbstractReceiver {

    private final Socket socket;

    /**
     * For every attempt to receive the expected message which times out, a counter will be increased by MAX_WAITING_TIMEOUT.
     * Once this counter reaches maxTimeoutMs, alive message is sent. If OK is received, attempt counter is increased.
     * and timeout counter is set to 0. Otherwise SocketTimeoutException is thrown.
     */
    private final int maxTimeoutMs;

    /**
     * For every attempt to receive the expected message which ends up with exception thrown or receiving an
     * unexpected message, a counter will be increased by one. Once this counter reaches maxAttempts,
     * receiver will throw a MaxAttemptsReached.
     */
    private final int maxAttempts;

    /**
     * Comparator which will return true if the received AbstractReceivedMessage is the expected one.
     */
    private final ExpectedMessageComparator expectedMessageComparator;

    /**
     * Exception thrown in call() method.
     * @exception SocketTimeoutException maxTimeoutMs is reached.
     * @exception IOException Exception during reading/writing from/to data stream.
     * @exception MaxAttemptsReached Max number of attempts reached while receiving the expected message.
     * @exception EndOfStreamReached Thrown when the unexpected end of stream is reached.

     */
    public PreStartReceiver(Socket socket, ExpectedMessageComparator expectedMessageComparator, int maxTimeoutMs, int maxAttempts) {
        this.socket = socket;
        this.expectedMessageComparator = expectedMessageComparator;
        this.maxTimeoutMs = maxTimeoutMs;
        this.maxAttempts = maxAttempts;
    }

    /**
     * Tries tto receive the OK message. If exception raises or anything else is received,
     * null is returned.
     * @param inFromServer
     * @return
     */
    private AbstractReceivedMessage receiveOk(DataInputStream inFromServer) {
        AbstractReceivedMessage receivedMessage = null;
        try {
            receivedMessage = receiveMessage(inFromServer);
        } catch (Exception e) {
            return  null;
        }

        return ReceivedMessageTypeResolver.isOk(receivedMessage);
    }

    /**
     * If the value of attempts is >= maxAttempts,
     * MaxAttemptsReached is thrown.
     * @param attempts
     */
    private void checkAttempts(int attempts) throws MaxAttemptsReached {
        if(attempts >= maxAttempts) {
            logger.error("Maximum number of attempts reached.");
            throw new MaxAttemptsReached();
        }
    }

    public AbstractReceivedMessage waitForMessage(DataInputStream inFromServer, DataOutputStream outToServer) throws IOException, MaxAttemptsReached, EndOfStreamReached {
        AbstractReceivedMessage receivedMessage = null;
        AbstractReceivedMessage okReceived = null;
        int timeoutCntr = 0;
        int attemptCntr = 0;

        while (!expectedMessageComparator.isExpected(receivedMessage)) {

            // check before and after waiting for message
            if(Thread.currentThread().isInterrupted()) {
                return null;
            }

            try {
                receivedMessage = receiveMessage(inFromServer);
                if(Thread.currentThread().isInterrupted()) {
                    return null;
                }

                // handle some errors
            } catch (SocketTimeoutException ex) {
                // socket timed out => increase cntr
//                logger.warn("Socket timed out, increasing the timeout counter.");
                timeoutCntr += MAX_WAITING_TIMEOUT;
                if(timeoutCntr >= maxTimeoutMs) {
                    logger.error("Max timeout reached. Sending is alive message");
                    outToServer.write(Message.createOKMessage().toBytes());

                    // receive ok message
                    okReceived = receiveOk(inFromServer);
                    if(okReceived == null) {
                        logger.error("Server not responding.");
                        throw new SocketTimeoutException();
                    } else {
                        logger.debug("Server lives, incrementing attempt counter.");
                        timeoutCntr = 0;
                        attemptCntr++;
                        // check attempts
                        checkAttempts(attemptCntr);
                    }
                }
                continue;

            } catch (EndOfStreamReached ex) {
                throw ex;
            } catch (ReceivingException ex) {
                // error occured => increase the attemptCntr
                logger.warn("Error occurred while receiving the message: "+ex.error.code.name()+". Increasing the attempt counter.");
                attemptCntr++;
                // check attempts
                checkAttempts(attemptCntr);
                continue;
            }

            // handle expected message
            if (expectedMessageComparator.isExpected(receivedMessage)) {
                logger.debug("Expected message received.");

                // handle alive message
            } else if(ReceivedMessageTypeResolver.isAliveMessage(receivedMessage) != null) {
                // send ok
                logger.debug("Is alive message received, sending ok.");
                outToServer.write(Message.createOKMessage().toBytes());

                // handle unexpected message
            } else {
                logger.debug("Unexpected message received: "+receivedMessage.toString()+". incrementing the attempt counter.");
                attemptCntr++;
                checkAttempts(attemptCntr);
            }
        }

        return receivedMessage;
    }

    @Override
    protected AbstractReceivedMessage call() throws Exception {
        socket.setSoTimeout(MAX_WAITING_TIMEOUT);
        DataInputStream inFromServer = new DataInputStream(socket.getInputStream());
        DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
        return waitForMessage(inFromServer, outToServer);
    }
}
