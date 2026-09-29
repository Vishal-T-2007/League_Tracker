package com.leaguetrack.service;

import com.leaguetrack.config.ScoringProperties;
import com.leaguetrack.dto.FixtureResponse;
import com.leaguetrack.dto.MatchResultRequest;
import com.leaguetrack.enums.FixtureStatus;
import com.leaguetrack.enums.MatchResult;
import com.leaguetrack.exception.BusinessRuleException;
import com.leaguetrack.exception.ResourceNotFoundException;
import com.leaguetrack.model.Fixture;
import com.leaguetrack.model.Match;
import com.leaguetrack.model.StandingsEntry;
import com.leaguetrack.repository.FixtureRepository;
import com.leaguetrack.repository.MatchRepository;
import com.leaguetrack.repository.StandingsEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final FixtureRepository fixtureRepository;
    private final StandingsEntryRepository standingsEntryRepository;
    private final ScoringProperties scoringProperties;

    public MatchService(MatchRepository matchRepository,
                        FixtureRepository fixtureRepository,
                        StandingsEntryRepository standingsEntryRepository,
                        ScoringProperties scoringProperties) {
        this.matchRepository = matchRepository;
        this.fixtureRepository = fixtureRepository;
        this.standingsEntryRepository = standingsEntryRepository;
        this.scoringProperties = scoringProperties;
    }

    @Transactional
    public FixtureResponse recordMatchResult(Long fixtureId, MatchResultRequest request) {
        validateScores(request);

        Fixture fixture = fixtureRepository.findById(fixtureId)
                .orElseThrow(() -> new ResourceNotFoundException("Fixture not found with ID: " + fixtureId));

        if (matchRepository.existsByFixtureId(fixtureId)) {
            throw new BusinessRuleException("Match result has already been recorded for this fixture. Use PUT to edit the result.");
        }

        MatchResult result = determineMatchResult(request.getHomeScore(), request.getAwayScore());

        Match match = new Match(
                fixture,
                request.getHomeScore(),
                request.getAwayScore(),
                result,
                LocalDateTime.now()
        );
        match = matchRepository.save(match);

        fixture.setStatus(FixtureStatus.COMPLETED);
        fixture.setMatch(match);
        fixtureRepository.save(fixture);

        // Update standings for both teams
        StandingsEntry homeEntry = getOrCreateStandingsEntry(fixture.getHomeTeam().getId());
        StandingsEntry awayEntry = getOrCreateStandingsEntry(fixture.getAwayTeam().getId());

        applyMatchToStandings(homeEntry, awayEntry, request.getHomeScore(), request.getAwayScore(), result);

        standingsEntryRepository.save(homeEntry);
        standingsEntryRepository.save(awayEntry);

        return toFixtureResponse(fixture, match);
    }

    @Transactional
    public FixtureResponse updateMatchResult(Long fixtureId, MatchResultRequest request) {
        validateScores(request);

        Fixture fixture = fixtureRepository.findById(fixtureId)
                .orElseThrow(() -> new ResourceNotFoundException("Fixture not found with ID: " + fixtureId));

        Match match = matchRepository.findByFixtureId(fixtureId)
                .orElseThrow(() -> new ResourceNotFoundException("No existing match result found for fixture ID: " + fixtureId));

        StandingsEntry homeEntry = getOrCreateStandingsEntry(fixture.getHomeTeam().getId());
        StandingsEntry awayEntry = getOrCreateStandingsEntry(fixture.getAwayTeam().getId());

        // 1. Reverse old match contribution
        reverseMatchFromStandings(homeEntry, awayEntry, match.getResult());

        // 2. Determine new result
        MatchResult newResult = determineMatchResult(request.getHomeScore(), request.getAwayScore());

        // 3. Update Match entity
        match.setHomeScore(request.getHomeScore());
        match.setAwayScore(request.getAwayScore());
        match.setResult(newResult);
        match.setPlayedAt(LocalDateTime.now());
        match = matchRepository.save(match);

        // 4. Apply new match contribution
        applyMatchToStandings(homeEntry, awayEntry, request.getHomeScore(), request.getAwayScore(), newResult);

        standingsEntryRepository.save(homeEntry);
        standingsEntryRepository.save(awayEntry);

        return toFixtureResponse(fixture, match);
    }

    @Transactional(readOnly = true)
    public List<Match> getAllMatches() {
        return matchRepository.findAllByOrderByPlayedAtDesc();
    }

    @Transactional(readOnly = true)
    public Match getMatchById(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with ID: " + id));
    }

    private void validateScores(MatchResultRequest request) {
        if (request == null) {
            throw new BusinessRuleException("Match result request payload cannot be null.");
        }
        if (request.getHomeScore() == null || request.getAwayScore() == null) {
            throw new BusinessRuleException("Both home score and away score are required.");
        }
        if (request.getHomeScore() < 0 || request.getAwayScore() < 0) {
            throw new BusinessRuleException("Scores cannot be negative.");
        }
    }

    private MatchResult determineMatchResult(int homeScore, int awayScore) {
        if (homeScore > awayScore) {
            return MatchResult.HOME_WIN;
        } else if (awayScore > homeScore) {
            return MatchResult.AWAY_WIN;
        } else {
            return MatchResult.DRAW;
        }
    }

    private StandingsEntry getOrCreateStandingsEntry(Long teamId) {
        return standingsEntryRepository.findByTeamId(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Standings entry not found for team ID: " + teamId));
    }

    private void applyMatchToStandings(StandingsEntry home, StandingsEntry away, int homeScore, int awayScore, MatchResult result) {
        home.setPlayed(home.getPlayed() + 1);
        away.setPlayed(away.getPlayed() + 1);

        if (result == MatchResult.HOME_WIN) {
            home.setWins(home.getWins() + 1);
            home.setPoints(home.getPoints() + scoringProperties.getWin());

            away.setLosses(away.getLosses() + 1);
            away.setPoints(away.getPoints() + scoringProperties.getLoss());
        } else if (result == MatchResult.AWAY_WIN) {
            away.setWins(away.getWins() + 1);
            away.setPoints(away.getPoints() + scoringProperties.getWin());

            home.setLosses(home.getLosses() + 1);
            home.setPoints(home.getPoints() + scoringProperties.getLoss());
        } else {
            home.setDraws(home.getDraws() + 1);
            home.setPoints(home.getPoints() + scoringProperties.getDraw());

            away.setDraws(away.getDraws() + 1);
            away.setPoints(away.getPoints() + scoringProperties.getDraw());
        }
    }

    private void reverseMatchFromStandings(StandingsEntry home, StandingsEntry away, MatchResult oldResult) {
        home.setPlayed(Math.max(0, home.getPlayed() - 1));
        away.setPlayed(Math.max(0, away.getPlayed() - 1));

        if (oldResult == MatchResult.HOME_WIN) {
            home.setWins(Math.max(0, home.getWins() - 1));
            home.setPoints(home.getPoints() - scoringProperties.getWin());

            away.setLosses(Math.max(0, away.getLosses() - 1));
            away.setPoints(away.getPoints() - scoringProperties.getLoss());
        } else if (oldResult == MatchResult.AWAY_WIN) {
            away.setWins(Math.max(0, away.getWins() - 1));
            away.setPoints(away.getPoints() - scoringProperties.getWin());

            home.setLosses(Math.max(0, home.getLosses() - 1));
            home.setPoints(home.getPoints() - scoringProperties.getLoss());
        } else if (oldResult == MatchResult.DRAW) {
            home.setDraws(Math.max(0, home.getDraws() - 1));
            home.setPoints(home.getPoints() - scoringProperties.getDraw());

            away.setDraws(Math.max(0, away.getDraws() - 1));
            away.setPoints(away.getPoints() - scoringProperties.getDraw());
        }
    }

    private FixtureResponse toFixtureResponse(Fixture fixture, Match match) {
        Integer homeScore = match != null ? match.getHomeScore() : null;
        Integer awayScore = match != null ? match.getAwayScore() : null;
        MatchResult result = match != null ? match.getResult() : null;
        LocalDateTime playedAt = match != null ? match.getPlayedAt() : null;

        return new FixtureResponse(
                fixture.getId(),
                fixture.getHomeTeam().getId(),
                fixture.getHomeTeam().getName(),
                fixture.getAwayTeam().getId(),
                fixture.getAwayTeam().getName(),
                fixture.getRoundNumber(),
                fixture.getScheduledDate(),
                fixture.getStatus(),
                homeScore,
                awayScore,
                result,
                playedAt
        );
    }
}
