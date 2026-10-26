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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Feedback in a retrospective: content, a type and the name of its author.
 * Items can only be added, edited or deleted while the retrospective is OPEN
 * (enforced by {@code FeedbackService}).
 *
 * Inheritance: {@link ActionItem} extends this class with SINGLE_TABLE. All items
 * share one table: listing a retrospective's feedback needs no joins or unions,
 * and filtering action items is a plain WHERE clause. The price is that the
 * action-item columns (priority, completed) must be nullable.
 *
 * No setters (D12): only the content can change, through {@link #editContent(String)}. Changing
 * the type, author or retrospective would break the inheritance, D5 or the closed-retro rule.
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "item_kind")
@DiscriminatorValue("FEEDBACK")
public class FeedbackItem {

    public static final int MAX_CONTENT_LENGTH = 2000;
    public static final int MAX_AUTHOR_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retrospective_id", nullable = false)
    private Retrospective retrospective;

    @NotBlank
    @Size(max = MAX_CONTENT_LENGTH)
    @Column(nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackType type;

    @NotBlank
    @Size(max = MAX_AUTHOR_LENGTH)
    @Column(nullable = false, length = MAX_AUTHOR_LENGTH)
    private String author;

    public FeedbackItem() {
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

    /** Replaces the text. Only allowed while the retrospective is OPEN: the caller (service) checks that (D5). */
    public void editContent(String content) {
        this.content = content;
    }

    public FeedbackType getType() {
        return type;
    }

    public String getAuthor() {
        return author;
    }


}

