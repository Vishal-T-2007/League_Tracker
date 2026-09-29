package com.leaguetrack.repository;

import com.leaguetrack.model.StandingsEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StandingsEntryRepository extends JpaRepository<StandingsEntry, Long> {

    Optional<StandingsEntry> findByTeamId(Long teamId);

    void deleteByTeamId(Long teamId);

    @Query("SELECT s FROM StandingsEntry s JOIN FETCH s.team ORDER BY s.points DESC, s.wins DESC, s.team.name ASC")
    List<StandingsEntry> findAllOrderedForStandings();
}
