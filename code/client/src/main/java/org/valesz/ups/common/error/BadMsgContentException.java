package org.valesz.ups.common.error;

/**
 * Thrown when the message is received, but its content is malformed.
 *
 * @author Zdenek Vales
 */
public class BadMsgContentException extends ReceivingException {

    public BadMsgContentException() {
        super(Error.BAD_MSG_CONTENT());
    }
}
