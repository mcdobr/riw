package me.mircea.riw.dns;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

public class DnsMessage {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            DnsClient.class);

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

    public static Builder newBuilder() {
        return new Builder();
    }

    private final int id;
    private final int flagsAndCodes;

    private final List<DnsQuestion> questions;
    private final List<DnsRecord> answerRecords;
    private final List<DnsRecord> authorityRecords;
    private final List<DnsRecord> additionalRecords;

    private DnsMessage(int id, int flagsAndCodes,
                      List<DnsQuestion> questions, List<DnsRecord> answerRecords, List<DnsRecord> authorityRecords, List<DnsRecord> additionalRecords) {
        this.id = id;
        this.flagsAndCodes = flagsAndCodes;

        this.questions = ImmutableList.copyOf(questions);
        this.answerRecords = ImmutableList.copyOf(answerRecords);
        this.authorityRecords = ImmutableList.copyOf(authorityRecords);
        this.additionalRecords = ImmutableList.copyOf(additionalRecords);
    }


    public int getId() {
        return id;
    }

    public int getFlagsAndCodes() {
        return flagsAndCodes;
    }

    public List<DnsQuestion> getQuestions() {
        return questions;
    }

    public List<DnsRecord> getAnswerRecords() {
        return answerRecords;
    }

    public List<DnsRecord> getAuthorityRecords() {
        return authorityRecords;
    }

    public List<DnsRecord> getAdditionalRecords() {
        return additionalRecords;
    }

    public int getReturnCode(){
        //TODO: in flags and codes pe undeva
        return flagsAndCodes;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DnsMessage{");
        sb.append("id=").append(id);
        sb.append(", flagsAndCodes=").append(flagsAndCodes);
        sb.append(", questions=").append(questions);
        sb.append(", answerRecords=").append(answerRecords);
        sb.append(", authorityRecords=").append(authorityRecords);
        sb.append(", additionalRecords=").append(additionalRecords);
        sb.append('}');
        return sb.toString();
    }

    public static class Builder {
        private int id;
        private int flagsAndCodes;

        private List<DnsQuestion> questions;
        private List<DnsRecord> answerRecords;
        private List<DnsRecord> authorityRecords;
        private List<DnsRecord> additionalRecords;

        public Builder() {
            this.questions = new ArrayList<>();
            this.answerRecords = new ArrayList<>();
            this.authorityRecords = new ArrayList<>();
            this.additionalRecords = new ArrayList<>();
        }

        public DnsMessage build() {
            return new DnsMessage(id, flagsAndCodes, questions, answerRecords, authorityRecords, additionalRecords);
        }

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder flagsAndCodes(int flagsAndCodes) {
            this.flagsAndCodes = flagsAndCodes;
            return this;
        }

        public Builder questions(List<DnsQuestion> questions) {
            this.questions = questions;
            return this;
        }

        public Builder answerRecords(List<DnsRecord> answerRecords) {
            this.answerRecords = answerRecords;
            return this;
        }

        public Builder authorityRecords(List<DnsRecord> authorityRecords) {
            this.authorityRecords = authorityRecords;
            return this;
        }

        public Builder additionalRecords(List<DnsRecord> additionalRecords) {
            this.additionalRecords = additionalRecords;
            return this;
        }

        // TODO: make this for question also
        public Builder readFrom(byte[] response) throws UnknownHostException {
            // TODO: maybe check that return code is 0
            // checkReturnCode(response);

            MessageIterator iterator = new MessageIterator(response);

            id(iterator.readTwoBytes());
            flagsAndCodes(iterator.readTwoBytes());
            int questionCount = iterator.readTwoBytes();
            int answerRecordCount = iterator.readTwoBytes();
            int auxiliaryRecordCount = iterator.readTwoBytes();
            int additionalRecordCount = iterator.readTwoBytes();


            questions(readQuestions(questionCount, iterator));
            answerRecords(readAnswerRecords(answerRecordCount, iterator));
            //TODO: add auxiliary and additional

            return this;
        }


        private List<DnsQuestion> readQuestions(int questionCount, MessageIterator iterator) {
            List<DnsQuestion> questions = new ArrayList<>();
            for (int currentQuestion = 0; currentQuestion < questionCount; ++currentQuestion) {

                List<String> hostParts = new ArrayList<>();
                while (!iterator.hasEnd()) {
                    int partLen = iterator.readByte();
                    hostParts.add(iterator.readNBytesAsString(partLen));
                }
                iterator.readByte();

                int questionType = iterator.readTwoBytes();
                int questionClass = iterator.readTwoBytes();

                String questionHost = String.join(".", hostParts);
                questions.add(new DnsQuestion(questionHost, questionType, questionClass));
            }

            return questions;
        }

        private List<DnsRecord> readAnswerRecords(int answerRecordCount, MessageIterator iterator) {
            List<DnsRecord> answerRecords = new ArrayList<>();

            for (int currentAnswer = 0; currentAnswer < answerRecordCount; ++currentAnswer) {
                Stack<Integer> oldOffsetStack = new Stack<>();
                Set<Integer> visitedPointers = new HashSet<>();
                List<String> hostParts = new ArrayList<>();

                while (!iterator.hasEnd()) {
                    if (iterator.hasPointer()) {
                        int ptr = iterator.readPointer();

                        if (!visitedPointers.contains(ptr)) {
                            oldOffsetStack.push(iterator.getOffset());
                            visitedPointers.add(ptr);

                            iterator.setOffset(ptr);
                        }
                    } else {
                        int len = iterator.readByte();
                        hostParts.add(iterator.readNBytesAsString(len));
                    }
                }

                while (!oldOffsetStack.isEmpty()) {
                    iterator.setOffset(oldOffsetStack.pop());
                }

                String recordName = String.join(".", hostParts);
                DnsRecordType recordType = DnsRecordType.valueOf(iterator.readTwoBytes());
                int recordClass = iterator.readTwoBytes();
                int recordTTL = iterator.readFourBytes();
                int recordRdl = iterator.readTwoBytes();
                byte[] recordData = iterator.readNBytes(recordRdl);

                answerRecords.add(new DnsRecord(recordName, recordType, recordClass, recordTTL, recordData));
            }

            return answerRecords;
        }

        private List<DnsRecord> readAuthorityRecords(int len, MessageIterator iterator) {

            throw new UnsupportedOperationException("Not implemented yet!");
        }

        private List<DnsRecord> additionalRecords(int len, MessageIterator iterator) {
            throw new UnsupportedOperationException("Not implemented yet!");
        }
    }

    public static class MessageIterator {
        private final byte[] data;
        private int offset;

        public MessageIterator(byte[] data) {
            this.data = data;
            this.offset = 0;
        }

        public boolean hasPointer() {
            return (data[offset] & 0xFF) >= 0xC0;
        }

        public boolean hasEnd() {
            return (data[offset] == 0x00);
        }

        public int readByte() {
            int result = data[offset] & 0xFF;
            ++offset;
            return result;
        }

        public int readTwoBytes() {
            int result = ((data[offset] & 0xFF) << 8)
                    | (data[offset + 1] & 0xFF);

            offset += 2;
            return result;
        }

        public int readFourBytes() {
            int result = ((data[offset] & 0xFF) << 24)
                    | ((data[offset + 1] & 0xFF) << 16)
                    | ((data[offset + 2] & 0xFF) << 8)
                    | (data[offset + 3] & 0xFF);

            offset += 4;
            return result;
        }

        public byte[] readNBytes(int n) {
            byte[] bytes = Arrays.copyOfRange(data, offset, offset + n);
            offset += n;
            return bytes;
        }

        public String readNBytesAsString(int n) {
            String result = new String(data, offset, n);
            offset += n;
            return result;
        }

        public int readPointer() {
            int result = ((data[offset] & 0x3F) << 8) | (data[offset + 1] & 0xFF);
            offset += 2;
            return result;
        }

        public int getOffset() {
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }
    }
}
