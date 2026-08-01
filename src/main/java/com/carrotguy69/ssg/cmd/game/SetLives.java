package com.carrotguy69.ssg.cmd.game;

import com.carrotguy69.cxyz.messages.MessageUtils;
import com.carrotguy69.ssg.game.Game;
import com.carrotguy69.ssg.game.GamePlayer;
import com.carrotguy69.ssg.game.GameState;
import com.carrotguy69.ssg.messages.MessageGrabber;
import com.carrotguy69.ssg.messages.SSGMessageKey;
import com.carrotguy69.ssg.messages.utils.MapFormatters;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class SetLives implements CommandExecutor {

    public static CommandExecutor executor = new SetLives();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        String node = "ssg.setlives";

        // usage: /game setlives <amount> [player]

        if (!sender.hasPermission(node)) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_NO_ACCESS), Map.of("permission", node));
            return true;
        }

        if (!(sender instanceof Player p)) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_PLAYER_ONLY), Map.of());
            return true;
        }

        if (args.length == 0) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.MISSING_GENERAL), Map.of("missing-args", "amount"));
            return true;
        }

        int amount = -1;

        try {
            amount = Integer.parseInt(args[0]);

            if (amount < 1) {
                throw new RuntimeException();
            }
        }
        catch (RuntimeException e) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.INVALID_INTEGER), Map.of("input", args[0]));
            return true;
        }

        Game game = Game.getByPlayer(p);

        if (game == null) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.ERROR_NOT_IN_GAME), Map.of());
            return true;
        }

        GamePlayer target;

        if (args.length == 1) {
            target = game.getPlayer(p);
        }

        else {
            target = game.getPlayerByName(args[1]);
        }

        if (target == null) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.ERROR_PLAYER_NOT_FOUND), Map.of("username", args[0]));
            return true;
        }

        if (target.getLives() == 0 && game.getGameState() == GameState.ACTIVE) {
            target.setLives(amount);
            game.respawn(target, true);
        }

        else {
            target.setLives(amount);
        }


        Map<String, Object> commonMap = MapFormatters.gameFormatter(game);
        commonMap.putAll(MapFormatters.gamePlayerFormatter(target));

        MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.SET_PLAYER_LIVES), commonMap);

        return true;
    }
}
