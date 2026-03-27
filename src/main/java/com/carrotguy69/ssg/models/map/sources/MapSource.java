package com.carrotguy69.ssg.models.map.sources;

public interface MapSource {
    enum Type {
        SCHEMATIC,
        WORLD_COPY,
        STATIC
    }

    Type getType();
}
