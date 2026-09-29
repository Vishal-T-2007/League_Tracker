package com.leaguetrack.dto;

public class DashboardStatsResponse {

    private long totalTeams;
    private long totalFixtures;
    private long completedMatches;
    private String currentLeader;

    public DashboardStatsResponse() {
    }

    public DashboardStatsResponse(long totalTeams, long totalFixtures, long completedMatches, String currentLeader) {
        this.totalTeams = totalTeams;
        this.totalFixtures = totalFixtures;
        this.completedMatches = completedMatches;
        this.currentLeader = currentLeader;
    }

    public long getTotalTeams() {
        return totalTeams;
    }

    public void setTotalTeams(long totalTeams) {
        this.totalTeams = totalTeams;
    }

    public long getTotalFixtures() {
        return totalFixtures;
    }

    public void setTotalFixtures(long totalFixtures) {
        this.totalFixtures = totalFixtures;
    }

    public long getCompletedMatches() {
        return completedMatches;
    }

    public void setCompletedMatches(long completedMatches) {
        this.completedMatches = completedMatches;
    }

    public String getCurrentLeader() {
        return currentLeader;
    }

    public void setCurrentLeader(String currentLeader) {
        this.currentLeader = currentLeader;
    }
}
