package com.stackcraft.retroflow.controller.dto;

import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import com.stackcraft.retroflow.entity.Team;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Request and response bodies for the retrospective endpoints. */
public final class RetrospectiveDtos {

    private RetrospectiveDtos() {
    }

    /** {@code createdBy} must be a member of the team (D3); {@code date} is required, any date (D4). */
    public record CreateRetrospectiveRequest(
            @NotBlank @Size(max = Retrospective.MAX_TITLE_LENGTH) String title,
            @NotNull LocalDate date,
            @NotBlank @Size(max = Team.MAX_NAME_LENGTH) String createdBy) {
    }

    /** {@code closedBy} must be a member of the team (D3). */
    public record CloseRetrospectiveRequest(
            @NotBlank @Size(max = Team.MAX_NAME_LENGTH) String closedBy) {
    }

    public record RetrospectiveResponse(Long id, Long teamId, String title, LocalDate date, RetrospectiveStatus status) {

        public static RetrospectiveResponse from(Retrospective retrospective) {
            // getTeam().getId() doesn't initialize the lazy team proxy
            return new RetrospectiveResponse(retrospective.getId(), retrospective.getTeam().getId(),
                    retrospective.getTitle(), retrospective.getDate(), retrospective.getStatus());
        }
    }

}

