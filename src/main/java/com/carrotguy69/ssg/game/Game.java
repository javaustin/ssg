package com.carrotguy69.ssg.game;

import com.carrotguy69.cxyz.CXYZ;
import com.carrotguy69.cxyz.messages.MessageUtils;
import com.carrotguy69.cxyz.models.config.channel.registry.ChannelFunction;
import com.carrotguy69.cxyz.models.config.channel.registry.ChannelRegistry;
import com.carrotguy69.cxyz.models.db.GameStat;
import com.carrotguy69.cxyz.models.db.NetworkPlayer;
import com.carrotguy69.cxyz.utils.BroadcastUtils;
import com.carrotguy69.cxyz.utils.TimeUtils;
import com.carrotguy69.ssg.SpeedSG;
import com.carrotguy69.ssg.exceptions.TeamFullException;
import com.carrotguy69.ssg.game.loot.LootTable;
import com.carrotguy69.ssg.game.map.GameMap;
import com.carrotguy69.ssg.game.other.DamageSource;
import com.carrotguy69.ssg.game.other.Durations;
import com.carrotguy69.ssg.messages.MessageGrabber;
import com.carrotguy69.ssg.messages.SSGMessageKey;
import com.carrotguy69.ssg.messages.utils.MapFormatters;
import com.carrotguy69.ssg.utils.objects.ColorUtils;
import com.carrotguy69.ssg.utils.objects.NumberRange;

import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang3.tuple.Pair;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;

import static com.carrotguy69.cxyz.CXYZ.random;
import static com.carrotguy69.cxyz.messages.MessageUtils.formatPlaceholders;
import static com.carrotguy69.cxyz.utils.TimeUtils.unixTimeNow;

import static com.carrotguy69.ssg.SpeedSG.*;
import static com.carrotguy69.ssg.messages.SSGMessageKey.*;

public class Game {

    private GameMap map;

    private final String gameID;
    private GameState gameState;

    private final List<GameTeam> teams;
    private final List<GamePlayer> players;
    private final List<Integer> taskIDs;

    private NumberRange amountOfTeams;
    private NumberRange teamCapacity;

    public Durations durations;

    public boolean counting = false;

    private final GameMode defaultGamemode;

    // Runtime specific variables
    public boolean invulEnabled = true;
    private final List<Block> barrierBlocks = new ArrayList<>();
    private List<Block> chests = new ArrayList<>();
    private final Map<GamePlayer, DamageSource> playerLastDamageSourceMap = new Hashtable<>();
    private LootTable lootTable;

    public int maxLives;
    public int originalPlayersSize = 0;
    public int originalTeamsSize = 0;
    public int elapsedSeconds = 0;

    boolean frozen = false;


    // Updated settings (settings that can't be changed in the middle of the script, but will be applied during our transfer method)
    public GameMap nextMap;
    public LootTable nextLootTable;
    public NumberRange nextAmountOfTeams;
    public NumberRange nextTeamCapacity;
    public int nextMaxLives;

    public Game(String id, GameMap map, LootTable lootTable, NumberRange amountOfTeams, NumberRange teamCapacity, int maxLives) {

        this.gameID = id.toLowerCase();
        this.map = map;

        if (map.getID().equalsIgnoreCase("lobby")) {
            throw new RuntimeException("The lobby cannot be used as a game map.");
        }

        map.isInUse = true;

        this.teams = new ArrayList<>();
        this.players = new ArrayList<>();
        this.taskIDs = new ArrayList<>();

        this.maxLives = maxLives;

        this.amountOfTeams = amountOfTeams;

        this.teamCapacity = teamCapacity;

        this.lootTable = lootTable;

        this.durations = new Durations();

        createTeams(amountOfTeams.max().intValue());

        this.gameState = GameState.WAITING;

        this.defaultGamemode = GameMode.valueOf(configYML.getString("game.misc.default-gamemode", "adventure").toUpperCase());

        nextMap = map;
        nextLootTable = lootTable;
        nextAmountOfTeams = amountOfTeams;
        nextTeamCapacity = teamCapacity;
        nextMaxLives = maxLives;

        initialize();
    }

