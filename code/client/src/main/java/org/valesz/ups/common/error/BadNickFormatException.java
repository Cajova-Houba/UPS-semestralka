package org.valesz.ups.common.error;

/**
 * Thrown when a message should contain nick, but the nick is malformed.
 *
 * @author Zdenek Vales
 */
public class BadNickFormatException extends ReceivingException {

    public BadNickFormatException() {
        super(Error.BAD_NICK());
    }
}
