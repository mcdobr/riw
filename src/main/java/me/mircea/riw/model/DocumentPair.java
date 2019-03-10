package me.mircea.riw.model;

public class DocumentPair {
    private DocumentContent content;
    private DocumentMetadata metadata;

    public DocumentPair(DocumentContent content, DocumentMetadata metadata) {
        this.content = content;
        this.metadata = metadata;
    }

    public DocumentContent getContent() {
        return content;
    }

    public void setContent(DocumentContent content) {
        this.content = content;
    }

    public DocumentMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(DocumentMetadata metadata) {
        this.metadata = metadata;
    }
}
