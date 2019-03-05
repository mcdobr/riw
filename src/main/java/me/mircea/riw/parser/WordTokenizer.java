package me.mircea.riw.parser;

import java.util.HashMap;
import java.util.Map;

public class WordTokenizer {
    public Map<String, Integer> countOccurences(String str) {
        final int SINGLE_QUOTE_CODE_POINT = 0x27;
    	
    	Map<String, Integer> occurences = new HashMap<>();

        StringBuilder builder = new StringBuilder();
        
        final int length = str.length();
        for (int offset = 0; offset < length; ) {
        	final int codePoint = Character.toLowerCase(str.codePointAt(offset));
        	
        	if (Character.isLetterOrDigit(codePoint) || codePoint == SINGLE_QUOTE_CODE_POINT) {
        		builder.appendCodePoint(codePoint);
        	} else {
        		String word = builder.toString();
        		
        		if (word.length() > 0)
        			occurences.merge(word, 1, Integer::sum);
        		
        		builder.setLength(0);
        	}
        	
        	offset += Character.charCount(codePoint);
        }
        
        return occurences;
    }
}
