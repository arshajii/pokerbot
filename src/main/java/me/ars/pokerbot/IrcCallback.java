package me.ars.pokerbot;

public interface IrcCallback {
    void messageChannel(String channel, String message);
    void messagePlayer(Player player, String message);
}
