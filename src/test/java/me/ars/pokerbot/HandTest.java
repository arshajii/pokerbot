package me.ars.pokerbot;

import org.junit.Test;
import org.junit.Assert;

import me.ars.pokerbot.Card.Suit;
import me.ars.pokerbot.Hand.HandType;

public class HandTest {

  private final Player player = new Player("tester");

/*
todo:
case HIGH_CARD:
*/
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
}
