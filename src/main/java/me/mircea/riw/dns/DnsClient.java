package me.mircea.riw.dns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;

public class DnsClient {
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



    public InetAddress getByName(String host) {
        InetAddress result = null;

        boolean successfulCommunication = false;
        while (!successfulCommunication) {

            try {
                sendRequest(host);
                DatagramPacket responsePacket = getResponsePacket();
                byte[] response = responsePacket.getData();
                checkReturnCode(response);


                int answerCount = getTwoBytes(response, HEADER_ANSWER_RECORD_COUNT_OFFSET);
                final int answersOffset = getAnswersOffset(response);

                int offset = answersOffset;
                int currentAnswer = 0;

                while (currentAnswer < answerCount) {
                    Stack<Integer> offsetStack = new Stack<>();
                    HashSet<Integer> visitedPointers = new HashSet<>();

                    offsetStack.push(offset);
                    visitedPointers.add(offset);

                    while (!offsetStack.isEmpty()) {
                        offset = offsetStack.peek();

                        // is pointer
                        if ((response[offset] & 0xFF) >= 0xC0) {
                            int ptr = ((response[offset] & 0x3F) << 8) | (response[offset + 1] & 0xFF);
                            if (!visitedPointers.contains(ptr)) {
                                offsetStack.push(ptr);
                                visitedPointers.add(ptr);
                            } else {
                                offsetStack.pop();
                            }
                        } else {
                            List<String> hostParts = new ArrayList<>();

                            while (response[offset] != 0x00) {
                                int len = response[offset];
                                hostParts.add(new String(response, offset + 1, len));
                                offset += len + 1;
                            }
                            String returnedHost = String.join(".", hostParts);

                            offsetStack.pop();
                        }
                    }

                    // increment offset on type
                    if ((response[offset] & 0xFF) >= 0xC0) {
                        offset += 2;
                    } else if (response[offset] == 0x00) {
                        ++offset;
                    }


                    int type = getTwoBytes(response, offset);
                    offset += 2;

                    int clazz = getTwoBytes(response, offset);
                    offset += 2;

                    int ttl = getFourBytes(response, offset);
                    offset += 4;

                    int rdl = getTwoBytes(response, offset);
                    offset += 2;

                    // parse resource data
                    result = InetAddress.getByAddress(Arrays.copyOfRange(response, offset, offset + rdl));

                    // set offset to next answer
                    offset += rdl;
                    ++currentAnswer;
                }


                successfulCommunication = true;
            } catch (SocketTimeoutException e) {
                LOGGER.warn("Could not receive datagram before timeout {}", e);
            } catch (IOException e) {
                LOGGER.warn("An I/O error occured {}", e);
            } catch (DnsException e) {
                LOGGER.warn("A DNS error occured {}", e);
            }
        }

        return result;
    }

    private void sendRequest(String host) throws IOException {
        byte[] request = new byte[DNS_HEADER_LENGTH + host.length() + 2 + DNS_QTYPE_LENGTH + DNS_QCLASS_LENGTH];

        int id = rng.nextInt(1 << 16);

        // Set id
        request[HEADER_ID_OFFSET] = (byte)((id >> 8) & 0xFF);
        request[HEADER_ID_OFFSET + 1] = (byte)((id & 0xFF));

        // set flag to query (0)
        request[HEADER_FLAGS_AND_CODES_OFFSET] &= ~(1 << QR_FLAG_OFFSET);
        // set opcode to 0
        request[HEADER_FLAGS_AND_CODES_OFFSET] &= ~(0x0F << OPCODE_OFFSET);
        // set rd to 0
        request[HEADER_FLAGS_AND_CODES_OFFSET] &= ~(1 << RD_FLAG_OFFSET);

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

    private void checkReturnCode(byte[] response) throws DnsException {
        int code = response[HEADER_FLAGS_AND_CODES_OFFSET + 1] & 0x0F;
        if (code != 0) {
            throw new DnsException(code);
        }
    }

    private int getAnswersOffset(byte[] response) {
        int questionCount = (response[HEADER_QUESTION_COUNT_OFFSET] & 0xFF) << 8 | (response[HEADER_QUESTION_COUNT_OFFSET + 1] & 0xFF);
        int offset = DNS_HEADER_LENGTH;
        int currentQuestion = 0;
        while (currentQuestion < questionCount) {
            while (response[offset] != 0x00) {
                offset += response[offset] + 1;
            }
            offset += DNS_QTYPE_LENGTH + DNS_QCLASS_LENGTH + 1;
            ++currentQuestion;
        }
        return offset;
    }

    private int getTwoBytes(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8)
                | (data[offset + 1] & 0xFF);
    }

    private int getFourBytes(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    private void setTwoBytes(byte[] data, int offset, int value) {

    }
}
