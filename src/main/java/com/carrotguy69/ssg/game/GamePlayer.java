package com.carrotguy69.ssg.game;

import com.carrotguy69.cxyz.models.db.NetworkPlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GamePlayer {
    private UUID uuid;
    private boolean alive;
    private int kills;
    private GameTeam team;

    public NetworkPlayer getNetworkPlayer() {
        return NetworkPlayer.getPlayerByUUID(this.uuid);
    }

    public GamePlayer(Player p) {
        this.uuid = p.getUniqueId();
        this.alive = false;
        this.kills = 0;
        this.team = null;
    }

    public GamePlayer(NetworkPlayer p) {
        new GamePlayer(p.getPlayer());
    }

    public GamePlayer(UUID uuid, GameTeam team, boolean alive, int kills) {
        this.uuid = uuid;
        this.team = team;
        this.alive = alive;
        this.kills = kills;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public int getKills() {
        return this.kills;
    }

    public boolean isAlive() {
        return this.alive;
    }

    public GameTeam getTeam() {
        return this.team;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public void setTeam(GameTeam team) {
        this.team = team;
    }
}
