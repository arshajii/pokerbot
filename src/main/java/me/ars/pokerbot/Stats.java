package me.ars.pokerbot;

import java.io.Serializable;
import java.util.Objects;

public class Stats implements Serializable {
    private String nickname;
    private int wins;
    private int games;

    public String getNickname() {
        return nickname;
    }

    @Override
    public String toString() {
        return  "Player " + nickname +
                ", wins: " + wins +
                ", games played: " + games;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stats stats = (Stats) o;
        return nickname.equals(stats.nickname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nickname);
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void incrementWins() {
        wins++;
    }

    public void incrementGames() {
        games++;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getGames() {
        return games;
    }

    public void setGames(int games) {
        this.games = games;
    }
}
