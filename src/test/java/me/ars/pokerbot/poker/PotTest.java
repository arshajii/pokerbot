package me.ars.pokerbot.poker;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

public class PotTest {

  private Player player1;
  private Player player2;
  private Player player3;

  private static final int ANTE = 5;

  public static <T> Set<T> toSet(T... stuff) {
    Set<T> set = new HashSet<>();
    Collections.addAll(set, stuff);
    return set;
  }

  @Before
  public void setup() {
    player1 = new Player("player1");
    player2 = new Player("player2");
    player3 = new Player("player3");
  }

  @Test
  public void testAnte() {
    final Pot pot = new Pot();
    pot.collectAnte(player1, ANTE);
    pot.collectAnte(player2, ANTE);
    pot.collectAnte(player3, ANTE);
    Assert.assertEquals(ANTE * 3, pot.getMoney());
    Assert.assertEquals(ANTE, pot.getCurrentBet());
    Set<Player> expectedList = toSet(player1, player2, player3);
    Assert.assertEquals(expectedList, pot.getParticipants());
    Assert.assertFalse("There shouldn't be a side pot", pot.hasSidePot());
  }

  @Test
  public void testAnteAndRaise() {
    final Pot pot = new Pot();
    pot.collectAnte(player1, ANTE);
    pot.collectAnte(player2, ANTE);
    pot.collectAnte(player3, ANTE);
    Assert.assertEquals(ANTE * 3, pot.getMoney());
    Assert.assertEquals(ANTE, pot.getCurrentBet());
    final Set<Player> expectedList = toSet(player1, player2, player3);
    Assert.assertEquals(expectedList, pot.getParticipants());
    Assert.assertFalse("There shouldn't be a side pot", pot.hasSidePot());
    pot.checkPlayer(player1);
    pot.checkPlayer(player2);
    pot.checkPlayer(player3);
    pot.newTurn();
    pot.checkPlayer(player1);
    pot.checkPlayer(player2);
    pot.checkPlayer(player3);
    pot.newTurn();
    Assert.assertEquals("The ante should remain in the pot", ANTE * 3, pot.getMoney());
    Assert.assertEquals("The 'bet' should remain at " + ANTE, ANTE, pot.getCurrentBet());
    Assert.assertEquals("Everyone should still be on the main pot", expectedList, pot.getParticipants());
    Assert.assertFalse("There still shouldn't be a side pot", pot.hasSidePot());
    pot.raise(player1, 50);
    final int paid = 50 + ANTE;
    Assert.assertEquals(paid, pot.getTotalContribution(player1));
    Assert.assertEquals(paid, player1.getAmountPayed());
    Assert.assertEquals("Player 2 should owe the new raise", 50, pot.getTotalOwed(player2));
    pot.call(player2);
    pot.call(player3);
    pot.newTurn();
    Assert.assertEquals(paid*3, pot.getTotalMoney());
    Assert.assertEquals(paid, pot.getTotalContribution(player1));
    Assert.assertEquals(paid, pot.getTotalContribution(player2));
    Assert.assertEquals(paid, pot.getTotalContribution(player3));
  }

  /*
  11:08 < Poker> Starting game with: kobran, jocke-l.
11:08 < Poker> Starting new hand...
11:08 < Poker> [kobran - $200] [jocke-l - $200]
11:08 < Poker> Collecting a $5 ante from each player...
11:08 < Poker> On the table: no cards || In the pot: $10 || Current player: kobran
11:08 < Poker> kobran's turn!
11:08 < kobran> .raise 5
11:08 < Poker> kobran raised $5.
11:08 < Poker> On the table: no cards || In the pot: $15 || Current player: jocke-l
11:08 < Poker> jocke-l's turn!
11:09 < jocke-l> .f
11:09 < Poker> kobran wins (all other players folded)!
11:09 < Poker> Starting new hand...
11:09 < Poker> [kobran - $200] [jocke-l - $190]
   */
  @Test
  public void testAnteRaiseAndFold() {
    final Pot pot = new Pot();
    pot.collectAnte(player1, ANTE);
    pot.collectAnte(player2, ANTE);
    Assert.assertEquals(ANTE * 2, pot.getMoney());
    Assert.assertEquals(ANTE, pot.getCurrentBet());
    final Set<Player> expectedList = toSet(player1, player2);
    Assert.assertEquals(expectedList, pot.getParticipants());
    Assert.assertFalse("There shouldn't be a side pot", pot.hasSidePot());
    pot.checkPlayer(player1);
    pot.checkPlayer(player2);
    pot.newTurn();
    pot.raise(player1, 5);
    // player 2 folds
    final int winnings = pot.getTotalMoney();
    player1.win(winnings);
    pot.reset();
    Assert.assertEquals(205, player1.getMoney());
    Assert.assertEquals(195, player2.getMoney());
  }

  @Test
  public void testCheckingRound() {
    final Pot pot = new Pot();
    pot.collectAnte(player1, ANTE);
    pot.collectAnte(player2, ANTE);
    pot.collectAnte(player3, ANTE);
    pot.checkPlayer(player1);
    pot.checkPlayer(player2);
    pot.checkPlayer(player3);
    pot.newTurn();
    pot.checkPlayer(player1);
    pot.checkPlayer(player2);
    pot.checkPlayer(player3);
    Set<Player> expectedSet = toSet(player1, player2, player3);
    Assert.assertEquals(expectedSet, pot.getParticipants());
    Assert.assertEquals(ANTE*3, pot.getMoney());
    Assert.assertFalse("There shouldn't be a side pot", pot.hasSidePot());
  }

