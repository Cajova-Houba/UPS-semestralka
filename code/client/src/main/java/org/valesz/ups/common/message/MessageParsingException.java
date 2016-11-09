package org.valesz.ups.common.message;

/**
 * Exception thrown if the message parsing fails.
 *
 * @author Zdenek Vales
 */
public class MessageParsingException extends Exception {

    /**
     * Error which is not mentioned here.
     */
    public static final int OTHER_ERROR = 0;

    /**
     * Message type was not recognized.
     */
    public static final int BAD_MESSAGE_TYPE = 1;

    /**
     * Unexpected or null content.
     */
    public static final int BAD_MESSAGE_CONTENT = 2;

    private int exCode;

    public MessageParsingException(int exCode) {
        this.exCode = exCode;
    }

    public MessageParsingException(String message, int exCode) {
        super(message);
        this.exCode = exCode;
    }

    public int getExCode() {
        return exCode;
    }

    @Override
    public String toString() {
        return "MessageParsingException{" +
                "cause=" + exCode +
                ", description='" + getMessage() + '\'' +
                '}';
    }
}
