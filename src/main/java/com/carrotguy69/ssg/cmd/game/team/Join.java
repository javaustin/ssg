package com.carrotguy69.ssg.cmd.game.team;

import com.carrotguy69.cxyz.messages.MessageUtils;
import com.carrotguy69.ssg.game.Game;

import com.carrotguy69.ssg.game.GamePlayer;
import com.carrotguy69.ssg.game.GameState;
import com.carrotguy69.ssg.game.GameTeam;
import com.carrotguy69.ssg.messages.MessageGrabber;
import com.carrotguy69.ssg.messages.SSGMessageKey;
import com.carrotguy69.ssg.messages.utils.MapFormatters;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class Join implements CommandExecutor {
    public static CommandExecutor executor = new Join();


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        /*
        SYNTAX:
            /team join <id/name>
            /team join blue
        */

        String node = "ssg.team.join";

        if (!sender.hasPermission(node)) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_NO_ACCESS), Map.of("permission", node));
            return true;
        }

        if (!(sender instanceof Player p)) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.COMMAND_PLAYER_ONLY), Map.of());
            return true;
        }

        if (args.length == 0) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.MISSING_GENERAL), Map.of("missing-args", "team"));
            return true;
        }

        Game game = Game.getByPlayer(p);

        if (game == null) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.ERROR_NOT_IN_GAME), Map.of());
            return true;
        }

        if (game.getGameState() != GameState.WAITING) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.ERROR_TEAM_NO_SWITCHING), Map.of());
            return true;
        }

        GamePlayer gp = game.getPlayer(p);
        GameTeam originalTeam = gp.getTeam();
        GameTeam team = game.getTeamByName(args[0]);

        Map<String, Object> commonMap = MapFormatters.teamFormatter(team); // team is allowed to be null for the formatter

        if (originalTeam != null && originalTeam.equals(team)) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.ERROR_TEAM_ALREADY_IN_TEAM), commonMap);
            return true;
        }

        if (team == null) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.INVALID_TEAM), commonMap);
            return true;
        }

        try {
            game.assignTeam(gp, team);

            commonMap.putAll(MapFormatters.gamePlayerFormatter(gp));

            if (originalTeam != null) {
                originalTeam.removePlayer(gp);

                originalTeam.sendTeamMessage(MessageGrabber.grab(SSGMessageKey.TEAM_LEAVE_ANNOUNCEMENT), MapFormatters.gamePlayerFormatter(gp), List.of());
            }

            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.TEAM_JOIN), commonMap);

            team.sendTeamMessage(MessageGrabber.grab(SSGMessageKey.TEAM_JOIN_ANNOUNCEMENT), commonMap, List.of(gp));

            game.updateScoreboard();
        }
        catch (RuntimeException e) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SSGMessageKey.ERROR_TEAM_FULL), commonMap);
        }

        return true;
    }
}
