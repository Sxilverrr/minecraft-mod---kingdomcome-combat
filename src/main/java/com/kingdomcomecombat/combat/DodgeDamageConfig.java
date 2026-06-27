package com.kingdomcomecombat.combat;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

public class DodgeDamageConfig {
    private static final Set<RegistryKey<DamageType>> DODGEABLE_TYPES = new HashSet<>();
    private static final Set<TagKey<DamageType>> DODGEABLE_TAGS = new HashSet<>();
    private static final Set<RegistryKey<DamageType>> EXCLUDED_TYPES = new HashSet<>();
    private static boolean dodgeDamageWithAttacker = true;

    private DodgeDamageConfig() {
    }

    public static void reset() {
        DODGEABLE_TYPES.clear();
        DODGEABLE_TAGS.clear();
        EXCLUDED_TYPES.clear();
        dodgeDamageWithAttacker = true;

        addDodgeableType("minecraft:mob_attack");
        addDodgeableType("minecraft:mob_attack_no_aggro");
        addDodgeableType("minecraft:player_attack");
        addDodgeableType("minecraft:arrow");
        addDodgeableType("minecraft:trident");
        addDodgeableType("minecraft:thrown");
        addDodgeableType("minecraft:sting");
        addDodgeableType("minecraft:sonic_boom");
        addDodgeableType("minecraft:indirect_magic");
        addDodgeableTag("minecraft:is_projectile");

        addExcludedType("minecraft:fall");
        addExcludedType("minecraft:fly_into_wall");
        addExcludedType("minecraft:in_wall");
        addExcludedType("minecraft:drown");
        addExcludedType("minecraft:starve");
        addExcludedType("minecraft:on_fire");
        addExcludedType("minecraft:in_fire");
        addExcludedType("minecraft:lava");
        addExcludedType("minecraft:hot_floor");
        addExcludedType("minecraft:freeze");
        addExcludedType("minecraft:out_of_world");
        addExcludedType("minecraft:generic");
        addExcludedType("minecraft:magic");
        addExcludedType("minecraft:wither");
        addExcludedType("minecraft:thorns");
    }

    public static void setDodgeDamageWithAttacker(boolean value) {
        dodgeDamageWithAttacker = value;
    }

    public static void addDodgeableType(String id) {
        Identifier identifier = Identifier.tryParse(id);
        if (identifier != null) {
            DODGEABLE_TYPES.add(RegistryKey.of(RegistryKeys.DAMAGE_TYPE, identifier));
        }
    }

    public static void addDodgeableTag(String id) {
        Identifier identifier = Identifier.tryParse(id);
        if (identifier != null) {
            DODGEABLE_TAGS.add(TagKey.of(RegistryKeys.DAMAGE_TYPE, identifier));
        }
    }

    public static void addExcludedType(String id) {
        Identifier identifier = Identifier.tryParse(id);
        if (identifier != null) {
            EXCLUDED_TYPES.add(RegistryKey.of(RegistryKeys.DAMAGE_TYPE, identifier));
        }
    }

    public static boolean isDodgeable(DamageSource source) {
        for (RegistryKey<DamageType> type : EXCLUDED_TYPES) {
            if (source.isOf(type)) {
                return false;
            }
        }

        for (RegistryKey<DamageType> type : DODGEABLE_TYPES) {
            if (source.isOf(type)) {
                return true;
            }
        }

        for (TagKey<DamageType> tag : DODGEABLE_TAGS) {
            if (source.isIn(tag)) {
                return true;
            }
        }

        return dodgeDamageWithAttacker && source.getAttacker() != null;
    }
}
