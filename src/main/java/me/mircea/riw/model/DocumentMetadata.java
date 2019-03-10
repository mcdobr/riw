package me.mircea.riw.model;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Set;

public class DocumentMetadata {
    private String robots;
    private Set<String> links;

    public DocumentMetadata(Document doc) {
        Element robotsTag = doc.selectFirst("meta[name='robots']");
        if (robotsTag != null)
            this.robots = robotsTag.attr("content");

        Elements links = doc.select("a[href]");

        Set<String> seenLinks = new HashSet<>();
        seenLinks.add(doc.baseUri());

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
    }


}
