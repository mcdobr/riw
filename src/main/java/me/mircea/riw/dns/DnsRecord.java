package me.mircea.riw.dns;

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
}
