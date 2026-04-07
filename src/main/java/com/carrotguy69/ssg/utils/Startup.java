package com.carrotguy69.ssg.utils;

import com.carrotguy69.ssg.cmd.Print;
import com.carrotguy69.ssg.cmd.Test;
import com.carrotguy69.ssg.game.loot.LootTable;
import com.carrotguy69.ssg.game.map.GameMap;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Objects;

import static com.carrotguy69.ssg.SpeedSG.plugin;
import static com.carrotguy69.ssg.SpeedSG.configYML;
import static com.carrotguy69.ssg.SpeedSG.msgYML;
import static com.carrotguy69.ssg.SpeedSG.mapYML;
import static com.carrotguy69.ssg.SpeedSG.lootYML;
import static com.carrotguy69.ssg.SpeedSG.gameMaps;
import static com.carrotguy69.ssg.SpeedSG.lootTables;


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
        File mapYMLFile = new File(dataFolder, "map.yml");

        if (!mapYMLFile.exists()) {
            plugin.saveResource("map.yml", false);
        }

        mapYML = YamlConfiguration.loadConfiguration(mapYMLFile);

        // for loot.yml
        File lootYMLFile = new File(dataFolder, "loot.yml");

        if (!mapYMLFile.exists()) {
            plugin.saveResource("loot.yml", false);
        }

        lootYML = YamlConfiguration.loadConfiguration(lootYMLFile);
    }

    public static void registerCommands() {
        Objects.requireNonNull(plugin.getCommand("test")).setExecutor(new Test());
        Objects.requireNonNull(plugin.getCommand("test")).setTabCompleter(new com.carrotguy69.ssg.tabCompleters.Test());

        Objects.requireNonNull(plugin.getCommand("print")).setExecutor(new Print());
        Objects.requireNonNull(plugin.getCommand("print")).setTabCompleter(new com.carrotguy69.ssg.tabCompleters.Print());
    }

    public static void registerEvents() {
        plugin.getServer().getPluginManager().registerEvents(plugin, plugin);
    }

    public static void startTasks() {

    }

    public static void loadConstants() {
        gameMaps = GameMap.loadMaps();
        lootTables = LootTable.loadLootTables();
    }
}
