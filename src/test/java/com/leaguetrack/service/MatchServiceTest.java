package com.leaguetrack.service;

import com.leaguetrack.dto.FixtureResponse;
import com.leaguetrack.dto.MatchResultRequest;
import com.leaguetrack.dto.StandingsResponse;
import com.leaguetrack.dto.TeamRequest;
import com.leaguetrack.dto.TeamResponse;
import com.leaguetrack.enums.FixtureStatus;
import com.leaguetrack.enums.MatchResult;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MatchServiceTest {

    @Autowired
    private MatchService matchService;

    @Autowired
    private FixtureService fixtureService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private StandingsService standingsService;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private FixtureRepository fixtureRepository;

    @Autowired
    private StandingsEntryRepository standingsEntryRepository;

    @Autowired
    private TeamRepository teamRepository;

    private TeamResponse teamA;
    private TeamResponse teamB;
    private FixtureResponse fixture;

    @BeforeEach
    void setUp() {
        matchRepository.deleteAll();
        fixtureRepository.deleteAll();
        standingsEntryRepository.deleteAll();
        teamRepository.deleteAll();

        teamA = teamService.registerTeam(new TeamRequest("Arsenal"));
        teamB = teamService.registerTeam(new TeamRequest("Chelsea"));

        List<FixtureResponse> fixtures = fixtureService.generateRoundRobinFixtures();
        fixture = fixtures.get(0);
    }

    @Test
    @DisplayName("1. Record Home Win updates match, fixture status to COMPLETED, and standings (3 pts to winner, 0 to loser)")
    void testRecordHomeWin() {
        // Find who is home and who is away
        Long homeId = fixture.getHomeTeamId();
        Long awayId = fixture.getAwayTeamId();

        FixtureResponse updated = matchService.recordMatchResult(fixture.getId(), new MatchResultRequest(3, 1));

        assertEquals(FixtureStatus.COMPLETED, updated.getStatus());
        assertEquals(3, updated.getHomeScore());
        assertEquals(1, updated.getAwayScore());
        assertEquals(MatchResult.HOME_WIN, updated.getResult());

        StandingsEntry homeEntry = standingsEntryRepository.findByTeamId(homeId).orElseThrow();
        assertEquals(1, homeEntry.getPlayed());
        assertEquals(1, homeEntry.getWins());
        assertEquals(0, homeEntry.getDraws());
        assertEquals(0, homeEntry.getLosses());
        assertEquals(3, homeEntry.getPoints());

        StandingsEntry awayEntry = standingsEntryRepository.findByTeamId(awayId).orElseThrow();
        assertEquals(1, awayEntry.getPlayed());
        assertEquals(0, awayEntry.getWins());
        assertEquals(0, awayEntry.getDraws());
        assertEquals(1, awayEntry.getLosses());
        assertEquals(0, awayEntry.getPoints());
    }

    @Test
    @DisplayName("2. Record Draw gives 1 point to each team")
    void testRecordDraw() {
        Long homeId = fixture.getHomeTeamId();
        Long awayId = fixture.getAwayTeamId();

        matchService.recordMatchResult(fixture.getId(), new MatchResultRequest(2, 2));

        StandingsEntry homeEntry = standingsEntryRepository.findByTeamId(homeId).orElseThrow();
        assertEquals(1, homeEntry.getPlayed());
        assertEquals(1, homeEntry.getDraws());
        assertEquals(1, homeEntry.getPoints());

        StandingsEntry awayEntry = standingsEntryRepository.findByTeamId(awayId).orElseThrow();
        assertEquals(1, awayEntry.getPlayed());
        assertEquals(1, awayEntry.getDraws());
        assertEquals(1, awayEntry.getPoints());
    }

    @Test
    @DisplayName("3. Record Away Win gives 3 points to away team and 0 to home team")
    void testRecordAwayWin() {
        Long homeId = fixture.getHomeTeamId();
        Long awayId = fixture.getAwayTeamId();

        matchService.recordMatchResult(fixture.getId(), new MatchResultRequest(0, 2));

        StandingsEntry awayEntry = standingsEntryRepository.findByTeamId(awayId).orElseThrow();
        assertEquals(1, awayEntry.getWins());
        assertEquals(3, awayEntry.getPoints());

        StandingsEntry homeEntry = standingsEntryRepository.findByTeamId(homeId).orElseThrow();
        assertEquals(1, homeEntry.getLosses());
        assertEquals(0, homeEntry.getPoints());
    }

    @Test
    @DisplayName("4. Edit match result reverses old standings contribution and applies new result without double-counting")
    void testEditMatchResult() {
        Long homeId = fixture.getHomeTeamId();
        Long awayId = fixture.getAwayTeamId();

        // 1. First record Home Win (3-1)
        matchService.recordMatchResult(fixture.getId(), new MatchResultRequest(3, 1));

        StandingsEntry homeBefore = standingsEntryRepository.findByTeamId(homeId).orElseThrow();
        assertEquals(3, homeBefore.getPoints());
        assertEquals(1, homeBefore.getWins());

        // 2. Edit match result to a Draw (2-2)
        FixtureResponse edited = matchService.updateMatchResult(fixture.getId(), new MatchResultRequest(2, 2));
        assertEquals(2, edited.getHomeScore());
        assertEquals(2, edited.getAwayScore());
        assertEquals(MatchResult.DRAW, edited.getResult());

        StandingsEntry homeAfter = standingsEntryRepository.findByTeamId(homeId).orElseThrow();
        assertEquals(1, homeAfter.getPlayed(), "Played count should still be 1");
        assertEquals(0, homeAfter.getWins(), "Wins should be reversed to 0");
        assertEquals(1, homeAfter.getDraws(), "Draws should be 1");
        assertEquals(1, homeAfter.getPoints(), "Points should be 1 for draw, not 4 (no double counting!)");

        StandingsEntry awayAfter = standingsEntryRepository.findByTeamId(awayId).orElseThrow();
        assertEquals(1, awayAfter.getPlayed());
        assertEquals(0, awayAfter.getLosses());
        assertEquals(1, awayAfter.getDraws());
        assertEquals(1, awayAfter.getPoints());
    }

    @Test
    @DisplayName("5. Negative score is rejected")
    void testNegativeScoreRejection() {
        assertThrows(BusinessRuleException.class, () -> {
            matchService.recordMatchResult(fixture.getId(), new MatchResultRequest(-1, 2));
        });
        assertThrows(BusinessRuleException.class, () -> {
            matchService.recordMatchResult(fixture.getId(), new MatchResultRequest(2, -3));
        });
    }

    @Test
    @DisplayName("6. Duplicate match recording rejected when result already exists (must use PUT to edit)")
    void testDuplicateMatchResultRecordingRejection() {
        matchService.recordMatchResult(fixture.getId(), new MatchResultRequest(1, 0));

        assertThrows(BusinessRuleException.class, () -> {
            matchService.recordMatchResult(fixture.getId(), new MatchResultRequest(2, 0));
        });
    }

    @Test
    @DisplayName("7. Recording match result on invalid fixture ID throws ResourceNotFoundException")
    void testInvalidFixtureRejection() {
        assertThrows(ResourceNotFoundException.class, () -> {
            matchService.recordMatchResult(999999L, new MatchResultRequest(1, 1));
        });
    }
}
