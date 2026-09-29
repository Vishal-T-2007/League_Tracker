package com.leaguetrack.model;

import com.leaguetrack.enums.MatchResult;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "fixture_id", nullable = false, unique = true)
    private Fixture fixture;

    @Column(name = "home_score", nullable = false)
    private Integer homeScore;

    @Column(name = "away_score", nullable = false)
    private Integer awayScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private MatchResult result;

    @Column(name = "played_at", nullable = false)
    private LocalDateTime playedAt;

    public Match() {
    }

    public Match(Fixture fixture, Integer homeScore, Integer awayScore, MatchResult result, LocalDateTime playedAt) {
        this.fixture = fixture;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.result = result;
        this.playedAt = playedAt;
    }

    @PrePersist
    protected void onPlay() {
        if (this.playedAt == null) {
            this.playedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Fixture getFixture() {
        return fixture;
    }

    public void setFixture(Fixture fixture) {
        this.fixture = fixture;
    }

    public Integer getHomeScore() {
        return homeScore;
    }

    public void setHomeScore(Integer homeScore) {
        this.homeScore = homeScore;
    }

    public Integer getAwayScore() {
        return awayScore;
    }

    public void setAwayScore(Integer awayScore) {
        this.awayScore = awayScore;
    }

    public MatchResult getResult() {
        return result;
    }

    public void setResult(MatchResult result) {
        this.result = result;
    }

    public LocalDateTime getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(LocalDateTime playedAt) {
        this.playedAt = playedAt;
    }
}
