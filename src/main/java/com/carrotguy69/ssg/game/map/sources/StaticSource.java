package com.carrotguy69.ssg.game.map.sources;

public class StaticSource implements MapSource {
    @Override
    public Type getType() {
        return Type.STATIC;
    }

    @Override
    public String toString() {
        return "StaticSource{}";
    }
}
