package me.ars.pokerbot.poker;

import me.ars.pokerbot.Constants;
import me.ars.pokerbot.stats.Roster;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Table {
  private final StateCallback callback;
  private final List<Player> players = new ArrayList<>();
  private final Queue<Card> deck = new ArrayDeque<>(52);
  private final List<Card> table = new ArrayList<>(5);
  private final Queue<String> buyInPlayers = new ArrayDeque<>();
  private final Roster roster;
  private final List<Pot> sidePots;

  private Calendar lastActivity = null;
  private boolean gameInProgress = false;
  private int turnIndex;
  private int lastIndex;
  private int startPlayer;
  private Pot mainPot;

  public Table(StateCallback callback, Roster roster) {
    this.callback = callback;
    this.roster = roster;
    this.mainPot = new Pot();
    this.sidePots = new ArrayList<>();
  }

  private boolean verifyCurrentPlayer(Player player) {
    if (player == null) return false;
    return (player.equals(players.get(turnIndex)));
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
    final int bet;

    callback.playerCalled(nick, money, owed);
    bet = Math.min(money, owed);
    int playerPays = player.bet(bet);

    final boolean hadSidepots = mainPot.hasSidePot();
    mainPot.call(player, playerPays);
    /*
    if (sidePot != null) {
      sidePots.add(sidePot);
    }

     */

    if (hadSidepots) {
      for (Pot otherSidePot : sidePots) {
        otherSidePot.call(player, otherSidePot.getCurrentBet());
      }
    }

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
      callback.announce("Not currently playing.");
      return;
    }
    final String currentPlayer = players.get(turnIndex).getName();
    callback.updateTable(table, mainPot.getMoney(), currentPlayer);
  }

  /**
   * Incoming check from [player]
   */
  public void check(String nick) {
    final Player player = getPlayer(nick);
    if (!verifyCurrentPlayer(player)) return;
    setActivity();

    int totalBets = mainPot.getCurrentBet() + sidePots.stream().mapToInt(Pot::getCurrentBet).sum();

    callback.playerChecked(nick);
    if (player.getAmountPayed() >= totalBets) {
      mainPot.checkPlayer(player);
      nextTurn();
    } else {
      callback.mustCallRaise(player.getName(), amountOwed(player));
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
      if (!mainPot.hasSidePot()) {
        mainPot.raise(player, player.bet(totalBet));
      } else {
        for(Pot sidePot: sidePots) {
          if (!sidePot.hasSidePot()) {
            sidePot.raise(player, player.bet(totalBet));
          }
        }
      }

      callback.playerRaised(player.getName(), newRaise);
      lastIndex = lastUnfolded(turnIndex - 1);
      nextTurn();
    } else {
      callback.playerCannotRaise(player.getName(), totalBet, money);
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

    callback.playerAllin(player.getName());
    player.setAllIn(true);

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
    callback.playerFolded(player.getName());
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
    callback.playerCashedOut(player.getName(), player.getMoney());
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

  public void registerPlayer(String name) {
    if (gameInProgress) {
      callback.announce("A game is already in progress! Use the buyin command if you still want to join");
      return;
    }
    addPlayer(name, true);
  }

  private boolean addPlayer(String name, boolean verbose) {
    for(Player player: players) {
      if (player.getName().equals(name)) {
        if (verbose) {
          callback.announce(name + " has already joined.");
        }
        return false;
      }
    }
    final boolean added = players.add(new Player(name));
    if (verbose) {
      if (added) {
        callback.announce(name + " has joined the game.");
      } else {
        callback.announce("Could not add " + name + " to the game");
      }
    }
    return added;
  }

  private void deal() {
    for (Player player : players) {
      final Card card1 = deck.poll();
      final Card card2 = deck.poll();
      callback.showPlayerCards(player.getName(), card1, card2);
      player.receiveCards(card1, card2);
    }
  }

  private void setupHand() {
    for (Player player : players) {
      player.setAllIn(false);
      if (player.isBroke()) {
        player.cashout();
        roster.modifyMoney(player.getName(), -Constants.START_MONEY);
      }
    }

    if (!buyInPlayers.isEmpty()) {
      for (String name : buyInPlayers) {
        addPlayer(name, false);
        roster.trackGame(name);
      }
    }

    try {
      roster.saveRoster();
    } catch (IOException e) {
      System.err.println(e.toString());
      e.printStackTrace();
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
      callback.announce("Not enough players left to continue: game ended.");
      stopGame();
      return;
    }

    callback.announce("Starting new hand...");

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
    mainPot.reset();
    sidePots.clear();
    // TODO: Blinds

    callback.showPlayers(players.stream().collect(Collectors.toMap(Player::getName, Player::getMoney)));
    deal();
    collectAntes();
    sendStatus(players.get(turnIndex).getName());
  }

  private void nextTurn() {
    mainPot.newTurn();
    sidePots.forEach(Pot::newTurn);
    final Player player = players.get(turnIndex);
    if (turnIndex == lastIndex && (player.isFolded() || player.isBroke() || amountOwed(player) == 0)) {

      if (table.size() == 5) {
        // winner selection
        // todo check for pot
        checkWinners(mainPot);
        if (sidePots.size()>0) {
          callback.announce("Checking sidepots...");
          for(Pot sidePot: sidePots) {
            checkWinners(sidePot);
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

    if (nextPlayer.isAllIn()) {
      callback.announce(nextPlayer.getName() + " is all-in, next player...");
      nextTurn();
    } else {
      sendStatus(nextPlayer.getName());
    }
  }

  private void checkWinners(Pot pot) {
    List<Hand> hands = new ArrayList<>(pot.getParticipants().size());
    callback.announce("Amount of participants in pot: " + pot.getParticipants().size());
    callback.announce("Has side pot: " + pot.hasSidePot());
    for (Player p : pot.getParticipants()) {
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
    final Map<String, List<Card>> reveal = new HashMap<>();
    for(Player p: players) {
      if (!p.isFolded()) {
        final List<Card> cards = new ArrayList<>();
        cards.add(p.getCard1());
        cards.add( p.getCard2());
        reveal.put(p.getName(), cards);
      }
    }

    callback.revealPlayers(reveal);

    int numWinners = winners.size();

    if (numWinners == 1) {
      callback.declareWinner(winner1.getName(), winningHand, pot.getMoney());
      winner1.win(pot.getMoney());
    } else {
      callback.declareSplitPot(winners.stream().map(Hand::getPlayer).map(Player::getName)
              .collect(Collectors.toList()), winningHand.getHandType(), pot.getMoney());
      int winnings = pot.getMoney() / numWinners;
      for (Hand hand : winners) {
        hand.getPlayer().win(winnings);
      }
    }
  }

  private void sendStatus(String turn) {
    callback.updateTable(table, mainPot.getMoney(), turn);
    callback.declarePlayerTurn(turn);
  }

  private void collectAntes() {
    callback.collectAnte(Constants.ANTE);

    for (Player player : players) {
      mainPot.collectAnte(player, player.bet(Constants.ANTE));
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
    callback.announce("Starting game with: "
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
      callback.announce("Game stopped. No conclusive winner.");
    } else {
      callback.announce("Game stopped. " + winner.getName() + " is the winner!");
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
      callback.announce(last.getName() + " wins (all other players folded)!");

      int totalMoney = mainPot.getMoney();
      for(Pot sidePot: sidePots) {
        totalMoney += sidePot.getMoney();
      }
      last.win(totalMoney);
      setupHand();
      return true;
    }

    return false;
  }

  private int amountOwed(Player player) {
    final int totalBet = mainPot.getCurrentBet() + sidePots.stream().mapToInt(Pot::getCurrentBet).sum();
    return totalBet - player.getAmountPayed();
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
        callback.announce(sender + ": You have unjoined.");
        everJoined = true;
        break;
      }
    }

    if (buyInPlayers.contains(sender)) {
      callback.announce(sender + ": Your buyin was nulled.");
      buyInPlayers.remove(sender);
    } else if (!everJoined) {
      callback.announce(sender + ": You never joined.");
    }
  }

  public void buyin(String sender) {
    if (!gameInProgress) {
      callback.announce(sender + ": Game hasn't started yet, putting you up for the game");
      registerPlayer(sender);
      return;
    }
    for(Player player: players) {
      if (player.getName().equals(sender)) {
        callback.announce(sender + ": You're already in the game.");
        return;
      }
    }
    if (buyInPlayers.contains(sender)) {
      callback.announce(sender + ": You've already bought in");
      return;
    }
    buyInPlayers.add(sender);
    callback.announce(sender + " has bought in the game, will join on next hand.");
  }
}
