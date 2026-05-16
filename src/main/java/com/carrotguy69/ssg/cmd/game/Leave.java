package com.carrotguy69.ssg.cmd.game;

import com.carrotguy69.cxyz.messages.MessageKey;
import com.carrotguy69.cxyz.messages.MessageUtils;
import com.carrotguy69.ssg.game.Game;
import com.carrotguy69.ssg.game.GamePlayer;
import com.carrotguy69.ssg.messages.MessageGrabber;
import com.carrotguy69.ssg.messages.SSGMessageKey;
import com.carrotguy69.ssg.messages.utils.MapFormatters;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class Leave implements CommandExecutor {
    public static CommandExecutor executor = new Leave();


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        /*
        SYNTAX:
            /sg leave
        */

        String node = "ssg.game.leave";

        if (!sender.hasPermission(node)) {
            MessageUtils.sendParsedMessage(sender, MessageKey.COMMAND_NO_ACCESS, Map.of("permission", node));
            return true;
        }

        if (!(sender instanceof Player p)) {
            MessageUtils.sendParsedMessage(sender, MessageKey.COMMAND_PLAYER_ONLY, Map.of());
            return true;
        }

        Game game = Game.getByPlayer(p);

        if (game == null) {
            MessageUtils.sendParsedMessage(
                    sender,
                    MessageGrabber.grab(SSGMessageKey.ERROR_NOT_IN_GAME),
                    Map.of("input", args[0])
            );

            return true;
        }


        GamePlayer gp = game.getPlayer(p);


        game.removePlayer(gp);


        Map<String, Object> commonMap = MapFormatters.gameFormatter(game);
        commonMap.putAll(MapFormatters.gamePlayerFormatter(gp));

        MessageUtils.sendParsedMessage(
                sender,
                MessageGrabber.grab(SSGMessageKey.COMMAND_LEAVE_GAME),
                commonMap
        );

        return true;
    }
}
