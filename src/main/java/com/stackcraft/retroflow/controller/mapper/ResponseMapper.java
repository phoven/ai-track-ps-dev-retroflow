package com.stackcraft.retroflow.controller.mapper;

import com.stackcraft.retroflow.controller.dto.ActionItemDtos.ActionItemResponse;
import com.stackcraft.retroflow.controller.dto.RetrospectiveDtos.RetrospectiveResponse;
import com.stackcraft.retroflow.controller.dto.TeamDtos.TeamResponse;
import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.Team;

import java.util.List;

/**
 * The only place in the web layer that reads entities: turns them into response DTOs, so
 * the DTO records themselves stay free of entity classes. Stateless, so static methods.
 */
public final class ResponseMapper {

    private ResponseMapper() {
    }

    public static TeamResponse toResponse(Team team) {
        return new TeamResponse(team.getId(), team.getName(), List.copyOf(team.getMembers()));
    }

    public static RetrospectiveResponse toResponse(Retrospective retrospective) {
        // getTeam().getId() doesn't initialize the lazy team proxy
        return new RetrospectiveResponse(retrospective.getId(), retrospective.getTeam().getId(),
                retrospective.getTitle(), retrospective.getDate(), retrospective.getStatus());
    }

    public static ActionItemResponse toResponse(ActionItem actionItem) {
        // getRetrospective().getId() doesn't initialize the lazy retrospective proxy
        return new ActionItemResponse(actionItem.getId(), actionItem.getRetrospective().getId(),
                actionItem.getContent(), actionItem.getAuthor(), actionItem.getPriority(), actionItem.isCompleted());
    }

    public static List<RetrospectiveResponse> toRetrospectiveResponses(List<Retrospective> retrospectives) {
        return retrospectives.stream().map(ResponseMapper::toResponse).toList();
    }

    public static List<ActionItemResponse> toActionItemResponses(List<ActionItem> actionItems) {
        return actionItems.stream().map(ResponseMapper::toResponse).toList();
    }

}

