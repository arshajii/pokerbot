package me.ars.pokerbot.poker;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PotTest {

  private Player player1;
  private Player player2;
  private Player player3;

  private static final int ANTE = 5;

  public static <T> List<T> toList(T... stuff) {
    List<T> list = new ArrayList<>();
    Collections.addAll(list, stuff);
    return list;
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
    List<Player> expectedList = toList(player1, player2, player3);
    Assert.assertEquals(expectedList, pot.getParticipants());
    Assert.assertFalse("There shouldn't be a side pot", pot.hasSidePot());
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
    List<Player> expectedList = toList(player1, player2, player3);
    Assert.assertEquals(expectedList, pot.getParticipants());
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
    pot.call(player2, 100);
    Assert.assertEquals("There should be 215 in the pot",(ANTE * 3)+200, pot.getMoney());
    Assert.assertEquals("The current bet should remain unchanged at 105",ANTE + 100, pot.getCurrentBet());
    pot.call(player3, 100);
    List<Player> expectedList = toList(player1, player2, player3);
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
    pot.call(donald, 100);
    Assert.assertEquals("There should only be 200 in the main pot", 200, pot.getMoney());
    Assert.assertEquals("The main pot bet should be floored to 100", 100, pot.getCurrentBet());
    Assert.assertTrue("There should be a sidepot", pot.hasSidePot());
    final Pot sidePot = pot.getSidePot();
    Assert.assertEquals("There should be 50 in the side pot", 50, sidePot.getMoney());
    List<Player> everyone = toList(scrooge, donald);
    Assert.assertEquals("Everyone should be in the main pot", everyone, pot.getParticipants());
    List<Player> loneList = toList(scrooge);
    Assert.assertEquals("Scrooge should be alone in the side pot", loneList, sidePot.getParticipants());
    Assert.assertEquals("The total pot size should be 250", 250, pot.getTotalMoney());
  }

  @Test
  public void testMakingSidePotsThreePlayers() {
    final Player scrooge = new Player("Scrooge", 500);
    final Player gearloose = new Player("Gearloose", 200);
    final Player donald = new Player("Donald", 50);
    final List<Player> everyone = toList(scrooge, gearloose, donald);
    final Pot pot = new Pot();
    pot.raise(scrooge, 100);
    pot.call(gearloose, 100);
    pot.call(donald, 50);
    Assert.assertEquals("There should only be 150 in the main pot", 150, pot.getMoney());
    Assert.assertEquals("The main pot bet should be floored to 50", 50, pot.getCurrentBet());
    Assert.assertTrue("There should be a sidepot", pot.hasSidePot());
    final Pot sidePot = pot.getSidePot();
    Assert.assertEquals("There should be 100 in the side pot", 100, sidePot.getMoney());
    Assert.assertEquals("Everyone should be in the main pot", everyone, pot.getParticipants());
    List<Player> sidePotList = toList(scrooge, gearloose);
    Assert.assertEquals("Scrooge and Gearloose should be in the side pot", sidePotList, sidePot.getParticipants());
  }

  @Test
  public void testMultipleSidepots() {
    final Player scrooge = new Player("Scrooge", 500);
    final Player gearloose = new Player("Gearloose", 200);
    final Player donald = new Player("Donald", 50);
    final List<Player> everyone = toList(scrooge, gearloose, donald);
    final List<Player> secondPotParticipants = toList(scrooge, gearloose);
    final List<Player> thirdPotParticipants = toList(scrooge);
    final Pot pot = new Pot();
    pot.raise(scrooge, 100);
    pot.call(gearloose, 100);
    pot.call(donald, 50);
    pot.newTurn();
    pot.raise(scrooge, 150);
    pot.call(gearloose, 100);
    pot.checkPlayer(donald);

    Assert.assertEquals("There should only be 150 in the main pot", 150, pot.getMoney());
    Assert.assertEquals("Everyone should be in the main pot", everyone, pot.getParticipants());
    final Pot firstSidePot = pot.getSidePot();
    Assert.assertEquals("Scrooge and Gearloose should be in the first sidepot", secondPotParticipants, firstSidePot.getParticipants());
    Assert.assertEquals("There should only be 200 in the first sidepot", 200, firstSidePot.getMoney());



  }
}