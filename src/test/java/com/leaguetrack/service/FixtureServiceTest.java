package com.leaguetrack.service;

import com.leaguetrack.dto.FixtureResponse;
import com.leaguetrack.dto.TeamRequest;
import com.leaguetrack.dto.TeamResponse;
import com.leaguetrack.enums.FixtureStatus;
import com.leaguetrack.exception.BusinessRuleException;
import com.leaguetrack.repository.FixtureRepository;
import com.leaguetrack.repository.MatchRepository;
import com.leaguetrack.repository.StandingsEntryRepository;
import com.leaguetrack.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class FixtureServiceTest {

    @Autowired
    private FixtureService fixtureService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private FixtureRepository fixtureRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private StandingsEntryRepository standingsEntryRepository;

    @Autowired
    private TeamRepository teamRepository;

    @BeforeEach
    void setUp() {
        matchRepository.deleteAll();
        fixtureRepository.deleteAll();
        standingsEntryRepository.deleteAll();
        teamRepository.deleteAll();
    }

    @Test
    @DisplayName("1. Round-robin generation with Even number of teams (4 teams -> 6 fixtures, no duplicate pairings, no team plays itself)")
    void testGenerateFixturesEvenTeams() {
        teamService.registerTeam(new TeamRequest("Team A"));
        teamService.registerTeam(new TeamRequest("Team B"));
        teamService.registerTeam(new TeamRequest("Team C"));
        teamService.registerTeam(new TeamRequest("Team D"));

        List<FixtureResponse> fixtures = fixtureService.generateRoundRobinFixtures();

        // 4 * (4 - 1) / 2 = 6 fixtures
        assertEquals(6, fixtures.size());

        Set<String> uniquePairings = new HashSet<>();
        for (FixtureResponse f : fixtures) {
            assertNotEquals(f.getHomeTeamId(), f.getAwayTeamId(), "Team should not play against itself");
            assertEquals(FixtureStatus.SCHEDULED, f.getStatus());

            // Check pairing uniqueness regardless of home/away
            long id1 = Math.min(f.getHomeTeamId(), f.getAwayTeamId());
            long id2 = Math.max(f.getHomeTeamId(), f.getAwayTeamId());
            String pairKey = id1 + "-" + id2;

            assertFalse(uniquePairings.contains(pairKey), "Duplicate fixture pairing found: " + pairKey);
            uniquePairings.add(pairKey);
        }

        assertEquals(6, uniquePairings.size());
    }

    @Test
    @DisplayName("2. Round-robin generation with Odd number of teams (5 teams -> 10 fixtures, 5 rounds)")
    void testGenerateFixturesOddTeams() {
        for (int i = 1; i <= 5; i++) {
            teamService.registerTeam(new TeamRequest("Team " + i));
        }

        List<FixtureResponse> fixtures = fixtureService.generateRoundRobinFixtures();

        // 5 * (5 - 1) / 2 = 10 fixtures
        assertEquals(10, fixtures.size());

        Set<String> uniquePairings = new HashSet<>();
        for (FixtureResponse f : fixtures) {
            assertNotEquals(f.getHomeTeamId(), f.getAwayTeamId());

            long id1 = Math.min(f.getHomeTeamId(), f.getAwayTeamId());
            long id2 = Math.max(f.getHomeTeamId(), f.getAwayTeamId());
            String pairKey = id1 + "-" + id2;

            assertFalse(uniquePairings.contains(pairKey));
            uniquePairings.add(pairKey);
        }

        assertEquals(10, uniquePairings.size());
    }

    @Test
    @DisplayName("3. Fixture generation fails when less than 2 teams are registered")
    void testGenerateFixturesLessThanTwoTeams() {
        teamService.registerTeam(new TeamRequest("Solo Team"));

        assertThrows(BusinessRuleException.class, () -> {
            fixtureService.generateRoundRobinFixtures();
        });
    }

    @Test
    @DisplayName("4. Fixture generation rejected if fixtures already exist")
    void testDuplicateFixtureGenerationRejection() {
        teamService.registerTeam(new TeamRequest("Alpha"));
        teamService.registerTeam(new TeamRequest("Beta"));

        fixtureService.generateRoundRobinFixtures();

        assertThrows(BusinessRuleException.class, () -> {
            fixtureService.generateRoundRobinFixtures();
        });
    }

    @Test
    @DisplayName("5. Reset fixtures clears all fixtures and matches")
    void testResetFixtures() {
        teamService.registerTeam(new TeamRequest("Alpha"));
        teamService.registerTeam(new TeamRequest("Beta"));

        fixtureService.generateRoundRobinFixtures();
        assertEquals(1, fixtureRepository.count());

        fixtureService.resetFixtures();
        assertEquals(0, fixtureRepository.count());
    }
}
