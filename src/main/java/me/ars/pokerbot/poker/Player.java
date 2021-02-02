package me.ars.pokerbot.poker;

import me.ars.pokerbot.Constants;

import java.util.Objects;

public class Player {
	private final String name;
	private int money = Constants.START_MONEY;

	/*
	 * how much this player has payed in a given hand
	 */
	private int payed = 0;

	private Card card1, card2;
	private boolean active = true;
	private boolean folded = false;
	private boolean isAllIn = false;

	@Override
	public String toString() {
		return name;
	}

	public Player(String name) {
		this.name = name;
	}

	public Player(String name, int startMoney) {
		this.name = name;
		this.money = startMoney;
	}

	public boolean isAllIn() {
		return isAllIn;
	}

	public void setAllIn(boolean allIn) {
		isAllIn = allIn;
	}

	public String getName() {
		return name;
	}

	public int getMoney() {
		return money;
	}

	public boolean isBroke() {
		return money == 0;
	}

	public int getAmountPayed() {
		return payed;
	}

	public void newHand() {
		payed = 0;
		folded = false;
	}

	public void receiveCards(Card card1, Card card2) {
		this.card1 = card1;
		this.card2 = card2;
	}

	public Card getCard1() {
		return card1;
	}

	public Card getCard2() {
		return card2;
	}

	public boolean isFolded() {
		return folded;
	}

	public boolean isActive() {
		return active;
	}

	public int bet(int amount) {
		if (!active)
			return 0;

		payed += amount;
		money -= amount;
		return amount;
	}

	public void win(int pot) {
		if (!active)
			return;

		money += pot;
	}

	public void cashout() {
		fold();
		active = false;
	}

	public void fold() {
		folded = true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Player player = (Player) o;
		return name.equals(player.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}
}
