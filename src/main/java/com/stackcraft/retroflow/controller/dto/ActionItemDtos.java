package com.stackcraft.retroflow.controller.dto;

import com.stackcraft.retroflow.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request and response bodies for the action item endpoints. No entity classes: mapping is in
 * {@code ResponseMapper}. The {@link Priority} enum is a value type, not an entity (P9).
 */
public final class ActionItemDtos {

    private ActionItemDtos() {
    }

    /** {@code author} is any name, not checked against the team (D2); a null priority means MEDIUM (D6). */
    public record CreateActionItemRequest(
            @NotBlank @Size(max = FieldLimits.MAX_CONTENT_LENGTH) String content,
            @NotBlank @Size(max = FieldLimits.MAX_NAME_LENGTH) String author,
            Priority priority) {
    }

    /**
     * Body of {@code PATCH /api/action-items/{id}} (D8, D14). Both fields are optional, but at least
     * one is required (checked in the service). An unknown priority value is rejected with 400.
     */
    public record UpdateActionItemRequest(Priority priority, Boolean completed) {
    }

    public record ActionItemResponse(Long id, Long retrospectiveId, String content, String author,
                                     Priority priority, boolean completed) {
    }

}

