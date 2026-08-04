package com.kingdomcomecombat.passive;

import com.kingdomcomecombat.network.PassiveSkillUnlocksSyncPayload;
import com.kingdomcomecombat.network.PassiveSkillConfigsSyncPayload;
import com.kingdomcomecombat.equipment.MobCombatAttributesRegistry;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.mob.HostileEntity;
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
    private static final String COMBAT_EXPERIENCE_NBT_KEY = "kingdom_come_combat_combat_experience";
    private static final String COMBAT_EXPERIENCE_REMAINDER_NBT_KEY =
            "kingdom_come_combat_combat_experience_remainder";
    private static final ConcurrentHashMap<UUID, Set<String>> UNLOCKED = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Integer> COMBAT_EXPERIENCE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Double> COMBAT_EXPERIENCE_REMAINDER =
            new ConcurrentHashMap<>();
    private static int scanTicks;

    private PlayerPassiveSkillProgress() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sync(handler.player);
            ServerPlayNetworking.send(handler.player, PassiveSkillConfigsSyncPayload.current());
        });
        ServerTickEvents.END_SERVER_TICK.register(PlayerPassiveSkillProgress::tick);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (source.getAttacker() instanceof ServerPlayerEntity player
                    && (entity instanceof HostileEntity
                    || MobCombatAttributesRegistry.get(entity).isPresent())) {
                addCombatExperience(player, CombatExperienceConfig.kill());
            }
        });
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
        if (combatExperience(player) < cost) {
            player.sendMessage(Text.literal("战斗经验不足：需要 " + cost + " 点"), false);
            return false;
        }

        if (cost > 0) {
            COMBAT_EXPERIENCE.compute(player.getUuid(), (uuid, current) ->
                    Math.max(0, (current == null ? 0 : current) - cost));
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

    public static int combatExperience(ServerPlayerEntity player) {
        return COMBAT_EXPERIENCE.getOrDefault(player.getUuid(), 0);
    }

    public static void addCombatExperience(ServerPlayerEntity player, int amount) {
        if (player == null || amount <= 0) {
            return;
        }
        COMBAT_EXPERIENCE.merge(player.getUuid(), amount, (current, added) -> {
            long total = (long) current + added;
            return (int) Math.min(Integer.MAX_VALUE, total);
        });
        sync(player);
    }

    public static void addCombatExperienceFromVanilla(ServerPlayerEntity player, int vanillaExperience) {
        if (player == null || vanillaExperience <= 0
                || CombatExperienceConfig.vanillaExperienceMultiplier() <= 0.0) {
            return;
        }
        double converted = vanillaExperience * CombatExperienceConfig.vanillaExperienceMultiplier()
                + COMBAT_EXPERIENCE_REMAINDER.getOrDefault(player.getUuid(), 0.0);
        int wholeExperience = (int) Math.min(Integer.MAX_VALUE, Math.floor(converted));
        COMBAT_EXPERIENCE_REMAINDER.put(
                player.getUuid(),
                wholeExperience == Integer.MAX_VALUE ? 0.0 : converted - wholeExperience
        );
        if (wholeExperience > 0) {
            addCombatExperience(player, wholeExperience);
        }
    }

    public static void clearCache() {
        UNLOCKED.clear();
        COMBAT_EXPERIENCE.clear();
        COMBAT_EXPERIENCE_REMAINDER.clear();
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
        COMBAT_EXPERIENCE.put(player.getUuid(), Math.max(0, view.getInt(COMBAT_EXPERIENCE_NBT_KEY, 0)));
        COMBAT_EXPERIENCE_REMAINDER.put(
                player.getUuid(),
                Math.max(0.0, Math.min(0.999999,
                        view.getDouble(COMBAT_EXPERIENCE_REMAINDER_NBT_KEY, 0.0)))
        );
    }

    public static void save(ServerPlayerEntity player, WriteView view) {
        view.putString(NBT_KEY, String.join(",", unlocked(player.getUuid())));
        view.putInt(COMBAT_EXPERIENCE_NBT_KEY, combatExperience(player));
        view.putDouble(
                COMBAT_EXPERIENCE_REMAINDER_NBT_KEY,
                COMBAT_EXPERIENCE_REMAINDER.getOrDefault(player.getUuid(), 0.0)
        );
    }

    public static void copy(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer) {
        UNLOCKED.put(newPlayer.getUuid(), new HashSet<>(unlocked(oldPlayer.getUuid())));
        COMBAT_EXPERIENCE.put(newPlayer.getUuid(), combatExperience(oldPlayer));
        COMBAT_EXPERIENCE_REMAINDER.put(
                newPlayer.getUuid(),
                COMBAT_EXPERIENCE_REMAINDER.getOrDefault(oldPlayer.getUuid(), 0.0)
        );
    }

    public static void sync(ServerPlayerEntity player) {
        ServerPlayNetworking.send(
                player,
                new PassiveSkillUnlocksSyncPayload(
                        unlocked(player.getUuid()).stream().sorted().toList(),
                        combatExperience(player),
                        CombatExperienceConfig.kill(),
                        CombatExperienceConfig.perfectBlock(),
                        CombatExperienceConfig.perfectCounter(),
                        CombatExperienceConfig.attack(),
                        CombatExperienceConfig.masterCounter(),
                        CombatExperienceConfig.combo(),
                        CombatExperienceConfig.vanillaExperienceMultiplier()
                )
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
