package com.leaguetrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.leaguetrack.config.ScoringProperties;

@SpringBootApplication
@EnableConfigurationProperties(ScoringProperties.class)
public class LeagueTrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeagueTrackApplication.class, args);
    }
}
