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
class FixtureControllerTest {

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
    @DisplayName("POST /api/fixtures/generate generates fixtures for registered teams")
    void testGenerateFixtures() throws Exception {
        mockMvc.perform(post("/api/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TeamRequest("Team Red"))));

        mockMvc.perform(post("/api/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TeamRequest("Team Blue"))));

        mockMvc.perform(post("/api/fixtures/generate"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].roundNumber", is(1)))
                .andExpect(jsonPath("$[0].status", is("SCHEDULED")));
    }

    @Test
    @DisplayName("POST /api/fixtures/generate with < 2 teams returns 400 Bad Request")
    void testGenerateFixturesInsufficientTeams() throws Exception {
        mockMvc.perform(post("/api/fixtures/generate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("At least 2 teams")));
    }

    @Test
    @DisplayName("GET /api/fixtures returns all fixtures")
    void testGetAllFixtures() throws Exception {
        mockMvc.perform(get("/api/fixtures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
