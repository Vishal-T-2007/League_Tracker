package com.leaguetrack.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebViewController {

    @GetMapping({"/", "/dashboard"})
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/teams")
    public String teams() {
        return "teams";
    }

    @GetMapping("/fixtures")
    public String fixtures() {
        return "fixtures";
    }

    @GetMapping("/standings")
    public String standings() {
        return "standings";
    }
}
