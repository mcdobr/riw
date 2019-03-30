package me.mircea.riw.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.mircea.riw.model.Document;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;

public class HtmlParser implements Parser {
    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlParser.class);

    private final TextParser textParser;

    // TODO: pass only locale and pass down to text parser, and text parser loads the appropriate resources
    // and maybe do not set text parser as field but in each method because you could have files with
    // different locales
    public HtmlParser() {
        this.textParser = new TextParser();
    }


    @Override
    public Document parse(Path path) {
        return null;
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
        try (PrintWriter writer = new PrintWriter(doc.getName() + ".index");
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
