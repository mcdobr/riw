package me.mircea.riw.db;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import me.mircea.riw.model.Document;
import me.mircea.riw.model.Term;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;

import java.util.function.Consumer;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class DatabaseManager {
    public static final DatabaseManager instance = new DatabaseManager();

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

    public void insertDocument(Document doc) {
        this.documents.insertOne(doc);
    }

    public void getDocument(ObjectId id)
    {
        this.documents.find(new BasicDBObject("_id", id)).forEach((Consumer<Document>) it -> System.out.println(it.getTerms()));
    }
}
