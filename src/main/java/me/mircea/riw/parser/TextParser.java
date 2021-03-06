package me.mircea.riw.parser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import me.mircea.riw.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextParser implements Parser {
    private static final Logger LOGGER = LoggerFactory.getLogger(TextParser.class);

    private final Set<String> exceptionWords;
    private final Set<String> stopWords;
    private final WordTokenizer tokenizer;

    public TextParser() {
        this(Locale.ENGLISH);
    }

    public TextParser(Locale locale) {
        ResourceBundle rb = ResourceBundle.getBundle("lexicon", locale);
        this.stopWords = Arrays.stream(rb.getString("stopwords").split(","))
                .collect(Collectors.toSet());
        this.exceptionWords = Arrays.stream(rb.getString("exceptionwords").split(","))
                .collect(Collectors.toSet());
        this.tokenizer = new WordTokenizer();
    }

    @Override
    public Document parse(Path path) throws IOException {
        Preconditions.checkNotNull(path);
        String text = new String(Files.readAllBytes(path));

        Map<String, Integer> stemAppearances = extractWordStems(text);

        return new Document(path.toString(), stemAppearances);
    }

    public Map<String, Integer> extractWordStems(Path path) throws IOException{
        //Files.lines(path).map()

        throw new UnsupportedOperationException("Not implemented yet");
    }

    public Map<String, Integer> extractWordStems(String text) {
        Map<String, Integer> wordsInDocument = tokenizer.countOccurences(text);
        wordsInDocument.entrySet()
                .removeIf(entry -> !exceptionWords.contains(entry.getKey()) && stopWords.contains(entry.getKey()));

        return wordsInDocument;
    }
}
