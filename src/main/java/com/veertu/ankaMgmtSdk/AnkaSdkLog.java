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

    static String cloudLabel(String cloudName) {
        if (cloudName != null && !cloudName.isEmpty()) {
            return String.format("Anka cloud '%s'", cloudName);
        }
        return "Anka cloud (unnamed)";
    }
}
