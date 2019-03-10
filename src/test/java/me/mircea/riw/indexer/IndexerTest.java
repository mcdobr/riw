package me.mircea.riw.indexer;

import static org.junit.Assert.*;

import me.mircea.riw.util.ResourceManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class IndexerTest {

    @Test
    public void shouldIndexCorrectly() throws IOException {
        File htmlDir = new File(getClass().getClassLoader().getResource("htmlFiles").getFile());
        assertTrue(htmlDir.isDirectory());

        Indexer indexer = new Indexer();
        for (File htmlFile : htmlDir.listFiles()) {
            Document doc = Jsoup.parse(htmlFile, null);
            Element canonicalUrlTag = doc.selectFirst("meta[property='og:url']");

            if (canonicalUrlTag != null) {
                String canonicalUrl = canonicalUrlTag.attr("content");
                indexer.indexFile(htmlFile.getAbsolutePath(), canonicalUrl);
            }
        }

        assertEquals(2, indexer.getDirectIndex().size());
        indexer.getDirectIndex().entrySet().forEach(System.out::println);
        9

        indexer.getInvertedIndex().entrySet().forEach(System.out::println);
    }
}
