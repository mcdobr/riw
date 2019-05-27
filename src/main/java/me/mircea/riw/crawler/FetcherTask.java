package me.mircea.riw.crawler;

/**
 * This class exists so a fetcher must not be reinitialized at every request and still be polite to web servers
 * and cpu friendly (not use Thread.sleep())
 */
public class FetcherTask implements Runnable{
    private final Fetcher fetcher;

    public FetcherTask(Fetcher fetcher) {
        this.fetcher = fetcher;
    }

    @Override
    public void run() {
        fetcher.fetchOne();
    }
}
