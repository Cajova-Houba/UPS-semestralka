package org.valesz.ups.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Player's stone.
 *
 * @author Zdenek Vales
 */
public class Stone {

    public static final int DEF_WIDTH = 50;
    public static final int DEF_HEIGHT = 50;
    public static final int DEF_X_MARING = (Board.DEF_FIELD_WIDTH - DEF_WIDTH)/2;
    public static final int DEF_Y_MARING = (Board.DEF_FIELD_HEIGHT - DEF_HEIGHT)/2;

    public static final Color DEF_FIRST_PLAYER_COLOR = Color.WHITE;
    public static final Color DEF_SECOND_PLAYER_COLOR = Color.BLACK;

    /**
     * 1 if this stone belongs to the first player.
     * 2 if this stone belongs to the second player.
     */
    private final int player;

    /**
     * Number of field for this stone.
     */
    private int field;

    /**
     * Color which will be used for this stone.
     */
    private Color color;


    public Stone(int player, int field, Color color) {
        this.player = player;
        this.field = field;
        this.color = color;
    }

    public int getField() {
        return field;
    }

    public void setField(int field) {
        this.field = field;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * Draws itself on graphical context.
     * @param gc
     */
    public void draw(GraphicsContext gc) {
        gc.setFill(color);
        int[] coords = getXY(field);
        gc.fillOval(coords[0], coords[1], DEF_WIDTH, DEF_HEIGHT);
    }

    /**
     * Converts the field number to coordinates of left upper corner.
     * Also adds margin.
     * @param fieldNumber
     * @return
     */
    public int[] getXY(int fieldNumber) {

        int[] coords = Board.fieldNumberToCoordinates(fieldNumber);

        return new int[] {coords[0] + DEF_X_MARING, coords[1] + DEF_Y_MARING};
    }

    public int getPlayer() {
        return this.player;
    }

    @Override
    public String toString() {
        return "Stone{" +
                "player=" + player +
                ", field=" + field +
                ", color=" + color +
                '}';
    }
}
