package org.valesz.ups.common.error;

/**
 * Codes for errors.
 *
 *
 * @author Zdenek Vales
 */
public enum ErrorCode {

    NO_ERROR(-1),
    GENERAL_ERROR(0),
    BAD_OPERATION(1),
    BAD_MSG_TYPE(2);


    public final int code;

    ErrorCode(int code) {
        this.code = code;
    }
}
