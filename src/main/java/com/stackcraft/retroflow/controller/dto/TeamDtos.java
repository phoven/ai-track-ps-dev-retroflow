package com.stackcraft.retroflow.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Request and response bodies for {@code /api/teams}. No entity classes: mapping is in {@code ResponseMapper}. */
public final class TeamDtos {

    private TeamDtos() {
    }

    public record CreateTeamRequest(
            @NotBlank @Size(max = FieldLimits.MAX_NAME_LENGTH) String name,
            @NotEmpty List<@NotBlank @Size(max = FieldLimits.MAX_NAME_LENGTH) String> members) {
    }

    public record TeamResponse(Long id, String name, List<String> members) {
    }

}
