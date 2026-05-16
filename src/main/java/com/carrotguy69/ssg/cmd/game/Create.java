package com.carrotguy69.ssg.cmd.game;

import com.carrotguy69.cxyz.messages.MessageKey;
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
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Random;

import static com.carrotguy69.ssg.SpeedSG.gameMaps;
import static com.carrotguy69.ssg.SpeedSG.lootTables;

public class Create implements CommandExecutor {
    public static CommandExecutor executor = new Create();


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        /*
        SYNTAX:
            /sg create [id] [team-capacity-range] [loot-table] [map]
            /sg create SG-2 solos OP cavern
        */

        String node = "ssg.game.create";

        if (!sender.hasPermission(node)) {
            MessageUtils.sendParsedMessage(sender, MessageKey.COMMAND_NO_ACCESS, Map.of("permission", node));
            return true;
        }

        String gameId = "SSG-1";
        GameMap gameMap = gameMaps.get(new Random().nextInt(0, gameMaps.size() - 1));
        LootTable lootTable = lootTables.get(new Random().nextInt(0, lootTables.size() - 1));
        NumberRange teamCapacity = new NumberRange(1, 1);

        if (args.length >= 1) {
            String input = args[0];

            Game game = Game.getByID(gameId);
            if (game != null) {
                MessageUtils.sendParsedMessage(
                        sender,
                        MessageGrabber.grab(SSGMessageKey.ERROR_DUPLICATE_GAME),
                        Map.of("input", input)
                );
                return true;
            }

            gameId = input;
        }

        if (args.length >= 2) {
            String input = args[1];

            switch (input.toUpperCase()) {
                case "SOLO":
                case "SOLOS":
                    break;

                case "DUOS":
                case "DUO":
                    teamCapacity = new NumberRange(1, 2);
                    break;

                case "TRIOS":
                case "TRIO":
                    teamCapacity = new NumberRange(1, 3);
                    break;

                case "SQUADS":
                case "SQUAD":
                    teamCapacity = new NumberRange(1, 4);
                    break;

                default:
                    try {
                        // If the user is stubborn and wants to define a specific minimum and maximum team size, they indicate that by using the hyphen. This can convert easily to a NumberRange.
                        if (input.contains("-")) {
                            teamCapacity = NumberRange.fromString(input);
                        }
                        // Usually the user is not stubborn, and they might put a single number for the maxTeamCapacity. We can fulfill that while keeping minTeamCapacity = 0;
                        else {
                            teamCapacity = new NumberRange(0, Integer.valueOf(input));
                        }
                    }
                    catch (RuntimeException e) {
                        MessageUtils.sendParsedMessage(
                                sender,
                                MessageGrabber.grab(SSGMessageKey.INVALID_TEAM_CAPACITY),
                                Map.of("input", input)
                        );
                        return true;
                    }

            }
        }

        if (args.length >= 3) {
            String input = args[2];

            lootTable = LootTable.getByName(input);

            if (lootTable == null) {
                MessageUtils.sendParsedMessage(
                        sender,
                        MessageGrabber.grab(SSGMessageKey.INVALID_LOOT_TABLE),
                        Map.of("input", input)
                );
                return true;
            }
        }

        if (args.length >= 4) {
            String input = args[3];

            gameMap = GameMap.getByID(input);

            if (gameMap == null) {
                MessageUtils.sendParsedMessage(
                        sender,
                        MessageGrabber.grab(SSGMessageKey.INVALID_MAP),
                        Map.of("input", input)
                );
                return true;
            }
        }

        Game game = new Game(gameId, gameMap, lootTable, new NumberRange(1, 16), teamCapacity);

        MessageUtils.sendParsedMessage(
                sender,
                MessageGrabber.grab(SSGMessageKey.COMMAND_CREATE_GAME),
                MapFormatters.gameFormatter(game)
        );

        return true;
    }
}
