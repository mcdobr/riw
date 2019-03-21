package me.mircea.riw.parser;

import me.mircea.riw.model.Document;

import java.io.IOException;
import java.nio.file.Path;

public interface Parser {
    Document parse(Path path) throws IOException;
}
