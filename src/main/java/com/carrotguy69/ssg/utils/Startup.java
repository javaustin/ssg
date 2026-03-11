package com.carrotguy69.ssg.utils;

import com.carrotguy69.ssg.cmd.Test;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Objects;

import static com.carrotguy69.ssg.SpeedSG.msgYML;
import static com.carrotguy69.ssg.SpeedSG.plugin;


public class Startup {

    public static void loadConfigYMLs() {

        // for config.yml
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();

        // for messages.yml
        File dataFolder = plugin.getDataFolder();

        File msgYMLFile = new File(dataFolder, "messages.yml");

        if (!msgYMLFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        msgYML = YamlConfiguration.loadConfiguration(msgYMLFile);
    }

    public static void registerCommands() {
        Objects.requireNonNull(Bukkit.getPluginCommand("test")).setExecutor(new Test());
    }

    public static void registerEvents() {
        plugin.getServer().getPluginManager().registerEvents(plugin, plugin);
    }

    public static void startTasks() {
        Objects.requireNonNull(Bukkit.getPluginCommand("test")).setTabCompleter(new com.carrotguy69.ssg.tabCompleters.Test());
    }

    public static void loadConstants() {

    }
}
