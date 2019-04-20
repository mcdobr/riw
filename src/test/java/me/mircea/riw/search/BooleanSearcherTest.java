package me.mircea.riw.search;

import me.mircea.riw.indexer.InMemoryIndexer;
import me.mircea.riw.model.Document;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class BooleanSearcherTest {
    private Document doc1 = new Document("doc1", "Muhammad Ali is beaten by smokin' Joe Frazier in their boxing bout");
    private Document doc2 = new Document("doc2", "ali began boxing at age 12. Ali was born and raised in Louisville, Kentucky.");
    private Document doc3 = new Document("doc3", "Some string is here to show off that smokin is bad");
    private BooleanSearcher booleanSearcher;

    @Before
    public void setUp() {
        InMemoryIndexer indexer = new InMemoryIndexer(doc1, doc2, doc3);
        booleanSearcher = new BooleanSearcher(indexer.getInvertedIndex());
    }

    @Test
    public void shouldOrCorrectly() {
        Set<String> reunion;

        reunion = booleanSearcher.reunion("muhammad", "frazier");
        assertEquals(1, reunion.size());

        reunion = booleanSearcher.reunion("smokin", "born");
        assertEquals(3, reunion.size());
    }

    @Test
    public void shouldAndCorrectly() {
        Set<String> intersection = booleanSearcher.intersection("ali", "box");
        assertEquals(2, intersection.size());
    }

    @Test
    public void shouldDifferenceCorrectly() {
        Set<String> difference = booleanSearcher.difference("smokin", "born");
        assertEquals(2, difference.size());
    }


    @Test
    public void shouldResolveQueryCorrectly() {
        Set<String> results;

        results = booleanSearcher.search("smokin | born & frazier");
        assertEquals(1, results.size());

        results = booleanSearcher.search("frazier % frazier");
        assertEquals(0, results.size());

        results = booleanSearcher.search("began % born | muhammad");
        assertEquals(1, results.size());
    }
}
