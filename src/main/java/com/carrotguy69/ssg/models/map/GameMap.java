package com.carrotguy69.ssg.models.map;

import com.carrotguy69.ssg.exceptions.MapLoadException;
import com.carrotguy69.ssg.models.map.sources.MapSource;
import com.carrotguy69.ssg.models.map.sources.SchematicSource;
import com.carrotguy69.ssg.models.map.sources.WorldCopySource;
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
import org.bukkit.util.BoundingBox;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class GameMap {

    private final String id;
    private final String name;
    private final MapSource source;
    private final Location lobbySpawn;
    private final List<Location> spawns;
    private final BoundingBox bounds;

    public GameMap(String id, String name, MapSource source, List<Location> spawns, Location lobbySpawnPoint, BoundingBox bounds) {
        this.id = id;
        this.name = name;
        this.source = source;
        this.spawns = spawns;
        this.lobbySpawn = lobbySpawnPoint;
        this.bounds = bounds;
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

    public void load() {
        if (source.getType() == MapSource.Type.SCHEMATIC) {
            SchematicSource schemSource = (SchematicSource) source;

            generateBySchematic(schemSource.getFileName(), schemSource.getPasteLocation());
        }

        else if (source.getType() == MapSource.Type.WORLD_COPY) {
            WorldCopySource wcSource = (WorldCopySource) source;

            generateByWorldCopy(wcSource.getWorldName(), wcSource.getCopyBounds(), wcSource.getPasteLocation());
        }

    }

    private void generateBySchematic(String fileName, Location pasteLocation) {
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
            throw new MapLoadException(String.format("Could not find schematic by id='%s'. Make sure your schematic is in WorldEdit/schematics and matches the ID in maps.yml.", name));
        }
    }

    private void generateByWorldCopy(String fromWorldName, BoundingBox copyBounds, Location pasteLocation) {
        World world = Bukkit.getWorld(fromWorldName);

        if (world == null) {
            throw new MapLoadException(String.format("Could not find world by name='%s'.", fromWorldName));
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


}
