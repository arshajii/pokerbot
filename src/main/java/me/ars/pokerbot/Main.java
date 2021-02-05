package me.ars.pokerbot;

import java.io.IOException;
import java.io.File;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.TrustingSSLSocketFactory;
import com.moandjiezana.toml.Toml;

import me.ars.pokerbot.config.IrcBotConfig;

public class Main {
	public static void main(String[] args) {
		final IrcBotConfig config = new Toml().read(new File("config.toml")).to(IrcBotConfig.class);
		IrcBot bot = new IrcBot(config);
		bot.setVerbose(config.irc.verbose);

		try {
			if (config.irc.serverPassword == null) {
				bot.connect(
					config.irc.server,
					config.irc.port,
					new TrustingSSLSocketFactory()
				);
			} else {
				bot.connect(
					config.irc.server, config.irc.port,
					config.irc.serverPassword,
					new TrustingSSLSocketFactory()
				);
			}
			bot.joinGameChannel(config.irc.channel, config.irc.channelPassword);
		} catch (IrcException | IOException e) {
			e.printStackTrace();
		}
	}
}
