package me.mircea.riw.search;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
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

    public QuantitativeSearcher() {
        dbManager = DatabaseManager.getInstance();
        noDocuments = dbManager.getNumberOfDocuments();
    }

    public List<SimpleImmutableEntry<Document, Double>> search(String query) {
        Preconditions.checkNotNull(query);
        return search(new Document(query, "query"));
    }

    public List<SimpleImmutableEntry<Document, Double>> search(Document query) {
        Preconditions.checkNotNull(query);

        Set<Term> relevantTerms = lookupRelevantTerms(query);
        Set<Document> relevantDocuments = lookupRelevantDocuments(relevantTerms);

        List<Double> queryVector = createVector(query, relevantTerms);
        Map<Document, List<Double>> relevantDocumentVectors = createDocumentVectors(relevantTerms, relevantDocuments);

        List<SimpleImmutableEntry<Document, Double>> documentRelevancePairs = new ArrayList<>(relevantDocuments.size());
        relevantDocumentVectors.forEach((doc, docVector) -> {
            Double relevance = LinearAlgebraUtil.cosine(docVector, queryVector);
            documentRelevancePairs.add(new SimpleImmutableEntry<>(doc, relevance));
        });

        Comparator<SimpleImmutableEntry<Document, Double>> byRelevance =
                Comparator.comparing(entry -> -entry.getValue());
        documentRelevancePairs.sort(byRelevance);
        return documentRelevancePairs;
    }

    private Map<Document, List<Double>> createDocumentVectors(Set<Term> relevantTerms, Set<Document> relevantDocuments) {
        Preconditions.checkNotNull(relevantTerms);
        Preconditions.checkNotNull(relevantDocuments);

        Map<Document, List<Double>> relevantDocumentVectors = new HashMap<>(relevantDocuments.size());
        for (Document doc : relevantDocuments) {
            relevantDocumentVectors.put(doc, createVector(doc, relevantTerms));
        }

        return relevantDocumentVectors;
    }

    private List<Double> createVector(Document doc, Collection<Term> terms) {
        Preconditions.checkNotNull(doc);
        Preconditions.checkNotNull(terms);

        List<Double> vector = new ArrayList<>(terms.size());

        for (Term term : terms) {
            vector.add(tfidf(doc, term));
        }
        return vector;
    }

    private Set<Term> lookupRelevantTerms(Document query) {
        Preconditions.checkNotNull(query);
        Set<String> stems = query.getTerms().keySet();
        if (stems.isEmpty())
            return Collections.emptySet();
        else
            return Sets.newHashSet(dbManager.getRelevantTerms(stems));
    }

    private Set<Document> lookupRelevantDocuments(Collection<Term> terms) {
        Preconditions.checkNotNull(terms);
        if (terms.isEmpty()) {
            return Collections.emptySet();
        } else {
            Set<ObjectId> relevantDocumentIds = new HashSet<>();
            for (Term term : terms) {
                List<ObjectId> docIds = term.getDocumentFrequency()
                        .stream()
                        .map(TermLink::getDocumentId)
                        .collect(Collectors.toList());
                relevantDocumentIds.addAll(docIds);
            }
            return Sets.newHashSet(dbManager.getDocuments(relevantDocumentIds));
        }
    }

    private double tfidf(Document doc, Term term) {
        Preconditions.checkNotNull(doc);
        Preconditions.checkNotNull(term);

        double tf = 1.0 * doc.getTerms().getOrDefault(term.getName(), 0) / doc.size();
        double idf = Math.log(1.0 * noDocuments / (1 + term.getDocumentFrequency().size()));

        return tf * idf;
    }
}
