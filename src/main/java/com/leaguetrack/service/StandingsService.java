package com.leaguetrack.service;

import com.leaguetrack.config.ScoringProperties;
import com.leaguetrack.dto.DashboardStatsResponse;
import com.leaguetrack.dto.StandingsResponse;
import com.leaguetrack.enums.FixtureStatus;
import com.leaguetrack.model.Match;
import com.leaguetrack.model.StandingsEntry;
import com.leaguetrack.model.Team;
import com.leaguetrack.repository.FixtureRepository;
import com.leaguetrack.repository.MatchRepository;
import com.leaguetrack.repository.StandingsEntryRepository;
import com.leaguetrack.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StandingsService {

    private final StandingsEntryRepository standingsEntryRepository;
    private final TeamRepository teamRepository;
    private final FixtureRepository fixtureRepository;
    private final MatchRepository matchRepository;
    private final ScoringProperties scoringProperties;

    public StandingsService(StandingsEntryRepository standingsEntryRepository,
                            TeamRepository teamRepository,
                            FixtureRepository fixtureRepository,
                            MatchRepository matchRepository,
                            ScoringProperties scoringProperties) {
        this.standingsEntryRepository = standingsEntryRepository;
        this.teamRepository = teamRepository;
        this.fixtureRepository = fixtureRepository;
        this.matchRepository = matchRepository;
        this.scoringProperties = scoringProperties;
    }

    @Transactional(readOnly = true)
    public List<StandingsResponse> getStandings() {
        List<StandingsEntry> entries = standingsEntryRepository.findAllOrderedForStandings();
        List<StandingsResponse> responses = new ArrayList<>();
        int position = 1;
        for (StandingsEntry entry : entries) {
            responses.add(new StandingsResponse(
                    position++,
                    entry.getTeam().getId(),
                    entry.getTeam().getName(),
                    entry.getPlayed(),
                    entry.getWins(),
                    entry.getDraws(),
                    entry.getLosses(),
                    entry.getPoints()
            ));
        }
        return responses;
    }

    @Transactional
    public void recalculateStandings() {
        List<Team> teams = teamRepository.findAll();
        Map<Long, StandingsEntry> entryMap = standingsEntryRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getTeam().getId(), Function.identity()));

        // Reset or initialize standings entries for all existing teams
        for (Team team : teams) {
            StandingsEntry entry = entryMap.get(team.getId());
            if (entry == null) {
                entry = new StandingsEntry(team);
                entryMap.put(team.getId(), entry);
            } else {
                entry.reset();
            }
        }

        // Process all recorded match results
        List<Match> matches = matchRepository.findAll();
        for (Match match : matches) {
            if (match.getFixture() == null || match.getFixture().getHomeTeam() == null || match.getFixture().getAwayTeam() == null) {
                continue;
            }

            Long homeTeamId = match.getFixture().getHomeTeam().getId();
            Long awayTeamId = match.getFixture().getAwayTeam().getId();

            StandingsEntry homeEntry = entryMap.get(homeTeamId);
            StandingsEntry awayEntry = entryMap.get(awayTeamId);

            if (homeEntry == null || awayEntry == null) {
                continue;
            }

            int homeScore = match.getHomeScore();
            int awayScore = match.getAwayScore();

            homeEntry.setPlayed(homeEntry.getPlayed() + 1);
            awayEntry.setPlayed(awayEntry.getPlayed() + 1);

            if (homeScore > awayScore) {
                homeEntry.setWins(homeEntry.getWins() + 1);
                homeEntry.setPoints(homeEntry.getPoints() + scoringProperties.getWin());

                awayEntry.setLosses(awayEntry.getLosses() + 1);
                awayEntry.setPoints(awayEntry.getPoints() + scoringProperties.getLoss());
            } else if (awayScore > homeScore) {
                awayEntry.setWins(awayEntry.getWins() + 1);
                awayEntry.setPoints(awayEntry.getPoints() + scoringProperties.getWin());

                homeEntry.setLosses(homeEntry.getLosses() + 1);
                homeEntry.setPoints(homeEntry.getPoints() + scoringProperties.getLoss());
            } else {
                homeEntry.setDraws(homeEntry.getDraws() + 1);
                homeEntry.setPoints(homeEntry.getPoints() + scoringProperties.getDraw());

                awayEntry.setDraws(awayEntry.getDraws() + 1);
                awayEntry.setPoints(awayEntry.getPoints() + scoringProperties.getDraw());
            }
        }

        standingsEntryRepository.saveAll(entryMap.values());
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        long totalTeams = teamRepository.count();
        long totalFixtures = fixtureRepository.count();
        long completedMatches = fixtureRepository.countByStatus(FixtureStatus.COMPLETED);

        String currentLeader = "No leader yet";
        if (totalTeams > 0) {
            List<StandingsEntry> ordered = standingsEntryRepository.findAllOrderedForStandings();
            if (!ordered.isEmpty()) {
                currentLeader = ordered.get(0).getTeam().getName();
            }
        }

        return new DashboardStatsResponse(totalTeams, totalFixtures, completedMatches, currentLeader);
    }
}
