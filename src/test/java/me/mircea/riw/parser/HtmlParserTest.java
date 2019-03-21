package me.mircea.riw.parser;

import me.mircea.riw.model.Document;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HtmlParserTest {
    HtmlParser parser = new HtmlParser();


    @Test
    public void shouldParseContentCorrectly() throws IOException {
        Document doc = parser.parseFile("C:\\Users\\mircea\\IdeaProjects\\riw\\MapReduce.txt",
                "https://en.wikipedia.org/wiki/MapReduce");

        assertTrue(doc.getTitle().toLowerCase().contains("mapreduce"));
    }
}
