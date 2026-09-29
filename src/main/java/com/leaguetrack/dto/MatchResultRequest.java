package com.leaguetrack.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class MatchResultRequest {

    @NotNull(message = "Home score is required")
    @PositiveOrZero(message = "Home score cannot be negative")
    private Integer homeScore;

    @NotNull(message = "Away score is required")
    @PositiveOrZero(message = "Away score cannot be negative")
    private Integer awayScore;

    public MatchResultRequest() {
    }

    public MatchResultRequest(Integer homeScore, Integer awayScore) {
        this.homeScore = homeScore;
        this.awayScore = awayScore;
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
}
