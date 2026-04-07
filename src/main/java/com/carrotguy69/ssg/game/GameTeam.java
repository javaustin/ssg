package com.carrotguy69.ssg.game;

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

    private final Map<String, Integer> stats = new HashMap<>();

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
        this.players.add(gp);
    }

    public void removePlayer(GamePlayer gp) {
        this.players.remove(gp);
    }

    public int getStat(String key) {
        return stats.getOrDefault(key, 0);
    }

    public void addStat(String key, int value) {
        stats.put(key, getStat(key) + value);
    }

    public void setStat(String key, int value) {
        stats.put(key, value);
    }

}
