package com.carrotguy69.ssg.game;

import com.carrotguy69.cxyz.messages.MessageUtils;
import com.carrotguy69.ssg.SpeedSG;
import com.carrotguy69.ssg.exceptions.TeamFullException;
import com.carrotguy69.ssg.game.loot.LootTable;
import com.carrotguy69.ssg.messages.MessageGrabber;
import com.carrotguy69.ssg.messages.utils.MapFormatters;
import com.carrotguy69.ssg.game.map.GameMap;
import com.carrotguy69.ssg.utils.objects.NumberRange;
import net.md_5.bungee.api.chat.TextComponent;

import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.WorldBorder;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;

import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static com.carrotguy69.ssg.SpeedSG.*;
import static com.carrotguy69.ssg.messages.MessageKey.*;

public class Game {

    private final GameMap map;
    public Map<String, Object> gameSettings = new HashMap<>();

    private final int gameID;
    private GameState gameState;

    private final List<GameTeam> teams;
    private final List<GamePlayer> players;
    private final List<Integer> taskIDs;

    public NumberRange amountOfTeams;
    public NumberRange teamCapacity;

    public Durations durations;

    public boolean counting = false;

    private final int maxLives;

    // Game specific variables
    private boolean invulnerability = true;
    private final List<Block> barrierBlocks = new ArrayList<>();
    private List<Block> chests = new ArrayList<>();
    private LootTable lootTable;

    private final Map<GamePlayer, DamageSource> playerLastDamageSourceMap = new HashMap<>();

