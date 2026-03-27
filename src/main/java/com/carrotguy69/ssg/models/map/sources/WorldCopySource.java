package com.carrotguy69.ssg.models.map.sources;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

public class WorldCopySource implements MapSource {
    @Override
    public Type getType() {
        return Type.WORLD_COPY;
    }

    private final String worldName;
    private final BoundingBox copyBounds;
    private final Location pasteLocation;

    public WorldCopySource(String worldName, BoundingBox copyBounds, Location pasteLocation) {
        this.worldName = worldName;
        this.copyBounds = copyBounds;
        this.pasteLocation = pasteLocation;
    }

    public String getWorldName() {
        return worldName;
    }

    public BoundingBox getCopyBounds() {
        return copyBounds;
    }

    public Location getPasteLocation() {
        return pasteLocation;
    }
}
