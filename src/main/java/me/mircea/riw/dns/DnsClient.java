package me.mircea.riw.dns;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;

public class DnsClient {
    private static final DnsCache DNS_CACHE;

    static {
        DNS_CACHE = new DnsCache();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DnsClient.class);

    private static final int DNS_PORT = 53;
    private static final int DNS_HEADER_LENGTH = 12;

    private static final int DNS_MAX_MESSAGE_LENGTH = 512;
    private static final int DNS_QTYPE_LENGTH = 2;
    private static final int DNS_QCLASS_LENGTH = 2;

    private static final int UDP_RECV_TIMEOUT_MILLIS = 2000;

    // Header offsets
    private static final int HEADER_ID_OFFSET = 0;
    private static final int HEADER_FLAGS_AND_CODES_OFFSET = 2;
    private static final int HEADER_QUESTION_COUNT_OFFSET = 4;
    private static final int HEADER_ANSWER_RECORD_COUNT_OFFSET = 6;
    private static final int HEADER_NAME_SERVER_COUNT_OFFSET = 8;
    private static final int HEADER_ADDITIONAL_RECORD_COUNT_OFFSET = 10;

    // Flags and codes offsets
    private static final int QR_FLAG_OFFSET = 7;
    private static final int OPCODE_OFFSET = 3;
    private static final int AA_FLAG_OFFSET = 2;
    private static final int TC_FLAG_OFFSET = 1;
    private static final int RD_FLAG_OFFSET = 0;

    private static final int RA_FLAG_OFFSET = 7;
    private static final int ZERO_CODE_OFFSET = 3;
    private static final int RESP_CODE = 0;

    private static final String HOST_PARTS_SPLITTER_REGEX = "\\.";


    private final InetAddress dnsServerAddress;
    private final DatagramSocket socket;
    private final Random rng;


    public DnsClient(String dnsServerAddress) throws UnknownHostException, SocketException {
        String[] ipBytesStrings =  dnsServerAddress.split(HOST_PARTS_SPLITTER_REGEX);
        byte[] ipBytes = new byte[ipBytesStrings.length];

        for (int i = 0; i < ipBytes.length; ++i) {
            ipBytes[i] = (byte)Integer.parseInt(ipBytesStrings[i]);
        }

        this.dnsServerAddress = InetAddress.getByAddress(ipBytes);
        this.socket = new DatagramSocket();
        this.socket.setSoTimeout(UDP_RECV_TIMEOUT_MILLIS);
        this.rng = new Random();
    }

    /**
     * Returns an ip address for a host (will check cache first).
     * @param host
     * @return
     */
    public InetAddress recursiveLookup(String host) {
        InetAddress ipAddress = null;

        DnsRecord cachedAddressRecord = DNS_CACHE.getAddressRecordFor(host);
        if (cachedAddressRecord != null) {
            try {
                ipAddress = InetAddress.getByAddress(cachedAddressRecord.getData());
            } catch (UnknownHostException e) {
                LOGGER.trace("Unknown host on cached entry {}", e);
            }
        } else {
            try {
                sendRequest(host, true);
                DatagramPacket responsePacket = getResponsePacket();
                ipAddress = parseSimpleLookupResponse(host, responsePacket);
            } catch (SocketTimeoutException e) {
                LOGGER.warn("Could not receive datagram before timeout {}", e);
            } catch (IOException e) {
                LOGGER.warn("An I/O error occured on DNS lookup {}", e);
            }
        }
        return ipAddress;
    }

    /**
     * Creates a dns message with only a simple question.
     * @deprecated
     * @param host
     * @param recursive
     * @throws IOException
     */
    private void sendRequest(String host, boolean recursive) throws IOException {
        byte[] request = new byte[DNS_HEADER_LENGTH + host.length() + 2 + DNS_QTYPE_LENGTH + DNS_QCLASS_LENGTH];

        int id = rng.nextInt(1 << 16);

        // Set id
        request[HEADER_ID_OFFSET] = (byte)((id >> 8) & 0xFF);
        request[HEADER_ID_OFFSET + 1] = (byte)((id & 0xFF));

        // set flag to query (0)
        request[HEADER_FLAGS_AND_CODES_OFFSET] &= ~(1 << QR_FLAG_OFFSET);
        // set opcode to 0
        request[HEADER_FLAGS_AND_CODES_OFFSET] &= ~(0x0F << OPCODE_OFFSET);

        if (recursive) {
            request[HEADER_FLAGS_AND_CODES_OFFSET] |= (1 << RD_FLAG_OFFSET);
        } else {
            // set rd to 0
            request[HEADER_FLAGS_AND_CODES_OFFSET] &= ~(1 << RD_FLAG_OFFSET);
        }

        // Set question count to 1
        request[HEADER_QUESTION_COUNT_OFFSET] = 0x00;
        request[HEADER_QUESTION_COUNT_OFFSET + 1] = 0x01;

        // Add question
        int dnsMessageOffset = DNS_HEADER_LENGTH;
        String[] hostParts = host.split(HOST_PARTS_SPLITTER_REGEX);
        for (int i = 0; i < hostParts.length; ++i) {
            request[dnsMessageOffset++] = (byte)(hostParts[i].length() & 0xFF);

            byte[] hostPartBytes = hostParts[i].getBytes(Charset.forName("US-ASCII"));
            System.arraycopy(hostPartBytes, 0, request, dnsMessageOffset, hostPartBytes.length);
            dnsMessageOffset += hostPartBytes.length;
        }
        request[dnsMessageOffset++] = 0x00;

        // Set question type and class
        request[dnsMessageOffset++] = 0x00;
        request[dnsMessageOffset++] = 0x01;

        request[dnsMessageOffset++] = 0x00;
        request[dnsMessageOffset++] = 0x01;

        DatagramPacket requestPacket = new DatagramPacket(request, request.length, this.dnsServerAddress, DNS_PORT);
        socket.send(requestPacket);
    }

    private DatagramPacket getResponsePacket() throws IOException {
        byte[] response = new byte[DNS_MAX_MESSAGE_LENGTH];
        DatagramPacket responsePacket = new DatagramPacket(response, response.length);

        socket.receive(responsePacket);
        return responsePacket;
    }


    /**
     * Creates a DNS message from a datagram and caches answer results.
     * @param host
     * @param responsePacket
     * @return
     * @throws UnknownHostException
     */
    private InetAddress parseSimpleLookupResponse(String host, DatagramPacket responsePacket) throws UnknownHostException {
        byte[] responseBytes = responsePacket.getData();
        DnsMessage msg = DnsMessage.newBuilder().readFrom(responseBytes).build();
        msg.getAnswerRecords().forEach(DNS_CACHE::add);

        DnsRecord addressRecord = getAddressRecord(host, msg.getAnswerRecords());
        if (addressRecord != null) {
            return getAddressFromDnsRecord(addressRecord);
        } else {
            return null;
        }
    }

    private DnsRecord getAddressRecord(String host, Collection<DnsRecord> records) throws UnknownHostException {
        return records.stream().filter(dnsRecord -> dnsRecord.getType() == DnsRecordType.A && dnsRecord.getName().equals(host)).findFirst().orElse(null);
    }

    private InetAddress getAddressFromDnsRecord(DnsRecord record) throws UnknownHostException {
        Preconditions.checkArgument(record.getType() == DnsRecordType.A);
        return InetAddress.getByAddress(record.getData());
    }

}
