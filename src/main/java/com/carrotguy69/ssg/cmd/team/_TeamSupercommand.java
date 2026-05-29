package com.carrotguy69.ssg.cmd.team;

import com.carrotguy69.cxyz.messages.MessageUtils;
import com.carrotguy69.cxyz.utils.ObjectUtils;
import com.carrotguy69.ssg.messages.MessageGrabber;
import com.carrotguy69.ssg.messages.SSGMessageKey;
import com.carrotguy69.ssg.tabCompleters.Team;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class _TeamSupercommand implements CommandExecutor {
    public static CommandExecutor executor = new _TeamSupercommand();
    public static TabCompleter tabCompleter = new Team();


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        String node = "ssg.team";

        if (!sender.hasPermission(node)) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_NO_ACCESS), Map.of("permission", node));
            return true;
        }

        if (args.length == 0) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.MISSING_GENERAL), Map.of("missing-args", "subcommand"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join":
                Join.executor.onCommand(sender, command, label, ObjectUtils.slice(args, 1));
                break;

            case "leave":
                Leave.executor.onCommand(sender, command, label, ObjectUtils.slice(args, 1));
                break;
        }

        return true;
    }
}
