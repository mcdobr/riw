package me.mircea.riw.indexer;

import static org.junit.Assert.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class InMemoryIndexerTest {
    File htmlDir;

    @Before
    public void setUp() {
        htmlDir = new File(getClass().getClassLoader().getResource("htmlFiles").getFile());
    }

    @Test
    public void shouldIndexAllResourceFiles() throws IOException {
        InMemoryIndexer indexer = new InMemoryIndexer();
        for (File htmlFile : htmlDir.listFiles()) {
            Document doc = Jsoup.parse(htmlFile, null);
            Element canonicalUrlTag = doc.selectFirst("meta[property='og:url']");

            if (canonicalUrlTag != null) {
                String canonicalUrl = canonicalUrlTag.attr("content");
                indexer.indexDocument(new me.mircea.riw.model.Document(doc));
            }
        }

        assertEquals(2, indexer.getDirectIndex().size());

        indexer.persistIndices();
        //indexer.getDirectIndex().entrySet().forEach(System.out::println);
        //indexer.getInvertedIndex().entrySet().forEach(System.out::println);
    }
}
