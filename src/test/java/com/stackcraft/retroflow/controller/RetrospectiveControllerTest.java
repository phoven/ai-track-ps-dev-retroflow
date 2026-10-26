package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static com.stackcraft.retroflow.controller.TeamControllerTest.postJson;
import static com.stackcraft.retroflow.TestData.uniqueTeamName;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Status codes of the retrospective endpoints (decisions.md, "HTTP status policy"). */
@SpringBootTest
@AutoConfigureMockMvc
class RetrospectiveControllerTest {

    private static final long UNKNOWN_ID = Long.MAX_VALUE;
    private static final String MEMBER = "Grace";
    private static final String OUTSIDER = "Mallory";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TeamService teamService;

    private Team team;

    @BeforeEach
    void setUp() {
        team = teamService.createTeam(uniqueTeamName(), List.of(MEMBER));
    }

    private ResultActions createRetro(long teamId, String title, String date, String createdBy) throws Exception {
        return postJson(mockMvc, "/api/teams/" + teamId + "/retrospectives", """
                {"title": "%s", "date": %s, "createdBy": "%s"}""".formatted(title, date, createdBy));
    }

    private long createdRetroId(String title, String date) throws Exception {
        String location = createRetro(team.getId(), title, date, MEMBER).andReturn().getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private ResultActions close(long retroId, String closedBy) throws Exception {
        return mockMvc.perform(put("/api/retrospectives/{id}/close", retroId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"closedBy\": \"%s\"}".formatted(closedBy)));
    }

    @Test
    void createRetrospective_byMember_returns201() throws Exception {
        // Act & Assert
        createRetro(team.getId(), "Sprint 7", "\"2026-10-12\"", MEMBER)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/retrospectives/")))
                .andExpect(jsonPath("$.teamId").value(team.getId()))
                .andExpect(jsonPath("$.date").value("2026-10-12"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void createRetrospective_withMissingOrInvalidDate_returns400() throws Exception {
        // Act & Assert
        createRetro(team.getId(), "Sprint 7", "null", MEMBER)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Request new: date must not be null"));
        createRetro(team.getId(), "Sprint 7", "\"26/10/2026\"", MEMBER)
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRetrospective_byNonMember_returns403() throws Exception {
        // Act & Assert
        createRetro(team.getId(), "Sprint 7", "\"2026-10-12\"", OUTSIDER)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(containsString("'Mallory' is not a member")));
    }

    @Test
    void createRetrospective_forUnknownTeam_returns404() throws Exception {
        // Act & Assert
        createRetro(UNKNOWN_ID, "Sprint 7", "\"2026-10-12\"", MEMBER).andExpect(status().isNotFound());
    }

    @Test
    void createRetrospective_whenTeamAlreadyHasOpenRetro_returns409() throws Exception {
        // Arrange
        createdRetroId("Sprint 7", "\"2026-10-12\"");

        // Act & Assert
        createRetro(team.getId(), "Sprint 8", "\"2026-10-26\"", MEMBER)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(containsString("already has an open retrospective")));
    }

    @Test
    void listRetrospectives_forTeam_returns200ArrayNewestFirst() throws Exception {
        // Arrange
        long older = createdRetroId("Sprint 7", "\"2026-10-12\"");
        close(older, MEMBER);
        long newer = createdRetroId("Sprint 8", "\"2026-10-26\"");

        // Act & Assert
        mockMvc.perform(get("/api/teams/{teamId}/retrospectives", team.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(newer))
                .andExpect(jsonPath("$[1].status").value("CLOSED"));
    }

    @Test
    void listRetrospectives_forUnknownTeam_returns404() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/teams/{teamId}/retrospectives", UNKNOWN_ID)).andExpect(status().isNotFound());
    }

    @Test
    void getRetrospective_whenExistsOrUnknown_returns200Or404() throws Exception {
        // Arrange
        long retroId = createdRetroId("Sprint 7", "\"2026-10-12\"");

        // Act & Assert
        mockMvc.perform(get("/api/retrospectives/{id}", retroId)).andExpect(status().isOk());
        mockMvc.perform(get("/api/retrospectives/{id}", UNKNOWN_ID)).andExpect(status().isNotFound());
    }

    @Test
    void closeRetrospective_byMember_returns200Closed() throws Exception {
        // Arrange
        long retroId = createdRetroId("Sprint 7", "\"2026-10-12\"");

        // Act & Assert
        close(retroId, MEMBER).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void closeRetrospective_whenAlreadyClosed_returns409() throws Exception {
        // Arrange
        long retroId = createdRetroId("Sprint 7", "\"2026-10-12\"");
        close(retroId, MEMBER);

        // Act & Assert
        close(retroId, MEMBER)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Retrospective %d: already closed, cannot be closed again".formatted(retroId)));
    }

    @Test
    void closeRetrospective_byNonMemberOrWithoutClosedBy_returns403Or400() throws Exception {
        // Arrange
        long retroId = createdRetroId("Sprint 7", "\"2026-10-12\"");

        // Act & Assert
        close(retroId, OUTSIDER).andExpect(status().isForbidden());
        close(retroId, " ").andExpect(status().isBadRequest());
    }

    @Test
    void closeRetrospective_withUnknownId_returns404() throws Exception {
        // Act & Assert
        close(UNKNOWN_ID, MEMBER).andExpect(status().isNotFound());
    }

}

