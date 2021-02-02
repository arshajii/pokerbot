package me.ars.pokerbot.poker;

import java.util.List;
import java.util.Map;

public interface StateCallback {
    /**
     * A player has called a bet. If the player didn't have enough money, then [money] will be lower than [owed].
     *
     * @param nick  Nickname of caller
     * @param money Amount called
     */
    void playerCalled(String nick, int money);

    /**
     * A player has raised a bet.
     *
     * @param name     Player who raised
     * @param newRaise Amount that was raised
     */
    void playerRaised(String name, int newRaise);

    /**
     * A player has checked.
     *
     * @param name Checking player
     */
    void playerChecked(String name);

    /**
     * Announce a message to all players.
     *
     * @param message Message to announce
     */
    void announce(String message);

    /**
     * Update what's shown to be on the table.
     *
     * @param table         List of cards on the table (may be empty)
     * @param pot           Current pot on the table
     * @param currentPlayer Current players turn
     */
    void updateTable(List<Card> table, int pot, String currentPlayer);

    /**
     * Notify that a player must call a raise.
     *
     * @param name       Player who needs to call
     * @param amountOwed Amount of money needed to call
     */
    void mustCallRaise(String name, int amountOwed);

    /**
     * Announce that a player could not raise the specified bet.
     *
     * @param name   Player who tried to bet
     * @param money  Amount of money they actually had
     */
    void playerCannotRaise(String name, int money);

    /**
     * A player has gone all in.
     *
     * @param name Name of allin player
     */
    void playerAllin(String name);

    /**
     * A player has folded.
     *
     * @param name Name of folding player
     */
    void playerFolded(String name);

    /**
     * A player cashed out and left the table.
     *
     * @param name  Name of cashing out player
     * @param money Amount of money they walked away with
     */
    void playerCashedOut(String name, int money);

    /**
     * Show the player the two cards they were dealt.
     *
     * @param name  Name of player
     * @param card1 First card
     * @param card2 Second card
     */
    void showPlayerCards(String name, Card card1, Card card2);

    /**
     * Display the currently playing players and the money they have
     *
     * @param players Map of player names to how much money they have
     */
    void showPlayers(Map<String, Integer> players);

    /**
     * Reveals the hands of the supplied players.
     *
     * @param reveal Names mapped to their own cards
     */
    void revealPlayers(Map<String, List<Card>> reveal);

    /**
     * Declare that a player has won the pot
     *
     * @param name        Name of winner
     * @param winningHand Their winning hand
     * @param pot         Money they've won from the pot
     */
    void declareWinner(String name, Hand winningHand, int pot);

    /**
     * Declare that there are multiple winners splitting the pot
     *
     * @param winners  Name of all winners
     * @param handType The hand type they had in common
     * @param pot      The pot they are splitting
     */
    void declareSplitPot(List<String> winners, Hand.HandType handType, int pot);

    /**
     * Declares whos turn it is.
     *
     * @param player Current player
     */
    void declarePlayerTurn(String player);

    /**
     * Declare that ante is being collected
     *
     * @param ante How much money the ante is
     */
    void collectAnte(int ante);

    /**
     * Declare that the blinds are being collected
     *
     * @param bigBlindPlayer   The player that pays the big blind
     * @param bigBlind         How big the big blind is
     * @param smallBlindPlayer The player that pays the small blind
     * @param smallBlind       How big the small blind is
     */
    void collectBlinds(String bigBlindPlayer, int bigBlind, String smallBlindPlayer, int smallBlind);
}
