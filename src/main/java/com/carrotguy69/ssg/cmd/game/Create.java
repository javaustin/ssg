package com.carrotguy69.ssg.cmd.game;

import com.carrotguy69.cxyz.messages.MessageUtils;
import com.carrotguy69.ssg.SpeedSG;
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
import java.util.UUID;

import static com.carrotguy69.ssg.SpeedSG.gameMaps;
import static com.carrotguy69.ssg.SpeedSG.lootTables;

public class Create implements CommandExecutor {
    public static CommandExecutor executor = new Create();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        /*
        SYNTAX:
            /sg create [id] [team-capacity-range] [loot-table] [map] [amountOfTeams] [maxLives]
            /sg create SG-2 solos OP cavern 4 3
        */

        String node = "ssg.create";

        if (!sender.hasPermission(node)) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_NO_ACCESS), Map.of("permission", node));
            return true;
        }

        String gameId = generateValidGameID();
        GameMap gameMap = gameMaps.size() - 1 > 0 ? gameMaps.get(new Random().nextInt(0, gameMaps.size() - 1)) : gameMaps.getFirst();
        LootTable lootTable = lootTables.size() - 1 > 0 ? lootTables.get(new Random().nextInt(0, lootTables.size() - 1)) : lootTables.getFirst();
        NumberRange teamCapacity = new NumberRange(1, 1);
        NumberRange amountOfTeams = new NumberRange(2, 16);
        int maxLives = 1;

        if (args.length >= 1) {
            gameId = args[0];
        }

        if (args.length >= 2) {
            String input = args[1];

            try {
                teamCapacity = Game.parseTeamCapacity(input);
            }
            catch (RuntimeException e) {
                MessageUtils.sendParsedMessage(
                        sender,
                        MessageGrabber.grab(SSGMessageKey.INVALID_AMOUNT_OF_TEAMS),
                        Map.of("input", input)
                );
                return true;
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

            if (gameMap == null || gameMap.getID().equalsIgnoreCase("lobby")) {
                MessageUtils.sendParsedMessage(
                        sender,
                        MessageGrabber.grab(SSGMessageKey.INVALID_MAP),
                        Map.of("input", input)
                );
                return true;
            }
        }

        if (args.length == 5) {
            String input = args[4];

            try {
                amountOfTeams = NumberRange.fromString(input);

                if (amountOfTeams.min().intValue() < 2) {
                    throw new RuntimeException("Range cannot include any values < 2. (There need to be at least two teams to run a game.)");
                }
            }
            catch (RuntimeException e) {
                MessageUtils.sendParsedMessage(
                        sender,
                        MessageGrabber.grab(SSGMessageKey.INVALID_AMOUNT_OF_TEAMS),
                        Map.of("input", input)
                );
                return true;
            }
        }

        if (args.length >= 6) {
            String input = args[5];

            try {
                maxLives = Integer.parseInt(input);
            }
            catch (NumberFormatException ignored) {}

        }

        Game game = Game.getByID(gameId);
        if (game != null) {
            MessageUtils.sendParsedMessage(
                    sender,
                    MessageGrabber.grab(SSGMessageKey.ERROR_DUPLICATE_GAME),
                    Map.of("input", gameId)
            );
            return true;
        }

        game = new Game(gameId, gameMap, lootTable, amountOfTeams, teamCapacity, maxLives);
        SpeedSG.gameIDMap.put(game.getGameID().toLowerCase(), game);

        MessageUtils.sendParsedMessage(
                sender,
                MessageGrabber.grab(SSGMessageKey.COMMAND_CREATE_GAME),
                MapFormatters.gameFormatter(game)
        );

        return true;
    }

    private String generateValidGameID() {
        for (int i = 1; i < 100; i++) {
            Game game = Game.getByID("ssg-" + i);

            if (game == null) {
                return "ssg-" + i;
            }
        }

        // If there are literally 100 games that already exist we are going to return a random uuid.
        return UUID.randomUUID().toString();
    }
}
