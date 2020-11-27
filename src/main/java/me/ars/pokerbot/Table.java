package me.ars.pokerbot;

import java.util.*;
import java.util.stream.Collectors;

public class Table {

  private final IrcCallback irc;
  private final List<Player> players = new ArrayList<>();
  private final Queue<Card> deck = new ArrayDeque<>(52);
  private final List<Card> table = new ArrayList<>(5);

  private boolean gameInProgress = false;
  private int turnIndex;
  private int lastIndex;
  private int startPlayer;
  private int pot;
  private int raise;

  public Table(IrcCallback irc) {
    this.irc = irc;
  }

  private boolean verifyCurrentPlayer(Player player) {
    return (player.equals(players.get(turnIndex)));
  }

  /**
   * Incoming 'call' from [player]
   */
  public boolean call(Player player) {
    if (!verifyCurrentPlayer(player)) return false;

    final int owed = amountOwed(player);
    final int money = player.getMoney();
    int bet;

    if (money >= owed) {
      irc.messageChannel("You called! (" + moneyString(owed) + ")");
      bet = owed;
    } else {
      irc.messageChannel("You called! (" + moneyString(money) + " of " + moneyString(owed) + ")");
      bet = money;
    }

    pot += player.bet(bet);
    return true;
  }

  /**
   * Incoming check from [player]
   */
  public boolean check(Player player) {
    if (!verifyCurrentPlayer(player)) return false;

    if (player.getAmountPayed() >= raise) {
      irc.messageChannel(player.getName() + " checked!");
      return true;
    } else {
      irc.messageChannel(player.getName() + " must at least call last raise (" + moneyString(amountOwed(player)) + ").");
      return false;
    }
  }

  /**
   * Incoming raise from [player]
   */
  public boolean raise(Player player, int newRaise) {
    if (!verifyCurrentPlayer(player)) return false;

    final int totalBet = amountOwed(player) + newRaise;
    final int money = player.getMoney();

    if (totalBet <= money) {
      pot += player.bet(totalBet);
      raise += newRaise;

      irc.messageChannel(player.getName() + " raised " + moneyString(newRaise) + ".");

      lastIndex = lastUnfolded(turnIndex - 1);
      return true;
    } else {
      irc.messageChannel(player.getName() + " doesn't have enough money. They need " + moneyString(totalBet) + " but only have " + moneyString(money) + ".");
      return false;
    }
  }

  /**
   * Incoming allin from [player]
   */
  public boolean allIn(Player player) {
    if (!verifyCurrentPlayer(player)) return false;
    final int owed = amountOwed(player);
    final int money = player.getMoney();

    irc.messageChannel(player.getName() + " goes all in!");

    if (money > owed) {
      return raise(player, money - owed);
    } else {
      return call(player);
    }
  }

  /**
   * Incoming fold from [player]
   */
  public boolean fold(Player player) {
    if (!verifyCurrentPlayer(player)) return false;
    player.fold();
    irc.messageChannel(player.getName() + " folds.");
    return !checkForWinByFold();
  }

  private boolean cashout() {
    final Player player = players.get(turnIndex);
    player.cashout();
    irc.messageChannel(player.getName() + " cashed out with " + moneyString(player.getMoney()) + "!");
    return !checkForWinByFold();
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
      irc.messagePlayer(player, "Your cards: " + card1 + ", " + card2);
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
        if (index < startPlayer) {
          startPlayer = wrappedDecrement(startPlayer);
        }
      }
      index++;
    }

    if (players.size() < 2) {
      irc.messageChannel("Not enough players left to continue: game ended.");
      stopGame();
      return;
    }

    irc.messageChannel("Starting new hand...");

    for (Player player : players) {
      player.newHand();
    }

    final List<Card> rawDeck = Arrays.asList(Card.getDeck());
    Collections.shuffle(rawDeck);
    deck.clear();
    deck.addAll(rawDeck);
    table.clear();
    turnIndex = startPlayer;
    lastIndex = lastUnfolded(startPlayer - 1);
    startPlayer = wrappedIncrement(startPlayer);
    pot = 0;
    raise = Constants.ANTE;
    // TODO: Blinds

    irc.messageChannel(
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

        irc.messageChannel(
            "Reveal: "
                + players
                .stream()
                .filter(p -> !p.isFolded())
                .map(p -> "[" + p.getName() + " - " + p.getCard1() + ", " + p.getCard2() + "]")
                .collect(Collectors.joining(" ")));

        int numWinners = winners.size();

        if (numWinners == 1) {
          irc.messageChannel(winner1.getName() + " wins with the hand " + winningHand + "!");
          winner1.win(pot);
        } else {
          irc.messageChannel(
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

    irc.messageChannel("On the table: " + tableStr + " || In the pot: " + moneyString(pot));

    irc.messageChannel(turn + "'s turn!");
  }

  private void collectAntes() {
    irc.messageChannel("Collecting a " + moneyString(Constants.ANTE) + " ante from each player...");

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
    irc.messageChannel("Starting game with: "
        + players.stream().map(Player::getName)
        .collect(Collectors.joining(", ")) + ".");

    gameInProgress = true;
    startPlayer = 0;
    setupHand();
  }

  private void stopGame() {
    gameInProgress = false;
    players.clear();
    deck.clear();
    table.clear();

    irc.messageChannel("Game stopped.");
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
      irc.messageChannel(last.getName() + " wins (all other players folded)!");

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
}
