package org.valesz.ups.ui;

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
import org.valesz.ups.common.message.received.AbstractReceivedMessage;
import org.valesz.ups.common.message.received.ReceivedMessageTypeResolver;
import org.valesz.ups.controller.LoginController;
import org.valesz.ups.main.MainApp;
import org.valesz.ups.model.LoginData;
import org.valesz.ups.model.game.Game;
import org.valesz.ups.network.NickService;
import org.valesz.ups.network.TcpClient;

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

    private LoginController controller;

    public LoginPane(TcpClient tcpClient, LoginController loginController) {
        super();
        this.tcpClient = tcpClient;
        this.controller = loginController;
        this.controller.setView(this);
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
        int p = 0;
        try {
            p = Integer.parseInt(getPort());
        } catch (Exception e) {
            logger.error("Error while parsing port: "+e.getMessage());
            return;
        }
        String nick = getNick();
        LoginData loginData = new LoginData(nick, addr, p);
        controller.connect(loginData);

    }

    /**
     * Displays message in feedback.
     * @param message
     */
    public void displayMessage(String message) {
        feedback.setText(message);
    }
}
