package com.leaguetrack.controller;

import com.leaguetrack.dto.FixtureResponse;
import com.leaguetrack.service.FixtureService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fixtures")
public class FixtureController {

    private final FixtureService fixtureService;

    public FixtureController(FixtureService fixtureService) {
        this.fixtureService = fixtureService;
    }

    @PostMapping("/generate")
    public ResponseEntity<List<FixtureResponse>> generateFixtures() {
        List<FixtureResponse> fixtures = fixtureService.generateRoundRobinFixtures();
        return new ResponseEntity<>(fixtures, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<FixtureResponse>> getAllFixtures() {
        return ResponseEntity.ok(fixtureService.getAllFixtures());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FixtureResponse> getFixtureById(@PathVariable Long id) {
        return ResponseEntity.ok(fixtureService.getFixtureById(id));
    }

    @DeleteMapping
    public ResponseEntity<Void> resetFixtures() {
        fixtureService.resetFixtures();
        return ResponseEntity.noContent().build();
    }
}
