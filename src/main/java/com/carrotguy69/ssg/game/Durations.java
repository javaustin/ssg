package com.carrotguy69.ssg.game;

import static com.carrotguy69.ssg.SpeedSG.configYML;

public class Durations {

    public Durations() {}

    public int lobbyCountdown = configYML.getInt("timers.lobby-countdown", 10);
    public int gameStartCountdown = configYML.getInt("timers.game-countdown", 10);
    public int invulCountdown = configYML.getInt("timers.invul-countdown", 15);
    public int chestRefillCountdown = configYML.getInt("timers.chest-refill", 150);
    public int showdownCountdown = configYML.getInt("timers.showdown", 300);
    public int gameEndCountdown = configYML.getInt("timers.game-end", 360);
}
