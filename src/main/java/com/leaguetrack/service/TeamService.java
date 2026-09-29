package com.leaguetrack.service;

import com.leaguetrack.dto.TeamRequest;
import com.leaguetrack.dto.TeamResponse;
import com.leaguetrack.exception.BusinessRuleException;
import com.leaguetrack.exception.ResourceNotFoundException;
import com.leaguetrack.model.Fixture;
import com.leaguetrack.model.StandingsEntry;
import com.leaguetrack.model.Team;
import com.leaguetrack.repository.FixtureRepository;
import com.leaguetrack.repository.MatchRepository;
import com.leaguetrack.repository.StandingsEntryRepository;
import com.leaguetrack.repository.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final StandingsEntryRepository standingsEntryRepository;
    private final FixtureRepository fixtureRepository;
    private final MatchRepository matchRepository;
    private final StandingsService standingsService;

    public TeamService(TeamRepository teamRepository,
                       StandingsEntryRepository standingsEntryRepository,
                       FixtureRepository fixtureRepository,
                       MatchRepository matchRepository,
                       StandingsService standingsService) {
        this.teamRepository = teamRepository;
        this.standingsEntryRepository = standingsEntryRepository;
        this.fixtureRepository = fixtureRepository;
        this.matchRepository = matchRepository;
        this.standingsService = standingsService;
    }

    @Transactional
    public TeamResponse registerTeam(TeamRequest request) {
        String trimmedName = request.getName() != null ? request.getName().trim() : "";
        if (trimmedName.isEmpty()) {
            throw new BusinessRuleException("Team name cannot be blank.");
        }

        if (teamRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new BusinessRuleException("A team with this name already exists.", HttpStatus.CONFLICT);
        }

        Team team = new Team(trimmedName);
        Team savedTeam = teamRepository.save(team);

        StandingsEntry standingsEntry = new StandingsEntry(savedTeam);
        standingsEntryRepository.save(standingsEntry);

        return toResponse(savedTeam);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getAllTeams() {
        return teamRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeamById(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with ID: " + id));
        return toResponse(team);
    }

    @Transactional
    public TeamResponse updateTeam(Long id, TeamRequest request) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with ID: " + id));

        String trimmedName = request.getName() != null ? request.getName().trim() : "";
        if (trimmedName.isEmpty()) {
            throw new BusinessRuleException("Team name cannot be blank.");
        }

        if (teamRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new BusinessRuleException("A team with this name already exists.", HttpStatus.CONFLICT);
        }

        team.setName(trimmedName);
        Team updated = teamRepository.save(team);
        return toResponse(updated);
    }

    @Transactional
    public void deleteTeam(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with ID: " + id));

        // 1. Find all fixtures involving this team (home or away)
        List<Fixture> relatedFixtures = fixtureRepository.findByHomeTeamIdOrAwayTeamId(id, id);

        // 2. Delete matches associated with those fixtures
        for (Fixture fixture : relatedFixtures) {
            matchRepository.deleteByFixtureId(fixture.getId());
        }

        // 3. Delete related fixtures
        fixtureRepository.deleteAll(relatedFixtures);

        // 4. Delete the team's standings entry
        standingsEntryRepository.deleteByTeamId(id);

        // 5. Delete the team
        teamRepository.delete(team);

        // 6. Recalculate standings from remaining valid match records
        standingsService.recalculateStandings();
    }

    private TeamResponse toResponse(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getCreatedAt());
    }
}
