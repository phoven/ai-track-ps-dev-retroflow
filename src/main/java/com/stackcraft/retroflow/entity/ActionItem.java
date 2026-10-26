package com.stackcraft.retroflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * A feedback item of type ACTION_ITEM that also tracks a priority and a
 * completed flag. Completing is one-way: a completed action item can never
 * be marked incomplete again (enforced by {@code RetrospectiveService}).
 *
 * Stored in the feedback item table (SINGLE_TABLE inheritance, see
 * {@link FeedbackItem}).
 */
@Entity
@DiscriminatorValue("ACTION")
public class ActionItem extends FeedbackItem {

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private Priority priority;

    // Wrapper type: the column is null for plain feedback rows in the shared table
    @Column(name = "completed")
    private Boolean completed = false;

    protected ActionItem() {
        // for JPA
    }

    public ActionItem(Retrospective retrospective, String content, Priority priority, String author) {
        super(retrospective, content, FeedbackType.ACTION_ITEM, author);
        this.priority = priority;
    }

    public Priority getPriority() {
        return priority;
    }

    public boolean isCompleted() {
        return Boolean.TRUE.equals(completed);
    }

    public void complete() {
        this.completed = true;
    }

}
