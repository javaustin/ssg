package com.carrotguy69.ssg.utils;

import com.carrotguy69.cxyz.CXYZ;
import com.carrotguy69.cxyz.messages.MessageUtils;
import com.carrotguy69.cxyz.models.db.GameStat;
import com.carrotguy69.cxyz.models.db.NetworkPlayer;
import com.carrotguy69.cxyz.utils.TimeUtils;
import com.carrotguy69.cxyz.webhook.DiscordWebhook;
import com.carrotguy69.cxyz.webhook.WebhookMessageParser;

import com.carrotguy69.ssg.SpeedSG;
import com.carrotguy69.ssg.other.Logger;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public class LeaderboardUpdater {

    public static void update() {
        ConfigurationSection section = SpeedSG.configYML.getConfigurationSection("discord-leaderboards");

        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {

            String stat = section.getString(key + ".settings.stat-key");
            String entryFormat = section.getString(key + ".settings.entry-format");
            String delimiter = section.getString(key + ".settings.separator", "\n{i}.)");
            int limit = section.getInt(key + ".settings.limit", 10);

            if (entryFormat == null) {
                Logger.warning("Failed to update leaderboard " + key + " because no entry format was provided.");
                continue;
            }

            Pair<String, Map<String, Object>> pair = getTopStatText(stat, entryFormat, delimiter, limit);

            String text = pair.getLeft();


            Map<String, Object> commonMap = pair.getRight();

            long now = TimeUtils.unixTimeNow();
            commonMap.put("timestamp", TimeUtils.dateOf(now, CXYZ.timezone) + " at " + TimeUtils.timeOf(now, CXYZ.timezone)); // For webhook footer

            commonMap.put("leaderboard", MessageUtils.formatPlaceholders(text, commonMap));

            ConfigurationSection webhookSection = SpeedSG.configYML.getConfigurationSection("discord-leaderboards." + key + ".webhook");

            DiscordWebhook webhook = WebhookMessageParser.createWebhook(webhookSection, null, commonMap);
            if (webhook == null)
                Logger.warning("Failed to update leaderboard " + key + " because the webhook URL was not found.");
            else
                webhook.send();
        }
    }

    private static Pair<String, Map<String, Object>> getTopStatText(String statKey, String entryFormat, String delimiter, int limit) {

        Map<Integer, GameStat> lb = GameStat.getStatLeaderboard(statKey);

        List<NetworkPlayer> players = lb.values().stream().map(gs -> NetworkPlayer.resolvePlayer(gs.getUUID())).toList();

        com.carrotguy69.cxyz.messages.utils.MapFormatters.ListFormatter formatter = com.carrotguy69.cxyz.messages.utils.MapFormatters.playerListFormatter(
                players,
                entryFormat,
                delimiter,
                limit,
                1
        );

        com.carrotguy69.cxyz.messages.utils.MapFormatters.NumberedListFormatter numberedListFormatter = new com.carrotguy69.cxyz.messages.utils.MapFormatters.NumberedListFormatter(
                formatter.getEntries(),
                formatter.getDelimiter(),
                formatter.getFormatMap(),
                limit
        );

        // Creating a new class for this result would be too much abstraction, and secondly I am too lazy.
        return Pair.of(numberedListFormatter.generatePage(1), numberedListFormatter.getFormatMap());
    }

}
