package com.kingdomcomecombat.api;

import com.kingdomcomecombat.combat.CombatDirection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public final class KingdomComeCombatApi {
    private static final List<ExternalAttackClassifier> EXTERNAL_ATTACK_CLASSIFIERS =
            new CopyOnWriteArrayList<>();

    private KingdomComeCombatApi() {
    }

    public static void registerExternalAttackClassifier(ExternalAttackClassifier classifier) {
        EXTERNAL_ATTACK_CLASSIFIERS.add(classifier);
    }

    public static Optional<ExternalAttack> classifyExternalAttack(
            ServerWorld world,
            LivingEntity target,
            DamageSource source,
            float amount
    ) {
        for (ExternalAttackClassifier classifier : EXTERNAL_ATTACK_CLASSIFIERS) {
            Optional<ExternalAttack> attack = classifier.classify(world, target, source, amount);
            if (attack.isPresent()) {
                return attack;
            }
        }

        return defaultMeleeAttack(target, source);
    }

    private static Optional<ExternalAttack> defaultMeleeAttack(
            LivingEntity target,
            DamageSource source
    ) {
        Entity attacker = source.getAttacker();
        if (!(attacker instanceof LivingEntity livingAttacker) || attacker == target) {
            return Optional.empty();
        }

        if (source.getSource() instanceof ProjectileEntity
                || source.isIn(DamageTypeTags.BYPASSES_ARMOR)
                || source.isIn(DamageTypeTags.IS_FIRE)
                || source.isIn(DamageTypeTags.IS_EXPLOSION)
                || source.isOf(DamageTypes.THORNS)
                || source.isOf(DamageTypes.MAGIC)
                || source.isOf(DamageTypes.INDIRECT_MAGIC)
                || source.isOf(DamageTypes.OUT_OF_WORLD)
                || source.isOf(DamageTypes.IN_WALL)
                || source.isOf(DamageTypes.DROWN)
                || source.isOf(DamageTypes.FALL)) {
            return Optional.empty();
        }

        return Optional.of(new ExternalAttack(livingAttacker, Optional.empty(), true, true));
    }

    @FunctionalInterface
    public interface ExternalAttackClassifier {
        Optional<ExternalAttack> classify(
                ServerWorld world,
                LivingEntity target,
                DamageSource source,
                float amount
        );
    }

    public record ExternalAttack(
            LivingEntity attacker,
            Optional<CombatDirection> blockDirection,
            boolean blockable,
            boolean dodgeable
    ) {
        public ExternalAttack(
                LivingEntity attacker,
                CombatDirection blockDirection,
                boolean blockable,
                boolean dodgeable
        ) {
            this(attacker, Optional.ofNullable(blockDirection), blockable, dodgeable);
        }
    }
}
