package org.valesz.ups.common.message.received;

import org.valesz.ups.common.error.Error;
import org.valesz.ups.common.error.ErrorCode;
import org.valesz.ups.common.message.MessageType;

/**
 * @author Zdenek Vales
 */
public class ErrorReceivedMessage extends AbstractReceivedMessage<Error> {

    private Error content;

    public ErrorReceivedMessage(Error content) {
        this.content = content;
    }

    public ErrorReceivedMessage(ErrorCode errorCode) {
        this.content = new Error(errorCode,"");
    }

    @Override
    public Error getContent() {
        return content;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.ERR;
    }
}
