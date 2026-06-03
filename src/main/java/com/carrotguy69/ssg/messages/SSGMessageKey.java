package com.carrotguy69.ssg.messages;

public enum SSGMessageKey {

    ALIVE_INDICATOR("indicators.alive-indicator"),
    DEAD_INDICATOR("indicators.dead-indicator"),

    LOBBY_CHAT("lobby.chat"),
    LOBBY_JOIN("lobby.join"),
    LOBBY_LEAVE("lobby.leave"),

    LOBBY_COUNTDOWN("lobby.info.start-countdown"),
    LOBBY_START_CANCELLED("lobby.info.start-cancel"),

    TEAM_JOIN("lobby.team.join"),
    TEAM_JOIN_ANNOUNCEMENT("lobby.team.join-announcement"),

    TEAM_LEAVE("lobby.team.leave"),
    TEAM_LEAVE_ANNOUNCEMENT("lobby.team.leave-announcement"),

    TEAM_LIST_PLAYERS("lobby.team.list.list"),

    TEAM_LIST_PLAYERS_ENTRY_FORMAT("lobby.team.list.team-members-list.entry-format"),
    TEAM_LIST_PLAYERS_DELIMITER("lobby.team.list.team-members-list.separator"),
    TEAM_LIST_PLAYERS_MAX_ENTRIES("lobby.team.list.team-members-list.max-entries"),


    GAME_CHAT("game.chat"),
    GAME_JOIN("game.join"),
    GAME_LEAVE("game.leave"),

    INFO_BLURB("game.info.blurb"),
    INFO_GAME_START_CANCEL("game.info.start-cancel"),
    INVUL_COUNTDOWN_MESSAGE("game.info.invul-countdown"),
    CHEST_REFILLED_MESSAGE("game.info.refill-announcement"),

    MID_GAME_JOIN_TITLE("game.info.mid-game-join.title.title"),
    MID_GAME_JOIN_SUBTITLE("game.info.mid-game-join.title.subtitle"),
    MID_GAME_JOIN_FADE_IN_TICKS("game.info.mid-game-join.title.fade-in-ticks"),
    MID_GAME_JOIN_STAY_TICKS("game.info.mid-game-join.title.stay-ticks"),
    MID_GAME_JOIN_FADE_OUT_TICKS("game.info.mid-game-join.title.fade-out-ticks"),

    INFO_MID_GAME_JOIN_MESSAGE("game.info.mid-game-join.message"),

    SHOWDOWN_TITLE("game.info.showdown.title.title"),
    SHOWDOWN_SUBTITLE("game.info.showdown.title.subtitle"),
    SHOWDOWN_FADE_IN_TICKS("game.info.showdown.title.fade-in-ticks"),
    SHOWDOWN_STAY_TICKS("game.info.showdown.title.stay-ticks"),
    SHOWDOWN_FADE_OUT_TICKS("game.info.showdown.title.fade-out-ticks"),

    SHOWDOWN_MESSAGE("game.info.showdown.message"),

    DEATH_ANNOUNCEMENT_MELEE("game.death.announcement.player.melee"),
    DEATH_ANNOUNCEMENT_PROJECTILE("game.death.announcement.player.projectile"),
    DEATH_ANNOUNCEMENT_EXPLOSIVE("game.death.announcement.player.explosive"),
    DEATH_ANNOUNCEMENT_NATURAL("game.death.announcement.player.default"),

    DEATH_MESSAGE_MELEE("game.death.message.player.melee"),
    DEATH_MESSAGE_PROJECTILE("game.death.message.player.projectile"),
    DEATH_MESSAGE_EXPLOSIVE("game.death.message.player.explosive"),
    DEATH_MESSAGE_NATURAL("game.death.message.player.natural"),

    KILL_MESSAGE_MELEE("game.kill.message.player.melee"),
    KILL_MESSAGE_PROJECTILE("game.kill.message.player.projectile"),
    KILL_MESSAGE_EXPLOSIVE("game.kill.message.player.explosive"),
    KILL_MESSAGE_NATURAL("game.kill.message.player.default"),

    DEATH_ANNOUNCEMENT_TEAM("game.death.announcement.team.default"),
    DEATH_MESSAGE_TEAM("game.death.message.team.default"),
    KILL_MESSAGE_TEAM("game.kill.message.team.default"),

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

