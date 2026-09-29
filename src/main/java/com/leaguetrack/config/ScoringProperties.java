package com.leaguetrack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "league.scoring")
public class ScoringProperties {

    private int win = 3;
    private int draw = 1;
    private int loss = 0;

    public ScoringProperties() {
    }

    public ScoringProperties(int win, int draw, int loss) {
        this.win = win;
        this.draw = draw;
        this.loss = loss;
    }

    public int getWin() {
        return win;
    }

    public void setWin(int win) {
        this.win = win;
    }

    public int getDraw() {
        return draw;
    }

    public void setDraw(int draw) {
        this.draw = draw;
    }

    public int getLoss() {
        return loss;
    }

    public void setLoss(int loss) {
        this.loss = loss;
    }
}
