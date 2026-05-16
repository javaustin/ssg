package com.carrotguy69.ssg.cmd;

import com.carrotguy69.cxyz.messages.MessageKey;
import com.carrotguy69.cxyz.messages.MessageUtils;
import com.carrotguy69.ssg.SpeedSG;
import com.carrotguy69.ssg.game.Game;
import com.carrotguy69.ssg.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class Print implements CommandExecutor {

    public static CommandExecutor executor = new Print();

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

                case "game":
                    if (args.length > 1) {
                    // Allow the user to get a specific game by ID or a participating player
                        Game g = Game.getByID(args[0]);

                        if (g == null)
                            g = Game.getByPlayer(Bukkit.getPlayer(args[0]));

                        if (g != null) {
                            Logger.info(g.toString());
                            break;
                        }
                    }

                    Logger.info(SpeedSG.gameIDMap.values().stream().toList().toString());
                    break;

            }
        }


        return true;
    }

}
