package me.mircea.riw.indexer;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.mircea.riw.model.Document;
import me.mircea.riw.parser.HtmlParser;
import me.mircea.riw.parser.TextParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class InMemoryIndexer implements Indexer {
    private final static Logger LOGGER = LoggerFactory.getLogger(InMemoryIndexer.class);

    private final Map<String, Map<String, Integer>> directIndex;
    private final Map<String, Map<String, Integer>> invertedIndex;
    private final HtmlParser parser;

    public InMemoryIndexer(Document ...docs) {
        this();
        for (Document doc : docs) {
            indexDocument(doc);
        }
    }

    public InMemoryIndexer() {
        directIndex = new HashMap<>();
        invertedIndex = new HashMap<>();
        parser = new HtmlParser();
    }

    @Override
    public void indexDocument(Document doc) {
        TextParser textParser = parser.getTextParser();
        Map<String, Integer> wordOccurences = textParser.extractWordStems(doc.getText());

        addFileToDirectIndex(doc.getAbsUrl(), wordOccurences);
        addFileToInvertedIndex(doc.getAbsUrl(), wordOccurences);
    }

    private void addFileToDirectIndex(String url, Map<String, Integer> wordOccurences) {
        directIndex.put(url, wordOccurences);
    }

    private void addFileToInvertedIndex(String url, Map<String, Integer> wordOccurence) {
        wordOccurence.entrySet()
                .stream()
                .forEach(entry -> {
                    if (!invertedIndex.containsKey(entry.getKey())) {
                        invertedIndex.put(entry.getKey(), new HashMap<>());
                    }

                    invertedIndex.get(entry.getKey()).put(url, entry.getValue());
                });
    }

    public void persistIndices() {
        try {
            persistDirectIndex();
            persistInvertedIndex();
        } catch (IOException e) {
            LOGGER.warn("Could not persist indices {}", e);
        }
    }

    private void persistDirectIndex() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(new PrintWriter("direct.index"), directIndex);
    }

    private void persistInvertedIndex() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(new PrintWriter("inverted.index"), invertedIndex);
    }

    public Map<String, Map<String, Integer>> getDirectIndex() {
        return directIndex;
    }

    public Map<String, Map<String, Integer>> getInvertedIndex() {
        return invertedIndex;
    }
}
