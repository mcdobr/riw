package me.mircea.riw.parser;

import org.tartarus.snowball.SnowballProgram;
import org.tartarus.snowball.ext.PorterStemmer;

import java.util.HashMap;
import java.util.Map;

public class WordTokenizer {
	static final int SINGLE_QUOTE_CODE_POINT = 0x27;

	private final SnowballProgram stemmer;

	public WordTokenizer(SnowballProgram stemmer) {
		this.stemmer = stemmer;
	}

	public WordTokenizer() {
		this(new PorterStemmer());
	}

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
				upsertCanonicalForm(occurences, builder);
        	}
        	
        	offset += Character.charCount(codePoint);
        }
        upsertCanonicalForm(occurences, builder);
        
        return occurences;
    }

    private void upsertCanonicalForm(final Map<String, Integer> occurences, StringBuilder builder) {
		while (builder.length() > 0 && builder.charAt(builder.length() - 1) == SINGLE_QUOTE_CODE_POINT) {
			builder.setLength(builder.length() - 1);
		}

		if (builder.length() > 0) {
			String fullWord = builder.toString();
			builder.setLength(0);

			stemmer.setCurrent(fullWord);
			stemmer.stem();
			String stem = stemmer.getCurrent();

			if (stem != null && !stem.isEmpty())
				occurences.merge(stem, 1, Integer::sum);
		}
	}
}
