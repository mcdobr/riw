package me.mircea.riw.dns;

import org.junit.Test;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;

public class DnsClientTest {

    @Test
    public void reachesTarget() throws UnknownHostException, SocketException {
        final String target = "www.tuiasi.ro";
        final InetAddress result = InetAddress.getByName("81.180.223.65");

        DnsClient client = new DnsClient("81.180.223.1");


        InetAddress ipAddress = client.getByName(target);

        assertEquals(result, ipAddress);
    }
}
