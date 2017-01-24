package org.valesz.ups.network;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.valesz.ups.common.error.Error;
import org.valesz.ups.common.error.ReceivingException;
import org.valesz.ups.common.message.Message;
import org.valesz.ups.common.message.received.AbstractReceivedMessage;
import org.valesz.ups.common.message.received.ReceivedMessageTypeResolver;
import org.valesz.ups.model.LoginData;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

/**
 * Class used for communication with senet server.
 *
 * @author Zdenek Vales
 */
public class TcpClient {

    public static final Logger logger = LogManager.getLogger(TcpClient.class);

    /**
     * Max time to wait for new turn message.
     */
    public static final int MAX_WAITING_TIMEOUT = 1000;

    public static final int NO_TIMEOUT = -1;

    public static final int MAX_ATTEMPTS = 10;

    public static final int MAX_TIMEOUT = 9000;

    public static final int INF_ATTEMPTS = -1;

    private LoginData lastSuccessfulConnection;
    private Socket socket;
    private NickService nickService;
    private ReceivingService receivingService;
    private TurnService turnService;
    private ConnectionService connectionService;
    private DataOutputStream outToServer;
    private DataInputStream inFromServer;

    private PreStartReceiverService preStartReceiverService;
    private PostStartReceiverService postStartReceiverService;

    /**
     * Tries to connect to the server and if the connection is successful,
     * NO_ERROR is returned.
     * @param address
     * @param port
     * @return
     */
    public Error connect(String address, int port,
                         EventHandler<WorkerStateEvent> successCallback,
                         EventHandler<WorkerStateEvent> failCallback) {

        connectionService = new ConnectionService(address, port);
        connectionService.setOnFailed(event -> {
            String err = String.format("Error while connecting to %s:%d",address, port);
            logger.error(err);

            failCallback.handle(event);
        });
        connectionService.setOnSucceeded(event -> {
            this.socket = connectionService.getValue();
            try {
                socket.setReuseAddress(true);
//                socket.getKeepAlive();
                socket.setSoTimeout(MAX_WAITING_TIMEOUT);
                nickService = new NickService();
                inFromServer = new DataInputStream(socket.getInputStream());
                outToServer = new DataOutputStream(socket.getOutputStream());
                receivingService = new ReceivingService();
                turnService = new TurnService();
                preStartReceiverService = new PreStartReceiverService();
                postStartReceiverService = new PostStartReceiverService();

                lastSuccessfulConnection = new LoginData("", address, port);
            } catch (IOException e) {
                logger.debug("Error setting reuse address.");
                socket = null;

                failCallback.handle(event);
                return;
            }

            successCallback.handle(event);
        });
        connectionService.restart();

        return Error.NO_ERROR();
    }

    /**
     * Disconnect from the server.
     */
    public void disconnect() {
        if(isConnected()) {
            try {
                if(preStartReceiverService != null) {
                    preStartReceiverService.cancel();
                }
                if(postStartReceiverService != null) {
                    postStartReceiverService.cancel();
                }
                socket.close();
            } catch (IOException e) {
                logger.error("Error while closing the socket: "+e.getMessage());
            }
        }
        socket = null;
    }

    /**
     * Returns true if the socket is active.
     * @return
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    public void sendEndTurnMessage(int[] firstPlayerTurnWord,
                                   int[] secondPlayerTurnWord) throws IOException {
        if(!isConnected()) {
            return ;
        }

        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        dos.write(Message.createEndTurnMessage(firstPlayerTurnWord, secondPlayerTurnWord).toBytes());
    }

    public void sendExitMessage() throws IOException {
        if(!isConnected()) {
            return ;
        }

        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        dos.write(Message.createExitMessage().toBytes());
    }

    public void sendOkMessage() throws IOException {
        if(!isConnected()) {
            return ;
        }

        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        dos.write(Message.createOKMessage().toBytes());
    }

    public NickService getNickService() {
        return nickService;
    }

    public ReceivingService getReceivingService() {
        return receivingService;
    }

    public Socket getSocket() {
        return socket;
    }

    public LoginData getLastSuccessfulConnection() {
        return lastSuccessfulConnection;
    }

    public ConnectionService getConnectionService() {
        return connectionService;
    }

    public PreStartReceiverService getPreStartReceiverService() {
        return preStartReceiverService;
    }

    public PostStartReceiverService getPostStartReceiverService() {
        return postStartReceiverService;
    }

    /**
     * Uses PreStartReceiverService to wait for response. Waiting is successful if
     * ok message or error message is received in time.
     * @param successCallback
     * @param failCallback
     */
    public void waitForNickConfirm(EventHandler<WorkerStateEvent> successCallback,
                                   EventHandler<WorkerStateEvent> failCallback) {
        preStartReceiverService.setOnSucceeded(successCallback);
        preStartReceiverService.setOnFailed(failCallback);
        preStartReceiverService.setMaxAttempts(MAX_ATTEMPTS);
        preStartReceiverService.setMaxTimeoutMs(MAX_TIMEOUT);
        preStartReceiverService.setSocket(socket);
        preStartReceiverService.setExpectedMessageComparator(message -> {
            if(message == null) {
                return false;
            }

            // accept ok messages and errors
            if(ReceivedMessageTypeResolver.isOk(message) != null || ReceivedMessageTypeResolver.isError(message) != null) {
                return true;
            } else {
                return false;
            }
        });
        preStartReceiverService.restart();
    }

