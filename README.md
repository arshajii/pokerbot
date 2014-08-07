PokerBot -- An IRC Croupier
===========================

PokerBot is a lightweight IRC bot used for playing text-based poker (specifically, Texas hold 'em), built on top of the [PircBot framework](http://www.jibble.org/pircbot.php).


Details
--------

### Commands

Commands are issued by prefixing a command keyword with a predefined command prefix (`.` by default). Available commands are listed below.

#### General Commands

Command Keyword | Description
--------|------------
`ping` | Ping the bot for a reply.
`gamechan`* | Change the game channel to the specified channel.
`join` | Add yourself to the players list for the next game.
`unjoin` | Remove yourself from the players list for the next game.
`joined` | Display who is in the players list for the next game.
`clear`* | Clear the players list for the next game.
`start`* | Start the game.
`stop`* | Stop the game.
`help` | Display help information.

<sup>* Requires administrative access.

#### Game Commands

Command Keyword | Description
--------|------------
`call` | Match the current bet.
`check` | Raise nothing, pass on to the next player.
`raise` | Raise by the specified amount *on top of* the last raise (which may have been 0).
`fold` | Discard your hand and forfeit. You can resume playing next hand.
`cashout` | Quit the game, taking the fortunes you've won with you.

### Authentication

The bot operates using a hostname-based authentication system. To register yourself as an administrator, simply issue the special `auth` command followed by the admin password in a private-message to the bot.

### Customization

The bot's parameters (e.g. name, game channel, command prefix, admin key) can be customized through the `Constants.java` source file.


Requirements
------------

- PircBot library
- Java 8


To-do
-----

- [ ] Side pot logic
- [ ] Small and big blinds
- [ ] Further separate front-end (bot) and back-end (poker engine)


Issues
------

PokerBot is still in its early stages of development, so there will likely be various bugs that have been overlooked. If you happen to find one, please submit an issue about it.

Feel free to also submit an issue to request a feature that does not exist, or to request an enhancement to an existing feature.



