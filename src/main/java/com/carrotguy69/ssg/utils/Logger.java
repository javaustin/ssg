package com.carrotguy69.ssg.utils;

import org.bukkit.Bukkit;

import static com.carrotguy69.ssg.SpeedSG.plugin;

public class Logger {

    public static void info(String content) {
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getLogger().info(content));
    }

    public static void warning(String content) {
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getLogger().warning(content));
    }

    public static void severe(String content) {
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getLogger().severe(content));
    }

}
