package com.leaguetrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TeamRequest {

    @NotBlank(message = "Team name cannot be blank")
    @Size(min = 1, max = 100, message = "Team name must be between 1 and 100 characters")
    private String name;

    public TeamRequest() {
    }

    public TeamRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