  @Test
  public void testRaisingAndCallingOnAnteRound() {
    final Pot pot = new Pot();
    pot.collectAnte(player1, ANTE);
    pot.collectAnte(player2, ANTE);
    pot.collectAnte(player3, ANTE);
    Assert.assertEquals(ANTE * 3, pot.getMoney());
    pot.raise(player1, 100);
    Assert.assertEquals((ANTE * 3)+100, pot.getMoney());
    Assert.assertEquals(ANTE + 100, pot.getCurrentBet());
    pot.call(player2);
    Assert.assertEquals("There should be 215 in the pot",(ANTE * 3)+200, pot.getMoney());
    Assert.assertEquals("The current bet should remain unchanged at 105",ANTE + 100, pot.getCurrentBet());
    pot.call(player3);
    Set<Player> expectedList = toSet(player1, player2, player3);
    Assert.assertEquals(expectedList, pot.getParticipants());
    Assert.assertEquals((ANTE * 3)+300, pot.getMoney());
    Assert.assertEquals(100 + ANTE, pot.getCurrentBet());
    Assert.assertFalse("There shouldn't be a side pot", pot.hasSidePot());
  }

  @Test
  public void testMakingSidePots() {
    final Player scrooge = new Player("Scrooge", 200);
    final Player donald = new Player("Donald", 100);
    final Pot pot = new Pot();
    pot.raise(scrooge, 150);
    pot.call(donald);
    Assert.assertEquals("There should only be 200 in the main pot", 200, pot.getMoney());
    Assert.assertEquals("The main pot bet should be floored to 100", 100, pot.getCurrentBet());
    Assert.assertTrue("There should be a sidepot", pot.hasSidePot());
    final Pot sidePot = pot.getSidePot();
    Assert.assertEquals("There should be 50 in the side pot", 50, sidePot.getMoney());
    Set<Player> everyone = toSet(scrooge, donald);
    Assert.assertEquals("Everyone should be in the main pot", everyone, pot.getParticipants());
    Set<Player> loneList = toSet(scrooge);
    Assert.assertEquals("Scrooge should be alone in the side pot", loneList, sidePot.getParticipants());
    Assert.assertEquals("The total pot size should be 250", 250, pot.getTotalMoney());
  }

  @Test
  public void testMakingSidePotsThreePlayers() {
    final Player scrooge = new Player("Scrooge", 500);
    final Player gearloose = new Player("Gearloose", 200);
    final Player donald = new Player("Donald", 50);
    final Set<Player> everyone = toSet(scrooge, gearloose, donald);
    final Pot pot = new Pot();
    pot.raise(scrooge, 100);
    pot.call(gearloose);
    pot.call(donald);
    Assert.assertEquals("There should only be 150 in the main pot", 150, pot.getMoney());
    Assert.assertEquals("The main pot bet should be floored to 50", 50, pot.getCurrentBet());
    Assert.assertTrue("There should be a sidepot", pot.hasSidePot());
    final Pot sidePot = pot.getSidePot();
    Assert.assertEquals("There should be 100 in the side pot", 100, sidePot.getMoney());
    Assert.assertEquals("Everyone should be in the main pot", everyone, pot.getParticipants());
    Set<Player> sidePotList = toSet(scrooge, gearloose);
    Assert.assertEquals("Scrooge and Gearloose should be in the side pot", sidePotList, sidePot.getParticipants());
  }

  @Test
  public void testMultipleSidepots() {
    final Player scrooge = new Player("Scrooge", 500);
    final Player gearloose = new Player("Gearloose", 200);
    final Player donald = new Player("Donald", 50);
    final Set<Player> everyone = toSet(scrooge, gearloose, donald);
    final Set<Player> secondPotParticipants = toSet(scrooge, gearloose);
    final Set<Player> thirdPotParticipants = toSet(scrooge);
    final Pot pot = new Pot();
    pot.raise(scrooge, 100);
    pot.call(gearloose);
    pot.call(donald); // 250
    pot.newTurn();
    final Pot firstSidePot = pot.getSidePot();
    Assert.assertEquals("There should be only 100 in the side pot at this point", 100, firstSidePot.getMoney());
    Assert.assertEquals("Only 250 has been put into the pot at this point", 250, pot.getTotalMoney());
    pot.raise(scrooge, 200);
    pot.call(gearloose); // 300
    pot.checkPlayer(donald);
    Assert.assertEquals("Everyone should be in the main pot", everyone, pot.getParticipants());
    Assert.assertEquals("There should only be 150 in the main pot", 150, pot.getMoney());
    Assert.assertEquals("Scrooge and Gearloose should be in the first sidepot", secondPotParticipants, firstSidePot.getParticipants());
    Assert.assertEquals("There should only be 250 in the first sidepot", 250, firstSidePot.getMoney());
    final Pot secondSidePot = firstSidePot.getSidePot();
    Assert.assertNotNull("There should be a second side pot", secondSidePot);
    Assert.assertEquals("Scrooge should be alone in the final side pot", thirdPotParticipants, secondSidePot.getParticipants());
    Assert.assertEquals("Everyone has put in 550", 550, pot.getTotalMoney());
    Assert.assertEquals("Scrooge should be alone in the last sidepot", thirdPotParticipants, secondSidePot.getParticipants());
  }
}
