package me.mircea.riw.parser;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Parser {
    public void indexFile(String path, String url) throws IOException {
        File inputFile = new File(path);
        indexFile(inputFile, url);
    }

    public void indexFile(File file, String url) throws IOException {
        Document doc = Jsoup.parse(file, "UTF-8", "https://en.wikipedia.org/wiki/MapReduce");

        PrintWriter contentWriter = new PrintWriter("content.index");
        PrintWriter metaWriter = new PrintWriter("meta.index");

        indexContent(doc, contentWriter);
        indexMetadata(doc, metaWriter);
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

        writer.println("Links:");
        for (Element link : links) {
            String absUrl = link.absUrl("href");
            if (!absUrl.contains(doc.baseUri()))
                writer.println(absUrl);
        }
    }

    public static void main(String[] args) throws IOException {
        Parser parser = new Parser();
        parser.indexFile("/home/student/workspace-jee/prob1/input.html", "https://en.wikipedia.org/wiki/MapReduce");

        //words.keySet().forEach(System.out::println);
    }
}
