package org.valesz.ups.common.error;

/**
 * Possible errors which can occur in application.
 *
 * @author Zdenek Vales
 */
public class Error {

    public static Error NO_ERROR() {
        return new Error(ErrorCode.NO_ERROR,"");
    }

    public static Error GENERAL_ERROR(String msg) {
        return new Error(ErrorCode.GENERAL_ERROR, msg);
    }

    public static Error BAD_OPERATION() {
        return new Error(ErrorCode.GENERAL_ERROR,"");
    }

    public static Error BAD_MSG_TYPE() {
        return new Error(ErrorCode.GENERAL_ERROR,"");
    }

    public static Error BAD_MSG_CONTENT() {
        return new Error(ErrorCode.GENERAL_ERROR,"");
    }

    public static Error BAD_NICK() {
        return new Error(ErrorCode.GENERAL_ERROR,"");
    }

    public static Error NICK_EXISTS() {
        return new Error(ErrorCode.GENERAL_ERROR,"");
    }

    public static Error SERVER_FULL() {
        return new Error(ErrorCode.GENERAL_ERROR,"");
    }

    public static Error BAD_TURN() {
        return new Error(ErrorCode.GENERAL_ERROR,"");
    }

    /**
     * Returns true if the code is equal to ErrorCode.NO_ERROR.
     * @return
     */
    public boolean ok() {
        return code == ErrorCode.NO_ERROR;
    }

    public final ErrorCode code;
    public final String msg;

    public Error(String msg) {
        this(ErrorCode.GENERAL_ERROR, msg);
    }

    private Error(ErrorCode code, String msg) {
        this.code = code;
        this.msg = msg;
    }



}
