package me.mircea.riw.search;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class BooleanSearcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(BooleanSearcher.class);

    public enum BooleanSearchOperator {
        AND,
        OR,
        DIFF
    }

    private final Map<String, Map<String, Integer>> invertedIndex;

    public BooleanSearcher(Map<String, Map<String, Integer>> invertedIndex) {
        Preconditions.checkNotNull(invertedIndex);
        this.invertedIndex = invertedIndex;
    }

    public Set<String> search(String query) {
        //TODO: make this more robust
        /* Split strings into operands and operators */
        final int SINGLE_QUOTE_CODE_POINT = 0x27;

        List<String> words = new ArrayList<>();
        List<BooleanSearchOperator> operators = new ArrayList<>();

        StringBuilder builder = new StringBuilder();
        final int length = query.length();
        for (int offset = 0; offset < length; ) {
            final int codePoint = Character.toLowerCase(query.codePointAt(offset));

            if (Character.isLetterOrDigit(codePoint) ||
                    (codePoint == SINGLE_QUOTE_CODE_POINT && builder.length() > 0)) {
                builder.appendCodePoint(codePoint);
            } else {
                while (builder.length() > 0 && builder.charAt(builder.length() - 1) == SINGLE_QUOTE_CODE_POINT) {
                    builder.setLength(builder.length() - 1);
                }

                if (operators.size() == words.size() - 1) {
                    if (codePoint == (int) '&') {
                        operators.add(BooleanSearchOperator.AND);
                    } else if (codePoint == (int)'|') {
                        operators.add(BooleanSearchOperator.OR);
                    } else if (codePoint == (int)'%') {
                        operators.add(BooleanSearchOperator.DIFF);
                    }
                }

                if (builder.length() > 0) {
                    words.add(builder.toString());
                    builder.setLength(0);
                }
            }

            offset += Character.charCount(codePoint);
        }
        // Add last word
        if (builder.length() > 0) {
            words.add(builder.toString());
            builder.setLength(0);
        }



        Set<String> acc = invertedIndex.getOrDefault(words.get(0), Collections.emptyMap()).keySet();
        for (int i = 0; i < words.size() - 1; ++i) {
            switch (operators.get(i)) {
                case OR:
                    acc = reunion(acc,
                            invertedIndex.getOrDefault(words.get(i + 1), Collections.emptyMap()).keySet());
                    break;
                case AND:
                    acc = intersection(acc,
                            invertedIndex.getOrDefault(words.get(i + 1), Collections.emptyMap()).keySet());
                    break;
                case DIFF:
                    acc = difference(acc,
                            invertedIndex.getOrDefault(words.get(i + 1), Collections.emptyMap()).keySet());
                    break;
                default:
                    break;
            }
        }
        return acc;
    }


    public Set<String> reunion(String firstStem, String secondStem, String ...moreStems) {
        Set<String> fstDocumentIds = invertedIndex.getOrDefault(firstStem, Collections.emptyMap()).keySet();
        Set<String> sndDocumentIds = invertedIndex.getOrDefault(secondStem, Collections.emptyMap()).keySet();

        Set<String> accumulator = reunion(fstDocumentIds, sndDocumentIds);
        for (String otherStem : moreStems) {
            accumulator = reunion(accumulator,
                    invertedIndex.getOrDefault(otherStem, Collections.emptyMap()).keySet());
        }

        return accumulator;
    }

    private Set<String> reunion(Set<String> xDocumentIds, Set<String> yDocumentIds) {
        Set<String> reunion = new HashSet<>(xDocumentIds.size() + yDocumentIds.size());
        reunion.addAll(xDocumentIds);
        reunion.addAll(yDocumentIds);

        return reunion;
    }

    public Set<String> intersection(String firstStem, String secondStem, String ...moreStems) {
        Set<String> fstDocumentIds = invertedIndex.getOrDefault(firstStem, Collections.emptyMap()).keySet();
        Set<String> sndDocumentIds = invertedIndex.getOrDefault(secondStem, Collections.emptyMap()).keySet();

        Set<String> accumulator = intersection(fstDocumentIds, sndDocumentIds);
        for (String otherStem : moreStems) {
            accumulator = reunion(accumulator,
                    invertedIndex.getOrDefault(otherStem, Collections.emptyMap()).keySet());
        }

        return accumulator;
    }
    private Set<String> intersection(Set<String> fstDocumentIds, Set<String> sndDocumentIds) {
        Set<String> primary = (fstDocumentIds.size() <= sndDocumentIds.size()) ? fstDocumentIds : sndDocumentIds;
        Set<String> secondary = (primary != fstDocumentIds) ? fstDocumentIds : sndDocumentIds;

        Set<String> intersection = new HashSet<>(primary.size());

        for (String docId: primary) {
            if (secondary.contains(docId)) {
                intersection.add(docId);
            }
        }

        return intersection;
    }

    public Set<String> difference(String firstStem, String secondStem, String ...moreStems) {
        Set<String> fstDocumentIds = invertedIndex.getOrDefault(firstStem, Collections.emptyMap()).keySet();
        Set<String> sndDocumentIds = invertedIndex.getOrDefault(secondStem, Collections.emptyMap()).keySet();

        Set<String> accumulator = difference(fstDocumentIds, sndDocumentIds);
        for (String otherStem : moreStems) {
            accumulator = difference(accumulator,
                    invertedIndex.getOrDefault(otherStem, Collections.emptyMap()).keySet());
        }

        return accumulator;
    }

    private Set<String> difference(Set<String> fstDocumentIds, Set<String> sndDocumentIds) {
        Set<String> difference = new HashSet<>(fstDocumentIds.size());
        for (String docId: fstDocumentIds) {
            if (!sndDocumentIds.contains(docId)) {
                difference.add(docId);
            }
        }

        return difference;
    }
}