    public Game(int gameID, GameMap map, LootTable lootTable, NumberRange amountOfTeams, NumberRange teamCapacity, int maxLives, Durations durations) {
        // todo:
        //  - map voting will NOT be supported in this version. if we want to implement it later we need to refactor Game and centralize a lobby spawn.
        //  - replace minTeams, maxTeams, minTeamCapacity, maxTeamCapacity with number ranges. ✅
        //  - implement lives - so you are able to die and respawn based of the game setings
        this.gameID = gameID;
        this.map = map;

        this.teams = new ArrayList<>();
        this.players = new ArrayList<>();
        this.taskIDs = new ArrayList<>();

        this.amountOfTeams = amountOfTeams;

        this.teamCapacity = teamCapacity;

        this.lootTable = lootTable;

        this.durations = durations;

        this.maxLives = maxLives;

        createTeams(teamCapacity.max().intValue());

        this.gameState = GameState.WAITING;

        try {
            map.paste();
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        tryLobbyCountdown(durations.lobbyCountdown);
    }

    public void announce(String unparsedContent, Map<String, Object> formatMap, List<GamePlayer> excludingPlayers) {
        TextComponent component = MessageUtils.createMessage(unparsedContent, formatMap);

        for (GamePlayer gp : this.players) {
            if (gp.getBukkitPlayer() == null) {
                continue;
            }

            if (excludingPlayers.contains(gp))
                continue;

            Player p = gp.getBukkitPlayer();

            p.sendMessage(component);
        }
    }

    public void addPlayer(GamePlayer gp) {
        players.add(gp);
        Player p = gp.getBukkitPlayer();

        if (gameState == GameState.WAITING) {
            p.teleport(map.getLobbySpawnPoint());

            this.announce(
                    MessageGrabber.grab(LOBBY_JOIN),
                    MapFormatters.gamePlayerFormatter(gp),
                    List.of()
            );

            if (isPlayable()) {
                tryLobbyCountdown(durations.lobbyCountdown);
            }
        }

        if (gameState == GameState.STARTING) {
            boolean allowJoinDuringStart = false;
            // todo: ^ expose this boolean to config

            if (allowJoinDuringStart) {

            }


        }
    }

    public void removePlayer(GamePlayer np) {

    }

    private void tryLobbyCountdown(int seconds) {
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

        final int[] count = {seconds};

        int id = new BukkitRunnable() {public void run() {
            counting = true;

            if (!isPlayable()) {
                announce(MessageGrabber.grab(LOBBY_START_CANCELLED), Map.of(), List.of());

                this.cancel();
                counting = false;
                // Cancel the countdown and do nothing; wait for another player join.
            }


            else if (count[0] > 0) {
                announce(MessageGrabber.grab(LOBBY_COUNTDOWN), Map.of("count", count[0]), List.of());
                count[0] -= 1;
            }

            else { // count == 0
                this.cancel();
                counting = false;
                prep();
            }

        }}.runTaskTimer(SpeedSG.plugin, 0, 20).getTaskId();

        taskIDs.add(id);
    }

    private void tryGameCountdown(int seconds) {
        /*
        Game countdown:
        - gs == STARTING
        - Players have been teleported to their respective spawn points
        - Teams are final and unswitchable (a new joining player *may* still be able to join a team but not by their choosing)
        - Leaving may cancel the countdown if the game is left with an insufficient amount of players
        - Leaving will count as an elimination
        - If a game countdown finalizes (w/out breaking), the game will start
        */

        final int[] count = {seconds};

        int id = new BukkitRunnable() {public void run() {

            if (!isPlayable()) {
                // todo: handle quit during start (grant win to the last standing team)
                this.cancel();
            }


            else if (count[0] > 0) {
                // Not really in the mood to expose this to the config. We will keep this countdown as a hard coded title.
                String color = switch (count[0]) {
                    case 3 -> "&e&l";
                    case 2 -> "&6&l";
                    case 1 -> "&c&l";
                    default -> "&a";
                };

                List<Player> gamePlayers = players.stream().map(g -> Bukkit.getPlayer(g.getUUID())).toList();
                MessageUtils.sendTitle(
                        gamePlayers,
                        color + count[0],
                        "",
                        0,
                        20,
                        40
                );
                for (Player p : gamePlayers) {
                    p.playSound(p, Sound.UI_BUTTON_CLICK, 0.7F, 1F);
                }
                count[0] -= 1;
            }

            else { // count == 0
                List<Player> gamePlayers = players.stream().map(g -> Bukkit.getPlayer(g.getUUID())).toList();

                MessageUtils.sendTitle(
                        gamePlayers,
                        "&a&lGO!",
                        "",
                        0,
                        20,
                        40
                );

                for (Player p : gamePlayers) {
                    p.playSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7F, 1F);
                }

                this.cancel();
                start();
            }

        }}.runTaskTimer(SpeedSG.plugin, 0, 20).getTaskId();

        taskIDs.add(id);
    }


    public void prep() {
        /*
        Prepare the game for a start
           - cleaning and locking down teams
           - spawning teams/players in their respective spawnpoints
           - fill chests
           - updating game state
           - start the countdown
        */


        teams.removeIf(GameTeam::isEmpty);

        setBarriers();

        spawnTeams();

        gameState = GameState.STARTING;

        chests = getChestLocations();
        fillChests(true);

        if (map.getWorldBorderSettings().isBorderEnabled()) {
            map.getWorld().getWorldBorder().setCenter(map.getBounds().getCenterX(), map.getBounds().getCenterZ());
            map.getWorld().getWorldBorder().setSize(Math.max(map.getBounds().getWidthX(), map.getBounds().getWidthZ()));
        }

        tryGameCountdown(durations.gameStartCountdown);
    }