    private void initialize() {
        /*
        1. Ensure lobby map is loaded
        2. Try to start a lobby countdown
        */

        try {
            lobbyMap.paste(); // Will only paste if specified as a world_copy or schematic
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        if (lobbyMap.getWorldBorderSettings().isBorderEnabled()) {
            lobbyMap.getWorld().getWorldBorder().setCenter(Math.round(lobbyMap.getBounds().getCenterX()), Math.round(lobbyMap.getBounds().getCenterZ()));
            lobbyMap.getWorld().getWorldBorder().setSize(Math.round(Math.max(lobbyMap.getBounds().getWidthX(), lobbyMap.getBounds().getWidthZ())));
        }
        else {
            lobbyMap.getWorld().getWorldBorder().setSize(1_000_000);
        }


        lobbyMap.getWorld().setSpawnLocation(lobbyMap.getSpawns().getFirst());

        tryLobbyCountdown();
    }

    public void addPlayer(GamePlayer gp) {

        if (gp == null) {
            return;
        }

        for (Map.Entry<String, Game> entry : gameIDMap.entrySet()) {
            Game game = entry.getValue();

            if (game.getPlayers().contains(gp)) {
                throw new RuntimeException("A player may not be in two games at once!");
            }
        }

        players.add(gp);
        Player p = gp.getBukkitPlayer();

        gp.setTemporaryStat("kills", 0);
        gp.setLives(maxLives);

        if (gameState == GameState.WAITING) {
            spawnPlayer(p, lobbyMap.getSpawns().size() > 1 ? lobbyMap.getSpawns().get(new Random().nextInt(0, lobbyMap.getSpawns().size() - 1)) : lobbyMap.getSpawns().getFirst());

            this.announce(
                    MessageGrabber.grab(LOBBY_JOIN),
                    MapFormatters.gamePlayerFormatter(gp),
                    List.of()
            );

            if (isPlayable()) {
                tryLobbyCountdown();
            }
        }

        else if (gameState == GameState.STARTING) {

            boolean allowJoinDuringStart = configYML.getBoolean("game.misc.allow-join-during-start", false);

            if (allowJoinDuringStart) {

                p.setGameMode(defaultGamemode);

                // Stealing the code from spawnTeams() to teleport the player to a predetermined spawn based on the team index.
                GameTeam team = assignTeam(gp);
                int spawnIndex = (int) Math.ceil((double) team.getIndex() / teams.size()) * map.getSpawns().size();

                spawnPlayer(p, map.getSpawns().get(spawnIndex));

                Map<String, Object> commonMap = MapFormatters.gamePlayerFormatter(gp);

                this.announce(
                        MessageGrabber.grab(GAME_JOIN),
                        commonMap,
                        List.of()
                );

                MessageUtils.sendParsedMessage(
                        p,
                        MessageGrabber.grab(TEAM_JOIN),
                        commonMap
                );
                team.sendTeamMessage(
                        MessageGrabber.grab(TEAM_JOIN_ANNOUNCEMENT),
                        commonMap,
                        List.of(gp)
                );
            }

            else {
                handleJoinMidGame(gp);
            }
        }

        else {
            handleJoinMidGame(gp);
        }

        originalPlayersSize = this.getPlayers().size();
        originalTeamsSize = this.getTeams().size();

        updateScoreboard();
    }

    public void removePlayer(@Nullable GamePlayer gp) {

        if (gp == null) {
            return;
        }

        Map<String, Object> commonMap = MapFormatters.gamePlayerFormatter(gp);

        if (gameState == GameState.WAITING) {
            this.announce(
                    MessageGrabber.grab(LOBBY_LEAVE),
                    commonMap,
                    List.of()
            );
        }

        else {

            this.announce(
                    MessageGrabber.grab(GAME_LEAVE),
                    commonMap,
                    List.of()
            );

            if (gameState == GameState.ACTIVE) {
                gp.setLives(1);
                eliminate(gp);
            }

            if (gp.getTeam() != null) {
                gp.getTeam().removePlayer(gp);
            }
        }

        closeScoreboard(gp.getBukkitPlayer());

        while (players.contains(gp)) {
            players.remove(gp);
        }

        originalPlayersSize = this.getPlayers().size();
        originalTeamsSize = this.getTeams().size();


        updateScoreboard();
    }

    public void announce(String unparsedContent, Map<String, Object> formatMap, List<GamePlayer> excludingPlayers) {
        announce(unparsedContent, formatMap, excludingPlayers, null);
    }

    public void announce(String unparsedContent, Map<String, Object> formatMap, List<GamePlayer> excludingPlayers, @Nullable NetworkPlayer sender) {
        TextComponent component = MessageUtils.createMessage(unparsedContent, formatMap);

        for (GamePlayer gp : this.players) {
            NetworkPlayer np = NetworkPlayer.resolvePlayer(gp.getUUID());

            if (gp.getBukkitPlayer() == null) {
                continue;
            }

            if (np != null && sender != null) {
                if (np.isIgnoring(sender)) {
                    continue;
                }

                if (np.isMutingChannel(ChannelRegistry.getChannelByFunction(ChannelFunction.PUBLIC))) {
                    continue;
                }
            }

            if (excludingPlayers.contains(gp))
                continue;

            Player p = gp.getBukkitPlayer();

            p.sendMessage(component);
        }
    }

    private void cancelStart() {

        gameState = GameState.WAITING;

        List<Player> players = this.getBukkitPlayers();

        int nSpawns = lobbyMap.getSpawns().size();
        int nPlayers = players.size();

        if (lobbyMap.getWorldBorderSettings().isBorderEnabled()) {
            lobbyMap.getWorld().getWorldBorder().setCenter(Math.round(lobbyMap.getBounds().getCenterX()), Math.round(lobbyMap.getBounds().getCenterZ()));
            lobbyMap.getWorld().getWorldBorder().setSize(Math.round(Math.max(lobbyMap.getBounds().getWidthX(), lobbyMap.getBounds().getWidthZ())));
        }
        else {
            lobbyMap.getWorld().getWorldBorder().setSize(lobbyMap.getWorld().getWorldBorder().getMaxSize());
        }

        lobbyMap.getWorld().setSpawnLocation(lobbyMap.getSpawns().getFirst());


        for (int i = 0; i < nPlayers; i++) {
            int j = (i < nSpawns) ? i : (i % nSpawns);

            spawnPlayer(players.get(i), lobbyMap.getSpawns().get(j));
        }

        tryLobbyCountdown();
    }

    private void handleJoinMidGame(GamePlayer gp) {
        Player p = gp.getBukkitPlayer();

        // Message
        MessageUtils.sendParsedMessage(
                p,
                MessageGrabber.grab(INFO_MID_GAME_JOIN_MESSAGE),
                Map.of()
        );

        BroadcastUtils.sendTitle(
                List.of(p),
                MessageGrabber.grab(MID_GAME_JOIN_TITLE),
                MessageGrabber.grab(MID_GAME_JOIN_SUBTITLE),
                msgYML.getInt(MID_GAME_JOIN_FADE_IN_TICKS.getPath(), 0),
                msgYML.getInt(MID_GAME_JOIN_STAY_TICKS.getPath(), 40),
                msgYML.getInt(MID_GAME_JOIN_FADE_OUT_TICKS.getPath(), 20)
        );

        p.setGameMode(GameMode.SPECTATOR);

        // Teleport to any alive player
        p.teleport(getAlivePlayers().getFirst().getBukkitPlayer().getLocation());
    }

    private void tryLobbyCountdown() {
        /*
        Lobby countdown:
        - gs == WAITING
        - Players are at the lobby spawn point
        - Teams are not assigned and switching is allowed
        - Leaving may cancel the countdown if it leaves an insufficient amount of players
        - If a lobby countdown finalizes (without breaking), it triggers the start() method which triggers the game countdown.
         */

        // Will return if there is already a countdown in progress.
        if (counting) {
            return;
        }


        int id = new BukkitRunnable() {public void run() {
            updateScoreboard();

            if (frozen) {
                return;
            }

            counting = true;


            if (!isPlayable()) {

                announce(MessageGrabber.grab(START_CANCELLED), Map.of(), List.of());
                BroadcastUtils.playSound(getBukkitPlayers(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);

                counting = false;

                restartCurrentCountdown();
                updateScoreboard();

                this.cancel();

            }

            else if (durations.lobbyCountdown > 0 && !players.stream().allMatch(GamePlayer::isReady)) {
                if (durations.lobbyCountdown <= 5 || durations.lobbyCountdown == getNextEventTimeMaxSeconds()) {
                    announce(MessageGrabber.grab(LOBBY_COUNTDOWN), Map.of("count", durations.lobbyCountdown), List.of());
                    BroadcastUtils.playSound(getBukkitPlayers(), Sound.BLOCK_NOTE_BLOCK_HARP, 0.8f, 1.0f);
                }
                durations.lobbyCountdown -= 1;
            }

            else {
                if (players.stream().allMatch(GamePlayer::isReady)) {
                    // announce what happened
                    announce(MessageGrabber.grab(LOBBY_ALL_READY), Map.of(), List.of());
                }

                this.cancel();
                counting = false;
                durations.lobbyCountdown = -1;
                prep();
            }

        }}.runTaskTimer(SpeedSG.plugin, 0, 20).getTaskId();

        taskIDs.add(id);
    }

    private void tryGameCountdown() {
        /*
        Game countdown:
        - gs == STARTING
        - Players have been teleported to their respective spawn points
        - Teams are final and unswitchable (a new joining player *may* still be able to join a team but not by their choosing)
        - Leaving may cancel the countdown if the game is left with an insufficient amount of players
        - Leaving will count as an elimination
        - If a game countdown finalizes (w/out breaking), the game will start
        */

        int id = new BukkitRunnable() {public void run() {

            updateScoreboard();

            if (frozen) {
                return;
            }

            if (!isPlayable()) {
                announce(
                        MessageGrabber.grab(START_CANCELLED),
                        Map.of(),
                        List.of()
                );

                cancelStart();
                this.cancel();
            }


            else if (durations.gameStartCountdown > 0) {
                // Not really in the mood to expose this to the config. We will keep this countdown as a hard coded title.
                String color = switch (durations.gameStartCountdown) {
                    case 3 -> "&e&l";
                    case 2 -> "&6&l";
                    case 1 -> "&c&l";
                    default -> "&a";
                };

                List<Player> gamePlayers = players.stream().map(g -> Bukkit.getPlayer(g.getUUID())).toList();
                BroadcastUtils.sendTitle(
                        gamePlayers,
                        color + durations.gameStartCountdown,
                        "",
                        0,
                        20,
                        40
                );
                for (Player p : gamePlayers) {
                    p.playSound(p, Sound.UI_BUTTON_CLICK, 0.7F, 1F);
                }
                durations.gameStartCountdown -= 1;
            }

            else { // count == 0
                List<Player> gamePlayers = players.stream().map(g -> Bukkit.getPlayer(g.getUUID())).toList();

                BroadcastUtils.sendTitle(
                        gamePlayers,
                        "&a&lGO",
                        "",
                        0,
                        20,
                        40
                );

                for (Player p : gamePlayers) {
                    p.playSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7F, 1F);
                }
                durations.gameStartCountdown = -1;

                this.cancel();
                start();
            }

        }}.runTaskTimer(SpeedSG.plugin, 0, 20).getTaskId();

        taskIDs.add(id);
    }


    private void prep() {
        /*
        Prepare the game for a start
           - cleaning and locking down teams
           - spawning teams/players in their respective spawnpoints
           - fill chests
           - updating game state
           - start the countdown
        */

        originalPlayersSize = this.getPlayers().size();
        originalTeamsSize = this.getTeams().size();

        setBarriers();

        for (GamePlayer gp : players) {
            assignTeam(gp);
        }

        teams.removeIf(GameTeam::isEmpty);
        recalcTeamIndexes();


        spawnTeams();

        gameState = GameState.STARTING;

        chests = getChestLocations();
        fillChests(true);

        removeGroundItems();

        if (map.getWorldBorderSettings().isBorderEnabled()) {
            map.getWorld().getWorldBorder().setCenter(map.getBounds().getCenterX() + 0.5, map.getBounds().getCenterZ() + 0.5);
            map.getWorld().getWorldBorder().setSize(Math.max(map.getBounds().getWidthX() + 0.5, map.getBounds().getWidthZ()) + 0.5);
        }
        else {
            lobbyMap.getWorld().getWorldBorder().setSize(1_000_000);
        }


        tryGameCountdown();
    }

    private void recalcTeamIndexes() {
        for (int i = 0; i < teams.size(); i++) {
            GameTeam team = teams.get(i);
            team.setIndex(i);
        }
    }

    private void start() {

        gameState = GameState.ACTIVE;

        unsetBarriers();

        // Send info blurb on game start
        announce(MessageGrabber.grab(INFO_BLURB), Map.of(), List.of());

        // Invulnerability timer
        int initialCount = durations.invulCountdown;

        invulEnabled = true;

        for (GamePlayer gp : this.getPlayers()) {
            gp.setTemporaryStat("kills", 0);
            gp.setLives(maxLives);

            GameStat sgLifetimeKills = GameStat.getStat(gp.getUUID(), "sg-lifetime-kills");
            GameStat sgLifetimeWins = GameStat.getStat(gp.getUUID(), "sg-lifetime-wins");

            if (sgLifetimeKills == null)
                GameStat.setStat(gp.getUUID(), "sg-lifetime-kills", "0").sync();

            if (sgLifetimeWins == null)
                GameStat.setStat(gp.getUUID(), "sg-lifetime-wins", "0").sync();
        }

        // Anything in this task runs every second
        taskIDs.add(
                new BukkitRunnable() {public void run() {
                    if (!frozen) {
                        elapsedSeconds += 1;
                    }

                    updateScoreboard();
                }}.runTaskTimer(plugin, 20, 20).getTaskId()
        );

        taskIDs.add(
            new BukkitRunnable() {public void run() {
                if (frozen) {
                    return;
                }

//                if (durations.invulCountdown == initialCount) {
//                    announce(MessageGrabber.grab(INVUL_COUNTDOWN_MESSAGE), Map.of("count", durations.invulCountdown), List.of());
//                }

                else if ((durations.invulCountdown > 0 && durations.invulCountdown <= 5)) {
                    announce(MessageGrabber.grab(INVUL_COUNTDOWN_MESSAGE), Map.of("count", durations.invulCountdown), List.of());
                    BroadcastUtils.playSound(getAlivePlayers().stream().map(GamePlayer::getBukkitPlayer).toList(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.7f, 0.7f);
                }
                durations.invulCountdown -= 1;

                if (durations.invulCountdown < 0) {
                    announce(MessageGrabber.grab(INVUL_OVER_MESSAGE), Map.of(), List.of());
                    BroadcastUtils.playSound(getAlivePlayers().stream().map(GamePlayer::getBukkitPlayer).toList(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.3f, 0.5f);

                    invulEnabled = false;
                    durations.invulCountdown = -1;
                    this.cancel();
                }
            }}.runTaskTimer(SpeedSG.plugin, 0, 20).getTaskId()
        );

        // Chest refill timer
        taskIDs.add(
            new BukkitRunnable() {public void run() {

                if (frozen) {
                    return;
                }

                if (durations.chestRefillCountdown > 0) {
                    durations.chestRefillCountdown -= 1;
                }

                else {
                    fillChests(false);
                    announce(MessageGrabber.grab(CHEST_REFILLED_MESSAGE), Map.of(), List.of());
                    BroadcastUtils.playSound(getBukkitPlayers(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);

                    durations.chestRefillCountdown = -1;

                    this.cancel();
                }
            }}.runTaskTimer(SpeedSG.plugin, 0, 20).getTaskId()
        );

        // Showdown timer
        taskIDs.add(
            new BukkitRunnable() {public void run() {
                if (frozen) {
                    return;
                }

                if (durations.showdownCountdown > 0)
                    durations.showdownCountdown -= 1;

                else {
                    if (!map.getWorldBorderSettings().isBorderShrink() || !map.getWorldBorderSettings().isBorderEnabled())
                        return;

                    announce(MessageGrabber.grab(SHOWDOWN_MESSAGE), Map.of(), List.of());

                    BroadcastUtils.sendTitle(
                            getBukkitPlayers(),
                            f(MessageGrabber.grab(SHOWDOWN_TITLE)),
                            f(MessageGrabber.grab(SHOWDOWN_SUBTITLE)),
                            0,
                            20,
                            40
                    );

                    BroadcastUtils.playSound(getBukkitPlayers(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0F, 1.0F);

                    durations.showdownCountdown = -1;

                    showdown();
                    this.cancel();
                }

            }}.runTaskTimer(SpeedSG.plugin, 0, 20).getTaskId()
        );

        // Game end timer
        taskIDs.add(
            new BukkitRunnable() {public void run() {
                if (frozen) {
                    return;
                }

                if (durations.gameEndCountdown > 0) {
                    durations.gameEndCountdown -= 1;
                }

                else {
                    durations.gameEndCountdown = 0;
                    if (gameState != GameState.ENDING) {
                        forceWin();
                    }
                    this.cancel();
                }

            }}.runTaskTimer(SpeedSG.plugin, 0, 20).getTaskId()
        );
    }

    private void showdown() {
        double width = map.getWorldBorderSettings().getFinalWidth();
        long seconds = map.getWorldBorderSettings().getShrinkTimeSeconds();

        map.getWorld().getWorldBorder().changeSize(width, seconds * 20L);

        for (GamePlayer gp : getAlivePlayers()) {
            if (gp.getLives() > 1) {
                gp.setLives(1);
            }
        }
    }

    private GameTeam determinePrematureWinner() {
        // This function relies on (takes for granted) the following conditions:

        assert teams != null;
        assert !teams.isEmpty();
        assert teams.size() >= 2;

        // Determine winner prematurely by considering:
        // 1. Total players alive (in team)
        // 2. Total health (in team combined)
        // 3. Total kills
        // 4. Total damage

        try {
            return teams.stream().max(Comparator
                    .comparingDouble((GameTeam t) -> t.getAliveMembers().size())
                    .thenComparingDouble(GameTeam::getCombinedHealth)
                    .thenComparingDouble((GameTeam t) -> t.getStat("kills", 0))
                    .thenComparingDouble((GameTeam t) -> t.getStat("damage-dealt", 0))
            ).get();
        }

        // If comparison fails (due to same values), return a random team
        catch (NoSuchElementException ex) {
            return teams.get(random.nextInt(0, teams.size() - 1));
        }
    }

    public void forceWin() {
        /*
         1. Choose a team based on total damage dealt (store this as a team stat)
         2. Quietly eliminate all other teams (set team to not alive and set all players into spectator mode)
         3. Teleport all spectators to a member of the winning team
         */
        
        GameTeam winner = determinePrematureWinner();
        
        for (GameTeam gt : teams) {
            if (Objects.equals(gt, winner))
                continue;
            
            for (GamePlayer gp : gt.getPlayers()) {
                resetLastDamageSource(gp);
                eliminate(gp);
            }
        }
    }
      
    public void resetLastDamageSource(GamePlayer gp) {
        this.playerLastDamageSourceMap.remove(gp);
    }


    public void setLastDamageSource(GamePlayer player, DamageSource source) {
        // The playerLastDamageSourceMap map is structured Map<GamePlayer, DamageSource>, where DamageSource is (@Nullable GamePlayer attacker, DamageSource.Reason reason).
        // Entries are to expire within 10 seconds (200 ticks). If a player dies within 10 seconds of being attacked by another player, the kill is attributed to the attacker, and not suicide.

        // DamageSource can contain a player if the type is MELEE, PROJECTILE, or EXPLOSIVE. The provided player is allowed to be themselves.


        this.playerLastDamageSourceMap.put(player, source);

        long expireTicks = 10 * 20L;
        new BukkitRunnable() {public void run() {
            playerLastDamageSourceMap.remove(player);
        }}.runTaskLater(plugin, expireTicks);
    }

    public void eliminate(@NotNull GamePlayer player) {

        // (dev) create messages in yml
        // 1. mark player as not alive (and update any other relevant attributes)
        // - if not solos: announce team death
        // 3. set player GameMode to spectator
        // 4. send messages/titles to newly dead player
        // 5. send game announcements
        // (6) check win condition and do logic if necessary

        if (!player.isAlive())
            return;

        player.setLives(player.getLives() - 1);

        if (player.getLives() == 0) {
            player.setAlive(false);
        }

        player.getBukkitPlayer().setGameMode(GameMode.SPECTATOR);
        dropInventory(player.getBukkitPlayer());

        GameTeam team = player.getTeam();

        DamageSource lastDamageSource = playerLastDamageSourceMap.get(player);

        if (lastDamageSource == null || lastDamageSource.isAttackerSelf(player)) {
            // A player shouldn't get kill credit if they kill themselves.
            lastDamageSource = new DamageSource(null, DamageSource.Reason.NATURAL);
        }

        GamePlayer attacker = lastDamageSource.attacker();

        Map<String, Object> commonMap = MapFormatters.gamePlayerFormatter(player);
        commonMap.put("final-kill-indicator", player.getLives() == 0 && maxLives > 1 ? MessageGrabber.grab(FINAL_KILL_INDICATOR) : "");

        // If an attacker exists, update the common map and send kill messages to the attacker first.
        if (attacker != null) {
            commonMap.putAll(MapFormatters.cloneFormaterToNewKey(MapFormatters.gamePlayerFormatter(attacker), "player", "attacker"));
        }

        // Announce death to game
        announce(
                MessageGrabber.grab(valueOf("DEATH_ANNOUNCEMENT_" + lastDamageSource.reason().name().toUpperCase())),
                commonMap,
                List.of()
        );

        // Send death message to the player who died
        MessageUtils.sendParsedMessage(
                player.getBukkitPlayer(),
                MessageGrabber.grab(valueOf("DEATH_MESSAGE_" + lastDamageSource.reason().name().toUpperCase())),
                commonMap
        );


        // Display death title (with respawn or no respawn depending on amount of lives.)
        if (player.getLives() >= 1) {

            final int[] respawnSeconds = {SpeedSG.configYML.getInt("game.respawns.respawn-seconds", 5)};

            new BukkitRunnable() {public void run() {

                if (respawnSeconds[0] <= 0) {
                    respawn(player, false);

                    this.cancel();
                    return;
                }

                commonMap.put("count", respawnSeconds[0]);

                BroadcastUtils.sendTitle(
                        List.of(player.getBukkitPlayer()),
                        formatPlaceholders(MessageGrabber.grab(DEATH_RESPAWN_TITLE), commonMap),
                        formatPlaceholders(MessageGrabber.grab(DEATH_RESPAWN_SUBTITLE), commonMap),
                        msgYML.getInt(DEATH_RESPAWN_FADE_IN_TICKS.getPath(), 0),
                        msgYML.getInt(DEATH_RESPAWN_STAY_TICKS.getPath(), 40),
                        msgYML.getInt(DEATH_RESPAWN_FADE_OUT_TICKS.getPath(), 20)
                );

                respawnSeconds[0] -= 1;
            }}.runTaskTimer(plugin, 0, 20);
        }

        else {
            strikeLightning(player.getBukkitPlayer().getLocation());

            BroadcastUtils.sendTitle(
                    List.of(player.getBukkitPlayer()),
                    MessageGrabber.grab(DEATH_NO_RESPAWN_TITLE),
                    MessageGrabber.grab(DEATH_NO_RESPAWN_SUBTITLE),
                    msgYML.getInt(DEATH_NO_RESPAWN_FADE_IN_TICKS.getPath(), 0),
                    msgYML.getInt(DEATH_NO_RESPAWN_STAY_TICKS.getPath(), 40),
                    msgYML.getInt(DEATH_NO_RESPAWN_FADE_OUT_TICKS.getPath(), 20)
            );
        }

        // Announce a team as dead, and notify team members (if applicable and true)
        if (!team.isAlive() && !isSolos()) {
            commonMap.putAll(MapFormatters.teamFormatter(team));

            announce(
                    MessageGrabber.grab(DEATH_ANNOUNCEMENT_TEAM),
                    commonMap,
                    List.of()
            );

            team.sendTeamMessage(
                    MessageGrabber.grab(DEATH_MESSAGE_TEAM),
                    commonMap,
                    List.of()
            );
        }

        if (attacker != null) {

            double kills = attacker.getTemporaryStat("kills", 0);

            attacker.setTemporaryStat("kills", kills + 1);

            GameStat attackerLifetimeKills = GameStat.getStat(attacker.getUUID(), "sg-lifetime-kills");

            GameStat.setStat(attacker.getUUID(), "sg-lifetime-kills", attackerLifetimeKills != null ? String.valueOf(Integer.parseInt(attackerLifetimeKills.getValue()) + 1) : "1")
                        .sync();


            MessageUtils.sendParsedMessage(
                    player.getBukkitPlayer(),
                    MessageGrabber.grab(valueOf("KILL_MESSAGE_" + lastDamageSource.reason().name().toUpperCase())),
                    commonMap
            );


            // If the team is dead, and we are not in solos mode, we will send a message to the killer, notifying they killed a team.
            if (!team.isAlive() && !isSolos()) {
                MessageUtils.sendParsedMessage(
                        player.getBukkitPlayer(),
                        MessageGrabber.grab(KILL_MESSAGE_TEAM),
                        commonMap
                );
            }
        }

        // Run config-defined command actions for 'on-kill' and 'on-death' YAML keys:
        runConfigCommands(configYML.getStringList("game.command-actions.on-kill"), commonMap);
        runConfigCommands(configYML.getStringList("game.command-actions.on-death"), commonMap);


        // Teleport player to center of map on death (after the config commands run).
        player.getBukkitPlayer().teleport(map.getBounds().getCenter().toLocation(map.getWorld()));

        // Why do this after the config commands run?
        // Because I want to allow commands to control kill effects. So, we need to expose the location. We can do this by allowing the player
        // entity to act as the location. (e.g.: /summon lightning {player})

        if (isWon())
            win(getAliveTeams().getFirst());
    }

    public void respawn(@NotNull GamePlayer gp, boolean byAdmin) {
        if (gp.getLives() < 1) {
            gp.setLives(1);
        }
        else {
            return;
        }

        if (gameState != GameState.ACTIVE) {
            return;
        }

        Map<String, Object> commonMap = MapFormatters.gamePlayerFormatter(gp);

        GameTeam team = gp.getTeam() != null ? gp.getTeam() : assignTeam(gp);

        int spawnIndex = (int) Math.ceil((double) team.getIndex() / getNonEmptyTeams().size()) * (map.getSpawns().size() - 1);

        spawnPlayer(gp.getBukkitPlayer(), map.getSpawns().get(spawnIndex));

        BroadcastUtils.sendTitle(
                List.of(gp.getBukkitPlayer()),
                formatPlaceholders(MessageGrabber.grab(RESPAWN_TITLE), commonMap),
                formatPlaceholders(MessageGrabber.grab(RESPAWN_SUBTITLE), commonMap),
                msgYML.getInt(RESPAWN_FADE_IN_TICKS.getPath(), 0),
                msgYML.getInt(RESPAWN_STAY_TICKS.getPath(), 40),
                msgYML.getInt(RESPAWN_FADE_OUT_TICKS.getPath(), 20)
        );
        gp.getBukkitPlayer().playSound(gp.getBukkitPlayer().getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.7f, 2.0f);

        if (byAdmin) {
            announce(MessageGrabber.grab(RESPAWN_BY_ADMIN_MESSAGE), commonMap, List.of());
        }

        else {
            MessageUtils.sendParsedMessage(
                    gp.getBukkitPlayer(),
                    MessageGrabber.grab(RESPAWN_MESSAGE),
                    commonMap
            );
        }
    }

    private static void runConfigCommands(List<String> commandLines, Map<String, Object> commonMap) {
        for (String line : commandLines) {
            String formattedLine = formatPlaceholders(line, commonMap);

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedLine);
        }
    }

    private static void strikeLightning(Location l) {
        l.getWorld().strikeLightning(l);
    }

    private static void dropInventory(Player p) {
        PlayerInventory inv = p.getInventory();

        for (int i = 0; i < inv.getSize(); i++) {
            p.dropItem(i);
        }
    }

    public boolean isWon() {
        return getAliveTeams().size() == 1;
    }

    public void win(GameTeam winningTeam) {

        gameState = GameState.ENDING;
        cancelAllTasks();

        invulEnabled = true;

        // Send victory title for winners
        List<Player> winnerBukkitPlayers = winningTeam.getPlayers().stream().map(GamePlayer::getBukkitPlayer).toList();

        for (Player p : winnerBukkitPlayers) {
            GameStat attackerLifetimeKills = GameStat.getStat(p.getUniqueId(), "sg-lifetime-wins");

            GameStat.setStat(p.getUniqueId(), "sg-lifetime-wins", attackerLifetimeKills != null ? String.valueOf(Integer.parseInt(attackerLifetimeKills.getValue()) + 1) : "1")
                    .sync();
        }

        BroadcastUtils.playSound(winnerBukkitPlayers, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        BroadcastUtils.sendTitle(
                winnerBukkitPlayers,
                MessageGrabber.grab(WIN_TITLE),
                MessageGrabber.grab(WIN_SUBTITLE),
                msgYML.getInt(WIN_FADE_IN_TICKS.getPath(), 0),
                msgYML.getInt(WIN_STAY_TICKS.getPath(), 40),
                msgYML.getInt(WIN_FADE_OUT_TICKS.getPath(), 20)
        );

        // Send game over title for losers
        List<Player> loserBukkitPlayers = players.stream().filter(gp -> !winningTeam.getPlayers().contains(gp)).map(GamePlayer::getBukkitPlayer).toList();
        BroadcastUtils.sendTitle(
                loserBukkitPlayers,
                MessageGrabber.grab(LOSE_TITLE),
                MessageGrabber.grab(LOSE_SUBTITLE),
                msgYML.getInt(LOSE_FADE_IN_TICKS.getPath(), 0),
                msgYML.getInt(LOSE_STAY_TICKS.getPath(), 40),
                msgYML.getInt(LOSE_FADE_OUT_TICKS.getPath(), 20)
        );

        // Send recaps
        if (!isSolos()) {
            sendTeamsRecap(winningTeam);
        }
        else {
            sendSoloRecap(winningTeam);
        }

        Player destination = winningTeam.getPlayers().getFirst().getBukkitPlayer();
        for (Player p : loserBukkitPlayers) {
            p.teleport(destination);
        }

        int rgb = winningTeam.getRGBColor();

        doFireworks(winnerBukkitPlayers, Color.fromRGB(rgb));

        // Covers both solo winner and team winner cases
        for (GamePlayer gp : winningTeam.getPlayers()) {
            Map<String, Object> commonMap = MapFormatters.gamePlayerFormatter(gp);
            runConfigCommands(configYML.getStringList("game.command-actions.on-win"), commonMap);
        }

        new BukkitRunnable(){
            public void run() {
                cancelAllTasks();
                gameState = GameState.RESET;
                Game newGame = transfer();
                gameIDMap.put(newGame.getGameID(), newGame);
            }
        }.runTaskLater(plugin, 7 * 20L);

        updateScoreboard();
    }

    private void doFireworks(List<Player> targets, Color color) {
        int id = new BukkitRunnable() {public void run() {
            for (Player p : targets) {
                Firework fw = p.getWorld().spawn(p.getLocation(), Firework.class);
                FireworkMeta meta = fw.getFireworkMeta();


                meta.addEffect(FireworkEffect.builder().withColor(color).trail(true).with(FireworkEffect.Type.BALL_LARGE).withFlicker().build());
                meta.setPower(1);
                fw.setFireworkMeta(meta);
                new BukkitRunnable() {
                    public void run() {
                        fw.detonate();
                    }
                }.runTaskLater(plugin, 20L);
            }

        }}.runTaskTimer(plugin, 0L, 20L).getTaskId();

        this.taskIDs.add(id);
    }


    private Pair<String, Map<String, Object>> getTopKillersText() {

        players.sort(Comparator.comparingDouble(gp -> gp.getTemporaryStat("kills", 0)));


        com.carrotguy69.cxyz.messages.utils.MapFormatters.NumberedListFormatter topKillsFormatter = MapFormatters.gamePlayerNumberedListFormatter(
                players.reversed(),
                MessageGrabber.grab(TOP_KILLERS_LIST_ENTRY_FORMAT) != null ? MessageGrabber.grab(TOP_KILLERS_LIST_ENTRY_FORMAT) : "{player}",
                MessageGrabber.grab(TOP_KILLERS_LIST_DELIMITER) != null ? MessageGrabber.grab(TOP_KILLERS_LIST_DELIMITER) : "\n{i}.) ",
                msgYML.getInt(TOP_KILLERS_LIST_MAX_ENTRIES.getPath(), 9999),
                1
        );

        // Creating a new class for this result would be too much abstraction, and secondly I am too lazy.
        return Pair.of(topKillsFormatter.generatePage(1), topKillsFormatter.getFormatMap());
    }

    private static Pair<String, Map<String, Object>> getTeamMembersText(GameTeam winnerTeam) {

        com.carrotguy69.cxyz.messages.utils.MapFormatters.ListFormatter playerFormatter = MapFormatters.gamePlayerListFormatter(
                winnerTeam.getPlayers(),
                MessageGrabber.grab(TEAM_LIST_ENTRY_FORMAT) != null ? MessageGrabber.grab(TEAM_LIST_ENTRY_FORMAT) : "{player}",
                MessageGrabber.grab(TEAM_LIST_DELIMITER) != null ? MessageGrabber.grab(TEAM_LIST_DELIMITER) : ",",
                msgYML.getInt(TEAM_LIST_MAX_ENTRIES.getPath(), 9999),
                1
        );

        return Pair.of(playerFormatter.generatePage(1), playerFormatter.getFormatMap());
    }

    private void sendTeamsRecap(GameTeam winningTeam) {

        Map<String, Object> commonMap = (MapFormatters.teamFormatter(winningTeam));
        commonMap = MapFormatters.cloneFormaterToNewKey(commonMap, "team", "winner-team");

        commonMap.put("game-id", gameID);


        String unparsed = MessageGrabber.grab(RECAP_TEAM_WINNER);

        // Fulfill {team-members}
        Pair<String, Map<String, Object>> pair1 = getTeamMembersText(winningTeam);

        String teamMembersText = pair1.getLeft();
        commonMap.putAll(pair1.getRight());

        unparsed = unparsed.replace("{winner-team-members}", teamMembersText);

        // Fulfill {top-killers}
        Pair<String, Map<String, Object>> pair2 = getTopKillersText();

        String topKillersText = pair2.getLeft();
        commonMap.putAll(pair2.getRight());

        unparsed = unparsed.replace("{top-killers}", topKillersText);

        announce(unparsed, commonMap, List.of());
    }

    private void sendSoloRecap(GameTeam winningTeam) {
        Map<String, Object> commonMap = MapFormatters.cloneFormaterToNewKey(MapFormatters.gamePlayerFormatter(winningTeam.getPlayers().getFirst()), "player", "winner-player"); // For solo's we get the first (and the only) player in the team
        commonMap.putAll(MapFormatters.cloneFormaterToNewKey(MapFormatters.teamFormatter(winningTeam), "team", "winner-team"));
        commonMap.put("game-id", gameID);

        String unparsed = MessageGrabber.grab(RECAP_SOLO_WINNER);

        Pair<String, Map<String, Object>> pair = getTopKillersText();

        String topKillersText = pair.getLeft();
        commonMap.putAll(pair.getRight());

        unparsed = unparsed.replace("{top-killers}", topKillersText);

        announce(unparsed, commonMap, List.of());
    }


    public void assignTeam(GamePlayer gp, GameTeam team) {
        if (team.isFull() || (this.getPlayers().size() == 2 * teamCapacity.min().intValue() && !team.getPlayers().isEmpty())) {
            throw new TeamFullException("Team %s is at or above its max capacity (%d/%d)!".formatted(team.getName(), team.getPlayers().size(), team.getCapacity()));
        }

        team.addPlayer(gp);
        gp.setTeam(team); // IMPORTANT: update the GamePlayer object so it knows what team it is a part of
    }

    public GameTeam assignTeam(GamePlayer gp) {
        /*
        Primarily used as a "last resort", when the player does not self-assign.
        Adds a player to a team and returns that GameTeam.

        Assign player to:
        0. A non-full team
        1. the team with the least players
        2. the team with the least matchmaking score
        */

        // This function relies on (takes for granted) the following conditions:
        assert teams != null;
        assert !teams.isEmpty();
        assert teams.size() >= 2;

        if (gp.getTeam() != null) {
            return gp.getTeam();
        }
        
        GameTeam chosenTeam = null;
        for (GameTeam team : teams) {
            if (team.isFull()) {
                continue;
            }    
            
            if (getPlayers().size() == team.getPlayers().size() - 1) {
                continue;
            }
            
            if (chosenTeam == null) {
                chosenTeam = team;
                continue;
            }
            
            if (team.getPlayers().size() < chosenTeam.getPlayers().size()) {
                chosenTeam = team;
                continue;
            }
            
            if (team.matchmakingScore < chosenTeam.matchmakingScore) {
                chosenTeam = team;
                continue;
            }
            
        }
        
        if (chosenTeam == null) {
            throw new TeamFullException(String.format("All teams in game %s are full", gameID));
        }


        chosenTeam.addPlayer(gp);
        gp.setTeam(chosenTeam);

        return chosenTeam;
    }

    private void spawnTeams() {
        // Teleport all teams to their respective spawn point.

        for (int i = 0; i < teams.size(); i++) {
            GameTeam team = teams.get(i);

            int spawnIndex = (int) Math.ceil(((double) i / (double) teams.size()) * map.getSpawns().size());

            for (GamePlayer gp : team.getPlayers()) {
                Player p = gp.getBukkitPlayer();

                spawnPlayer(p, map.getSpawns().get(spawnIndex));
            }
        }

    }

    private void spawnPlayer(Player p, Location l) {
        p.closeInventory();
        p.getInventory().clear();
        p.setFireTicks(0);
        p.setGameMode(defaultGamemode);
        p.setFlying(false);
        Objects.requireNonNull(p.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(20.0); // The "official" (non-depreceated) way to set max health?
        p.setHealth(20.0);
        p.setFoodLevel(20);
        for (PotionEffect effect : p.getActivePotionEffects()) {
            if (effect.getType() != PotionEffectType.NIGHT_VISION) // This is our FakeFullbright hook lol
                p.removePotionEffect(effect.getType());
        }

        p.teleport(l.clone().add(0.5, 1, 0.5));
    }

    private void removeGroundItems() {
        for (Item entity : map.getWorld().getEntitiesByClass(Item.class)) {
            entity.remove();
        }
    }

    private void fillChests(boolean clearExisting) {

        for (Block block : chests) {

            Chest chest = (Chest) block.getState();

            if (clearExisting) {
                chest.getInventory().clear();
            }

            int amt = lootTable.getLootManager().getItemsPerChest().generateRandom(0).intValue();

            for (int i = 0; i < amt; i++) {

                ItemStack stack = lootTable.getLootManager().selectItem().toItemStack();

                if (stack == null) {
                    i -= 1;
                    continue;
                }

                chest.getInventory().setItem(
                        new Random().nextInt(0, chest.getInventory().getSize() - 1),
                        stack
                );
            }
        }
    }

    private List<Block> getChestLocations() {
        // warning: expensive!
        List<Block> results = new ArrayList<>();

        BoundingBox bounds = this.map.getBounds();

        int minChunkX = bounds.getMin().getBlockX() >> 4; // binary shifting (">> 4" is equivalent to "* 16")
        int minChunkZ = bounds.getMin().getBlockZ() >> 4;

        int maxChunkX = bounds.getMax().getBlockX() >> 4;
        int maxChunkZ = bounds.getMax().getBlockZ() >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                Chunk chunk = map.getWorld().getChunkAt(cx, cz);

                for (BlockState tile : chunk.getTileEntities()) {

                    if (tile.getType().equals(Material.CHEST)) {
                        results.add(tile.getBlock());
                    }
                }

            }
        }


        return results;
    }

    private void setBarriers() {

        barrierBlocks.clear();

        Material effectiveBarrierType = Material.BARRIER;

        for (Location loc : map.getSpawns()) {

            Block base = loc.getBlock();

            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y <= 2; y++) {
                    for (int z = -1; z <= 1; z++) {

                        // Do not fill the center block
                        if (x == 0 && z == 0)
                            continue;

                        Block relative = base.getRelative(x, y, z);

                        if (relative.getType() == Material.AIR || relative.getType() == Material.BARRIER) {
                            relative.setType(effectiveBarrierType);
                            barrierBlocks.add(relative);
                        }
                    }
                }
            }

        }
    }



    private void unsetBarriers() {
        for (Block block : barrierBlocks) {
            block.setType(Material.AIR);
        }
    }

    private void createTeams(int n) {
        // Create `n` amount of joinable teams with no initial players (using the config provided naming scheme).

        List<String> teamNames = configYML.getStringList("game.teams.names");
        List<String> shortNames = configYML.getStringList("game.teams.short-names");

        for (int i = 0; i < n; i++) {
            String teamName = teamNames.size() >= i + 1 ? teamNames.get(i) : "&aTeam " + (i + 1) + " ";
            String shortName = shortNames.size() >= i + 1 ? shortNames.get(i) : String.valueOf(i + 1) + " ";

            teams.add(
                    new GameTeam(
                            this,
                            i + 1,
                            teamName,
                            shortName,
                            ColorUtils.getRGB(teamName),
                            new ArrayList<>(),
                            teamCapacity.max().intValue()
                    )
            );
        }
    }

    public boolean isPlayable() {
        // A game is playable unless all players belong to the same team, or unless there are fewer players than twice the minimum team capacity.
        if (players.size() < 2 * teamCapacity.min().intValue()) {
            return false;
        }

        GameTeam firstTeam = null;

        for (GamePlayer gp : players) {
            GameTeam team = gp.getTeam();

            if (team == null) {
                return true;
            }

            if (firstTeam == null) {
                firstTeam = team;
                continue;
            }

            if (!team.getName().equalsIgnoreCase(firstTeam.getName())) {
                return true;
            }
        }

        return false;
    }

    public List<GameTeam> getNonEmptyTeams() {
        // Returns the teams that have players in them

        List<GameTeam> nonEmpty = new ArrayList<>();

        for (GameTeam team : teams) {
            if (!team.getPlayers().isEmpty()) {
                nonEmpty.add(team);
            }
        }

        return nonEmpty;
    }

    public List<GamePlayer> getAlivePlayers() {
        List<GamePlayer> alivePlayers = new ArrayList<>();

        for (GamePlayer gamePlayer : players) {
            if (gamePlayer.isAlive()) {
                alivePlayers.add(gamePlayer);
            }
        }

        return alivePlayers;
    }

    public List<GamePlayer> getDeadPlayers() {
        List<GamePlayer> deadPlayers = new ArrayList<>();

        for (GamePlayer gp : players) {
            if (!gp.isAlive()) {
                deadPlayers.add(gp);
            }
        }

        return deadPlayers;
    }

    public List<GameTeam> getAliveTeams() {
        List<GameTeam> aliveTeams = new ArrayList<>();

        for (GameTeam team : teams) {
            if (team.isAlive()) {
                aliveTeams.add(team);
            }
        }

        return aliveTeams;
    }

    public List<Player> getBukkitPlayers() {
        return players.stream().map(g -> Bukkit.getPlayer(g.getUUID())).toList();
    }

    public List<GamePlayer> getPlayers() {
        return players;
    }

    public List<GameTeam> getTeams() {
        return teams;
    }

    public void cancelAllTasks() {
        for (Integer taskID : taskIDs) {
            Bukkit.getScheduler().cancelTask(taskID);
        }
    }

    public boolean isSolos() {
        return teamCapacity.max().intValue() == 1;
    }

    public String getGameID() {
        return this.gameID;
    }

    public LootTable getLootTable() {
        return this.lootTable;
    }

    public void setLootTable(LootTable table) {
        this.lootTable = table;
    }

    public static Game getByID(String id) {
        return gameIDMap.get(id.toLowerCase());
    }

    public static Game getByPlayer(Player p) {
        for (Map.Entry<String, Game> entry : gameIDMap.entrySet()) {
            if (entry.getValue().getBukkitPlayers().contains(p)) {
                return entry.getValue();
            }
        }

        return null;
    }

    public GamePlayer getPlayer(Player p) {
        GamePlayer gp = null;

        for (GamePlayer player : this.getPlayers()) {
            if (player.getUUID().equals(p.getUniqueId())) {
                gp = player;
            }
        }

        return gp;
    }

    public GamePlayer getPlayerByName(String name) {
        NetworkPlayer np = NetworkPlayer.getPlayerByUsername(name);

        if (np == null) {
            return null;
        }

        for (GamePlayer gp : this.getPlayers()) {
            if (gp.getUUID().equals(np.getUUID())) {
                return gp;
            }
        }

        return null;
    }

    public GameTeam getTeamByName(String name) {
        for (GameTeam team : teams) {
            if (team.getName().strip().equalsIgnoreCase(name) || team.getShortName().strip().equalsIgnoreCase(name)) {
                return team;
            }
        }

        return null;
    }

    public GameMap getGameMap() {
        return this.map;
    }

    public void setGameMap(GameMap map) {
        this.map = map;
    }

    public GameState getGameState() {
        return this.gameState;
    }

    public NumberRange getAmountOfTeams() {
        return amountOfTeams;
    }

    public NumberRange getTeamCapacity() {
        return teamCapacity;
    }

    public void setAmountOfTeams(NumberRange range) {
        if (range.min().intValue() < 2) {
            return;
        }

        if (gameState != GameState.WAITING) {
            return;
        }

        this.amountOfTeams = range;


        for (GameTeam team : teams) {
            for (GamePlayer gp : team.getPlayers()) {
                gp.setTeam(null);
            }
        }

        teams.clear();

        createTeams(amountOfTeams.max().intValue());

        this.announce(MessageGrabber.grab(LOBBY_TEAMS_RESET_ANNOUNCEMENT), Map.of(), List.of());
        BroadcastUtils.playSound(getBukkitPlayers(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.5f, 0.5f);

        restartCurrentCountdown();
        updateScoreboard();
    }

    public void setTeamCapacity(NumberRange range) {
        if (range.min().intValue() < 1) {
            return;
        }

        if (gameState != GameState.WAITING) {
            return;
        }

        this.teamCapacity = range;

        for (GameTeam team : teams) {
            for (GamePlayer gp : team.getPlayers()) {
                gp.setTeam(null);
            }
        }

        teams.clear();

        createTeams(amountOfTeams.max().intValue());

        this.announce(MessageGrabber.grab(LOBBY_TEAMS_RESET_ANNOUNCEMENT), Map.of(), List.of());
        BroadcastUtils.playSound(getBukkitPlayers(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.5f, 0.5f);

        restartCurrentCountdown();
        updateScoreboard();
    }

    public void setMaxLives(int max) {
        this.maxLives = max;

        for (GamePlayer gp : getAlivePlayers()) {
            if (gp.getLives() > max) {
                gp.setLives(max);
            }
        }
    }

    public String getNextEventName() {
        if (durations.lobbyCountdown >= 0) {
            // This signifies the teleport from the lobby to the arena
            return "Game start";
        }

        if (durations.gameStartCountdown >= 0) {
            return "Cage release";
        }

        if (durations.invulCountdown >= 0) {
            return "PvP enable";
        }

        if (durations.chestRefillCountdown >= 0) {
            return "Chest refill";
        }

        if (durations.showdownCountdown >= 0 && isShowdownAllowed()) {
            return "Showdown";
        }

        return "Game end";
    }

    public int getNextEventTimeSeconds() {
        if (durations.lobbyCountdown >= 0) {
            // This signifies the teleport from the lobby to the arena
            return durations.lobbyCountdown;
        }

        if (durations.gameStartCountdown >= 0) {
            return durations.gameStartCountdown;
        }

        if (durations.invulCountdown >= 0) {
            return durations.invulCountdown;
        }

        if (durations.chestRefillCountdown >= 0) {
            return durations.chestRefillCountdown;
        }

        if (durations.showdownCountdown >= 0 && isShowdownAllowed()) {
            return durations.showdownCountdown;
        }

        return durations.gameEndCountdown;
    }

    public int getNextEventTimeMaxSeconds() {

        if (durations.lobbyCountdown >= 0) {
            // This signifies the teleport from the lobby to the arena
            return configYML.getInt("timers.lobby-countdown", 10);
        }

        if (durations.gameStartCountdown >= 0) {
            return configYML.getInt("timers.game-countdown", 10);
        }

        if (durations.invulCountdown >= 0) {
            return configYML.getInt("timers.invul-countdown", 15);
        }

        if (durations.chestRefillCountdown >= 0) {
            return configYML.getInt("timers.chest-refill", 150);
        }

        if (durations.showdownCountdown >= 0 && isShowdownAllowed()) {
            return configYML.getInt("timers.showdown", 300);
        }

        return configYML.getInt("timers.game-end", 360);
    }

    public boolean isShowdownAllowed() {
        return map.getWorldBorderSettings().isBorderEnabled() && map.getWorldBorderSettings().isBorderShrink();
    }

    public void updateScoreboard() {

        if (!scoreboardsEnabled) {
            return;
        }

        Map<String, Object> ogCommonMap = MapFormatters.gameFormatter(this);

        for (GamePlayer gp : players) {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

            List<String> scoreboardLines;

            if (gameState == GameState.ACTIVE || gameState == GameState.STARTING) {
                scoreboardLines = gameScoreboardLines;
            }
            else {
                scoreboardLines = lobbyScoreboardLines;
            }

            if (scoreboardLines.isEmpty()) {
                scoreboardLines.add("Sample text");
            }

            Map<String, Object> commonMap = new HashMap<>(Map.copyOf(ogCommonMap));
            commonMap.putAll(MapFormatters.gamePlayerFormatter(gp));
            commonMap.put("date", TimeUtils.dateOf(unixTimeNow(), CXYZ.timezone));
            commonMap.put("date-short", TimeUtils.dateOfShort(unixTimeNow(), CXYZ.timezone));
            commonMap.put("time", TimeUtils.timeOf(unixTimeNow(), CXYZ.timezone));
            commonMap.put("time-short", TimeUtils.timeOfShort(unixTimeNow(), CXYZ.timezone));


            Objective objective = scoreboard.registerNewObjective("sidebar", Criteria.DUMMY, f(scoreboardLines.getFirst()));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);


            for (int i = 1; i < scoreboardLines.size(); i++) {
                objective.getScore(f(formatPlaceholders(scoreboardLines.get(i), commonMap))).setScore(Math.abs(scoreboardLines.size() - i));
            }

            gp.getBukkitPlayer().setScoreboard(scoreboard);
        }
    }

    public void closeScoreboard() {
        if (!scoreboardsEnabled) {
            return;
        }

        for (Player p : getBukkitPlayers()) {
            Scoreboard scoreboard = p.getScoreboard();
            Objective obj = scoreboard.getObjective("sidebar");
            if (obj != null) {
                obj.unregister();
            }
        }
    }

    public void closeScoreboard(Player p) {
        Scoreboard scoreboard = p.getScoreboard();
        Objective obj = scoreboard.getObjective("sidebar");
        if (obj != null) {
            obj.unregister();
        }
    }

    public void freeze(GamePlayer admin) {
        this.frozen = !this.frozen;

        announce(MessageGrabber.grab(SSGMessageKey.valueOf("GAME_" + (this.frozen ? "FREEZE" : "UNFREEZE") + "_ANNOUNCEMENT")), MapFormatters.gamePlayerFormatter(admin), List.of());
    }

    public void restartCurrentCountdown() {
        String cur = getNextEventName();

        switch (cur) {
            case "Game start":
                durations.lobbyCountdown = configYML.getInt("timers.lobby-countdown", 10);
                break;
            case "Cage release":
                durations.gameStartCountdown = configYML.getInt("timers.game-countdown", 10);
                durations.invulCountdown = configYML.getInt("timers.invul-countdown", 15);
                durations.chestRefillCountdown = configYML.getInt("timers.chest-refill", 150);
                durations.showdownCountdown = configYML.getInt("timers.showdown", 300);
                durations.gameEndCountdown = configYML.getInt("timers.game-end", 360);
                break;
            case "PvP enable":
                durations.invulCountdown = configYML.getInt("timers.invul-countdown", 15);
                durations.chestRefillCountdown = configYML.getInt("timers.chest-refill", 150);
                durations.showdownCountdown = configYML.getInt("timers.showdown", 300);
                durations.gameEndCountdown = configYML.getInt("timers.game-end", 360);
                break;
            case "Chest refill":
                durations.chestRefillCountdown = configYML.getInt("timers.chest-refill", 150);
                durations.showdownCountdown = configYML.getInt("timers.showdown", 300);
                durations.gameEndCountdown = configYML.getInt("timers.game-end", 360);
                break;
            case "Showdown":
                durations.showdownCountdown = configYML.getInt("timers.showdown", 300);
                durations.gameEndCountdown = configYML.getInt("timers.game-end", 360);
                break;
            case "Game end":
                durations.gameEndCountdown = configYML.getInt("timers.game-end", 360);
                break;
            default:
                return;
        }
//
//        this.announce(MessageGrabber.grab(GAME_TIMER_RESET_ANNOUNCEMENT), Map.of("timer", cur), List.of());
//        BroadcastUtils.playSound(getBukkitPlayers(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.5f, 0.5f);

    }

    public Game transfer() {
        map.isInUse = false;

        String newGameID = this.gameID;
        GameMap newMap = nextMap.equals(map) ? gameMaps.stream().filter(m -> !Objects.equals(m, map)).findAny().orElse(map) : nextMap;
        LootTable newLootTable = nextLootTable;
        NumberRange newAmountOfTeams = nextAmountOfTeams;
        NumberRange newTeamCapacity = nextTeamCapacity;
        int newMaxLives = nextMaxLives;

        List<Player> keepPlayers = this.getBukkitPlayers();

        this.delete();
        Game newGame = new Game(newGameID, newMap, newLootTable, newAmountOfTeams, newTeamCapacity, newMaxLives);
        newGame.frozen = this.frozen;

        new BukkitRunnable() {public void run() {
            for (Player p : keepPlayers) {
                newGame.addPlayer(new GamePlayer(p.getUniqueId()));
            }
        }}.runTaskLater(CXYZ.plugin, 2L);



        return newGame;
    }

    public void delete() {
        // Send players to lobby and cancel tasks

        this.cancelAllTasks();

        List<Player> players = this.getBukkitPlayers();

        int nSpawns = lobbyMap.getSpawns().size();
        int nPlayers = players.size();


        // stolen from the initialization script
        if (lobbyMap.getWorldBorderSettings().isBorderEnabled()) {
            lobbyMap.getWorld().getWorldBorder().setCenter(Math.round(lobbyMap.getBounds().getCenterX()), Math.round(lobbyMap.getBounds().getCenterZ()));
            lobbyMap.getWorld().getWorldBorder().setSize(Math.round(Math.max(lobbyMap.getBounds().getWidthX(), lobbyMap.getBounds().getWidthZ())));
        }
        else {
            lobbyMap.getWorld().getWorldBorder().setSize(lobbyMap.getWorld().getWorldBorder().getMaxSize());
        }

        lobbyMap.getWorld().setSpawnLocation(lobbyMap.getSpawns().getFirst());


        for (int i = 0; i < nPlayers; i++) {
            int j = (i < nSpawns) ? i : (i % nSpawns);

            spawnPlayer(players.get(i), lobbyMap.getSpawns().get(j));
        }

        closeScoreboard();

        SpeedSG.gameIDMap.remove(this.gameID, this);
    }

    public static NumberRange parseTeamCapacity(String input) {
        NumberRange teamCapacity = new NumberRange(1, 1);

        switch (input.toUpperCase()) {
            case "SOLO":
            case "SOLOS":
                break;

            case "DUOS":
            case "DUO":
                teamCapacity = new NumberRange(1, 2);
                break;

            case "TRIOS":
            case "TRIO":
                teamCapacity = new NumberRange(1, 3);
                break;

            case "SQUADS":
            case "SQUAD":
                teamCapacity = new NumberRange(1, 4);
                break;

            default:
                // If the user is stubborn and wants to define a specific minimum and maximum team size, they indicate that by using the hyphen. This can convert easily to a NumberRange.
                if (input.contains("-")) {
                    teamCapacity = NumberRange.fromString(input);
                }
                // Usually the user is not stubborn, and they might put a single number for the maxTeamCapacity. We can fulfill that while keeping minTeamCapacity = 0;
                else {
                    teamCapacity = new NumberRange(0, Integer.valueOf(input));
                }

        }

        return teamCapacity;
    }

    @Override
    public String toString() {
        // todo: clean up the toString so it doesn't look so ass when im trying to debug
        return "Game{"
                + "gameID=" + gameID + ","
                + "teams=" + teams + ","
                + "players=" + players + ","
                + "taskIDs=" + taskIDs + ","
                + "teamCapacity=" + teamCapacity + ","
                + "lootTable=" + lootTable + ","
                + "gameState=" + gameState + ","
                + "defaultGamemode=" + defaultGamemode.name() +
                "}";
    }

}
