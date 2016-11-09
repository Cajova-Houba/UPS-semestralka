package org.valesz.ups.main.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.valesz.ups.common.Constraits;
import org.valesz.ups.common.error.Error;
import org.valesz.ups.common.error.ErrorMessages;
import org.valesz.ups.common.message.Message;
import org.valesz.ups.common.message.MessageType;
import org.valesz.ups.main.MainApp;
import org.valesz.ups.network.NickService;
import org.valesz.ups.network.TcpClient;

import java.net.Socket;

/**
 * Login window.
 *
 * @author Zdenek Vales
 */
public class LoginPane extends GridPane {

    private static final Logger logger = LogManager.getLogger(LoginPane.class);
    public static final Font DEF_FONT = Font.font("Tahoma", FontWeight.NORMAL, 20);

    public static final String DEFAULT_USERNAME = "valesz";
    public static final String DEFAULT_IP = "127.0.0.1";
    public static final String DEFAULT_PORT = "65000";

    private TextField nickTextField, addressTextFiled, portTextField;
    private Text feedback;

    private TcpClient tcpClient;
    private NickService nickService;

    public LoginPane(TcpClient tcpClient) {
        super();
        this.tcpClient = tcpClient;
        nickService = new NickService();
        initialize();
    }

    /**
     * Initializes the components in this pane.
     */
    private void initialize() {
        setAlignment(Pos.CENTER);
        setHgap(10);
        setVgap(10);
        setPadding(new Insets(25,25,25,25));

        Text sceneTitle = new Text("Connect to server:");
        sceneTitle.setFont(DEF_FONT);
        add(sceneTitle, 0, 0, 2, 1);

        Label userName = new Label("Nickname:");
        add(userName, 0, 1);
        nickTextField = new TextField(DEFAULT_USERNAME);
        add(nickTextField, 1, 1);

        Label address = new Label("IP address:");
        add(address, 0, 2);
        addressTextFiled = new TextField(DEFAULT_IP);
        add(addressTextFiled, 1, 2);

        Label port = new Label("Port:");
        add(port, 0, 3);
        portTextField = new TextField(DEFAULT_PORT);
        add(portTextField, 1, 3);

        feedback = new Text();
        feedback.setFont(Font.font("Tahoma", FontWeight.BOLD, 16));
        feedback.setFill(Color.FIREBRICK);
        add(feedback, 0, 6, 2, 1);

        Button btn = new Button("Connect");
        btn.setOnAction(e -> {onLoginClick();});
        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().add(btn);
        add(hbBtn, 1, 5);
    }

    /**
     * Returns address from the text field.
     * @return
     */
    public String getAddress() {
        return addressTextFiled.getText();
    }

    /**
     * Returns port from the text field.
     * @return
     */
    public String getPort() {
        return portTextField.getText();
    }

    /**
     * Returns nick from the text field.
     * @return
     */
    public String getNick() {
        return nickTextField.getText();
    }

    /**
     * Login button callback.
     */
    public void onLoginClick() {
        String addr = getAddress();
        if(!Constraits.checkAddress(addr)) {
            feedback.setText("Špatná adresa serveru.");
            logger.warn("Bas server address");
            return;
        }
        String p = getPort();
        if(!Constraits.checkPort(p)) {
            feedback.setText("Špatný port.");
            logger.warn("Bad port");
            return;
        }

        String nick = getNick();
        if(!Constraits.checkNickLength(nick)) {
            feedback.setText("Délka nicku musí být v rozmezí 3-8");
            logger.warn("Bad nick length");
            return;
        }
        switch (Constraits.checkNick(nick)) {
            case 0:
                        /*ok*/
                break;
            case 1:
                feedback.setText("První znak nicku musí být malé, nebo velké písmeno.");
                logger.warn("Bad nick length");
                return;
            case 2:
                feedback.setText("Nick může obsahovat pouze čísla a velká, nebo malá písmena.");
                logger.warn("Bad nick length");
                return;
        }

        Error res = tcpClient.connect(addr, Integer.parseInt(p));
        if (!res.ok()) {
            feedback.setText(res.msg);
            return;
        }

        tcpClient.sendNick(nick,

                // success callback
                e -> {
                    Object result = tcpClient.getNickService().getValue();
                    if(result == null) {
                        logger.error("Unknown error.");
                        feedback.setText("Neznámá chyba.");
                        tcpClient.disconnect();
                    }
                    if(result instanceof Error ) {
                        Error err = ((Error)result);
                        logger.debug("Error while loging into server: "+err.toString());
                        feedback.setText(err.toString());
                        tcpClient.disconnect();

                        //TODO switch err.code
                    } else if (result instanceof Message) {
                        Message m = (Message)result;
                        if(m.isOk()) {
                            logger.debug("Login ok.");
                            MainApp.switchToMain();
                        } else {
                            logger.debug("Unexpected message: "+m.toString());
                            feedback.setText(ErrorMessages.UNEXPECTED_RESPONSE);
                            tcpClient.disconnect();
                        }
                    }
                },

                // fail callback
                e -> {
                    Object val = tcpClient.getNickService().getValue();
                    String err = val == null ? "" : " ("+val.toString()+")";
                    logger.error("Error while sending nick to server: "+err);
                    feedback.setText("Chyba při odesílání nicku na server.");
                    tcpClient.disconnect();
                });
    }
}