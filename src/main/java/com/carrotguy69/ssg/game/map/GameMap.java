package com.carrotguy69.ssg.game.map;

import com.carrotguy69.cxyz.exceptions.YAMLFormatException;
import com.carrotguy69.ssg.game.map.sources.MapSource;
import com.carrotguy69.ssg.game.map.sources.SchematicSource;
import com.carrotguy69.ssg.game.map.sources.StaticSource;
import com.carrotguy69.ssg.game.map.sources.WorldCopySource;
import com.carrotguy69.ssg.utils.objects.LocationUtils;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.BoundingBox;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.carrotguy69.ssg.SpeedSG.mapYML;

public class GameMap {

    private final String id;
    private final String name;
    private final MapSource source;
    private final Location lobbySpawn;
    private final List<Location> spawns;
    private final BoundingBox bounds;
    private final World world;

    private GameMap(String id, String name, MapSource source, List<Location> spawns, Location lobbySpawnPoint, BoundingBox bounds, World world) {
        this.id = id;
        this.name = name;
        this.source = source;
        this.spawns = spawns;
        this.lobbySpawn = lobbySpawnPoint;
        this.bounds = bounds;
        this.world = world;
    }

    public String getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public MapSource getSource() {
        return source;
    }

    public List<Location> getSpawns() {
        return spawns;
    }

    public Location getLobbySpawnPoint() {
        return lobbySpawn;
    }

    public BoundingBox getBounds() {
        return bounds;
    }

    public World getWorld() {
        return world;
    }

    public void paste() throws FileNotFoundException {
        if (source.getType() == MapSource.Type.SCHEMATIC) {
            SchematicSource schemSource = (SchematicSource) source;

            pasteWithSchematic(schemSource.getFileName(), schemSource.getPasteLocation());
        }

        else if (source.getType() == MapSource.Type.WORLD_COPY) {
            WorldCopySource wcSource = (WorldCopySource) source;

            pasteWithWorldCopy(wcSource.getWorldName(), wcSource.getCopyBounds(), wcSource.getPasteLocation());
        }

    }

