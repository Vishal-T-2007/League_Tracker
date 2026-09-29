package com.leaguetrack.controller;

import com.leaguetrack.dto.DashboardStatsResponse;
import com.leaguetrack.dto.StandingsResponse;
import com.leaguetrack.service.StandingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class StandingsController {

    private final StandingsService standingsService;

    public StandingsController(StandingsService standingsService) {
        this.standingsService = standingsService;
    }

    @GetMapping("/standings")
    public ResponseEntity<List<StandingsResponse>> getStandings() {
        return ResponseEntity.ok(standingsService.getStandings());
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(standingsService.getDashboardStats());
    }
}
