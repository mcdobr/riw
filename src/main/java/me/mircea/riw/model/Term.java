package me.mircea.riw.model;

import com.google.common.base.Preconditions;
import org.bson.types.ObjectId;

import java.util.HashMap;
import java.util.Map;

public class Term {
    private ObjectId id;
    private String name;
    private Map<ObjectId, Integer> documentFrequency;

    public Term() {
        this.documentFrequency = new HashMap<>();
    }

    public Term(String name) {
        Preconditions.checkNotNull(name);
        this.name = name;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<ObjectId, Integer> getDocumentFrequency() {
        return documentFrequency;
    }

    public void setDocumentFrequency(Map<ObjectId, Integer> documentFrequency) {
        this.documentFrequency = documentFrequency;
    }
}
