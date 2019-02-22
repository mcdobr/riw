package me.mircea.riw.parser;

import java.util.HashMap;
import java.util.Map;

public class WordTokenizer {
    public Map<String, Integer> countOccurences(String str) {
        Map<String, Integer> occurences = new HashMap<>();

        StringBuilder builder = new StringBuilder();
        String delimiters = " \r\n\t\",:;.!?[]{}()<>";

        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (delimiters.indexOf(c) == -1) {
                builder.append(c);
            } else if (builder.length() != 0) {
                String word = builder.toString();
                occurences.merge(word, 1, Integer::sum);

                builder.setLength(0);
            } else {
                builder.setLength(0);
            }
        }

        return occurences;
    }
}