    DEATH_RESPAWN_TITLE("game.death.title-with-respawn.title"),
    DEATH_RESPAWN_SUBTITLE("game.death.title-with-respawn.substitle"),
    DEATH_RESPAWN_FADE_IN_TICKS("game.death.title-with-respawn.fade-in-ticks"),
    DEATH_RESPAWN_STAY_TICKS("game.death.title-with-respawn.stay-ticks"),
    DEATH_RESPAWN_FADE_OUT_TICKS("game.death.title-with-respawn.fade-out-ticks"),

    DEATH_NO_RESPAWN_TITLE("game.death-title-with-no-respawn.title"),
    DEATH_NO_RESPAWN_SUBTITLE("game.death-title-with-no-respawn.substitle"),
    DEATH_NO_RESPAWN_FADE_IN_TICKS("game.death-title-with-no-respawn.fade-in-ticks"),
    DEATH_NO_RESPAWN_STAY_TICKS("game.death-title-with-no-respawn.stay-ticks"),
    DEATH_NO_RESPAWN_FADE_OUT_TICKS("game.death-title-with-no-respawn.fade-out-ticks"),

    RESPAWN_TITLE("game.respawn.title.title"),
    RESPAWN_SUBTITLE("game.respawn.title.substitle"),
    RESPAWN_FADE_IN_TICKS("game.respawn.title.fade-in-ticks"),
    RESPAWN_STAY_TICKS("game.respawn.title.stay-ticks"),
    RESPAWN_FADE_OUT_TICKS("game.respawn.title.fade-out-ticks"),

    RESPAWN_MESSAGE("game.respawn-message"),

    TOP_KILLERS_LIST_ENTRY_FORMAT("game.recaps.top-killers-numbered-list.entry-format"),
    TOP_KILLERS_LIST_DELIMITER("game.recaps.top-killers-numbered-list.separator"),
    TOP_KILLERS_LIST_MAX_ENTRIES("game.recaps.top-killers-numbered-list.max-entries"),

    TEAM_LIST_ENTRY_FORMAT("game.recaps.team-list.entry-format"),
    TEAM_LIST_DELIMITER("game.recaps.team-list.separator"),
    TEAM_LIST_MAX_ENTRIES("game.recaps.team-list.max-entries"),


    RECAP_SOLO_WINNER("game.recaps.solo-winner"),
    RECAP_TEAM_WINNER("game.recaps.team-winner"),

    INVALID_GAME("errors.args.invalid.game"),
    INVALID_MAP("errors.args.invalid.map"),
    INVALID_TEAM("errors.args.invalid.team"),
    INVALID_LOOT_TABLE("errors.args.invalid.loot-table"),
    INVALID_TEAM_CAPACITY("errors.args.invalid.team-capacity"),

    ERROR_NO_GAMES("errors.game.no-games"),
    ERROR_DUPLICATE_GAME("errors.game.duplicate-game"),
    ERROR_DUPLICATE_GAME_JOIN("errors.game.duplicate-game-join"),
    ERROR_NOT_IN_GAME("errors.game.not-in-game"),

    ERROR_TEAM_NO_SWITCHING("errors.team.no-switching"),
    ERROR_TEAM_FULL("errors.team.full"),
    ERROR_TEAM_NOT_IN_TEAM("errors.team.not-in-team"),
    ERROR_TEAM_ALREADY_IN_TEAM("errors.team.already-in-team"),

    COMMAND_NO_ACCESS("errors.command.no-access"),
    COMMAND_PLAYER_ONLY("errors.command.player-only"),

    ERROR_PLAYER_IS_OFFLINE("errors.player.is-offline"),
    ERROR_PLAYER_IS_SELF("errors.player.is-self"),
    ERROR_PLAYER_NOT_FOUND("errors.player.not-found"),

    MISSING_GENERAL("errors.args.missing.general"),

    COMMAND_CREATE_GAME("command.create"),
    COMMAND_DELETE_GAME("command.delete"),
    COMMAND_JOIN_GAME("command.join"),
    COMMAND_LEAVE_GAME("command.leave")

    ;

    private final String path;

    SSGMessageKey(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
