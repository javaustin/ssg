package com.carrotguy69.ssg.utils;

import com.carrotguy69.cxyz.exceptions.InvalidConfigException;
import com.carrotguy69.ssg.SpeedSG;
import com.carrotguy69.ssg.cmd.Print;
import com.carrotguy69.ssg.cmd.game._GameSupercommand;
import com.carrotguy69.ssg.cmd.team._TeamSupercommand;
import com.carrotguy69.ssg.game.loot.LootTable;
import com.carrotguy69.ssg.game.map.GameMap;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Objects;

import static com.carrotguy69.ssg.SpeedSG.*;


public class Startup {

    public static void loadConfigYMLs() {
        File dataFolder = plugin.getDataFolder();

        // for config.yml
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();

        configYML = plugin.getConfig();


        // for messages.yml
        File msgYMLFile = new File(dataFolder, "messages.yml");

        if (!msgYMLFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        msgYML = YamlConfiguration.loadConfiguration(msgYMLFile);

        // for map.yml
        File mapYMLFile = new File(dataFolder, "maps.yml");

        if (!mapYMLFile.exists()) {
            plugin.saveResource("maps.yml", false);
        }

        mapYML = YamlConfiguration.loadConfiguration(mapYMLFile);

        // for loot.yml
        File lootYMLFile = new File(dataFolder, "loot.yml");

        if (!lootYMLFile.exists()) {
            plugin.saveResource("loot.yml", false);
        }

        lootYML = YamlConfiguration.loadConfiguration(lootYMLFile);
    }

    public static void registerCommands() {

        Objects.requireNonNull(plugin.getCommand("print")).setExecutor(Print.executor);
        Objects.requireNonNull(plugin.getCommand("print")).setTabCompleter(Print.tabCompleter);

        Objects.requireNonNull(plugin.getCommand("game")).setExecutor(_GameSupercommand.executor);
        Objects.requireNonNull(plugin.getCommand("game")).setTabCompleter(_GameSupercommand.tabCompleter);

        Objects.requireNonNull(plugin.getCommand("team")).setExecutor(_TeamSupercommand.executor);
        Objects.requireNonNull(plugin.getCommand("team")).setTabCompleter(_TeamSupercommand.tabCompleter);
    }

    public static void registerBukkitEvents() {
        plugin.getServer().getPluginManager().registerEvents(plugin, plugin);
    }

    public static void startTasks() {

    }

    public static void loadConstants() {
        gameMaps = GameMap.loadMaps();

        if (gameMaps.stream().noneMatch(gameMap -> gameMap.getID().equalsIgnoreCase("lobby"))) {
            throw new InvalidConfigException("maps.yml", "lobby", "Lobby map not found!");
        }
        else {
            // Ensure that lobby map is not being used as game maps.
            GameMap lobbyMap = GameMap.getByID("lobby");
            gameMaps.remove(lobbyMap);

            SpeedSG.lobbyMap = lobbyMap;
        }

        lootTables = LootTable.loadLootTables();
    }
}
