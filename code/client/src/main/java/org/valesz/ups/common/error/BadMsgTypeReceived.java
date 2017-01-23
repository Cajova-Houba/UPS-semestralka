package org.valesz.ups.common.error;

/**
 * Exception thrown when an unrecognizable message type is received.
 *
 * @author Zdenek Vales
 */
public class BadMsgTypeReceived extends ReceivingException{

    public BadMsgTypeReceived() {
        super(Error.BAD_MSG_TYPE());
    }
}
