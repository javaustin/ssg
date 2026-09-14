package com.carrotguy69.ssg.game;

import com.carrotguy69.cxyz.messages.MessageUtils;
import com.carrotguy69.cxyz.models.db.NetworkPlayer;
import com.carrotguy69.ssg.SpeedSG;
import com.carrotguy69.ssg.messages.utils.MapFormatters;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.carrotguy69.cxyz.CXYZ.f;
import static com.carrotguy69.ssg.SpeedSG.configYML;
import static com.carrotguy69.ssg.SpeedSG.playerTabNameFormat;

public class GamePlayer {
    private final UUID uuid;
    private boolean alive;
    private int lives;
    private GameTeam team;
    private final Map<String, Double> stats = new HashMap<>();
    private boolean ready = false;

    public GamePlayer(UUID uuid) {
        this.uuid = uuid;
        this.team = null;
        this.lives = configYML.getInt("game.respawns.default-lives");
        this.alive = true;
    }

    public boolean isReady() {
        return this.ready;
    }

    public void setReady(boolean value) {
        this.ready = value;
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
        return NetworkPlayer.resolvePlayer(this.uuid);
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public boolean isAlive() {
        return this.alive;
    }

    public GameTeam getTeam() {
        return this.team;
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

    public void updateTabName() {

        if (playerTabNameFormat != null)
            this.getBukkitPlayer().setPlayerListName(f(MessageUtils.formatPlaceholders(SpeedSG.playerTabNameFormat, MapFormatters.gamePlayerFormatter(this))));
    }

    public void clearTabName() {
        this.getBukkitPlayer().setPlayerListName(null);
    }

    @Override
    public String toString() {
        return "GamePlayer{"
                + "uuid=" + uuid + ","
                + "name=" + NetworkPlayer.resolvePlayer(uuid).getDisplayName() + ","
                + "team=" + (team != null ? team.getShortName() : null) + ","
                + "lives=" + lives + ","
                + "alive=" + alive + ","
                +
                "}";
    }
}
