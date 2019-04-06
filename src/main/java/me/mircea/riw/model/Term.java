package me.mircea.riw.model;

import com.google.common.base.Preconditions;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Term {
    private ObjectId id;
    private String name;
    private List<TermLink> documentFrequency;

    public Term() {
        this.id = new ObjectId();
        this.documentFrequency = new ArrayList<>();
    }

    public Term(String name) {
        Preconditions.checkNotNull(name);
        this.id = new ObjectId();
        this.name = name;
        this.documentFrequency = new ArrayList<>();
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

    public List<TermLink> getDocumentFrequency() {
        return documentFrequency;
    }

    public void setDocumentFrequency(List<TermLink> documentFrequency) {
        this.documentFrequency = documentFrequency;
    }
}
