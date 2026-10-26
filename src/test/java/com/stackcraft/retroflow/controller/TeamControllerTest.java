package com.stackcraft.retroflow.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.stackcraft.retroflow.TestData.uniqueTeamName;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Status codes and error bodies of the team endpoints. */
@SpringBootTest
@AutoConfigureMockMvc
class TeamControllerTest {

    private static final long UNKNOWN_ID = Long.MAX_VALUE;

    @Autowired
    private MockMvc mockMvc;

    static ResultActions postJson(MockMvc mockMvc, String url, String json) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions createTeam(String name, String membersJson) throws Exception {
        return postJson(mockMvc, "/api/teams", """
                {"name": "%s", "members": %s}""".formatted(name, membersJson));
    }

    @Test
    void createTeam_withValidBody_returns201WithLocation() throws Exception {
        // Act & Assert
        createTeam(uniqueTeamName(), "[\"Alice\", \"Bob\"]")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/teams/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.members[1]").value("Bob"));
    }

    @Test
    void createTeam_withBlankNameAndNoMembers_returns400WithBothFields() throws Exception {
        // Act & Assert
        createTeam(" ", "[]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(startsWith("Request new: ")))
                .andExpect(jsonPath("$.error").value(containsString("members must not be empty")))
                .andExpect(jsonPath("$.error").value(containsString("name must not be blank")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void createTeam_withDuplicateMembers_returns400() throws Exception {
        // Act & Assert
        createTeam(uniqueTeamName(), "[\"Alice\", \"alice\"]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Team new: duplicate member name 'alice'"));
    }

    @Test
    void createTeam_withTakenName_returns409() throws Exception {
        // Arrange
        String name = uniqueTeamName();
        createTeam(name, "[\"Alice\"]");

        // Act & Assert
        createTeam(name, "[\"Bob\"]")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(endsWith("already exists")));
    }

    @Test
    void createTeam_withMalformedJson_returns400WithoutInternals() throws Exception {
        // Act & Assert
        postJson(mockMvc, "/api/teams", "{\"name\": ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("malformed")));
    }

    @Test
    void getTeam_whenExists_returns200() throws Exception {
        // Arrange
        String location = createTeam(uniqueTeamName(), "[\"Alice\"]").andReturn().getResponse().getHeader("Location");

        // Act & Assert
        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members[0]").value("Alice"));
    }

    @Test
    void getTeam_withUnknownId_returns404() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/teams/{id}", UNKNOWN_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Team %d: not found".formatted(UNKNOWN_ID)))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getTeam_withNonNumericId_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/teams/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Request new: 'abc' is not a valid id"));
    }

    @Test
    void unsupportedMethodAndUnknownUrl_keepTheirSpringStatus() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/teams/1")).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(get("/api/nope")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

}

