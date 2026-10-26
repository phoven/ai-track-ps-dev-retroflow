package com.stackcraft.retroflow.controller;

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

/** Status codes of PATCH /api/retrospectives/{id} (D15). */
@SpringBootTest
@AutoConfigureMockMvc
class RetrospectiveUpdateControllerTest {

    private static final long UNKNOWN_ID = Long.MAX_VALUE;
    private static final String MEMBER = "Grace";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TeamService teamService;
    @Autowired
    private RetrospectiveService retrospectiveService;

    private Retrospective retro;

    @BeforeEach
    void setUp() {
        Team team = teamService.createTeam(uniqueTeamName(), List.of(MEMBER));
        retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 7", LocalDate.of(2026, 10, 26), MEMBER);
    }

    private ResultActions patchRetro(long id, String body) throws Exception {
        return mockMvc.perform(patch("/api/retrospectives/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void updateRetrospective_whenOpen_returns200() throws Exception {
        // Act & Assert
        patchRetro(retro.getId(), """
                {"title": "Sprint 7 (moved)", "date": "2026-10-28", "updatedBy": "Grace"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Sprint 7 (moved)"))
                .andExpect(jsonPath("$.date").value("2026-10-28"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void reopenRetrospective_whenClosed_returns409() throws Exception {
        // Arrange
        retrospectiveService.closeRetrospective(retro.getId(), MEMBER);

        // Act & Assert
        patchRetro(retro.getId(), """
                {"status": "OPEN", "updatedBy": "Grace"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Retrospective %d: closed, cannot be reopened".formatted(retro.getId())));
    }

    @Test
    void updateRetrospective_whenClosed_returns409() throws Exception {
        // Arrange
        retrospectiveService.closeRetrospective(retro.getId(), MEMBER);

        // Act & Assert
        patchRetro(retro.getId(), """
                {"title": "Renamed", "updatedBy": "Grace"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(containsString("cannot be modified")));
    }

    @Test
    void updateRetrospective_withInvalidBody_returns400() throws Exception {
        // Act & Assert
        patchRetro(retro.getId(), """
                {"updatedBy": "Grace"}""").andExpect(status().isBadRequest());
        patchRetro(retro.getId(), """
                {"title": "Renamed"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Request new: updatedBy must not be blank"));
        patchRetro(retro.getId(), """
                {"status": "REOPENED", "updatedBy": "Grace"}""").andExpect(status().isBadRequest());
        patchRetro(retro.getId(), """
                {"status": "CLOSED", "updatedBy": "Grace"}""").andExpect(status().isBadRequest());
    }

    @Test
    void updateRetrospective_byNonMember_returns403() throws Exception {
        // Act & Assert
        patchRetro(retro.getId(), """
                {"title": "Renamed", "updatedBy": "Mallory"}""").andExpect(status().isForbidden());
    }

    @Test
    void updateRetrospective_withUnknownId_returns404() throws Exception {
        // Act & Assert
        patchRetro(UNKNOWN_ID, """
                {"title": "Renamed", "updatedBy": "Grace"}""").andExpect(status().isNotFound());
    }

}

