package com.leaguetrack.controller;

import com.leaguetrack.dto.FixtureResponse;
import com.leaguetrack.dto.MatchResultRequest;
import com.leaguetrack.model.Match;
import com.leaguetrack.service.MatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping("/{fixtureId}/result")
    public ResponseEntity<FixtureResponse> recordMatchResult(
            @PathVariable Long fixtureId,
            @Valid @RequestBody MatchResultRequest request) {
        FixtureResponse response = matchService.recordMatchResult(fixtureId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{fixtureId}/result")
    public ResponseEntity<FixtureResponse> updateMatchResult(
            @PathVariable Long fixtureId,
            @Valid @RequestBody MatchResultRequest request) {
        FixtureResponse response = matchService.updateMatchResult(fixtureId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<Match>> getAllMatches() {
        return ResponseEntity.ok(matchService.getAllMatches());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Match> getMatchById(@PathVariable Long id) {
        return ResponseEntity.ok(matchService.getMatchById(id));
    }
}
