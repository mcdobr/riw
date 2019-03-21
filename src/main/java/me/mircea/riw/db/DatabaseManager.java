package me.mircea.riw.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class DatabaseManager {
    public static final DatabaseManager instance = new DatabaseManager();

    private final MongoClient mongoClient;
    private final MongoDatabase database;

    private DatabaseManager() {
        this.mongoClient = MongoClients.create();
        this.database = this.mongoClient.getDatabase("index");
    }

    public MongoDatabase getDatabase() {
        return database;
    }
}
