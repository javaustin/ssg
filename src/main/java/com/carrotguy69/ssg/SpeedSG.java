package com.carrotguy69.ssg;

import com.carrotguy69.cxyz.CXYZ;
import com.carrotguy69.ssg.utils.Logger;
import com.carrotguy69.ssg.utils.Startup;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpeedSG extends JavaPlugin implements Listener {

    public static String f(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static FileConfiguration msgYML = null;
    public static FileConfiguration mapYML = null;

    public static SpeedSG plugin;
    public static CXYZ cxyz;


    /*

    TODO:

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
