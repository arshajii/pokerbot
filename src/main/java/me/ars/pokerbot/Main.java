package me.ars.pokerbot;

import java.io.IOException;

import org.jibble.pircbot.IrcException;

public class Main {
	public static void main(String[] args) {
		IrcBot bot = new IrcBot(Constants.GAME_CHANNEL);
		bot.setVerbose(Constants.VERBOSE);

		try {
			if (Constants.SERVER_PASS == null) {
				bot.connect(Constants.HOST, Constants.PORT);
			} else {
				bot.connect(Constants.HOST, Constants.PORT,
						Constants.SERVER_PASS);
			}
			bot.joinGameChannel(Constants.CHANNEL_KEY);
		} catch (IrcException | IOException e) {
			e.printStackTrace();
		}
	}
}
