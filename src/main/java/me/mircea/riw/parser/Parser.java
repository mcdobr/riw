package me.mircea.riw.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Parser {
	private static final Logger logger = LoggerFactory.getLogger(Parser.class);
	
	private Set<String> exceptionWords;
	private Set<String> stopWords;
	
	public Parser(Set<String> exceptionWords, Set<String> stopWords) {
		this.exceptionWords = exceptionWords;
		this.stopWords = stopWords;
	}
	
	public Parser() {
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
    
    public void parseFile(File file) throws IOException {
    	WordTokenizer tokenizer = new WordTokenizer();
    	String textOfFile = new String(Files.readAllBytes(Paths.get(file.getPath())), StandardCharsets.UTF_8);
    	
    	Map<String, Integer> wordsInDocument = tokenizer.countOccurences(textOfFile);
    	
    	int lengthStart = wordsInDocument.size();
    	
    	boolean removedSomeElements = wordsInDocument.entrySet()
    		.removeIf(entry -> !exceptionWords.contains(entry.getKey()) && stopWords.contains(entry.getKey()));
    	
    	int lengthEnd = wordsInDocument.size();
    	
    	System.out.printf("File %s %b %d %d %n", file.getName(), removedSomeElements, lengthStart, lengthEnd);
    	wordsInDocument.keySet().forEach(System.out::println);
    }
	
    public void indexFile(String path, String url) throws IOException {
        File inputFile = new File(path);
        indexFile(inputFile, url);
    }

    public void indexFile(File file, String url) throws IOException {
        Document doc = Jsoup.parse(file, null, "https://en.wikipedia.org/wiki/MapReduce");

        try (PrintWriter contentWriter = new PrintWriter("content.index")) {
        	indexContent(doc, contentWriter);
        } catch (FileNotFoundException fnfe) {
        	logger.warn("Could not create content index file {}", fnfe);
        }
        
        try (PrintWriter metaWriter = new PrintWriter("meta.index")) {
        	indexMetadata(doc, metaWriter);
        } catch (FileNotFoundException fnfe) {
        	logger.warn("Could not create meta index file {}", fnfe);
        }
    }

    private void indexContent(Document doc, PrintWriter writer) {
        writer.println(doc.title());

        Element keywordsTag = doc.selectFirst("meta[name='keywords']");
        Element descriptionTag = doc.selectFirst("meta[name='description']");

        if (keywordsTag != null)
            writer.println(keywordsTag.attr("content"));
        if (descriptionTag != null)
            writer.println(descriptionTag.attr("content"));

        writer.println();
        writer.print(doc.text());
    }

    private void indexMetadata(Document doc, PrintWriter writer) {

        Element robotsTag = doc.selectFirst("meta[name='robots']");
        if (robotsTag != null)
            writer.println(robotsTag.attr("content"));
        Elements links = doc.select("a[href]");
        
        Set<String> seenLinks = new HashSet<>();
        seenLinks.add(doc.baseUri());
        
        writer.println("Links:");
        
        for (Element link : links) {
            String absUrl = link.absUrl("href");
            
            int indexOfFragment = absUrl.indexOf('#');
            String resourceUrl;
            if (indexOfFragment != -1)
            	resourceUrl = absUrl.substring(0, indexOfFragment);
            else
            	resourceUrl = absUrl;
            
            if (!seenLinks.contains(resourceUrl)) {
            	writer.println(resourceUrl);
            }
            
            seenLinks.add(resourceUrl);
        }
    }
}
