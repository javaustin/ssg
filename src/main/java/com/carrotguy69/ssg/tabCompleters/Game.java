package com.carrotguy69.ssg.tabCompleters;

import com.carrotguy69.ssg.SpeedSG;
import com.carrotguy69.ssg.game.loot.LootTable;
import com.carrotguy69.ssg.game.map.GameMap;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Game implements TabCompleter {

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        String node = "ssg.game";

        if (!sender.hasPermission(node)) {
            return List.of();
        }

        List<String> options = Arrays.asList("create", "delete", "join", "leave", "list");
        List<String> results = new ArrayList<>();

        if (args.length == 0) {
            return options;
        }

        String subcommand = args[0];

        // if args len is 1, jump to the bottom and return matching subcommand options

        if (args.length == 2) {
            switch (subcommand.toLowerCase()) {
                case "create":
                case "leave":
                case "list":
                    options = List.of();
                    break;

                case "delete":
                case "join":
                    options = SpeedSG.gameIDMap.values().stream().map(com.carrotguy69.ssg.game.Game::getGameID).toList();
                    break;
            }
        }

//        if (!sender.hasPermission(node + "." + subcommand)) {
//            return List.of();
//        }

        if (args.length == 3) {
            if (subcommand.equalsIgnoreCase("create"))
                options = List.of("solos", "duos", "trios", "squads");
            else
                options = List.of();
        }

        if (args.length == 4) {
            if (subcommand.equalsIgnoreCase("create"))
                options = SpeedSG.lootTables.stream().map(LootTable::getName).toList();
            else
                options = List.of();
        }

        if (args.length == 5) {
            if (subcommand.equalsIgnoreCase("create"))
                options = SpeedSG.gameMaps.stream().map(GameMap::getID).toList();
            else
                options = List.of();
        }

        if (args.length >= 6) {
            return List.of();
        }


        for (String s : options) {
            if (s.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
                results.add(s);
            }
        }

        return results;
    }
}
