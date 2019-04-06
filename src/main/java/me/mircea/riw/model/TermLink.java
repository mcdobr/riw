package me.mircea.riw.model;

import com.google.common.base.Preconditions;
import org.bson.types.ObjectId;

public class TermLink {
    private ObjectId documentId;
    private String documentUrl;
    private Integer appearances;

    public TermLink() {
    }

    public TermLink(Document doc, int appearances) {
        Preconditions.checkNotNull(doc.getId());
        Preconditions.checkNotNull(doc.getAbsUrl());
        this.documentId = doc.getId();
        this.documentUrl = doc.getAbsUrl();
        this.appearances = appearances;
    }

    public ObjectId getDocumentId() {
        return documentId;
    }

    public void setDocumentId(ObjectId documentId) {
        this.documentId = documentId;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }

    public Integer getAppearances() {
        return appearances;
    }

    public void setAppearances(Integer appearances) {
        this.appearances = appearances;
    }
}
