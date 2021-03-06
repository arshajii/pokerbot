package me.ars.pokerbot;

public class Player {
	private String name;
	private int money = Constants.START_MONEY;

	/*
	 * how much this player has payed in a given hand
	 */
	private int payed = 0;

	private Card card1, card2;
	private boolean active = true;
	private boolean folded = false;

	public Player(String name) {
		this.name = name;
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
}
