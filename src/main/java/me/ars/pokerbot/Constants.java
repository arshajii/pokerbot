package me.ars.pokerbot;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Constants {
	public static final String ADMIN_KEY;

	public static final String GAME_CHANNEL;
	public static final String CHANNEL_KEY;
	public static final String HOST;
	public static final int PORT;
	public static final String SERVER_PASS;

	public static final String BOT_NAME;
	public static final char CMD_PREFIX;

	public static final boolean VERBOSE;


	public static final int START_MONEY;
	public static final int ANTE;
	public static final int BIG_BLIND_AMOUNT = 5;

	public static final int FORCED_BET_ANTE = 99;
	public static final int FORCED_BET_BLINDS = 100;

	private static final String CFG_FILENAME = "parameters.cfg";

	private static final Pattern CFG_LINE = Pattern
			.compile("^\\s*(\\S+)\\s*=\\s*(\\S+)\\s*(?:#.*)?$");

	private static final Pattern CFG_COMMENT = Pattern.compile("^\\s*#.*$");

	private static final String PARAM_ADMIN_KEY = "admin_key";
	private static final String PARAM_GAME_CHANNEL = "gamechan";
	private static final String PARAM_CHANNEL_KEY = "chan_key";
	private static final String PARAM_HOST = "host";
	private static final String PARAM_PORT = "port";
	private static final String PARAM_SERVER_PASS = "serv_pass";
	private static final String PARAM_BOT_NAME = "bot_name";
	private static final String PARAM_CMD_PREFIX = "cmd_prefix";
	private static final String PARAM_VERBOSE = "verbose";
	private static final String PARAM_START_MONEY = "start_money";
	private static final String PARAM_ANTE = "ante";

	private Constants() {
	}

	static {
		Scanner cfgFile = null;

		try {
			cfgFile = new Scanner(new File(CFG_FILENAME));
		} catch (FileNotFoundException fnfe) {
			System.err.println("Could not find configuration file ("
					+ CFG_FILENAME + ").");
			System.exit(1);
		}

		Map<String, String> parameters = new HashMap<>();

		int lineno = 0;
		while (cfgFile.hasNextLine()) {
			lineno++;

			final String line = cfgFile.nextLine();

			if (line.isEmpty() || CFG_COMMENT.matcher(line).matches())
				continue;

			final Matcher matcher = CFG_LINE.matcher(line);

			if (!matcher.find())
				cfgSyntaxError(lineno);

			parameters.put(matcher.group(1), matcher.group(2));
		}

		cfgFile.close();
		boolean error = false;

		ADMIN_KEY = parameters.remove(PARAM_ADMIN_KEY);

		if (ADMIN_KEY == null) {
			cfgNotSpecifiedError("Admin key", PARAM_ADMIN_KEY);
			error = true;
		}

		GAME_CHANNEL = parameters.remove(PARAM_GAME_CHANNEL);

		if (GAME_CHANNEL == null) {
			cfgNotSpecifiedError("Game channel", PARAM_GAME_CHANNEL);
			error = true;
		}

		// a null channel key means there is no channel key
		CHANNEL_KEY = parameters.remove(PARAM_CHANNEL_KEY);

		HOST = parameters.remove(PARAM_HOST);

		if (HOST == null) {
			cfgNotSpecifiedError("Host", PARAM_HOST);
			error = true;
		}

		final String portStr = parameters.remove(PARAM_PORT);

		if (portStr == null) {
			cfgNotSpecifiedError("Port", PARAM_PORT);
			error = true;
		}

		int port = 0;

		try {
			port = Integer.parseInt(portStr);
			if (port <= 0)
				throw new NumberFormatException();
		} catch (NumberFormatException nfe) {
			System.err.println(PARAM_PORT + " must be a positive integer.");

			error = true;
		}

		PORT = port;

		// a null server password means there is no server password
		SERVER_PASS = parameters.remove(PARAM_SERVER_PASS);

		BOT_NAME = parameters.remove(PARAM_BOT_NAME);

		if (BOT_NAME == null) {
			cfgNotSpecifiedError("Bot name", PARAM_BOT_NAME);
			error = true;
		}

		final String cmdPrefix = parameters.remove(PARAM_CMD_PREFIX);

		if (cmdPrefix == null) {
			cfgNotSpecifiedError("Command prefix", PARAM_CMD_PREFIX);
			error = true;
		} else if (cmdPrefix.length() != 1) {
			System.err.println(PARAM_CMD_PREFIX
					+ " must be only 1 character in length.");
			error = true;
		}

		CMD_PREFIX = error ? 0 : cmdPrefix.charAt(0);

		final String verbose = parameters.remove(PARAM_VERBOSE);

		if (verbose == null) {
			cfgNotSpecifiedError("Verbosity", PARAM_VERBOSE);
			error = true;
		} else if (!(verbose.equals("true") || verbose.equals("false"))) {
			System.err.println(PARAM_VERBOSE
					+ " must be either 'true' or 'false'.");
			error = true;
		}

		VERBOSE = error ? false : Boolean.parseBoolean(verbose);

		final String startMoneyStr = parameters.remove(PARAM_START_MONEY);

		if (startMoneyStr == null) {
			cfgNotSpecifiedError("Starting player money", PARAM_VERBOSE);
			error = true;
		}

		int startMoney = 0;

		try {
			startMoney = Integer.parseInt(startMoneyStr);
			if (startMoney <= 0)
				throw new NumberFormatException();
		} catch (NumberFormatException nfe) {
			System.err.println(PARAM_START_MONEY
					+ " must be a positive integer.");

			error = true;
		}

		START_MONEY = startMoney;

		final String anteStr = parameters.remove(PARAM_ANTE);

		if (anteStr == null) {
			cfgNotSpecifiedError("Ante", PARAM_ANTE);
			error = true;
		}

		int ante = 0;

		try {
			ante = Integer.parseInt(anteStr);
			if (ante <= 0)
				throw new NumberFormatException();
		} catch (NumberFormatException nfe) {
			System.err.println(PARAM_ANTE + " must be a positive integer.");
			error = true;
		}

		ANTE = ante;

		if (error)
			System.exit(1);
	}

	private static void cfgSyntaxError(int lineno) {
		System.err.println("Configuration file syntax error on line " + lineno
				+ ".");

		System.err
				.println("Parameters are assigned with the syntax: <parameter> = <value>");

		System.exit(1);
	}

	private static void cfgNotSpecifiedError(String notSpecified, String param) {
		System.err.println(notSpecified
				+ " not specified in configuration file, specify with '"
				+ param + "'.");
	}
}
