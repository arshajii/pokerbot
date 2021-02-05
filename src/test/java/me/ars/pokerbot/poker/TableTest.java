package me.ars.pokerbot.poker;

import me.ars.pokerbot.stats.Roster;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;

public class TableTest {

  private Table table;
  private StateCallback callback;
  private Roster roster;

  @Before
  public void before() {
    callback = Mockito.mock(StateCallback.class);
    roster = Mockito.mock(Roster.class);
    table = new Table(callback, roster);
  }

  @Test
  public void shortGame() {
    // Simple smoke test to see if two players can check all the way to the end without anything crashing.

    final Helper hadWinner = new Helper(false);

    Mockito.doAnswer(invocation -> {
      Object[] args = invocation.getArguments();
      Object mock = invocation.getMock();
      String winner = (String)args[0];
      System.out.println(winner + " has won!");
      hadWinner.set(true);
      return null;
    }).when(callback).declareWinner(anyString(), any(Hand.class), anyInt());

    Mockito.doAnswer(invocation -> {
      Object[] args = invocation.getArguments();
      Object mock = invocation.getMock();
      System.out.println("Split pot!");
      hadWinner.set(true);
      return null;
    }).when(callback).declareSplitPot(anyList(), any(Hand.HandType.class), anyInt());

    Mockito.doAnswer(invocation -> {
      Object[] args = invocation.getArguments();
      Object mock = invocation.getMock();
      String currentPlayer = (String)args[2];
      List<Card> cards = (List<Card>)args[0];
      final String tableStr = cards.isEmpty() ? "no cards" : cards.stream()
          .map(Card::toString).collect(Collectors.joining(", "));
      System.out.println(currentPlayer + " - " + tableStr);
      return null;
    }).when(callback).updateTable(anyList(),anyInt(),anyString());

    final String player1 = "player1";
    final String player2 = "player2";
    table.registerPlayer(player1);
    table.registerPlayer(player2);
    table.startGame();
    table.check(player1);
    table.check(player2);
    table.check(player1);
    table.check(player2);
    table.check(player1);
    table.check(player2);
    table.check(player1);
    table.check(player2);
    //Assert.assertTrue("The game must have had a winner", hadWinner.get());
  }

  @Test
  public void testTurnOrder() {
    final String player1 = "player1";
    final String player2 = "player2";
    table.registerPlayer(player1);
    table.registerPlayer(player2);
    final Player p1 = table.getPlayer(player1);
    final Player p2 = table.getPlayer(player2);
    /*
    table.startGame();
    Assert.assertEquals("The first joined player should be the first to play", p1, table.getCurrentPlayer());
    table.check(player1);
    Assert.assertEquals("Player 2 should be next", p2, table.getCurrentPlayer());
    table.check(player2);
    Assert.assertEquals("Player 1 should be next", p1, table.getCurrentPlayer());
    table.check(player1);
    Assert.assertEquals("Player 2 should be next", p2, table.getCurrentPlayer());
    table.raise(player2, 5);
    Assert.assertEquals("It should be player 1's turn after player 2 has raised", p1, table.getCurrentPlayer());
    table.call(player1);
    Assert.assertEquals("It should be back to Player 1's turn", p1, table.getCurrentPlayer());
     */
  }

  /**
   * Helper class to hold state for usage with Mockito
   */
  private final class Helper {
    private boolean b;

    Helper(boolean b) {
      this.b = b;
    }

    void set(boolean b) {
      this.b = b;
    }

    boolean get() {
      return b;
    }
  }
}
