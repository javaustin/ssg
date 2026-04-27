package com.carrotguy69.ssg.game;

import com.carrotguy69.cxyz.models.db.NetworkPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GamePlayer {
    private final UUID uuid;
    private boolean alive;
    private int lives;
    private int kills;
    private GameTeam team;
    private final Map<String, Double> stats = new HashMap<>();


    public GamePlayer(Player p) {
        this.uuid = p.getUniqueId();
        this.alive = false;
        this.kills = 0;
        this.lives = 1;
        this.team = null;

    }

    public GamePlayer(UUID uuid, GameTeam team, int lives, boolean alive, int kills) {
        this.uuid = uuid;
        this.team = team;
        this.lives = lives;
        this.alive = alive;
        this.kills = kills;
    }

    public Player getBukkitPlayer() {
        return Bukkit.getPlayer(this.uuid);
    }

    public NetworkPlayer getNetworkPlayer() {
        return NetworkPlayer.getPlayerByUUID(this.uuid);
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

    public Map<String, Double> getStats() {
        return stats;
    }

    public double getStat(String key, double def) {
        return stats.getOrDefault(key, def);
    }

    public void setStat(String key, double value) {
        stats.put(key, value);
    }

}
