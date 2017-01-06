package org.valesz.ups.main.ui;

import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.valesz.ups.common.message.received.AbstractReceivedMessage;
import org.valesz.ups.common.message.received.ReceivedMessageTypeResolver;
import org.valesz.ups.common.message.received.StartGameReceivedMessage;
import org.valesz.ups.main.MainApp;
import org.valesz.ups.main.game.Game;
import org.valesz.ups.network.TcpClient;

/**
 * Main window.
 *
 * @author Zdenek Vales
 */
public class MainPane extends BorderPane {

    private static final Logger logger = LogManager.getLogger(MainPane.class);
    public static final Font DEF_FONT = Font.font("Tahoma", FontWeight.NORMAL, 20);
    public static final Font DEF_SMALL_FONT = Font.font("Tahoma", FontWeight.NORMAL, 12);
    public static final String GAME_TITLE = "Senet";
    public static final int CANVAS_WIDTH = 650;
    public static final int CANVAS_HEIGHT = 210;

    private TcpClient tcpClient;

    /*components*/
    private TextArea infoArea;
    private Board canvas;
    private Text p1Text, p2Text, turnText;

    public MainPane(TcpClient tcpClient) {
        super();
        this.tcpClient = tcpClient;
        initialize();
    }

    /**
     * Starts a task which will wait for a START_GAME message.
     */
    public void waitForStartGame() {
        tcpClient.getResponse(
                event -> {
                    // response
                    AbstractReceivedMessage response = tcpClient.getReceivingService().getValue();
                    StartGameReceivedMessage startGame = ReceivedMessageTypeResolver.isStartGame(response);
                    if (startGame == null) {
                        // wrong response
                        logger.error("Wrong message received. Expected START_GAME, received: "+response);
                    } else {
                        Game.getInstance().startGame(startGame.getFirstNickname(),startGame.getSecondNickname());
                        logger.info("The game has started");

                        //update graphical components
                        startGame();
                    }
                },
                event -> {
                    // failure
                    String error = tcpClient.getReceivingService().getException().getMessage();
                    logger.debug("Error while receiving response: "+error);
                }
        );
    }

    /**
     * Updates the components.
     */
    private void startGame() {
        p1Text.setText(Game.getInstance().getFirstPlayer().getNick());
        p2Text.setText(Game.getInstance().getSecondPlayer().getNick());
        turnText.setText(Game.getInstance().getCurrentPlayer());

        // place stones
        canvas.placeStones();
    }

    /**
     * Initializes the components in this pane.
     */
    private void initialize() {
        setLeft(getControlPane());
        setCenter(getMainPane());
        setBottom(getStatusPane());
    }

    /**
     * Exit button callback.
     */
    public void onExitClick() {
        MainApp.switchToMain();
    }

    /**
     * Returns pane with control components.
     * @return
     */
    private Pane getControlPane() {
        VBox container = new VBox();
        container.setPadding(new Insets(20));
        container.setSpacing(10);

        HBox buttons = new HBox();
        buttons.setSpacing(5);
        Button endBtn = new Button("Exit");
        endBtn.setOnAction(event -> {onExitClick();});
        buttons.getChildren().add(endBtn);
        Button turnBtn = new Button("End turn");
        endBtn.setOnAction(event -> {onExitClick();});
        buttons.getChildren().add(turnBtn);

        container.getChildren().add(getInfoPane());
        container.getChildren().add(buttons);

        return container;
    }

    /**
     * Returns a pane with info about game.
     * @return
     */
    private Pane getInfoPane() {
        VBox container = new VBox();
        container.setPadding(new Insets(10));

        Text gameTitle = new Text(GAME_TITLE);
        gameTitle.setFont(DEF_FONT);
        container.getChildren().add(gameTitle);

        HBox player1Caption = new HBox();
        player1Caption.getChildren().add(new Text("Hráč 1: "));
        p1Text = new Text("-");
        p1Text.setFont(DEF_SMALL_FONT);
        player1Caption.getChildren().add(p1Text);
        container.getChildren().add(player1Caption);

        HBox player2Caption = new HBox();
        player2Caption.getChildren().add(new Text("Hráč 2: "));
        p2Text = new Text("-");
        p2Text.setFont(DEF_SMALL_FONT);
        player2Caption.getChildren().add(p2Text);
        container.getChildren().add(player2Caption);

        HBox turnTextCaption = new HBox();
        turnTextCaption.getChildren().add(new Text("Táhne: "));
        turnText = new Text("-");
        turnText.setFont(DEF_SMALL_FONT);
        turnTextCaption.getChildren().add(turnText);
        container.getChildren().add(turnTextCaption);

        return container;
    }

    /**
     * Returns pane with status display.
     * @return
     */
    private Pane getStatusPane() {
        VBox container = new VBox();
        container.setPadding(new Insets(20));

        infoArea = new TextArea();
        infoArea.setEditable(false);
        infoArea.setWrapText(true);
        infoArea.setPrefRowCount(10);
        infoArea.setText("Have a nice game of Senet\n");
        infoArea.appendText("=========================\n\n");
        container.getChildren().add(infoArea);

        return container;
    }

    /**
     * Returns pane with board display.
     * @return
     */
    private Pane getMainPane() {
        Pane container = new VBox();
        container.setPadding(new Insets(20));

        canvas = new Board(this);
        canvas.init();
        container.getChildren().add(canvas);

        return container;
    }

    /**
     * Adds a message which will be displayed in status pane.
     */
    public void addLogMessage(String message) {
        infoArea.appendText(message);
    }

}
