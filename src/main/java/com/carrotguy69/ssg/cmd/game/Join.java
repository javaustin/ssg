package com.carrotguy69.ssg.cmd.game;

import com.carrotguy69.cxyz.messages.MessageKey;
import com.carrotguy69.cxyz.messages.MessageUtils;
import com.carrotguy69.ssg.SpeedSG;
import com.carrotguy69.ssg.game.Game;
import com.carrotguy69.ssg.game.GamePlayer;
import com.carrotguy69.ssg.messages.MessageGrabber;
import com.carrotguy69.ssg.messages.SSGMessageKey;
import com.carrotguy69.ssg.messages.utils.MapFormatters;
import com.carrotguy69.ssg.utils.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Map;
import java.util.NoSuchElementException;

import static com.carrotguy69.ssg.SpeedSG.configYML;

public class Join implements CommandExecutor {
    public static CommandExecutor executor = new Join();


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        /*
        SYNTAX:
            /sg join [id]
            /sg join 32
        */

        String node = "ssg.game.join";

        if (!sender.hasPermission(node)) {
            MessageUtils.sendParsedMessage(sender, MessageKey.COMMAND_NO_ACCESS, Map.of("permission", node));
            return true;
        }

        if (!(sender instanceof Player p)) {
            MessageUtils.sendParsedMessage(sender, MessageKey.COMMAND_PLAYER_ONLY, Map.of());
            return true;
        }

        Game game;

        if (args.length >= 1) {
            game = Game.getByID(args[0]);

            if (game == null) {
                MessageUtils.sendParsedMessage(
                        sender,
                        MessageGrabber.grab(SSGMessageKey.INVALID_GAME),
                        Map.of("input", args[0])
                );

                return true;
            }
        }

        else {

            // Get any game (prioritizing player count)
            try {
                game = SpeedSG.gameIDMap.values().stream().max(Comparator.comparingInt(g -> g.getPlayers().size())).stream().findFirst().orElseThrow();
            }
            catch (NoSuchElementException ex) {
                Logger.warning("No SSG games found!");
                    MessageUtils.sendParsedMessage(
                            sender,
                            MessageGrabber.grab(SSGMessageKey.ERROR_NO_GAMES),
                            Map.of()
                    );

                    return true;
            }
        }

        int defaultLives = configYML.getInt("", 1);

        GamePlayer gp = new GamePlayer(p.getUniqueId());

        Game originalGame = Game.getByPlayer(p);

        if (originalGame != null) {
            originalGame.removePlayer(gp);
        }

        game.addPlayer(gp);


        Map<String, Object> commonMap = MapFormatters.gameFormatter(game);
        commonMap.putAll(MapFormatters.gamePlayerFormatter(gp));

        MessageUtils.sendParsedMessage(
                sender,
                MessageGrabber.grab(SSGMessageKey.COMMAND_JOIN_GAME),
                commonMap
        );

        return true;
    }
}
