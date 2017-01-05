package org.valesz.ups.main.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Player's stone.
 *
 * @author Zdenek Vales
 */
public class Stone {

    public static final int DEF_FIELD_WIDTH = Board.DEF_WIDTH / 10;
    public static final int DEF_FIELD_HEIGHT = Board.DEF_HEIGHT / 3;

    public static final int DEF_WIDTH = 50;
    public static final int DEF_HEIGHT = 50;

    public static final Color DEF_FIRST_PLAYER_COLOR = Color.WHITE;
    public static final Color DEF_SECOND_PLAYER_COLOR = Color.BLACK;

    /**
     * Number of field for this stone.
     */
    private int field;

    /**
     * Color which will be used for this stone.
     */
    private Color color;

    public Stone(int field, Color color) {
        this.field = field;
        this.color = color;
    }

    /**
     * Draws itself on graphical context.
     * @param gc
     */
    public void draw(GraphicsContext gc) {
        gc.setFill(color);
        int[] coord = getXY(field);
        gc.fillOval(coord[0], coord[1], DEF_WIDTH, DEF_HEIGHT);
    }

    /**
     * Converts the field number to coordinates of left upper corner.
     * @param field
     * @return
     */
    public int[] getXY(int field) {

        int x = 0;
        int y = 0;

        int xMargin = (DEF_FIELD_WIDTH - DEF_WIDTH)/2;
        int yMargin = (DEF_FIELD_HEIGHT - DEF_HEIGHT)/2;

        if(field < 1 || field > 30) {
            return new int[] {x,y};
        } else if(field < 11) {
            x = (field - 1)*DEF_FIELD_WIDTH + xMargin;
            y = yMargin;
        } else if(field < 21) {
            x = 10 * DEF_FIELD_WIDTH - (field - 11) * DEF_FIELD_WIDTH + xMargin;
            y = DEF_FIELD_HEIGHT + yMargin;
        } else {
            x = (field - 1)*DEF_FIELD_WIDTH + xMargin;
            y = 2*DEF_FIELD_HEIGHT + yMargin;
        }

        return new int[] {x,y};
    }
}
