package me.ars.pokerbot;

import java.io.IOException;

import org.jibble.pircbot.IrcException;

public class Main {
	public static void main(String[] args) {
		PokerBot bot = new PokerBot(Constants.GAME_CHANNEL);
		bot.setVerbose(Constants.VERBOSE);

		try {
			bot.connect(Constants.HOST);
			bot.joinGameChannel(Constants.CHANNEL_KEY);
		} catch (IrcException | IOException e) {
			e.printStackTrace();
		}
	}
}
