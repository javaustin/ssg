package com.carrotguy69.ssg.game;

import com.carrotguy69.cxyz.messages.MessageUtils;
import com.carrotguy69.cxyz.models.db.GameStat;
import com.carrotguy69.ssg.exceptions.TeamFullException;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameTeam {

    private int index;
    private String name;
    private String shortName;
    private final int rgbColor;
    private final List<GamePlayer> players;
    private final int capacity;

    // Matchmaking scores (if the score is lower, then the team is underbalanced and needs more players)
    public double matchmakingScore = 0;

    private final Map<String, Double> stats = new HashMap<>();

    public GameTeam(int index, String prefix, String shortName, int rgbColor, List<GamePlayer> players, int capacity) {
        this.index = index;
        this.name = prefix;
        this.shortName = shortName;
        this.rgbColor = rgbColor;
        this.players = players;
        this.capacity = capacity;
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int i) {
        this.index = i;
    }

    public String getName() {
        return this.name;
    }

    public String getShortName() {
        return this.shortName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setShortName(String name) {
        this.shortName = name;
    }

    public int getRGBColor() {
        return this.rgbColor;
    }

    public boolean isAlive() {
        return !getAliveMembers().isEmpty();
    }

    public boolean isFull() {
        return players.size() >= capacity;
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    public int getCapacity() {
        return this.capacity;
    }

    public double getCombinedHealth() {
        double hp = 0;

        for (GamePlayer gp : getAliveMembers()) {
            hp += gp.getBukkitPlayer().getHealth();
        }

        return hp;
    }

    public List<GamePlayer> getAliveMembers() {
        List<GamePlayer> results = new ArrayList<>();

        for (GamePlayer gp : players) {
            if (gp.isAlive()) {
                results.add(gp);
            }
        }

        return results;
    }

    public List<GamePlayer> getPlayers() {
        return this.players;
    }

    public void addPlayer(GamePlayer gp) {
        if (this.players.size() == capacity) {
            throw new TeamFullException("Team %s is at or above its max capacity (%d/%d)!".formatted(this.getName(), this.getPlayers().size(), this.getCapacity()));
        }

        GameStat playerLifetimeKills = GameStat.getStat(gp.getUUID(), "sg-lifetime-kills");

        if (playerLifetimeKills != null) {
            this.matchmakingScore += Integer.parseInt(playerLifetimeKills.getValue());
        }

        GameStat playerLifetimeWins = GameStat.getStat(gp.getUUID(), "sg-lifetime-wins");

        if (playerLifetimeKills != null) {
            // Scale by x0.5 (wins are a less significant skill indicator than kills)
            this.matchmakingScore += ((double) Integer.parseInt(playerLifetimeWins.getValue()) / 2);
        }

        this.players.add(gp);
    }

    public void removePlayer(GamePlayer gp) {

        GameStat playerLifetimeKills = GameStat.getStat(gp.getUUID(), "sg-lifetime-kills");

        if (playerLifetimeKills != null) {
            this.matchmakingScore -= Integer.parseInt(playerLifetimeKills.getValue());
        }

        GameStat playerLifetimeWins = GameStat.getStat(gp.getUUID(), "sg-lifetime-wins");

        if (playerLifetimeKills != null) {
            // Scale by x0.5 (wins are a less significant skill indicator than kills)
            this.matchmakingScore -= ((double) Integer.parseInt(playerLifetimeWins.getValue()) / 2);
        }

        this.players.remove(gp);
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

    public void sendTeamMessage(String unparsedContent, Map<String, Object> formatMap, List<GamePlayer> excludingPlayers) {
        TextComponent component = MessageUtils.createMessage(unparsedContent, formatMap);

        for (GamePlayer gp : this.players) {
            if (gp.getBukkitPlayer() == null) {
                continue;
            }

            if (excludingPlayers.contains(gp))
                continue;

            Player p = gp.getBukkitPlayer();

            p.sendMessage(component);
        }
    }

    @Override
    public String toString() {
        return "GameTeam{"
                + "index=" + index + ","
                + "name=" + name  + ","
                + "shortName=" + shortName  + ","
                + "rgbColor=" + rgbColor + ","
                + "players=" + players + ","
                + "capacity=" + capacity +
                "}";
    }

}
