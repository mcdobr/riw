package me.mircea.riw.dns;

public class DnsQuestion {
    private final String host;
    private final int type;
    private final int clazz;

    public DnsQuestion(String host, int type, int clazz) {
        this.host = host;
        this.type = type;
        this.clazz = clazz;
    }

    public String getQuestionHost() {
        return host;
    }

    public int getQuestionType() {
        return type;
    }

    public int getQuestionClass() {
        return clazz;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DnsQuestion{");
        sb.append("host='").append(host).append('\'');
        sb.append(", type=").append(type);
        sb.append(", clazz=").append(clazz);
        sb.append('}');
        return sb.toString();
    }
}
