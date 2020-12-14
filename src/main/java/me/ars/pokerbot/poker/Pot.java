package me.ars.pokerbot.poker;

import java.util.ArrayList;
import java.util.List;

public class Pot {

    private final List<Player> players;
    private final boolean isMainPot;
    private boolean hasSidePot;
    private int size;
    private int currentBet;

    public Pot() {
        players = new ArrayList<>();
        size = 0;
        currentBet = 0;
        hasSidePot = false;
        isMainPot = true;
    }

    private Pot(List<Player> players, int size, int currentBet) {
        this.players = players;
        this.size = size;
        this.currentBet = currentBet;
        this.hasSidePot = false;
        this.isMainPot = false;
    }

    private void addPlayer(Player player) {
        if (!players.contains(player)) {
            players.add(player);
        }
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public void newTurn() {
        currentBet = 0;
        players.clear();
    }

    public void reset() {
        size = 0;
        currentBet = 0;
        hasSidePot = false;
        players.clear();
    }

    public void collectAnte(Player player, int ante) {
        size += ante;
        addPlayer(player);
    }

    public List<Player> getParticipants() {
        return players;
    }

    public int getMoney() {
        return size;
    }

    public boolean hasSidePot() {
        return hasSidePot;
    }

    public boolean isMainPot() {
        return isMainPot;
    }

    public void raise(Player player, int amount) {
        currentBet += amount;
        size += amount;
        addPlayer(player);
    }

    /**
     * Returns a new Pot (sidepot) if one needs to be created
     */
    public Pot call(Player player, int amount) {
        if (amount > currentBet) {
            raise(player, amount);
            return null;
        }
        addPlayer(player);
        size += amount;
        if (amount == currentBet) {
            return null;
        } else {
            final int difference = currentBet - amount;
            final List<Player> sidepotPlayers = new ArrayList<>();
            int sidePot = 0;
            for (Player callingPlayer : players) {
                if (callingPlayer.equals(player)) continue;
                sidePot += difference;
                size -= difference;
                sidepotPlayers.add(callingPlayer);
            }
            hasSidePot = true;
            currentBet -= difference;
            return new Pot(sidepotPlayers, sidePot, difference);
        }
    }
}
