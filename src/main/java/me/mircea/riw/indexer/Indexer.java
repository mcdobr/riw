package me.mircea.riw.indexer;

import com.google.common.base.Preconditions;
import me.mircea.riw.model.Document;

public interface Indexer extends Runnable {
    void indexDocument(Document doc);
    void end();
}
