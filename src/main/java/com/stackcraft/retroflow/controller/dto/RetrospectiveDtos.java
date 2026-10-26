package com.stackcraft.retroflow.controller.dto;

import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request and response bodies for the retrospective endpoints. No entity classes: mapping is in
 * {@code ResponseMapper}. The {@link RetrospectiveStatus} enum is a value type, not an entity (P9).
 */
public final class RetrospectiveDtos {

    private RetrospectiveDtos() {
    }

    /** {@code createdBy} must be a member of the team (D3); {@code date} is required, any date (D4). */
    public record CreateRetrospectiveRequest(
            @NotBlank @Size(max = FieldLimits.MAX_TITLE_LENGTH) String title,
            @NotNull LocalDate date,
            @NotBlank @Size(max = FieldLimits.MAX_NAME_LENGTH) String createdBy) {
    }

    /** {@code closedBy} must be a member of the team (D3). */
    public record CloseRetrospectiveRequest(
            @NotBlank @Size(max = FieldLimits.MAX_NAME_LENGTH) String closedBy) {
    }

    /**
     * Body of {@code PATCH /api/retrospectives/{id}} (D15). title, date and status are optional, but at
     * least one is required (checked in the service). {@code updatedBy} must be a member of the team.
     */
    public record UpdateRetrospectiveRequest(
            @Size(max = FieldLimits.MAX_TITLE_LENGTH) String title,
            LocalDate date,
            RetrospectiveStatus status,
            @NotBlank @Size(max = FieldLimits.MAX_NAME_LENGTH) String updatedBy) {
    }

    public record RetrospectiveResponse(Long id, Long teamId, String title, LocalDate date, RetrospectiveStatus status) {
    }

}

