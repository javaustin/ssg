package com.carrotguy69.ssg.cmd.game;

import com.carrotguy69.cxyz.messages.MessageUtils;
import com.carrotguy69.ssg.game.Game;
import com.carrotguy69.ssg.game.loot.LootTable;
import com.carrotguy69.ssg.game.map.GameMap;
import com.carrotguy69.ssg.messages.MessageGrabber;
import com.carrotguy69.ssg.messages.SSGMessageKey;
import com.carrotguy69.ssg.messages.utils.MapFormatters;
import com.carrotguy69.ssg.utils.objects.NumberRange;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class Setting implements CommandExecutor {

    public static CommandExecutor executor = new Setting();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        String node = "ssg.game.setting";

        if (!sender.hasPermission(node)) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_NO_ACCESS), Map.of("permission", node));
            return true;
        }

        Game game;

        if (args.length == 0 && !(sender instanceof Player)) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.MISSING_GENERAL), Map.of("missing-args", "gameID, setting"));
            return true;
        }

        else if (args.length == 0) {
            Player p = (Player) sender;

            game = Game.getByPlayer(p);

            if (game == null) {
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.ERROR_NOT_IN_GAME), Map.of());
                return true;
            }
        }

        else {
            game = Game.getByID(args[0]);
        }

        if (game == null) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.INVALID_GAME), Map.of("input", args[0]));
            return true;
        }

        if (args.length == 1) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.MISSING_GENERAL), Map.of("missing-args", "setting"));
            return true;
        }

        Map<String, Object> commonMap = MapFormatters.gameFormatter(game);

        String key = args[1];
        String value = args.length >= 3 ? args[2] : null;

        /*
        What settings can we support?
        - map
        - lootTable
        - amountOfTeams
        - teamCapacity
        - maxLives
        - durations (lobby, game, invul, chestRefill, showdown, gameEnd)

        */

        // This will be a bit scripty don't mind me

        if (key.equalsIgnoreCase("map")) {
            commonMap.put("key", key);
            if (value == null) {
                commonMap.put("value", game.getGameMap().getID());
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_GAME_SETTING_GET), commonMap);
                return true;
            }

            GameMap map = GameMap.getByID(value);

            if (map == null || map.getID().equalsIgnoreCase("lobby")) {
                commonMap.put("input", value);
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.LOBBY_INVALID_MAP), commonMap);
            }

            else {
                game.nextMap = map;
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_GAME_SETTING_SET), commonMap);
            }
        }

        else if (key.equalsIgnoreCase("loottable")) {
            commonMap.put("key", key);
            if (value == null) {
                commonMap.put("value", game.getLootTable().getName());
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_GAME_SETTING_GET), commonMap);
                return true;
            }

            LootTable table = LootTable.getByName(value);

            if (table == null) {
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.INVALID_LOOT_TABLE), Map.of("input", value));
            }

            else {
                game.setLootTable(table);
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_GAME_SETTING_SET), commonMap);
            }
        }

        else if (key.equalsIgnoreCase("amountofteams")) {
            commonMap.put("key", key);

            if (value == null) {
                commonMap.put("value", game.amountOfTeams.toPrettyString());
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_GAME_SETTING_GET), commonMap);
                return true;
            }

            try {
                NumberRange range = NumberRange.fromString(value);

                if (range.min().intValue() < 2) {
                    throw new RuntimeException("Range cannot include any values < 2. (There need to be at least two teams to run a game.)");
                }

                game.nextAmountOfTeams = range;

                commonMap.put("value", value);
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_GAME_SETTING_SET), commonMap);

            }
            catch (RuntimeException e) {
                commonMap.put("input", value);
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.INVALID_AMOUNT_OF_TEAMS), commonMap);
            }
        }

        else if (key.equalsIgnoreCase("teamcapacity")) {
            commonMap.put("key", key);

            if (value == null) {
                commonMap.put("value", game.teamCapacity.toPrettyString());
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_GAME_SETTING_GET), commonMap);
                return true;
            }

            try {
                NumberRange range = NumberRange.fromString(value);

                if (range.min().intValue() < 1) {
                    throw new RuntimeException("Range cannot include any values < 1. (There need to be at least two teams to run a game.)");
                }

                game.nextTeamCapacity = range;

                commonMap.put("value", value);
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_GAME_SETTING_SET), commonMap);

            }
            catch (RuntimeException e) {
                commonMap.put("input", value);
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.INVALID_TEAM_CAPACITY), commonMap);
            }
        }

        else if (key.equalsIgnoreCase("maxlives")) {
            commonMap.put("key", key);

            if (value == null) {
                commonMap.put("value", game.maxLives);
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_GAME_SETTING_GET), commonMap);
                return true;
            }

            try {
                game.nextMaxLives = Integer.parseInt(value);

                commonMap.put("value", value);
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_GAME_SETTING_SET), commonMap);
            }
            catch (RuntimeException e) {
                commonMap.put("input", value);
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.INVALID_MAX_LIVES), commonMap);
            }
        }

        else {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.INVALID_GAME_SETTING), Map.of("input", key));
        }

        return true;
    }

}
