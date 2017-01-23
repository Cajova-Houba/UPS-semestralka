package org.valesz.ups.common.error;

/**
 * Error codes.
 *
 *
 * @author Zdenek Vales
 */
public enum ErrorCode {

    NO_ERROR(-1),
    UNRECOGNIZED_ERROR(51),
    GENERAL_ERROR(50),
    BAD_OPERATION(49),
    BAD_MSG_TYPE(48),
    BAD_MSG_CONTENT(47),
    BAD_NICKNAME(46),
    NICK_ALREADY_EXIST(45),
    NICK_LENGTH(44),
    SERVER_FULL(43),
    NOT_MY_TURN(42),
    GAME_ALREADY_RUNNING(41),
    BAD_TURN(40),
    TIMEOUT(39),
    MAX_ATTEMPTS(38),
    UNEXPECTED_MESSAGE(37),
    NO_CONNECTION(8);


    public final int code;

    ErrorCode(int code) {
        this.code = code;
    }

    /**
     * Returns adequate ErrorCode object for errCode.
     * If the errCode doesn't match any ErrorCode, UNRECOGNIZED_ERROR will be returned.
     *
     * @return
     */
    public static ErrorCode getCodeByInt(int errCode) {
        for (ErrorCode ec : values()) {
            if(ec.code == errCode) {
                return ec;
            }
        }

        return ErrorCode.UNRECOGNIZED_ERROR;
    }

    /**
     * Converts the byte array (represented as char digits) to int and returns error code.
     * @param buffer
     * @return
     */
    public static ErrorCode getCodeByInt(byte buffer[]) {
        String errCode = new String(buffer);
        try {
            int ec = Integer.parseInt(errCode);
            return getCodeByInt(ec);
        } catch (NumberFormatException ex) {
            return ErrorCode.UNRECOGNIZED_ERROR;
        }
    }
}
