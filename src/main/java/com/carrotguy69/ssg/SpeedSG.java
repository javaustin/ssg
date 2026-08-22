package com.carrotguy69.ssg;

import com.carrotguy69.cxyz.CXYZ;
import com.carrotguy69.cxyz.events.custom.PublicChatEvent;
import com.carrotguy69.cxyz.events.custom.VanishToggleEvent;
import com.carrotguy69.cxyz.events.custom.base.Priority;
import com.carrotguy69.cxyz.events.custom.service.EventService;
import com.carrotguy69.cxyz.utils.NumberRange;
import com.carrotguy69.ssg.cmd.game.Create;
import com.carrotguy69.ssg.eventHandler.CoreChatHandler;
import com.carrotguy69.ssg.eventHandler.VanishHandler;
import com.carrotguy69.ssg.game.other.DamageSource;
import com.carrotguy69.ssg.game.Game;
import com.carrotguy69.ssg.game.GamePlayer;
import com.carrotguy69.ssg.game.GameState;
import com.carrotguy69.ssg.game.loot.LootTable;
import com.carrotguy69.ssg.game.map.GameMap;
import com.carrotguy69.ssg.messages.utils.MapFormatters;
import com.carrotguy69.ssg.other.Logger;
import com.carrotguy69.ssg.other.Startup;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.UUID;

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
    public static GameMap lobbyMap = null;
    public static Map<String, Game> gameIDMap = new HashMap<>();
    public static List<GameMap> gameMaps = new ArrayList<>();
    public static List<LootTable> lootTables = new ArrayList<>();

    public static List<String> lobbyScoreboardLines = new ArrayList<>();
    public static List<String> gameScoreboardLines = new ArrayList<>();

    public static List<UUID> noInteractionTicks = new ArrayList<>();

    public static boolean scoreboardsEnabled = true;

    public static boolean autoJoinEnabled = false;
    public static AutoJoinScope autoJoinScope;

    public enum AutoJoinScope {
        SERVER,
        WORLD;
        public static AutoJoinScope fromString(@Nullable String s) {
            return s != null && s.toUpperCase().equals(SERVER.name()) ? SERVER : WORLD;
        }
    }


    public static SpeedSG plugin;
    public static CXYZ cxyz;

    /*

    TODO:
        - better config files (good descriptions of keys and examples)
        - fulfill config files with all applicable examples
        - better README.md (description, features, hyperlinks to config)
    */

    @Override
    public void onEnable() {
        plugin = JavaPlugin.getPlugin(SpeedSG.class);

        Startup.loadConfigYMLs();
        Startup.loadConstants();
        Startup.registerCommands();
        Startup.registerBukkitEvents();

        // Register event handler with the core plugin's EventService
        EventService.registerHandler(PublicChatEvent.class, new CoreChatHandler(), Priority.NORMAL);
        EventService.registerHandler(VanishToggleEvent.class, new VanishHandler(), Priority.NORMAL);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        Logger.info("See ya later!");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!autoJoinEnabled) {
            return;
        }

        if (!(autoJoinScope == AutoJoinScope.SERVER || (autoJoinScope == AutoJoinScope.WORLD && e.getPlayer().getWorld().equals(GameMap.getMaps().getFirst().getWorld())))) {
            return;
        }

        Game game;
        try {
            game = SpeedSG.gameIDMap.values().stream().max(Comparator.comparingInt(g -> g.getPlayers().size())).stream().findFirst().orElseThrow();
        }
        catch (NoSuchElementException ex) {
            game = new Game(
                    Create.generateValidGameID(),
                    gameMaps.size() - 1 > 0
                            ? new ArrayList<>(gameMaps).get(new Random().nextInt(0, gameMaps.size() - 1))
                            : new ArrayList<>(gameMaps).getFirst(),
                    lootTables.size() - 1 > 0 ? lootTables.get(new Random().nextInt(0, lootTables.size() - 1)) : lootTables.getFirst(),
                    new NumberRange(2, Math.max(configYML.getStringList("game.teams.names").size(), configYML.getStringList("game.teams.short-names").size())),
                    new NumberRange(1, 4),
                    1
            );
        }

        GamePlayer gamePlayer = new GamePlayer(e.getPlayer().getUniqueId());
        game.addPlayer(gamePlayer);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        Player p = e.getPlayer();

        Game game = Game.getByPlayer(p);

        if (game != null) {
            GamePlayer gp = game.getPlayer(p);
            game.removePlayer(gp);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        // traditionally this handler is only used for natural damages such as fall damage.
        if (!(e.getEntity() instanceof Player p)) {
            return;
        }

        Game game = Game.getByPlayer(p);

        if (game == null) {
            return;
        }

        GamePlayer gp = game.getPlayer(p);

        if (game.invulEnabled) {
            e.setCancelled(true);
            return;
        }

        EntityDamageEvent.DamageCause cause = e.getCause();

        if (cause == EntityDamageEvent.DamageCause.LIGHTNING) {
            e.setCancelled(true);
            return;
        }

        if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK || cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK || cause == EntityDamageEvent.DamageCause.PROJECTILE) {
            return;
        }

        double damageTaken = gp.getTemporaryStat("damage-taken", 0.0);
        gp.setTemporaryStat("damage-taken", damageTaken + e.getFinalDamage());

        double hp = p.getHealth() - e.getFinalDamage();

        if (hp <= 0) {
            e.setCancelled(true);
            game.eliminate(gp);
        }
    }

    @EventHandler
    public void onDamageByPlayer(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player p)) {
            return;
        }

        Entity attackerEntity = e.getDamager();
        Player attacker = null;
        DamageSource.Reason reason = null;

        if (attackerEntity.getType() == EntityType.PLAYER) {
            assert attackerEntity instanceof Player;
            attacker = (Player) attackerEntity;
            reason = DamageSource.Reason.MELEE;
        }

        else if (attackerEntity instanceof Projectile projectile) {
            assert attackerEntity instanceof Arrow;

            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
                reason = DamageSource.Reason.PROJECTILE;
            }
        }

        Game game = Game.getByPlayer(p);

        if (game == null) {
            return;
        }

        if (game.invulEnabled)
            return;

        GamePlayer gp = game.getPlayer(p); // The above check ensures that the game player is not null (because the player is sourced from a game)
        GamePlayer attackerGP = null;

        if (attacker != null) {
            attackerGP = game.getPlayer(attacker);
        }

        if (attackerGP == null) { // attacker was outside the game
            e.setCancelled(true);
        }

        else {
            DamageSource source = new DamageSource(attackerGP, reason);
            game.setLastDamageSource(gp, source);

            double damageTaken = gp.getTemporaryStat("damage-taken", 0.0);
            gp.setTemporaryStat("damage-taken", damageTaken + e.getFinalDamage());

            double damageDealt = gp.getTemporaryStat("damage-dealt", 0.0);
            attackerGP.setTemporaryStat("damage-dealt", damageDealt + e.getFinalDamage());
        }

        double hp = p.getHealth() - e.getFinalDamage();
        if (hp <= 0) {
            e.setCancelled(true);
            game.eliminate(gp);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent e) {
        // Do not allow hunger change when game is not active (keep them fed until game time)

        Player p = (Player) e.getEntity();
        Game game = Game.getByPlayer(p);

        if (game == null) {
            return;
        }

        if (game.getGameState() != GameState.ACTIVE) {
            e.setFoodLevel(20);
        }
    }

    @EventHandler
    public void onPearl(PlayerTeleportEvent e) {
        // Easiest way to cancel pearl damage is to cancel the pearl event and teleport the player ourselves (and play the pearl sound).

        Player p = e.getPlayer();

        Game game = Game.getByPlayer(p);

        if (game == null) {
            return;
        }

        if (game.getGameState() != GameState.ACTIVE) {
            return;
        }

        if (e.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            p.teleport(e.getTo());
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_TELEPORT, 1.0f, 1.0f);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Game game = Game.getByPlayer(e.getPlayer());

        if (game == null) {
            return;
        }

        e.getPlayer().teleport(game.getGameMap().getSpawns().getFirst());
    }

    @EventHandler
    public void onContainerOpen(InventoryOpenEvent e) {
        Player p = (Player) e.getPlayer();

        Game game = Game.getByPlayer(p);

        if (game == null) {
            return;
        }

        if ((e.getInventory().getType() != InventoryType.PLAYER && e.getInventory().getType() != InventoryType.CHEST)) {
             e.setCancelled(true);
        }

    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {

        Game game = Game.getByPlayer(e.getPlayer());

        if (game == null) {
            return;
        }

        ItemStack hand = e.getPlayer().getInventory().getItemInMainHand();

        ConfigurationSection section = configYML.getConfigurationSection("game.click-actions");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    Material material = Material.valueOf(key.toUpperCase().replace("-", "_"));
                    String actionTypeString = section.getString(key + ".click-type", "RIGHT_CLICK");

                    if (!e.getAction().name().startsWith(actionTypeString.toUpperCase().replace("-", "_")) || material != hand.getType()) {
                        continue;
                    }

                    List<String> actions = section.getStringList(key + ".actions");

                    Game.runConfigCommands(actions, MapFormatters.gamePlayerFormatter(game.getPlayer(e.getPlayer())));
                }
                catch (IllegalArgumentException ex) {
                    Logger.info("Failed to run click action command because %s is not a valid item!".formatted(key));
                }
            }

        }
    }

    @EventHandler
    public void onInventory(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();

        Game game = Game.getByPlayer(p);

        if (game == null) {
            return;
        }

        if (game.getGameState() == GameState.WAITING && p.getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
            return;
        }

    }

    @EventHandler
    public void onInventory(InventoryDragEvent e) {
        Player p = (Player) e.getWhoClicked();

        Game game = Game.getByPlayer(p);

        if (game == null) {
            return;
        }

        if (game.getGameState() == GameState.WAITING && p.getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
            return;
        }

    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();

        Game game = Game.getByPlayer(p);

        if (game == null) {
            return;
        }

        if (game.getGameState() == GameState.WAITING && p.getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
            noInteractionTicks.add(e.getPlayer().getUniqueId());

            new BukkitRunnable() {public void run() {
                noInteractionTicks.remove(e.getPlayer().getUniqueId());
            }}.runTaskLater(this, 1);
            return;
        }
    }

}
