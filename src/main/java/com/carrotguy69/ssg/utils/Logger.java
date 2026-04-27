package com.carrotguy69.ssg.utils;

import org.bukkit.Bukkit;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static com.carrotguy69.cxyz.CXYZ.plugin;

public class Logger {

    private static final Path latestLog = Paths.get("logs/latest.log");

    private static void toLatestLog(String text) {

        Bukkit.getScheduler().runTask(plugin, () -> {
            try (BufferedWriter writer = Files.newBufferedWriter(latestLog, StandardOpenOption.APPEND)) {
                writer.write("[" + java.time.LocalDateTime.now() + "] [Server thread/INFO]: " + text);
                writer.newLine();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void logStackTrace(Exception ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));

        plugin.getLogger().severe(ex.getMessage());
        String join = String.join("\n", sw.toString().split("\n")); // Stack-trace text

        plugin.getLogger().info(join);
        toLatestLog(join);
    }

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
