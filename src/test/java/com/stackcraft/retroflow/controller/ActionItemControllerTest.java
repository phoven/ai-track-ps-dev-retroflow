package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.service.RetrospectiveService;
import com.stackcraft.retroflow.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;

import static com.stackcraft.retroflow.TestData.uniqueTeamName;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Status codes of PATCH /api/action-items/{id} (D8, D14). */
@SpringBootTest
@AutoConfigureMockMvc
class ActionItemControllerTest {

    private static final long UNKNOWN_ID = Long.MAX_VALUE;
    private static final String MEMBER = "Grace";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TeamService teamService;
    @Autowired
    private RetrospectiveService retrospectiveService;

    private Retrospective retro;
    private ActionItem actionItem;

    @BeforeEach
    void setUp() {
        Team team = teamService.createTeam(uniqueTeamName(), List.of(MEMBER));
        retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 7", LocalDate.of(2026, 10, 26), MEMBER);
        actionItem = retrospectiveService.addActionItem(retro.getId(), "Fix CI", "HIGH", MEMBER);
    }

    private ResultActions patchItem(long id, String body) throws Exception {
        return mockMvc.perform(patch("/api/action-items/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void changePriority_inOpenRetrospective_returns200() throws Exception {
        // Act & Assert
        patchItem(actionItem.getId(), "{\"priority\": \"LOW\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    void changePriority_inClosedRetrospective_returns409() throws Exception {
        // Arrange
        retrospectiveService.closeRetrospective(retro.getId(), MEMBER);

        // Act & Assert
        patchItem(actionItem.getId(), "{\"priority\": \"LOW\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(containsString("cannot change the priority")));
    }

    @Test
    void completeActionItem_returns200ThenRepeatReturns409() throws Exception {
        // Act & Assert
        patchItem(actionItem.getId(), "{\"completed\": true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));
        patchItem(actionItem.getId(), "{\"completed\": true}").andExpect(status().isConflict());
    }

    @Test
    void uncompleteActionItem_whenCompletedReturns409_whenNotCompletedReturns200() throws Exception {
        // Act & Assert
        patchItem(actionItem.getId(), "{\"completed\": false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false));
        retrospectiveService.completeActionItem(actionItem.getId());
        patchItem(actionItem.getId(), "{\"completed\": false}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(containsString("cannot be undone")));
    }

    @Test
    void updateActionItem_withEmptyOrInvalidBody_returns400() throws Exception {
        // Act & Assert
        patchItem(actionItem.getId(), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("nothing to update")));
        patchItem(actionItem.getId(), "{\"priority\": \"URGENT\"}").andExpect(status().isBadRequest());
    }

    @Test
    void updateActionItem_withUnknownId_returns404() throws Exception {
        // Act & Assert
        patchItem(UNKNOWN_ID, "{\"priority\": \"LOW\"}").andExpect(status().isNotFound());
    }

}

