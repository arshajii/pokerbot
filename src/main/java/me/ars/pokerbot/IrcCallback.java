package me.ars.pokerbot;

public interface IrcCallback {
    void messageChannel(String message);
    void messagePlayer(Player player, String message);
}
