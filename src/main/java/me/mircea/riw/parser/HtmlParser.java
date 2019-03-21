package me.mircea.riw.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.mircea.riw.model.Document;
import me.mircea.riw.util.ResourceManager;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.Set;

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


    public Document parseFile(String path, String url) throws IOException {
        File inputFile = new File(path);
        return parseFile(inputFile, url);
    }

    public Document parseFile(File file, String url) throws IOException {
        org.jsoup.nodes.Document htmlDoc = Jsoup.parse(file, null, "https://en.wikipedia.org/wiki/MapReduce");
        return new Document(htmlDoc);
    }

    public void persistDocument(Document doc) throws IOException {
        try (PrintWriter writer = new PrintWriter(doc.getTitle() + ".index");
        ) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, doc);
            //doc.setPath();

        } catch (FileNotFoundException fnfe) {
            LOGGER.warn("Could not create file {}", fnfe);
        }
    }

    public TextParser getTextParser() {
        return textParser;
    }
}
