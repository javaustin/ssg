package com.carrotguy69.ssg.game;

import com.carrotguy69.cxyz.messages.MessageUtils;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameTeam {

    private final String prefix;
    private final String color;
    private final List<GamePlayer> players;
    private final int capacity;

    // Matchmaking scores (if the score is lower than the team is underbalanced and needs more players)
    public final double matchmakingScore = 0;

    private final Map<String, Double> stats = new HashMap<>();

    public GameTeam(String prefix, String color, List<GamePlayer> players, int capacity) {
        this.prefix = prefix;
        this.color = color;
        this.players = players;
        this.capacity = capacity;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String getColor() {
        return this.color;
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
            throw new RuntimeException("Cannot add player to team because the team is full!");
        }

        this.players.add(gp);
    }

    public void removePlayer(GamePlayer gp) {
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

}
