package org.valesz.ups.network;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.net.Socket;

/**
 * Service used for connecting to server.
 *
 * @author Zdenek Vales
 */
public class ConnectionService extends Service<Socket>{

    private final String address;
    private final int port;

    /**
     * A task which will return a socket if the connection is successful.
     */
    private class ConnectionTask extends Task<Socket> {

        private final String address;
        private final int port;

        public ConnectionTask(String address, int port) {
            this.address = address;
            this.port = port;
        }

        @Override
        protected Socket call() throws Exception {
            return new Socket(address, port);
        }
    }

    public ConnectionService(String address, int port) {
        this.address = address;
        this.port = port;
    }

    @Override
    protected Task<Socket> createTask() {
        return new ConnectionTask(address, port);
    }
}
