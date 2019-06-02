package me.mircea.riw.dns;

import java.util.Arrays;
import java.util.Objects;

public class DnsRecord {
    private final String name;
    private final DnsRecordType type;
    private final Integer clazz;
    private final Integer ttl;
    private final byte[] data;

    public DnsRecord(String name, DnsRecordType type, Integer clazz, Integer ttl, byte[] data) {
        this.name = name;
        this.type = type;
        this.clazz = clazz;
        this.ttl = ttl;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public DnsRecordType getType() {
        return type;
    }

    public Integer getClazz() {
        return clazz;
    }

    public Integer getTtl() {
        return ttl;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DnsRecord{");
        sb.append("name='").append(name).append('\'');
        sb.append(", type=").append(type);
        sb.append(", clazz=").append(clazz);
        sb.append(", ttl=").append(ttl);
        sb.append(", data='").append(data).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DnsRecord record = (DnsRecord) o;
        return name.equals(record.name) &&
                type == record.type &&
                clazz.equals(record.clazz) &&
                ttl.equals(record.ttl) &&
                Arrays.equals(data, record.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name, type, clazz, ttl);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}
