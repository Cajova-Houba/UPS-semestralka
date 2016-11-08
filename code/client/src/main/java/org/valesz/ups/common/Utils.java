package org.valesz.ups.common;

/**
 * Utilities.
 *
 * @author Zdenek Vales
 */
public class Utils {

    /**
     * Converts a positive integer to the array of 4 bytes.
     * Example:
     *  i = 14
     *  return = [14, 0, 0, 0] (base 10)
     *
     * @param i Integer to be converted.
     * @return Converted integer.
     */
    public static byte[] intToByte(int i) {
        byte res[] = new byte[4];

        res[0] = (byte)(i >> 24);
        res[1] = (byte)((i << 8) >> 24);
        res[2] = (byte)((i << 16) >> 24);
        res[3] = (byte)((i << 24) >> 24);

        return res;
    }

    /**
     * Converts an array of 4 (!!) bytes to positive integer.
     * Example:
     *  b = [14, 0, 0, 0]
     *  return = 14
     *
     *  b = [14, 14, 0, 0]
     *  return = 14 + 14 *256
     *
     * @param b Array of bytes. If the length is not 4, 0 will be returned.
     * @return
     */
    public static int byteToInt(byte[] b) {
        int res = 0;
        if (b.length != 4) {
            return res;
        }

        res = b[0] << 24;
        res = res + ((int)(b[1]) << 16);
        res = res + ((int)(b[2]) << 8);
        res = res + (int)(b[3]);

        return res;
    }
}
