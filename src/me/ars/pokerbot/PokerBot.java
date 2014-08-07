package me.ars.pokerbot;

import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jibble.pircbot.Colors;
import org.jibble.pircbot.PircBot;

public class PokerBot extends PircBot {

	/*
	 * pattern representing one or more contiguous whitespace characters, used
	 * for parsing commands
	 */
	private static final Pattern SPACES = Pattern.compile("\\s+");

	/*
	 * channel in which the games will be played
	 */
	private String gameChannel;

	/*
	 * set of administrators by host name
	 */
	private Set<String> admins = new HashSet<>();

	private final List<Player> players = new ArrayList<>();
	private boolean gameInProgress = false;
	private final Queue<Card> deck = new ArrayDeque<>(52);
	private final List<Card> table = new ArrayList<>(5);

	private int turnIndex;
	private int lastIndex;
	private int startPlayer;
	private int pot;
	private int raise;

	public PokerBot(String gameChannel) {
		this.gameChannel = gameChannel;

		setName(Constants.BOT_NAME);
		setAutoNickChange(true);
		setLogin(Constants.BOT_NAME);
		setVersion("");

		try {
			setEncoding("UTF-8");
		} catch (UnsupportedEncodingException uee) {
			System.err.println("UTF-8 must be supported.");
			System.exit(1);
		}
	}

	@Override
	public void onMessage(String channel, String sender, String login,
			String hostname, String message) {

		if (!gameChannel.equals(channel) || message.isEmpty()
				|| message.charAt(0) != Constants.CMD_PREFIX) {

			return;
		}

		final String[] split = SPACES.split(message);

		switch (split[0].substring(1)) {
		case "ping": {
			sendReply(channel, sender, "pong");
			break;
		}
		case "gamechan": {
			if (!isAdmin(hostname)) {
				sendReply(channel, sender,
						"Only an admin can change the game channel.");
				break;
			}

			if (gameInProgress) {
				sendReply(
						channel,
						sender,
						"A game is currently in progress. The current game "
								+ "must be stopped before changing the game channel.");
				break;
			}

			if (split.length > 2)
				joinChannel(split[1], split[2]);
			else
				joinChannel(split[1]);

			partChannel(gameChannel);
			gameChannel = split[1];
			break;
		}
		case "join": {
			if (registerPlayer(sender)) {
				sendReply(channel, sender,
						"You have now joined! Please wait for the game to start.");
			} else {
				sendReply(channel, sender,
						"Could not join. A game is already in progress.");
			}
			break;
		}
		case "unjoin": {
			final Iterator<Player> iter = players.iterator();
			boolean everJoined = false;

			while (iter.hasNext()) {
				Player player = iter.next();

				if (player.getName().equals(sender)) {
					iter.remove();
					sendReply(channel, sender, "You have unjoined.");
					everJoined = true;
					break;
				}
			}

			if (!everJoined)
				sendReply(channel, sender, "You had never joined.");

			break;
		}
		case "joined": {
			if (players.isEmpty()) {
				sendMessage(channel, "No joined players.");
				break;
			}

			sendMessage(
					channel,
					"Joined players: "
							+ players.stream().map(Player::getName)
									.collect(Collectors.joining(", ")) + ".");

			break;
		}
		case "clear": {
			if (!isAdmin(hostname)) {
				sendReply(channel, sender,
						"Only an admin can clear the joined players list.");
				break;
			}

			if (gameInProgress) {
				sendReply(channel, sender, "A game is already in progress.");
				break;
			}

			players.clear();
			sendMessage(channel, "Players list cleared.");
			break;
		}
		case "start": {
			if (!isAdmin(hostname)) {
				sendReply(channel, sender, "Only an admin can start the game.");
				break;
			}

			if (players.size() < 2) {
				sendReply(channel, sender,
						"Need at least 2 players to join before starting.");
				break;
			}

			if (players.size() > 15) {
				sendReply(channel, sender, "Cannot play with over 15 players.");
				break;
			}

			startGame();
			break;
		}
		case "stop": {
			if (!isAdmin(hostname)) {
				sendReply(channel, sender, "Only an admin can stop the game.");
				break;
			}

			if (!gameInProgress) {
				sendReply(channel, sender, "No game is currently in progress.");
				break;
			}

			stopGame(sender);
			break;
		}
		case "call": {
			if (!gameInProgress) {
				noGameInProgressMsg(channel, sender);
				break;
			}

			if (!turnCheck(sender)) {
				break;
			}

			if (call(sender))
				nextTurn();

			break;
		}
		case "check": {
			if (!gameInProgress) {
				noGameInProgressMsg(channel, sender);
				break;
			}

			if (!turnCheck(sender)) {
				break;
			}

			if (check(sender))
				nextTurn();

			break;
		}
		case "raise": {
			if (!gameInProgress) {
				noGameInProgressMsg(channel, sender);
				break;
			}

			if (!turnCheck(sender)) {
				break;
			}

			if (split.length == 1) {
				sendReply(channel, sender, "Specify an amount to raise by.");
				break;
			}

			int newRaise;

			try {
				newRaise = Integer.parseInt(split[1]);
			} catch (NumberFormatException nfe) {
				sendReply(channel, sender, "Malformed number: " + split[1]
						+ ".");
				break;
			}

			if (newRaise <= 0) {
				sendReply(channel, sender,
						"Can only raise by a positive amount.");
				break;
			}

			if (raise(sender, newRaise))
				nextTurn();

			break;
		}
		case "allin": {
			if (!gameInProgress) {
				noGameInProgressMsg(channel, sender);
				break;
			}

			if (!turnCheck(sender)) {
				break;
			}

			if (allIn(sender))
				nextTurn();

			break;
		}
		case "fold": {
			if (!gameInProgress) {
				noGameInProgressMsg(channel, sender);
				break;
			}

			if (!turnCheck(sender)) {
				break;
			}

			if (fold(sender))
				nextTurn();

			break;
		}
		case "cashout": {
			if (!gameInProgress) {
				noGameInProgressMsg(channel, sender);
				break;
			}

			if (!turnCheck(sender)) {
				break;
			}

			if (cashout(sender))
				nextTurn();

			break;
		}
		case "help": {
			sendHelp(sender, split.length > 1 ? split[1] : null);
			break;
		}
		}
	}

