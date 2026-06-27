package com.kingdomcomecombat.equipment;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class BloodSplashConfig {
    private static final Map<RegistryKey<DamageType>, Double> TYPE_MULTIPLIERS = new HashMap<>();
    private static final Map<TagKey<DamageType>, Double> TAG_MULTIPLIERS = new HashMap<>();
    private static double defaultMultiplier = 1.0;

    private BloodSplashConfig() {
    }

    public static void clear() {
        TYPE_MULTIPLIERS.clear();
        TAG_MULTIPLIERS.clear();
        defaultMultiplier = 1.0;
    }

    public static void setDefaultMultiplier(double multiplier) {
        defaultMultiplier = sanitize(multiplier);
    }

    public static void putType(RegistryKey<DamageType> type, double multiplier) {
        TYPE_MULTIPLIERS.put(type, sanitize(multiplier));
    }

    public static void putTag(TagKey<DamageType> tag, double multiplier) {
        TAG_MULTIPLIERS.put(tag, sanitize(multiplier));
    }

    public static double multiplier(DamageSource source) {
        if (source == null) {
            return defaultMultiplier;
        }

        double multiplier = defaultMultiplier;
        Double typeMultiplier = source.getTypeRegistryEntry()
                .getKey()
                .map(TYPE_MULTIPLIERS::get)
                .orElse(null);
        if (typeMultiplier != null) {
            multiplier = typeMultiplier;
        }

        for (Map.Entry<TagKey<DamageType>, Double> entry : TAG_MULTIPLIERS.entrySet()) {
            if (source.isIn(entry.getKey())) {
                multiplier *= entry.getValue();
            }
        }
        return sanitize(multiplier);
    }

    public static double multiplier(RegistryKey<DamageType> type) {
        return sanitize(TYPE_MULTIPLIERS.getOrDefault(type, defaultMultiplier));
    }

    public static RegistryKey<DamageType> damageTypeKey(Identifier id) {
        return RegistryKey.of(net.minecraft.registry.RegistryKeys.DAMAGE_TYPE, id);
    }

    private static double sanitize(double multiplier) {
        return Math.max(0.0, Math.min(8.0, multiplier));
    }
}
