package com.carrotguy69.ssg.cmd.game;

import com.carrotguy69.cxyz.messages.MessageUtils;
import com.carrotguy69.ssg.game.Game;
import com.carrotguy69.ssg.game.GameState;
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
        //                       [0]    [1]
        // usage: /game setting {key} {value}

        String node = "ssg.setting";

        if (!sender.hasPermission(node)) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_NO_ACCESS), Map.of("permission", node));
            return true;
        }

        Game game = null;

        if (!(sender instanceof Player p)) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_PLAYER_ONLY), Map.of());
            return true;
        }

        String key = "";
        String value = null;

        game = Game.getByPlayer(p);

        if (game == null) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.ERROR_NOT_IN_GAME), Map.of());
            return true;
        }

        if (args.length == 0) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.MISSING_GENERAL), Map.of("missing-args", "setting"));
            return true;
        }

        if (args.length == 1) {
            key = args[0];
        }

        if (args.length == 2) {
            key = args[0];
            value = args[1];
        }

        Map<String, Object> commonMap = MapFormatters.gameFormatter(game);

        if (key.equalsIgnoreCase("map")) {
            commonMap.put("key", "map");
            if (value == null) {
                commonMap.put("value", game.getGameMap().getID());
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_GAME_SETTING_GET), commonMap);
                return true;
            }

            GameMap map = GameMap.getByID(value);

            commonMap.put("input", value);
            commonMap.put("value", value);

            if (map == null || map.getID().equalsIgnoreCase("lobby")) {
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.LOBBY_INVALID_MAP), commonMap);
                return true;
            }

            if (game.getGameState() != GameState.WAITING) {
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_GAME_SETTING_FAIL), commonMap);
                return true;
            }

            game.setGameMap(map);
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_GAME_SETTING_SET), commonMap);
            return true;

        }

        else if (key.equalsIgnoreCase("loottable")) {
            commonMap.put("key", "lootTable");
            if (value == null) {
                commonMap.put("value", game.getLootTable().getName());
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_GAME_SETTING_GET), commonMap);
                return true;
            }

            LootTable table = LootTable.getByName(value);

            commonMap.put("input", value);
            commonMap.put("value", value);

            if (table == null) {
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.INVALID_LOOT_TABLE), Map.of("input", value));
                return true;
            }

            game.setLootTable(table);
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_GAME_SETTING_SET), commonMap);
        }

        else if (key.equalsIgnoreCase("amountofteams")) {
            commonMap.put("key", "amountOfTeams");

            if (value == null) {
                commonMap.put("value", game.getAmountOfTeams().toPrettyString());
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_GAME_SETTING_GET), commonMap);
                return true;
            }

            if (game.getGameState() != GameState.WAITING) {
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_GAME_SETTING_FAIL), commonMap);
                return true;
            }

            try {
                NumberRange range = NumberRange.fromString(value);

                if (range.min().intValue() < 2) {
                    throw new RuntimeException("Range cannot include any values < 2. (There need to be at least two teams to run a game.)");
                }

                if (game.getGameState() != GameState.WAITING) {
                    MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_GAME_SETTING_FAIL), commonMap);
                    return true;
                }

                game.setAmountOfTeams(range);

                commonMap.put("value", value);
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_GAME_SETTING_SET), commonMap);

            }
            catch (RuntimeException e) {
                commonMap.put("input", value);
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.INVALID_AMOUNT_OF_TEAMS), commonMap);
            }
        }

        else if (key.equalsIgnoreCase("teamcapacity")) {
            commonMap.put("key", "teamCapacity");

            if (value == null) {
                commonMap.put("value", game.getTeamCapacity().toPrettyString());
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_GAME_SETTING_GET), commonMap);
                return true;
            }

            try {
                NumberRange range = Game.parseTeamCapacity(value);

                commonMap.put("value", MapFormatters.teamCapacityNiceNumber(range.max().intValue()));

                if (game.getGameState() != GameState.WAITING) {
                    MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_GAME_SETTING_FAIL), commonMap);
                    return true;
                }

                game.setTeamCapacity(range);

                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_GAME_SETTING_SET), commonMap);

            }
            catch (RuntimeException e) {
                commonMap.put("input", value);
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.INVALID_TEAM_CAPACITY), commonMap);
            }
        }

        else if (key.equalsIgnoreCase("maxlives")) {
            commonMap.put("key", "maxLives");

            if (value == null) {
                commonMap.put("value", game.maxLives);
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_GAME_SETTING_GET), commonMap);
                return true;
            }

            try {
                game.setMaxLives(Integer.parseInt(value));

                commonMap.put("value", value);
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_GAME_SETTING_SET), commonMap);
            }
            catch (RuntimeException e) {
                commonMap.put("input", value);
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.INVALID_INTEGER), commonMap);
            }
        }


        else {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.INVALID_GAME_SETTING), Map.of("input", key));
        }

        return true;
    }

}
