package me.mircea.riw.dns;

public class DnsException extends Throwable {
    private final int returnCode;
    public DnsException(int returnCode) {
        this.returnCode = returnCode;
    }
}
