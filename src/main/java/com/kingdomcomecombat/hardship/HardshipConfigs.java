package com.kingdomcomecombat.hardship;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HardshipConfigs {
    private static final Map<String, HardshipConfig> BY_ID = new LinkedHashMap<>();

    private HardshipConfigs() {}

    public static synchronized void clear() { BY_ID.clear(); }

    public static synchronized void register(HardshipConfig config) {
        if (config != null && !config.id().isBlank() && BY_ID.size() < 20) {
            BY_ID.put(config.id(), config);
        }
    }

    public static synchronized HardshipConfig get(String id) { return BY_ID.get(id); }

    public static synchronized List<HardshipConfig> all() {
        return List.copyOf(new ArrayList<>(BY_ID.values()));
    }
}
