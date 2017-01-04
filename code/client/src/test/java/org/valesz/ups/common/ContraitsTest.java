package org.valesz.ups.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Zdenek Vales
 */
public class ContraitsTest {

    @Test
    public void testNickValid() {
        String noNick = "";
        String wrongFirstChar = "0asas";
        String wrongChars = "A351!!";
        String tooLong = "Asdasdasdadasdasd";
        String tooShort = "Ac";
        String correctNick = "Valesz11";

        assertEquals("No nick!", 2, Constraits.checkNick(noNick));
        assertEquals("Wrong first char!", 1, Constraits.checkNick(wrongFirstChar));
        assertEquals("Wrong other chars!", 2, Constraits.checkNick(wrongChars));
        assertEquals("Too long!", 2, Constraits.checkNick(tooLong));
        assertEquals("Too short!", 2, Constraits.checkNick(tooShort));
        assertEquals("Nick ok!", 0, Constraits.checkNick(correctNick));
    }
}
