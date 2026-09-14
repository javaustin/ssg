package com.carrotguy69.ssg.eventHandler;

import com.carrotguy69.cxyz.events.custom.PublicChatEvent;
import com.carrotguy69.cxyz.events.custom.base.EventHandler;
import com.carrotguy69.cxyz.messages.MessageParser;
import com.carrotguy69.cxyz.models.db.NetworkPlayer;
import com.carrotguy69.cxyz.webhook.DiscordWebhook;
import com.carrotguy69.ssg.SpeedSG;
import com.carrotguy69.ssg.game.Game;
import com.carrotguy69.ssg.messages.MessageGrabber;
import com.carrotguy69.ssg.messages.SSGMessageKey;
import com.carrotguy69.ssg.messages.utils.MapFormatters;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class CoreChatHandler implements EventHandler<PublicChatEvent> {

    @Override
    public boolean handle(PublicChatEvent e) {

        String content = e.getContent();
        NetworkPlayer np = e.getSender();

        Player p = np.getPlayer();
        Game game = Game.getByPlayer(p);


        if (game == null) {
            return false;
        }

        // handle lobby chat, game chat

        Map<String, Object> commonMap = MapFormatters.gamePlayerFormatter(game.getPlayer(p));

        if (game.getTeamCapacity().max().intValue() == 1) {
            commonMap.put("player-team", "");
            commonMap.put("player-team-prefix", "");
            commonMap.put("player-team-name", "");
        }

        commonMap.putAll(MapFormatters.gameFormatter(game));
        commonMap.put("message", content);
        commonMap.put("content", content);

        SSGMessageKey key = switch (game.getGameState()) {
            case WAITING, RESET -> SSGMessageKey.LOBBY_CHAT;
            default -> SSGMessageKey.GAME_CHAT;
        };


        String webhookText = MessageGrabber.grab(SSGMessageKey.valueOf(key.name() + "_WEBHOOK"));

        game.announce(MessageGrabber.grab(SSGMessageKey.LOBBY_CHAT), commonMap, List.of(), np);

        if (SpeedSG.WebhookSettings.enabled && SpeedSG.WebhookSettings.eventsLogged.contains(SpeedSG.WebhookSettings.Event.CHAT)) {

            String webhookContent = MessageParser.getStrippedText(new MessageParser(webhookText, commonMap).parse());

            DiscordWebhook webhook = new DiscordWebhook(SpeedSG.WebhookSettings.url, webhookContent, List.of());

            webhook.send();
        }

        return true;
    }
}
