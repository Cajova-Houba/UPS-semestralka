package org.valesz.ups.network;

import org.junit.Test;
import org.valesz.ups.common.error.Error;

import static org.junit.Assert.assertTrue;

/**
 * @author Zdenek Vales
 */
public class TcpClientTest {

    private String addresss = "127.0.0.1";
    private int port = 65000;
    private String nick = "valesz";

    @Test
    public void testConnection() {
        TcpClient tcpClient = new TcpClient();

        Error res = tcpClient.connect(addresss, port);
        assertTrue("Error occured! "+res.msg, res.ok());

//        res = tcpClient.sendNick(nick, event -> {});
//        assertTrue("Error occured! "+res.msg, res.ok());
    }
}
