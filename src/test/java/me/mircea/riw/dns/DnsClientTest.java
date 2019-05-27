package me.mircea.riw.dns;

import org.junit.Test;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DnsClientTest {

    @Test
    public void reachesTargetWithSimpleLookup() throws UnknownHostException, SocketException {
        //final String target = "www.tuiasi.ro";
        final String target = "riweb.tibeica.com";
        final InetAddress result = InetAddress.getByName("81.180.223.65");

        //DnsClient client = new DnsClient("81.180.223.1");
        DnsClient client = new DnsClient("8.8.8.8");


        InetAddress ipAddress = client.iterativeLookup(target);

        assertEquals(result, ipAddress);
    }

    @Test
    public void reachesTargetWithRecursiveLookup() throws UnknownHostException, SocketException {
        //final String target = "www.google.com";
        final String target = "riweb.tibeica.com";
        InetAddress addr = InetAddress.getByName(target);
        String actualHost = addr.getHostName();

        DnsClient client = new DnsClient("8.8.8.8");
        InetAddress ipAddresss = client.recursiveLookup(target);

        assertTrue(ipAddresss.getCanonicalHostName().contains("1e100.net"));
    }
}