	@Override
	protected void onPrivateMessage(String sender, String login,
			String hostname, String message) {

		if (message.isEmpty() || message.charAt(0) != Constants.CMD_PREFIX)
			return;

		String[] split = SPACES.split(message, 2);

		switch (split[0].substring(1)) {
		case "auth":
		case "authenticate":
			if (Constants.ADMIN_KEY.equals(split[1])) {
				addAdmin(hostname);
				sendMessage(sender,
						"You have successfully authenticated (hostname: "
								+ hostname + ").");
			} else {
				sendMessage(sender, "Incorrect key.");
			}
			break;
		}
	}

	private boolean call(String sender) {
		final Player player = players.get(turnIndex);
		final int owed = amountOwed(player);
		final int money = player.getMoney();
		int bet;

		if (money >= owed) {
			sendReply(gameChannel, sender, "You called! (" + moneyString(owed)
					+ ")");
			bet = owed;
		} else {
			sendReply(gameChannel, sender, "You called! (" + moneyString(money)
					+ " of " + moneyString(owed) + ")");
			bet = money;
		}

		pot += player.bet(bet);
		return true;
	}

	private boolean check(String sender) {
		final Player player = players.get(turnIndex);

		if (player.getAmountPayed() >= raise) {
			sendReply(gameChannel, sender, "You checked!");
			return true;
		} else {
			sendReply(gameChannel, sender,
					"You must at least call last raise ("
							+ moneyString(amountOwed(player)) + ").");
			return false;
		}
	}

	private boolean raise(String sender, int newRaise) {
		final Player player = players.get(turnIndex);

		final int totalBet = amountOwed(player) + newRaise;
		final int money = player.getMoney();

		if (totalBet <= money) {
			pot += player.bet(totalBet);
			raise += newRaise;

			sendReply(gameChannel, sender, "You raised "
					+ moneyString(newRaise) + ".");

			lastIndex = lastUnfolded(turnIndex - 1);
			return true;
		} else {
			sendReply(gameChannel, sender,
					"You don't have enough money. You need "
							+ moneyString(totalBet) + " but only have "
							+ moneyString(money) + ".");
			return false;
		}
	}

	private boolean allIn(String sender) {
		final Player player = players.get(turnIndex);
		final int owed = amountOwed(player);
		final int money = player.getMoney();

		sendReply(gameChannel, sender, "Going all in...");

		if (money > owed) {
			return raise(sender, money - owed);
		} else {
			return call(sender);
		}
	}

	private boolean fold(String sender) {
		final Player player = players.get(turnIndex);
		player.fold();
		sendReply(gameChannel, sender, "You folded!");

		return !checkForWinByFold();
	}

	private boolean cashout(String sender) {
		final Player player = players.get(turnIndex);
		player.cashout();
		sendReply(gameChannel, sender, "You cashed out with "
				+ moneyString(player.getMoney()) + "!");

		return !checkForWinByFold();
	}

	protected void sendReply(String target, String name, String message) {
		sendMessage(target, Colors.BOLD + name + Colors.NORMAL + ": " + message);
	}

	private boolean registerPlayer(String name) {
		if (gameInProgress)
			return false;

		return players.add(new Player(name));
	}

	private void noGameInProgressMsg(String channel, String sender) {
		sendReply(channel, sender, "No game is currently in progress.");
	}

