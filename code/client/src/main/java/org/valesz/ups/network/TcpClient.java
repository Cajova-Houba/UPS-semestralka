package org.valesz.ups.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.valesz.ups.common.error.Error;
import org.valesz.ups.common.message.Message;
import org.valesz.ups.common.message.MessageType;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Class used for communication with senet server.
 *
 * @author Zdenek Vales
 */
public class TcpClient{

    public static final Logger logger = LogManager.getLogger(TcpClient.class);

    private Socket socket;

    /**
     * Tries to connect to the server and if the connection is successful,
     * NO_ERROR is returned.
     * @param address
     * @param port
     * @return
     */
    public Error connect(String address, int port) {
        Socket tmpSocket;
        String errMsg;

        logger.debug(String.format("Connecting to %s:%d.", address, port));

        try {
            tmpSocket = new Socket(address, port);
        } catch (IOException e) {
            errMsg = String.format("Error while connecting to %s:%d",address, port);
            logger.error(errMsg);
            return Error.GENERAL_ERROR(errMsg);
        }

        socket = tmpSocket;

        logger.debug("Connected.");
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
    }

    /**
     * Returns true if the socket is active.
     * @return
     */
    public boolean isConnected() {
        return socket.isConnected();
    }

    /**
     * Sends a new nick to the server. If the nick is accepted by the server,
     * NO_ERROR will be returned.
     * @param nick nickname.
     * @return
     */
    public Error sendNick(String nick) {
        String errMsg;
        if(!isConnected()) {
            return Error.GENERAL_ERROR("No active connection.");
        }

        Message message = new Message(MessageType.CMD,nick);
        logger.debug("Sending message: "+message.toString());

        try {
            sendMessage(message);
        } catch (IOException e) {
            errMsg = String.format("Error while sending nick to server: %s.",e.getMessage());
            logger.error(errMsg);
            return Error.GENERAL_ERROR(errMsg);
        }

        return Error.NO_ERROR();
    }

    /**
     * Send a message to socket.
     * @param message
     */
    public void sendMessage(Message message) throws IOException {
        DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());

        // send message
        outToServer.write(message.toBytes());

        outToServer.close();
    }

}
