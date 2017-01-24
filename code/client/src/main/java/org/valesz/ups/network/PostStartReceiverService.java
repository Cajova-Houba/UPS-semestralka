package org.valesz.ups.network;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.valesz.ups.common.message.received.AbstractReceivedMessage;
import org.valesz.ups.common.message.received.ExpectedMessageComparator;

import java.net.Socket;

/**
 * A service which will create a new PostStartReceiver.
 *
 * Use this to receive messages after the game has started.
 *
 * @author Zdenek Vales
 */
public class PostStartReceiverService extends Service<AbstractReceivedMessage> {

    private Socket socket;

    private ExpectedMessageComparator expectedMessageComparator;

    private int maxTimeoutMs;

    private int maxAttempts;

    private PostStartReceiver task;

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public void setExpectedMessageComparator(ExpectedMessageComparator expectedMessageComparator) {
        this.expectedMessageComparator = expectedMessageComparator;
    }

    public void setMaxTimeoutMs(int maxTimeoutMs) {
        this.maxTimeoutMs = maxTimeoutMs;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    @Override
    public boolean cancel() {
        if(task != null) {
            task.setShutdown();
        }
        return super.cancel();
    }

    @Override
    protected Task<AbstractReceivedMessage> createTask() {
        task = new PostStartReceiver(socket, expectedMessageComparator, maxTimeoutMs, maxAttempts);
        return task;
    }
}
