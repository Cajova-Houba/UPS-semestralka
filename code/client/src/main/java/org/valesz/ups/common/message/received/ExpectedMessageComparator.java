package org.valesz.ups.common.message.received;

/**
 * A simple comparator for received messages.
 * @author Zdenek Vales
 */
public interface ExpectedMessageComparator {

    /**
     * Returns true if the message is the expected one.
     * @param message Received message. May be null.
     * @return
     */
    public boolean isExpected(AbstractReceivedMessage message);
}
