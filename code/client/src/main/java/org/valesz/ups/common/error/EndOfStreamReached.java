package org.valesz.ups.common.error;

/**
 * @author Zdenek Vales
 */
public class EndOfStreamReached extends ReceivingException{

    public EndOfStreamReached() {
        super(Error.NO_CONNECTION());
    }
}
