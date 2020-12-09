package me.ars.pokerbot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jibble.pircbot.Colors;

public class Hand implements Comparable<Hand> {
	enum HandType {
		// the order of these matters:
		HIGH_CARD, ONE_PAIR, TWO_PAIR, THREE_OF_KIND, STRAIGHT, FLUSH, FULL_HOUSE, FOUR_OF_KIND, STRAIGHT_FLUSH;

		@Override
		public String toString() {
			String str;

			switch (this) {
			case STRAIGHT_FLUSH:
				str = "straight flush";
				break;
			case FOUR_OF_KIND:
				str = "four of a kind";
				break;
			case FULL_HOUSE:
				str = "full house";
				break;
			case FLUSH:
				str = "flush";
				break;
			case STRAIGHT:
				str = "straight";
				break;
			case THREE_OF_KIND:
				str = "three of a kind";
				break;
			case TWO_PAIR:
				str = "two pair";
				break;
			case ONE_PAIR:
				str = "one pair";
				break;
			case HIGH_CARD:
				str = "high card";
				break;
			default:
				throw new IllegalStateException();
			}

			return Colors.BOLD + str + Colors.NORMAL;
		}
	}

	/*
	 * the player with the hand
	 */
	private final Player player;

	/*
	 * the type of hand
	 */
	private final HandType type;

	/*
	 * cards pertinent to the hand
	 */
	private final Card[] bestHand;

	private Hand(final Player player, final HandType type,
			final Card... bestHand) {
		if (bestHand.length != 5) {
			throw new IllegalArgumentException("Invalid hand size: " + bestHand.length);
		}

		this.player = player;
		this.type = type;
		this.bestHand = bestHand;
	}

	private Hand(final Player player, final HandType type,
			final List<Card> bestHandList) {
		this(player, type, bestHandList.toArray(new Card[bestHandList.size()]));
	}

	public Player getPlayer() {
		return player;
	}

	public HandType getHandType() {
		return type;
	}

	public Card[] getCards() {
		return Arrays.copyOf(bestHand, bestHand.length);
	}

	private static boolean isStraightAt(List<Card> uniqueCards, int index) {
		if (uniqueCards.size() - index < 5)
			return false;

		int upper = index + 4;
		for (int i = index; i < upper; i++) {
			int v1 = uniqueCards.get(i).getValue();
			int v2 = uniqueCards.get(i + 1).getValue();
			if (v1 + 1 != v2) {
				return false;
			}
		}

		return true;
	}

	/*
	 * @param cards the 7 cards to be analyzed
	 */
	public static Hand getBestHand(Player player, Card... cards) {
		final Hand hand1 = bestHand(player, cards);
		try {
			final Card[] acesAsOnes = countAcesAsOnes(cards);
			final Hand hand2 = bestHand(player, acesAsOnes);
			if (hand1.compareTo(hand2) > 0) {
				return hand1;
			} else {
				return hand2;
			}
		} catch (IllegalArgumentException e) {
			System.err.println("Error making hand with ones: " + e);
			e.printStackTrace();
			return hand1;
		}
	}

