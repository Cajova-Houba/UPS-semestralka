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

    public static final int MAX_ATTEMPTS = 10;

    public static final int MAX_TIMEOUT = 120000;

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

    /**
     * Sends a new nick to the server.
     *
     * @param nick nickname.
     * @param successCallback Called if the nick is sent. Received: OK
     * @param failCallback Called if the nick sending fails.
     * @return GENERAL_ERROR if there's no connection and NO_ERROR if everything is ok.
     */
    public Error sendNick(String nick,
                          EventHandler<WorkerStateEvent> successCallback,
                          EventHandler<WorkerStateEvent> failCallback) {
        if(!isConnected()) {
            return Error.GENERAL_ERROR("No active connection.");
        }

        nickService.setNick(nick);
        nickService.setOutToServer(outToServer);
        nickService.setOnSucceeded(event -> {
            lastSuccessfulConnection = new LoginData(nick, lastSuccessfulConnection.getAddress(), lastSuccessfulConnection.getPort());
            successCallback.handle(event);
        });
        nickService.setOnFailed(failCallback);
        nickService.restart();

        return Error.NO_ERROR();
    }

    /**
     * Sends an end turn message to the server.
     *
     * @param firstPlayerTurnWord Turn word of the first player.
     * @param secondPlayerTurnWord Turn word of the second player.
     * @param successCallback Called if the message was sent.
     * @param failCallback Called if there's some error during sending the message.
     * @return GENERAL_ERROR if there's no connection and NO_ERROR if everything is ok.
     */
    public Error sendEndturn(int[] firstPlayerTurnWord,
                             int[] secondPlayerTurnWord,
                             EventHandler<WorkerStateEvent> successCallback,
                             EventHandler<WorkerStateEvent> failCallback) {
        if(!isConnected()) {
            return Error.GENERAL_ERROR("No active connection.");
        }

        turnService.setOutToServer(outToServer);
        turnService.setFirstTurnWord(firstPlayerTurnWord);
        turnService.setSecondTurnWord(secondPlayerTurnWord);
        turnService.setOnSucceeded(successCallback);
        turnService.setOnFailed(failCallback);
        turnService.restart();

        return Error.NO_ERROR();
    }

    /**
     * Reads 1 response from server.
     *
     * @param successCallback Called if the response is obtained.
     * @param failCallback Called if error occurs. ReceivingException is used.
     * @return GENERAL_ERROR if there's no connection and NO_ERROR if everything is ok.
     */
    public Error getResponse(EventHandler<WorkerStateEvent> successCallback,
                             EventHandler<WorkerStateEvent> failCallback) {
        if(!isConnected()) {
            return Error.GENERAL_ERROR("No active connection.");
        }

        receivingService.setInFromServer(inFromServer);
        receivingService.setOnSucceeded(successCallback);
        receivingService.setOnFailed(failCallback);
        receivingService.restart();

        return Error.NO_ERROR();

    }

    public Error sendExitMessage(EventHandler<WorkerStateEvent> successCallback,
                                 EventHandler<WorkerStateEvent> failCallback) {
        if(!isConnected()) {
            return Error.GENERAL_ERROR("No active connection.");
        }

        ExitService exitService = new ExitService();
        exitService.setOutToServer(outToServer);
        exitService.setOnSucceeded(successCallback);
        exitService.setOnFailed(failCallback);
        exitService.restart();

        return Error.NO_ERROR();
    }

    public Error sendOkMessage(EventHandler<WorkerStateEvent> successCallback,
                               EventHandler<WorkerStateEvent> failCallback) {
        if(!isConnected()) {
            return Error.GENERAL_ERROR("No active connection.");
        }

        OkService okService = new OkService();
        okService.setOutToServer(outToServer);
        okService.setOnSucceeded(successCallback);
        okService.setOnFailed(failCallback);
        okService.restart();

        return Error.NO_ERROR();
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
     * start turn message is received.
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
}
