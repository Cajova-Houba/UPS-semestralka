package org.valesz.ups.common.error;

/**
 * Error codes.
 *
 *
 * @author Zdenek Vales
 */
public enum ErrorCode {

    // TODO: update code numbers

    NO_ERROR(-1),
    GENERAL_ERROR(0),
    BAD_OPERATION(1),
    BAD_MSG_TYPE(2),
    BAD_MSG_CONTENT(3),
    BAD_NICKNAME(4),
    NICK_ALREADY_EXIST(5),
    SERVER_FULL(6),
    BAD_TURN(7),
    NO_CONNECTION(8);


    public final int code;

    ErrorCode(int code) {
        this.code = code;
    }

    /**
     * Returns adequate ErrorCode object for errCode.
     * If the errCode doesn't match any ErrorCode, NO_ERROR will be returned.
     *
     * @return
     */
    public static ErrorCode getCodeByInt(int errCode) {
        for (ErrorCode ec : values()) {
            if(ec.code == errCode) {
                return ec;
            }
        }

        return ErrorCode.NO_ERROR;
    }
}
