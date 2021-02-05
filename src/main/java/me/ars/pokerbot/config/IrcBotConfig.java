package me.ars.pokerbot.config;

public class IrcBotConfig {
    public class Irc {
        public String server;
        public Integer port;
        public String serverPassword;
        public String nick;
        public String channel;
        public String channelPassword;
        public String adminPassword;
        public char commandPrefix;
        public Boolean verbose = true;
    }

    public Irc irc;
    public GameConfig game;
}


