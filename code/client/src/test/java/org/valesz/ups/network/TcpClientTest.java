package org.valesz.ups.network;

import javafx.application.Application;
import javafx.embed.swing.JFXPanel;
import org.junit.Test;
import org.valesz.ups.common.error.Error;
import org.valesz.ups.common.message.received.AbstractReceivedMessage;
import org.valesz.ups.common.message.received.ReceivedMessageTypeResolver;
import org.valesz.ups.main.MainApp;

import java.net.SocketTimeoutException;

import static org.junit.Assert.*;

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

//        Error res = tcpClient.connect(addresss, port);
//        assertTrue("Error occured! "+res.msg, res.ok());

//        res = tcpClient.sendNick(nick, event -> {});
//        assertTrue("Error occured! "+res.msg, res.ok());
    }

}
