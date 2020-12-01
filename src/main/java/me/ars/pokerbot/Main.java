package me.ars.pokerbot;

import java.io.IOException;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.TrustingSSLSocketFactory;

public class Main {
	public static void main(String[] args) {
		IrcBot bot = new IrcBot(Constants.GAME_CHANNEL);
		bot.setVerbose(Constants.VERBOSE);

		try {
			if (Constants.SERVER_PASS == null) {
				bot.connect(Constants.HOST, Constants.PORT, new TrustingSSLSocketFactory());
			} else {
				bot.connect(Constants.HOST, Constants.PORT,
						Constants.SERVER_PASS, new TrustingSSLSocketFactory());
			}
			bot.joinGameChannel(Constants.CHANNEL_KEY);
		} catch (IrcException | IOException e) {
			e.printStackTrace();
		}
	}
}
