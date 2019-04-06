package me.mircea.riw.indexer;

import me.mircea.riw.model.Document;

public interface Indexer {
    void indexDocument(Document doc);
}
