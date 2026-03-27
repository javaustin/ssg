package com.carrotguy69.ssg.messages;

public enum MessageKey {
    LOBBY_CHAT("lobby.chat"),
    LOBBY_JOIN("lobby.join"),
    LOBBY_LEAVE("lobby.leave"),

    LOBBY_COUNTDOWN("lobby.info.countdown"),
    LOBBY_START_CANCELLED("lobby.info.start-cancel"),

    GAME_CHAT("game.chat"),
    GAME_JOIN("game.join"),
    GAME_LEAVE("game.leave"),

    GAME_INFO_BLURB("game.info.blurb"),
    ;

    private final String path;

    MessageKey(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
