package me.mircea.riw.model;

import me.mircea.riw.parser.TextParser;
import org.bson.types.ObjectId;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Document {
    private ObjectId id;

    private String name;
    private String keywords;
    private String description;
    private String text;

    private String robots;
    private Set<String> links;
    private Path path;
    private String absUrl;
    private Map<String, Integer> terms;

    public Document() {
        //this.id = new ObjectId();
        this.terms = new HashMap<>();
    }

    public Document(String text, String absUrl) {
        //this.id = new ObjectId();
        this.text = text;
        this.absUrl = absUrl;
        this.terms = new TextParser().extractWordStems(this.text);
    }

    public Document(org.jsoup.nodes.Document htmlDoc) {
        // Extract content
        this.name = htmlDoc.title();

        Element keywordsTag = htmlDoc.selectFirst("meta[name='keywords']");
        if (keywordsTag != null)
            this.keywords = keywordsTag.attr("content");

        Element descriptionTag = htmlDoc.selectFirst("meta[name='description']");
        if (descriptionTag != null)
            this.description = descriptionTag.attr("content");

        this.text = htmlDoc.text();
        this.terms = new TextParser().extractWordStems(this.text);

        Element robotsTag = htmlDoc.selectFirst("meta[name='robots']");
        if (robotsTag != null)
            this.robots = robotsTag.attr("content");

        Elements links = htmlDoc.select("a[href]");

        Set<String> seenLinks = new HashSet<>();
        seenLinks.add(htmlDoc.baseUri());

        for (Element link : links) {
            String absUrl = link.absUrl("href");

            int indexOfFragment = absUrl.indexOf('#');
            String resourceUrl;
            if (indexOfFragment != -1)
                resourceUrl = absUrl.substring(0, indexOfFragment);
            else
                resourceUrl = absUrl;

            seenLinks.add(resourceUrl);
        }

        this.absUrl = htmlDoc.location();
    }

    public long size() {
        return terms.values().stream().collect(Collectors.summingLong(Integer::longValue));
    }

    @Override
    public String toString() {
        return "Document{" +
                "id=" + id +
                ", absUrl='" + absUrl + '\'' +
                // ", terms=" + terms +
                '}';
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getRobots() {
        return robots;
    }

    public void setRobots(String robots) {
        this.robots = robots;
    }

    public Set<String> getLinks() {
        return links;
    }

    public void setLinks(Set<String> links) {
        this.links = links;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public String getAbsUrl() {
        return absUrl;
    }

    public void setAbsUrl(String absUrl) {
        this.absUrl = absUrl;
    }

    public Map<String, Integer> getTerms() {
        return terms;
    }

    public void setTerms(Map<String, Integer> terms) {
        this.terms = terms;
    }
}
