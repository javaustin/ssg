package com.carrotguy69.ssg.messages.utils;

import com.carrotguy69.ssg.game.GamePlayer;

import java.util.Map;

public class MapFormatters {
    public static Map<String, Object> gamePlayerFormatter(GamePlayer gp) {
        Map<String, Object> commonMap = com.carrotguy69.cxyz.messages.utils.MapFormatters.playerFormatter(gp.getNetworkPlayer());

        commonMap.put("player-team", gp.getTeam() != null ? gp.getTeam().getPrefix() : "");
        commonMap.put("player-team-prefix", gp.getTeam() != null ? gp.getTeam().getPrefix() : "");
        commonMap.put("player-team-color", gp.getTeam() != null ? gp.getTeam().getColor() : "");

        commonMap.put("player-dead", !gp.isAlive() ? "&7&lDEAD" : "");

        return commonMap;
    }
}