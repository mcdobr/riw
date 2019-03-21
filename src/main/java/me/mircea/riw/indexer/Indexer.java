package me.mircea.riw.indexer;

import com.google.common.base.Preconditions;
import me.mircea.riw.model.Document;

public interface Indexer {
    void indexDocument(Document doc);
}
