package com.local.monitor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class AlertState {
    private final Set<String> acknowledgedKeys = new HashSet<>();

    public void acknowledge(String alertKey) {
        for (String key : splitKeys(alertKey)) {
            acknowledgedKeys.add(key);
        }
    }

    boolean isAcknowledged(String alertKey) {
        Set<String> keys = splitKeys(alertKey);
        return !keys.isEmpty() && acknowledgedKeys.containsAll(keys);
    }

    void clearAcknowledgementsForCodes(Collection<String> codes) {
        acknowledgedKeys.removeIf(key -> belongsToAnyCode(key, codes));
    }

    private static boolean belongsToAnyCode(String key, Collection<String> codes) {
        for (String code : codes) {
            if (key.startsWith(code + ":")) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> splitKeys(String alertKey) {
        Set<String> keys = new HashSet<>();
        if (alertKey == null || alertKey.isBlank()) {
            return keys;
        }
        for (String key : alertKey.split("\\|")) {
            if (!key.isBlank()) {
                keys.add(key);
            }
        }
        return keys;
    }
}


