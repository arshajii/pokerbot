package me.ars.pokerbot;

import org.jibble.pircbot.Colors;
import org.jibble.pircbot.PircBot;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.text.SimpleDateFormat;

public class IrcBot extends PircBot implements IrcCallback {

  /*
   * pattern representing one or more contiguous whitespace characters, used
   * for parsing commands
   */
  private static final Pattern SPACES = Pattern.compile("\\s+");
  private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM dd HH:mm:ss");
  private Map<String, Table> tables;

  private Roster roster;

  /*
   * set of administrators by hostname
   */
  private Set<String> admins = new HashSet<>();

  public IrcBot(String startingChannel) {
    try {
      roster = Roster.getRoster();
    } catch (IOException e) {
      e.printStackTrace();
    }
    tables = new HashMap<>();
    tables.put(startingChannel, new Table(this, startingChannel, roster));

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

  private void getStats(String nickname, String channel) {
    if (roster == null) {
      sendMessage(channel, "No stats available at this time");
      return;
    }
    final Stats stats = roster.getStats(nickname);
    if (stats == null) {
      sendMessage(channel, "No stats tracked for " + nickname);
      return;
    }
    sendMessage(channel, stats.toString());
  }

  public void joinGameChannel(String channel, String key) {
    if (key == null)
      joinChannel(channel);
    else
      joinChannel(channel, key);
  }

  @Override
  protected String getUserString() {
    return getName() + " " + getName() + " " + getName() + " :" + getName();
  }

  @Override
  public void onMessage(String channel, String sender, String login, String hostname, String message) {

    if (!tables.containsKey(channel) || message.isEmpty() || message.charAt(0) != Constants.CMD_PREFIX) {
      return;
    }

    final String[] split = SPACES.split(message);
    final Table table = tables.get(channel);

    switch (split[0].substring(1)) {
      case "ping": {
        sendMessage(channel, Colors.BOLD + sender + Colors.NORMAL + ": " + message);
        break;
      }
      case "join": {
        final boolean success = table.registerPlayer(sender);

        if (success) {
          sendReply(channel, sender, "You have now joined! Please wait for the game to start.");
        } else {
          sendReply(channel, sender, "Could not join. A game is already in progress.");
        }
        break;
      }
      case "unjoin": {
        table.unjoin(sender);
        break;
      }
      case "current": {
        table.showCurrent();
        break;
      }
      case "players": {
        final List<Player> players = table.getPlayers();
        if (players.isEmpty()) {
          sendMessage(channel, "No joined players.");
          break;
        }

        if (table.isGameInProgress()) {
          sendMessage(
                  channel,
                  "Now playing: "
                          + players.stream().map(player ->
                          player.getName() + " $" + player.getMoney())
                          .collect(Collectors.joining(", ")) + ".");
        } else {
          sendMessage(
                  channel,
                  "Joined players: "
                          + players.stream().map(Player::getName)
                          .collect(Collectors.joining(", ")) + ".");
        }
        break;
      }
      case "buyin": {
        table.buyin(sender);
        break;
      }
      case "activity": {
        final Calendar activity = table.getLastActivity();
        if (activity == null) {
          sendMessage(channel, "There hasn't been any activity on this table.");
        } else {
          sendMessage(channel, "Last activity: " + sdf.format(activity.getTime()));
        }
        break;
      }
      case "stats" : {
        getStats(sender, channel);
        break;
      }
      case "clear": {
        if (table.isGameInProgress()) {
          sendReply(channel, sender, "A game is already in progress.");
          break;
        }
        table.clearPlayers();
        sendMessage(channel, "Players list cleared.");
        break;
      }
      case "start": {
        if (table.isGameInProgress()) {
          break;
        }
        if (table.getPlayers().size() > 1) {
          table.startGame();
        } else {
          sendReply(channel, sender, "Need at least 2 players to join before starting.");
        }
        break;
      }
      case "stop": {
        if (!table.isGameInProgress()) {
          break;
        }

        table.stopGame();
        break;
      }
      case "call": {
        if (!table.isGameInProgress()) {
          break;
        }
        table.call(sender);
        break;
      }
      case "c":
      case "czech":
      case "check": {
        if (!table.isGameInProgress()) {
          break;
        }

        table.check(sender);
        break;
      }
      case "r":
      case "raise": {
        if (!table.isGameInProgress()) {
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

        if (newRaise == 1) {
          table.call(sender);
        } else {
          table.raise(sender, newRaise);
        }

        break;
      }
      case "allin": {
        if (!table.isGameInProgress()) {
          break;
        }
        table.allIn(sender);
        break;
      }
      case "f":
      case "fold": {
        if (!table.isGameInProgress()) {
          break;
        }

        table.fold(sender);
        break;
      }
      case "cashout": {
        if (!table.isGameInProgress()) {
          break;
        }

        table.cashout(sender);
        break;
      }
      case "help": {
        // todo
        break;
      }
    }
  }

  private boolean isAdmin(String hostname) {
    return admins.contains(hostname);
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
      case "createtable": {
        if (split.length < 2) {
          sendMessage(sender, "You need to specify a channel where I'm creating a table");
          break;
        }
        final String newChannel = split[1];
        if (tables.containsKey(newChannel)) {
          sendMessage(sender, "#" + newChannel + " already has a table.");
          break;
        }
        tables.put(newChannel, new Table(this, newChannel, roster));
        if (split.length > 2) {
          joinChannel(newChannel, split[2]);
        } else {
          joinChannel(newChannel);
        }
        sendMessage(sender, "Table created. To remove the table, simply kick the bot from the channel.");
        break;
      }
    }
  }

  @Override
  protected void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
    if (recipientNick.equals(getNick()) && tables.containsKey(channel)) {
      final Table table = tables.get(channel);
      table.stopGame();
      tables.remove(channel);
    }
  }

  private void addAdmin(String hostname) {
    admins.add(hostname);
  }


  protected void sendReply(String target, String name, String message) {
    sendMessage(target, Colors.BOLD + name + Colors.NORMAL + ": " + message);
  }

  @Override
  public void messageChannel(String channel, String message) {
    sendMessage(channel, message);
  }

  @Override
  public void messagePlayer(Player player, String message) {
    sendMessage(player.getName(), message);
  }
}
