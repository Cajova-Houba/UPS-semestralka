package org.valesz.ups.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A simple class with static methods to perform validation.
 *
 * @author Zdenek Vales
 */
public class Constraits {

    public static final String IP_REGEXP = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
            "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    public static final int MIN_NICK_LENGTH = 3;
    public static final int MAX_NICK_LENGTH = 8;

    /**
     * Maximal number of stones per player.
     */
    public static final int MAX_NUMBER_OF_STONES = 5;


    /**
     * Checks if the address is valid ip address.
     * @param address Ip address to be checked.
     * @return True if the address is valid ip.
     */
    public static boolean checkAddress(String address) {
        if(address == null || address.isEmpty() || address.length() < 7 || address.length() > 15) {
            return false;
        }

        try {
            Pattern p = Pattern.compile(IP_REGEXP);
            Matcher matcher = p.matcher(address);
            return matcher.matches();
        } catch (PatternSyntaxException e ) {
            return false;
        }
    }

    /**
     * Checks if the port is number between 0 and 65535.
     * @param port String to be checked.
     * @return True if the string is a valid port number.
     */
    public static boolean checkPort(String port) {
        Integer p;
        try {
            p = Integer.parseInt(port);
        } catch (Exception e) {
            return false;
        }

        if (p == null) {
            return false;
        }

        return p >= 0 && p < 65536;
    }

    /**
     * Checks if the nick length is <= MAX_NICK_LENGTH and >= MIN_NICK_LENGTH.
     * @param nick Nick to be checked.
     * @return True if nick length is ok.
     */
    public static boolean checkNickLength(String nick) {
        if(nick == null) {
            return false;
        }
        int l = nick.length();

        return (l >= MIN_NICK_LENGTH && l <= MAX_NICK_LENGTH);
    }

    /**
     * Checks if the nick contains valid characters.
     * @param nick Nick to be checked. Nick is expected to be non-null.
     * @return 0 if nick is ok, 1 if the first char is invalid, 2 if any other char is invalid.
     */
    public static int checkNick(String nick) {
        if(nick == null || nick.length() < MIN_NICK_LENGTH) {
            return 2;
        }

        char first = nick.charAt(0);
        if (first < 65 ||
            first > 90 && first < 97 ||
            first > 122) {
            /* first char is invalid */
            return 1;
        }

        for(char ch : nick.toCharArray()) {
            if (ch < 48 ||
                ch > 57 && ch < 65 ||
                ch > 90 && ch < 97 ||
                ch > 122 ) {

                /* another char invalid */
                return 2;
            }
        }

        return 0;
    }
}
