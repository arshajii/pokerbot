package me.ars.pokerbot;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Table {

  private final String channel;
  private final IrcCallback irc;
  private final List<Player> players = new ArrayList<>();
  private final Queue<Card> deck = new ArrayDeque<>(52);
  private final List<Card> table = new ArrayList<>(5);
  private final Queue<String> buyInPlayers = new ArrayDeque<>();
  private final Roster roster;

  private Calendar lastActivity = null;
  private boolean gameInProgress = false;
  private int turnIndex;
  private int lastIndex;
  private int startPlayer;
  private int pot;
  private int raise;

  public Table(IrcCallback irc, String channel, Roster roster) {
    this.irc = irc;
    this.channel = channel;
    this.roster = roster;
  }

  private boolean verifyCurrentPlayer(Player player) {
    if (player == null) return false;
    return (player.equals(players.get(turnIndex)));
  }

  private void messageChannel(String message) {
    irc.messageChannel(channel, message);
  }

  private void messagePlayer(Player player, String message) {
    irc.messagePlayer(player, "[" + channel + "] " + message);
  }

  public Calendar getLastActivity() {
    return lastActivity;
  }

  private void setActivity() {
    lastActivity = Calendar.getInstance();
  }

  /**
   * Incoming 'call' from [player]
   */
  public void call(String nick) {
    final Player player = getPlayer(nick);
    if (!verifyCurrentPlayer(player)) return;
    setActivity();

    final int owed = amountOwed(player);
    final int money = player.getMoney();
    int bet;

    if (money >= owed) {
      messageChannel(nick + " called! (" + moneyString(owed) + ")");
      bet = owed;
    } else {
      messageChannel(nick + " called! (" + moneyString(money) + " of " + moneyString(owed) + ")");
      bet = money;
    }

    pot += player.bet(bet);
    nextTurn();
  }

  public boolean isGameInProgress() {
    return gameInProgress;
  }

  public List<Player> getPlayers() {
    return players;
  }

  public void showCurrent() {
    if (!gameInProgress) {
      messageChannel("Not currently playing.");
      return;
    }
    final String tableStr = table.isEmpty() ? "no cards" : table.stream()
        .map(Card::toString).collect(Collectors.joining(", "));
    final String currentPlayer = players.get(turnIndex).getName();
    messageChannel("On the table: " + tableStr + " || In the pot: " + moneyString(pot) + " || Current player: " + currentPlayer);
  }

  /**
   * Incoming check from [player]
   */
  public void check(String nick) {
    final Player player = getPlayer(nick);
    if (!verifyCurrentPlayer(player)) return;
    setActivity();

    if (player.getAmountPayed() >= raise) {
      nextTurn();
    } else {
      messageChannel(player.getName() + " must at least call last raise (" + moneyString(amountOwed(player)) + ").");
    }
  }

  /**
   * Incoming raise from [player]
   */
  public void raise(String nick, int newRaise) {
    final Player player = getPlayer(nick);
    if (!verifyCurrentPlayer(player)) return;
    setActivity();

    final int totalBet = amountOwed(player) + newRaise;
    final int money = player.getMoney();

    if (totalBet <= money) {
      pot += player.bet(totalBet);
      raise += newRaise;

      messageChannel(player.getName() + " raised " + moneyString(newRaise) + ".");

      lastIndex = lastUnfolded(turnIndex - 1);
      nextTurn();
    } else {
      messageChannel(player.getName() + " doesn't have enough money. They need " + moneyString(totalBet) + " but only have " + moneyString(money) + ".");
    }
  }

  /**
   * Incoming allin from [player]
   */
  public void allIn(String nick) {
    final Player player = getPlayer(nick);
    if (!verifyCurrentPlayer(player)) return;
    setActivity();
    final int owed = amountOwed(player);
    final int money = player.getMoney();

    messageChannel(player.getName() + " goes all in!");

    if (money > owed) {
      raise(nick, money - owed);
    } else {
      call(nick);
    }
  }

  /**
   * Incoming fold from [player]
   */
  public void fold(String nick) {
    final Player player = getPlayer(nick);
    if (!verifyCurrentPlayer(player)) return;
    setActivity();
    player.fold();
    //messageChannel(player.getName() + " folds.");
    final boolean nextTurn = !checkForWinByFold();
    if (nextTurn) {
      nextTurn();
    }
  }

  public void cashout(String nick) {
    final Player player = getPlayer(nick);
    if (!verifyCurrentPlayer(player)) return;
    setActivity();
    player.cashout();
    messageChannel(player.getName() + " cashed out with " + moneyString(player.getMoney()) + "!");
    roster.modifyMoney(player.getName(), player.getMoney() - Constants.START_MONEY);
    final boolean nextTurn = !checkForWinByFold();
    if (nextTurn) {
      nextTurn();
    }
  }

  public Player getPlayer(String nick) {
    for(Player player: players) {
      if (player.getName().equals(nick)) {
        return player;
      }
    }
    return null;
  }

  /**
   * Adds a player to the table
   *
   * @return True if the player was added
   */
  public boolean registerPlayer(String name) {
    if (gameInProgress) {
      return false;
    }

    return players.add(new Player(name));
  }

  private void deal() {
    for (Player player : players) {
      final Card card1 = deck.poll();
      final Card card2 = deck.poll();
      messagePlayer(player, "Your cards: " + card1 + ", " + card2);
      player.receiveCards(card1, card2);
    }
  }

  private void setupHand() {
    for (Player player : players) {
      if (player.isBroke()) {
        player.cashout();
        roster.modifyMoney(player.getName(), -Constants.START_MONEY);
      }
    }

    if (!buyInPlayers.isEmpty()) {
      for (String name : buyInPlayers) {
        players.add(new Player(name));
        roster.trackGame(name);
      }
    }
    buyInPlayers.clear();

    final Iterator<Player> playerIter = players.iterator();
    int index = 0;

    while (playerIter.hasNext()) {
      Player player = playerIter.next();
      if (!player.isActive()) {
        playerIter.remove();
        if (index < startPlayer) {
          startPlayer = wrappedDecrement(startPlayer);
        }
      }
      index++;
    }

    if (players.size() < 2) {
      messageChannel("Not enough players left to continue: game ended.");
      stopGame();
      return;
    }

    messageChannel("Starting new hand...");

    for (Player player : players) {
      player.newHand();
    }

    final List<Card> rawDeck = Arrays.asList(Card.getDeck());
    Collections.shuffle(rawDeck);
    deck.clear();
    deck.addAll(rawDeck);
    table.clear();
    turnIndex = startPlayer;
    try {
      lastIndex = lastUnfolded(startPlayer - 1);
      startPlayer = wrappedIncrement(startPlayer);
    } catch (IndexOutOfBoundsException e) {
      System.err.println(e.toString());
      e.printStackTrace();
      startPlayer = 0;
    }
    pot = 0;
    raise = Constants.ANTE;
    // TODO: Blinds

    messageChannel(
        players.stream()
            .map(p -> "[" + p.getName()
                + " - "
                + moneyString(p.getMoney()) + "]")
            .collect(Collectors.joining(" ")));

    deal();
    collectAntes();
    sendStatus(players.get(turnIndex).getName());
  }

  private void nextTurn() {
    final Player player = players.get(turnIndex);
    if (turnIndex == lastIndex && (player.isFolded() || player.isBroke() ||
        amountOwed(player) == 0)) {

      if (table.size() == 5) {
        // winner selection
        List<Hand> hands = new ArrayList<>(players.size());

        for (Player p : players) {
          final Card[] playerCards = table.toArray(new Card[7]);
          playerCards[5] = p.getCard1();
          playerCards[6] = p.getCard2();
          hands.add(Hand.getBestHand(p, playerCards));
        }

        hands.sort(Collections.reverseOrder());
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

        messageChannel(
            "Reveal: "
                + players
                .stream()
                .filter(p -> !p.isFolded())
                .map(p -> "[" + p.getName() + " - " + p.getCard1() + ", " + p.getCard2() + "]")
                .collect(Collectors.joining(" ")));

        int numWinners = winners.size();

        if (numWinners == 1) {
          messageChannel(winner1.getName() + " wins with the hand " + winningHand + "!");
          winner1.win(pot);
        } else {
          messageChannel(
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

    messageChannel("On the table: " + tableStr + " || In the pot: " + moneyString(pot));

    messageChannel(turn + "'s turn!");
  }

  private void collectAntes() {
    messageChannel("Collecting a " + moneyString(Constants.ANTE) + " ante from each player...");

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

  public void startGame() {
    messageChannel("Starting game with: "
        + players.stream().map(Player::getName)
        .collect(Collectors.joining(", ")) + ".");

    for (Player player : players) {
      roster.trackGame(player.getName());
    }

    gameInProgress = true;
    startPlayer = 0;
    setupHand();
  }

  public void stopGame() {
    gameInProgress = false;
    Player winner = null;
    if (players.size() == 1) {
      winner = players.get(0);
      roster.modifyMoney(winner.getName(), winner.getMoney() - Constants.START_MONEY);
    } else {
      int highscore = 0;
      for (Player player: players) {
        final int playerMoney = player.getMoney();
        roster.modifyMoney(player.getName(), playerMoney - Constants.START_MONEY);
        if (playerMoney > highscore) {
          highscore = playerMoney;
          winner = player;
        }
      }
    }
    players.clear();
    deck.clear();
    table.clear();

    if (winner == null) {
      messageChannel("Game stopped. No conclusive winner.");
    } else {
      messageChannel("Game stopped. " + winner.getName() + " is the winner!");
    }

    try {
      roster.saveRoster();
    } catch (IOException e) {
      System.err.println(e.toString());
      e.printStackTrace();
    }
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
      messageChannel(last.getName() + " wins (all other players folded)!");

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
    return "$" + amount;
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

  public void clearPlayers() {
    players.clear();
  }

  public void unjoin(String sender) {
    final Iterator<Player> iter = players.iterator();
    boolean everJoined = false;

    while (iter.hasNext()) {
      Player player = iter.next();

      if (player.getName().equals(sender)) {
        iter.remove();
        messageChannel(sender + ": You have unjoined.");
        everJoined = true;
        break;
      }
    }

    if (buyInPlayers.contains(sender)) {
      messageChannel(sender + ": Your buyin was nulled.");
      buyInPlayers.remove(sender);
    } else if (!everJoined) {
      messageChannel(sender + ": You never joined.");
    }
  }

  public void buyin(String sender) {
    if (!gameInProgress) {
      messageChannel(sender + ": Game hasn't started yet, putting you up for the game");
      registerPlayer(sender);
      return;
    }
    for(Player player: players) {
      if (player.getName().equals(sender)) {
        messageChannel(sender + ": You're already in the game.");
        return;
      }
    }
    if (buyInPlayers.contains(sender)) {
      messageChannel(sender + ": You've already bought in");
      return;
    }
    buyInPlayers.add(sender);
    messageChannel(sender + " has bought in the game, will join on next hand.");
  }
}
