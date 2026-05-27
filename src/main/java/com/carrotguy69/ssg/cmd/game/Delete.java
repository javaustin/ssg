package com.carrotguy69.ssg.cmd.game;

import com.carrotguy69.cxyz.messages.MessageKey;
import com.carrotguy69.cxyz.messages.MessageUtils;
import com.carrotguy69.cxyz.messages.utils.MessageGrabber;
import com.carrotguy69.ssg.SpeedSG;
import com.carrotguy69.ssg.game.Game;
import com.carrotguy69.ssg.messages.SSGMessageKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class Delete implements CommandExecutor {

    public static CommandExecutor executor = new Delete();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        String node = "ssg.game.delete";

        if (!sender.hasPermission(node)) {
            MessageUtils.sendParsedMessage(sender, MessageKey.COMMAND_NO_ACCESS, Map.of("permission", node));
            return true;
        }

        if (args.length == 0) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(MessageKey.MISSING_GENERAL), Map.of("missing-args", "id"));
            return true;
        }

        String key = args[0].toLowerCase();

        Game game = Game.getByID(key);
        game.delete();

        SpeedSG.gameIDMap.remove(key, game);

        MessageUtils.sendParsedMessage(
                sender,
                com.carrotguy69.ssg.messages.MessageGrabber.grab(SSGMessageKey.COMMAND_DELETE_GAME),
                Map.of("game-id", key.toLowerCase())
        );

        return true;
    }

}
