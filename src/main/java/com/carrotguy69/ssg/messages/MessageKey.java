package com.carrotguy69.ssg.messages;

public enum MessageKey {
    ;

    private final String path;

    MessageKey(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
