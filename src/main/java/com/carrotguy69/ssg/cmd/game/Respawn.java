package com.carrotguy69.ssg.cmd.game;

import com.carrotguy69.cxyz.messages.MessageUtils;
import com.carrotguy69.ssg.game.Game;
import com.carrotguy69.ssg.game.GamePlayer;
import com.carrotguy69.ssg.messages.MessageGrabber;
import com.carrotguy69.ssg.messages.SSGMessageKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class Respawn implements CommandExecutor {

    public static CommandExecutor executor = new Respawn();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        String node = "ssg.respawn";

        if (!sender.hasPermission(node)) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_NO_ACCESS), Map.of("permission", node));
            return true;
        }

        Game game;

        if (!(sender instanceof Player p)) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_PLAYER_ONLY), Map.of());
            return true;
        }

        game = Game.getByPlayer(p);

        if (game == null) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.ERROR_NOT_IN_GAME), Map.of());
            return true;
        }

        GamePlayer target;

        if (args.length == 0) {
            target = game.getPlayer(p);
        }

        else {
            target = game.getPlayerByName(args[0]);
        }

        if (target == null) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.ERROR_PLAYER_NOT_FOUND), Map.of("username", args[0]));
            return true;
        }

        game.respawn(target, true);
        return true;
    }
}
