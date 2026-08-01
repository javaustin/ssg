package com.carrotguy69.ssg.messages.utils;

import com.carrotguy69.cxyz.utils.TimeUtils;
import com.carrotguy69.ssg.game.Game;
import com.carrotguy69.ssg.game.GamePlayer;
import com.carrotguy69.ssg.game.GameState;
import com.carrotguy69.ssg.game.GameTeam;
import com.carrotguy69.ssg.messages.MessageGrabber;
import com.carrotguy69.ssg.messages.SSGMessageKey;
import com.carrotguy69.ssg.utils.objects.ColorUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapFormatters {

    public static Map<String, Object> gamePlayerFormatter(@NotNull GamePlayer gp) {
        Map<String, Object> commonMap = com.carrotguy69.cxyz.messages.utils.MapFormatters.playerFormatter(gp.getNetworkPlayer());

        if (gp.getTeam() != null)
            commonMap.putAll(cloneFormaterToNewKey(teamFormatter(gp.getTeam()), "team", "player-team"));
        else
            commonMap.putAll(cloneFormaterToNewKey(teamFormatter(null), "team", "player-team"));

        String aliveIndicator = MessageGrabber.grab(SSGMessageKey.ALIVE_INDICATOR) != null ? MessageGrabber.grab(SSGMessageKey.ALIVE_INDICATOR) : "";
        String deadIndicator = MessageGrabber.grab(SSGMessageKey.DEAD_INDICATOR) != null ? MessageGrabber.grab(SSGMessageKey.DEAD_INDICATOR) : "&7&lDEAD ";
        String readyIndicator = MessageGrabber.grab(SSGMessageKey.READY_INDICATOR) != null ? MessageGrabber.grab(SSGMessageKey.READY_INDICATOR) : "";
        String notReadyIndicator = MessageGrabber.grab(SSGMessageKey.NOT_READY_INDICATOR) != null ? MessageGrabber.grab(SSGMessageKey.NOT_READY_INDICATOR) : "";

        commonMap.put("player-dead", !gp.isAlive() ? deadIndicator : aliveIndicator);

        commonMap.put("player-ready", gp.isReady() ? readyIndicator : notReadyIndicator);

        commonMap.put("player-health", String.format("%.1f", gp.getBukkitPlayer().getHealth()));
        commonMap.put("player-hp", String.format("%.1f", gp.getBukkitPlayer().getHealth()));

        commonMap.put("player-lives", gp.getLives());

        for (Map.Entry<String, Double> entry : gp.getTemporaryStat().entrySet()) {
            String key = entry.getKey();
            double val = entry.getValue();

            if ((BigDecimal.valueOf(val)).stripTrailingZeros().scale() > 0)
                commonMap.put("player-temp-stat-" + key, String.format("%.1f", val));

            else
                commonMap.put("player-temp-stat-" + key, String.format("%.0f", val));
        }

        return commonMap;
    }

    public static Map<String, Object> teamFormatter(@Nullable GameTeam gt) {
        // We fill these with ternary operators with default="" because sometimes there is not a team, and we'd rather have the placeholders filled as "" than remaining.

        Map<String, Object> commonMap = new HashMap<>();

        String aliveIndicator = MessageGrabber.grab(SSGMessageKey.ALIVE_INDICATOR) != null ? MessageGrabber.grab(SSGMessageKey.ALIVE_INDICATOR) : "";
        String deadIndicator = MessageGrabber.grab(SSGMessageKey.DEAD_INDICATOR) != null ? MessageGrabber.grab(SSGMessageKey.DEAD_INDICATOR) : "&7&lDEAD ";

        String name = gt != null ? gt.getName() : "&fN/A";

        commonMap.put("team", name);
        commonMap.put("team-prefix", name);
        commonMap.put("team-name", name);

        commonMap.put("team-stripped", name.strip());
        commonMap.put("team-prefix-stripped", name.strip());
        commonMap.put("team-name-stripped", name.strip());

        commonMap.put("team-short-name-stripped", gt != null ? gt.getShortName().strip() : "");

        commonMap.put("team-short-name", gt != null ? gt.getShortName() : "");
        commonMap.put("team-color", gt != null ? ColorUtils.getColorCode(gt.getRGBColor()) : "");
        commonMap.put("team-capacity", gt != null ? gt.getCapacity() : "");

        commonMap.put("team-dead", gt != null ?(!gt.isAlive() ? deadIndicator : aliveIndicator): "");

        if (gt != null) {
            for (Map.Entry<String, Double> entry : gt.getStats().entrySet()) {
                String key = entry.getKey();
                double val = entry.getValue();

                if (String.valueOf(val).contains(".")) {
                    commonMap.put("team-stat-" + key, String.format("%.1f", val));
                }
                else {
                    commonMap.put("team-stat-" + key, String.format("%.0f", val));
                }
            }
        }

        return commonMap;
    }

    public static com.carrotguy69.cxyz.messages.utils.MapFormatters.ListFormatter gamePlayerListFormatter(List<GamePlayer> players, String format, String delimiter, int maxEntriesPerPage, int pageNumber) {

        if (maxEntriesPerPage < 1) {
            maxEntriesPerPage = 9999;
        }

        int size = players.size();

        int startIndex = Math.max((pageNumber - 1) * maxEntriesPerPage, 0);
        int endIndex = Math.max(Math.min((pageNumber * maxEntriesPerPage) - 1, size - 1), 0);


        List<String> strings = new ArrayList<>(); // Each string contains the specified format with keys replaced with enumerated ones: "{player-color}{player}" -> "{player-color-0}{rank-0}"

        Map<String, Object> commonMap = new HashMap<>(); // Will represent all the placeholder keys and values we will fulfill at parse time.

        if (players.isEmpty()) {
            return new com.carrotguy69.cxyz.messages.utils.MapFormatters.ListFormatter(List.of(), delimiter, commonMap, maxEntriesPerPage, pageNumber);
        }

        for (int i = startIndex; i <= endIndex; i++) {

            String string = format; // Individual GamePlayer string
            GamePlayer gp = players.get(i);

            for (Map.Entry<String, Object> entry : gamePlayerFormatter(gp).entrySet()) { // Add all keys and values from the single rank map formatter
                string = string.replace("{" + entry.getKey() + "}", "{" + entry.getKey() + "-" + i + "}"); // Enumerate placeholders in format string
                commonMap.put(entry.getKey() + "-" + i, entry.getValue()); // Add enumerated placeholders to commonMap.
            }

            strings.add(string);
        }


        return new com.carrotguy69.cxyz.messages.utils.MapFormatters.ListFormatter(strings, delimiter, commonMap, maxEntriesPerPage, pageNumber);
    }

    public static com.carrotguy69.cxyz.messages.utils.MapFormatters.NumberedListFormatter gamePlayerNumberedListFormatter(List<GamePlayer> players, String format, String delimiter, int maxEntriesPerPage, int pageNumber) {
        com.carrotguy69.cxyz.messages.utils.MapFormatters.ListFormatter formatter = gamePlayerListFormatter(players, format, delimiter, maxEntriesPerPage, pageNumber);

        return new com.carrotguy69.cxyz.messages.utils.MapFormatters.NumberedListFormatter(formatter.getEntries(), formatter.getDelimiter(), formatter.getFormatMap(), maxEntriesPerPage);
    }

    public static com.carrotguy69.cxyz.messages.utils.MapFormatters.ListFormatter gameListFormatter(List<Game> games, String format, String delimiter, int maxEntriesPerPage, int pageNumber) {
        if (maxEntriesPerPage < 1) {
            maxEntriesPerPage = 9999;
        }

        int size = games.size();

        int startIndex = Math.max((pageNumber - 1) * maxEntriesPerPage, 0);
        int endIndex = Math.max(Math.min((pageNumber * maxEntriesPerPage) - 1, size - 1), 0);


        List<String> strings = new ArrayList<>(); // Each string contains the specified format with keys replaced with enumerated ones: "{player-color}{player}" -> "{player-color-0}{rank-0}"

        Map<String, Object> commonMap = new HashMap<>(); // Will represent all the placeholder keys and values we will fulfill at parse time.

        for (int i = startIndex; i <= endIndex; i++) {

            String string = format; // Individual GamePlayer string
            Game game = games.get(i);

            for (Map.Entry<String, Object> entry : gameFormatter(game).entrySet()) { // Add all keys and values from the single rank map formatter
                string = string.replace("{" + entry.getKey() + "}", "{" + entry.getKey() + "-" + i + "}"); // Enumerate placeholders in format string
                commonMap.put(entry.getKey() + "-" + i, entry.getValue()); // Add enumerated placeholders to commonMap.
            }

            strings.add(string);
        }

        return new com.carrotguy69.cxyz.messages.utils.MapFormatters.ListFormatter(strings, delimiter, commonMap, maxEntriesPerPage, pageNumber);
    }

    public static Map<String, Object> cloneFormaterToNewKey(Map<String, Object> originalMap, String fromKey, String toKey) {
        // Returns a new map with identical values but with keys renamed by replacing a given prefix/identifier (fromKey) with a new one (toKey).
        // e.g.: clonePlayerFormatter(playerFormatter(np), "player", "mod") -> {player} will be {mod}, {player-prefix} will be {mod-prefix}

        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<String, Object> entry : originalMap.entrySet()) {
            String newKey = entry.getKey().replace(fromKey, toKey);
            Object value = entry.getValue();

            result.put(newKey, value);
        }

        return result;
    }

    public static Map<String, Object> gameFormatter(Game game) {
        Map<String, Object> commonMap = new HashMap<>();

        commonMap.put("game", game.getGameID());
        commonMap.put("game-id", game.getGameID());
        commonMap.put("game-map", game.getGameMap().getName());
        commonMap.put("game-team-capacity", teamCapacityNiceNumber(game.getTeamCapacity().max().intValue()));
        commonMap.put("game-size", game.getPlayers().size());
        commonMap.put("game-max-lives", game.maxLives);
        commonMap.put("game-original-players-size", game.originalPlayersSize);
        commonMap.put("game-original-teams-size", game.originalTeamsSize);
        commonMap.put("game-alive-players-size", game.getAlivePlayers().size());
        commonMap.put("game-alive-teams-size", game.getAliveTeams().size());
        commonMap.put("game-next-event", game.getNextEventName());
        commonMap.put("game-next-event-time", game.getGameState() == GameState.WAITING && game.getNextEventTimeSeconds() == game.getNextEventTimeMaxSeconds() || game.getNextEventTimeMaxSeconds() < 0 ? "Waiting..." : TimeUtils.countdownShort(game.getNextEventTimeSeconds()));
        commonMap.put("game-time-elapsed", TimeUtils.countdownShort(game.elapsedSeconds).equalsIgnoreCase("permanent") ? "0s" : TimeUtils.countdown(game.elapsedSeconds));

        return commonMap;
    }

    public static String teamCapacityNiceNumber(int max) {
        if (max > 4)
            return max + "p";

        List<String> list = new ArrayList<>(List.of("solos", "duos", "trios", "squads"));

        return list.get(max - 1);
    }
}