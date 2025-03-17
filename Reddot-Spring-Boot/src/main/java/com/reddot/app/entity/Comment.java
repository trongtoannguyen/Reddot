package com.reddot.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity(name = "comments")
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Comment extends BaseEntity {
    private boolean isDeleted = false;
    private String text;
    private int upvotes;
    private int downvotes;
    /**
     * The question that this comment is related to
     * Never null
     */
    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false, updatable = false) // prevent equals and hashCode breaking
    private Question question;
    @OneToMany(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE}
    )
    private List<Comment> replies = new ArrayList<>();
    /**
     * The comment id that this comment is a response to
     * Null if this comment responds to a question
     */
    @Column(name = "response_to_id")
    private Integer responseTo;
    @ManyToOne
    @JoinColumn(name = "author_id")
    private User user;
    @Column(name = "report_count", nullable = false)
    private int reportCount = 0;
    @JsonIgnore
    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vote> votes = new ArrayList<>();

    public Comment(String body, User author) {
        this.text = body;
        this.user = author;
    }

    public int getScore() {
        return upvotes * 3 - downvotes;
    }

    public void addReply(Comment rep) {
        rep.setQuestion(this.getQuestion());
        rep.setResponseTo(this.getId());
        this.replies.add(rep);
    }

    // set status to deleted to keep data
    public void removeReply(Comment rep) {
        rep.setDeleted(true);
    }
}