	private static Hand bestHand(Player player, Card... cards) {
		Arrays.sort(cards);

		int[] suitFreqs = new int[4];

		for (Card card : cards) {
			suitFreqs[card.getSuit().ordinal()]++;
		}

		Card.Suit flushSuit = null;
		List<Card> flushCards = new ArrayList<>(7);
		for (int i = 0; i < 4; i++) {
			if (suitFreqs[i] >= 5) {
				flushSuit = Card.Suit.values()[i];
			}
		}

		if (flushSuit != null) {
			for (Card card : cards) {
				if (card.getSuit() == flushSuit) {
					flushCards.add(card);
				}
			}
		}

		// straight flush check
		for (int i = flushCards.size() - 1; i >= 0; i--) {
			if (isStraightAt(flushCards, i)) {
				Card[] bestHand = new Card[5];

				for (int j = 0; j < 5; j++) {
					bestHand[j] = flushCards.get(i + j);
				}

				return new Hand(player, HandType.STRAIGHT_FLUSH, bestHand);
			}
		}

		int[] freqs = new int[13];

		if (containsOnes(cards)) {
			for (Card card : cards) {
				freqs[card.getValue() - 1]++;
			}
		} else {
			for (Card card : cards) {
				freqs[card.getValue() - 2]++;
			}
		}

		int maxIndex = 0;
		for (int i = 0; i < freqs.length; i++) {
			if (freqs[i] > freqs[maxIndex]) {
				maxIndex = i;
			}
		}
		int maxValue = maxIndex + 2;

		// four of a kind check
		if (freqs[maxIndex] == 4) {
			List<Card> bestHandList = new ArrayList<>(5);

			for (Card card : cards) {
				if (card.getValue() == maxValue) {
					bestHandList.add(card);
				}
			}

			Card kicker = null;

			for (Card card : cards) {
				if (card.getValue() == maxValue)
					continue;

				if (kicker == null || card.getValue() > kicker.getValue())
					kicker = card;
			}

			bestHandList.add(kicker);

			return new Hand(player, HandType.FOUR_OF_KIND, bestHandList);
		}

		int threes = -1;
		int secondthrees = -1;
		int twos = -1;
		int secondTwos = -1;

		for (int i = freqs.length - 1; i >= 0; i--) {
			final int freq = freqs[i];
			final int value = i + 2;
			if (freq == 3) {
				if (threes < 0) {
					threes = value;
				} else {
					secondthrees = value;
				}
			} else if (freq == 2) {
				if (twos < 0) {
					twos = value;
				} else {
					secondTwos = value;
				}
			}
		}

		// full house check
		if (threes >= 0 && (twos >= 0 || secondthrees >= 0)) {
			List<Card> l1 = new ArrayList<>(5);
			List<Card> l2 = new ArrayList<>(2);
			final boolean firstBigger = (threes > secondthrees);

			for (Card card : cards) {
				if (card.getValue() == threes) {
					if (l1.size() < 2 || firstBigger) {
						l1.add(card);
					}
				} else if (card.getValue() == twos) {
					l2.add(card);
				} else if(card.getValue() == secondthrees) {
					if (l2.size() < 2 || !firstBigger) {
						l2.add(card);
					}
				}
			}

			l1.addAll(l2);

			return new Hand(player, HandType.FULL_HOUSE, l1.toArray(new Card[l1
					.size()]));
		}

		// flush check
		if (flushSuit != null) {
			List<Card> bestHandList = new ArrayList<>(5);
			for (int i = cards.length - 1; i >= 0; i--) {
				if (cards[i].getSuit() == flushSuit)
					bestHandList.add(cards[i]);

				if (bestHandList.size() == 5)
					break;
			}

			return new Hand(player, HandType.FLUSH, bestHandList);
		}

		int valset = 0;
		List<Card> unique = new ArrayList<>(cards.length);

		for (Card card : cards) {
			int val = card.getValue();
			if ((valset & (1 << val)) == 0) {
				unique.add(card);
				valset &= (1 << val);
			}
		}

		int straightStart = -1;

		for (int i = unique.size() - 1; i >= 0; i--) {
			if (isStraightAt(unique, i)) {
				straightStart = i;
				break;
			}
		}

		// straight check
		if (straightStart >= 0) {
			Card[] bestHand = new Card[5];

			for (int i = 0; i < 5; i++) {
				bestHand[i] = unique.get(straightStart + i);
			}

			return new Hand(player, HandType.STRAIGHT, bestHand);
		}

		// three of a kind check
		if (threes >= 0) {
			List<Card> bestHandList = new ArrayList<>(5);

			for (Card card : cards) {
				if (card.getValue() == threes)
					bestHandList.add(card);
			}

			Card kicker = null;

			for (Card card : cards) {
				if (card.getValue() == threes)
					continue;

				if (kicker == null || card.getValue() > kicker.getValue())
					kicker = card;
			}

			bestHandList.add(kicker);

			Card kicker2 = null;

			for (Card card : cards) {
				if (card.getValue() == threes || card.equals(kicker))
					continue;

				if (kicker2 == null || card.getValue() > kicker2.getValue())
					kicker2 = card;
			}

			bestHandList.add(kicker2);

			return new Hand(player, HandType.THREE_OF_KIND, bestHandList);
		}

		if (twos >= 0) {
			// two pair check
			if (secondTwos >= 0) {
				List<Card> l1 = new ArrayList<>(5);
				List<Card> l2 = new ArrayList<>(2);

				for (Card card : cards) {
					if (card.getValue() == twos) {
						l1.add(card);
					} else if (card.getValue() == secondTwos) {
						l2.add(card);
					}
				}

				l1.addAll(l2);

				Card kicker = null;

				for (Card card : cards) {
					if (card.getValue() == twos
							|| card.getValue() == secondTwos)
						continue;

					if (kicker == null || card.getValue() > kicker.getValue())
						kicker = card;
				}

				l1.add(kicker);

				return new Hand(player, HandType.TWO_PAIR, l1);
			}
			// one pair check
			else {
				List<Card> bestHandList = new ArrayList<>(5);

				for (Card card : cards) {
					if (card.getValue() == twos)
						bestHandList.add(card);
				}

				for (int i = cards.length - 1; i >= 0; i--) {
					if (!bestHandList.contains(cards[i]))
						bestHandList.add(cards[i]);

					if (bestHandList.size() == 5)
						break;
				}

				return new Hand(player, HandType.ONE_PAIR, bestHandList);
			}
		}

		// high card
		Card[] bestHand = new Card[5];
		for (int i = 0; i < 5; i++) {
			bestHand[i] = cards[cards.length - i - 1];
		}

		return new Hand(player, HandType.HIGH_CARD, bestHand);
	}

