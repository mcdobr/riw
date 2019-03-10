package me.mircea.riw.indexer;

import me.mircea.riw.parser.HtmlParser;
import me.mircea.riw.parser.TextParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws IOException {
        HtmlParser parser = new HtmlParser();
        parser.parseFile("C:\\Users\\mircea\\IdeaProjects\\riw\\MapReduce.txt",
                "https://en.wikipedia.org/wiki/MapReduce");

        // Dir parsing
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        Set<String> exceptionWords = readWordSet(classloader.getResourceAsStream("english_exceptionwords.txt"));
        Set<String> stopWords = readWordSet(classloader.getResourceAsStream("english_stopwords.txt"));

        TextParser dirParser = new TextParser(exceptionWords, stopWords);
        String baseDirPath = "C:\\Users\\mircea\\Desktop\\txt_files";

        dirParser.parseDirectory(baseDirPath);
    }

    private static Set<String> readWordSet(InputStream is) throws IOException {
        Set<String> wordSet = new HashSet<>();

        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String wordLine = null;
        while ((wordLine = br.readLine()) != null) {
            wordSet.add(wordLine.trim());
        }

        return wordSet;
    }
}