    public void start() {
        gameState = GameState.ACTIVE;

        unsetBarriers();

        // Send info blurb on game start
        announce(MessageGrabber.grab(INFO_MESSAGE), Map.of(), List.of());

        // Invulnerability timer
        int originalInvulCountdown = durations.invulCountdown;

        taskIDs.add(
            new BukkitRunnable() {public void run() {
                if ((durations.invulCountdown > 0 && durations.invulCountdown <= 5) || durations.invulCountdown == originalInvulCountdown) {
                    announce(MessageGrabber.grab(INVUL_COUNTDOWN_MESSAGE), Map.of("count", durations.invulCountdown), List.of());
                    durations.invulCountdown -= 1;
                }

                else { // count == 0
                    invulnerability = false;
                    this.cancel();
                }
            }}.runTaskTimer(SpeedSG.plugin, 0, 20).getTaskId()
        );

        // Chest refill timer
        taskIDs.add(
            new BukkitRunnable() {public void run() {
                if (durations.chestRefillCountdown > 0) {
                    durations.chestRefillCountdown -= 1;
                }

                else {
                    fillChests(false);
                    announce(MessageGrabber.grab(CHEST_REFILLED_MESSAGE), Map.of(), List.of());
                    MessageUtils.playSound(getBukkitPlayers(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);

                    this.cancel();
                }
            }}.runTaskTimer(SpeedSG.plugin, 0, 20).getTaskId()
        );

        // Showdown timer
        taskIDs.add(
            new BukkitRunnable() {public void run() {
                if (durations.showdownCountdown > 0) {
                    durations.showdownCountdown -= 1;
                }

                else {
                    announce(MessageGrabber.grab(SHOWDOWN_MESSAGE), Map.of("remaining-seconds", durations.gameEndCountdown), List.of());
                    //                                                                                        ^ Note: using the gameEndCountdown for the showdown message here is correct.
                    //                                                                                          The intended behavior is to pass a remaining-seconds placeholder.

                    MessageUtils.sendTitle(
                            getBukkitPlayers(),
                            f(MessageGrabber.grab(SHOWDOWN_TITLE)),
                            f(MessageGrabber.grab(SHOWDOWN_SUBTITLE)),
                            0,
                            20,
                            40
                    );
                    MessageUtils.playSound(getBukkitPlayers(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0F, 1.0F);
                    showdown();
                    this.cancel();
                }

            }}.runTaskTimer(SpeedSG.plugin, 0, 20).getTaskId()
        );

        // Game end timer
        taskIDs.add(
            new BukkitRunnable() {public void run() {
                if (durations.gameEndCountdown > 0) {
                    durations.gameEndCountdown -= 1;
                }

                else {
                    if (gameState != GameState.ENDING) {
                        forceWin();
                    }
                    this.cancel();
                }

            }}.runTaskTimer(SpeedSG.plugin, 0, 20).getTaskId()
        );
    }

    private void showdown() {
        // todo: expose all these variables to maps config (shrinkBorder, width, seconds)
        double width = 25;
        long seconds = 60;

        if (map.getWorldBorderSettings().isBorderShrink())
            shrinkBorder(width, seconds * 20L);
    }

    private void shrinkBorder(double finalWidth, long ticks) {
        WorldBorder border = map.getWorld().getWorldBorder();

        border.changeSize(finalWidth, ticks);
    }

    private GameTeam determinePrematureWinner() {
        // This function relies on (takes for granted) the following conditions:

        assert teams != null;
        assert !teams.isEmpty();
        assert teams.size() >= 2;

        // Determine winner prematurely by considering:
        // 1. Total kills
        // 2. Total damage

        return teams.stream().max(Comparator
                .comparingDouble((GameTeam t) -> t.getStat("kills", 0))
                .thenComparingDouble((GameTeam t) -> t.getStat("damage-dealt", 0))
        ).get();
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

        // todo: award attacker via exposing config commands
        //  e.g. (yaml):
        //    on-kill:
        //        actions:
        //            - coins add {attacker} 20
        //            - show {attacker} You got 20 coins for killing {player}

        // todo: we can also expose commands for:
        //     - on-kill
        //     - on-win
        //     - ...

        player.setAlive(false);
        player.getBukkitPlayer().setGameMode(GameMode.SPECTATOR);
        dropInventory(player.getBukkitPlayer());

        GameTeam team = player.getTeam();

        DamageSource lastDamageSource = playerLastDamageSourceMap.get(player);
        GamePlayer attacker = lastDamageSource.attacker();

        Map<String, Object> commonMap = MapFormatters.gamePlayerFormatter(player);


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

        // Announce a team as dead, and notify team members (if applicable and true)
        if (!team.isAlive() && this.teamCapacity.max().intValue() > 1) {
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

            MessageUtils.sendParsedMessage(
                    player.getBukkitPlayer(),
                    MessageGrabber.grab(valueOf("KILL_MESSAGE_" + lastDamageSource.reason().name().toUpperCase())),
                    commonMap
            );


            // If the team is dead, and we are not in solos mode, we will send a message to the killer, notifying they killed a team.
            if (!team.isAlive() && this.teamCapacity.max().intValue() > 1) {
                MessageUtils.sendParsedMessage(
                        player.getBukkitPlayer(),
                        MessageGrabber.grab(KILL_MESSAGE_TEAM),
                        commonMap
                );
            }
        }

        if (checkWin())
            win(teams.getFirst());
    }

    private static void dropInventory(Player p) {
        PlayerInventory inv = p.getInventory();

        for (int i = 0; i < inv.getSize(); i++) {
            p.dropItem(i);
        }
    }

    public boolean checkWin() {
        return getAliveTeams().size() == 1;
    }

    public void win(GameTeam winningTeam) {
        cancelAllTasks();

        // Send victory title for winners
        List<Player> winnerBukkitPlayers = winningTeam.getPlayers().stream().map(GamePlayer::getBukkitPlayer).toList();
        MessageUtils.playSound(winnerBukkitPlayers, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        MessageUtils.sendTitle(
                winnerBukkitPlayers,
                MessageGrabber.grab(WIN_TITLE),
                MessageGrabber.grab(WIN_SUBTITLE),
                msgYML.getInt(WIN_FADE_IN_TICKS.getPath(), 0),
                msgYML.getInt(WIN_STAY_TICKS.getPath(), 40),
                msgYML.getInt(WIN_FADE_OUT_TICKS.getPath(), 20)
        );

        // Send game over title for losers
        List<Player> loserBukkitPlayers = players.stream().filter(gp -> !winningTeam.getPlayers().contains(gp)).map(GamePlayer::getBukkitPlayer).toList();
        MessageUtils.sendTitle(
                loserBukkitPlayers,
                MessageGrabber.grab(LOSE_TITLE),
                MessageGrabber.grab(LOSE_SUBTITLE),
                msgYML.getInt(LOSE_FADE_IN_TICKS.getPath(), 0),
                msgYML.getInt(LOSE_STAY_TICKS.getPath(), 40),
                msgYML.getInt(LOSE_FADE_OUT_TICKS.getPath(), 20)
        );

        // Send recaps
        if (teamCapacity.min().intValue() > 1) {
            sendTeamsRecap(winningTeam);
        }
        else {
            sendSoloRecap(winningTeam);
        }

        Player destination = winningTeam.getPlayers().getFirst().getBukkitPlayer();
        for (Player p : loserBukkitPlayers) {
            p.teleport(destination);
        }

        // TODO: derive team color for fireworks
        doFireworks(winnerBukkitPlayers, Color.GREEN);

        // TODO: expose commands for on-win {player}, in teams mode we will loop across the team players.
    }

    private static void doFireworks(List<Player> targets, Color color) {
        // todo: when to cancel?
        new BukkitRunnable() {public void run() {
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

        }}.runTaskTimer(plugin, 0L, 20L);
    }


    private Pair<String, Map<String, Object>> getTopKillersText() {

        players.sort(Comparator.comparingDouble(p -> p.getStat("round-kills", 0)));

        com.carrotguy69.cxyz.messages.utils.MapFormatters.NumberedListFormatter topKillsFormatter = MapFormatters.gamePlayerNumberedListFormatter(
                players,
                MessageGrabber.grab(TOP_KILLERS_LIST_ENTRY_FORMAT) != null ? MessageGrabber.grab(TOP_KILLERS_LIST_ENTRY_FORMAT) : "{player}",
                MessageGrabber.grab(TOP_KILLERS_LIST_DELIMITER) != null ? MessageGrabber.grab(TOP_KILLERS_LIST_DELIMITER) : "\n{i}.) ",
                msgYML.getInt(TOP_KILLERS_LIST_MAX_ENTRIES.getPath(), 9999),
                1
        );

        // Creating a new class would be too much abstraction, and secondly I am too lazy.
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
        Map<String, Object> commonMap = MapFormatters.teamFormatter(winningTeam);
        commonMap.put("game-id", gameID);


        String unparsed = MessageGrabber.grab(RECAP_TEAM_WINNER);

        // Fulfill {team-members}
        Pair<String, Map<String, Object>> pair1 = getTeamMembersText(winningTeam);

        String teamMembersText = pair1.getLeft();
        commonMap.putAll(pair1.getRight());

        unparsed = unparsed.replace("{team-members}", teamMembersText);

        // Fulfill {top-killers}
        Pair<String, Map<String, Object>> pair2 = getTopKillersText();

        String topKillersText = pair2.getLeft();
        commonMap.putAll(pair2.getRight());

        unparsed = unparsed.replace("{top-killers}", topKillersText);

        announce(unparsed, commonMap, List.of());
    }

    private void sendSoloRecap(GameTeam winningTeam) {
        Map<String, Object> commonMap = MapFormatters.gamePlayerFormatter(winningTeam.getPlayers().getFirst());
        commonMap.putAll(MapFormatters.teamFormatter(winningTeam));
        commonMap.put("game-id", gameID);

        String unparsed = MessageGrabber.grab(RECAP_SOLO_WINNER);

        Pair<String, Map<String, Object>> pair = getTopKillersText();

        String topKillersText = pair.getLeft();
        commonMap.putAll(pair.getRight());

        unparsed = unparsed.replace("{top-killers}", topKillersText);

        announce(unparsed, commonMap, List.of());
    }




    public GameTeam assignTeam(GamePlayer gp, @Nullable GameTeam team) {
        if (team == null) {
            return assignTeam(gp);
        }

        if (team.isFull()) {
            throw new TeamFullException("%s is at or above its max capacity (%d/%d)!".formatted(team.getPrefix(), team.getPlayers().size(), team.getCapacity()));
        }

        team.addPlayer(gp);

        return team;
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

        GameTeam chosenTeam = teams.stream()
                .filter(t -> !t.isFull())
                .min(Comparator
                        .comparingInt((GameTeam t) -> t.getPlayers().size())
                        .thenComparingDouble((GameTeam t) -> t.matchmakingScore)
                ).orElseThrow(() -> new TeamFullException(String.format("All teams in game #%d are full!", gameID)));

        chosenTeam.addPlayer(gp);

        return chosenTeam;
    }

    private void spawnTeams() {
        // Teleport all teams to their respective spawn point.

        for (int i = 0; i < teams.size(); i++) {
            GameTeam team = teams.get(i);
            int spawnIndex = (int) Math.floor((double) i / teams.size()) * map.getSpawns().size();

            for (GamePlayer gp : team.getPlayers()) {
                Player p = gp.getBukkitPlayer();

                p.teleport(map.getSpawns().get(spawnIndex));
            }

        }
    }

    private void fillChests(boolean clearExisting) {

        for (Block block : chests) {
            Chest chest = (Chest) block;

            if (clearExisting) {
                chest.getInventory().clear();
            }

            int amt = lootTable.getLootManager().getItemsPerChest().generateRandom(0).intValue();

            for (int i = 0; i < amt; i++) {
                chest.getInventory().setItem(
                        new Random().nextInt(0, chest.getInventory().getSize() - 1),
                        lootTable.getLootManager().selectItem().toItemStack()
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
                    if (tile instanceof Chest chest) {

                        Block block = chest.getLocation().getBlock();

                        if (bounds.contains(block.getX(), block.getY(), block.getZ())) {
                            results.add(block);
                        }
                    }
                }

            }
        }

        return results;
    }

    private void setBarriers() {
        for (Location loc : map.getSpawns()) {
            Block center = loc.add(0, 1, 0).getBlock();

            Block block1 = center.getRelative(BlockFace.NORTH);
            Block block2 = center.getRelative(BlockFace.EAST);
            Block block3 = center.getRelative(BlockFace.SOUTH);
            Block block4 = center.getRelative(BlockFace.WEST);

            Block block5 = center.getRelative(BlockFace.NORTH_EAST);
            Block block6 = center.getRelative(BlockFace.NORTH_WEST);
            Block block7 = center.getRelative(BlockFace.SOUTH_EAST);
            Block block8 = center.getRelative(BlockFace.SOUTH_WEST);

            List<Block> faces = List.of(block1, block2, block3, block4, block5, block6, block7, block8);

            for (Block block : faces) {
                if (block.getType() == Material.AIR) {
                    block.setType(Material.BARRIER);
                    barrierBlocks.add(block);
                }
            }
        }
    }

    private void unsetBarriers() {
        for (Block block : barrierBlocks) {
                block.setType(Material.AIR);
                barrierBlocks.add(block);
        }
    }

    private void createTeams(int n) {
        // Create `n` amount of joinable teams with no initial players.

        for (int i = 1; i <= n; i++) {
            teams.add(
                    new GameTeam("&aTeam " + i, "&a", new ArrayList<>(), teamCapacity.max().intValue())
            );
        }
    }

    private boolean isPlayable() {
        // Checks if the game can be started by ensuring:
        // 1. There are at least 2 non-empty teams
        // 2. Every non-empty team has a player count >= minTeamCapacity

        boolean startable = getNonEmptyTeams().size() >= 2;

        for (GameTeam team : getNonEmptyTeams()) {
            if (team.getPlayers().size() < teamCapacity.min().intValue()) {
                startable = false;
                break;
            }
        }

        return startable;
    }

    private List<GameTeam> getNonEmptyTeams() {
        // Returns the teams that have players in them

        List<GameTeam> nonEmpty = new ArrayList<>();

        for (GameTeam team : teams) {
            if (!team.getPlayers().isEmpty()) {
                nonEmpty.add(team);
            }
        }

        return nonEmpty;
    }

    private List<GamePlayer> getAlivePlayers() {
        List<GamePlayer> alivePlayers = new ArrayList<>();

        for (GamePlayer gamePlayer : players) {
            if (gamePlayer.isAlive()) {
                alivePlayers.add(gamePlayer);
            }
        }

        return alivePlayers;
    }

    private List<GamePlayer> getDeadPlayers() {
        List<GamePlayer> deadPlayers = new ArrayList<>();

        for (GamePlayer gp : players) {
            if (!gp.isAlive()) {
                deadPlayers.add(gp);
            }
        }

        return deadPlayers;
    }

    private List<GameTeam> getAliveTeams() {
        List<GameTeam> aliveTeams = new ArrayList<>();

        for (GameTeam team : teams) {
            if (team.isAlive()) {
                aliveTeams.add(team);
            }
        }

        return aliveTeams;
    }

    private List<Player> getBukkitPlayers() {
        return players.stream().map(g -> Bukkit.getPlayer(g.getUUID())).toList();
    }

    private void cancelAllTasks() {
        for (Integer taskID : taskIDs) {
            Bukkit.getScheduler().cancelTask(taskID);
        }
    }


    public int getGameID() {
        return this.gameID;
    }

    public void setGameSetting(String key, String value) {
        this.gameSettings.put(key, value);
    }

    public void setLootTable(LootTable table) {
        this.lootTable = table;
    }
}
