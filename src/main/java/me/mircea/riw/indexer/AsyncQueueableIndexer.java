package me.mircea.riw.indexer;


import java.nio.file.Path;
import java.util.concurrent.Callable;

public interface AsyncQueueableIndexer extends Indexer, Callable<Boolean> {
    void scheduleDocument(Path path);
    void stop();
}
