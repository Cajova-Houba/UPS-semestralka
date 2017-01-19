package org.valesz.ups.common.error;

/**
 * Constants with error messages.
 *
 * Some of those constants will be mapped to error codes.
 *
 * @author Zdenek Vales
 */
public class ErrorMessages {

    public static final String UNEXPECTED_RESPONSE = "Server vrátil nečekanou zprávu.";
    public static final String UNRECOGNIZED_ERROR = "Server vrátil neznámou chybu";
    public static final String WAITING_FOR_RESPONSE = "Chyba při čekání na odpověď serveru.";
    public static final String RECEIVING_RESPONSE = "Chyba příjimání odpovědi ze serveru.";
    public static final String BAD_NICKNAME = "Nickname má chybný tvar.";
    public static final String SERVER_FULL = "Server je plný.";
    public static final String NICK_ALREADY_EXISTS = "Hráč s tímto nickem již existuje.";

    public static String getErrorForCode(ErrorCode errorCode) {
        switch (errorCode) {
            case BAD_NICKNAME:
                return BAD_NICKNAME;
            case NICK_ALREADY_EXIST:
                return NICK_ALREADY_EXISTS;
            case SERVER_FULL:
                return SERVER_FULL;
            default:
                return UNRECOGNIZED_ERROR;
        }
    }

}
