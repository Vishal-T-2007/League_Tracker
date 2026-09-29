package com.leaguetrack.service;

import com.leaguetrack.dto.MatchResultRequest;
import com.leaguetrack.dto.TeamRequest;
import com.leaguetrack.dto.TeamResponse;
import com.leaguetrack.exception.BusinessRuleException;
import com.leaguetrack.exception.ResourceNotFoundException;
import com.leaguetrack.model.StandingsEntry;
import com.leaguetrack.repository.FixtureRepository;
import com.leaguetrack.repository.MatchRepository;
import com.leaguetrack.repository.StandingsEntryRepository;
import com.leaguetrack.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class TeamServiceTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private FixtureService fixtureService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private StandingsService standingsService;

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
    @DisplayName("1. Team registration initializes team and initial standings with 0 points")
    void testRegisterTeam() {
        TeamResponse response = teamService.registerTeam(new TeamRequest("Falcons"));

        assertNotNull(response.getId());
        assertEquals("Falcons", response.getName());
        assertNotNull(response.getCreatedAt());

        // Check initial standings entry exists and is zero
        StandingsEntry entry = standingsEntryRepository.findByTeamId(response.getId()).orElse(null);
        assertNotNull(entry);
        assertEquals(0, entry.getPlayed());
        assertEquals(0, entry.getWins());
        assertEquals(0, entry.getDraws());
        assertEquals(0, entry.getLosses());
        assertEquals(0, entry.getPoints());
    }

    @Test
    @DisplayName("2. Duplicate team registration throws Conflict BusinessRuleException")
    void testDuplicateTeamRejection() {
        teamService.registerTeam(new TeamRequest("Warriors"));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> {
            teamService.registerTeam(new TeamRequest("warriors")); // case-insensitive check
        });

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("3. Blank team name rejection")
    void testBlankTeamNameRejection() {
        assertThrows(BusinessRuleException.class, () -> {
            teamService.registerTeam(new TeamRequest("   "));
        });
    }

    @Test
    @DisplayName("4. Team deletion removes team, standings, related fixtures and matches, and recalculates remaining standings")
    void testTeamDeletionCascadesAndRecalculates() {
        // Register 4 teams
        TeamResponse tA = teamService.registerTeam(new TeamRequest("Team A"));
        TeamResponse tB = teamService.registerTeam(new TeamRequest("Team B"));
        TeamResponse tC = teamService.registerTeam(new TeamRequest("Team C"));
        TeamResponse tD = teamService.registerTeam(new TeamRequest("Team D"));

        // Generate fixtures (6 matches)
        var fixtures = fixtureService.generateRoundRobinFixtures();
        assertEquals(6, fixtures.size());

        // Record a match result where Team C wins against Team D (3-0)
        var fixtureCD = fixtures.stream()
                .filter(f -> (f.getHomeTeamId().equals(tC.getId()) && f.getAwayTeamId().equals(tD.getId())) ||
                             (f.getHomeTeamId().equals(tD.getId()) && f.getAwayTeamId().equals(tC.getId())))
                .findFirst().orElseThrow();

        if (fixtureCD.getHomeTeamId().equals(tC.getId())) {
            matchService.recordMatchResult(fixtureCD.getId(), new MatchResultRequest(3, 0));
        } else {
            matchService.recordMatchResult(fixtureCD.getId(), new MatchResultRequest(0, 3));
        }

        // Record a match where Team A wins against Team B (2-1)
        var fixtureAB = fixtures.stream()
                .filter(f -> (f.getHomeTeamId().equals(tA.getId()) && f.getAwayTeamId().equals(tB.getId())) ||
                             (f.getHomeTeamId().equals(tB.getId()) && f.getAwayTeamId().equals(tA.getId())))
                .findFirst().orElseThrow();

        if (fixtureAB.getHomeTeamId().equals(tA.getId())) {
            matchService.recordMatchResult(fixtureAB.getId(), new MatchResultRequest(2, 1));
        } else {
            matchService.recordMatchResult(fixtureAB.getId(), new MatchResultRequest(1, 2));
        }

        // Before delete: Team C has 3 points, Team A has 3 points, Team B has 0, Team D has 0
        var standingsBefore = standingsService.getStandings();
        assertEquals(4, standingsBefore.size());

        // Delete Team C
        teamService.deleteTeam(tC.getId());

        // Assert Team C is deleted
        assertFalse(teamRepository.existsById(tC.getId()));
        assertFalse(standingsEntryRepository.findByTeamId(tC.getId()).isPresent());

        // Assert no fixtures referencing Team C remain
        var remainingFixtures = fixtureRepository.findAll();
        assertEquals(3, remainingFixtures.size()); // 6 - 3 fixtures involving C = 3 remaining
        for (var f : remainingFixtures) {
            assertNotEquals(tC.getId(), f.getHomeTeam().getId());
            assertNotEquals(tC.getId(), f.getAwayTeam().getId());
        }

        // Assert remaining standings are recalculated
        var standingsAfter = standingsService.getStandings();
        assertEquals(3, standingsAfter.size());
        assertEquals("Team A", standingsAfter.get(0).getTeamName());
        assertEquals(3, standingsAfter.get(0).getPoints());
        assertEquals(1, standingsAfter.get(0).getPlayed());
    }

    @Test
    @DisplayName("5. Update team name successfully and prevent updating to duplicate name")
    void testUpdateTeam() {
        TeamResponse t1 = teamService.registerTeam(new TeamRequest("Tigers"));
        TeamResponse t2 = teamService.registerTeam(new TeamRequest("Lions"));

        TeamResponse updated = teamService.updateTeam(t1.getId(), new TeamRequest("Bengal Tigers"));
        assertEquals("Bengal Tigers", updated.getName());

        assertThrows(BusinessRuleException.class, () -> {
            teamService.updateTeam(t1.getId(), new TeamRequest("Lions"));
        });
    }

    @Test
    @DisplayName("6. Non-existent team lookup throws ResourceNotFoundException")
    void testGetNonExistentTeam() {
        assertThrows(ResourceNotFoundException.class, () -> {
            teamService.getTeamById(99999L);
        });
    }
}
