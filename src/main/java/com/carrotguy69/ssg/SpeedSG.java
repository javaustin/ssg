package com.carrotguy69.ssg;

import com.carrotguy69.cxyz.CXYZ;
import com.carrotguy69.ssg.game.loot.LootTable;
import com.carrotguy69.ssg.game.map.GameMap;
import com.carrotguy69.ssg.utils.Logger;
import com.carrotguy69.ssg.utils.Startup;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class SpeedSG extends JavaPlugin implements Listener {

    public static String f(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    // Core configurations
    public static FileConfiguration configYML = null;
    public static FileConfiguration msgYML = null;

    // Game configurations
    public static FileConfiguration mapYML = null;
    public static FileConfiguration lootYML = null;

    // Global variables
    public static List<GameMap> gameMaps = null;

    public static List<LootTable> lootTables = null;

    public static SpeedSG plugin;
    public static CXYZ cxyz;


    /*

    TODO:
    - InvalidConfigurationException is ambiguous and does not imply that it throws when files, worlds, or other args are invalid. It only implies the format.
      We still probably want to keep this exception, but we can use builtin exceptions like FileNotFoundException more often

    - Do not throw exceptions from the CXYZ package

    */

    @Override
    public void onEnable() {
        cxyz = JavaPlugin.getPlugin(CXYZ.class);
        plugin = JavaPlugin.getPlugin(SpeedSG.class);

        Startup.loadConfigYMLs();
        Startup.loadConstants();
        Startup.registerCommands();
        Startup.registerEvents();

        Logger.info("hi");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
