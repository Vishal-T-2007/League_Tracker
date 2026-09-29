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
class StandingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private FixtureRepository fixtureRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private StandingsEntryRepository standingsEntryRepository;

    @BeforeEach
    void setUp() {
        matchRepository.deleteAll();
        fixtureRepository.deleteAll();
        standingsEntryRepository.deleteAll();
        teamRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/standings on empty DB returns empty list")
    void testGetStandingsEmpty() throws Exception {
        mockMvc.perform(get("/api/standings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/dashboard/stats returns correct counts and empty leader")
    void testGetDashboardStats() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTeams", is(0)))
                .andExpect(jsonPath("$.totalFixtures", is(0)))
                .andExpect(jsonPath("$.completedMatches", is(0)))
                .andExpect(jsonPath("$.currentLeader", is("No leader yet")));
    }

    @Test
    @DisplayName("GET /api/standings after registering teams returns standings with 0 points")
    void testGetStandingsWithTeams() throws Exception {
        mockMvc.perform(post("/api/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TeamRequest("Dynamos"))));

        mockMvc.perform(get("/api/standings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].teamName", is("Dynamos")))
                .andExpect(jsonPath("$[0].played", is(0)))
                .andExpect(jsonPath("$[0].points", is(0)));
    }
}
