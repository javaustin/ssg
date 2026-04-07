package com.carrotguy69.ssg.game.map.sources;

public interface MapSource {
    enum Type {
        SCHEMATIC,
        WORLD_COPY,
        STATIC
    }

    Type getType();
}
