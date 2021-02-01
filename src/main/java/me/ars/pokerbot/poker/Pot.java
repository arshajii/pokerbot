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

    public boolean checkPlayer(Player player) {
        if (player.isAllIn()) {
            return true;
        }
        if (!contributions.containsKey(player)) {
            contributions.put(player, 0);
        }
        return (getTotalOwed(player) == 0);
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

    public int raise(Player player, int amount) {
        final int totalRaised;
        final int owed;

        if (player.isAllIn()) {
            owed = call(player);
            totalRaised = Math.min(player.getMoney(), amount - owed);
            if (totalRaised < 1) {
                // Not enough money to raise the specified amount, we stop at the call() above.
                return 0;
            }
        } else {
            totalRaised = Math.min(player.getMoney(), amount);
            owed = getTotalOwed(player);
            if (owed > totalRaised + player.getMoney()) {
                // Cannot raise, not enough money. Let player reconsider raise.
                return -1;
            }
        }
        if (sidePot == null) {
            System.out.println(player + " has raised by " + totalRaised);
            currentBet += player.bet(totalRaised);
            addContribution(player, totalRaised);
        } else {
            System.out.println(player + " raised, but its going into a sidepot");
            sidePot.raise(player, totalRaised);
        }
        return totalRaised;
    }

    public void allIn(Player player) {
        player.setAllIn(true);
        raise(player, player.getMoney());
    }

    public int call(Player player) {
        final int previousContribution = getTotalContribution(player);
        final int owed = getTotalOwed(player);
        final int callAmount = Math.min(owed, player.getMoney());
        call(player, callAmount);
        if (player.getMoney() == 0) {
            player.setAllIn(true);
        }
        return getTotalContribution(player) - previousContribution;
    }

    private void call(Player player, int amount) {
        final int currentContribution = getContribution(player);
        System.out.println(player + " is putting " + amount + " into pot [ current contribution: " + currentContribution + ", current bet: " + currentBet + "]");
        if (getContribution(player) == currentBet) {
            // Player has already satisfied this pot
            if (sidePot != null) {
                System.out.println(player + " is channeling " + amount + " into a side pot.");
                sidePot.call(player, amount);
                return;
            } else {
                // This should just be a check.
                System.err.println(player + " is trying to shove in " + amount + " but this pot is already satisfied and there is no side pot");
                if (sidePot != null) {
                    sidePot.checkPlayer(player);
                }
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
