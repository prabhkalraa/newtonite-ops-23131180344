package com.newtonite.ops.comment;

import java.time.Instant;
import java.util.UUID;

public class CommentResponse {
    private UUID id;
    private UUID workItemId;
    private UUID authorId;
    private String authorName;
    private String content;
    private Instant createdAt;

    public CommentResponse() {}

    public CommentResponse(UUID id, UUID workItemId, UUID authorId, String authorName, String content, Instant createdAt) {
        this.id = id;
        this.workItemId = workItemId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static CommentResponse fromEntity(Comment comment) {
        if (comment == null) return null;
        return new CommentResponse(
                comment.getId(),
                comment.getWorkItemId(),
                comment.getAuthorId(),
                comment.getAuthorName(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWorkItemId() {
        return workItemId;
    }

    public void setWorkItemId(UUID workItemId) {
        this.workItemId = workItemId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
