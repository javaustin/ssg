package com.carrotguy69.ssg.messages;

import java.util.Map;

import static com.carrotguy69.cxyz.CXYZ.msgYML;
import static com.carrotguy69.cxyz.messages.MessageUtils.formatPlaceholders;

public class MessageGrabber {
    public static String grab(String key, Map<String, Object> values) {
        String template = msgYML.getString(key, key);

        return formatPlaceholders(template, values);
    }

    public static String grab(MessageKey key, Map<String, Object> values) {
        return grab(key.getPath(), values);
    }

    public static String grab(MessageKey key) {
        return grab(key.getPath(), Map.of());
    }

    public static String grab(String key) {
        return grab(key, Map.of());
    }
}
