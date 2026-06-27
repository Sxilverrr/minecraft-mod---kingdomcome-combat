package com.kingdomcomecombat.passive;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PassiveSkillConfigs {
    private static final Map<String, PassiveSkillConfig> BY_ID = new ConcurrentHashMap<>();

    private PassiveSkillConfigs() {
    }

    public static void clear() {
        BY_ID.clear();
    }

    public static void register(PassiveSkillConfig config) {
        if (config == null || config.id().isBlank()) {
            return;
        }
        BY_ID.put(config.id(), config);
    }

    public static PassiveSkillConfig get(String id) {
        return BY_ID.get(id);
    }

    public static List<PassiveSkillConfig> all() {
        List<PassiveSkillConfig> skills = new ArrayList<>(BY_ID.values());
        skills.sort(Comparator.comparing(PassiveSkillConfig::source).thenComparing(skill -> skill.name()));
        return List.copyOf(skills);
    }
}
