package me.mircea.riw.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.mircea.riw.model.DocumentContent;
import me.mircea.riw.model.DocumentMetadata;
import me.mircea.riw.model.DocumentPair;
import me.mircea.riw.util.ResourceManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.AbstractMap;
import java.util.Set;
import java.util.stream.Collectors;

public class HtmlParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlParser.class);

    private final TextParser textParser;

    // TODO: pass only locale and pass down to text parser, and text parser loads the appropriate resources
    public HtmlParser() {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        Set<String> exceptionWords = ResourceManager.readAllLines("english_exceptionwords.txt");
        Set<String> stopWords = ResourceManager.readAllLines("english_stopwords.txt");

        this.textParser = new TextParser(exceptionWords, stopWords);
    }


    public DocumentPair parseFile(String path, String url) throws IOException {
        File inputFile = new File(path);
        return parseFile(inputFile, url);
    }

    public DocumentPair parseFile(File file, String url) throws IOException {
        Document doc = Jsoup.parse(file, null, "https://en.wikipedia.org/wiki/MapReduce");

        DocumentContent content = new DocumentContent(doc);
        DocumentMetadata metadata = new DocumentMetadata(doc);

        return new DocumentPair(content, metadata);
    }

    public void persistDocumentPair(DocumentPair pair) throws IOException {
        try (PrintWriter contentWriter = new PrintWriter("content.index");
             PrintWriter metaWriter = new PrintWriter("meta.index");
        ) {
            persistContent(pair.getContent(), contentWriter);
            persistMetadata(pair.getMetadata(), metaWriter);
        } catch (FileNotFoundException fnfe) {
            LOGGER.warn("Could not create file {}", fnfe);
        }
    }

    private void persistContent(DocumentContent content, PrintWriter writer) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, content);
    }

    private void persistMetadata(DocumentMetadata metadata, PrintWriter writer) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, metadata);
    }

    public TextParser getTextParser() {
        return textParser;
    }
}
