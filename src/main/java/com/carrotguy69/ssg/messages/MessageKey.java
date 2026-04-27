package com.carrotguy69.ssg.messages;

public enum MessageKey {

    ALIVE_INDICATOR("game.alive-indicator"),
    DEAD_INDICATOR("game.dead-indicator"),

    LOBBY_CHAT("lobby.chat"),
    LOBBY_JOIN("lobby.join"),
    LOBBY_LEAVE("lobby.leave"),

    LOBBY_COUNTDOWN("lobby.info.start-countdown"),
    LOBBY_START_CANCELLED("lobby.info.start-cancel"),

    GAME_CHAT("game.chat"),
    GAME_JOIN("game.join"),
    GAME_LEAVE("game.leave"),

    INFO_MESSAGE("game.info.blurb"),

    INVUL_COUNTDOWN_MESSAGE("game.info.invul-countdown"),
    CHEST_REFILLED_MESSAGE("game.info.refill-announcement"),

    SHOWDOWN_MESSAGE("game.info.showdown-announcement"),
    SHOWDOWN_TITLE("game.info.showdown-title"),
    SHOWDOWN_SUBTITLE("game.info.showdown-subtitle"),


    DEATH_ANNOUNCEMENT_MELEE("game.death-announcement.player.melee"),
    DEATH_ANNOUNCEMENT_PROJECTILE("game.death-announcement.player.projectile"),
    DEATH_ANNOUNCEMENT_EXPLOSIVE("game.death-announcement.player.explosive"),
    DEATH_ANNOUNCEMENT_NATURAL("game.death-announcement.player.default"),

    DEATH_MESSAGE_MELEE("game.death-message.player.melee"),
    DEATH_MESSAGE_PROJECTILE("game.death-message.player.projectile"),
    DEATH_MESSAGE_EXPLOSIVE("game.death-message.player.explosive"),
    DEATH_MESSAGE_NATURAL("game.death-message.player.natural"),

    KILL_MESSAGE_MELEE("game.kill-message.player.melee"),
    KILL_MESSAGE_PROJECTILE("game.kill-message.player.projectile"),
    KILL_MESSAGE_EXPLOSIVE("game.kill-message.player.explosive"),
    KILL_MESSAGE_NATURAL("game.kill-message.player.default"),

    DEATH_ANNOUNCEMENT_TEAM("game.death-announcement.team.default"),
    DEATH_MESSAGE_TEAM("game.death-announcement.team.default"),
    KILL_MESSAGE_TEAM("game.kill-message.team.default"),

    WIN_TITLE("game.win.victory-title.title"),
    WIN_SUBTITLE("game.win.victory-title.subtitle"),
    WIN_FADE_IN_TICKS("game.win.victory-title.fade-in-ticks"),
    WIN_STAY_TICKS("game.win.victory-title.stay-ticks"),
    WIN_FADE_OUT_TICKS("game.win.victory-title.fade-out-ticks"),

    LOSE_TITLE("game.win.lose-title.title"),
    LOSE_SUBTITLE("game.win.lose-title.subtitle"),
    LOSE_FADE_IN_TICKS("game.win.lose-title.fade-in-ticks"),
    LOSE_STAY_TICKS("game.win.lose-title.stay-ticks"),
    LOSE_FADE_OUT_TICKS("game.win.lose-title.fade-out-ticks"),

    TEAM_LIST_ENTRY_FORMAT("game.win.recaps.team-list.entry-format"),
    TEAM_LIST_DELIMITER("game.win.recaps.team-list.separator"),
    TEAM_LIST_MAX_ENTRIES("game.win.recaps.team-list.max-entries"),

    TOP_KILLERS_LIST_ENTRY_FORMAT("game.win.recaps.top-killers-numbered-list.entry-format"),
    TOP_KILLERS_LIST_DELIMITER("game.win.recaps.top-killers-numbered-list.separator"),
    TOP_KILLERS_LIST_MAX_ENTRIES("game.win.recaps.top-killers-numbered-list.max-entries"),

    RECAP_SOLO_WINNER("game.win.recaps.solo-winner"),
    RECAP_TEAM_WINNER("game.win.recaps.team-winner"),

    ;

    private final String path;

    MessageKey(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
