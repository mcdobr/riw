package me.mircea.riw.search;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import me.mircea.riw.db.DatabaseManager;
import me.mircea.riw.model.Document;
import me.mircea.riw.model.Term;
import me.mircea.riw.model.TermLink;
import me.mircea.riw.util.LinearAlgebraUtil;
import org.bson.types.ObjectId;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.AbstractMap.*;

public class QuantitativeSearcher {
    private final DatabaseManager dbManager;
    private final long noDocuments;

    public QuantitativeSearcher(DatabaseManager dbManager) {
        Preconditions.checkNotNull(dbManager);
        this.dbManager = dbManager;
        this.noDocuments = dbManager.getNumberOfDocuments();
    }

    public List<SimpleImmutableEntry<Document, Double>> search(String query) {
        Preconditions.checkNotNull(query);
        return search(new Document("query", query));
    }

    public List<SimpleImmutableEntry<Document, Double>> search(Document query) {
        Preconditions.checkNotNull(query);

        Iterable<Term> queryTerms = lookupQueryTerms(query);
        Iterable<Document> relevantDocuments = lookupRelevantDocuments(queryTerms);

        List<Double> queryVector = createVector(query, queryTerms);
        Map<Document, List<Double>> documentVectors = createDocumentVectors(queryTerms, relevantDocuments);

        List<SimpleImmutableEntry<Document, Double>> documentRelevancePairs = new ArrayList<>();

        documentVectors.forEach((doc, docVector) -> {
            Double relevance = LinearAlgebraUtil.cosine(docVector, queryVector);
            documentRelevancePairs.add(new SimpleImmutableEntry<>(doc, relevance));
        });
        Comparator<SimpleImmutableEntry<Document, Double>> byRelevance =
                Comparator.comparing(entry -> -entry.getValue());
        documentRelevancePairs.sort(byRelevance);
        return documentRelevancePairs;
    }

    private Map<Document, List<Double>> createDocumentVectors(Iterable<Term> queryTerms, Iterable<Document> relevantDocuments) {
        Preconditions.checkNotNull(queryTerms);
        Preconditions.checkNotNull(relevantDocuments);

        Map<Document, List<Double>> relevantDocumentVectors = new HashMap<>();
        for (Document doc : relevantDocuments) {
            relevantDocumentVectors.put(doc, createVector(doc, queryTerms));
        }

        return relevantDocumentVectors;
    }

    private List<Double> createVector(Document doc, Iterable<Term> queryTerms) {
        Preconditions.checkNotNull(doc);
        Preconditions.checkNotNull(queryTerms);

        Set<String> seenTerms = new LinkedHashSet<>();
        List<Double> vector = new ArrayList<>();

        for (Term term : queryTerms) {
            if (!seenTerms.contains(term.getName())) {
                vector.add(tfidf(doc, term));
                seenTerms.add(term.getName());
            }
        }

        Iterable<Term> documentTerms = lookupTermsOfDocument(doc);
        for (Term term : documentTerms) {
            if (!seenTerms.contains(term.getName())) {
                vector.add(tfidf(doc, term));
                seenTerms.add(term.getName());
            }
        }

        return vector;
    }

    private Iterable<Term> lookupQueryTerms(Document query) {
        Preconditions.checkNotNull(query);
        Set<String> stems = query.getTerms().keySet();
        if (stems.isEmpty())
            return Collections.emptyList();
        else
            return dbManager.getTerms(stems);
    }

    private Iterable<Document> lookupRelevantDocuments(Iterable<Term> terms) {
        Preconditions.checkNotNull(terms);
        if (Iterables.isEmpty(terms)) {
            return Collections.emptyList();
        } else {
            Set<ObjectId> relevantDocumentIds = new HashSet<>();
            for (Term term : terms) {
                List<ObjectId> docIds = term.getDocumentFrequency()
                        .stream()
                        .map(TermLink::getDocumentId)
                        .collect(Collectors.toList());
                relevantDocumentIds.addAll(docIds);
            }
            return dbManager.getDocuments(relevantDocumentIds);
        }
    }

    private Iterable<Term> lookupTermsOfDocument(Document doc) {
        Preconditions.checkNotNull(doc);

        Set<String> termStrings = doc.getTerms().keySet();
        if (termStrings.isEmpty())
            return Collections.emptyList();
        else
            return dbManager.getTerms(termStrings);
    }

    private double tfidf(Document doc, Term term) {
        Preconditions.checkNotNull(doc);
        Preconditions.checkNotNull(term);

        double tf = 1.0 * doc.getTerms().getOrDefault(term.getName(), 0) / doc.size();
        double idf = Math.log(1.0 * noDocuments / (1 + term.getDocumentFrequency().size()));

        return tf * idf;
    }

}
