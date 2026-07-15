package com.local.monitor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class PinnedColumnLayout {
    static final int MAX_PINNED_COLUMNS = 4;

    private PinnedColumnLayout() {
    }

    static List<String> presetFor(List<String> available) {
        Map<String, String> actual = availableByNormalizedName(available);
        String landCode = actual.get("map_data_code");
        String podCode = actual.get("pod_code");
        if (landCode == null || podCode == null) {
            return List.of();
        }
        return List.of(landCode, podCode);
    }

    static List<String> normalize(List<String> available, List<String> requested) {
        Map<String, String> actual = availableByNormalizedName(available);
        Set<String> seen = new LinkedHashSet<>();
        List<String> normalized = new ArrayList<>();
        if (requested != null) {
            for (String candidate : requested) {
                String key = normalizedName(candidate);
                String actualName = actual.get(key);
                if (actualName != null && seen.add(key)) {
                    normalized.add(actualName);
                }
            }
        }
        if (normalized.size() > MAX_PINNED_COLUMNS) {
            throw new IllegalArgumentException("固定列太多，请先取消一列");
        }
        return List.copyOf(normalized);
    }

    static List<String> moveUp(List<String> columns, int index) {
        List<String> moved = mutableCopy(columns);
        if (index > 0 && index < moved.size()) {
            String value = moved.remove(index);
            moved.add(index - 1, value);
        }
        return List.copyOf(moved);
    }

    static List<String> moveDown(List<String> columns, int index) {
        List<String> moved = mutableCopy(columns);
        if (index >= 0 && index < moved.size() - 1) {
            String value = moved.remove(index);
            moved.add(index + 1, value);
        }
        return List.copyOf(moved);
    }

    private static Map<String, String> availableByNormalizedName(List<String> available) {
        Map<String, String> actual = new LinkedHashMap<>();
        if (available != null) {
            for (String name : available) {
                String key = normalizedName(name);
                if (!key.isEmpty()) {
                    actual.putIfAbsent(key, name);
                }
            }
        }
        return actual;
    }

    private static List<String> mutableCopy(List<String> columns) {
        return columns == null ? new ArrayList<>() : new ArrayList<>(columns);
    }

    private static String normalizedName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
