package com.kingdomcomecombat.combat;

import com.kingdomcomecombat.network.ComboUnlocksSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.util.Identifier;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerComboProgress {
    private static final String NBT_KEY = "kingdom_come_combat_unlocked_combos";
    private static final Set<String> DEFAULT_UNLOCKS = Set.of("comboleft", "comboright");
    private static final Identifier SWORDMASTER_ADVANCEMENT =
            Identifier.of("kingdom_come_combat", "bohemian_swordmaster");
    private static final ConcurrentHashMap<UUID, Set<String>> UNLOCKED = new ConcurrentHashMap<>();

    private PlayerComboProgress() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                sync(handler.player)
        );
    }

    public static boolean isUnlocked(ServerPlayerEntity player, ComboMoveConfig combo) {
        return combo != null && unlocked(player.getUuid()).contains(combo.id());
    }

    public static boolean unlock(ServerPlayerEntity player, String comboId) {
        Set<String> combos = unlocked(player.getUuid());
        boolean changed = combos.add(comboId);
        if (changed) {
            sync(player);
            grantSwordmasterAdvancement(player);
        }
        return changed;
    }

    public static Set<String> unlocked(UUID uuid) {
        return UNLOCKED.computeIfAbsent(uuid, ignored -> new HashSet<>(DEFAULT_UNLOCKS));
    }

    public static void clearCache() {
        UNLOCKED.clear();
    }

    public static void load(ServerPlayerEntity player, ReadView view) {
        Set<String> combos = new HashSet<>(DEFAULT_UNLOCKS);
        String encoded = view.getString(NBT_KEY, "");
        if (!encoded.isBlank()) {
            for (String id : encoded.split(",")) {
                if (!id.isBlank()) {
                    combos.add(id);
                }
            }
        }
        UNLOCKED.put(player.getUuid(), combos);
    }

    public static void save(ServerPlayerEntity player, WriteView view) {
        view.putString(NBT_KEY, String.join(",", unlocked(player.getUuid())));
    }

    public static void copy(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer) {
        UNLOCKED.put(newPlayer.getUuid(), new HashSet<>(unlocked(oldPlayer.getUuid())));
    }

    public static void sync(ServerPlayerEntity player) {
        ServerPlayNetworking.send(
                player,
                new ComboUnlocksSyncPayload(unlocked(player.getUuid()).stream().sorted().toList())
        );
        grantSwordmasterAdvancement(player);
    }

    private static void grantSwordmasterAdvancement(ServerPlayerEntity player) {
        Set<String> unlocked = unlocked(player.getUuid());
        if (ComboMoveConfigs.all().isEmpty()
                || ComboMoveConfigs.all().stream().anyMatch(combo -> !unlocked.contains(combo.id()))) {
            return;
        }
        if (player.getServer() == null) {
            return;
        }
        AdvancementEntry advancement = player.getServer().getAdvancementLoader().get(SWORDMASTER_ADVANCEMENT);
        if (advancement != null) {
            player.getAdvancementTracker().grantCriterion(advancement, "all_combos");
        }
    }
}
