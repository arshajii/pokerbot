package me.ars.pokerbot;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jibble.pircbot.Colors;
import org.jibble.pircbot.PircBot;

public class IrcBot extends PircBot implements IrcCallback {

  /*
   * pattern representing one or more contiguous whitespace characters, used
   * for parsing commands
   */
  private static final Pattern SPACES = Pattern.compile("\\s+");

  /*
   * channel in which the games will be played
   */
  private String gameChannel;

  /**
   * todo: support multiple of these!
   */
  private Table table;

  /*
   * set of administrators by hostname
   */
  private Set<String> admins = new HashSet<>();

  public IrcBot(String gameChannel) {
    this.gameChannel = gameChannel;
    table = new Table(this);

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

  public void joinGameChannel(String key) {
    if (key == null)
      joinChannel(gameChannel);
    else
      joinChannel(gameChannel, key);
  }

  @Override
  public void onMessage(String channel, String sender, String login,
                        String hostname, String message) {

    if (!gameChannel.equals(channel) || message.isEmpty() || message.charAt(0) != Constants.CMD_PREFIX) {
      return;
    }

    final String[] split = SPACES.split(message);

    switch (split[0].substring(1)) {
      case "ping": {
        sendMessage(channel, Colors.BOLD + sender + Colors.NORMAL + ": " + message);
        break;
      }
      case "gamechan": {
        if (!isAdmin(hostname)) {
          sendMessage(channel, sender + ": Only an admin can change the game channel.");
          break;
        }

        if (table.isGameInProgress()) {
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
      case "joined": {
        final List<Player> players = table.getPlayers();
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

        if (table.isGameInProgress()) {
          sendReply(channel, sender, "A game is already in progress.");
          break;
        }
        table.clearPlayers();
        sendMessage(channel, "Players list cleared.");
        break;
      }
      case "start": {
        if (table.getPlayers().size() > 1) {
          table.startGame();
        } else {
          sendReply(channel, sender, "Need at least 2 players to join before starting.");
        }
        break;
      }
      case "stop": {
        if (!isAdmin(hostname)) {
          sendReply(channel, sender, "Only an admin can stop the game.");
          break;
        }

        if (!table.isGameInProgress()) {
          sendReply(channel, sender, "No game is currently in progress.");
          break;
        }

        table.stopGame();
        break;
      }
      case "call": {
        if (!table.isGameInProgress()) {
          // calling what??
          break;
        }
        table.call(sender);
        break;
      }
      case "check": {
        if (!table.isGameInProgress()) {
          break;
        }

        table.check(sender);
        break;
      }
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

        table.raise(sender, newRaise);

        break;
      }
      case "allin": {
        if (!table.isGameInProgress()) {
          break;
        }
        table.allIn(sender);
        break;
      }
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
    }
  }

  private void addAdmin(String hostname) {
    admins.add(hostname);
  }


  protected void sendReply(String target, String name, String message) {
    sendMessage(target, Colors.BOLD + name + Colors.NORMAL + ": " + message);
  }

  @Override
  public void messageChannel(String message) {
    sendMessage(gameChannel, message);
  }

  @Override
  public void messagePlayer(Player player, String message) {
    sendMessage(player.getName(), message);
  }
}
