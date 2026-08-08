package com.carrotguy69.ssg.eventHandler;

import com.carrotguy69.cxyz.events.custom.VanishToggleEvent;
import com.carrotguy69.cxyz.events.custom.base.EventHandler;
import com.carrotguy69.cxyz.models.db.NetworkPlayer;
import com.carrotguy69.ssg.game.Game;

public class VanishHandler implements EventHandler<VanishToggleEvent> {
    @Override
    public boolean handle(VanishToggleEvent vanishToggleEvent) {

        NetworkPlayer np = vanishToggleEvent.getPlayer();

        Game game = Game.getByPlayer(np.getPlayer());

        if (game != null && vanishToggleEvent.getToggle()) {
            game.removePlayer(game.getPlayer(np.getPlayer()));
        }

        return true;
    }
}
