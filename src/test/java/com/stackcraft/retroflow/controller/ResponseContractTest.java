package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.Priority;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.service.FeedbackService;
import com.stackcraft.retroflow.service.RetrospectiveService;
import com.stackcraft.retroflow.service.TeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer only, services mocked: the JSON of every response type has exactly the agreed fields,
 * now that the mapping lives in {@code ResponseMapper} instead of the DTO records.
 */
@WebMvcTest
class ResponseContractTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private TeamService teamService;
    @MockBean
    private RetrospectiveService retrospectiveService;
    @MockBean
    private FeedbackService feedbackService;

    /** Entities have no id setter (ids are generated, D12), so tests set it the way JPA does. */
    private static <T> T withId(T entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private final Team team = withId(new Team("Alpha", List.of("Alice", "Bob")), 1L);
    private final Retrospective retro = withId(new Retrospective(team, "Sprint 7", LocalDate.of(2026, 10, 26)), 2L);
    private final ActionItem actionItem = withId(new ActionItem(retro, "Fix CI", "Alice", Priority.HIGH), 3L);

    @Test
    void teamResponse_hasIdNameAndMembers() throws Exception {
        when(teamService.createTeam(anyString(), anyList())).thenReturn(team);

        mockMvc.perform(post("/api/teams").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Alpha", "members": ["Alice", "Bob"]}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/teams/1"))
                .andExpect(jsonPath("$.*", hasSize(3)))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Alpha"))
                .andExpect(jsonPath("$.members[0]").value("Alice"))
                .andExpect(jsonPath("$.members[1]").value("Bob"));
    }

    @Test
    void retrospectiveResponse_hasIdTeamIdTitleDateAndStatus() throws Exception {
        when(retrospectiveService.listRetrospectives(1L)).thenReturn(List.of(retro));

        mockMvc.perform(get("/api/teams/1/retrospectives"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].*", hasSize(5)))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].teamId").value(1))
                .andExpect(jsonPath("$[0].title").value("Sprint 7"))
                .andExpect(jsonPath("$[0].date").value("2026-10-26"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void actionItemResponse_hasIdRetrospectiveIdContentAuthorPriorityAndCompleted() throws Exception {
        when(feedbackService.updateActionItem(3L, Priority.LOW, null)).thenReturn(actionItem);

        mockMvc.perform(patch("/api/action-items/3").contentType(MediaType.APPLICATION_JSON).content("""
                        {"priority": "LOW"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(6)))
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.retrospectiveId").value(2))
                .andExpect(jsonPath("$.content").value("Fix CI"))
                .andExpect(jsonPath("$.author").value("Alice"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.completed").value(false));
    }

}

