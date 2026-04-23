package com.veertu.plugin.anka;

final class AnkaLog {
    private static final String PREFIX = "[Anka] ";

    private AnkaLog() {
    }

    static String prefix(String message) {
        if (message == null || message.startsWith(PREFIX)) {
            return message;
        }
        return PREFIX + message;
    }
}
