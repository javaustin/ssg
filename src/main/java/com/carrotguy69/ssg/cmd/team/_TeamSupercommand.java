package com.carrotguy69.ssg.cmd.team;

import com.carrotguy69.cxyz.messages.MessageKey;
import com.carrotguy69.cxyz.messages.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class _TeamSupercommand implements CommandExecutor {
    public static CommandExecutor executor = new _TeamSupercommand();


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        String node = "ssg.team";

        if (!sender.hasPermission(node)) {
            MessageUtils.sendParsedMessage(sender, MessageKey.COMMAND_NO_ACCESS, Map.of("permission", node));
            return true;
        }

        if (args.length == 0) {
            MessageUtils.sendParsedMessage(sender, MessageKey.MISSING_GENERAL, Map.of("missing-args", "subcommand"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join":
                Join.executor.onCommand(sender, command, label, args);
                break;

            case "leave":
                Leave.executor.onCommand(sender, command, label, args);
                break;
        }

        return true;
    }
}
