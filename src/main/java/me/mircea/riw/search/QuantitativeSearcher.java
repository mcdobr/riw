package me.mircea.riw.search;

import com.google.common.base.Preconditions;
import me.mircea.riw.db.DatabaseManager;
import me.mircea.riw.model.Document;
import me.mircea.riw.model.Term;
import me.mircea.riw.model.TermLink;

import javax.xml.crypto.Data;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QuantitativeSearcher {
    private DatabaseManager dbManager;

    public QuantitativeSearcher() {
        dbManager = DatabaseManager.getInstance();
    }

    public List<Document> search(String query) {
        Preconditions.checkNotNull(query);
        return search(new Document(query, "query"));
    }

    public List<Document> search(Document query) {
        Preconditions.checkNotNull(query);
        final Set<String> stems = query.getTerms().keySet();

        final long noDocuments = dbManager.getNumberOfDocuments();

        Map<Document, List<Double>> relevantDocumentsCoordinates = new HashMap<>();
        for (Term term : dbManager.getRelevantTerms(stems)) {
            for (TermLink termLink: term.getDocumentFrequency()) {
                Document doc = dbManager.getDocument(termLink.getDocumentId());

                // term frequency tf and log scaled idf
                double tf = termLink.getAppearances() / doc.size();
                //double idf = log();
            }
        }

        /*
        getAllTermsOfQuery();
        for all terms
                $or between wordsets and get list of documents

        for all documents compute relevance and sort in decreasing order by relevance
*/
    }

}
