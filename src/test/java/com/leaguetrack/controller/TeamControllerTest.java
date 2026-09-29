package com.leaguetrack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leaguetrack.dto.TeamRequest;
import com.leaguetrack.repository.FixtureRepository;
import com.leaguetrack.repository.MatchRepository;
import com.leaguetrack.repository.StandingsEntryRepository;
import com.leaguetrack.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private StandingsEntryRepository standingsEntryRepository;

    @Autowired
    private FixtureRepository fixtureRepository;

    @Autowired
    private MatchRepository matchRepository;

    @BeforeEach
    void setUp() {
        matchRepository.deleteAll();
        fixtureRepository.deleteAll();
        standingsEntryRepository.deleteAll();
        teamRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/teams creates team and returns 201 Created")
    void testCreateTeam() throws Exception {
        TeamRequest request = new TeamRequest("Eagles");

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Eagles")));
    }

    @Test
    @DisplayName("POST /api/teams with duplicate name returns 409 Conflict")
    void testDuplicateTeamReturns409() throws Exception {
        TeamRequest request1 = new TeamRequest("Hawks");
        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        TeamRequest request2 = new TeamRequest("hawks");
        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("POST /api/teams with blank name returns 400 Bad Request")
    void testBlankNameReturns400() throws Exception {
        TeamRequest request = new TeamRequest("   ");

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("GET /api/teams returns list of teams")
    void testGetAllTeams() throws Exception {
        mockMvc.perform(post("/api/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TeamRequest("Team 1"))));

        mockMvc.perform(get("/api/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Team 1")));
    }

    @Test
    @DisplayName("DELETE /api/teams/{id} deletes team and returns 204")
    void testDeleteTeam() throws Exception {
        String response = mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TeamRequest("DeleteMe"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/teams/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/teams/" + id))
                .andExpect(status().isNotFound());
    }
}
