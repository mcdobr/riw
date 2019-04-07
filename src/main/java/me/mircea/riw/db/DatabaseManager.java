package me.mircea.riw.db;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import me.mircea.riw.model.Document;
import me.mircea.riw.model.Term;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;


public class DatabaseManager {
    private static final DatabaseManager instance = new DatabaseManager();

    public static DatabaseManager getInstance() {
        return instance;
    }

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<Document> documents;
    private final MongoCollection<Term> terms;


    private DatabaseManager() {
        this.mongoClient = MongoClients.create();

        CodecRegistry pojoCodecRegistry = fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );
        this.database = this.mongoClient.getDatabase("sengine")
                .withCodecRegistry(pojoCodecRegistry);

        this.documents = this.database.getCollection("documents", Document.class);
        this.documents.createIndex(Indexes.ascending("name"));

        this.terms = this.database.getCollection("terms", Term.class);
        this.terms.createIndex(Indexes.ascending("name"));
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public void upsertDocument(Document doc) {
        Bson sameDocumentFilter = or(
                eq("_id", doc.getId()),
                eq("absUrl", doc.getAbsUrl())
        );

        Bson updateOperation = combine(
                set("name", doc.getName()),
                set("keywords", doc.getKeywords()),
                set("description", doc.getDescription()),

                set("robots", doc.getRobots()),
                set("links", doc.getLinks()),
                set("absUrl", doc.getAbsUrl()),
                set("terms", doc.getTerms())
        );
        UpdateOptions options = new UpdateOptions().upsert(true);
        UpdateResult result = this.documents.updateOne(sameDocumentFilter, updateOperation, options);

        if (result.getUpsertedId() != null)
            doc.setId(result.getUpsertedId().asObjectId().getValue());
    }

    public Document getDocument(ObjectId id) {
        return this.documents.find(eq("_id", id)).first();
    }

    public Iterable<Document> getDocuments(Set<ObjectId> relevantDocumentIds) {
        List<Bson> idEqFilter = relevantDocumentIds.stream()
                .map(doc -> eq("_id", doc))
                .collect(Collectors.toList());

        return this.documents.find(or(idEqFilter));
    }

    public void upsertTerm(Term term) {
        Bson sameTermFilter = or(
                eq("_id", term.getId()),
                eq("name", term.getName())
        );

        FindIterable<Term> terms = this.terms.find(sameTermFilter);
        for (Term temp : terms) {
            term.getDocumentFrequency().addAll(temp.getDocumentFrequency());
        }

        this.terms.deleteMany(sameTermFilter);
        this.terms.insertOne(term);
    }

    public void clean() {
        documents.drop();
        terms.drop();
    }

    public Iterable<Term> getRelevantTerms(Collection<String> queryTerms) {
        final List<Bson> termFilter = queryTerms.stream().map(stem -> eq("name", stem)).collect(Collectors.toList());
        return this.terms.find(or(termFilter));
    }

    public long getNumberOfDocuments()
    {
        return this.documents.countDocuments();
    }

}
