package com.leaguetrack.repository;

import com.leaguetrack.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    Optional<Match> findByFixtureId(Long fixtureId);

    boolean existsByFixtureId(Long fixtureId);

    void deleteByFixtureId(Long fixtureId);

    List<Match> findAllByOrderByPlayedAtDesc();
}
