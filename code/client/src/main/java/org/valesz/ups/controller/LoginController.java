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

/**
 * Controller for fetching login and displaying errors in login pane.
 *
 * @author Zdenek Vales
 */
public class LoginController {

    private static final Logger logger = LogManager.getLogger(LoginController.class);

    private TcpClient tcpClient;

    private LoginPane view;

    public LoginController(TcpClient tcpClient, LoginPane view) {
        this.tcpClient = tcpClient;
        this.view = view;
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

    public void login(LoginData loginData) {

        //check nick
        if(!checkNick(loginData.getNick())) {
            return;
        }

        // connect to server
        Error res = tcpClient.connect(loginData.getAddress(), loginData.getPort());
        if (!res.ok()) {
            view.displayMessage(res.msg);
            return;
        }

        //send nick
        tcpClient.sendNick(loginData.getNick(),
                e -> {
                    //get response
                    tcpClient.getResponse(
                            event -> {
                                //check received response
                                AbstractReceivedMessage response = tcpClient.getReceivingService().getValue();
                                if (ReceivedMessageTypeResolver.isOk(response) == null) {
                                    // check error
                                    ErrorReceivedMessage err = ReceivedMessageTypeResolver.isError(response);
                                    if(err != null) {
                                        logger.error("Server returned an error: "+err.getContent().toString());
                                        view.displayMessage(ErrorMessages.getErrorForCode(err.getContent().code));
                                    } else {
                                        // wrong response
                                        logger.error("Wrong response received. "+response);
                                        view.displayMessage("Chyba při odesílání nicku na server.");
                                    }
                                    tcpClient.disconnect();
                                } else {
                                    // nick ok
                                    Game.getInstance().waitingForOpponent(loginData.getNick());
                                    logger.debug("Login ok.");
                                    MainApp.switchToMain();
                                }
                            },
                            event -> {
                                //receiving response failed
                                String err = tcpClient.getReceivingService().getException().getMessage();
                                logger.error("Error while receiving response from server: "+err);
                                view.displayMessage("Chyba při odesílání nicku na server.");
                                tcpClient.disconnect();
                            });

                },
                e -> {
                    //sending nick failed
                    String err = tcpClient.getNickService().getException().getMessage();
                    logger.error("Error while sending nick to server: "+err);
                    view.displayMessage("Chyba při odesílání nicku na server.");
                    tcpClient.disconnect();
                });
    }
}
