package com.carrotguy69.ssg.tabCompleters;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Print implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        String node = "cxyz.print";

        if (!sender.hasPermission(node)) {
            return List.of();
        }

        List<String> options = Arrays.asList("loot", "maps");

        options.sort(String.CASE_INSENSITIVE_ORDER);

        if (args.length == 0) {
            return options;
        }

        if (args.length == 1) {
            List<String> results = new ArrayList<>();

            for (String s : options) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                    results.add(s);
                }
            }

            return results;
        }

        return List.of();
    }
}