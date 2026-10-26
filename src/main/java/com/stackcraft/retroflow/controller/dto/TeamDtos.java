package com.stackcraft.retroflow.controller.dto;

import com.stackcraft.retroflow.entity.Team;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Request and response bodies for {@code /api/teams}. */
public final class TeamDtos {

    private TeamDtos() {
    }

    public record CreateTeamRequest(
            @NotBlank @Size(max = Team.MAX_NAME_LENGTH) String name,
            @NotEmpty List<@NotBlank @Size(max = Team.MAX_NAME_LENGTH) String> members) {
    }

    public record TeamResponse(Long id, String name, List<String> members) {

        public static TeamResponse from(Team team) {
            return new TeamResponse(team.getId(), team.getName(), List.copyOf(team.getMembers()));
        }
    }

}

