package me.mircea.riw.indexer;

import me.mircea.riw.model.Document;

public interface AsyncQueueableIndexer extends Indexer, Runnable {
    void scheduleDocument(Document doc);
    void stop();
}
