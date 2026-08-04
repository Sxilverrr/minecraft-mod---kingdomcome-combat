package com.kingdomcomecombat.client.passive;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kingdomcomecombat.passive.PassiveSkillConfig;

import java.lang.reflect.Type;
import java.util.List;

public final class ClientPassiveSkillConfigState {
    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<PassiveSkillConfig>>() { }.getType();
    private static volatile List<PassiveSkillConfig> skills = List.of();

    private ClientPassiveSkillConfigState() {
    }

    public static void replaceFromJson(String json) {
        try {
            List<PassiveSkillConfig> decoded = GSON.fromJson(json, LIST_TYPE);
            skills = decoded == null ? List.of() : List.copyOf(decoded);
        } catch (RuntimeException ignored) {
            skills = List.of();
        }
    }

    public static List<PassiveSkillConfig> all() {
        return skills;
    }

    public static void clear() {
        skills = List.of();
    }
}
