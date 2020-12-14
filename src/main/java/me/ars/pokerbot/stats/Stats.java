package me.ars.pokerbot.stats;

import java.io.Serializable;
import java.util.Objects;

public class Stats implements Serializable {
    private String nickname;
    private int games;
    private int money;

    public String getNickname() {
        return nickname;
    }

    @Override
    public String toString() {
        return  "Player " + nickname +
                ", winnings: " + money +
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

    public void setMoney(int money) {
        this.money = money;
    }

    public int getMoney() {
        return money;
    }

    public void incrementGames() {
        games++;
    }

    public int getGames() {
        return games;
    }

    public void setGames(int games) {
        this.games = games;
    }
}