	private void deal() {
		for (Player player : players) {
			Card card1 = deck.poll();
			Card card2 = deck.poll();
			sendMessage(player.getName(), "Your cards: " + card1 + ", " + card2);
			player.receiveCards(card1, card2);
		}
	}

	private void setupHand() {
		for (Player player : players) {
			if (player.isBroke())
				player.cashout();
		}

		final Iterator<Player> playerIter = players.iterator();
		int index = 0;

		while (playerIter.hasNext()) {
			Player player = playerIter.next();

			if (!player.isActive()) {
				playerIter.remove();

				if (index < startPlayer)
					startPlayer = wrappedDecrement(startPlayer);
			}

			index++;
		}

		if (players.size() < 2) {
			sendMessage(gameChannel,
					"Not enough players left to continue: game ended.");
			stopGame(null);
			return;
		}

		sendMessage(gameChannel, "Starting new hand...");

		for (Player player : players)
			player.newHand();

		List<Card> rawDeck = Arrays.asList(Card.getDeck());
		Collections.shuffle(rawDeck);
		deck.clear();
		deck.addAll(rawDeck);
		table.clear();
		turnIndex = startPlayer;
		lastIndex = lastUnfolded(startPlayer - 1);

		startPlayer = wrappedIncrement(startPlayer);

		pot = 0;
		raise = Constants.ANTE;

		sendMessage(
				gameChannel,
				players.stream()
						.map(p -> "[" + Colors.BOLD + p.getName()
								+ Colors.NORMAL + " - "
								+ moneyString(p.getMoney()) + "]")
						.collect(Collectors.joining(" ")));

		deal();
		collectAntes();
		sendStatus(players.get(turnIndex).getName());
	}

	private void nextTurn() {
		Player player = players.get(turnIndex);

		if (turnIndex == lastIndex
				&& (player.isFolded() || player.isBroke() || amountOwed(player) == 0)) {

			if (table.size() == 5) {
				/*
				 * winner selection
				 */

				List<Hand> hands = new ArrayList<>(players.size());

				for (Player p : players) {
					Card[] playerCards = table.toArray(new Card[7]);
					playerCards[5] = p.getCard1();
					playerCards[6] = p.getCard2();
					hands.add(Hand.getBestHand(p, playerCards));
				}

				Collections.sort(hands, Collections.reverseOrder());

				Iterator<Hand> orderedHands = hands.iterator();

				Hand winningHand;
				Player winner1;

				do {
					winningHand = orderedHands.next();
					winner1 = winningHand.getPlayer();
				} while (winner1.isFolded());

				List<Hand> winners = new ArrayList<>(players.size());
				winners.add(winningHand);

				while (orderedHands.hasNext()) {
					Hand next = orderedHands.next();

					if (winningHand.compareTo(next) != 0)
						break;

					if (!next.getPlayer().isFolded())
						winners.add(next);
				}

				sendMessage(
						gameChannel,
						Colors.BOLD
								+ "Reveal: "
								+ Colors.NORMAL
								+ players
										.stream()
										.filter(p -> !p.isFolded())
										.map(p -> "[" + Colors.BOLD
												+ p.getName() + Colors.NORMAL
												+ " - " + p.getCard1() + ", "
												+ p.getCard2() + "]")
										.collect(Collectors.joining(" ")));

				int numWinners = winners.size();

				if (numWinners == 1) {
					sendMessage(gameChannel, Colors.BOLD + winner1.getName()
							+ " wins" + Colors.NORMAL + " with the hand "
							+ winningHand + "!");

					winner1.win(pot);
				} else {
					sendMessage(
							gameChannel,
							"Split pot between "
									+ winners.stream().map(Hand::getPlayer)
											.map(Player::getName)
											.collect(Collectors.joining(", "))
									+ " (each with a "
									+ winningHand.getHandType() + ").");

					int winnings = pot / numWinners;
					for (Hand hand : winners) {
						hand.getPlayer().win(winnings);
					}
				}

				setupHand();
				return;
			} else {
				turnIndex = -1;
				lastIndex = lastUnfolded(players.size() - 1);
				draw();
			}
		}

		Player nextPlayer;

		do {
			turnIndex = wrappedIncrement(turnIndex);
		} while ((nextPlayer = players.get(turnIndex)).isFolded());

		sendStatus(nextPlayer.getName());
	}

	private void sendStatus(String turn) {
		final String tableStr = table.isEmpty() ? "no cards" : table.stream()
				.map(Card::toString).collect(Collectors.joining(", "));

		sendMessage(gameChannel, "On the table: " + tableStr
				+ " || In the pot: " + moneyString(pot));

		sendMessage(gameChannel, turn + "'s turn!");
	}