    private void pasteWithSchematic(String fileName, Location pasteLocation) throws FileNotFoundException {
        Clipboard clipboard;

        try {
            ClipboardFormat format = ClipboardFormats.findByAlias(fileName);
            assert format != null;

            ClipboardReader reader = format.getReader(new FileInputStream(fileName));
            clipboard = reader.read();

            try (EditSession editSession = WorldEdit.getInstance().newEditSession((com.sk89q.worldedit.world.World) pasteLocation.getWorld())) {
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BlockVector3.at(pasteLocation.x(), pasteLocation.y(), pasteLocation.z()))
                        .build();

                Operations.complete(operation);
            }
            catch (WorldEditException e) {
                throw new RuntimeException(e);
            }
        }
        catch (AssertionError | IOException ex) {
            throw new FileNotFoundException(String.format("Could not find schematic by id='%s'. Make sure your schematic is in WorldEdit/schematics and matches the ID in maps.yml.", name));
        }
    }

    private void pasteWithWorldCopy(String fromWorldName, BoundingBox copyBounds, Location pasteLocation) throws FileNotFoundException {
        World world = Bukkit.getWorld(fromWorldName);

        if (world == null) {
            throw new FileNotFoundException(String.format("Could not find world by name='%s'.", fromWorldName));
        }

        CuboidRegion region = new CuboidRegion(
                BlockVector3.at(copyBounds.getMinX(), copyBounds.getMinY(), copyBounds.getMinZ()),
                BlockVector3.at(copyBounds.getMaxX(), copyBounds.getMaxY(), copyBounds.getMaxZ())
        );

        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);

        ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
                (com.sk89q.worldedit.world.World) world, region, clipboard, region.getMinimumPoint()
        );

        try (EditSession editSession = WorldEdit.getInstance().newEditSession((com.sk89q.worldedit.world.World) pasteLocation.getWorld())) {
            Operations.complete(forwardExtentCopy); // copies our region to `clipboard`


            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(pasteLocation.x(), pasteLocation.y(), pasteLocation.z()))
                    .build();

            Operations.complete(operation);
        }
        catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<GameMap> loadMaps() {

        List<GameMap> results = new ArrayList<>();

        ConfigurationSection section = mapYML.getConfigurationSection("maps");

        if (section == null) {
            throw new YAMLFormatException("maps.yml", "maps", "Could not find YAML section!");
        }

        for (String mapID : section.getKeys(false)) {
            String displayName = section.getString(mapID + ".display-name", mapID);

            String sourceType = section.getString(mapID + ".source.type", null);
            String sourceFileName = section.getString(mapID + ".source.file", null);
            String sourceWorldName = section.getString(mapID + ".source.world", null);

            String gameWorldName = section.getString(mapID + ".world", null);
            if (gameWorldName == null) {
                throw new YAMLFormatException("maps.yml", "maps." + mapID + ".world", "World not defined!");
            }

            World world = Bukkit.getWorld(gameWorldName);

            if (world == null) {
                throw new RuntimeException(String.format("World \"%s\" could not be found!", gameWorldName));
            }

            // Get copy bounds
            List<Map<?, ?>> copyBoundMapList1 = section.getMapList(mapID + ".source.copy-bounds.pos1");
            Location copyBoundsPos1 = LocationUtils.getLocationFromYML(copyBoundMapList1);

            List<Map<?, ?>> copyBoundMapList2 = section.getMapList(mapID + ".source.copy-bounds.pos2");
            Location copyBoundsPos2 = LocationUtils.getLocationFromYML(copyBoundMapList2);

            BoundingBox copyBounds = null;
            if (copyBoundsPos1 != null && copyBoundsPos2 != null)
                copyBounds = new BoundingBox(copyBoundsPos1.x(), copyBoundsPos1.y(), copyBoundsPos1.z(), copyBoundsPos2.x(), copyBoundsPos2.y(), copyBoundsPos2.z());
            

            // Get paste location
            List<Map<?, ?>> pasteLocationMapList = section.getMapList(mapID + ".source.paste-location");
            Location pasteLocation = LocationUtils.getLocationFromYML(pasteLocationMapList);


            // Get spawns
            List<Map<?, ?>> spawnsMapList = section.getMapList(mapID + ".spawns");
            List<Location> spawns = LocationUtils.getLocationsFromYML(spawnsMapList);


            // Get lobby spawn
            List<Map<?, ?>> lobbySpawnMapList = section.getMapList(mapID + ".lobby-spawn");
            Location lobbySpawnLocation = LocationUtils.getLocationFromYML(lobbySpawnMapList);


            // Get map bounds
            List<Map<?, ?>> boundsMapList1 = section.getMapList(mapID + ".bounds.pos1");
            Location boundsPos1 = LocationUtils.getLocationFromYML(boundsMapList1);

            List<Map<?, ?>> boundsMapList2 = section.getMapList(mapID + ".bounds.pos2");
            Location boundsPos2 = LocationUtils.getLocationFromYML(boundsMapList2);

            // todo: enforce that all destination worlds are equal to the one provided: 'world'

            BoundingBox mapBounds;


            MapSource source;

            switch (sourceType) {
                case "SCHEMATIC":

                    if (sourceFileName == null) {
                        throw new YAMLFormatException("maps.yml", "maps." + mapID + ".source.file", "Source file not defined!");
                    }
                    if (sourceWorldName == null) {
                        throw new YAMLFormatException("maps.yml", "maps." + mapID + ".source.world", "Source world not defined!");
                    }

                    source = new SchematicSource(sourceFileName, pasteLocation);
                    break;

                case "WORLD_COPY":

                    if (sourceWorldName == null) {
                        throw new YAMLFormatException("maps.yml", "maps." + mapID + ".source.world", "Source world not defined!");
                    }

                    if (copyBoundsPos1 == null) {
                        throw new YAMLFormatException("maps.yml", "maps." + mapID + ".source.copy-bounds.pos1", "Position 1 not defined!");
                    }

                    if (copyBoundsPos2 == null) {
                        throw new YAMLFormatException("maps.yml", "maps." + mapID + ".source.copy-bounds.pos2", "Position 2 not defined!");
                    }

                    if (pasteLocation == null) {
                        throw new YAMLFormatException("maps.yml", "maps." + mapID + ".source.copy-bounds.pos2", "Position 2 not defined!");
                    }

                    source = new WorldCopySource(sourceWorldName, copyBounds, pasteLocation);
                    break;

                case "STATIC":
                    source = new StaticSource();
                    break;

                case null, default:
                    throw new RuntimeException("Invalid map source type! Use SCHEMATIC, WORLD_COPY, or STATIC!");
            }

            if (spawns.isEmpty()) {
                throw new YAMLFormatException("maps.yml", "maps." + mapID + ".spawns", "Spawns not defined!");
            }

            if (lobbySpawnLocation == null) {
                throw new YAMLFormatException("maps.yml", "maps." + mapID + ".lobby-spawn", "Lobby spawn not defined!");
            }

            if (boundsPos1 == null) {
                throw new YAMLFormatException("maps.yml", "maps." + mapID + ".bounds.pos1", "Position 1 not defined!");
            }

            if (boundsPos2 == null) {
                throw new YAMLFormatException("maps.yml", "maps." + mapID + ".bounds.pos2", "Position 2 not defined!");
            }

            mapBounds = new BoundingBox(boundsPos1.x(), boundsPos1.y(), boundsPos1.z(), boundsPos2.x(), boundsPos2.y(), boundsPos2.z());


            GameMap map = new GameMap(mapID, displayName, source, spawns, lobbySpawnLocation, mapBounds, world);

            results.add(map);
        }



        return results;
    }


}
