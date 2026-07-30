package com.carrotguy69.ssg.tabCompleters;

import com.carrotguy69.ssg.game.GamePlayer;
import com.carrotguy69.ssg.game.GameTeam;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Team implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        String node = "ssg.team";
        // I'll make the executive decision not to permission gate for each subcommand, unless there is an obvious privacy concern with viewing online players. (In which this is not the case)

        if (!sender.hasPermission(node)) {
            return List.of();
        }

        if (!(sender instanceof Player)) {
            return List.of();
        }

        Player p = (Player) sender;
        com.carrotguy69.ssg.game.Game game = com.carrotguy69.ssg.game.Game.getByPlayer(p);

        if (game == null) {
            return List.of();
        }
        GamePlayer gp = game.getPlayer(p);

        if (gp == null) {
            return List.of();
        }

        List<String> results = new ArrayList<>();
        List<String> options = List.of("join", "leave", "list");

        if (args.length == 0) {
            return options;
        }

        String subcommand = args[0];
        if (subcommand.equalsIgnoreCase("leave")) {
            return List.of();
        }

        // (skip to bottom if args.length == 1)

        if (args.length == 2) {
            if (subcommand.equalsIgnoreCase("join") || subcommand.equalsIgnoreCase("list")) {
                options = new ArrayList<>(game.getTeams().stream().map(GameTeam::getShortName).toList());
            }
        }

        if (args.length >= 3) {
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
