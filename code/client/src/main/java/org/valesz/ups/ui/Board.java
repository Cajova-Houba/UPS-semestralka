package org.valesz.ups.ui;


import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.valesz.ups.common.Constraits;
import org.valesz.ups.controller.GameController;
import org.valesz.ups.model.game.Game;

/**
 * Game board.
 *
 * @author Zdenek Vales
 */
public class Board extends Canvas {

    private static final Logger logger = LogManager.getLogger(Board.class);

    public static final int DEF_WIDTH = 650;
    public static final int DEF_HEIGHT = 210;
    public static final Color DEF_BG_COLOR = Color.BURLYWOOD;
    public static final Color DEF_BORDER_COLOR = Color.SADDLEBROWN;
    public static final int DEF_FIELD_WIDTH = DEF_WIDTH / 10;
    public static final int DEF_FIELD_HEIGHT = DEF_HEIGHT / 3;

    private GameController gameController;

    private final MainPane parent;

    private Stone[] firstPlayersStones;
    private Stone[] secondPlayersStones;

    /**
     * Currently selected stone.
     */
    private Stone selected;

    private boolean boardDirty;

    /**
     * Converts game position to the field number. Game position =/= coordinates on the board.
     * @param position
     * @return
     */
    public static int gamePosToFieldNumber(int[] position) {
        return (position[1] -1)*10 + position[0];
    }

    /**
     * Converts field number to XY coordinates.
     * The coordinates of upper left corner are returned.
     *
     * @param fieldNumber
     * @return
     */
    public static int[] fieldNumberToCoordinates(int fieldNumber) {
        int x = 0;
        int y = 0;

        if(fieldNumber < 1 || fieldNumber > 30) {
            return new int[] {x,y};
        } else if(fieldNumber < 11) {
            x = (fieldNumber - 1)*DEF_FIELD_WIDTH;
            y = 0;
        } else if(fieldNumber < 21) {
            x = 10 * DEF_FIELD_WIDTH - (fieldNumber - 10) * DEF_FIELD_WIDTH ;
            y = DEF_FIELD_HEIGHT;
        } else {
            x = (fieldNumber - 21)*DEF_FIELD_WIDTH;
            y = 2*DEF_FIELD_HEIGHT;
        }

        return new int[] {x,y};
    }


