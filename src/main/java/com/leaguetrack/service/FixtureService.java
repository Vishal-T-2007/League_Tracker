package com.leaguetrack.service;

import com.leaguetrack.dto.FixtureResponse;
import com.leaguetrack.enums.FixtureStatus;
import com.leaguetrack.exception.BusinessRuleException;
import com.leaguetrack.exception.ResourceNotFoundException;
import com.leaguetrack.model.Fixture;
import com.leaguetrack.model.Match;
import com.leaguetrack.model.Team;
import com.leaguetrack.repository.FixtureRepository;
import com.leaguetrack.repository.MatchRepository;
import com.leaguetrack.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FixtureService {

    private final FixtureRepository fixtureRepository;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final StandingsService standingsService;

    public FixtureService(FixtureRepository fixtureRepository,
                          TeamRepository teamRepository,
                          MatchRepository matchRepository,
                          StandingsService standingsService) {
        this.fixtureRepository = fixtureRepository;
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
        this.standingsService = standingsService;
    }

    @Transactional
    public List<FixtureResponse> generateRoundRobinFixtures() {
        if (fixtureRepository.count() > 0) {
            throw new BusinessRuleException("Fixtures have already been generated. Reset or clear existing fixtures to regenerate.");
        }

        List<Team> teams = teamRepository.findAll();
        if (teams.size() < 2) {
            throw new BusinessRuleException("At least 2 teams are required to generate tournament fixtures.");
        }

        List<Team> rotationList = new ArrayList<>(teams);
        boolean hasBye = false;

        // If odd number of teams, add null to represent a BYE
        if (rotationList.size() % 2 != 0) {
            rotationList.add(null);
            hasBye = true;
        }

        int totalTeamsWithBye = rotationList.size();
        int totalRounds = totalTeamsWithBye - 1;
        int matchesPerRound = totalTeamsWithBye / 2;

        List<Fixture> generatedFixtures = new ArrayList<>();
        LocalDateTime baseScheduleDate = LocalDateTime.now().plusDays(1).withHour(18).withMinute(0).withSecond(0).withNano(0);

        for (int round = 0; round < totalRounds; round++) {
            int roundNumber = round + 1;
            LocalDateTime roundDate = baseScheduleDate.plusDays(round * 2L);

            for (int matchIdx = 0; matchIdx < matchesPerRound; matchIdx++) {
                Team teamA = rotationList.get(matchIdx);
                Team teamB = rotationList.get(totalTeamsWithBye - 1 - matchIdx);

                // Skip bye pairings
                if (teamA == null || teamB == null) {
                    continue;
                }

                // Alternate home and away to maintain fair venue balance
                Team homeTeam;
                Team awayTeam;
                if (round % 2 == 0) {
                    homeTeam = (matchIdx % 2 == 0) ? teamA : teamB;
                    awayTeam = (matchIdx % 2 == 0) ? teamB : teamA;
                } else {
                    homeTeam = (matchIdx % 2 == 0) ? teamB : teamA;
                    awayTeam = (matchIdx % 2 == 0) ? teamA : teamB;
                }

                Fixture fixture = new Fixture(
                        homeTeam,
                        awayTeam,
                        roundNumber,
                        roundDate,
                        FixtureStatus.SCHEDULED
                );
                generatedFixtures.add(fixture);
            }

            // Circle algorithm rotation: keep index 0 fixed, rotate others
            Team lastTeam = rotationList.remove(rotationList.size() - 1);
            rotationList.add(1, lastTeam);
        }

        List<Fixture> savedFixtures = fixtureRepository.saveAll(generatedFixtures);
        return getAllFixtures();
    }

    @Transactional(readOnly = true)
    public List<FixtureResponse> getAllFixtures() {
        List<Fixture> fixtures = fixtureRepository.findAllByOrderByRoundNumberAscIdAsc();
        Map<Long, Match> matchMap = matchRepository.findAll().stream()
                .filter(m -> m.getFixture() != null)
                .collect(Collectors.toMap(m -> m.getFixture().getId(), Function.identity(), (a, b) -> a));

        return fixtures.stream()
                .map(f -> toResponse(f, matchMap.get(f.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FixtureResponse getFixtureById(Long id) {
        Fixture fixture = fixtureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fixture not found with ID: " + id));
        Match match = matchRepository.findByFixtureId(id).orElse(null);
        return toResponse(fixture, match);
    }

    @Transactional
    public void resetFixtures() {
        matchRepository.deleteAll();
        fixtureRepository.deleteAll();
        standingsService.recalculateStandings();
    }

    private FixtureResponse toResponse(Fixture fixture, Match match) {
        Integer homeScore = match != null ? match.getHomeScore() : null;
        Integer awayScore = match != null ? match.getAwayScore() : null;
        var result = match != null ? match.getResult() : null;
        var playedAt = match != null ? match.getPlayedAt() : null;

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
