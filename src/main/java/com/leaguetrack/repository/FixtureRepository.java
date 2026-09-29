package com.leaguetrack.repository;

import com.leaguetrack.enums.FixtureStatus;
import com.leaguetrack.model.Fixture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FixtureRepository extends JpaRepository<Fixture, Long> {

    List<Fixture> findByHomeTeamIdOrAwayTeamId(Long homeTeamId, Long awayTeamId);

    List<Fixture> findByHomeTeamId(Long homeTeamId);

    List<Fixture> findByAwayTeamId(Long awayTeamId);

    List<Fixture> findAllByOrderByRoundNumberAscIdAsc();

    long countByStatus(FixtureStatus status);
}
