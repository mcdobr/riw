package me.mircea.riw.parser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import me.mircea.riw.parser.stem.BasicEnglishLowerCaseStemmer;
import me.mircea.riw.parser.stem.Stemmer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(TextParser.class);

    private Set<String> exceptionWords;
    private Set<String> stopWords;
    private Stemmer stemmer;

    public TextParser(Set<String> exceptionWords, Set<String> stopWords) {
        this.exceptionWords = exceptionWords;
        this.stopWords = stopWords;
        this.stemmer = new BasicEnglishLowerCaseStemmer();
    }

    public TextParser() {
        this(Collections.emptySet(), Collections.emptySet());
    }

    public void parseDirectory(String baseDirPath) throws IOException {
        File baseDir = new File(baseDirPath);
        if (!baseDir.isDirectory())
            throw new IllegalArgumentException("Specified file is not a folder");

        Queue<File> fileQueue = new LinkedList<>();
        fileQueue.add(baseDir);

        while (!fileQueue.isEmpty()) {
            File frontFile = fileQueue.poll();
            if (frontFile.isDirectory()) {
                fileQueue.addAll(Arrays.asList(frontFile.listFiles()));
            } else if (frontFile.isFile()) {
                parseFile(frontFile);
            }
        }
    }

    public Map<String, Integer> parseFile(File file) throws IOException {
        String text = new String(Files.readAllBytes(Paths.get(file.getPath())), StandardCharsets.UTF_8);
        return extractWordStems(text);
    }

    public Map<String, Integer> extractWordStems(String text) {
        WordTokenizer tokenizer = new WordTokenizer();
        Map<String, Integer> wordsInDocument = tokenizer.countOccurences(text);
        wordsInDocument.entrySet()
                .removeIf(entry -> !exceptionWords.contains(entry.getKey()) && stopWords.contains(entry.getKey()));

        return wordsInDocument;
    }
}
