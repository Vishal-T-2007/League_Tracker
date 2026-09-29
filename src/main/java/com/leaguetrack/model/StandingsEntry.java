package com.leaguetrack.model;

import jakarta.persistence.*;

@Entity
@Table(name = "standings_entries")
public class StandingsEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "team_id", nullable = false, unique = true)
    private Team team;

    @Column(name = "played", nullable = false)
    private int played = 0;

    @Column(name = "wins", nullable = false)
    private int wins = 0;

    @Column(name = "draws", nullable = false)
    private int draws = 0;

    @Column(name = "losses", nullable = false)
    private int losses = 0;

    @Column(name = "points", nullable = false)
    private int points = 0;

    public StandingsEntry() {
    }

    public StandingsEntry(Team team) {
        this.team = team;
        this.played = 0;
        this.wins = 0;
        this.draws = 0;
        this.losses = 0;
        this.points = 0;
    }

    public StandingsEntry(Team team, int played, int wins, int draws, int losses, int points) {
        this.team = team;
        this.played = played;
        this.wins = wins;
        this.draws = draws;
        this.losses = losses;
        this.points = points;
    }

    public void reset() {
        this.played = 0;
        this.wins = 0;
        this.draws = 0;
        this.losses = 0;
        this.points = 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public int getPlayed() {
        return played;
    }

    public void setPlayed(int played) {
        this.played = played;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getDraws() {
        return draws;
    }

    public void setDraws(int draws) {
        this.draws = draws;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }
}