	@Override
	public int compareTo(Hand other) {
		int master = type.compareTo(other.type);

		if (master != 0)
			return master;

		switch (type) {
		case STRAIGHT_FLUSH: {
			return bestHand[0].compareTo(other.bestHand[0]);
		}
		case FOUR_OF_KIND: {
			master = bestHand[0].compareTo(other.bestHand[0]);

			if (master == 0) {
				master = bestHand[4].compareTo(other.bestHand[4]);
			}

			return master;
		}
		case FULL_HOUSE: {
			master = bestHand[0].compareTo(other.bestHand[0]);

			if (master == 0) {
				master = bestHand[3].compareTo(other.bestHand[3]);
			}

			return master;
		}
		case FLUSH: {
			for (int i = 0; i < bestHand.length; i++) {
				master = bestHand[i].compareTo(other.bestHand[i]);

				if (master != 0)
					break;
			}

			return master;
		}
		case STRAIGHT: {
			return bestHand[0].compareTo(other.bestHand[0]);
		}
		case THREE_OF_KIND: {
			master = bestHand[0].compareTo(other.bestHand[0]);

			if (master == 0) {
				master = bestHand[3].compareTo(other.bestHand[3]);

				if (master == 0) {
					master = bestHand[4].compareTo(other.bestHand[4]);
				}
			}

			return master;
		}
		case TWO_PAIR: {
			master = bestHand[0].compareTo(other.bestHand[0]);

			if (master == 0) {
				master = bestHand[2].compareTo(other.bestHand[2]);

				if (master == 0) {
					master = bestHand[4].compareTo(other.bestHand[4]);
				}
			}

			return master;
		}
		case ONE_PAIR: {
			master = bestHand[0].compareTo(other.bestHand[0]);

			if (master == 0) {
				for (int i = 2; i < bestHand.length; i++) {
					master = bestHand[i].compareTo(other.bestHand[i]);

					if (master != 0)
						break;
				}
			}

			return master;
		}
		case HIGH_CARD: {
			for (int i = 0; i < bestHand.length; i++) {
				master = bestHand[i].compareTo(other.bestHand[i]);

				if (master != 0)
					return master;
			}
			return 0;
		}
		default:
			System.err.println("Couldn't compare types: " + type + " to " + other.type);
			throw new IllegalStateException();
		}
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof Hand) && compareTo((Hand) o) == 0;
	}

	@Override
	public int hashCode() {
		int[] hash = new int[bestHand.length + 1];

		for (int i = 0; i < bestHand.length; i++)
			hash[i] = bestHand[i].hashCode();

		hash[bestHand.length] = type.hashCode();

		return Arrays.hashCode(hash);
	}

	private static Card[] countAcesAsOnes(Card[] cards) {
		Card[] newCards = new Card[cards.length];
		for(int i = 0; i < cards.length; i++) {
			if (cards[i].getValue() == 14) {
				newCards[i] = new Card(1, cards[i].getSuit());
			} else {
				newCards[i] = cards[i];
			}
		}
		return newCards;
	}

	private static boolean containsOnes(Card[] cards) {
		for( Card card: cards) {
			if (card.getValue() == 1) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return Arrays.stream(bestHand).map(Card::toString)
				.collect(Collectors.joining(", "))
				+ " (" + type.toString() + ")";
	}
}
