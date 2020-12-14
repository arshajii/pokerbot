package me.ars.pokerbot.stats;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Roster {
    private static final String ROSTER_FILE = "roster.txt";
    private final Map<String, Stats> roster;

    private Roster() {
        roster = new HashMap<>();
    }

    public static Roster getRoster() throws IOException {
        Roster roster = new Roster();
        final File rosterFile = new File(ROSTER_FILE);
        if (!rosterFile.exists()) {
            rosterFile.createNewFile();
        } else {
            try (ObjectInputStream stream = new ObjectInputStream(new FileInputStream(rosterFile))) {
                final Map<String, Stats> input = (Map<String, Stats>)stream.readObject();
                roster.roster.putAll(input);
            } catch (ClassNotFoundException e) {
                throw new IOException("Invalid stats file", e);
            }
        }
        return roster;
    }

    public void saveRoster() throws IOException {
        final File rosterFile = new File(ROSTER_FILE);
        if (!rosterFile.exists()) {
            rosterFile.createNewFile();
        }
        try (ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(rosterFile))) {
            stream.writeObject(roster);
        }
    }

    public void modifyMoney(String nickname, int newMoney) {
        System.out.println("ROSTER: Modifying number for " + nickname + ": $" + newMoney);
        final Stats stats;
        if (!roster.containsKey(nickname)) {
            stats = new Stats();
            stats.setNickname(nickname);
            roster.put(nickname, stats);
        } else {
            stats = roster.get(nickname);
        }
        int oldMoney = stats.getMoney();
        stats.setMoney(oldMoney + newMoney);
    }

    public void trackGame(String nickname) {
        final Stats stats;
        if (!roster.containsKey(nickname)) {
            stats = new Stats();
            stats.setNickname(nickname);
            roster.put(nickname, stats);
        } else {
            stats = roster.get(nickname);
        }
        stats.incrementGames();
    }

    public Stats getStats(String nickname) {
        return roster.get(nickname);
    }
}
