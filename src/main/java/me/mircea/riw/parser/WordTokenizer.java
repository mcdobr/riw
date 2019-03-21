package me.mircea.riw.parser;

import java.util.HashMap;
import java.util.Map;

public class WordTokenizer {
	static final int SINGLE_QUOTE_CODE_POINT = 0x27;

    public Map<String, Integer> countOccurences(String str) {
    	Map<String, Integer> occurences = new HashMap<>();

        StringBuilder builder = new StringBuilder();
        final int length = str.length();
        for (int offset = 0; offset < length; ) {
        	final int codePoint = Character.toLowerCase(str.codePointAt(offset));
        	
        	if (Character.isLetterOrDigit(codePoint) ||
					(codePoint == SINGLE_QUOTE_CODE_POINT && builder.length() > 0)) {
        		builder.appendCodePoint(codePoint);
        	} else {
				trimSingleQuotesAndUpsert(occurences, builder);
        	}
        	
        	offset += Character.charCount(codePoint);
        }
        trimSingleQuotesAndUpsert(occurences, builder);
        
        return occurences;
    }

    private void trimSingleQuotesAndUpsert(final Map<String, Integer> occurences, StringBuilder builder) {
		while (builder.length() > 0 && builder.charAt(builder.length() - 1) == SINGLE_QUOTE_CODE_POINT) {
			builder.setLength(builder.length() - 1);
		}

		if (builder.length() > 0) {
			String word = builder.toString();
			occurences.merge(word, 1, Integer::sum);
			builder.setLength(0);
		}
	}
}
