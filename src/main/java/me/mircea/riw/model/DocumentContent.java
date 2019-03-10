package me.mircea.riw.model;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class DocumentContent {
    private String title;
    private String keywords;
    private String description;
    private String text;

    public DocumentContent(Document doc) {
        this.title = doc.title();

        Element keywordsTag = doc.selectFirst("meta[name='keywords']");
        if (keywordsTag != null)
            this.keywords = keywordsTag.attr("content");

        Element descriptionTag = doc.selectFirst("meta[name='description']");
        if (descriptionTag != null)
            this.description = descriptionTag.attr("content");

        this.text = doc.text();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
}
