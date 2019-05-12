package me.mircea.riw.indexer;


import me.mircea.riw.indexer.Indexer;
import me.mircea.riw.model.Document;

public class NoopIndexer implements Indexer {

    @Override
    public void indexDocument(Document doc) {
        System.out.println(doc.getAbsUrl());
    }
}