	private void collectAntes() {
		sendMessage(gameChannel, "Collecting a " + moneyString(Constants.ANTE)
				+ " ante from each player...");

		for (Player player : players) {
			pot += player.bet(Constants.ANTE);
		}
	}

	private void draw() {
		if (table.isEmpty()) {
			table.add(deck.poll());
			table.add(deck.poll());
			table.add(deck.poll());
		} else if (table.size() < 5) {
			table.add(deck.poll());
		}
	}

	private void startGame() {
		sendMessage(
				gameChannel,
				"Starting game with: "
						+ players.stream().map(Player::getName)
								.collect(Collectors.joining(", ")) + ".");

		gameInProgress = true;
		startPlayer = 0;
		setupHand();
	}

	private void stopGame(String sender) {
		gameInProgress = false;
		players.clear();
		deck.clear();
		table.clear();

		if (sender != null)
			sendReply(gameChannel, sender, "Game stopped.");
	}

	private boolean turnCheck(String sender) {
		if (!players.get(turnIndex).getName().equals(sender)) {
			sendReply(gameChannel, sender, "It's not your turn!");
			return false;
		}
		return true;
	}

	private boolean checkForWinByFold() {
		Player last = null;
		int numPlayersLeft = players.size();
		for (Player player : players) {
			if (player.isFolded())
				numPlayersLeft--;
			else
				last = player;
		}

		if (numPlayersLeft == 1) {
			sendMessage(gameChannel, last.getName()
					+ " wins (all other players folded)!");

			last.win(pot);
			setupHand();
			return true;
		}

		return false;
	}

	private int amountOwed(Player player) {
		return raise - player.getAmountPayed();
	}

	private void ensureNotAllFolded() {
		for (Player player : players) {
			if (!player.isFolded())
				return;
		}

		throw new IllegalStateException("All players are folded.");
	}

	private int lastUnfolded(int index) {
		ensureNotAllFolded();

		if (index < 0)
			index = players.size() - 1;

		if (index >= players.size())
			index = 0;

		while (players.get(index).isFolded()) {
			index = wrappedDecrement(index);
		}
		return index;
	}

	private static String moneyString(int amount) {
		return Colors.BOLD + Colors.DARK_GREEN + "$" + amount + Colors.NORMAL;
	}

	private int wrappedIncrement(int n) {
		n++;
		if (n >= players.size())
			n = 0;
		return n;
	}

	private int wrappedDecrement(int n) {
		n--;
		if (n < 0)
			n = players.size() - 1;
		return n;
	}

	public void joinGameChannel(String key) {
		if (key == null)
			joinChannel(gameChannel);
		else
			joinChannel(gameChannel, key);
	}

	private void addAdmin(String hostname) {
		admins.add(hostname);
	}

	private boolean isAdmin(String hostname) {
		return admins.contains(hostname);
	}

	private static final Map<String, String> commandHelp = new LinkedHashMap<>();

	private static final String ADMIN_NEEDED = " (" + Colors.RED
			+ "admin command" + Colors.NORMAL + ")";

	static {
		commandHelp.put("ping", "Ping me for a reply.");

		commandHelp.put("gamechan",
				"Change the game channel to the specified channel."
						+ ADMIN_NEEDED);

		commandHelp.put("join",
				"Add yourself to the players list for the next game.");

		commandHelp.put("unjoin",
				"Remove yourself from the players list for the next game.");

		commandHelp.put("joined",
				"Display who is in the players list for the next game.");

		commandHelp.put("clear", "Clear the players list for the next game."
				+ ADMIN_NEEDED);

		commandHelp.put("start", "Start the game." + ADMIN_NEEDED);

		commandHelp.put("stop", "Stop the game." + ADMIN_NEEDED);

		commandHelp.put("call", "Match the current bet.");

		commandHelp.put("check", "Raise nothing, pass on to the next player.");

		commandHelp.put("raise", "Raise by the specified amount " + Colors.BOLD
				+ "on top of" + Colors.NORMAL
				+ " the last raise (which may have been 0).");

		commandHelp
				.put("fold",
						"Discard your hand and forfeit. You can resume playing next hand.");

		commandHelp.put("cashout",
				"Quit the game, taking the fortunes you've won with you.");

		commandHelp.put("help", "Display help information.");
	}

	private void sendHelp(String name, String command) {
		if (command == null) {
			sendReply(
					gameChannel,
					name,
					"Commands: "
							+ commandHelp.keySet().stream()
									.collect(Collectors.joining(", "))
							+ ". Use 'help <command>' for additional details "
							+ "regarding a specific command.");
		} else {
			String helpText = commandHelp.get(command);
			if (helpText != null) {
				sendReply(gameChannel, name, "[" + Colors.DARK_BLUE + command
						+ Colors.NORMAL + "] " + helpText);
			} else {
				sendReply(gameChannel, name, command
						+ " is an unrecognized command.");
			}
		}
	}
}
