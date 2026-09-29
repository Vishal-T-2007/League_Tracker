package com.leaguetrack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leaguetrack.dto.MatchResultRequest;
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
class MatchControllerTest {

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

    private Long fixtureId;

    @BeforeEach
    void setUp() throws Exception {
        matchRepository.deleteAll();
        fixtureRepository.deleteAll();
        standingsEntryRepository.deleteAll();
        teamRepository.deleteAll();

        mockMvc.perform(post("/api/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TeamRequest("Team A"))));

        mockMvc.perform(post("/api/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TeamRequest("Team B"))));

        String fixResp = mockMvc.perform(post("/api/fixtures/generate"))
                .andReturn().getResponse().getContentAsString();

        fixtureId = objectMapper.readTree(fixResp).get(0).get("id").asLong();
    }

    @Test
    @DisplayName("POST /api/matches/{fixtureId}/result records result and updates status")
    void testRecordMatchResult() throws Exception {
        MatchResultRequest request = new MatchResultRequest(2, 1);

        mockMvc.perform(post("/api/matches/" + fixtureId + "/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.homeScore", is(2)))
                .andExpect(jsonPath("$.awayScore", is(1)))
                .andExpect(jsonPath("$.result", is("HOME_WIN")));
    }

    @Test
    @DisplayName("PUT /api/matches/{fixtureId}/result edits existing result")
    void testUpdateMatchResult() throws Exception {
        mockMvc.perform(post("/api/matches/" + fixtureId + "/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MatchResultRequest(2, 1))))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/matches/" + fixtureId + "/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MatchResultRequest(2, 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homeScore", is(2)))
                .andExpect(jsonPath("$.awayScore", is(2)))
                .andExpect(jsonPath("$.result", is("DRAW")));
    }

    @Test
    @DisplayName("POST /api/matches/{fixtureId}/result with negative score returns 400")
    void testNegativeScoreReturns400() throws Exception {
        MatchResultRequest request = new MatchResultRequest(-1, 0);

        mockMvc.perform(post("/api/matches/" + fixtureId + "/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }
}
