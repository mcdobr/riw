package me.mircea.riw.parser;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

import me.mircea.riw.parser.WordTokenizer;


public class WordTokenizerTest {
    @Test
    public void countsOccurencesCorrectly() {
        String mockStr = "Ana are mere, pere, bere, iarasi mere si bere. Apoi vine guvernu si ia 19%";

        WordTokenizer tokenizer = new WordTokenizer();
        Map<String, Integer> occurences = tokenizer.countOccurences(mockStr);

        assertEquals(Integer.valueOf(2), occurences.get("mere"));
        assertEquals(Integer.valueOf(2), occurences.get("bere"));
        assertEquals(Integer.valueOf(1), occurences.get("guvernu"));
    }
}
