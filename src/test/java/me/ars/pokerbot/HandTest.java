package me.ars.pokerbot;

import org.junit.Test;
import org.junit.Assert;

import me.ars.pokerbot.Card.Suit;
import me.ars.pokerbot.Hand.HandType;

public class HandTest {

  private final Player player = new Player("tester");

  @Test
  public void testHighCard() {
    final Card card1 = new Card(7, Suit.HEARTS);
    final Card card2 = new Card(9, Suit.CLUBS);
    final Card card3 = new Card(3, Suit.DIAMONDS);
    final Card card4 = new Card(5, Suit.SPADES);
    final Card card5 = new Card(13, Suit.HEARTS);
    final Hand hand = Hand.getBestHand(player, card1,card2,card3,card4,card5);
    Assert.assertEquals(HandType.HIGH_CARD, hand.getHandType());
  }

  @Test
  public void testFourOfAKind() {
    final Card card1 = new Card(7, Suit.HEARTS);
    final Card card2 = new Card(7, Suit.CLUBS);
    final Card card3 = new Card(7, Suit.DIAMONDS);
    final Card card4 = new Card(7, Suit.SPADES);
    final Card card5 = new Card(11, Suit.HEARTS);
    final Hand hand = Hand.getBestHand(player, card1,card2,card3,card4,card5);
    Assert.assertEquals(HandType.FOUR_OF_KIND, hand.getHandType());
  }

  @Test
  public void testStraightFlush() {
    final Card card1 = new Card(7, Suit.HEARTS);
    final Card card2 = new Card(8, Suit.HEARTS);
    final Card card3 = new Card(9, Suit.HEARTS);
    final Card card4 = new Card(10, Suit.HEARTS);
    final Card card5 = new Card(11, Suit.HEARTS);
    final Hand hand = Hand.getBestHand(player, card1,card2,card3,card4,card5);
    Assert.assertEquals(HandType.STRAIGHT_FLUSH, hand.getHandType());
  }

  @Test
  public void testFullHouse() {
    final Card card1 = new Card(10, Suit.HEARTS);
    final Card card2 = new Card(10, Suit.SPADES);
    final Card card3 = new Card(10, Suit.CLUBS);
    final Card card4 = new Card(7, Suit.HEARTS);
    final Card card5 = new Card(7, Suit.SPADES);
    final Hand hand = Hand.getBestHand(player, card1,card2,card3,card4,card5);
    Assert.assertEquals(HandType.FULL_HOUSE, hand.getHandType());
  }

  @Test
  public void testFullHouseWithAces() {
    final Card card1 = new Card(14, Suit.HEARTS);
    final Card card2 = new Card(14, Suit.SPADES);
    final Card card3 = new Card(14, Suit.CLUBS);
    final Card card4 = new Card(7, Suit.HEARTS);
    final Card card5 = new Card(7, Suit.SPADES);
    final Hand hand = Hand.getBestHand(player, card1,card2,card3,card4,card5);
    Assert.assertEquals(HandType.FULL_HOUSE, hand.getHandType());
  }

  @Test
  public void testOnePair() {
    final Card card1 = new Card(10, Suit.HEARTS);
    final Card card2 = new Card(10, Suit.SPADES);
    final Card card3 = new Card(12, Suit.CLUBS);
    final Card card4 = new Card(5, Suit.HEARTS);
    final Card card5 = new Card(7, Suit.HEARTS);
    final Hand hand = Hand.getBestHand(player, card1,card2,card3,card4,card5);
    Assert.assertEquals(HandType.ONE_PAIR, hand.getHandType());
  }

  @Test
  public void testTwoPair() {
    final Card card1 = new Card(10, Suit.HEARTS);
    final Card card2 = new Card(10, Suit.SPADES);
    final Card card3 = new Card(12, Suit.CLUBS);
    final Card card4 = new Card(12, Suit.HEARTS);
    final Card card5 = new Card(7, Suit.HEARTS);
    final Hand hand = Hand.getBestHand(player, card1,card2,card3,card4,card5);
    Assert.assertEquals(HandType.TWO_PAIR, hand.getHandType());
  }

  @Test
  public void testTwoPairAcesAndKings() {
    final Card card1 = new Card(14, Suit.HEARTS);
    final Card card2 = new Card(14, Suit.SPADES);
    final Card card3 = new Card(13, Suit.CLUBS);
    final Card card4 = new Card(13, Suit.HEARTS);
    final Card card5 = new Card(7, Suit.HEARTS);
    final Card card6 = new Card(2, Suit.SPADES);
    final Card card7 = new Card(5, Suit.CLUBS);
    final Hand hand = Hand.getBestHand(player, card1,card2,card3,card4,card5,card6,card7);
    Assert.assertEquals(HandType.TWO_PAIR, hand.getHandType());
  }

