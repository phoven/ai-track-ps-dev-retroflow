package com.stackcraft.retroflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * A piece of feedback in a retrospective: content, a type, and the name of
 * the team member who submitted it. Items can only be added to an OPEN
 * retrospective and can never be modified once it is CLOSED.
 *
 * Inheritance: {@link ActionItem} extends this class using SINGLE_TABLE.
 * All items live in one table (no joins when listing a retrospective's
 * feedback, simple WHERE clauses when filtering action items); the
 * action-item-only columns (priority, completed) are null for plain feedback.
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "item_kind")
@DiscriminatorValue("FEEDBACK")
public class FeedbackItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retrospective_id", nullable = false)
    private Retrospective retrospective;

    @Column(nullable = false, length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackType type;

    @Column(nullable = false)
    private String author;

    protected FeedbackItem() {
        // for JPA
    }

    public FeedbackItem(Retrospective retrospective, String content, FeedbackType type, String author) {
        this.retrospective = retrospective;
        this.content = content;
        this.type = type;
        this.author = author;
    }

    public Long getId() {
        return id;
    }

    public Retrospective getRetrospective() {
        return retrospective;
    }

    public String getContent() {
        return content;
    }

    public FeedbackType getType() {
        return type;
    }

    public String getAuthor() {
        return author;
    }

}
