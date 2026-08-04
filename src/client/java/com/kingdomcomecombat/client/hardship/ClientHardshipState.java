package com.kingdomcomecombat.client.hardship;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kingdomcomecombat.hardship.HardshipConfig;

import java.util.List;

public final class ClientHardshipState {
    private static final Gson GSON = new Gson();
    private static volatile List<HardshipConfig> definitions = List.of();
    private static volatile List<String> selected = List.of();

    private ClientHardshipState() {}

    public static void replaceDefinitions(String json) {
        try {
            List<HardshipConfig> decoded = GSON.fromJson(json, new TypeToken<List<HardshipConfig>>() {}.getType());
            definitions = decoded == null ? List.of() : List.copyOf(decoded);
        } catch (Exception ignored) { definitions = List.of(); }
    }

    public static List<HardshipConfig> definitions() { return definitions; }
    public static void setSelected(List<String> ids) { selected = ids == null ? List.of() : List.copyOf(ids); }
    public static List<String> selected() { return selected; }
    public static boolean selected(String id) { return selected.contains(id); }
}
