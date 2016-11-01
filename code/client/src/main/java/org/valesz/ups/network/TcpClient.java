package org.valesz.ups.network;

import org.apache.log4j.Logger;
import org.valesz.ups.common.error.Error;
import org.valesz.ups.common.error.ErrorCode;

import java.net.Socket;

/**
 * Class used for communication with senet server.
 *
 * @author Zdenek Vales
 */
public class TcpClient extends Thread {

    public static final Logger logger = Logger.getLogger(TcpClient.class.getName());

    private String address;
    private int port;
    private String nickname;
    private Socket socket;
    private State gameState;

    private final ThreadListener threadListener;

    /**
     * Possible states of thread.
     */
    private enum State {
        /**
         * The first gameState before thre client is connected to the server.
         */
        INIT,

        /**
         * Client tries to connect to the server and if unsuccessful, it will switch to the INIT gameState.
         */
        TRY_CONNECT,

        /**
         * Client was successfully connected to the server and is now waiting for the game to start.
         */
        CONNECTED,

        /**
         * This gameState means that the thread will be ended.
         */
        END
    }

    public TcpClient(ThreadListener threadListener) {
        this.threadListener = threadListener;
        logger.debug("Tcp client initialized.");
        gameState = State.INIT;
    }

    @Override
    public void run() {
        while(getGameState() != State.END) {
            switch (getGameState()) {
                /*
                   do nothing and wait for the address, port and nick to be provided.
                 */
                case INIT:
                    break;

                /*
                    try to connect.
                 */
                case TRY_CONNECT:
                    Error e = connect();
                    if(e.code == ErrorCode.NO_ERROR) {
                        logger.debug(String.format("Connected to %s:%d as %s.", address, port, nickname));
                        setGameState(State.CONNECTED);
                        threadListener.notifyOnOperationOk(this);
                    } else {
                        logger.debug(String.format("Error occured: %s.", e.code.name()));
                        setGameState(State.INIT);
                        threadListener.notifyOnError(this, e);
                    }

                    break;

                /*
                    wait for game to start.
                 */
                case CONNECTED:
                    break;

            }
        }
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    private synchronized State getGameState() {
        return this.gameState;
    }

    private synchronized void setGameState(State gameState) {
        this.gameState = gameState;
    }

    /**
     * If the thread is in INIT gameState, it will switch to TRY_CONNECT gameState
     * and will try to connect to the server.
     */
    public synchronized void tryConnect() {
        if (getGameState() == State.INIT) {
            setGameState(State.TRY_CONNECT);
        }
    }

    /**
     * Method tries to connect to server with provided address, port and nickname.
     */
    private Error connect() {
        return Error.NO_ERROR();
    }

    /**
     * Sends a simple text message to the socket.
     * @param message Message to be sent.
     */
    private void sendMessage(String message) {

    }
}