    /**
     * Uses PreStartReceiverService to wait for response. Waiting is successful if
     * start game message is received.
     * @param successCallback
     * @param failCallback
     */
    public void waitForStartGame(EventHandler<WorkerStateEvent> successCallback,
                                 EventHandler<WorkerStateEvent> failCallback) {
        preStartReceiverService.setOnSucceeded(successCallback);
        preStartReceiverService.setOnFailed(failCallback);
        preStartReceiverService.setMaxAttempts(MAX_ATTEMPTS);
        preStartReceiverService.setMaxTimeoutMs(MAX_TIMEOUT);
        preStartReceiverService.setSocket(socket);
        preStartReceiverService.setExpectedMessageComparator(message -> {
            if(message == null) {
                return false;
            }
            // accept only start_game messages
            return ReceivedMessageTypeResolver.isStartGame(message) != null;
        });
        preStartReceiverService.restart();
    }

    /**
     * Uses PostStartReceiverService to receive either OK or error message indicating the validation of turn.
     * @param successCallback
     * @param failCallback
     */
    public void waitForTurnConfirm(EventHandler<WorkerStateEvent> successCallback,
                                   EventHandler<WorkerStateEvent> failCallback) {
//        postStartReceiverService.cancel();
        postStartReceiverService.setOnSucceeded(successCallback);
        postStartReceiverService.setOnFailed(failCallback);
        postStartReceiverService.setMaxTimeoutMs(MAX_TIMEOUT);
        postStartReceiverService.setMaxAttempts(MAX_ATTEMPTS);
        postStartReceiverService.setSocket(socket);
        postStartReceiverService.setExpectedMessageComparator(message -> {
            if(message == null) {
                return false;
            }

            // accept only start turn messages
            return ReceivedMessageTypeResolver.isOk(message) != null || ReceivedMessageTypeResolver.isError(message) != null;
        });
        postStartReceiverService.restart();
    }

    /**
     * Uses post-start game receiver to wait for response. Waiting is successful if start
     * turn message or end game message is received.
     * @param successCallback
     * @param failCallback
     */
    public void waitForMyTurn(EventHandler<WorkerStateEvent> successCallback,
                              EventHandler<WorkerStateEvent> failCallback) {
//        postStartReceiverService.cancel();
        postStartReceiverService.setOnSucceeded(successCallback);
        postStartReceiverService.setOnFailed(failCallback);
        postStartReceiverService.setMaxTimeoutMs(MAX_TIMEOUT);
        postStartReceiverService.setMaxAttempts(MAX_ATTEMPTS);
        postStartReceiverService.setSocket(socket);
        postStartReceiverService.setExpectedMessageComparator(message -> {
            if(message == null) {
                return false;
            }

            // accept only start turn messages
            return ReceivedMessageTypeResolver.isStartTurn(message) != null;
        });
        postStartReceiverService.restart();
    }

    /**
     * Using the PostStartGameReceiverService runs a task which will be periodically receiving messages,
     * while the player does his turn. This task is ended when either ok, err or end game message is received.
     * Ok or err is response to the sent end turn message.
     *
     * @param successCallback
     * @param failCallback
     */
    public void handleWhileTurn(EventHandler<WorkerStateEvent> successCallback,
                                EventHandler<WorkerStateEvent> failCallback) {
//        postStartReceiverService.cancel();
        postStartReceiverService.setOnSucceeded(successCallback);
        postStartReceiverService.setOnFailed(failCallback);
        postStartReceiverService.setMaxTimeoutMs(NO_TIMEOUT);
        postStartReceiverService.setMaxAttempts(INF_ATTEMPTS);
        postStartReceiverService.setSocket(socket);
        postStartReceiverService.setExpectedMessageComparator(message -> {

            if(message == null) {
                return false;
            }

            // nothing is expected
            return ReceivedMessageTypeResolver.isOk(message) != null || ReceivedMessageTypeResolver.isError(message) != null;
        });
        postStartReceiverService.restart();
    }

    /**
     * Sends a nick message to socket.
     * @param nick
     */
    public void sendNickMessage(String nick) throws IOException {
        if (!socket.isConnected()) {
            return;
        }

        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        dos.write(Message.createNickMessage(nick).toBytes());
    }

    /**
     * Checks, if the other side of current connection is still alive.
     * @param maxTimeout
     * @return
     */
    public boolean isAlive(int maxTimeout) {
        if(!isConnected()) {
            return false;
        }

        Socket s = new Socket();

        try {
            s.connect(new InetSocketAddress(socket.getInetAddress().getHostAddress(), socket.getPort()), maxTimeout);
            s.close();
            return true;
        } catch (Exception e) {
            return  false;
        }

    }

    public void addLastSuccessfulNick(String nick) {
        lastSuccessfulConnection = new LoginData(nick, lastSuccessfulConnection.getAddress(), lastSuccessfulConnection.getPort());
    }
}
