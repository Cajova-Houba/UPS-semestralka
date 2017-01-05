package org.valesz.ups.main.ui;


import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.valesz.ups.common.Constraits;
import org.valesz.ups.main.game.Game;

/**
 * Game board.
 *
 * @author Zdenek Vales
 */
public class Board extends Canvas {

    public static final int DEF_WIDTH = 650;
    public static final int DEF_HEIGHT = 210;
    public static final Color DEF_BG_COLOR = Color.BURLYWOOD;
    public static final Color DEF_BORDER_COLOR = Color.SADDLEBROWN;

    private Stone[] firstPlayersStones;
    private Stone[] secondPlayersStones;

    private boolean boardDirty;

    public Board() {
        super(DEF_WIDTH, DEF_HEIGHT);

        firstPlayersStones = new Stone[Constraits.MAX_NUMBER_OF_STONES];
        secondPlayersStones = new Stone[Constraits.MAX_NUMBER_OF_STONES];

        boardDirty = false;
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
    public void placeStones() {
        if(boardDirty) {
            init();
        }

        GraphicsContext gc = this.getGraphicsContext2D();

        // first player
        int[] pStones = Game.getInstance().getFirstPlayer().getStones();
        for (int i = 0; i < Constraits.MAX_NUMBER_OF_STONES; i++) {
            firstPlayersStones[i] = new Stone(pStones[i], Stone.DEF_FIRST_PLAYER_COLOR);
            firstPlayersStones[i].draw(gc);
        }

        // second player
        pStones = Game.getInstance().getSecondPlayer().getStones();
        for (int i = 0; i < Constraits.MAX_NUMBER_OF_STONES; i++) {
            secondPlayersStones[i] = new Stone(pStones[i], Stone.DEF_SECOND_PLAYER_COLOR);
            secondPlayersStones[i].draw(gc);
        }
    }

}
