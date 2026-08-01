package com.carrotguy69.ssg.tabCompleters;

import com.carrotguy69.cxyz.models.db.NetworkPlayer;
import com.carrotguy69.cxyz.utils.ObjectUtils;
import com.carrotguy69.ssg.SpeedSG;
import com.carrotguy69.ssg.cmd.game.team._TeamSupercommand;
import com.carrotguy69.ssg.game.loot.LootTable;
import com.carrotguy69.ssg.game.map.GameMap;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Game implements TabCompleter {

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        String baseNode = "ssg";

        List<String> subcommands = new ArrayList<>(List.of("create", "delete", "join", "leave", "list", "info", "setting", "freeze", "respawn", "ready", "setlives", "team"));
        List<String> options = new ArrayList<>(subcommands);
        List<String> results = new ArrayList<>();


        List<String> temp = new ArrayList<>();
        for (String option : options) {
            if (sender.hasPermission(baseNode + "." + option)) {
                temp.add(option);
            }
        }

        options.clear();
        options.addAll(temp);

        if (args.length == 0) {
            return options;
        }

        String subcommand = args[0];

        // if args len is 1, jump to the bottom and return matching subcommand options

        if (args.length >= 2 && subcommand.equalsIgnoreCase("team")) {
            return _TeamSupercommand.tabCompleter.onTabComplete(sender, command, label, ObjectUtils.slice(args, 1, args.length));
        }

        if (args.length == 2) {
            options = switch (subcommand.toLowerCase()) {
                case "delete", "join", "info" ->
                        SpeedSG.gameIDMap.values().stream().map(com.carrotguy69.ssg.game.Game::getGameID).toList();
                case "setting" ->
                        new ArrayList<>(List.of("map", "lootTable", "amountOfTeams", "teamCapacity", "maxLives"));
                case "respawn" -> {
                    NetworkPlayer senderNp = sender instanceof Player ? NetworkPlayer.resolvePlayer(((Player) sender).getUniqueId()) : null;
                    yield com.carrotguy69.cxyz.tabCompleters.LocalOnlinePlayer.getUsernames(senderNp);
                }
                case "ready" ->
                    options = ObjectUtils.getCasualBooleanOptions();

                case "setlives" ->
                    options = List.of("1", "2", "3", "4", "5");
                default -> List.of();
            };
        }

        if (args.length == 3) {
            if (subcommand.equalsIgnoreCase("create")) {
                options = List.of("solos", "duos", "trios", "squads");
            }

            else if (subcommand.equalsIgnoreCase("setting")) {
                options = switch (args[1].toLowerCase()) {
                    case "map" -> SpeedSG.gameMaps.stream().map(GameMap::getID).toList();
                    case "loottable" -> SpeedSG.lootTables.stream().map(LootTable::getName).toList();
                    case "teamcapacity" -> List.of("solos", "duos", "trios", "squads");
                    default -> List.of();
                };
            }

            else if (subcommand.equalsIgnoreCase("setlives")) {
                NetworkPlayer senderNp = sender instanceof Player ? NetworkPlayer.resolvePlayer(((Player) sender).getUniqueId()) : null;
                options = com.carrotguy69.cxyz.tabCompleters.LocalOnlinePlayer.getUsernames(senderNp);
            }

            else {
                options = List.of();
            }
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

        if (subcommands.contains(args[0])) {
            if (!sender.hasPermission("ssg.game." + args[0])) {
                results.clear();
            }
        }

        return results;
    }
}
