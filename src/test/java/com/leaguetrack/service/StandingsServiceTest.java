package com.leaguetrack.service;

import com.leaguetrack.dto.DashboardStatsResponse;
import com.leaguetrack.dto.MatchResultRequest;
import com.leaguetrack.dto.StandingsResponse;
import com.leaguetrack.dto.TeamRequest;
import com.leaguetrack.dto.TeamResponse;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class StandingsServiceTest {

    @Autowired
    private StandingsService standingsService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private FixtureService fixtureService;

    @Autowired
    private MatchService matchService;

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
    @DisplayName("1. Empty database behavior returns empty standings and 0 dashboard stats")
    void testEmptyDatabaseBehavior() {
        List<StandingsResponse> standings = standingsService.getStandings();
        assertNotNull(standings);
        assertTrue(standings.isEmpty(), "Standings must be empty on fresh database");

        DashboardStatsResponse stats = standingsService.getDashboardStats();
        assertEquals(0, stats.getTotalTeams());
        assertEquals(0, stats.getTotalFixtures());
        assertEquals(0, stats.getCompletedMatches());
        assertEquals("No leader yet", stats.getCurrentLeader());
    }

    @Test
    @DisplayName("2. Deterministic Standings Sorting: Points DESC, Wins DESC, Team Name ASC")
    void testStandingsSorting() {
        TeamResponse tA = teamService.registerTeam(new TeamRequest("Zebras"));
        TeamResponse tB = teamService.registerTeam(new TeamRequest("Antelopes"));
        TeamResponse tC = teamService.registerTeam(new TeamRequest("Cheetahs"));

        var fixtures = fixtureService.generateRoundRobinFixtures();

        // Match: Zebras vs Antelopes -> Draw (1-1)
        var f1 = fixtures.stream().filter(f -> (f.getHomeTeamId().equals(tA.getId()) && f.getAwayTeamId().equals(tB.getId())) ||
                (f.getHomeTeamId().equals(tB.getId()) && f.getAwayTeamId().equals(tA.getId()))).findFirst().orElseThrow();
        matchService.recordMatchResult(f1.getId(), new MatchResultRequest(1, 1));

        // Both Zebras and Antelopes have 1 point, 0 wins.
        // Alphabetical order should put Antelopes before Zebras.
        List<StandingsResponse> standings = standingsService.getStandings();
        assertEquals(3, standings.size());
        assertEquals("Antelopes", standings.get(0).getTeamName());
        assertEquals("Zebras", standings.get(1).getTeamName());
        assertEquals("Cheetahs", standings.get(2).getTeamName());
    }

    @Test
    @DisplayName("3. Recalculate Standings accurately rebuilds table from recorded match history")
    void testRecalculateStandings() {
        TeamResponse t1 = teamService.registerTeam(new TeamRequest("Tigers"));
        TeamResponse t2 = teamService.registerTeam(new TeamRequest("Lions"));

        var fixtures = fixtureService.generateRoundRobinFixtures();
        var f = fixtures.get(0);

        // Record a match
        if (f.getHomeTeamId().equals(t1.getId())) {
            matchService.recordMatchResult(f.getId(), new MatchResultRequest(3, 0));
        } else {
            matchService.recordMatchResult(f.getId(), new MatchResultRequest(0, 3));
        }

        // Corrupt or clear standings manually
        var entries = standingsEntryRepository.findAll();
        for (var e : entries) {
            e.setPoints(999);
            standingsEntryRepository.save(e);
        }

        // Call recalculate
        standingsService.recalculateStandings();

        List<StandingsResponse> standings = standingsService.getStandings();
        assertEquals(2, standings.size());
        assertEquals("Tigers", standings.get(0).getTeamName());
        assertEquals(3, standings.get(0).getPoints());
        assertEquals(1, standings.get(0).getWins());

        assertEquals("Lions", standings.get(1).getTeamName());
        assertEquals(0, standings.get(1).getPoints());
        assertEquals(1, standings.get(1).getLosses());
    }
}
