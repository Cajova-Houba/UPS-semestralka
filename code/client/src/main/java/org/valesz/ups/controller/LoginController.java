package org.valesz.ups.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.valesz.ups.common.Constraits;
import org.valesz.ups.common.error.Error;
import org.valesz.ups.common.error.ErrorMessages;
import org.valesz.ups.common.message.received.AbstractReceivedMessage;
import org.valesz.ups.common.message.received.ErrorReceivedMessage;
import org.valesz.ups.common.message.received.ReceivedMessageTypeResolver;
import org.valesz.ups.main.MainApp;
import org.valesz.ups.model.LoginData;
import org.valesz.ups.model.game.Game;
import org.valesz.ups.network.TcpClient;
import org.valesz.ups.ui.LoginPane;

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * Controller for fetching login and displaying errors in login pane.
 *
 * @author Zdenek Vales
 */
public class LoginController {

    private static final Logger logger = LogManager.getLogger(LoginController.class);

    private TcpClient tcpClient;

    private LoginPane view;

    private ViewController viewController;

    public LoginController(TcpClient tcpClient) {
        this.tcpClient = tcpClient;
    }

    public void setView(LoginPane view) {
        this.view = view;
    }


    public void setViewController(ViewController viewController) {
        this.viewController = viewController;
    }

    /**
     * Checks nick and returns true if the nick is ok.
     * @param nick
     * @return
     */
    private boolean checkNick(String nick) {
        switch (Constraits.checkNick(nick)) {
            case 0:
                        /*ok*/
                return true;
            case 1:
                view.displayMessage("První znak nicku musí být malé, nebo velké písmeno.");
                logger.warn("Bad nick length");
                return false;
            case 2:
                view.displayMessage("Nick může obsahovat pouze čísla a velká, nebo malá písmena.");
                logger.warn("Bad nick length");
                return false;

            default:
                return false;
        }
    }

    /**
     * Tries to connect to server and if successful, sends player's nick to server.
     */
    public void connect(LoginData loginData) {

        logger.debug(String.format("Connecting to %s:%d.", loginData.getAddress(), loginData.getPort()));

        if(tcpClient.isConnected()) {
            login(loginData);
            return;
        }

        view.disableLoginButton();
        view.displayMessage("");
        tcpClient.connect(loginData.getAddress(), loginData.getPort(),
                event -> {
                    //ok
                    logger.debug("Connected.");
                    view.enableLoginButton();
                    login(loginData);
                },
                event -> {
                    // not ok
                    Throwable ex = tcpClient.getConnectionService().getException();
                    if(ex instanceof SocketTimeoutException ) {
                        logger.error(String.format("Connection to %s:%d timed out.",loginData.getAddress(), loginData.getPort()));
                        view.displayMessage(String.format("Během připojování k %s:%d vypršel čas.", loginData.getAddress(), loginData.getPort()));
                    } else {
                        logger.error(String.format("Error while connecting to %s:%d. Cause: %s",loginData.getAddress(), loginData.getPort(), ex.getMessage()));
                        view.displayMessage(String.format("Klient se nemohl připojit k %s:%d.", loginData.getAddress(), loginData.getPort()));
                    }
                    view.enableLoginButton();
                });

    }

    /**
     * Do not call this method directly, call connect() first.
     * @param loginData
     */
    public void login(LoginData loginData) {

        //check nick
        if(!checkNick(loginData.getNick())) {
            return;
        }

        if(!tcpClient.isConnected()) {
            logger.warn("Tcp client not connected!");
            return;
        }

        // send nick
        logger.debug("Sending nick "+loginData.getNick()+" to server.");
        try {
            tcpClient.sendNickMessage(loginData.getNick());
        } catch (IOException ex) {
            logger.error("Exception while sending the nick: "+ex.getMessage());
            tcpClient.disconnect();
            view.displayMessage("Chyba při odesílání nicku na server.");
            return;
        }

        // wait for ok message
        tcpClient.waitForNickConfirm(
                event -> {
                    // ok or error message received
                    //check received response
                    AbstractReceivedMessage response = tcpClient.getPreStartReceiverService().getValue();
                    if (ReceivedMessageTypeResolver.isOk(response) != null) {
                        // nick ok
                        Game.getInstance().waitingForOpponent(loginData.getNick());
                        logger.debug("Login ok.");
                        int port = tcpClient.getSocket().getLocalPort();
                        viewController.displayMainPane(port, loginData.getNick());
                    } else {
                        // error received
                        ErrorReceivedMessage err = ReceivedMessageTypeResolver.isError(response);
                        logger.error("Server returned an error: "+err.getContent().toString());
                        view.displayMessage(ErrorMessages.getErrorForCode(err.getContent().code));
                    }
                },

                event -> {
                    //receiving response failed
                    String err = tcpClient.getPreStartReceiverService().getException().getMessage();
                    logger.error("Error while receiving response from server: "+err);
                    view.displayMessage("Chyba při odesílání nicku na server.");
                    tcpClient.disconnect();
                }
        );
    }

    /**
     * Tries to reconnect to the same server. If no lastSuccessfulConnection exists, switches to LoginPane.
     */
    public void reconnect() {
        LoginData lastSuc = tcpClient.getLastSuccessfulConnection();
        if(lastSuc == null) {
            logger.warn("No last successful connection!");
            MainApp.switchToLogin();
        } else {
            connect(lastSuc);
        }
    }

    /**
     * Displays error message on login pane.
     * @param message
     */
    public void displayErrorMessage(String message) {
        view.displayMessage(message);
    }
}
