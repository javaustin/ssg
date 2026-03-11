package com.carrotguy69.ssg.tabCompleters;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Test implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        String node = "ssg.test";

        if (!sender.hasPermission(node))
            return List.of();

        else if (args.length == 0) {
            return List.of("...");
        }

        return List.of();
    }
}
