package me.mircea.riw.dns;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DnsCache {
    private final Set<DnsRecord> dnsRecords;
    private final ScheduledExecutorService evicter;

    public DnsCache() {
        dnsRecords = new HashSet<>();
        evicter = Executors.newSingleThreadScheduledExecutor();
    }

    public boolean add(DnsRecord record) {
        boolean wasAdded = dnsRecords.add(record);
        evicter.schedule(() -> this.remove(record), record.getTtl(), TimeUnit.SECONDS);
        return wasAdded;
    }

    public boolean remove(DnsRecord record) {
        return dnsRecords.remove(record);
    }

    public DnsRecord getAddressRecordFor(String host) {
        return dnsRecords.stream().filter(dnsRecord -> dnsRecord.getName().equals(host)).findFirst().orElse(null);
    }
}
