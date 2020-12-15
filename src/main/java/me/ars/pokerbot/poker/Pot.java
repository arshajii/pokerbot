package me.ars.pokerbot.poker;

import java.util.*;

public class Pot {

    private final Map<Player, Integer> contributions;
    private final boolean isMainPot;
    private Pot sidePot;
    private int currentBet;

    public Pot() {
        contributions = new HashMap<>();
        currentBet = 0;
        sidePot = null;
        isMainPot = true;
    }

    private Pot(int currentBet) {
        this.contributions = new HashMap<>();
        this.currentBet = currentBet;
        this.sidePot = null;
        this.isMainPot = false;
    }

    private void addContribution(Player player, int money) {
        if (!contributions.containsKey(player)) {
            contributions.put(player, money);
        } else {
            int oldmoney = contributions.get(player);
            contributions.replace(player, oldmoney + money);
        }
    }

    private int getContribution(Player player) {
        return contributions.getOrDefault(player, 0);
    }

    public int getTotalMoney() {
        int money = getMoney();
        if (sidePot != null) {
            money += sidePot.getTotalMoney();
        }
        return money;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public int getTotalBets() {
        int bet = currentBet;
        if (sidePot != null) {
            bet += sidePot.getTotalBets();
        }
        return bet;
    }

    public int getTotalContribution(Player player) {
        int contribution = 0;
        if (contributions.containsKey(player)) {
            contribution += contributions.get(player);
        }
        if (sidePot != null) {
            contribution += sidePot.getTotalContribution(player);
        }
        return contribution;
    }

    public int getTotalOwed(Player player) {
        final int totalBet = getTotalBets();
        final int contributions = getTotalContribution(player);
        return totalBet - contributions;
    }

    public void newTurn() {
        //currentBet = 0;
        if (sidePot != null) {
            sidePot.newTurn();
        }
    }

    public void checkPlayer(Player player) {
        if (!contributions.containsKey(player)) {
            contributions.put(player, 0);
        }
        //todo
    }

    public void reset() {
        contributions.clear();
        currentBet = 0;
        sidePot = null;
    }

    public void collectAnte(Player player, int ante) {
        System.out.println("Collecting ante from " + player + ", total paid: " + player.getAmountPayed());
        currentBet = ante;
        addContribution(player, player.bet(ante));
    }

    public Set<Player> getParticipants() {
        return contributions.keySet();
    }

    public int getMoney() {
        int cash = 0;
        for(Player player: contributions.keySet()) {
            cash += contributions.get(player);
        }
        return cash;
    }

    public boolean hasSidePot() {
        return sidePot != null;
    }

    private void setBet(int bet) {
        currentBet = bet;
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
            addContribution(player, amount);
        } else {
            System.out.println(player + " raised, but its going into a sidepot");
            sidePot.raise(player, amount);
        }
    }

    public void call(Player player, int amount) {
        final int a = getContribution(player);
        System.out.println(player + " is putting " + amount + " into pot [ current contribution: " + a + ", current bet: " + currentBet + "]");
        if (getContribution(player) == currentBet) {
            // Player has already satisfied this pot
            if (sidePot != null) {
                System.out.println(player + " is channeling " + amount + " into a side pot.");
                sidePot.call(player, amount);
                return;
            } else {
                throw new IllegalStateException(player + " is trying to shove in " + amount + " but this pot is already satisfied and there is no side pot");
            }
        }
        if (amount > currentBet) {
            if (sidePot == null) {
                throw new IllegalStateException(player + " called in excess and there is no sidepot. (Tried to put " + amount + " into " + currentBet);
            }
            if (getContribution(player) == currentBet) {
                System.out.println(player + " is funneling " + amount + " into a sidepot");
                sidePot.call(player, amount);
                return;
            } else {
                final int needed = currentBet - getContribution(player);
                if (needed > 0) {
                    System.err.println("Needed to put " + needed + " in this pot, current bet is " + currentBet);
                    call(player, needed);
                    sidePot.call(player, amount - needed);
                } else {
                    System.out.println(player + " is shoving " + amount + " into a sidepot");
                    sidePot.call(player, amount);
                }
                return;
            }
        }
        final int contribution = player.bet(amount);
        addContribution(player, contribution);
        if (getContribution(player) == currentBet) {
            return;
        } else if (getContribution(player) < currentBet){
            final int difference = currentBet - amount;
            sidePot = new Pot();
            for (Player callingPlayer : getParticipants()) {
                if (!callingPlayer.equals(player)) {
                    addContribution(callingPlayer, -difference);
                    sidePot.addContribution(callingPlayer, difference);
                }
            }
            sidePot.setBet(difference);
            System.out.println("A new sidepot was formed: " + sidePot.toString());
            currentBet -= difference;
        } else {
            throw new IllegalStateException("This case should have been caught earlier in this method.");
        }
    }

    @Override
    public String toString() {
        return "Pot{" +
                "contributions=" + contributions +
                ", isMainPot=" + isMainPot +
                ", sidePot=" + sidePot +
                ", currentBet=" + currentBet +
                '}';
    }
}
