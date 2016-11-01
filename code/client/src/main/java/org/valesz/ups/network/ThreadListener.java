package org.valesz.ups.network;

import org.valesz.ups.common.error.Error;

/**
 * The thread listener will listen for results of network operations from TcpClient thread.
 *
 * @author Zdenek Vales
 */
public interface ThreadListener {

    /**
     * Notifies the listener about error which was returned by a server.
     * @param thread Thread.
     * @param error Type of error.
     */
    void notifyOnError(final Thread thread, Error error);

    /**
     * Notifies the listener about the success of the last operation with the server.
     * @param thread
     */
    void notifyOnOperationOk(final Thread thread);


}
