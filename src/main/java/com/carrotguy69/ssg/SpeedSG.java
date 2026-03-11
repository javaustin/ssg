package com.carrotguy69.ssg;

import com.carrotguy69.cxyz.CXYZ;
import com.carrotguy69.ssg.utils.Startup;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public final class SpeedSG extends JavaPlugin implements Listener {

    public static String f(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static FileConfiguration msgYML = null;

    public static SpeedSG plugin;
    public static CXYZ cxyz;

    /*
    TODO:

        Game:
            GameMap {
                private final String name;
                private final MapSourceType sourceType;
                private final String source;
                private final Location pasteLocation;
                private final List<Location> spawns;
                private final BoundingBox bounds;

                public GameMap
            }

            GameRules {
                private int minPlayers;
                private int maxPlayers;

                private int minTeams;
                private int maxTeams;

                private int minTeamSize;
                private int maxTeamSize;

                private LinkedHashMap<String, Integer> timings; // timings.put("invulnerability", 15). Then in runtime use timings.get("invulnerability") and that's your custom timing.
            }

            GameInstance {
                private GameMap map;
                private GameRules rules;

                private int gameID;
                private GameState gameState;
                private List<Team> teams;
                private List<GamePlayer> players;

                public void assignTeams();
                public void assignTeam(GamePlayer gp, @Nullable Team team);
                public void addPlayer(GamePlayer gp);
                public void removePlayer(GamePlayer gp);

                public void start();
                public void end();
                public void reset();

                GamePlayer {
                    private UUID uuid;
                    private boolean alive;
                    private int kills;
                    private Team team;

                    public NetworkPlayer getNetworkPlayer();
                }

                Team {
                    private int index;
                    private String displayName;
                    private List<GamePlayer> members;
                    private int maxSize;

                    public boolean isAlive();
                    public boolean isFull();
                    public List<GamePlayer> getAliveMembers();
                }


            }

    on join:
        - vanish hook

    on leave:
        - vanish hook

     */


    @Override
    public void onEnable() {
        cxyz = JavaPlugin.getPlugin(CXYZ.class);
        plugin = JavaPlugin.getPlugin(SpeedSG.class);

        Startup.loadConfigYMLs();
        Startup.loadConstants();
        Startup.registerCommands();
        Startup.registerEvents();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
