package me.ars.pokerbot;

import java.util.Arrays;

import org.jibble.pircbot.Colors;

/*
 * Note: this class has a natural ordering that is inconsistent with equals.
 * 
 * Equality testing via the equals method compares both suit and value, whereas
 * compareTo uses only value.
 */
public class Card implements Comparable<Card> {
	public static enum Suit {
		SPADES, HEARTS, DIAMONDS, CLUBS;

		@Override
		public String toString() {
			switch (this) {
			case SPADES:
				return "\u2660";
			case HEARTS:
				return "\u2665";
			case DIAMONDS:
				return "\u2666";
			case CLUBS:
				return "\u2663";
			default:
				throw new IllegalStateException();
			}
		}
	}

	/*
	 * 2..10 = 2..10, 11 = jack, 12 = queen, 13 = king, 14 = ace
	 */
	private final int value;
	private final Suit suit;

	public Card(final int value, final Suit suit) {
		this.value = value;
		this.suit = suit;
	}

	public int getValue() {
		return value;
	}

	public Suit getSuit() {
		return suit;
	}

	@Override
	public String toString() {
		String color = (suit == Suit.HEARTS || suit == Suit.DIAMONDS) ? Colors.RED
				: Colors.BLACK;

		String valueStr;

		switch (value) {
		case 11:
			valueStr = "J";
			break;
		case 12:
			valueStr = "Q";
			break;
		case 13:
			valueStr = "K";
			break;
		case 14:
			valueStr = "A";
			break;
		default:
			valueStr = Integer.toString(value);
		}

		return Colors.BOLD + color + valueStr + suit + Colors.NORMAL;
	}

	@Override
	public int compareTo(Card other) {
		return Integer.compare(value, other.value);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Card) {
			Card c = (Card) o;
			return value == c.value && suit == c.suit;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 32 * suit.ordinal() + value;
	}

	private static final Card[] deck = new Card[52];

	public static Card[] getDeck() {
		if (deck[0] == null) {
			fillDeck();
		}
		return Arrays.copyOf(deck, deck.length);
	}

	private static void fillDeck() {
		int index = 0;
		for (Suit suit : Suit.values()) {
			for (int value = 2; value <= 14; value++) {
				deck[index++] = new Card(value, suit);
			}
		}
	}
}
