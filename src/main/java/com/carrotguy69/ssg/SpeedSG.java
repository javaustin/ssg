package com.carrotguy69.ssg;

import com.carrotguy69.cxyz.CXYZ;
import com.carrotguy69.cxyz.exceptions.InvalidConfigException;
import com.carrotguy69.cxyz.models.db.NetworkPlayer;
import com.carrotguy69.ssg.game.other.DamageSource;
import com.carrotguy69.ssg.game.Game;
import com.carrotguy69.ssg.game.GamePlayer;
import com.carrotguy69.ssg.game.GameState;
import com.carrotguy69.ssg.game.loot.LootTable;
import com.carrotguy69.ssg.game.map.GameMap;
import com.carrotguy69.ssg.messages.MessageGrabber;
import com.carrotguy69.ssg.messages.SSGMessageKey;
import com.carrotguy69.ssg.messages.utils.MapFormatters;
import com.carrotguy69.ssg.utils.Startup;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static SpeedSG plugin;
    public static CXYZ cxyz;

    /*

    TODO:
    - We can use the cxyz MessageUtils send and parser functions (because they return a non plugin dependent object),
    but not their message grabbers (we can't access their messages.yml so we can't get their MessageKey's).
    ^ (Keep this note)

    - we need to be the authoritative handlers of chat in game, cxyz thinks it is. so now cxyz is processing all chat messages and so is the game.

    - test create command and fix tab completer and other things

    - InvalidConfigException is ambiguous and does not imply that it throws when files, worlds, or other args are invalid. It only implies the format.
      We still probably want to keep this exception, but we can use builtin exceptions like FileNotFoundException more often

    - Do not throw exceptions from the CXYZ package

    - Create a "ready" system to skip a countdown


    What is a game?
    - Is a game cyclical or does it end after one game. Is it treated as a lobby? a server?
    - Should this plugin support multiple games at once? Or does that violate the nature of it?
    - With multiple games how do we ensure maps don't override each other? Technically two games can use the same map right now.
    - If we support a single game, how should we go about adding players and removing players to/from the game?
    */

    @Override
    public void onEnable() {
        plugin = JavaPlugin.getPlugin(SpeedSG.class);

        Startup.loadConfigYMLs();
        Startup.loadConstants();
        Startup.registerCommands();
        Startup.registerEvents();


        lobbyMap = GameMap.getByID("lobby");

        if (lobbyMap == null) {
            throw new InvalidConfigException("maps.yml", "lobby", "Lobby map not found!");
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
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

        // Ignore damages beyond the scope of this handler (anything that could possibly involve a player) (do NOT cancel)
        if (
                cause == EntityDamageEvent.DamageCause.PROJECTILE ||
                cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK ||
                cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
        ) {
            return;
        }

        // Any other death type represents "NATURAL" damage.

        double hp = p.getHealth() - e.getFinalDamage();

        if (hp <= 0) {
            game.eliminate(gp);
        }
    }

    @EventHandler
    public void onDamageByPlayer(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player p)) {
            return;
        }

        Entity attackerEntity = e.getEntity();
        Player attacker = null;
        DamageSource.Reason reason = null;

        if (attackerEntity.getType() == EntityType.PLAYER) {
            assert attackerEntity instanceof Player;
            attacker = (Player) attackerEntity;
            reason = DamageSource.Reason.MELEE;
        }

        // I suppose we'll cover both arrow cases
        else if (attackerEntity.getType() == EntityType.ARROW || attackerEntity.getType() == EntityType.SPECTRAL_ARROW) {
            assert attackerEntity instanceof Arrow;
            Arrow arrow = (Arrow) attackerEntity;

            if (arrow.getShooter() instanceof Player) {
                attacker = (Player) arrow.getShooter();
                reason = DamageSource.Reason.PROJECTILE;
            }
        }

        if (reason == null) {
            return;
        }

        // todo: When an explosion occurs, players will immediately be killed before we can cancel damage through here. To prevent this immediate killing, we will need to
        // (eventually) cancel all explosions and replace them with an explosion particle effect and sound.

        Game game = Game.getByPlayer(p);

        if (game == null) {
            return;
        }

        GamePlayer gp = game.getPlayer(p);
        GamePlayer attackerGP = game.getPlayer(attacker);

        DamageSource source = new DamageSource(attackerGP, reason);

        game.setLastDamageSource(gp, source);

        double hp = p.getHealth() - e.getFinalDamage();

        if (hp <= 0) {
            game.eliminate(gp);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    @SuppressWarnings("deprecation")
    public void onChat(AsyncPlayerChatEvent e) {

        Player p = e.getPlayer();
        String content = e.getMessage();
        NetworkPlayer np = NetworkPlayer.getPlayerByUUID(p.getUniqueId());

        Game game = Game.getByPlayer(p);

        if (game == null) {
            return;
        }

        // handle lobby chat, game chat

        Map<String, Object> commonMap = MapFormatters.gamePlayerFormatter(game.getPlayer(p));
        commonMap.putAll(MapFormatters.gameFormatter(game));
        commonMap.put("message", content);
        commonMap.put("content", content);

        switch (game.getGameState()) {
            case WAITING:
            case RESET:
                game.announce(MessageGrabber.grab(SSGMessageKey.LOBBY_CHAT), commonMap, List.of(), np);
                break;

            case ACTIVE:
            case STARTING:
            case ENDING:
                game.announce(MessageGrabber.grab(SSGMessageKey.GAME_CHAT), commonMap, List.of(), np);
                break;
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
    public void onSnowball(ProjectileHitEvent e) {
        // Reinstate projectile (snowball, egg, ...) knockback similar to 1.8.
        if (!(e.getHitEntity() instanceof Player p)) {
            return;
        }

        Game game = Game.getByPlayer(p);

        if (game == null) {
            return;
        }

        if (game.getGameState() != GameState.ACTIVE) {
            return;
        }

        if (!(e.getEntity() instanceof Snowball || e.getEntity() instanceof Egg)) {
            return;
        }

        Vector kbVector = e.getEntity().getVelocity().normalize().multiply(0.35).setY(0.25);

        // Player::damage() effectively shakes the screen like normal damage/kb.
        p.damage(0);

        p.setVelocity(p.getVelocity().add(kbVector));
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
}
