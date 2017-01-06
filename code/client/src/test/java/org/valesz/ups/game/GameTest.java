package org.valesz.ups.game;

import org.junit.Test;
import org.valesz.ups.model.game.Game;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Zdenek Vales
 */
public class GameTest {

    @Test
    public void testMoveLengthOk() {

        // move not possible
        assertFalse("Move shouldn't be possible!", Game.getInstance().isMoveLengthOk(1,3));

        Game.getInstance().setThrownValue(1);
        // move forward
        assertTrue("Move by one forward should be possible!", Game.getInstance().isMoveLengthOk(1,2));

        // move backwards
        assertTrue("Move by one backward should be possible!", Game.getInstance().isMoveLengthOk(2,1));
    }



}
