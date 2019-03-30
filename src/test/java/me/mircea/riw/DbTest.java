package me.mircea.riw;

import me.mircea.riw.db.DatabaseManager;
import me.mircea.riw.model.Document;
import org.bson.types.ObjectId;
import org.junit.Test;

public class DbTest {

    @Test
    public void shouldSerializeAndDeserializeCorrectly() {
        Document doc = new Document("Ana are mere!", "ana.txt");

        DatabaseManager.instance.insertDocument(doc);
        DatabaseManager.instance.getDocument(new ObjectId("5c9a29afe562f9335d5982f4"));

    }
}
