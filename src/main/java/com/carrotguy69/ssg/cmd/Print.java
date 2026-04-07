package com.carrotguy69.ssg.cmd;

import com.carrotguy69.cxyz.messages.MessageKey;
import com.carrotguy69.cxyz.messages.MessageUtils;
import com.carrotguy69.ssg.SpeedSG;
import com.carrotguy69.ssg.utils.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class Print implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        String node = "ssg.print";

        if (!sender.hasPermission(node)) {
            MessageUtils.sendParsedMessage(sender, MessageKey.COMMAND_NO_ACCESS, Map.of("permission", node));
            return true;
        }

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "loot":
                    Logger.info(SpeedSG.lootTables.toString());
                    break;

                case "maps":
                    Logger.info(SpeedSG.gameMaps.toString());
                    break;
            }
        }


        return true;
    }

}
