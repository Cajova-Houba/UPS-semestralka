package org.valesz.ups.common.error;

/**
 * Exception thrown while receiving a message.
 *
 * @author Zdenek Vales
 */
public class ReceivingException extends Exception {

    public final Error error;

    public ReceivingException(Error error) {
        this.error = error;
    }
}
