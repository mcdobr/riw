package me.mircea.riw.indexer;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.mircea.riw.model.DocumentContent;
import me.mircea.riw.model.DocumentPair;
import me.mircea.riw.parser.HtmlParser;
import me.mircea.riw.parser.TextParser;

import javax.swing.text.Document;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Indexer {
    private final Map<String, Map<String, Integer>> directIndex;
    private final Map<String, Map<String, Integer>> invertedIndex;
    private HtmlParser parser;

    public Indexer() {
        directIndex = new HashMap<>();
        invertedIndex = new HashMap<>();
        parser = new HtmlParser();
    }

    public void indexFile(String contentFilePath, String url) throws IOException {
        DocumentPair pair = parser.parseFile(contentFilePath, url);
        DocumentContent content = pair.getContent();

        TextParser textParser = parser.getTextParser();
        Map<String, Integer> wordOccurences = textParser.extractWords(content.getText());

        //Map<String, Integer> wordOccurences =;
        addFileToDirectIndex(url, wordOccurences);
        addFileToInvertedIndex(url, wordOccurences);
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
        persistDirectIndex();
        persistInvertedIndex();
    }

    private void persistDirectIndex() {
        System.out.println(directIndex);
    }

    private void persistInvertedIndex() {
        System.out.println(invertedIndex);
    }

    public Map<String, Map<String, Integer>> getDirectIndex() {
        return directIndex;
    }

    public Map<String, Map<String, Integer>> getInvertedIndex() {
        return invertedIndex;
    }
}
