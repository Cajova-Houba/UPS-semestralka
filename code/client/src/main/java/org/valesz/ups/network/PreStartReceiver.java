package org.valesz.ups.network;

import javafx.concurrent.Task;
import org.valesz.ups.common.message.received.AbstractReceivedMessage;
import org.valesz.ups.common.message.received.ExpectedMessageComparator;

import java.io.DataInputStream;
import java.net.Socket;
import java.util.Comparator;

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
     * Comparator which will return 0 if the received AbstractReceivedMessage is the expected one.
     */
    private final ExpectedMessageComparator expectedMessageComparator;

    public PreStartReceiver(Socket socket, ExpectedMessageComparator expectedMessageComparator) {
        this.socket = socket;
        this.expectedMessageComparator = expectedMessageComparator;
    }

    @Override
    protected AbstractReceivedMessage call() throws Exception {
        socket.setSoTimeout(MAX_WAITING_TIMEOUT);
        DataInputStream inFromServer = new DataInputStream(socket.getInputStream());
        AbstractReceivedMessage receivedMessage = null;

        while (!expectedMessageComparator.isExpected(receivedMessage)) {
            receivedMessage = receiveMessage(inFromServer);

            // handle some errors

            // handle alive message

            // handle other messages
        }

        return receivedMessage;
    }
}
