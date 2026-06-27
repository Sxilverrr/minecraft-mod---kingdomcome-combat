package com.kingdomcomecombat.passive;

import com.kingdomcomecombat.network.PassiveSkillUnlocksSyncPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerPassiveSkillProgress {
    private static final String NBT_KEY = "kingdom_come_combat_unlocked_passive_skills";
    private static final ConcurrentHashMap<UUID, Set<String>> UNLOCKED = new ConcurrentHashMap<>();
    private static int scanTicks;

    private PlayerPassiveSkillProgress() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));
        ServerTickEvents.END_SERVER_TICK.register(PlayerPassiveSkillProgress::tick);
    }

    public static boolean isUnlocked(ServerPlayerEntity player, String skillId) {
        return skillId != null && unlocked(player.getUuid()).contains(skillId);
    }

    public static boolean unlock(ServerPlayerEntity player, String skillId) {
        PassiveSkillConfig config = PassiveSkillConfigs.get(skillId);
        if (config == null) {
            player.sendMessage(Text.literal("被动技能不存在：" + skillId), false);
            return false;
        }

        Set<String> skills = unlocked(player.getUuid());
        boolean changed = skills.add(skillId);
        if (changed) {
            sync(player);
        }
        return changed;
    }

    public static boolean learnWithExperience(ServerPlayerEntity player, String skillId) {
        PassiveSkillConfig config = PassiveSkillConfigs.get(skillId);
        if (config == null) {
            player.sendMessage(Text.literal("被动技能不存在：" + skillId), false);
            return false;
        }
        if (config.source() != PassiveSkillConfig.Source.EXPERIENCE) {
            player.sendMessage(Text.literal("这个被动技能不能用经验学习：" + config.name()), false);
            return false;
        }
        if (isUnlocked(player, skillId)) {
            player.sendMessage(Text.literal("你已经掌握了：" + config.name()), false);
            return false;
        }
        int cost = config.experienceCost();
        if (player.totalExperience < cost) {
            player.sendMessage(Text.literal("经验不足：需要 " + cost + " 经验"), false);
            return false;
        }

        if (cost > 0) {
            player.addExperience(-cost);
        }
        boolean changed = unlock(player, skillId);
        if (changed) {
            player.sendMessage(Text.literal("习得被动技能：" + config.name()), false);
        }
        return changed;
    }

    public static Set<String> unlocked(UUID uuid) {
        return UNLOCKED.computeIfAbsent(uuid, ignored -> new HashSet<>());
    }

    public static void clearCache() {
        UNLOCKED.clear();
    }

    public static void load(ServerPlayerEntity player, ReadView view) {
        Set<String> skills = new HashSet<>();
        String encoded = view.getString(NBT_KEY, "");
        if (!encoded.isBlank()) {
            for (String id : encoded.split(",")) {
                if (!id.isBlank()) {
                    skills.add(id);
                }
            }
        }
        UNLOCKED.put(player.getUuid(), skills);
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
                new PassiveSkillUnlocksSyncPayload(unlocked(player.getUuid()).stream().sorted().toList())
        );
    }

    private static void tick(MinecraftServer server) {
        scanTicks++;
        if (scanTicks < 40) {
            return;
        }
        scanTicks = 0;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            scanAdvancementSkills(player);
            PassiveSkillPerks.tick(player);
        }
    }

    private static void scanAdvancementSkills(ServerPlayerEntity player) {
        for (PassiveSkillConfig skill : PassiveSkillConfigs.all()) {
            if (skill.source() != PassiveSkillConfig.Source.ADVANCEMENT
                    || skill.advancement().isBlank()
                    || isUnlocked(player, skill.id())) {
                continue;
            }

            Identifier id = Identifier.tryParse(skill.advancement());
            if (id == null) {
                continue;
            }

            AdvancementEntry advancement = player.getServer().getAdvancementLoader().get(id);
            if (advancement != null && player.getAdvancementTracker().getProgress(advancement).isDone()) {
                unlock(player, skill.id());
                player.sendMessage(Text.literal("习得被动技能：" + skill.name()), false);
            }
        }
    }
}