  @Test
  public void testTwoPairAcesAndTwos() {
    final Card card1 = new Card(14, Suit.HEARTS);
    final Card card2 = new Card(14, Suit.SPADES);
    final Card card3 = new Card(2, Suit.CLUBS);
    final Card card4 = new Card(2, Suit.HEARTS);
    final Card card5 = new Card(7, Suit.HEARTS);
    final Card card6 = new Card(10, Suit.SPADES);
    final Card card7 = new Card(5, Suit.CLUBS);
    final Hand hand = Hand.getBestHand(player, card1,card2,card3,card4,card5,card6,card7);
    Assert.assertEquals(HandType.TWO_PAIR, hand.getHandType());
  }

  @Test
  public void testThreeOfAKind() {
    final Card card1 = new Card(10, Suit.HEARTS);
    final Card card2 = new Card(10, Suit.SPADES);
    final Card card3 = new Card(10, Suit.CLUBS);
    final Card card4 = new Card(12, Suit.HEARTS);
    final Card card5 = new Card(7, Suit.HEARTS);
    final Hand hand = Hand.getBestHand(player, card1,card2,card3,card4,card5);
    Assert.assertEquals(HandType.THREE_OF_KIND, hand.getHandType());
  }

  @Test
  public void testFlush() {
    final Card card1 = new Card(10, Suit.HEARTS);
    final Card card2 = new Card(3, Suit.HEARTS);
    final Card card3 = new Card(9, Suit.HEARTS);
    final Card card4 = new Card(12, Suit.HEARTS);
    final Card card5 = new Card(7, Suit.HEARTS);
    final Hand hand = Hand.getBestHand(player, card1,card2,card3,card4,card5);
    Assert.assertEquals(HandType.FLUSH, hand.getHandType());
  }

  @Test
  public void testStraight() {
    final Card card1 = new Card(3, Suit.HEARTS);
    final Card card2 = new Card(4, Suit.SPADES);
    final Card card3 = new Card(5, Suit.HEARTS);
    final Card card4 = new Card(6, Suit.SPADES);
    final Card card5 = new Card(7, Suit.HEARTS);
    final Hand hand = Hand.getBestHand(player, card1,card2,card3,card4,card5);
    Assert.assertEquals(HandType.STRAIGHT, hand.getHandType());
  }

  @Test
  public void testStraight2() {
    final Card card1 = new Card(2, Suit.HEARTS);
    final Card card2 = new Card(4, Suit.CLUBS);
    final Card card3 = new Card(8, Suit.SPADES);
    final Card card4 = new Card(9, Suit.SPADES);
    final Card card5 = new Card(10, Suit.HEARTS);
    final Card card6 = new Card(11, Suit.SPADES);
    final Card card7 = new Card(12, Suit.HEARTS);
    final Hand hand = Hand.getBestHand(player, card1,card2,card3,card4,card5,card6,card7);
    Assert.assertEquals(HandType.STRAIGHT, hand.getHandType());
  }

  @Test
  public void testLowAceStraight() {
    final Card card1 = new Card(14, Suit.HEARTS);
    final Card card2 = new Card(2, Suit.HEARTS);
    final Card card3 = new Card(3, Suit.SPADES);
    final Card card4 = new Card(4, Suit.DIAMONDS);
    final Card card5 = new Card(5, Suit.CLUBS);
    final Card card6 = new Card(7, Suit.CLUBS);
    final Card card7 = new Card(8, Suit.SPADES);
    final Hand hand = Hand.getBestHand(player, card1,card2,card3,card4,card5,card6,card7);
    Assert.assertEquals(HandType.STRAIGHT, hand.getHandType());
  }

  @Test
  public void testHighAceStraight() {
    final Card card1 = new Card(14, Suit.HEARTS);
    final Card card2 = new Card(13, Suit.HEARTS);
    final Card card3 = new Card(12, Suit.SPADES);
    final Card card4 = new Card(11, Suit.DIAMONDS);
    final Card card5 = new Card(10, Suit.CLUBS);
    final Card card6 = new Card(7, Suit.CLUBS);
    final Card card7 = new Card(8, Suit.SPADES);
    final Hand hand = Hand.getBestHand(player, card1,card2,card3,card4,card5,card6,card7);
    Assert.assertEquals(HandType.STRAIGHT, hand.getHandType());
  }

