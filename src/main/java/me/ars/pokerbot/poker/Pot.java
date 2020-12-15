package me.ars.pokerbot.poker;

import java.util.ArrayList;
import java.util.List;

public class Pot {

    private final List<Player> players;
    private final boolean isMainPot;
    private Pot sidePot;
    private int size;
    private int currentBet;

    public Pot() {
        players = new ArrayList<>();
        size = 0;
        currentBet = 0;
        sidePot = null;
        isMainPot = true;
    }

    private Pot(List<Player> players, int size, int currentBet) {
        this.players = players;
        this.size = size;
        this.currentBet = currentBet;
        this.sidePot = null;
        this.isMainPot = false;
    }

    private void addPlayer(Player player) {
        if (!players.contains(player)) {
            players.add(player);
        }
    }

    public int getTotalMoney() {
        int money = size;
        if (sidePot != null) {
            money += sidePot.getTotalMoney();
        }
        return money;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public void newTurn() {
        currentBet = 0;
        if (sidePot != null) {
            sidePot.newTurn();
        } else {
            players.clear();
        }
    }

    public void checkPlayer(Player player) {
        addPlayer(player);
    }

    public void reset() {
        size = 0;
        currentBet = 0;
        sidePot = null;
        players.clear();
    }

    public void collectAnte(Player player, int ante) {
        size += player.bet(ante);
        System.out.println("Collecting ante from " + player + ", total paid: " + player.getAmountPayed());
        currentBet = ante;
        addPlayer(player);
    }

    public List<Player> getParticipants() {
        return players;
    }

    public int getMoney() {
        return size;
    }

    public boolean hasSidePot() {
        return sidePot != null;
    }

    public boolean isMainPot() {
        return isMainPot;
    }

    public Pot getSidePot() {
        return sidePot;
    }

    public void raise(Player player, int amount) {
        if (sidePot == null) {
            System.out.println(player + " has raised by " + amount);
            currentBet += player.bet(amount);
            size += amount;
            addPlayer(player);
        } else {
            System.out.println(player + " raised, but its going into a sidepot");
            sidePot.raise(player, amount);
        }
    }

    /**
     * Returns a new Pot (sidepot) if one needs to be created
     */
    public void call(Player player, int amount) {
        if (players.contains(player)) {
            
        }
        if (amount > currentBet) {
            throw new IllegalStateException("Trying to call higher than the bet");
        }
        addPlayer(player);
        size += player.bet(amount);
        if (player.getAmountPayed() == currentBet) {
            return;
        } else {
            final int difference = currentBet - amount;
            final List<Player> sidepotPlayers = new ArrayList<>();
            int sidePotAmount = 0;
            for (Player callingPlayer : players) {
                if (callingPlayer.equals(player)) continue;
                sidePotAmount += difference;
                size -= difference;
                sidepotPlayers.add(callingPlayer);
            }
            currentBet -= difference;
            sidePot = new Pot(sidepotPlayers, sidePotAmount, difference);
        }
    }
}
