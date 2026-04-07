package com.carrotguy69.ssg.game;

import com.carrotguy69.cxyz.messages.MessageUtils;
import com.carrotguy69.cxyz.models.db.NetworkPlayer;
import com.carrotguy69.ssg.SpeedSG;
import com.carrotguy69.ssg.exceptions.TeamFullException;
import com.carrotguy69.ssg.game.loot.LootTable;
import com.carrotguy69.ssg.messages.MessageGrabber;
import com.carrotguy69.ssg.messages.MessageKey;
import com.carrotguy69.ssg.messages.utils.MapFormatters;
import com.carrotguy69.ssg.game.map.GameMap;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Game {

    private final GameMap map;
    public Map<String, Object> gameSettings = new HashMap<>();

    private final int gameID;
    private GameState gameState;

    private final List<GameTeam> teams;
    private final List<GamePlayer> players;
    private final List<Integer> taskIDs;

    public int minTeams;
    public int maxTeams;
    public int minTeamCapacity;
    public int maxTeamCapacity;

    public boolean counting = false;

    // Game specific variables
    private boolean invulnerability = false;
    private final List<Block> barrierBlocks = new ArrayList<>();
    private List<Block> chests = new ArrayList<>();
    private LootTable lootTable;

    public Game(int gameID, GameMap map, LootTable lootTable, int minTeams, int maxTeams, int minTeamCapacity, int maxTeamCapacity) {
        // todo:
        //  - think of how to allow voting for a map (this means the map must not be determined when creating this class)
        //  - implement loot table into the class constructor ✅

        this.gameID = gameID;
        this.map = map;

        this.teams = new ArrayList<>();
        this.players = new ArrayList<>();
        this.taskIDs = new ArrayList<>();

        this.minTeams = minTeams;
        this.maxTeams = maxTeams;

        this.minTeamCapacity = minTeamCapacity;
        this.maxTeamCapacity = maxTeamCapacity;

        this.lootTable = lootTable;

        createTeams(maxTeams);

        this.gameState = GameState.WAITING;

        try {
            map.paste();
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        tryGameCountdown();
    }

    public void announce(String unparsedContent, Map<String, Object> formatMap, List<GamePlayer> excludingPlayers) {
        TextComponent component = MessageUtils.createMessage(unparsedContent, formatMap);

        for (GamePlayer gp : this.players) {
            if (gp.getNetworkPlayer().getPlayer() == null) {
                continue;
            }

            if (excludingPlayers.contains(gp))
                continue;

            Player p = gp.getNetworkPlayer().getPlayer();

            p.sendMessage(component);
        }
    }

    public void addPlayer(GamePlayer gp) {
        players.add(gp);
        Player p = gp.getNetworkPlayer().getPlayer();

        if (gameState == GameState.WAITING) {
            p.teleport(map.getLobbySpawnPoint());

            this.announce(
                    MessageGrabber.grab(MessageKey.LOBBY_JOIN),
                    MapFormatters.gamePlayerFormatter(gp),
                    List.of()
            );

            if (isPlayable()) {
                tryLobbyCountdown();
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

        final int[] count = {10};

        int id = new BukkitRunnable() {public void run() {
            counting = true;

            if (!isPlayable()) {
                announce(MessageGrabber.grab(MessageKey.LOBBY_START_CANCELLED), Map.of(), List.of());

                this.cancel();
                counting = false;
                // Cancel the countdown and do nothing; wait for another player join.
            }


            else if (count[0] > 0) {
                announce(MessageGrabber.grab(MessageKey.LOBBY_COUNTDOWN), Map.of("count", count[0]), List.of());
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

    private void tryGameCountdown() {
        /*
        Game countdown:
        - gs == STARTING
        - Players have been teleported to their respective spawn points
        - Teams are final and unswitchable (a new joining player *may* still be able to join a team but not by their choice)
        - Leaving may cancel the countdown if it leaves an insufficient amount of players
        - Leaving may count as an elimination since this state is during the game.
        - If a game countdown finalizes (w/out breaking), the game will start.
        */

        final int[] count = {10};

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

                List<Player> gamePlayers = players.stream().map(GamePlayer::getNetworkPlayer).map(NetworkPlayer::getPlayer).toList();
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
                List<Player> gamePlayers = players.stream().map(GamePlayer::getNetworkPlayer).map(NetworkPlayer::getPlayer).toList();

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

        boolean useWorldBorder = true; // todo expose flag to Game class and to config

        if (useWorldBorder) {
            map.getWorld().getWorldBorder().setCenter(map.getBounds().getCenterX(), map.getBounds().getCenterZ());
            map.getWorld().getWorldBorder().setSize(Math.max(map.getBounds().getWidthX(), map.getBounds().getWidthZ()));
        }

        tryGameCountdown();
    }

    public void start() {
        unsetBarriers();

        // Send info blurb
        announce(MessageGrabber.grab(MessageKey.GAME_INFO_BLURB), Map.of(), List.of());

        // todo: game timers (invulnerability period, chest refill, game end)
    }

    public void end() {

    }




    public GameTeam assignTeam(GamePlayer gp, @Nullable GameTeam team) {
        if (team == null) {
            return assignTeam(gp);
        }

        if (!team.isFull()) {
            throw new TeamFullException("%s is at or above its max capacity (%d/%d)!".formatted(team.getPrefix(), team.getPlayers().size(), team.getCapacity()));
        }

        team.addPlayer(gp);

        return team;
    }

    public GameTeam assignTeam(GamePlayer gp) {
        /*
        Primarily used as a "last resort", when the player does not self-assign.

        Assign player to:
        1. the team with the least players
        2. the team with the least matchmaking score
        */

        assert !teams.isEmpty();

        GameTeam leastPlayersTeam = teams.getFirst();
        GameTeam leastScoreTeam = teams.getFirst();

        boolean balancedPlayerAmount = true;
        boolean balancedScore = true;

        for (GameTeam team : teams) {
            if (team.isFull())
                continue;

            if (team.getPlayers().size() < leastPlayersTeam.getPlayers().size()) {
                leastPlayersTeam = team;
                balancedPlayerAmount = false;
            }

            if (team.matchmakingScore < leastPlayersTeam.matchmakingScore) {
                leastScoreTeam = team;
                balancedScore = false;
            }
        }

        if (!balancedPlayerAmount) {
            leastPlayersTeam.addPlayer(gp);
            return leastPlayersTeam;
        }

        if (!balancedScore) {
            leastScoreTeam.addPlayer(gp);
            return leastScoreTeam;
        }

        // At this point we've ensured the team player counts and matchmaking scores are balanced. So our default behavior will be returning the first available team that is not full.

        for (GameTeam team : teams) {
            if (team.isFull())
                continue;

            return team;
        }

        throw new TeamFullException(String.format("All teams in game #%d are full!", gameID));
    }

    private void spawnTeams() {
        // Teleport all members of a team to the team spawning point (for all teams).

        for (int i = 0; i < teams.size(); i++) {
            GameTeam team = teams.get(i);
            int spawnIndex = (int) Math.floor((double) i / teams.size()) * map.getSpawns().size();

            for (GamePlayer gp : team.getPlayers()) {
                Player p = gp.getNetworkPlayer().getPlayer();

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

            int max = lootTable.getItemsPerChestRange().generateRandom(0).intValue();

            for (int i = 0; i < max; i++) {
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

        int minChunkX = bounds.getMin().getBlockX() >> 4; // binary shifting (equivalent to multiply by 16)
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
                    new GameTeam("&aTeam " + i, "&a", new ArrayList<>(), maxTeamCapacity)
            );
        }
    }

    private boolean isPlayable() {
        // Checks if the game can be started by ensuring:
        // 1. There are at least 2 non-empty teams
        // 2. Every non-empty team has a player count >= minTeamCapacity

        boolean startable = getNonEmptyTeams().size() >= 2;

        for (GameTeam team : getNonEmptyTeams()) {
            if (team.getPlayers().size() < minTeamCapacity) {
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
}
