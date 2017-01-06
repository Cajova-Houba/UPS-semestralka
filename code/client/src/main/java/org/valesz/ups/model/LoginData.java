package org.valesz.ups.model;

/**
 * A simple class for login data - nick, address and port.
 *
 * @author Zdenek Vales
 */
public class LoginData {

    private final String nick;
    private final String address;
    private final int port;

    public LoginData(String nick, String ip, int port) {
        this.nick = nick;
        this.address = ip;
        this.port = port;
    }

    public String getNick() {
        return nick;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }
}
