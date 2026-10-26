package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.NotTeamMemberException;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.service.FeedbackService;
import com.stackcraft.retroflow.service.RetrospectiveService;
import com.stackcraft.retroflow.service.TeamService;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer only, services mocked: every error path returns the one error format
 * {@code {"error", "status", "timestamp"}} (standards §2.2), and never leaks internals (§4.2).
 */
@WebMvcTest
class ErrorFormatContractTest {

    private static final String TEAM_JSON = """
            {"name": "Alpha", "members": ["Alice"]}""";

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private TeamService teamService;
    @MockBean
    private RetrospectiveService retrospectiveService;
    @MockBean
    private FeedbackService feedbackService;

    /** The one error format: exactly these three fields, with the HTTP status repeated in the body. */
    private static ResultActions expectApiError(ResultActions result, int status, String error) throws Exception {
        return result
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.*", hasSize(3)))
                .andExpect(jsonPath("$.error").value(error))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.timestamp").isString());
    }

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                               String json) throws Exception {
        return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void invalidBody_returns400InTheErrorFormat() throws Exception {
        expectApiError(send(post("/api/teams"), """
                {"name": " ", "members": []}"""), 400,
                "Request new: members must not be empty; name must not be blank");
    }

    @Test
    void malformedJson_returns400InTheErrorFormat() throws Exception {
        expectApiError(send(post("/api/teams"), "{\"name\": "), 400,
                "Request new: body is missing, malformed, or has a value of the wrong type");
    }

    @Test
    void unknownEnumInBody_returns400NamingFieldAndAllowedValues() throws Exception {
        expectApiError(send(patch("/api/action-items/1"), """
                {"priority": "URGENT"}"""), 400,
                "Request new: 'URGENT' is not a valid priority (allowed: [LOW, MEDIUM, HIGH])");
        expectApiError(send(patch("/api/retrospectives/1"), """
                {"status": "REOPENED", "updatedBy": "Alice"}"""), 400,
                "Request new: 'REOPENED' is not a valid status (allowed: [OPEN, CLOSED])");
    }

    @Test
    void invalidPathOrQueryValue_returns400InTheErrorFormat() throws Exception {
        expectApiError(mockMvc.perform(get("/api/teams/abc")), 400, "Request new: 'abc' is not a valid id");
        expectApiError(mockMvc.perform(get("/api/retrospectives/1/action-items").param("priority", "URGENT")), 400,
                "Request new: 'URGENT' is not a valid priority");
    }

    @Test
    void serviceRejectsInput_returns400InTheErrorFormat() throws Exception {
        when(teamService.createTeam(anyString(), anyList()))
                .thenThrow(new InvalidInputException("Team", null, "duplicate member name 'alice'"));

        expectApiError(send(post("/api/teams"), TEAM_JSON), 400, "Team new: duplicate member name 'alice'");
    }

    @Test
    void notAMember_returns403InTheErrorFormat() throws Exception {
        when(retrospectiveService.closeRetrospective(anyLong(), anyString()))
                .thenThrow(new NotTeamMemberException(7L, "Mallory"));

        expectApiError(send(put("/api/retrospectives/1/close"), "{\"closedBy\": \"Mallory\"}"), 403,
                "Team 7: 'Mallory' is not a member of this team");
    }

    @Test
    void unknownId_returns404InTheErrorFormat() throws Exception {
        when(teamService.getTeam(1L)).thenThrow(new ResourceNotFoundException("Team", 1L));

        expectApiError(mockMvc.perform(get("/api/teams/1")), 404, "Team 1: not found");
    }

    @Test
    void unknownRoute_returns404InTheErrorFormat() throws Exception {
        expectApiError(mockMvc.perform(get("/api/nope")), 404, "Not Found");
    }

    @Test
    void unsupportedMethod_returns405InTheErrorFormat() throws Exception {
        expectApiError(mockMvc.perform(delete("/api/teams/1")), 405, "Method Not Allowed");
    }

    @Test
    void businessRuleViolation_returns409InTheErrorFormat() throws Exception {
        when(teamService.createTeam(anyString(), anyList()))
                .thenThrow(new RetroflowException("Team", null, "a team named 'Alpha' already exists"));

        expectApiError(send(post("/api/teams"), TEAM_JSON), 409, "Team new: a team named 'Alpha' already exists");
    }

    @Test
    void databaseConstraintRace_returns409WithoutSqlDetails() throws Exception {
        when(teamService.createTeam(anyString(), anyList()))
                .thenThrow(new DataIntegrityViolationException("Unique index or primary key violation: PUBLIC.TEAM(NAME)"));

        expectApiError(send(post("/api/teams"), TEAM_JSON), 409,
                "Request new: conflicts with a concurrent change, please try again")
                .andExpect(jsonPath("$.error").value(not(containsString("PUBLIC.TEAM"))));
    }

    @Test
    void lockTimeoutRace_returns409InTheErrorFormat() throws Exception {
        when(retrospectiveService.createRetrospective(anyLong(), anyString(), any(), anyString()))
                .thenThrow(new PessimisticLockingFailureException("Timeout trying to lock table TEAM"));

        expectApiError(send(post("/api/teams/1/retrospectives"), """
                {"title": "Sprint 7", "date": "2026-10-26", "createdBy": "Alice"}"""), 409,
                "Request new: conflicts with a concurrent change, please try again");
    }

    @Test
    void unexpectedException_returns500WithoutDetails() throws Exception {
        when(teamService.getTeam(1L)).thenThrow(new IllegalStateException("secret internals"));

        expectApiError(mockMvc.perform(get("/api/teams/1")), 500, "Unexpected error");
    }

    @Test
    void errorEndpoint_returnsTheErrorFormatInsteadOfSpringBootsDefault() throws Exception {
        expectApiError(mockMvc.perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 400)),
                400, "Bad Request");
        expectApiError(mockMvc.perform(get("/error")), 500, "Internal Server Error");
    }

    @Test
    void successResponses_areNotErrors() throws Exception {
        when(feedbackService.listActionItems(1L, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/retrospectives/1/action-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

}





