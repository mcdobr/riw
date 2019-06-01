package me.mircea.riw.dns;

import org.junit.Test;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DnsClientTest {
    @Test
    public void reachesTargetWithRecursiveLookup() throws UnknownHostException, SocketException {
        final String target = "www.google.com";
        //final String target = "riweb.tibeica.com";
        //final String target = "www.ace.tuiasi.ro";

        InetAddress addr = InetAddress.getByName(target);
        String actualHost = addr.getHostName();

        DnsClient client = new DnsClient("8.8.8.8");
        InetAddress ipAddresss = client.recursiveLookup(target);

        assertTrue(ipAddresss.getCanonicalHostName().contains("1e100.net"));
    }
}