    public Board(MainPane parent) {
        super(DEF_WIDTH, DEF_HEIGHT);

        this.parent = parent;

        firstPlayersStones = new Stone[Constraits.MAX_NUMBER_OF_STONES];
        secondPlayersStones = new Stone[Constraits.MAX_NUMBER_OF_STONES];

        boardDirty = false;

        selected = null;

        this.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> canvasClick(event.getX(), event.getY()));
    }

    public void setGameController(GameController gameController) {
        this.gameController = gameController;
    }

    /**
     * Initializes empty board.
     */
    public void init() {
        GraphicsContext gc = this.getGraphicsContext2D();
        gc.setFill(DEF_BG_COLOR);
        gc.fillRect(0,0,DEF_WIDTH,DEF_HEIGHT);

        // grid
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        for(int i = 0; i < 10 ; i++) {
            double x = DEF_WIDTH*i/10;
            gc.strokeLine(x,0,x,DEF_HEIGHT);
        }
        for(int i = 0; i < 3; i++) {
            double y = DEF_HEIGHT*i/3;
            gc.strokeLine(0,y,DEF_WIDTH,y);
        }

        // border
        gc.setLineWidth(8);
        gc.setStroke(DEF_BORDER_COLOR);
        gc.strokeRect(0,0,DEF_WIDTH, DEF_HEIGHT);

        // bold lines to display the direction
        gc.setLineWidth(4);
        gc.strokeLine(0,DEF_HEIGHT/3,DEF_WIDTH*9/10,DEF_HEIGHT/3);
        gc.strokeLine(DEF_WIDTH/10,DEF_HEIGHT*2/3, DEF_WIDTH, DEF_HEIGHT*2/3);

        boardDirty = false;
    }

    /**
     * Places stones on the blank board.
     */
    public void placeStones(int[] firstPlayerStones, int[] secondPlayerStones) {
        if(boardDirty) {
            init();
        }

        GraphicsContext gc = this.getGraphicsContext2D();

        // first player
        for (int i = 0; i < Constraits.MAX_NUMBER_OF_STONES; i++) {
            firstPlayersStones[i] = new Stone(1, firstPlayerStones[i], Stone.DEF_FIRST_PLAYER_COLOR);
            firstPlayersStones[i].draw(gc);
        }

        // second player
        for (int i = 0; i < Constraits.MAX_NUMBER_OF_STONES; i++) {
            secondPlayersStones[i] = new Stone(2, secondPlayerStones[i], Stone.DEF_SECOND_PLAYER_COLOR);
            secondPlayersStones[i].draw(gc);
        }
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
        int gy = (int)(Math.floor(3*y / DEF_HEIGHT)+1);
        int gx = (int)(Math.floor(10*x / DEF_WIDTH)+1);
        if(gy == 2) {
            gx = 11 - gx;
        }

        return new int[] {gx, gy};
    }

    /**
     * Returns either stone on field or null.
     * @param fieldNumber
     * @return
     */
    public Stone getStoneOnFiled(int fieldNumber) {
        for (Stone stone : firstPlayersStones) {
            if (stone.getField() == fieldNumber) {
                return  stone;
            }
        }

        for (Stone stone : secondPlayersStones) {
            if (stone.getField() == fieldNumber) {
                return  stone;
            }
        }

        return  null;
    }

    /**
     * Returns either current player's stone on field or null.
     * @param fieldNumber
     * @return
     */
    public Stone selectStoneOnField(int fieldNumber) {
        int player = Game.getInstance().getCurrentPlayerNum();
        if (player == 1) {
            for (Stone stone : firstPlayersStones) {
                if (stone.getField() == fieldNumber) {
                    return  stone;
                }
            }
        }

        return  null;
    }

    /**
     * Callback for click on [x,y].
     * @param x
     * @param y
     */
    public void canvasClick(double x, double y) {
        if(!Game.getInstance().isRunning()) {
            logger.warn("Game's not running yet.");
            return;
        }

        if(!Game.getInstance().isMyTurn()) {
            logger.warn("Not my turn("+Game.getInstance().getMyPlayer()+". Current turn: "+Game.getInstance().getCurrentPlayerNum());
            return;
        }

        int[] gPos = getGamePosition(x, y);
        int fieldNumber = gamePosToFieldNumber(gPos);

        Stone selectedTmp = selectStoneOnField(fieldNumber);
        logger.trace(String.format("Clicked on %d,%d - field %d.\n",gPos[0], gPos[1], fieldNumber));

        if (this.selected == null && selectedTmp != null) {
            logger.trace(String.format("Selected: %s.", selectedTmp));
            this.selected = selectedTmp;
        } else if(this.selected != null && selectedTmp != null) {

            // if user click's on same stone, deselect it
            if(this.selected.getField() == selectedTmp.getField()) {
                logger.trace(String.format("Deselecting %s.", this.selected));
                this.selected = null;
            } else {
                logger.trace(String.format("Selected %s instead of %s.", selectedTmp, this.selected));
                this.selected = selectedTmp;
            }
        } else if (this.selected != null && selectedTmp == null) {

            logger.trace(String.format("Moving %s to %d.", this.selected, fieldNumber));

            gameController.move(this.selected.getField(), fieldNumber);
        }
    }

    /**
     * Updates stones on the board.
     * Called from controller.
     *
     * @param firstPlayerStones
     * @param secondPlayerStones
     */
    public void updateStones(int[] firstPlayerStones, int[] secondPlayerStones) {
        // there is still reference in first/secondPlayerStones field
        this.selected = null;

        boardDirty = true;
        placeStones(firstPlayerStones, secondPlayerStones);
    }

    /**
     * Checks, if the other player (the one which is currently waiting for his turn) has stone
     * on field.
     * @param fieldNumber
     * @return
     */
    public boolean isOpponentOnField(int fieldNumber) {
        int curPlayer = Game.getInstance().getCurrentPlayerNum();
        Stone stone = getStoneOnFiled(fieldNumber);
        if(stone == null) {
            logger.trace("No stone on field "+fieldNumber);
            return false;
        }

        return stone.getPlayer() != curPlayer;
    }

    public Stone[] getFirstPlayersStones() {
        return firstPlayersStones;
    }

    public Stone[] getSecondPlayersStones() {
        return secondPlayersStones;
    }

    public Stone getSelected() {
        return selected;
    }
}
