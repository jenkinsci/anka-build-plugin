package com.veertu.ankaMgmtSdk;

final class AnkaSdkLog {
    private static final String PREFIX = "[Anka] ";

    private AnkaSdkLog() {
    }

    static String prefix(String message) {
        if (message == null || message.startsWith(PREFIX)) {
            return message;
        }
        return PREFIX + message;
    }
}
