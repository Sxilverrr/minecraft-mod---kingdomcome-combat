package com.kingdomcomecombat.hardship;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.game.ModGameRules;
import com.kingdomcomecombat.network.HardshipSelectionPromptPayload;
import com.kingdomcomecombat.network.HardshipSelectionSyncPayload;
import net.minecraft.advancement.AdvancementEntry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class HardshipSelectionState {
    public static final int MINIMUM_SELECTIONS = 6;
    private static final Gson GSON = new Gson();
    private static final Map<MinecraftServer, State> STATES = new WeakHashMap<>();

    private HardshipSelectionState() {}

    public static synchronized void clear(MinecraftServer server) { STATES.remove(server); }

    public static void onJoin(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        State state = state(server);
        PlayerSelection selection = state.players.get(player.getUuid());
        if (selection != null && selection.complete) {
            sync(player, selection.selected);
            grantAllHardshipsAdvancement(player, selection.selected);
            return;
        }
        if (!ModGameRules.hardcoreMode((net.minecraft.server.world.ServerWorld) player.getWorld())) return;
        ServerPlayNetworking.send(player, new HardshipSelectionPromptPayload(
                GSON.toJson(HardshipConfigs.all()), MINIMUM_SELECTIONS));
    }

    public static boolean submit(ServerPlayerEntity player, List<String> submitted) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;
        if (!ModGameRules.hardcoreMode((net.minecraft.server.world.ServerWorld) player.getWorld())) return false;
        State state = state(server);
        PlayerSelection existing = state.players.get(player.getUuid());
        if (existing != null && existing.complete) return false;
        Set<String> unique = new HashSet<>(submitted == null ? List.of() : submitted);
        if (unique.size() < MINIMUM_SELECTIONS || unique.size() > 20) return false;
        for (String id : unique) if (HardshipConfigs.get(id) == null) return false;
        List<String> selected = unique.stream().sorted().toList();
        state.players.put(player.getUuid(), new PlayerSelection(true, selected));
        save(server, state);
        sync(player, selected);
        grantAllHardshipsAdvancement(player, selected);
        return true;
    }

    public static List<HardshipConfig> selected(ServerPlayerEntity player) {
        List<HardshipConfig> result = new ArrayList<>();
        MinecraftServer server = player.getServer();
        if (server == null) return List.of();
        PlayerSelection selection = state(server).players.get(player.getUuid());
        if (selection == null || !selection.complete) return List.of();
        for (String id : selection.selected) {
            HardshipConfig config = HardshipConfigs.get(id);
            if (config != null) result.add(config);
        }
        return List.copyOf(result);
    }

    public static boolean active(ServerPlayerEntity player, String id) {
        MinecraftServer server = player.getServer();
        if (server == null
                || !ModGameRules.hardcoreMode((net.minecraft.server.world.ServerWorld) player.getWorld())) {
            return false;
        }
        State state = state(server);
        PlayerSelection selection = state.players.get(player.getUuid());
        return selection != null && selection.complete && selection.selected.contains(id);
    }

    public static double prek(ServerPlayerEntity player, String id, String key, double fallback) {
        if (!active(player, id)) return fallback;
        HardshipConfig config = HardshipConfigs.get(id);
        return config == null ? fallback : config.prek(key, fallback);
    }

    private static void sync(ServerPlayerEntity player, List<String> selected) {
        if (ServerPlayNetworking.canSend(player, HardshipSelectionSyncPayload.ID)) {
            ServerPlayNetworking.send(player, new HardshipSelectionSyncPayload(selected));
        }
    }

    private static synchronized State state(MinecraftServer server) {
        return STATES.computeIfAbsent(server, HardshipSelectionState::load);
    }

    private static State load(MinecraftServer server) {
        Path file = file(server);
        if (!Files.isRegularFile(file)) return new State();
        try {
            JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            State state = new State();
            JsonObject players = json.getAsJsonObject("players");
            if (players != null) {
                for (Map.Entry<String, com.google.gson.JsonElement> entry : players.entrySet()) {
                    UUID playerId = UUID.fromString(entry.getKey());
                    JsonObject value = entry.getValue().getAsJsonObject();
                    List<String> selected = readSelected(value.getAsJsonArray("selected"));
                    boolean complete = value.has("complete") && value.get("complete").getAsBoolean();
                    state.players.put(playerId, new PlayerSelection(complete, selected));
                }
                return state;
            }

            // Migrate the original world-wide choice without forcing it on every
            // multiplayer participant. Only its original chooser keeps that choice.
            String chooser = json.has("chooser") ? json.get("chooser").getAsString() : "";
            if (!chooser.isBlank()) {
                state.players.put(
                        UUID.fromString(chooser),
                        new PlayerSelection(
                                json.has("complete") && json.get("complete").getAsBoolean(),
                                readSelected(json.getAsJsonArray("selected"))
                        )
                );
            }
            return state;
        } catch (Exception exception) {
            KingdomComeCombat.LOGGER.error("Failed to load world hardship selection", exception);
            return new State();
        }
    }

    private static void save(MinecraftServer server, State state) {
        try {
            Path file = file(server);
            JsonObject json = new JsonObject();
            JsonObject players = new JsonObject();
            for (Map.Entry<UUID, PlayerSelection> entry : state.players.entrySet()) {
                JsonObject value = new JsonObject();
                value.addProperty("complete", entry.getValue().complete);
                JsonArray selected = new JsonArray();
                entry.getValue().selected.forEach(selected::add);
                value.add("selected", selected);
                players.add(entry.getKey().toString(), value);
            }
            json.add("players", players);
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(json), StandardCharsets.UTF_8);
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception exception) {
            KingdomComeCombat.LOGGER.error("Failed to save world hardship selection", exception);
        }
    }

    private static Path file(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("kcc_hardships.json");
    }

    private static List<String> readSelected(JsonArray values) {
        List<String> selected = new ArrayList<>();
        if (values != null) values.forEach(value -> selected.add(value.getAsString()));
        return List.copyOf(selected);
    }

    private static void grantAllHardshipsAdvancement(ServerPlayerEntity player, List<String> selected) {
        if (player.getServer() == null
                || HardshipConfigs.all().isEmpty()
                || selected.size() != HardshipConfigs.all().size()) {
            return;
        }
        AdvancementEntry advancement = player.getServer().getAdvancementLoader().get(
                net.minecraft.util.Identifier.of(KingdomComeCombat.MOD_ID, "godforsaken_child")
        );
        if (advancement != null) {
            player.getAdvancementTracker().grantCriterion(advancement, "all_hardships");
        }
    }

    private static final class State {
        private final Map<UUID, PlayerSelection> players = new java.util.HashMap<>();
    }

    private record PlayerSelection(boolean complete, List<String> selected) {}
}
