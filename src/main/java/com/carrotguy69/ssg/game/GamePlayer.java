package com.carrotguy69.ssg.game;

import com.carrotguy69.cxyz.models.db.NetworkPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.carrotguy69.ssg.SpeedSG.configYML;

public class GamePlayer {
    private final UUID uuid;
    private boolean alive;
    private int lives;
    private int kills;
    private GameTeam team;
    private final Map<String, Double> stats = new HashMap<>();

    public GamePlayer(UUID uuid) {
        this.uuid = uuid;
        this.team = null;
        this.lives = configYML.getInt("game.respawns.default-lives");
        this.alive = true;
        this.kills = 0;
    }

    public int getLives() {
        return this.lives;
    }

    public void setLives(int lives) {
        this.lives = Math.max(0, lives);
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

    public Map<String, Double> getTemporaryStat() {
        return stats;
    }

    public double getTemporaryStat(String key, double def) {
        return stats.getOrDefault(key, def);
    }

    public void setTemporaryStat(String key, double value) {
        stats.put(key, value);
    }

    public Game getGame() {
        return Game.getByPlayer(this.getBukkitPlayer());
    }
}
