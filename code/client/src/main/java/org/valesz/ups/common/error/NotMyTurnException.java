package org.valesz.ups.common.error;

/**
 * Exception thrown if the player tries to end turn when he's not the one playing.
 *
 * @author Zdenek Vales
 */
public class NotMyTurnException extends Exception {

    public NotMyTurnException() {
    }

    public NotMyTurnException(String message) {
        super(message);
    }
}
