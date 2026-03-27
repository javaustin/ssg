package com.carrotguy69.ssg.models.map.sources;

import org.bukkit.Location;

public class SchematicSource implements MapSource {

    private final String fileName;
    private final Location pasteLocation;

    public SchematicSource(String fileName, Location pasteLocation) {
        this.fileName = fileName;
        this.pasteLocation = pasteLocation;
    }

    @Override
    public Type getType() {
        return Type.SCHEMATIC;
    }

    public String getFileName() {
        return fileName;
    }

    public Location getPasteLocation() {
        return pasteLocation;
    }
}
