package org.valesz.ups.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Zdenek Vales
 */
public class UtilsTest {

    /**
     * Test intToByte and byteToInt methods.
     */
    @Test
    public void testIntByteConversion() {
        int[] values = new int[] {
            0,1,255,256,65280,65281,16711680,16711681,2147483647
        };

        for(int value : values) {
            byte[] btmp = Utils.intToByte(value);
            System.out.println(String.format("%d converted to [%x,%x,%x,%x].",value, btmp[0], btmp[1], btmp[2], btmp[3]));
            int itmp = Utils.byteToInt(btmp);
            System.out.println(String.format("[%x,%x,%x,%x] converted to  %d.",btmp[0], btmp[1], btmp[2], btmp[3], itmp));

            assertEquals(value+" was converted to "+itmp+"!",value, itmp);
        }
    }
}
