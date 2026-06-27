package com.kingdomcomecombat.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Random;

public final class HumanoidCombatAiProfileSpec {
    private final NumberValue minAttackIntervalTicks;
    private final NumberValue attackDesirePerHalfSecond;
    private final NumberValue blockChance;
    private final NumberValue perfectBlockChance;
    private final NumberValue comboLevel;
    private final NumberValue dodgeChance;
    private final NumberValue aiLevel;
    private final NumberValue staminaMax;
    private final NumberValue staminaRegenPerTick;
    private final NumberValue attackAnimationSpeed;
    private final NumberValue attackStartupSlowdown;
    private final NumberValue toughness;
    private final NumberValue wornArmorDurabilityMultiplier;
    private final NumberValue followUpAttackChance;
    private final NumberValue maxFollowUpAttacks;
    private final boolean keepDistance;
    private final boolean requiresWeapon;
    private final NumberValue combatEnterDistance;
    private final NumberValue attackDistance;
    private final NumberValue approachDistance;
    private final NumberValue exhaustedRetreatDistance;

    private HumanoidCombatAiProfileSpec(JsonObject json, HumanoidCombatAiProfile fallback) {
        minAttackIntervalTicks = number(json, "min_attack_interval_ticks", fallback.minAttackIntervalTicks());
        attackDesirePerHalfSecond = number(json, "attack_desire_per_half_second", fallback.attackDesirePerHalfSecond());
        blockChance = number(json, "block_chance", fallback.blockChance());
        perfectBlockChance = number(json, "perfect_block_chance", fallback.perfectBlockChance());
        comboLevel = number(json, "combo_level", fallback.comboLevel());
        dodgeChance = number(json, "dodge_chance", fallback.dodgeChance());
        aiLevel = number(json, "ai_level", fallback.aiLevel());
        staminaMax = number(json, "stamina_max", fallback.staminaMax());
        staminaRegenPerTick = number(json, "stamina_regen_per_tick", fallback.staminaRegenPerTick());
        attackAnimationSpeed = number(json, "attack_animation_speed", fallback.attackAnimationSpeed());
        attackStartupSlowdown = number(json, "attack_startup_slowdown", fallback.attackStartupSlowdown());
        toughness = number(json, "toughness", fallback.toughness());
        wornArmorDurabilityMultiplier = number(json, "worn_armor_durability_multiplier", fallback.wornArmorDurabilityMultiplier());
        followUpAttackChance = number(json, "follow_up_attack_chance", fallback.followUpAttackChance());
        maxFollowUpAttacks = number(json, "max_follow_up_attacks", fallback.maxFollowUpAttacks());
        keepDistance = bool(json, "keep_distance", fallback.keepDistance());
        requiresWeapon = bool(json, "requires_weapon", fallback.requiresWeapon());
        combatEnterDistance = number(json, "combat_enter_distance", fallback.combatEnterDistance());
        attackDistance = number(json, "attack_distance", fallback.attackDistance());
        approachDistance = number(json, "approach_distance", fallback.approachDistance());
        exhaustedRetreatDistance = number(json, "exhausted_retreat_distance", fallback.exhaustedRetreatDistance());
    }

    public static HumanoidCombatAiProfileSpec fromJson(JsonObject json, HumanoidCombatAiProfile fallback) {
        return new HumanoidCombatAiProfileSpec(json, fallback);
    }

    public HumanoidCombatAiProfile sample(Random random) {
        return new HumanoidCombatAiProfile(
                (int) minAttackIntervalTicks.sample(random),
                attackDesirePerHalfSecond.sample(random),
                blockChance.sample(random),
                perfectBlockChance.sample(random),
                (int) comboLevel.sample(random),
                dodgeChance.sample(random),
                (int) aiLevel.sample(random),
                staminaMax.sample(random),
                staminaRegenPerTick.sample(random),
                attackAnimationSpeed.sample(random),
                attackStartupSlowdown.sample(random),
                toughness.sample(random),
                wornArmorDurabilityMultiplier.sample(random),
                followUpAttackChance.sample(random),
                (int) maxFollowUpAttacks.sample(random),
                keepDistance,
                requiresWeapon,
                combatEnterDistance.sample(random),
                attackDistance.sample(random),
                com.kingdomcomecombat.combat.CombatMovementConfig.LOCKED_MIN_TARGET_DISTANCE,
                approachDistance.sample(random),
                exhaustedRetreatDistance.sample(random)
        );
    }

    private static NumberValue number(JsonObject json, String key, double fallback) {
        if (json == null || !json.has(key)) {
            return NumberValue.fixed(fallback);
        }

        JsonElement element = json.get(key);
        if (element.isJsonArray()) {
            JsonArray range = element.getAsJsonArray();
            if (range.size() >= 2) {
                return NumberValue.range(range.get(0).getAsDouble(), range.get(1).getAsDouble());
            }
        }
        return NumberValue.fixed(element.getAsDouble());
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        return json != null && json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    private record NumberValue(double min, double max) {
        static NumberValue fixed(double value) {
            return new NumberValue(value, value);
        }

        static NumberValue range(double first, double second) {
            return first <= second ? new NumberValue(first, second) : new NumberValue(second, first);
        }

        double sample(Random random) {
            return max <= min ? min : min + random.nextDouble() * (max - min);
        }
    }
}
