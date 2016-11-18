package org.valesz.ups.main.ui;

import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import org.valesz.ups.main.MainApp;
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
    private Canvas canvas;

    public MainPane(TcpClient tcpClient) {
        super();
        this.tcpClient = tcpClient;
        initialize();
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

        Text p1 = new Text("Player 1: valesz");
        p1.setFont(DEF_SMALL_FONT);
        container.getChildren().add(p1);
        Text p2 = new Text("Player 2: haxxorz");
        p2.setFont(DEF_SMALL_FONT);
        container.getChildren().add(p2);

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

        canvas = new Canvas(CANVAS_WIDTH,CANVAS_HEIGHT);
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> canvasClick(event.getX(), event.getY()));
        GraphicsContext gc = canvas.getGraphicsContext2D();
        initCanvas(gc);
        container.getChildren().add(canvas);

        return container;
    }

    private void initCanvas(GraphicsContext gc) {
        gc.setFill(Color.BURLYWOOD);
        gc.fillRect(0,0,CANVAS_WIDTH,CANVAS_HEIGHT);

        // grid
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        for(int i = 0; i < 10 ; i++) {
            double x = CANVAS_WIDTH*i/10;
            gc.strokeLine(x,0,x,CANVAS_HEIGHT);
        }
        for(int i = 0; i < 3; i++) {
            double y = CANVAS_HEIGHT*i/3;
            gc.strokeLine(0,y,CANVAS_WIDTH,y);
        }

        // border
        gc.setLineWidth(8);
        gc.setStroke(Color.SADDLEBROWN);
        gc.strokeRect(0,0,CANVAS_WIDTH, CANVAS_HEIGHT);

        // bold lines to display the direction
        gc.setLineWidth(4);
        gc.strokeLine(0,CANVAS_HEIGHT/3,CANVAS_WIDTH*9/10,CANVAS_HEIGHT/3);
        gc.strokeLine(CANVAS_WIDTH/10,CANVAS_HEIGHT*2/3, CANVAS_WIDTH, CANVAS_HEIGHT*2/3);

    }

    /**
     * Converts pixel coordinates [0-CANVAS_WIDTH, 0-CANVAS_HEIGHT] to senet coordinates [1-10,1-3].
     * Note that the fields in the middle row are numbered from right.
     *
     * @param x
     * @param y
     * @return
     */
    private int[] getGamePosition(double x, double y) {
        int gy = (int)(Math.floor(3*y / CANVAS_HEIGHT)+1);
        int gx = (int)(Math.floor(10*x / CANVAS_WIDTH)+1);
        if(gy == 2) {
            gx = 11 - gx;
        }

        return new int[] {gx, gy};
    }

    /**
     * Converts the game x,y position to a single number from 1 to 30.
     * @param gamePos
     * @return
     */
    private int gamePosToFieldNumber(int[] gamePos) {
        return (gamePos[1] -1)*10 + gamePos[0];
    }

    /**
     * Adds a message which will be displayed in status pane.
     */
    public void addLogMessage(String message) {
        infoArea.appendText(message);
    }



    /*
    =============================================================
    CALLBACKS
    =============================================================
     */

    /**
     * Callback for click on [x,y].
     * @param x
     * @param y
     */
    private void canvasClick(double x, double y) {
        int[] gPos = getGamePosition(x, y);
        addLogMessage(String.format("Clicked on %d,%d - field %d.\n",gPos[0], gPos[1], gamePosToFieldNumber(gPos)));
    }

    /**
     * Send a message to the server that the turn has been ended.
     */
    private void endTurnClick() {

    }
}
