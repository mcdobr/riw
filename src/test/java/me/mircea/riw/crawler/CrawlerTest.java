package me.mircea.riw.crawler;

import me.mircea.riw.indexer.NoopIndexer;
import org.junit.Test;

import java.net.URISyntaxException;

public class CrawlerTest {

    @Test
    public void shouldCrawlASimpleDomain() throws URISyntaxException {
        Crawler crawler = new Crawler(new NoopIndexer(), "http://riweb.tibeica.com/crawl");
        //Crawler crawler = new Crawler(new NoopIndexer(), "https://www.litera.ro/");

        crawler.crawl();
    }
}
