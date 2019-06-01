package me.mircea.riw.dns;

import java.util.Map;
import java.util.HashMap;

public enum DnsRecordType {
    A(1),
    NS(2),
    CNAME(5),
    SOA(6),
    PTR(12),
    MX(15),
    TXT(16);

    private static Map<Integer, DnsRecordType> valueMap;
    static {
        valueMap = new HashMap<>();
        for (DnsRecordType recordType : DnsRecordType.values()) {
            valueMap.put(recordType.getValue(), recordType);
        }
    }

    public static DnsRecordType valueOf(int value) {
        return valueMap.get(value);
    }

    private final int value;

    DnsRecordType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
