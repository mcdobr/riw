package me.mircea.riw.parser.stem;

public class BasicEnglishLowerCaseStemmer implements Stemmer {
    @Override
    public String stemWord(String word) {
        return word.toLowerCase();
    }
}
