package com.stackcraft.retroflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;

/**
 * A feedback item of type ACTION_ITEM that also has a priority and a completed
 * flag. Completing is one-way: there is no setter for {@code completed}, only
 * {@link #complete()} (D12). Stored in the feedback item table (SINGLE_TABLE).
 */
@Entity
@DiscriminatorValue("ACTION")
public class ActionItem extends FeedbackItem {

    public static final Priority DEFAULT_PRIORITY = Priority.MEDIUM;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private Priority priority = DEFAULT_PRIORITY;

    // Wrapper type: the column is null for plain feedback rows in the shared table
    @Column(name = "completed")
    private Boolean completed = false;

    public ActionItem() {
        // for JPA
    }

    /** All settable fields; a null priority means the default (D6). A new action item is never completed. */
    public ActionItem(Retrospective retrospective, String content, String author, Priority priority) {
        super(retrospective, content, FeedbackType.ACTION_ITEM, author);
        this.priority = priority != null ? priority : DEFAULT_PRIORITY;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public boolean isCompleted() {
        return Boolean.TRUE.equals(completed);
    }

    /** One-way transition; callers must check {@link #isCompleted()} first. */
    public void complete() {
        this.completed = true;
    }

}