  @Test
  public void testHighCardComparisonCrash() {
    /* This crash happened during play.
    1607432240313 ### java.lang.IllegalStateException
    1607432240313 ###       at me.ars.pokerbot.Hand.compareTo(Hand.java:464)
    1607432240313 ###       at me.ars.pokerbot.Hand.compareTo(Hand.java:10)
    1607432240313 ###       at java.util.Collections$ReverseComparator.compare(Collections.java:5117)
    1607432240313 ###       at java.util.Collections$ReverseComparator.compare(Collections.java:5108)
    1607432240313 ###       at java.util.TimSort.countRunAndMakeAscending(TimSort.java:355)
    1607432240313 ###       at java.util.TimSort.sort(TimSort.java:220)
    1607432240313 ###       at java.util.Arrays.sort(Arrays.java:1512)
    1607432240313 ###       at java.util.ArrayList.sort(ArrayList.java:1462)
    1607432240313 ###       at me.ars.pokerbot.Table.nextTurn(Table.java:284)
    1607432240314 ###       at me.ars.pokerbot.Table.check(Table.java:94)
    1607432240314 ###       at me.ars.pokerbot.IrcBot.onMessage(IrcBot.java:179)
    1607432240314 ###       at org.jibble.pircbot.PircBot.handleLine(PircBot.java:1017)
    1607432240314 ###       at org.jibble.pircbot.InputThread.run(InputThread.java:92)
    */
    final Card card1 = new Card(13, Suit.HEARTS);
    final Card card2 = new Card(12, Suit.HEARTS);
    final Card card3 = new Card(14, Suit.SPADES);
    final Card card4 = new Card(11, Suit.DIAMONDS);
    final Card card5 = new Card(9, Suit.CLUBS);
    final Card card6 = new Card(5, Suit.CLUBS);
    final Card card7 = new Card(8, Suit.SPADES);
    final Hand hand = Hand.getBestHand(player, card1, card2, card3, card4, card5, card6, card7);
    Assert.assertEquals(HandType.HIGH_CARD, hand.getHandType());
    final Card acard1 = new Card(13, Suit.HEARTS);
    final Card acard2 = new Card(12, Suit.HEARTS);
    final Card acard3 = new Card(14, Suit.SPADES);
    final Card acard4 = new Card(11, Suit.DIAMONDS);
    final Card acard5 = new Card(9, Suit.CLUBS);
    final Card acard6 = new Card(4, Suit.HEARTS);
    final Card acard7 = new Card(2, Suit.DIAMONDS);
    final Hand ahand = Hand.getBestHand(player, acard1, acard2, acard3, acard4, acard5, acard6, acard7);
    Assert.assertEquals(HandType.HIGH_CARD, hand.getHandType());

    // compareTo would crash.
    Assert.assertEquals(0, ahand.compareTo(hand));
  }

  @Test
  public void testTwoPairComparisonCrash() {
    /* This crash happened during play.
    607507385567 ### java.lang.IllegalArgumentException
    1607507385572 ###       at me.ars.pokerbot.Hand.<init>(Hand.java:73)
    1607507385572 ###       at me.ars.pokerbot.Hand.<init>(Hand.java:82)
    1607507385572 ###       at me.ars.pokerbot.Hand.bestHand(Hand.java:359)
    1607507385572 ###       at me.ars.pokerbot.Hand.getBestHand(Hand.java:119)
    1607507385572 ###       at me.ars.pokerbot.Table.nextTurn(Table.java:281)
    1607507385572 ###       at me.ars.pokerbot.Table.call(Table.java:73)
    1607507385572 ###       at me.ars.pokerbot.IrcBot.onMessage(IrcBot.java:170)
    1607507385578 ###       at org.jibble.pircbot.PircBot.handleLine(PircBot.java:1017)
    1607507385578 ###       at org.jibble.pircbot.InputThread.run(InputThread.java:92)
    */
    final Card card1 = new Card(2, Suit.DIAMONDS);
    final Card card2 = new Card(14, Suit.SPADES);
    final Card card3 = new Card(13, Suit.HEARTS);
    final Card card4 = new Card(11, Suit.DIAMONDS);
    final Card card5 = new Card(2, Suit.HEARTS);
    final Card card6 = new Card(12, Suit.CLUBS);
    final Card card7 = new Card(13, Suit.CLUBS);
    final Hand hand = Hand.getBestHand(player, card1, card2, card3, card4, card5, card6, card7);
    Assert.assertEquals(HandType.TWO_PAIR, hand.getHandType());
    final Card acard1 = new Card(2, Suit.DIAMONDS);
    final Card acard2 = new Card(14, Suit.SPADES);
    final Card acard3 = new Card(13, Suit.HEARTS);
    final Card acard4 = new Card(11, Suit.DIAMONDS);
    final Card acard5 = new Card(2, Suit.HEARTS);
    final Card acard6 = new Card(7, Suit.SPADES);
    final Card acard7 = new Card(7, Suit.DIAMONDS);
    final Hand ahand = Hand.getBestHand(player, acard1, acard2, acard3, acard4, acard5, acard6, acard7);
    Assert.assertEquals(HandType.TWO_PAIR, hand.getHandType());

    // compareTo would crash.
    Assert.assertEquals(-1, ahand.compareTo(hand));
  }
}
