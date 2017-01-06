package org.valesz.ups.ui;

import org.junit.Ignore;
import org.junit.Test;
import org.valesz.ups.model.game.Game;

import static org.junit.Assert.*;

/**
 * @author Zdenek Vales
 */
public class BoardTest {

    @Test
    @Ignore
    /* old */
    public void testSelectNewStone() {
        startNewGame("first", "second");
        Board board = getInitializedBoard();

        assertNull("Selected should be null at the beginning of the game!",board.getSelected());

        //it's first player's turn, stones of other player shouldn't be selectable
        board.canvasClick(80,10);
        assertNull("No stone should be selected!", board.getSelected());

        //no stone is selected, select the first one
        board.canvasClick(10,10);
        assertNotNull("Nothing selected on 10,10!",board.getSelected());
        assertEquals("Stone on field 1 should have been selected!", 1, board.getSelected().getField());

        //reselect the stone on filed 3
        board.canvasClick(65*2+20,10);
        assertNotNull("Nothing selected on 140,10!",board.getSelected());
        assertEquals("Stone on field 3 should have been selected!", 3, board.getSelected().getField());

        //move it to field 11
        board.canvasClick(630, 100);
        assertNull("Nothing should be selected after moving stone!", board.getSelected());
        assertNotNull("There should be a stone on field 11!", board.getStoneOnFiled(11));

        //select stone on field 1 and the deselect it
        board.canvasClick(10,10);
        assertNotNull("Nothing selected on 10,10!",board.getSelected());
        assertEquals("Stone on field 1 should have been selected!", 1, board.getSelected().getField());
        board.canvasClick(10,10);
        assertNull("Stone on field 10 should be deselected!", board.getSelected());
    }

    @Test
    @Ignore
    /* old */
    public void testIsOpponentOnField() {
        startNewGame("first", "second");
        Board board = getInitializedBoard();

        assertTrue("Opponent's stone should be on field 2!", board.isOpponentOnField(2));
    }

    @Test
    public void testGetFieldNumberToCoordinates() {
        int[] fieldNumbers = new int[] {1,2,11,20,21,30};
        int[][] coordinates = new int[][] {
                new int[] {0,0},
                new int[] {65,0},
                new int[] {585,70},
                new int[] {0,70},
                new int[] {0,140},
                new int[] {585,140},
        };

        for (int i = 0; i < fieldNumbers.length; i++) {
            int[] coords = Board.fieldNumberToCoordinates(fieldNumbers[i]);

            assertEquals("Wrong x coordinate returned for "+fieldNumbers[i]+"!", coordinates[i][0], coords[0]);
            assertEquals("Wrong y coordinate returned for "+fieldNumbers[i]+"!", coordinates[i][1], coords[1]);
        }
    }

    /**
     * Starts a new game.
     * @param firstPlayer
     * @param secondPlayer
     */
    private void startNewGame(String firstPlayer, String secondPlayer) {
        Game.getInstance().waitingForOpponent(firstPlayer);
        Game.getInstance().startGame(firstPlayer, secondPlayer);
    }

    /**
     * Initializes new board, places stones and returns it.
     * @return
     */
    private Board getInitializedBoard() {
        Board board = new Board(null);
        board.placeStones(new int[5], new int[5]);
        board.init();

        return board;
    }
}
