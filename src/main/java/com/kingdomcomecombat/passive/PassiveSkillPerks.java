package com.kingdomcomecombat.passive;

import com.kingdomcomecombat.combat.CombatItemUtil;
import com.kingdomcomecombat.combat.BeowulfArmState;
import com.kingdomcomecombat.equipment.EquipmentCombatAttributesRegistry;
import com.kingdomcomecombat.injury.ModStatusEffects;
import com.kingdomcomecombat.stamina.ServerStaminaState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PassiveSkillPerks {
    private static final Map<UUID, TimedStacks> TARGET_REGEN_PENALTIES = new HashMap<>();
    private static final Map<UUID, Long> KILL_REGEN_BUFFS = new HashMap<>();
    private static final Map<UUID, RapidAttackState> RAPID_ATTACKS = new HashMap<>();

    private PassiveSkillPerks() {
    }

    public static double damageMultiplier(
            LivingEntity attacker,
            ItemStack weapon,
            boolean combo,
            boolean masterCounter,
            String detailedPart
    ) {
        if (!(attacker instanceof ServerPlayerEntity player)) {
            return 1.0;
        }
        double bonus = 0.0;
        for (PassiveSkillConfig skill : unlocked(player)) {
            if (!matches(skill, player, weapon, combo, masterCounter, detailedPart)) {
                continue;
            }
            bonus += skill.perk("damage_multiplier", 1.0) - 1.0;
            if (combo) {
                bonus += skill.perk("combo_damage_multiplier", 1.0) - 1.0;
            }
            if (masterCounter) {
                bonus += skill.perk("master_counter_damage_multiplier", 1.0) - 1.0;
            }
            if (isHeadPart(detailedPart)) {
                bonus += skill.perk("head_hit_damage_multiplier", 1.0) - 1.0;
            }
            if (healthRatio(player) <= skill.perk("low_health_threshold", -1.0)) {
                bonus += skill.perk("low_health_damage_multiplier", 1.0) - 1.0;
            }
            if (skill.hasPerk("rapid_attack_damage_per_stack")) {
                bonus += rapidAttackBonus(player, skill);
            }
        }
        return Math.max(0.0, 1.0 + bonus);
    }

    public static double staminaCostMultiplier(LivingEntity entity, ItemStack weapon, boolean dodge, boolean attack) {
        if (!(entity instanceof ServerPlayerEntity player)) {
            return 1.0;
        }
        double bonus = 0.0;
        for (PassiveSkillConfig skill : unlocked(player)) {
            if (!matches(skill, player, weapon, false, false, "")) {
                continue;
            }
            bonus += skill.perk("stamina_cost_multiplier", 1.0) - 1.0;
            if (dodge) {
                bonus += skill.perk("dodge_stamina_cost_multiplier", 1.0) - 1.0;
            }
            if (attack) {
                bonus += skill.perk("attack_stamina_cost_multiplier", 1.0) - 1.0;
            }
        }
        return Math.max(0.0, 1.0 + bonus);
    }

    public static double staminaRegenMultiplier(LivingEntity entity) {
        if (!(entity instanceof ServerPlayerEntity player)) {
            return 1.0;
        }
        double bonus = 0.0;
        for (PassiveSkillConfig skill : unlocked(player)) {
            if (matches(skill, player, player.getMainHandStack(), false, false, "")) {
                bonus += skill.perk("stamina_regen_multiplier", 1.0) - 1.0;
            }
        }
        long now = player.getWorld().getTime();
        Long killBuffUntil = KILL_REGEN_BUFFS.get(player.getUuid());
        if (killBuffUntil != null) {
            if (killBuffUntil > now) {
                bonus += summedPerk(player, "kill_stamina_regen_multiplier") - 1.0;
            } else {
                KILL_REGEN_BUFFS.remove(player.getUuid());
            }
        }
        TimedStacks penalty = TARGET_REGEN_PENALTIES.get(player.getUuid());
        if (penalty != null) {
            if (penalty.expiresAt() > now) {
                bonus += penalty.stacks() * (penalty.multiplierPerStack() - 1.0);
            } else {
                TARGET_REGEN_PENALTIES.remove(player.getUuid());
            }
        }
        return Math.max(0.0, 1.0 + bonus);
    }

    public static double armorPenaltyMultiplier(LivingEntity entity) {
        if (!(entity instanceof ServerPlayerEntity player)) {
            return 1.0;
        }
        double bonus = 0.0;
        for (PassiveSkillConfig skill : unlocked(player)) {
            if (matches(skill, player, player.getMainHandStack(), false, false, "")) {
                bonus += skill.perk("armor_penalty_multiplier", 1.0) - 1.0;
            }
        }
        return Math.max(0.0, 1.0 + bonus);
    }

    public static double attackSpeedMultiplier(LivingEntity entity, ItemStack weapon) {
        if (!(entity instanceof ServerPlayerEntity player)) {
            return 1.0;
        }
        double bonus = 0.0;
        for (PassiveSkillConfig skill : unlocked(player)) {
            if (BeowulfArmState.SKILL_ID.equals(skill.id())) {
                continue;
            }
            if (matches(skill, player, weapon, false, false, "")) {
                bonus += skill.perk("attack_speed_multiplier", 1.0) - 1.0;
            }
        }
        return Math.max(0.0, 1.0 + bonus);
    }

    public static void afterKill(LivingEntity attacker) {
        if (!(attacker instanceof ServerPlayerEntity player)) {
            return;
        }
        for (PassiveSkillConfig skill : unlocked(player)) {
            if (matches(skill, player, player.getMainHandStack(), false, false, "")) {
                restoreStamina(player, skill.perk("kill_stamina_restore", 0.0));
                int duration = (int) skill.perk("kill_stamina_regen_duration_ticks", 0.0);
                if (duration > 0 && skill.hasPerk("kill_stamina_regen_multiplier")) {
                    KILL_REGEN_BUFFS.put(player.getUuid(), player.getWorld().getTime() + duration);
                }
            }
        }
    }

    public static void afterComboHit(LivingEntity attacker, LivingEntity target, ItemStack weapon, String detailedPart) {
        if (!(attacker instanceof ServerPlayerEntity player)) {
            return;
        }
        for (PassiveSkillConfig skill : unlocked(player)) {
            if (matches(skill, player, weapon, true, false, "")) {
                restoreStamina(player, skill.perk("combo_stamina_restore", 0.0));
                int injuryLevels = (int) skill.perk("combo_injury_levels", 0.0);
                if (injuryLevels > 0) {
                    ModStatusEffects.applyInjury(target, injuryType(detailedPart), injuryLevels);
                }
                applyTargetRegenPenalty(skill, target);
            }
        }
    }

    public static void afterHit(LivingEntity attacker) {
        if (!(attacker instanceof ServerPlayerEntity player)) {
            return;
        }
        for (PassiveSkillConfig skill : unlocked(player)) {
            if (!matches(skill, player, player.getMainHandStack(), false, false, "")) {
                continue;
            }
            recordRapidAttackHit(player, skill);
            int level = (int) skill.perk("hit_analgesia_level", 0.0);
            int duration = (int) skill.perk("hit_analgesia_duration_ticks", 0.0);
            if (level > 0 && duration > 0) {
                player.addStatusEffect(new StatusEffectInstance(
                        ModStatusEffects.ANALGESIA,
                        duration,
                        level - 1,
                        false,
                        true,
                        true
                ));
                ModStatusEffects.syncEffectiveAttributeModifiers(player);
            }
        }
    }

    public static void tick(ServerPlayerEntity player) {
        for (PassiveSkillConfig skill : unlocked(player)) {
            if (!matches(skill, player, player.getMainHandStack(), false, false, "")) {
                continue;
            }
            int level = (int) skill.perk("analgesia_level", 0.0);
            if (level > 0) {
                player.addStatusEffect(new StatusEffectInstance(
                        ModStatusEffects.ANALGESIA,
                        60,
                        level - 1,
                        false,
                        true,
                        true
                ));
                ModStatusEffects.syncEffectiveAttributeModifiers(player);
            }
        }
    }

    public static int bleedingIntervalTicks(LivingEntity entity) {
        if (!(entity instanceof ServerPlayerEntity player)) {
            return 200;
        }
        return Math.max(1, (int) Math.round(200.0 * summedPerk(player, "bleeding_interval_multiplier")));
    }

    public static double projectileSpreadMultiplier(LivingEntity shooter, ItemStack weapon) {
        return summedMatchingPerk(shooter, weapon, "projectile_spread_multiplier");
    }

    public static double projectileSpeedMultiplier(LivingEntity shooter, ItemStack weapon) {
        return summedMatchingPerk(shooter, weapon, "projectile_speed_multiplier");
    }

    public static void afterMasterCounter(LivingEntity attacker, ItemStack weapon) {
        if (!(attacker instanceof ServerPlayerEntity player)) {
            return;
        }
        for (PassiveSkillConfig skill : unlocked(player)) {
            if (matches(skill, player, weapon, false, true, "")) {
                restoreStamina(player, skill.perk("master_counter_stamina_restore", 0.0));
            }
        }
    }

    private static Iterable<PassiveSkillConfig> unlocked(ServerPlayerEntity player) {
        return PlayerPassiveSkillProgress.unlocked(player.getUuid()).stream()
                .map(PassiveSkillConfigs::get)
                .filter(skill -> skill != null)
                .toList();
    }

    private static void applyTargetRegenPenalty(PassiveSkillConfig skill, LivingEntity target) {
        if (!skill.hasPerk("target_stamina_regen_multiplier")) {
            return;
        }
        int duration = (int) skill.perk("target_stamina_regen_duration_ticks", 0.0);
        int maxStacks = Math.max(1, (int) skill.perk("target_stamina_regen_max_stacks", 1.0));
        if (duration <= 0) {
            return;
        }
        long now = target.getWorld().getTime();
        TimedStacks current = TARGET_REGEN_PENALTIES.get(target.getUuid());
        int stacks = current == null || current.expiresAt() <= now ? 1 : Math.min(maxStacks, current.stacks() + 1);
        TARGET_REGEN_PENALTIES.put(target.getUuid(), new TimedStacks(
                stacks,
                now + duration,
                skill.perk("target_stamina_regen_multiplier", 1.0)
        ));
    }

    private static double summedMatchingPerk(LivingEntity entity, ItemStack weapon, String key) {
        if (!(entity instanceof ServerPlayerEntity player)) {
            return 1.0;
        }
        double bonus = 0.0;
        for (PassiveSkillConfig skill : unlocked(player)) {
            if (matches(skill, player, weapon, false, false, "")) {
                bonus += skill.perk(key, 1.0) - 1.0;
            }
        }
        return Math.max(0.0, 1.0 + bonus);
    }

    private static double summedPerk(ServerPlayerEntity player, String key) {
        double bonus = 0.0;
        for (PassiveSkillConfig skill : unlocked(player)) {
            bonus += skill.perk(key, 1.0) - 1.0;
        }
        return Math.max(0.0, 1.0 + bonus);
    }

    private static double rapidAttackBonus(ServerPlayerEntity player, PassiveSkillConfig skill) {
        RapidAttackState state = RAPID_ATTACKS.get(player.getUuid());
        if (state == null) {
            return 0.0;
        }
        long now = player.getWorld().getTime();
        long window = Math.max(1L, Math.round(skill.perk("rapid_attack_window_ticks", 30.0)));
        if (now - state.lastHitTick() > window) {
            RAPID_ATTACKS.remove(player.getUuid());
            return 0.0;
        }
        int effectiveStacks = state.lastStackTick() == now
                ? Math.max(0, state.stacks() - 1)
                : state.stacks();
        return effectiveStacks * skill.perk("rapid_attack_damage_per_stack", 0.0);
    }

    private static void recordRapidAttackHit(ServerPlayerEntity player, PassiveSkillConfig skill) {
        if (!skill.hasPerk("rapid_attack_damage_per_stack")) {
            return;
        }
        long now = player.getWorld().getTime();
        long window = Math.max(1L, Math.round(skill.perk("rapid_attack_window_ticks", 30.0)));
        RapidAttackState current = RAPID_ATTACKS.get(player.getUuid());
        if (current != null && current.lastStackTick() == now) {
            RAPID_ATTACKS.put(player.getUuid(), new RapidAttackState(
                    current.stacks(),
                    now,
                    current.lastStackTick()
            ));
            return;
        }
        int stacks = current != null && now - current.lastHitTick() <= window
                ? current.stacks() + 1
                : 1;
        RAPID_ATTACKS.put(player.getUuid(), new RapidAttackState(stacks, now, now));
    }

    private static void restoreStamina(LivingEntity entity, double amount) {
        if (amount > 0.0) {
            ServerStaminaState.restore(entity, amount);
        }
    }

    private static boolean matches(
            PassiveSkillConfig skill,
            ServerPlayerEntity player,
            ItemStack weapon,
            boolean combo,
            boolean masterCounter,
            String detailedPart
    ) {
        if (skill.hasPerk("condition_combo") && asBool(skill.perk("condition_combo", 0.0)) != combo) {
            return false;
        }
        if (skill.hasPerk("condition_master_counter") && asBool(skill.perk("condition_master_counter", 0.0)) != masterCounter) {
            return false;
        }
        if (skill.hasPerk("condition_mounted") && asBool(skill.perk("condition_mounted", 0.0)) != player.hasVehicle()) {
            return false;
        }
        if (skill.hasPerk("condition_offhand_empty") && asBool(skill.perk("condition_offhand_empty", 0.0)) != player.getOffHandStack().isEmpty()) {
            return false;
        }
        if (skill.hasPerk("condition_offhand_occupied") && asBool(skill.perk("condition_offhand_occupied", 0.0)) != !player.getOffHandStack().isEmpty()) {
            return false;
        }
        if (skill.hasPerk("condition_armor_below") && vanillaArmor(player) >= skill.perk("condition_armor_below", 0.0)) {
            return false;
        }
        if (skill.hasPerk("condition_health_below") && healthRatio(player) >= skill.perk("condition_health_below", 0.0)) {
            return false;
        }
        if (skill.hasPerk("condition_head_hit") && asBool(skill.perk("condition_head_hit", 0.0)) != isHeadPart(detailedPart)) {
            return false;
        }
        return weaponMatches(skill, weapon);
    }

    private static boolean weaponMatches(PassiveSkillConfig skill, ItemStack weapon) {
        if (skill.hasPerk("condition_weapon_longsword")
                && asBool(skill.perk("condition_weapon_longsword", 0.0)) != CombatItemUtil.isLongsword(weapon)) {
            return false;
        }
        if (skill.hasPerk("condition_weapon_sword")
                && asBool(skill.perk("condition_weapon_sword", 0.0)) != CombatItemUtil.isSword(weapon)) {
            return false;
        }
        if (skill.hasPerk("condition_weapon_short_sword")
                && asBool(skill.perk("condition_weapon_short_sword", 0.0)) != CombatItemUtil.isShortSword(weapon)) {
            return false;
        }
        if (skill.hasPerk("condition_weapon_axe")
                && asBool(skill.perk("condition_weapon_axe", 0.0)) != weapon.isIn(ItemTags.AXES)) {
            return false;
        }
        if (skill.hasPerk("condition_weapon_crossbow")
                && asBool(skill.perk("condition_weapon_crossbow", 0.0)) != weapon.isOf(Items.CROSSBOW)) {
            return false;
        }
        return true;
    }

    private static boolean asBool(double value) {
        return value > 0.0;
    }

    private static boolean isHeadPart(String part) {
        return part != null && (part.contains("head") || part.equals("face") || part.equals("crown"));
    }

    private static double healthRatio(LivingEntity entity) {
        return entity.getMaxHealth() <= 0.0F ? 1.0 : Math.max(0.0, Math.min(1.0, entity.getHealth() / entity.getMaxHealth()));
    }

    private static double vanillaArmor(ServerPlayerEntity player) {
        double armor = 0.0;
        for (EquipmentSlot slot : java.util.List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            armor += EquipmentCombatAttributesRegistry.vanillaArmorValue(player.getEquippedStack(slot), slot);
        }
        return armor;
    }

    private static String injuryType(String detailedPart) {
        return switch (detailedPart == null ? "" : detailedPart) {
            case "face", "neck", "crown", "side_head" -> "head";
            case "shoulder", "arm" -> "arm";
            case "hand" -> "hand";
            case "thigh", "knee", "calf", "foot" -> "legs";
            default -> "torso";
        };
    }

    private record TimedStacks(int stacks, long expiresAt, double multiplierPerStack) {
    }

    private record RapidAttackState(int stacks, long lastHitTick, long lastStackTick) {
    }
}
