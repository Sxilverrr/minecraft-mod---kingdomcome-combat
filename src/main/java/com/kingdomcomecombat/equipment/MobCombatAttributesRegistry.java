package com.kingdomcomecombat.equipment;

import com.kingdomcomecombat.stamina.ServerStaminaState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MobCombatAttributesRegistry {
    private static final Map<EntityType<?>, MobCombatAttributes> ATTRIBUTES = new HashMap<>();
    private static final Map<UUID, ArmorDamage> ARMOR_DAMAGE = new HashMap<>();
    private static final Map<UUID, Double> APPLIED_MAX_HEALTH = new HashMap<>();

    private MobCombatAttributesRegistry() {
    }

    public static void clear() {
        ATTRIBUTES.clear();
        ARMOR_DAMAGE.clear();
        APPLIED_MAX_HEALTH.clear();
    }

    public static void register(Identifier entityId, MobCombatAttributes attributes) {
        EntityType<?> entityType = Registries.ENTITY_TYPE.get(entityId);
        if (entityType != null) {
            ATTRIBUTES.put(entityType, attributes);
        }
    }

    public static Optional<MobCombatAttributes> get(LivingEntity entity) {
        if (entity == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(ATTRIBUTES.get(entity.getType()));
    }

    public static void applyConfiguredAttributes(LivingEntity entity) {
        Optional<MobCombatAttributes> attributes = get(entity);
        if (attributes.isEmpty()) {
            return;
        }

        MobCombatAttributes configured = attributes.get();
        if (configured.maxStamina() > 0.0) {
            ServerStaminaState.setEntityMaxStamina(entity.getUuid(), configured.maxStamina());
        }
        if (configured.staminaRegenPerTick() > 0.0) {
            ServerStaminaState.setEntityRegenPerTick(entity.getUuid(), configured.staminaRegenPerTick());
        }

        if (configured.maxHealth() <= 0.0) {
            return;
        }

        EntityAttributeInstance maxHealth = entity.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        double configuredMaxHealth = configured.maxHealth();
        UUID uuid = entity.getUuid();
        double currentMaxHealth = Math.max(1.0, maxHealth.getBaseValue());
        Double previousApplied = APPLIED_MAX_HEALTH.get(uuid);
        if (previousApplied != null && Math.abs(previousApplied - configuredMaxHealth) <= 0.0001) {
            return;
        }

        float ratio = Math.max(0.0F, Math.min(1.0F, entity.getHealth() / (float) currentMaxHealth));
        maxHealth.setBaseValue(configuredMaxHealth);
        entity.setHealth(Math.max(1.0F, (float) (configuredMaxHealth * ratio)));
        APPLIED_MAX_HEALTH.put(uuid, configuredMaxHealth);
    }

    public static int armorDamage(LivingEntity entity, ArmorSection section) {
        ArmorDamage damage = ARMOR_DAMAGE.get(entity.getUuid());
        if (damage == null) {
            return 0;
        }

        return section == ArmorSection.HEAD ? damage.head() : damage.body();
    }

    public static void damageArmor(LivingEntity entity, ArmorSection section, int amount) {
        Optional<MobCombatAttributes> attributes = get(entity);
        if (attributes.isEmpty() || amount <= 0) {
            return;
        }

        MobCombatAttributes.NaturalArmor armor = section == ArmorSection.HEAD
                ? attributes.get().headArmor()
                : attributes.get().bodyArmor();
        if (armor.durability() <= 0) {
            return;
        }

        int maxDurability = armor.durability();
        UUID uuid = entity.getUuid();
        ArmorDamage current = ARMOR_DAMAGE.getOrDefault(uuid, ArmorDamage.EMPTY);
        int head = current.head();
        int body = current.body();
        if (section == ArmorSection.HEAD) {
            head = Math.min(maxDurability, head + amount);
        } else {
            body = Math.min(maxDurability, body + amount);
        }
        ARMOR_DAMAGE.put(uuid, new ArmorDamage(head, body));
    }

    public enum ArmorSection {
        HEAD,
        BODY
    }

    private record ArmorDamage(int head, int body) {
        private static final ArmorDamage EMPTY = new ArmorDamage(0, 0);
    }
}
