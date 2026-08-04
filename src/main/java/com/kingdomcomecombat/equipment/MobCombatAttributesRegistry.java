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
    private static final Map<UUID, AppliedStamina> APPLIED_STAMINA = new HashMap<>();
    private static final Map<UUID, Float> LAST_HEALTH = new HashMap<>();

    private MobCombatAttributesRegistry() {
    }

    public static void clear() {
        ATTRIBUTES.clear();
        ARMOR_DAMAGE.clear();
        APPLIED_MAX_HEALTH.clear();
        APPLIED_STAMINA.clear();
        LAST_HEALTH.clear();
    }

    public static void register(Identifier entityId, MobCombatAttributes attributes) {
        if (!Registries.ENTITY_TYPE.containsId(entityId)) {
            return;
        }
        EntityType<?> entityType = Registries.ENTITY_TYPE.get(entityId);
        ATTRIBUTES.put(entityType, attributes);
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
        recoverNaturalArmorWithHealth(entity, configured);
        UUID uuid = entity.getUuid();
        AppliedStamina appliedStamina = new AppliedStamina(
                configured.maxStamina(), configured.staminaRegenPerTick()
        );
        if (!appliedStamina.equals(APPLIED_STAMINA.get(uuid))) {
            if (configured.maxStamina() > 0.0) {
                ServerStaminaState.setEntityMaxStamina(uuid, configured.maxStamina());
            }
            if (configured.staminaRegenPerTick() > 0.0) {
                ServerStaminaState.setEntityRegenPerTick(uuid, configured.staminaRegenPerTick());
            }
            APPLIED_STAMINA.put(uuid, appliedStamina);
        }

        if (configured.maxHealth() <= 0.0) {
            return;
        }

        EntityAttributeInstance maxHealth = entity.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        double configuredMaxHealth = configured.maxHealth();
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

    private static void recoverNaturalArmorWithHealth(LivingEntity entity, MobCombatAttributes attributes) {
        UUID uuid = entity.getUuid();
        ArmorDamage damage = ARMOR_DAMAGE.get(uuid);
        if (damage == null) return;
        float health = entity.getHealth();
        Float previous = LAST_HEALTH.put(uuid, health);
        if (previous == null || health <= previous || entity.getMaxHealth() <= 0.0F) return;
        double healedFraction = (health - previous) / entity.getMaxHealth();
        int headRepair = (int) Math.ceil(attributes.headArmor().durability() * healedFraction);
        int bodyRepair = (int) Math.ceil(attributes.bodyArmor().durability() * healedFraction);
        int head = Math.max(0, damage.head() - headRepair);
        int body = Math.max(0, damage.body() - bodyRepair);
        if (head == 0 && body == 0) {
            ARMOR_DAMAGE.remove(uuid);
            LAST_HEALTH.remove(uuid);
        }
        else ARMOR_DAMAGE.put(uuid, new ArmorDamage(head, body));
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
        LAST_HEALTH.putIfAbsent(uuid, entity.getHealth());
    }

    public enum ArmorSection {
        HEAD,
        BODY
    }

    private record ArmorDamage(int head, int body) {
        private static final ArmorDamage EMPTY = new ArmorDamage(0, 0);
    }

    private record AppliedStamina(double max, double regenPerTick) {
    }
}
