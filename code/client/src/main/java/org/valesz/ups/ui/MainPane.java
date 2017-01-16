package org.valesz.ups.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.valesz.ups.controller.GameController;
import org.valesz.ups.controller.LoginController;
import org.valesz.ups.controller.ViewController;
import org.valesz.ups.main.MainApp;
import org.valesz.ups.model.game.Game;
import org.valesz.ups.network.TcpClient;

import java.util.Optional;


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

    private GameController controller;
    private ViewController viewController;
    private LoginController loginController;

    /*components*/
    private TextArea infoArea;
    private Board canvas;
    private Text p1Text, p2Text, turnText, throwText;
    private Button exitButton, endTurnButton, throwButton, leaveButton;

    public MainPane(TcpClient tcpClient, GameController gameController, ViewController viewController, LoginController loginController) {
        super();
        this.tcpClient = tcpClient;
        this.controller = gameController;
        this.viewController = viewController;
        this.loginController = loginController;
        this.controller.setView(this);
        initialize();
    }

    /**
     * Starts a task which will wait for a START_GAME message.
     */
    public void waitForStartGame() {
        controller.waitForStartGame();
    }

    /**
     * Updates the components.
     */
    public void startGame() {
        p1Text.setText(Game.getInstance().getFirstPlayer().getNick());
        p2Text.setText(Game.getInstance().getSecondPlayer().getNick());
        turnText.setText(Game.getInstance().getCurrentPlayerNick());
    }

    /**
     * Initializes the components in this pane.
     */
    public void initialize() {
        setLeft(getControlPane());
        setCenter(getMainPane());
        setBottom(getStatusPane());
        canvas.setGameController(controller);
        this.controller.setBoardView(canvas);
    }

    /**
     * Exit button callback.
     */
    public void onExitClick() {
        MainApp.switchToMain();
    }

    /**
     * End turn button callback.
     */
    public void onEndTurnClick() {
        logger.debug("Ending turn.");
        controller.endTurn();
    }

    /**
     * Throw sticks callbacks.
     */
    public void onThrowClick() {
        int val = controller.throwSticks();
        if(val == -1) {
            throwText.setText("-");
        } else {
            throwText.setText(Integer.toString(val));
        }
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
        exitButton = new Button("Exit");
        exitButton.setOnAction(event -> {onExitClick();});
        buttons.getChildren().add(exitButton);
        endTurnButton = new Button("End turn");
        endTurnButton.setOnAction(event -> {onEndTurnClick();});
        buttons.getChildren().add(endTurnButton);

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

        VBox throwBox = new VBox();
        HBox throwTextCaption = new HBox();
        throwTextCaption.getChildren().add(new Text("Hozeno: "));
        throwText = new Text("-");
        throwText.setFont(DEF_SMALL_FONT);
        throwTextCaption.getChildren().add(throwText);
        throwBox.getChildren().add(throwTextCaption);
        throwButton = new Button("Hoď dřívky");
        throwButton.setOnAction(event -> onThrowClick());
        throwBox.getChildren().add(throwButton);
        container.getChildren().add(throwBox);

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
        ((VBox)container).setAlignment(Pos.TOP_RIGHT);

        canvas = new Board(this);
        canvas.init();
        container.getChildren().add(canvas);

        leaveButton = new Button("Opustit pole");
        leaveButton.setVisible(false);
        leaveButton.setOnAction(e -> leaveButtonClick());
        container.getChildren().add(leaveButton);

        return container;
    }

    /**
     * Prepares components on the mane pane (not on the board!) for the
     * new turn.
     */
    public void newTurn() {
        enableButtons();
        throwText.setText("-");
    }

    public void disableButtons() {
        endTurnButton.setDisable(true);
        throwButton.setDisable(true);
    }

    public void disableEndTurnButton() {
        endTurnButton.setDisable(true);
    }

    public void enableEndTurnButton() {
        endTurnButton.setDisable(false);
    }

    public void enableButtons() {
        endTurnButton.setDisable(false);
        throwButton.setDisable(false);
    }

    public void showLeaveButton() {
        leaveButton.setVisible(true);
    }

    public void hideLeaveButton() {
        leaveButton.setVisible(false);
    }

    private void leaveButtonClick() {
        controller.leaveBoard();
    }

    /**
     * Adds a message which will be displayed in status pane.
     */
    public void addLogMessage(String message) {
        infoArea.appendText(message);
    }

    /**
     * Displays the end game dialog.
     * @param winner
     */
    public void displayEndGameDialog(String winner) {
        EndGameAlert alert = new EndGameAlert(winner);
        Optional<ButtonType> res = alert.showAndWait();
        if (res.get() == EndGameAlert.EXIT_BUTTON) {
            logger.debug("Exiting...");
        } else if (res.get() == EndGameAlert.ANOTHER_SERVER_BUTTON) {
            // switch to login pane
            viewController.displayLoginPane();
        } else if (res.get() == EndGameAlert.NEW_GAME_BUTTON) {
            // reconnect on the same server
            loginController.reconnect();
        }
    }

}
