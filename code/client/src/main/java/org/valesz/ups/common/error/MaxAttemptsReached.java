package org.valesz.ups.common.error;

/**
 * @author Zdenek Vales
 */
public class MaxAttemptsReached extends ReceivingException{

    public MaxAttemptsReached() {
        super(Error.MAX_ATTEMPTS());
    }
}
