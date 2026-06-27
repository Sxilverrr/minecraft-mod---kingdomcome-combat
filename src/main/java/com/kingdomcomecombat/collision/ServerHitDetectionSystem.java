package com.kingdomcomecombat.collision;

import com.kingdomcomecombat.combat.ActiveServerAttack;
import com.kingdomcomecombat.api.KingdomComeCombatApi;
import com.kingdomcomecombat.ai.HumanoidCombatAiProfiles;
import com.kingdomcomecombat.ai.HumanoidCombatAiTicker;
import com.kingdomcomecombat.ai.ZombieLeaderUtil;
import com.kingdomcomecombat.combat.AttackMoveConfig;
import com.kingdomcomecombat.combat.AttackMoveConfigs;
import com.kingdomcomecombat.combat.CombatItemUtil;
import com.kingdomcomecombat.combat.CombatControlConfig;
import com.kingdomcomecombat.combat.CombatDirection;
import com.kingdomcomecombat.combat.ComboMoveConfig;
import com.kingdomcomecombat.config.CombatClientConfig;
import com.kingdomcomecombat.config.CombatServerConfig;
import com.kingdomcomecombat.combat.ServerBlockState;
import com.kingdomcomecombat.combat.ServerComboState;
import com.kingdomcomecombat.combat.ServerCombatControlState;
import com.kingdomcomecombat.combat.ServerCombatStanceState;
import com.kingdomcomecombat.combat.ServerCombatState;
import com.kingdomcomecombat.ai.HumanoidCombatAiProfile;
import com.kingdomcomecombat.equipment.ArmorCombatAttributes;
import com.kingdomcomecombat.equipment.BloodiedEquipment;
import com.kingdomcomecombat.equipment.BloodSplashConfig;
import com.kingdomcomecombat.equipment.DamageTypeProfile;
import com.kingdomcomecombat.equipment.EquipmentCombatAttributesRegistry;
import com.kingdomcomecombat.equipment.EquipmentFallbackConfig;
import com.kingdomcomecombat.equipment.MobCombatAttributes;
import com.kingdomcomecombat.equipment.MobCombatAttributesRegistry;
import com.kingdomcomecombat.equipment.ProjectileCombatAttributesRegistry;
import com.kingdomcomecombat.equipment.WeaponCombatAttributes;
import com.kingdomcomecombat.game.ModGameRules;
import com.kingdomcomecombat.injury.ModStatusEffects;
import net.minecraft.enchantment.EnchantmentHelper;
import com.kingdomcomecombat.network.EntityAttackImpactPayload;
import com.kingdomcomecombat.network.EntityAttackInterruptPayload;
import com.kingdomcomecombat.network.EntityBlockAnimationPayload;
import com.kingdomcomecombat.network.EntityHitReactionPayload;
import com.kingdomcomecombat.network.EntitySuppressHurtOverlayPayload;
import com.kingdomcomecombat.network.HitFeedbackPayload;
import com.kingdomcomecombat.network.IncomingAttackWarningPayload;
import com.kingdomcomecombat.passive.PassiveSkillPerks;
import com.kingdomcomecombat.item.HandCannonProjectileTracker;
import com.kingdomcomecombat.particle.ModParticles;
import com.kingdomcomecombat.potion.PotionCoatingHandler;
import com.kingdomcomecombat.sound.ModSounds;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.world.Difficulty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ServerHitDetectionSystem {
    private static final int SLIME_DAMAGE_INTERVAL_TICKS = 10;
    private static final Map<SlimeHitKey, Long> LAST_SLIME_HIT_TICKS = new HashMap<>();
    private static final double ARMOR_DURABILITY_DAMAGE_MULTIPLIER = 0.18;
    private static final double ARMOR_LOW_DURABILITY_THRESHOLD = 0.10;
    private static final Identifier DENSITY_ENCHANTMENT_ID = Identifier.ofVanilla("density");
    private static final Identifier BREACH_ENCHANTMENT_ID = Identifier.ofVanilla("breach");
    private static final Identifier KNOCKBACK_ENCHANTMENT_ID = Identifier.ofVanilla("knockback");
    private static final Identifier SWEEPING_EDGE_ENCHANTMENT_ID = Identifier.ofVanilla("sweeping_edge");
    private static final Identifier SMITE_ENCHANTMENT_ID = Identifier.ofVanilla("smite");
    private static final Identifier BANE_OF_ARTHROPODS_ENCHANTMENT_ID = Identifier.ofVanilla("bane_of_arthropods");
    private static final double NATURAL_ARMOR_MIN_DURABILITY_PANEL_MULTIPLIER = 0.20;
    private static final int WEAPON_DURABILITY_PENETRATING_HIT = 1;
    private static final int WEAPON_DURABILITY_NON_PENETRATING_HIT = 2;
    private static final double BROKEN_WEAPON_EDGE_PANEL_MULTIPLIER = 0.40;
    private static final double EXHAUSTED_TARGET_BONUS_STRIKE_DIVISOR = 8.0;
    private static final int BLOOD_SPARKS_PER_CONTACT = 42;
    private static final int PENDING_CLIENT_HIT_MAX_TICKS = 10;
    private static final int PENDING_CLIENT_HIT_MAX_PER_PLAYER = 12;
    private static final Map<UUID, List<PendingClientHit>> PENDING_CLIENT_HITS = new HashMap<>();
    private static final Map<UUID, Double> PLAYER_WEAPON_DURABILITY_REMAINDERS = new HashMap<>();

    private static final RegistryKey<DamageType> CUSTOM_COMBAT_DAMAGE =
            RegistryKey.of(
                    RegistryKeys.DAMAGE_TYPE,
                    Identifier.of(com.kingdomcomecombat.KingdomComeCombat.MOD_ID, "custom_combat")
            );

    public static void detect(LivingEntity attacker, ActiveServerAttack attack) {
        if (attacker instanceof ServerPlayerEntity) {
            return;
        }

        ServerWorld world = (ServerWorld) attacker.getWorld();

        Optional<AnimatedAttackHitboxLibrary.SampledHitbox> sampledHitbox =
                attack.comboMove != null
                        ? AnimatedAttackHitboxLibrary.sampleNamedSeconds(
                                attack.comboMove.animationName(),
                                attack.getAnimationElapsedSeconds(),
                                attack.comboMove.useRealHitbox(),
                                realHitboxSizeUnits(attacker.getMainHandStack()),
                                EquipmentCombatAttributesRegistry.realHitboxOffsetUnits(
                                        attacker.getMainHandStack(),
                                        AnimatedAttackHitboxLibrary.getRealHitboxOffsetUnits()
                                ),
                                EquipmentCombatAttributesRegistry.realHitboxRotationDegrees(
                                        attacker.getMainHandStack(),
                                        AnimatedAttackHitboxLibrary.getRealHitboxRotationDegrees()
                                )
                        )
                        : sampleMoveHitbox(attacker, attack);

        if (sampledHitbox.isEmpty()) {
            return;
        }

        AnimatedAttackHitboxLibrary.OrientedBox hitBox =
                sampledHitbox.get().toWorldBox(attacker);
        Vec3d hitboxMotion = sampledHitboxMotion(attacker, attack, sampledHitbox.get());

        List<LivingEntity> candidates = world.getEntitiesByClass(
                LivingEntity.class,
                hitBox.candidateBox(),
                entity -> canHit(attacker, entity, attack)
        );

        for (LivingEntity target : candidates) {
            Optional<HumanoidHurtboxLibrary.HitResult> hitResult =
                    getTargetHitResult(hitBox, target);
            if (hitResult.isEmpty()) {
                continue;
            }

            handleConfirmedHit(world, attacker, target, hitResult.get(), attack, hitboxMotion);
        }
    }

    private static Optional<AnimatedAttackHitboxLibrary.SampledHitbox> sampleMoveHitbox(
            LivingEntity attacker,
            ActiveServerAttack attack
    ) {
        AttackMoveConfig moveConfig = attack.moveConfig();
        if (moveConfig.animationName().isBlank()) {
            return AnimatedAttackHitboxLibrary.sampleSeconds(
                    attack.direction,
                    attack.getAnimationElapsedSeconds(),
                    moveConfig.useRealHitbox()
            );
        }

        return AnimatedAttackHitboxLibrary.sampleNamedSeconds(
                moveConfig.animationName(),
                attack.getAnimationElapsedSeconds(),
                moveConfig.useRealHitbox(),
                realHitboxSizeUnits(attacker.getMainHandStack()),
                EquipmentCombatAttributesRegistry.realHitboxOffsetUnits(
                        attacker.getMainHandStack(),
                        AnimatedAttackHitboxLibrary.getRealHitboxOffsetUnits()
                ),
                EquipmentCombatAttributesRegistry.realHitboxRotationDegrees(
                        attacker.getMainHandStack(),
                        AnimatedAttackHitboxLibrary.getRealHitboxRotationDegrees()
                )
        );
    }

    public static void handleClientReportedHit(
            ServerPlayerEntity reporter,
            int attackerEntityId,
            int targetEntityId,
            int partOrdinal,
            Vec3d hitPosition,
            boolean extraHeadHit,
            long attackInstanceId
    ) {
        handleClientReportedHit(
                reporter,
                attackerEntityId,
                targetEntityId,
                partOrdinal,
                hitPosition,
                extraHeadHit,
                attackInstanceId,
                true
        );
    }

    private static void handleClientReportedHit(
            ServerPlayerEntity reporter,
            int attackerEntityId,
            int targetEntityId,
            int partOrdinal,
            Vec3d hitPosition,
            boolean extraHeadHit,
            long attackInstanceId,
            boolean allowPending
    ) {
        Entity attackerEntity = reporter.getWorld().getEntityById(attackerEntityId);
        if (!(attackerEntity instanceof LivingEntity attacker)) {
            return;
        }

        if (attacker instanceof ServerPlayerEntity && attacker != reporter) {
            return;
        }

        ActiveServerAttack attack = ServerCombatState.getAttack(attacker.getUuid());
        if (attack == null) {
            if (allowPending && attacker == reporter && attackInstanceId > 0L) {
                queuePendingClientHit(
                        reporter,
                        targetEntityId,
                        partOrdinal,
                        hitPosition,
                        extraHeadHit,
                        attackInstanceId
                );
            }
            return;
        }

        if (attacker instanceof ServerPlayerEntity && attack.clientAttackInstanceId != attackInstanceId) {
            if (allowPending && attacker == reporter && attackInstanceId > 0L) {
                queuePendingClientHit(
                        reporter,
                        targetEntityId,
                        partOrdinal,
                        hitPosition,
                        extraHeadHit,
                        attackInstanceId
                );
            }
            return;
        }

        Entity entity = reporter.getWorld().getEntityById(targetEntityId);
        if (!(entity instanceof LivingEntity target)) {
            return;
        }

        if (!(attacker instanceof ServerPlayerEntity) && !isReporterAllowedToReportMobHit(reporter, attacker, target)) {
            return;
        }

        if (!canHit(attacker, target, attack)) {
            return;
        }

        if (!isReasonableClientHit(attacker, target, attack)) {
            return;
        }

        HumanoidHurtboxLibrary.Part part = partFromOrdinal(partOrdinal);
        HumanoidHurtboxLibrary.HitResult hitResult =
                HumanoidHurtboxLibrary.reportedHitResult(target, part, hitPosition);
        handleConfirmedHit(
                (ServerWorld) attacker.getWorld(),
                attacker,
                target,
                hitResult,
                attack,
                extraHeadHit,
                currentHitboxMotion(attacker, attack)
        );
    }

    public static void flushPendingClientReportedHits(
            ServerPlayerEntity reporter,
            long attackInstanceId
    ) {
        if (attackInstanceId <= 0L) {
            return;
        }

        UUID uuid = reporter.getUuid();
        List<PendingClientHit> pendingHits = PENDING_CLIENT_HITS.get(uuid);
        if (pendingHits == null || pendingHits.isEmpty()) {
            return;
        }

        long now = reporter.getWorld().getTime();
        List<PendingClientHit> matching = new ArrayList<>();
        pendingHits.removeIf(hit -> {
            boolean expired = now - hit.receivedWorldTick() > PENDING_CLIENT_HIT_MAX_TICKS;
            boolean matched = hit.attackInstanceId() == attackInstanceId;
            if (matched && !expired) {
                matching.add(hit);
            }
            return expired || matched;
        });
        if (pendingHits.isEmpty()) {
            PENDING_CLIENT_HITS.remove(uuid);
        }

        for (PendingClientHit hit : matching) {
            handleClientReportedHit(
                    reporter,
                    reporter.getId(),
                    hit.targetEntityId(),
                    hit.partOrdinal(),
                    hit.hitPosition(),
                    hit.extraHeadHit(),
                    hit.attackInstanceId(),
                    false
            );
        }
    }

    private static void queuePendingClientHit(
            ServerPlayerEntity reporter,
            int targetEntityId,
            int partOrdinal,
            Vec3d hitPosition,
            boolean extraHeadHit,
            long attackInstanceId
    ) {
        UUID uuid = reporter.getUuid();
        long now = reporter.getWorld().getTime();
        List<PendingClientHit> pendingHits =
                PENDING_CLIENT_HITS.computeIfAbsent(uuid, ignored -> new ArrayList<>());
        pendingHits.removeIf(hit -> now - hit.receivedWorldTick() > PENDING_CLIENT_HIT_MAX_TICKS);
        if (pendingHits.size() >= PENDING_CLIENT_HIT_MAX_PER_PLAYER) {
            pendingHits.removeFirst();
        }
        pendingHits.add(new PendingClientHit(
                targetEntityId,
                partOrdinal,
                hitPosition,
                extraHeadHit,
                attackInstanceId,
                now
        ));
    }

    public static boolean tryHandleExternalAttackDefense(
            ServerWorld world,
            LivingEntity target,
            DamageSource source,
            float amount
    ) {
        Optional<KingdomComeCombatApi.ExternalAttack> classified =
                KingdomComeCombatApi.classifyExternalAttack(world, target, source, amount);
        if (classified.isEmpty()) {
            return false;
        }

        KingdomComeCombatApi.ExternalAttack externalAttack = classified.get();
        LivingEntity attacker = externalAttack.attacker();
        CombatDirection blockDirection = externalAttack.blockDirection()
                .orElseGet(() -> inferIncomingBlockDirection(attacker, target));

        if (externalAttack.dodgeable()
                && ServerCombatControlState.dodgesAttack(target, blockDirection)) {
            return true;
        }

        if (!externalAttack.blockable()) {
            return false;
        }

        BlockResult blockResult = tryBlockExternal(world, attacker, target, blockDirection, amount);
        if (!blockResult.blocked()) {
            return false;
        }

        syncBlockAnimation(world, target, attacker, blockResult);
        syncBlockImpact(world, attacker, target);
        Vec3d position = target.getPos().add(0.0, target.getHeight() * 0.65, 0.0);
        spawnTemporaryBlockParticles(world, position, blockResult.perfect());
        playBlockSound(world, target, blockResult);
        if (blockResult.perfect()) {
            pushAway(attacker, target, 0.32);
            ServerCombatControlState.disableAttack(
                    attacker.getUuid(),
                    CombatControlConfig.PERFECT_BLOCK_ATTACK_DISABLE_TICKS
            );
            ServerCombatControlState.startPerfectCounterWindow(target.getUuid());
            ServerCombatStanceState.set(target.getUuid(), blockResult.direction());
            ServerCombatStanceState.set(target.getUuid(), CombatDirection.afterPerfectBlock(blockResult.direction()));
        } else {
            pushAway(target, attacker, 0.22);
            ServerCombatControlState.disableAttack(
                    target.getUuid(),
                    CombatControlConfig.UNPERFECT_BLOCK_ATTACK_DISABLE_TICKS
            );
        }
        return true;
    }

    public static boolean tryHandleVanillaMeleePanelAttack(
            ServerWorld world,
            LivingEntity target,
            DamageSource source,
            float amount
    ) {
        if (amount <= 0.0F
                || !(source.getAttacker() instanceof PlayerEntity attacker)
                || source.getSource() instanceof ProjectileEntity
                || attacker == target) {
            return false;
        }

        Optional<KingdomComeCombatApi.ExternalAttack> classified =
                KingdomComeCombatApi.classifyExternalAttack(world, target, source, amount);
        if (classified.isEmpty() || classified.get().attacker() != attacker) {
            return false;
        }

        KingdomComeCombatApi.ExternalAttack externalAttack = classified.get();
        CombatDirection blockDirection = externalAttack.blockDirection()
                .orElseGet(() -> inferIncomingBlockDirection(attacker, target));
        if (externalAttack.dodgeable()
                && ServerCombatControlState.dodgesAttack(target, blockDirection)) {
            return true;
        }

        boolean heavyHammer = CombatItemUtil.isFightingMace(attacker.getMainHandStack());
        if (!heavyHammer && externalAttack.blockable()) {
            BlockResult blockResult = tryBlockExternal(world, attacker, target, blockDirection, amount);
            if (blockResult.blocked()) {
                syncBlockAnimation(world, target, attacker, blockResult);
                syncBlockImpact(world, attacker, target);
                Vec3d position = target.getPos().add(0.0, target.getHeight() * 0.65, 0.0);
                spawnTemporaryBlockParticles(world, position, blockResult.perfect());
                playBlockSound(world, target, blockResult);
                if (blockResult.perfect()) {
                    pushAway(attacker, target, 0.32);
                    ServerCombatControlState.disableAttack(
                            attacker.getUuid(),
                            CombatControlConfig.PERFECT_BLOCK_ATTACK_DISABLE_TICKS
                    );
                    ServerCombatControlState.startPerfectCounterWindow(target.getUuid());
                    ServerCombatStanceState.set(target.getUuid(), blockResult.direction());
                    ServerCombatStanceState.set(target.getUuid(), CombatDirection.afterPerfectBlock(blockResult.direction()));
                } else {
                    pushAway(target, attacker, 0.22);
                    ServerCombatControlState.disableAttack(
                            target.getUuid(),
                            CombatControlConfig.UNPERFECT_BLOCK_ATTACK_DISABLE_TICKS
                    );
                }
                return true;
            }
        }

        return damageVanillaMeleePanelAttack(world, attacker, target, amount);
    }

    public static boolean tryHandleConfiguredMobAttack(
            ServerWorld world,
            LivingEntity target,
            DamageSource source,
            float amount
    ) {
        Entity sourceAttacker = source.getAttacker();
        if (!(sourceAttacker instanceof LivingEntity attacker)
                || attacker instanceof PlayerEntity
                || attacker == target) {
            return false;
        }

        if (attacker instanceof SlimeEntity
                && !canSlimeDamageTarget(world, attacker, target)) {
            return true;
        }

        Optional<MobCombatAttributes> configured = MobCombatAttributesRegistry.get(attacker);
        if (configured.isEmpty() && !(attacker instanceof MobEntity)) {
            return false;
        }

        Optional<KingdomComeCombatApi.ExternalAttack> classified =
                KingdomComeCombatApi.classifyExternalAttack(world, target, source, amount);
        if (classified.isEmpty()) {
            return false;
        }

        KingdomComeCombatApi.ExternalAttack externalAttack = classified.get();
        CombatDirection blockDirection = externalAttack.blockDirection()
                .orElseGet(() -> inferIncomingBlockDirection(attacker, target));

        MobCombatAttributes.MeleeDefenseTier defenseTier = configured
                .map(MobCombatAttributes::meleeDefenseTier)
                .orElse(MobCombatAttributes.MeleeDefenseTier.BLOCKABLE);
        if (defenseTier == MobCombatAttributes.MeleeDefenseTier.DODGEABLE
                && externalAttack.dodgeable()
                && ServerCombatControlState.dodgesAttack(target, blockDirection)) {
            return true;
        }

        if (externalAttack.blockable()
                && defenseTier != MobCombatAttributes.MeleeDefenseTier.DODGEABLE) {
            BlockResult blockResult = tryBlockExternal(
                    world,
                    attacker,
                    target,
                    blockDirection,
                    amount,
                    defenseTier == MobCombatAttributes.MeleeDefenseTier.SHIELD_BLOCKABLE
            );
            if (blockResult.blocked()) {
                syncBlockAnimation(world, target, attacker, blockResult);
                syncBlockImpact(world, attacker, target);
                Vec3d position = target.getPos().add(0.0, target.getHeight() * 0.65, 0.0);
                spawnTemporaryBlockParticles(world, position, blockResult.perfect());
                playBlockSound(world, target, blockResult);
                if (blockResult.perfect()) {
                    pushAway(attacker, target, 0.32);
                    ServerCombatControlState.disableAttack(
                            attacker.getUuid(),
                            CombatControlConfig.PERFECT_BLOCK_ATTACK_DISABLE_TICKS
                    );
                    ServerCombatControlState.startPerfectCounterWindow(target.getUuid());
                    ServerCombatStanceState.set(target.getUuid(), blockResult.direction());
                    ServerCombatStanceState.set(target.getUuid(), CombatDirection.afterPerfectBlock(blockResult.direction()));
                } else {
                    pushAway(target, attacker, 0.22);
                    ServerCombatControlState.disableAttack(
                            target.getUuid(),
                            CombatControlConfig.UNPERFECT_BLOCK_ATTACK_DISABLE_TICKS
                    );
                }
                return true;
            }
        }

        return configured.isPresent()
                ? damageConfiguredMobAttack(world, attacker, target, amount)
                : damageDefaultMobMeleePanelAttack(world, attacker, target, amount);
    }

    private static boolean canSlimeDamageTarget(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target
    ) {
        long now = world.getTime();
        SlimeHitKey key = new SlimeHitKey(attacker.getUuid(), target.getUuid());
        Long previous = LAST_SLIME_HIT_TICKS.get(key);
        if ((now & 255L) == 0L) {
            LAST_SLIME_HIT_TICKS.entrySet().removeIf(entry -> now - entry.getValue() > 200L);
        }
        if (previous != null && now - previous < SLIME_DAMAGE_INTERVAL_TICKS) {
            return false;
        }
        LAST_SLIME_HIT_TICKS.put(key, now);
        return true;
    }

    public static boolean tryHandleProjectileShieldDefense(
            ServerWorld world,
            LivingEntity target,
            ProjectileEntity projectile,
            float amount
    ) {
        ItemStack shield = target.getOffHandStack();
        if (!EquipmentCombatAttributesRegistry.isShield(shield)
                || !ServerCombatControlState.canBlock(target)
                || ServerCombatState.getAttack(target.getUuid()) != null
                || !isProjectileIncomingFromFront(projectile, target)) {
            return false;
        }

        boolean activeLargeShield = EquipmentCombatAttributesRegistry.isLargeShield(shield)
                && ServerCombatStanceState.isLocked(target.getUuid())
                && ServerCombatStanceState.canUseLargeShield(target.getUuid());
        boolean mobArrowBlock = false;
        if (!activeLargeShield && target instanceof MobEntity) {
            HumanoidCombatAiProfile profile = HumanoidCombatAiProfiles.getProfile(target);
            double baseChance = profile == null
                    ? HumanoidCombatAiProfiles.defaultHumanoid().blockChance()
                    : profile.blockChance();
            double chance = Math.min(1.0, baseChance * 2.0);
            mobArrowBlock = target.getRandom().nextDouble() <= chance;
        }
        if (!activeLargeShield && !mobArrowBlock) {
            return false;
        }

        double mitigation = EquipmentCombatAttributesRegistry.getShield(
                shield
        ).blockImpactMitigation();
        double cost = Math.max(3.0, amount * 3.5) * (1.0 - mitigation);
        cost *= EquipmentCombatAttributesRegistry.armorStaminaCostMultiplier(target);
        boolean consumed = com.kingdomcomecombat.stamina.ServerStaminaState.consume(target, cost);
        damageShieldDurability(target, new TypeDamage(0.0, amount, 0.0), false);
        if (!consumed) {
            ServerCombatStanceState.disableLargeShield(
                    target.getUuid(),
                    CombatControlConfig.LARGE_SHIELD_EXHAUSTED_DISABLE_TICKS
            );
        }

        CombatDirection direction = inferProjectileBlockDirection(projectile, target);
        BlockResult result = BlockResult.unperfect(direction);
        Entity owner = projectile.getOwner();
        if (owner instanceof LivingEntity attacker) {
            syncBlockImpact(world, attacker, target);
        }
        syncBlockAnimation(world, target, owner instanceof LivingEntity attacker ? attacker : target, result);
        Vec3d position = target.getPos().add(0.0, target.getHeight() * 0.65, 0.0);
        spawnTemporaryBlockParticles(world, position, false);
        playBlockSound(world, target, result);
        // A blocked projectile must be terminal. Reversing it and assigning the
        // blocker as its owner lets it collide again and ricochet repeatedly.
        projectile.discard();
        return true;
    }

    private static boolean isReasonableClientHit(
            LivingEntity attacker,
            LivingEntity target,
            ActiveServerAttack attack
    ) {
        if (attacker.squaredDistanceTo(target) > 6.5 * 6.5) {
            return false;
        }

        return true;
    }

    private static boolean isReporterAllowedToReportMobHit(
            ServerPlayerEntity reporter,
            LivingEntity attacker,
            LivingEntity target
    ) {
        if (!(attacker instanceof MobEntity mob)) {
            return false;
        }

        if (target != mob.getTarget()) {
            return false;
        }

        return reporter.squaredDistanceTo(attacker) <= 16.0 * 16.0
                || reporter.squaredDistanceTo(target) <= 16.0 * 16.0;
    }

    private static HumanoidHurtboxLibrary.Part partFromOrdinal(int ordinal) {
        HumanoidHurtboxLibrary.Part[] parts = HumanoidHurtboxLibrary.Part.values();
        if (ordinal < 0 || ordinal >= parts.length) {
            return HumanoidHurtboxLibrary.Part.BODY;
        }

        return parts[ordinal];
    }

    private static void handleConfirmedHit(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
        HumanoidHurtboxLibrary.HitResult hitResult,
        ActiveServerAttack attack
    ) {
        handleConfirmedHit(world, attacker, target, hitResult, attack, null);
    }

    private static void handleConfirmedHit(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            HumanoidHurtboxLibrary.HitResult hitResult,
            ActiveServerAttack attack,
            Vec3d hitboxMotion
    ) {
        handleConfirmedHit(world, attacker, target, hitResult, attack, false, hitboxMotion);
    }

    public static void performConfiguredDirectHit(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            ActiveServerAttack attack
    ) {
        if (target == null || !target.isAlive() || target == attacker) {
            return;
        }

        if (attack.hitTargets.contains(target.getUuid())) {
            return;
        }

        String heightDetailedPart = directHitHeightDetailedPart(attacker, target, attack);
        HumanoidHurtboxLibrary.Part part = heightDetailedPart.isBlank()
                ? directHitPartFor(attack)
                : reactionPartForDetailedPart(HumanoidHurtboxLibrary.Part.BODY, heightDetailedPart);
        HumanoidHurtboxLibrary.HitResult hitResult =
                HumanoidHurtboxLibrary.reportedHitResult(
                        target,
                        part,
                        directHitParticlePosition(attacker, target)
                );
        attack.hitTargets.add(target.getUuid());
        if (MobCombatAttributesRegistry.get(attacker).isPresent()) {
            float configuredAmount = Math.max(
                    1.0F,
                    (float) attackDamageOrFallback(attacker, 1.0F)
            );
            if (damageConfiguredMobAttack(world, attacker, target, configuredAmount, heightDetailedPart)) {
                attack.connectedOrNormalBlocked = true;
                return;
            }
        }
        List<AttackMoveConfig.HitZoneRule> overrideHitZoneRules = heightDetailedPart.isBlank()
                ? null
                : List.of(new AttackMoveConfig.HitZoneRule(
                        hitZoneFromPart(part),
                        List.of(heightDetailedPart)
                ));
        damageBlockedTarget(
                world,
                attacker,
                target,
                hitResult,
                attack,
                1.0F,
                true,
                overrideHitZoneRules,
                null
        );
        syncAttackImpact(world, attacker);
    }

    public static void performComboChainHit(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            ActiveServerAttack attack,
            ComboMoveConfig.AttackChainEvent event
    ) {
        if (target == null || !target.isAlive() || target == attacker) {
            return;
        }

        HumanoidHurtboxLibrary.Part part = directHitPartFor(event.hitZoneRules());
        HumanoidHurtboxLibrary.HitResult hitResult =
                HumanoidHurtboxLibrary.reportedHitResult(
                        target,
                        part,
                        directHitParticlePosition(attacker, target)
                );
        damageBlockedTarget(
                world,
                attacker,
                target,
                hitResult,
                attack,
                1.0F,
                true,
                event.hitZoneRules(),
                event.damageModifiers(),
                true
        );
    }

    private static Vec3d directHitParticlePosition(LivingEntity attacker, LivingEntity target) {
        Vec3d towardAttacker = attacker.getPos().subtract(target.getPos());
        Vec3d horizontal = new Vec3d(towardAttacker.x, 0.0, towardAttacker.z);
        if (horizontal.lengthSquared() <= 0.000001) {
            horizontal = Vec3d.fromPolar(0.0F, target.getYaw()).multiply(-1.0);
        }

        Vec3d surfaceOffset = horizontal.normalize().multiply(Math.max(0.18, target.getWidth() * 0.55));
        return target.getPos()
                .add(surfaceOffset)
                .add(0.0, target.getHeight() * 0.76, 0.0);
    }

    private static boolean damageConfiguredMobAttack(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            float vanillaAmount
    ) {
        return damageConfiguredMobAttack(world, attacker, target, vanillaAmount, "");
    }

    private static boolean damageConfiguredMobAttack(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            float vanillaAmount,
            String forcedDetailedPart
    ) {
        Optional<MobCombatAttributes> configured = MobCombatAttributesRegistry.get(attacker);
        if (configured.isEmpty() || vanillaAmount <= 0.0F) {
            return false;
        }

        MobCombatAttributes attributes = configured.get();
        DamageTypeProfile modifiers = attributes.meleeDamageModifiers();
        if (modifiers.total() <= 0.000001) {
            return false;
        }

        String detailedPart = forcedDetailedPart != null && !forcedDetailedPart.isBlank()
                ? forcedDetailedPart
                : configuredMobDetailedPartFor(attacker, target, attributes.meleeHitZoneRules());

        TypeDamage raw = new TypeDamage(
                vanillaAmount * modifiers.thrust(),
                vanillaAmount * modifiers.strike(),
                vanillaAmount * modifiers.slash()
        );
        double rawTotal = raw.thrust() + raw.strike() + raw.slash();
        double penetrationBaseDamage = configuredMobPenetrationBaseDamage(attacker, vanillaAmount);
        TypeDamage penetration = new TypeDamage(
                Math.ceil(penetrationBaseDamage * modifiers.thrust() * 5.0),
                Math.ceil(penetrationBaseDamage * modifiers.strike() * 5.0),
                Math.ceil(penetrationBaseDamage * modifiers.slash() * 5.0)
        );
        ArmorResult armorResult = applyArmorReductionAndDurability(
                attacker,
                target,
                List.of(detailedPart),
                raw,
                rawTotal,
                penetration
        );

        float damage = (float) (armorResult.damage()
                * EquipmentFallbackConfig.partDamageMultiplier(detailedPart)
                * injuryDamageMultiplier(target, detailedPart));
        if (damage <= 0.0F) {
            applyConfiguredMobKnockback(attacker, target, attributes, armorResult.penetrated());
            spawnTemporaryBlockParticles(world, target.getPos().add(0.0, target.getHeight() * 0.55, 0.0), false);
            playConfiguredMobArmorSound(world, target);
            applyConfiguredMobImpactStaminaDamage(target, attributes, armorResult);
            return true;
        }

        float finalDamage = applyMobDifficultyScaling(world, attacker, target, damage);
        if (finalDamage <= 0.0F) {
            applyConfiguredMobKnockback(attacker, target, attributes, false);
            spawnTemporaryBlockParticles(world, target.getPos().add(0.0, target.getHeight() * 0.55, 0.0), false);
            playConfiguredMobArmorSound(world, target);
            applyConfiguredMobImpactStaminaDamage(target, attributes, armorResult);
            return true;
        }

        target.timeUntilRegen = 0;
        target.hurtTime = 0;
        DamageSource damageSource = createDamageSource(world, attacker);
        boolean damaged = target.damage(world, damageSource, finalDamage);
        if (!damaged) {
            playConfiguredMobArmorSound(world, target);
            return true;
        }
        if (!target.isAlive()) {
            PassiveSkillPerks.afterKill(attacker);
        }

        applyConfiguredMobKnockback(attacker, target, attributes, armorResult.penetrated());
        syncSuppressHurtOverlay(world, target);
        applyConfiguredMobImpactStaminaDamage(target, attributes, armorResult);
        maybeApplyDamageInjury(target, detailedPart, finalDamage);
        if (armorResult.reduction() > 0.08) {
            playConfiguredMobArmorSound(world, target);
        } else {
            playConfiguredMobFleshSound(world, target, attacker.getMainHandStack());
        }
        return true;
    }

    private static boolean damageDefaultMobMeleePanelAttack(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            float vanillaAmount
    ) {
        if (vanillaAmount <= 0.0F) {
            return false;
        }

        String detailedPart = "chest";
        TypeDamage raw = new TypeDamage(0.0, vanillaAmount, 0.0);
        TypeDamage penetration = new TypeDamage(0.0, Math.ceil(vanillaAmount * 5.0), 0.0);
        ArmorResult armorResult = applyArmorReductionAndDurability(
                attacker,
                target,
                List.of(detailedPart),
                raw,
                vanillaAmount,
                penetration
        );

        float damage = (float) (armorResult.damage()
                * EquipmentFallbackConfig.partDamageMultiplier(detailedPart)
                * injuryDamageMultiplier(target, detailedPart));
        if (damage <= 0.0F) {
            pushAway(target, attacker, 0.18);
            spawnTemporaryBlockParticles(world, target.getPos().add(0.0, target.getHeight() * 0.55, 0.0), false);
            playConfiguredMobArmorSound(world, target);
            return true;
        }

        target.timeUntilRegen = 0;
        target.hurtTime = 0;
        boolean damaged = target.damage(world, createDamageSource(world, attacker), damage);
        if (!damaged) {
            playConfiguredMobArmorSound(world, target);
            return true;
        }

        pushAway(target, attacker, armorResult.penetrated() ? 0.32 : 0.22);
        syncSuppressHurtOverlay(world, target);
        maybeApplyDamageInjury(target, detailedPart, damage);
        if (armorResult.reduction() > 0.08) {
            playConfiguredMobArmorSound(world, target);
        } else {
            playConfiguredMobFleshSound(world, target);
        }
        return true;
    }

    private static double configuredMobPenetrationBaseDamage(
            LivingEntity attacker,
            float fallbackAmount
    ) {
        double attackDamage = attackDamageOrFallback(attacker, fallbackAmount);
        if (attackDamage > 0.000001) {
            return attackDamage;
        }

        return Math.max(0.0F, fallbackAmount);
    }

    private static double attackDamageOrFallback(LivingEntity attacker, double fallback) {
        var attackDamage = attacker.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
        return attackDamage == null ? Math.max(0.0, fallback) : attackDamage.getValue();
    }

    private static String configuredMobDetailedPartFor(
            LivingEntity attacker,
            LivingEntity target,
            List<AttackMoveConfig.HitZoneRule> rules
    ) {
        String preferred = chestRayDetailedPart(attacker, target);
        List<String> candidates = configuredDetailedPartsFor(
                fallbackPartForDetailedPart(preferred),
                rules
        );
        if (!candidates.isEmpty()) {
            return bestDetailedPart(preferred, candidates);
        }

        return preferred.isBlank() ? "chest" : preferred;
    }

    private static String bestDetailedPart(String preferred, List<String> candidates) {
        if (candidates.contains(preferred)) {
            return preferred;
        }

        for (String related : relatedDetailedParts(preferred)) {
            if (candidates.contains(related)) {
                return related;
            }
        }

        return candidates.getFirst();
    }

    private static List<String> relatedDetailedParts(String preferred) {
        return switch (preferred) {
            case "face" -> List.of("face", "side_head", "crown", "neck");
            case "crown" -> List.of("crown", "side_head", "face", "neck");
            case "side_head" -> List.of("side_head", "face", "crown", "neck");
            case "neck" -> List.of("neck", "face", "side_head", "shoulder", "chest");
            case "shoulder" -> List.of("shoulder", "arm", "chest", "hand");
            case "arm" -> List.of("arm", "hand", "shoulder", "chest");
            case "hand" -> List.of("hand", "arm", "shoulder");
            case "abdomen" -> List.of("abdomen", "chest", "thigh");
            case "thigh" -> List.of("thigh", "knee", "abdomen", "calf");
            case "knee" -> List.of("knee", "thigh", "calf", "foot");
            case "calf" -> List.of("calf", "knee", "foot", "thigh");
            case "foot" -> List.of("foot", "calf", "knee");
            default -> List.of("chest", "abdomen", "shoulder");
        };
    }

    private static String chestRayDetailedPart(LivingEntity attacker, LivingEntity target) {
        double targetHeight = Math.max(0.1, target.getHeight());
        Vec3d start = attacker.getPos().add(0.0, attacker.getHeight() * 0.58, 0.0);
        Vec3d end = new Vec3d(target.getX(), start.y, target.getZ());
        Box targetBox = target.getBoundingBox().expand(0.02);
        Vec3d hit = targetBox.raycast(start, end).orElseGet(() -> {
            Vec3d direction = end.subtract(start);
            double lengthSquared = direction.lengthSquared();
            if (lengthSquared <= 0.000001) {
                return end;
            }

            double t = target.getPos().subtract(start).dotProduct(direction) / lengthSquared;
            t = Math.max(0.0, Math.min(1.0, t));
            return start.add(direction.multiply(t));
        });
        double relativeY = Math.max(0.0, Math.min(1.0, (hit.y - target.getY()) / targetHeight));

        if (relativeY >= 0.78) {
            return "crown";
        }
        if (relativeY >= 0.34) {
            return "chest";
        }
        if (relativeY >= 0.10) {
            return "thigh";
        }
        return "foot";
    }

    private static boolean damageVanillaMeleePanelAttack(
            ServerWorld world,
            PlayerEntity attacker,
            LivingEntity target,
            float amount
    ) {
        String detailedPart = "chest";
        ItemStack weapon = attacker.getMainHandStack();
        TypeDamage raw;
        TypeDamage penetration;
        boolean heavyHammer = CombatItemUtil.isFightingMace(weapon);
        if (heavyHammer) {
            double hammerDamage = heavyHammerVanillaStrikeDamage(attacker, amount);
            raw = new TypeDamage(0.0, hammerDamage, 0.0);
            penetration = new TypeDamage(0.0, Math.ceil(hammerDamage * 5.0), 0.0);
        } else {
            DamageTypeProfile profile = vanillaMeleeDamageProfile(weapon);
            raw = new TypeDamage(
                    amount * profile.thrust(),
                    amount * profile.strike(),
                    amount * profile.slash()
            );
            penetration = new TypeDamage(
                    Math.ceil(raw.thrust() * 5.0),
                    Math.ceil(raw.strike() * 5.0),
                    Math.ceil(raw.slash() * 5.0)
            );
        }
        double rawTotal = raw.thrust() + raw.strike() + raw.slash();
        if (rawTotal <= 0.000001) {
            return true;
        }

        ArmorResult armorResult = applyArmorReductionAndDurability(
                attacker,
                target,
                List.of(detailedPart),
                raw,
                rawTotal,
                penetration
        );

        float finalDamage = (float) (armorResult.damage()
                * EquipmentFallbackConfig.partDamageMultiplier(detailedPart)
                * injuryDamageMultiplier(target, detailedPart));
        if (finalDamage <= 0.0F) {
            pushAway(target, attacker, 0.18);
            spawnTemporaryBlockParticles(
                    world,
                    directHitParticlePosition(attacker, target),
                    false
            );
            playConfiguredMobArmorSound(world, target);
            syncAttackImpact(world, attacker);
            return true;
        }

        target.timeUntilRegen = 0;
        target.hurtTime = 0;
        DamageSource damageSource = createDamageSource(world, attacker);
        boolean damaged = target.damage(world, damageSource, finalDamage);
        if (!damaged) {
            playConfiguredMobArmorSound(world, target);
            return true;
        }

        interruptRangedUse(target);
        if (heavyHammer) {
            applyHeavyHammerVanillaPostDamageEffects(world, attacker, target, finalDamage, weapon);
        } else {
            applyPostDamageEffects(world, attacker, target, finalDamage);
        }
        if (!target.isAlive()) {
            PassiveSkillPerks.afterKill(attacker);
        }
        notifyReceivedAttack(world, attacker, target);
        syncSuppressHurtOverlay(world, target);
        pushAway(target, attacker, armorResult.penetrated() ? 0.32 : 0.22);
        maybeApplyDamageInjury(target, detailedPart, finalDamage);
        if (target instanceof MobEntity) {
            HumanoidCombatAiTicker.recordDefensivePressure(target);
        }
        bloodAttackerHeldItems(attacker, finalDamage);
        spawnVanillaMeleeBloodParticles(
                world,
                attacker,
                target,
                detailedPart,
                finalDamage,
                armorResult.reduction(),
                vanillaMeleeDamageProfile(weapon)
        );
        if (armorResult.reduction() > 0.08) {
            playConfiguredMobArmorSound(world, target);
        } else {
            playConfiguredMobFleshSound(world, target);
        }
        syncAttackImpact(world, attacker);
        return true;
    }

    private static double heavyHammerVanillaStrikeDamage(PlayerEntity attacker, float amount) {
        ItemStack stack = attacker.getMainHandStack();
        int densityLevel = enchantmentLevel(stack, DENSITY_ENCHANTMENT_ID);
        double densityBonus = densityLevel <= 0
                ? 0.0
                : Math.max(0.0F, attacker.fallDistance) * 0.5 * densityLevel;
        return Math.max(0.0F, amount) + densityBonus;
    }

    private static void spawnVanillaMeleeBloodParticles(
            ServerWorld world,
            PlayerEntity attacker,
            LivingEntity target,
            String detailedPart,
            float damage,
            double armorReduction,
            DamageTypeProfile damageProfile
    ) {
        if (damage <= 0.0F) {
            return;
        }

        Vec3d position = directHitParticlePosition(attacker, target);
        Vec3d fromAttacker = target.getPos().subtract(attacker.getPos());
        Vec3d surfaceNormal = fromAttacker.lengthSquared() <= 0.000001
                ? Vec3d.fromPolar(0.0F, target.getYaw()).normalize()
                : fromAttacker.normalize();
        Vec3d attackerForward = Vec3d.fromPolar(0.0F, attacker.getYaw()).normalize();
        Vec3d targetBackward = Vec3d.fromPolar(0.0F, target.getYaw()).multiply(-1.0).normalize();
        Vec3d impactDirection = attackerForward.add(targetBackward.multiply(0.45));
        if (impactDirection.lengthSquared() <= 0.000001) {
            impactDirection = surfaceNormal;
        } else {
            impactDirection = impactDirection.normalize();
        }
        Vec3d hitboxMotion = impactDirection.multiply(3.0 + Math.min(6.0, damage));

        double bloodTypeScale = bloodCutParticleScale(damageProfile);
        int bloodDrops = scaledParticleCount(
                scaleBloodParticleBase(Math.max(3, Math.min(22, (int) Math.ceil(damage * 3.5))), bloodTypeScale),
                bloodParticleScale(armorReduction)
        );
        int bloodSparks = scaledParticleCount(
                scaleBloodParticleBase(
                        Math.max(8, Math.min(BLOOD_SPARKS_PER_CONTACT, (int) Math.ceil(damage * 7.0))),
                        bloodTypeScale
                ),
                bloodParticleScale(armorReduction)
        );
        spawnBloodDrops(world, target, position, surfaceNormal, impactDirection, hitboxMotion, bloodDrops);
        int bloodMist = 6;
        for (int i = 0; i < bloodMist; i++) {
            Vec3d velocity = randomizedSprayDirection(impactDirection, surfaceNormal, 0.62, 0.28, target)
                    .multiply(0.015 + target.getRandom().nextDouble() * 0.018);
            world.spawnParticles(
                    ModParticles.BLOOD_MIST,
                    position.x,
                    position.y,
                    position.z,
                    0,
                    velocity.x,
                    velocity.y,
                    velocity.z,
                    1.0
            );
        }
        for (int i = 0; i < bloodSparks; i++) {
            Vec3d velocity = randomizedBloodSparkDirection(impactDirection, surfaceNormal, target)
                    .multiply(0.13 + target.getRandom().nextDouble() * 0.16);
            spawnDirectedParticle(
                    world,
                    ModParticles.BLOOD_SPARK,
                    position.x,
                    position.y,
                    position.z,
                    velocity
            );
        }
    }

    private static DamageTypeProfile vanillaMeleeDamageProfile(ItemStack stack) {
        if (CombatItemUtil.isHeavyWeapon(stack)) {
            return EquipmentCombatAttributesRegistry.getWeapon(stack).damagePanel();
        }

        if (CombatItemUtil.isSword(stack)) {
            return new DamageTypeProfile(0.0, 0.6, 1.0);
        }

        return DamageTypeProfile.even(1.0);
    }

    private static MobCombatAttributes.MeleeDefenseTier configuredMobAttackDefenseTier(LivingEntity attacker) {
        return MobCombatAttributesRegistry.get(attacker)
                .map(MobCombatAttributes::meleeDefenseTier)
                .orElse(MobCombatAttributes.MeleeDefenseTier.BLOCKABLE);
    }

    private static void applyConfiguredMobKnockback(
            LivingEntity attacker,
            LivingEntity target,
            MobCombatAttributes attributes,
            boolean penetratedArmor
    ) {
        double strength = attributes.meleeKnockback();
        double verticalStrength = attributes.meleeVerticalKnockback();
        if (!penetratedArmor) {
            strength *= 0.70;
            verticalStrength *= 0.70;
        }
        if (strength <= 0.0) {
            if (verticalStrength > 0.0) {
                Vec3d current = target.getVelocity();
                target.setVelocity(current.x, Math.max(current.y, verticalStrength), current.z);
                target.velocityModified = true;
            }
            return;
        }

        Vec3d away = target.getPos().subtract(attacker.getPos());
        Vec3d horizontal = new Vec3d(away.x, 0.0, away.z);
        if (horizontal.lengthSquared() <= 0.000001) {
            horizontal = Vec3d.fromPolar(0.0F, attacker.getYaw());
        }

        Vec3d current = target.getVelocity();
        Vec3d knockback = horizontal.normalize().multiply(strength);
        target.setVelocity(
                current.x + knockback.x,
                Math.max(current.y, verticalStrength),
                current.z + knockback.z
        );
        target.velocityModified = true;
    }

    private static void handleConfirmedHit(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            HumanoidHurtboxLibrary.HitResult hitResult,
            ActiveServerAttack attack,
            boolean extraHeadHit
    ) {
        handleConfirmedHit(world, attacker, target, hitResult, attack, extraHeadHit, null);
    }

    private static void handleConfirmedHit(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            HumanoidHurtboxLibrary.HitResult hitResult,
            ActiveServerAttack attack,
            boolean extraHeadHit,
            Vec3d hitboxMotion
    ) {
        if (attack.hitTargets.contains(target.getUuid())) {
            if (attack.bloodiedTargets.contains(target.getUuid())) {
                spawnSustainedBloodSparks(world, attacker, target, hitResult, attack, hitboxMotion);
            }
            return;
        }

        if (canAttackBeDodged(attack)) {
            if (ServerCombatControlState.dodgesAttack(target, attack.direction)) {
                attack.hitTargets.add(target.getUuid());
                return;
            }

            if (target instanceof MobEntity mob
                    && attacker instanceof PlayerEntity
                    && HumanoidCombatAiTicker.tryDodgeIncomingAttack(mob, attacker)) {
                attack.hitTargets.add(target.getUuid());
                return;
            }
        }

        if (attack.comboMove == null
                && target instanceof MobEntity mob
                && attacker instanceof PlayerEntity
                && isAttackInFrontOfTarget(attacker, target)
                && HumanoidCombatAiTicker.tryStartMasterCounter(mob, attacker, attack.direction)) {
            attack.hitTargets.add(target.getUuid());
            attack.perfectBlocked = true;
            return;
        }

        BlockResult blockResult = tryBlock(world, attacker, target, attack);
        Vec3d particleHitboxMotion = comboWeaponHitboxMotion(attacker, attack).orElse(hitboxMotion);
        if (blockResult.blocked()) {
            attack.hitTargets.add(target.getUuid());
            if (blockResult.perfect()) {
                attack.perfectBlocked = true;
            } else {
                attack.connectedOrNormalBlocked = true;
            }
            boolean exhaustedBlock = applyBlockStaminaAndDisable(attacker, target, attack, blockResult);
            if (!blockResult.perfect()
                    && target instanceof MobEntity
                    && attacker instanceof PlayerEntity) {
                HumanoidCombatAiTicker.recordDefensivePressure(target);
            }
            syncBlockAnimation(world, target, attacker, blockResult);
            syncBlockImpact(world, attacker, target);
            spawnTemporaryBlockParticles(world, hitResult.position(), blockResult.perfect());
            playBlockSound(world, target, blockResult);
            if (blockResult.perfect()) {
                pushAway(attacker, target, 0.46);
                if (target instanceof MobEntity mob) {
                    HumanoidCombatAiTicker.onPerfectBlock(mob, attacker, blockResult.direction());
                }
            } else {
                pushAway(target, attacker, 0.32);
                if (attacker instanceof MobEntity mob && target instanceof ServerPlayerEntity) {
                    HumanoidCombatAiTicker.onPlayerVulnerableToFollowUp(mob, target);
                }
            }
            if (exhaustedBlock) {
                damageBlockedTarget(world, attacker, target, hitResult, attack, 0.55F, true, particleHitboxMotion);
            } else {
                damageWeaponDurability(attacker, false);
            }
            return;
        }

        attack.hitTargets.add(target.getUuid());
        DamageApplication damage = damageBlockedTarget(
                world,
                attacker,
                target,
                hitResult,
                attack,
                1.0F,
                true,
                particleHitboxMotion
        );
        if (!damage.damaged()) {
            return;
        }
        if (damage.damage() > 0.0F) {
            attack.bloodiedTargets.add(target.getUuid());
            attack.targetArmorReductions.put(target.getUuid(), damage.armorReduction());
            bloodAttackerHeldItems(attacker, damage.damage());
        }
        if (attacker instanceof MobEntity mob && target instanceof ServerPlayerEntity) {
            HumanoidCombatAiTicker.onPlayerVulnerableToFollowUp(mob, target);
        }
        if (extraHeadHit && !HumanoidHurtboxLibrary.isHumanoidTarget(target)) {
            damageBlockedTarget(
                    world,
                    attacker,
                    target,
                    HumanoidHurtboxLibrary.reportedHitResult(target, HumanoidHurtboxLibrary.Part.HEAD),
                    attack,
                    0.6F,
                    false
            );
        }
        attack.connectedOrNormalBlocked = true;

    }

    /**
     * 使用接近原版的攻击伤害。
     *
     * EntityAttributes.ATTACK_DAMAGE 会包含玩家空手自带的 1 点伤害。
     * 武器战斗只结算武器和属性修正提供的部分，所以这里会扣掉这 1 点。
     *
     * 这里暂时不处理暴击、横扫、附魔额外伤害。
     */
    private static boolean shouldPlayHitReaction(ActiveServerAttack attack) {
        if (attack.comboMove != null) {
            return attack.comboMove.hitReaction();
        }

        return attack.moveConfig().hitReaction();
    }

    private static int hitAttackDisableTicks(ActiveServerAttack attack) {
        if (attack.comboMove == null) {
            return CombatControlConfig.HIT_ATTACK_DISABLE_TICKS;
        }

        int remainingTicks = Math.max(0, attack.attackTotalTicks - attack.ageTicks);
        return Math.max(
                CombatControlConfig.HIT_ATTACK_DISABLE_TICKS,
                remainingTicks + CombatControlConfig.COMBO_HIT_REACTION_ATTACK_DISABLE_EXTRA_TICKS
        );
    }

    private static float calculateVanillaLikeAttackDamage(LivingEntity attacker) {
        float baseDamage = (float) attackDamageOrFallback(attacker, 1.0F);
        if (!(attacker instanceof MobEntity && attacker.getMainHandStack().isEmpty())) {
            baseDamage = Math.max(0.0F, baseDamage - 1.0F);
        }

        float cooldown = attacker instanceof PlayerEntity player
                ? player.getAttackCooldownProgress(0.5F)
                : 1.0F;
        float cooldownMultiplier = 0.2F + cooldown * cooldown * 0.8F;

        return baseDamage * cooldownMultiplier;
    }

    private static float applyMobDifficultyScaling(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            float damage
    ) {
        if (!(attacker instanceof MobEntity) || !(target instanceof ServerPlayerEntity)) {
            return damage;
        }

        Difficulty difficulty = world.getDifficulty();
        if (difficulty == Difficulty.PEACEFUL) {
            return 0.0F;
        }
        if (difficulty == Difficulty.EASY) {
            return Math.min(damage / 2.0F + 1.0F, damage);
        }
        if (difficulty == Difficulty.HARD) {
            return damage * 1.5F;
        }
        return damage;
    }

    private static Optional<HumanoidHurtboxLibrary.HitResult> getTargetHitResult(
            AnimatedAttackHitboxLibrary.OrientedBox hitBox,
            LivingEntity target
    ) {
        ActiveServerAttack targetAttack = ServerCombatState.getAttack(target.getUuid());
        if (targetAttack != null) {
            return HumanoidHurtboxLibrary.getAnimatedHitResult(
                    hitBox,
                    target,
                    HumanoidAnimationPoseLibrary.Kind.ATTACK,
                    targetAttack.direction,
                    targetAttack.getAnimationElapsedSeconds()
            );
        }

        return HumanoidHurtboxLibrary.getHitResult(hitBox, target);
    }

    private static DamageApplication damageBlockedTarget(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            HumanoidHurtboxLibrary.HitResult hitResult,
            ActiveServerAttack attack,
            float damageMultiplier
    ) {
        return damageBlockedTarget(world, attacker, target, hitResult, attack, damageMultiplier, true);
    }

    private static DamageApplication damageBlockedTarget(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            HumanoidHurtboxLibrary.HitResult hitResult,
            ActiveServerAttack attack,
            float damageMultiplier,
            boolean damageWeapon
    ) {
        return damageBlockedTarget(world, attacker, target, hitResult, attack, damageMultiplier, damageWeapon, null);
    }

    private static DamageApplication damageBlockedTarget(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            HumanoidHurtboxLibrary.HitResult hitResult,
            ActiveServerAttack attack,
            float damageMultiplier,
            boolean damageWeapon,
            Vec3d hitboxMotion
    ) {
        return damageBlockedTarget(
                world,
                attacker,
                target,
                hitResult,
                attack,
                damageMultiplier,
                damageWeapon,
                null,
                null,
                false,
                hitboxMotion
        );
    }

    private static DamageApplication damageBlockedTarget(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            HumanoidHurtboxLibrary.HitResult hitResult,
            ActiveServerAttack attack,
            float damageMultiplier,
            boolean damageWeapon,
            List<AttackMoveConfig.HitZoneRule> overrideHitZoneRules,
            DamageTypeProfile overrideDamageModifiers
    ) {
        return damageBlockedTarget(
                world,
                attacker,
                target,
                hitResult,
                attack,
                damageMultiplier,
                damageWeapon,
                overrideHitZoneRules,
                overrideDamageModifiers,
                false,
                null
        );
    }

    private static DamageApplication damageBlockedTarget(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            HumanoidHurtboxLibrary.HitResult hitResult,
            ActiveServerAttack attack,
            float damageMultiplier,
            boolean damageWeapon,
            List<AttackMoveConfig.HitZoneRule> overrideHitZoneRules,
            DamageTypeProfile overrideDamageModifiers,
            boolean cinematic
    ) {
        return damageBlockedTarget(
                world,
                attacker,
                target,
                hitResult,
                attack,
                damageMultiplier,
                damageWeapon,
                overrideHitZoneRules,
                overrideDamageModifiers,
                cinematic,
                null
        );
    }

    private static DamageApplication damageBlockedTarget(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            HumanoidHurtboxLibrary.HitResult hitResult,
            ActiveServerAttack attack,
            float damageMultiplier,
            boolean damageWeapon,
            List<AttackMoveConfig.HitZoneRule> overrideHitZoneRules,
            DamageTypeProfile overrideDamageModifiers,
            boolean cinematic,
            Vec3d hitboxMotion
    ) {
        String detailedPart = detailedPartForHit(hitResult, attack, overrideHitZoneRules);
        CombatDamageBreakdown damageBreakdown =
                calculateCombatDamage(
                        attacker,
                        target,
                        hitResult.part(),
                        attack,
                        detailedPart,
                        overrideDamageModifiers
                );
        double postArmorMultiplier = damageMultiplier
                * injuryDamageMultiplier(target, detailedPart)
                * PassiveSkillPerks.damageMultiplier(
                        attacker,
                        attacker.getMainHandStack(),
                        attack.comboMove != null,
                        attack.moveConfig().animationName().startsWith("master_counter"),
                        detailedPart
                );
        float damage = (float) (damageBreakdown.damage() * postArmorMultiplier);

        if (target instanceof ArmorStandEntity) {
            cancelKnockback(target);
            if (!cinematic) {
                applyConfiguredHorizontalKnockback(attacker, target, attack, false);
            }
            spawnCustomHitParticles(world, attacker, target, hitResult, detailedPart, false, 0.0F, hitboxMotion);
            playArmorBlockedSound(world, attacker, target, attack, detailedPart, damageBreakdown);
            syncNonPenetratingHitFeedback(world, target, hitResult.part(), attack);
            syncAttackImpact(world, attacker);
            return new DamageApplication(true, 0.0F, damageBreakdown.reduction());
        }

        if (damage <= 0.0F) {
            if (damageWeapon) {
                damageWeaponDurability(attacker, false);
            }
            if (target instanceof MobEntity && attacker instanceof PlayerEntity) {
                HumanoidCombatAiTicker.recordDefensivePressure(target);
            }

            cancelKnockback(target);
            if (!cinematic) {
                applyConfiguredHorizontalKnockback(attacker, target, attack, false);
            }
            applyImpactStaminaDamage(
                    attacker,
                    target,
                    attack,
                    damageBreakdown.impactMitigation(),
                    damageBreakdown.blockedStrikeImpactBonus(),
                    false
            );

            spawnCustomHitParticles(world, attacker, target, hitResult, detailedPart, false, 0.0F, hitboxMotion);
            playArmorBlockedSound(world, attacker, target, attack, detailedPart, damageBreakdown);
            if (!cinematic) {
                syncNonPenetratingHitFeedback(world, target, hitResult.part(), attack);
                syncAttackImpact(world, attacker);
            }

            return new DamageApplication(true, 0.0F, damageBreakdown.reduction());
        }

        float finalDamage = applyMobDifficultyScaling(world, attacker, target, damage);
        if (finalDamage <= 0.0F) {
            if (damageWeapon) {
                damageWeaponDurability(attacker, false);
            }
            if (target instanceof MobEntity && attacker instanceof PlayerEntity) {
                HumanoidCombatAiTicker.recordDefensivePressure(target);
            }
            cancelKnockback(target);
            if (!cinematic) {
                applyConfiguredHorizontalKnockback(attacker, target, attack, false);
            }
            applyImpactStaminaDamage(
                    attacker,
                    target,
                    attack,
                    damageBreakdown.impactMitigation(),
                    damageBreakdown.blockedStrikeImpactBonus(),
                    false
            );
            spawnCustomHitParticles(world, attacker, target, hitResult, detailedPart, false, 0.0F, hitboxMotion);
            playArmorBlockedSound(world, attacker, target, attack, detailedPart, damageBreakdown);
            if (!cinematic) {
                syncNonPenetratingHitFeedback(world, target, hitResult.part(), attack);
                syncAttackImpact(world, attacker);
            }
            return new DamageApplication(true, 0.0F, damageBreakdown.reduction());
        }

        boolean delayedLethal = (cinematic || attack.moveConfig().directHitTick() >= 0)
                && finalDamage >= target.getHealth();
        float appliedDamage = delayedLethal
                ? Math.max(0.0F, target.getHealth() - 1.0F)
                : finalDamage;
        if (delayedLethal) {
            attack.delayedLethalTargetEntityId = target.getId();
            attack.delayedLethalDamage = Math.max(finalDamage, target.getHealth() + 1.0F);
        }

        boolean suppressVanillaFeedback = shouldSuppressVanillaHurtFeedback(target);
        target.timeUntilRegen = 0;
        if (suppressVanillaFeedback) {
            target.hurtTime = 0;
        }
        boolean damaged = appliedDamage <= 0.0F || target.damage(
                world,
                createDamageSource(world, attacker),
                appliedDamage
        );

        if (!damaged) {
            if (damageWeapon) {
                damageWeaponDurability(attacker, false);
            }
            cancelKnockback(target);
            if (!cinematic) {
                applyConfiguredHorizontalKnockback(attacker, target, attack, false);
            }
            spawnCustomHitParticles(world, attacker, target, hitResult, detailedPart, false, 0.0F, hitboxMotion);
            playArmorBlockedSound(world, attacker, target, attack, detailedPart, damageBreakdown);
            if (!cinematic) {
                syncNonPenetratingHitFeedback(world, target, hitResult.part(), attack);
                syncAttackImpact(world, attacker);
            }
            return new DamageApplication(true, 0.0F, damageBreakdown.reduction());
        }
        if (damageWeapon) {
            damageWeaponDurability(attacker, true);
        }
        interruptRangedUse(target);
        applyPostDamageEffects(world, attacker, target, appliedDamage);
        PassiveSkillPerks.afterHit(attacker);
        if (attack.comboMove != null) {
            PassiveSkillPerks.afterComboHit(attacker, target, attacker.getMainHandStack(), detailedPart);
        }
        if (!target.isAlive()) {
            PassiveSkillPerks.afterKill(attacker);
        }
        notifyReceivedAttack(world, attacker, target);
        if (suppressVanillaFeedback) {
            target.hurtTime = 0;
            syncSuppressHurtOverlay(world, target);
        } else {
            target.hurtTime = Math.max(target.hurtTime, 6);
            target.maxHurtTime = Math.max(target.maxHurtTime, 6);
        }

        cancelKnockback(target);
        if (!cinematic) {
            applyConfiguredHorizontalKnockback(attacker, target, attack, damageBreakdown.penetrated());
        }
        boolean interrupted = applyImpactStaminaDamage(
                attacker,
                target,
                attack,
                damageBreakdown.impactMitigation(),
                damageBreakdown.blockedStrikeImpactBonus(),
                damageBreakdown.penetrated() && finalDamage > 0.0F
        );
        maybeApplyDamageInjury(target, detailedPart, finalDamage);
        if (target instanceof MobEntity && attacker instanceof PlayerEntity) {
            HumanoidCombatAiTicker.recordDefensivePressure(target);
        }
        ServerCombatControlState.disableAttack(target.getUuid(), hitAttackDisableTicks(attack));
        ServerCombatControlState.disableBlock(
                target.getUuid(),
                CombatControlConfig.HIT_BLOCK_DISABLE_TICKS
        );
        boolean comboHitReaction = !cinematic && attack.comboMove != null && shouldPlayHitReaction(attack);
        if (!cinematic && attack.comboMove != null
                && (attack.comboMove.hitReactionInterruptsAttack() || comboHitReaction)) {
            ServerCombatState.removeAttack(target.getUuid());
            ServerComboState.clear(target.getUuid());
            if (target instanceof MobEntity mob) {
                HumanoidCombatAiTicker.interruptFollowUps(mob);
            }
            syncAttackInterrupt(world, target);
        }
        if (!cinematic && attack.comboMove != null && attack.comboMove.hitReactionMovementLockTicks() > 0) {
            ServerCombatControlState.disableMovement(
                    target.getUuid(),
                    attack.comboMove.hitReactionMovementLockTicks()
            );
        }
        spawnCustomHitParticles(
                world,
                attacker,
                target,
                hitResult,
                detailedPart,
                true,
                finalDamage,
                damageBreakdown.reduction(),
                getAttackDamageProfile(attacker, attack, overrideDamageModifiers),
                hitboxMotion
        );
        playFleshHitSound(world, attacker, target, attack, detailedPart, damageBreakdown);
        if (!cinematic && shouldPlayHitReaction(attack)) {
            syncHitReaction(
                    world,
                    target,
                    reactionPartForDetailedPart(hitResult.part(), detailedPart),
                    detailedPart,
                    attack,
                    interrupted || attack.comboMove != null
            );
        }
        if (!cinematic) {
            syncAttackImpact(world, attacker);
        }

        if (!cinematic && target instanceof ServerPlayerEntity player) {
            sendReceivedDamageNotice(player, attacker, detailedPart, damageBreakdown, damageMultiplier, finalDamage);
            clearPlayerVelocityForCustomHit(player);
            ServerPlayNetworking.send(
                    player,
                    new HitFeedbackPayload(attack.direction.ordinal(), true)
            );
        }

        return new DamageApplication(true, finalDamage, damageBreakdown.reduction());
    }

    public static void finishDelayedLethalHit(
            ServerWorld world,
            LivingEntity attacker,
            ActiveServerAttack attack
    ) {
        if (attack.delayedLethalTargetEntityId < 0 || attack.delayedLethalDamage <= 0.0F) {
            return;
        }

        Entity entity = world.getEntityById(attack.delayedLethalTargetEntityId);
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
            return;
        }

        target.timeUntilRegen = 0;
        target.hurtTime = 0;
        syncSuppressHurtOverlay(world, target);
        target.damage(
                world,
                createDamageSource(world, attacker),
                Math.max(attack.delayedLethalDamage, target.getHealth() + 1.0F)
        );
        target.hurtTime = 0;
        syncSuppressHurtOverlay(world, target);
    }

    private static CombatDamageBreakdown calculateCombatDamage(
            LivingEntity attacker,
            LivingEntity target,
            HumanoidHurtboxLibrary.Part hitPart,
            ActiveServerAttack attack,
            String detailedPart
    ) {
        return calculateCombatDamage(attacker, target, hitPart, attack, detailedPart, null);
    }

    private static CombatDamageBreakdown calculateCombatDamage(
            LivingEntity attacker,
            LivingEntity target,
            HumanoidHurtboxLibrary.Part hitPart,
            ActiveServerAttack attack,
            String detailedPart,
            DamageTypeProfile overrideDamageModifiers
    ) {
        float baseDamage = calculateVanillaLikeAttackDamage(attacker);
        baseDamage *= Math.max(
                0.0,
                1.0 - 0.06 * ModStatusEffects.effectiveLevel(attacker, ModStatusEffects.ARM_INJURY)
        );

        DamageTypeProfile attackProfile = getAttackDamageProfile(attacker, attack, overrideDamageModifiers);
        DamageTypeProfile enchantDamage = enchantmentDamage(attacker.getMainHandStack())
                .multiply(getMoveDamageModifiers(attack, overrideDamageModifiers))
                .multiply(DamageTypeProfile.even(0.2));

        ArmorResult armorResult = applyArmorReductionAndDurability(
                attacker,
                target,
                List.of(detailedPart),
                attack,
                baseDamage,
                attackProfile,
                enchantDamage
        );

        TypeDamage incomingPanel = incomingDamagePanel(
                attacker,
                target,
                attack,
                detailedPart,
                overrideDamageModifiers
        );

        return new CombatDamageBreakdown(
                armorResult.damage() * EquipmentFallbackConfig.partDamageMultiplier(detailedPart)
                        + postArmorTargetEnchantmentDamage(attacker, target, armorResult.penetrated()),
                incomingPanel.thrust(),
                incomingPanel.strike(),
                incomingPanel.slash(),
                armorResult.impactMitigation(),
                armorResult.reduction(),
                armorResult.blockedStrikeImpactBonus(),
                armorResult.penetrated()
        );
    }

    private static TypeDamage incomingDamagePanel(
            LivingEntity attacker,
            LivingEntity target,
            ActiveServerAttack attack,
            String detailedPart
    ) {
        return incomingDamagePanel(attacker, target, attack, detailedPart, null);
    }

    private static TypeDamage incomingDamagePanel(
            LivingEntity attacker,
            LivingEntity target,
            ActiveServerAttack attack,
            String detailedPart,
            DamageTypeProfile overrideDamageModifiers
    ) {
        float baseDamage = calculateVanillaLikeAttackDamage(attacker);
        baseDamage *= Math.max(0.0, 1.0 - 0.06 * ModStatusEffects.effectiveLevel(attacker, ModStatusEffects.ARM_INJURY));
        DamageTypeProfile attackProfile = getAttackDamageProfile(attacker, attack, overrideDamageModifiers);
        DamageTypeProfile enchantDamage = enchantmentDamage(attacker.getMainHandStack())
                .multiply(getMoveDamageModifiers(attack, overrideDamageModifiers))
                .multiply(DamageTypeProfile.even(0.2));
        double bonusStrikeDamage = com.kingdomcomecombat.stamina.ServerStaminaState.getCurrent(target) <= 0.5
                ? attackImpact(attacker, attack) / EXHAUSTED_TARGET_BONUS_STRIKE_DIVISOR
                : 0.0;
        double partMultiplier = EquipmentFallbackConfig.partDamageMultiplier(detailedPart);

        return new TypeDamage(
                (baseDamage * attackProfile.thrust() + enchantDamage.thrust()) * partMultiplier,
                (baseDamage * attackProfile.strike() + enchantDamage.strike() + bonusStrikeDamage) * partMultiplier,
                (baseDamage * attackProfile.slash() + enchantDamage.slash()) * partMultiplier
        );
    }

    private static void sendReceivedDamageNotice(
            ServerPlayerEntity player,
            LivingEntity attacker,
            String detailedPart,
            CombatDamageBreakdown damageBreakdown,
            float damageMultiplier,
            float finalDamage
    ) {
    }

    private static String displayPartName(String detailedPart) {
        return switch (detailedPart) {
            case "face" -> "脸部";
            case "neck" -> "颈部";
            case "crown" -> "头顶";
            case "side_head" -> "头侧";
            case "shoulder" -> "肩部";
            case "arm" -> "手臂";
            case "hand" -> "手部";
            case "chest" -> "胸部";
            case "abdomen" -> "腹部";
            case "thigh" -> "大腿";
            case "knee" -> "膝部";
            case "calf" -> "小腿";
            case "foot" -> "脚部";
            default -> detailedPart == null || detailedPart.isBlank() ? "未知" : detailedPart;
        };
    }

    private static String formatOneDecimal(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static DamageTypeProfile getAttackDamageProfile(
            LivingEntity attacker,
            ActiveServerAttack attack
    ) {
        return getAttackDamageProfile(attacker, attack, null);
    }

    private static DamageTypeProfile getAttackDamageProfile(
            LivingEntity attacker,
            ActiveServerAttack attack,
            DamageTypeProfile overrideDamageModifiers
    ) {
        DamageTypeProfile move = getMoveDamageModifiers(attack, overrideDamageModifiers);
        if (attacker.getMainHandStack().isEmpty()) {
            return move;
        }

        WeaponCombatAttributes weapon =
                EquipmentCombatAttributesRegistry.getWeapon(attacker.getMainHandStack());

        return weaponDamagePanelWithDurability(attacker.getMainHandStack(), weapon.damagePanel())
                .multiply(move);
    }

    private static DamageTypeProfile getMoveDamageModifiers(ActiveServerAttack attack) {
        return getMoveDamageModifiers(attack, null);
    }

    private static DamageTypeProfile getMoveDamageModifiers(
            ActiveServerAttack attack,
            DamageTypeProfile overrideDamageModifiers
    ) {
        if (overrideDamageModifiers != null) {
            return overrideDamageModifiers;
        }

        return attack.comboMove != null
                ? attack.comboMove.damageModifiers()
                : new DamageTypeProfile(
                        attack.moveConfig().thrustModifier(),
                        attack.moveConfig().strikeModifier(),
                        attack.moveConfig().slashModifier()
                );
    }

    private static Vec3d sampledHitboxMotion(
            LivingEntity attacker,
            ActiveServerAttack attack,
            AnimatedAttackHitboxLibrary.SampledHitbox current
    ) {
        double elapsed = attack.getAnimationElapsedSeconds();
        if (elapsed <= 0.0001) {
            return Vec3d.ZERO;
        }

        double previousElapsed = Math.max(0.0, elapsed - 0.05);
        Optional<AnimatedAttackHitboxLibrary.SampledHitbox> previous =
                sampleAttackHitboxAt(attacker, attack, (float) previousElapsed);
        if (previous.isEmpty()) {
            return Vec3d.ZERO;
        }

        Vec3d motion = current.toWorldBox(attacker).center()
                .subtract(previous.get().toWorldBox(attacker).center());
        return motion.lengthSquared() <= 0.000001 ? Vec3d.ZERO : motion.multiply(20.0);
    }

    private static Vec3d currentHitboxMotion(
            LivingEntity attacker,
            ActiveServerAttack attack
    ) {
        Optional<AnimatedAttackHitboxLibrary.SampledHitbox> current =
                sampleAttackHitboxAt(attacker, attack, attack.getAnimationElapsedSeconds());
        return current
                .map(sampled -> sampledHitboxMotion(attacker, attack, sampled))
                .orElse(Vec3d.ZERO);
    }

    private static Optional<Vec3d> comboWeaponHitboxMotion(
            LivingEntity attacker,
            ActiveServerAttack attack
    ) {
        if (attack.comboMove == null) {
            return Optional.empty();
        }

        Optional<AnimatedAttackHitboxLibrary.SampledHitbox> current =
                sampleComboHitboxAt(attacker, attack, attack.getAnimationElapsedSeconds(), true);
        if (current.isEmpty()) {
            return Optional.empty();
        }

        double elapsed = attack.getAnimationElapsedSeconds();
        if (elapsed <= 0.0001) {
            return Optional.of(Vec3d.ZERO);
        }

        Optional<AnimatedAttackHitboxLibrary.SampledHitbox> previous =
                sampleComboHitboxAt(attacker, attack, (float) Math.max(0.0, elapsed - 0.05), true);
        if (previous.isEmpty()) {
            return Optional.empty();
        }

        Vec3d motion = current.get().toWorldBox(attacker).center()
                .subtract(previous.get().toWorldBox(attacker).center());
        return Optional.of(motion.lengthSquared() <= 0.000001 ? Vec3d.ZERO : motion.multiply(20.0));
    }

    private static Optional<AnimatedAttackHitboxLibrary.SampledHitbox> sampleAttackHitboxAt(
            LivingEntity attacker,
            ActiveServerAttack attack,
            float elapsedSeconds
    ) {
        if (attack.comboMove != null) {
            return sampleComboHitboxAt(attacker, attack, elapsedSeconds, attack.comboMove.useRealHitbox());
        }

        AttackMoveConfig moveConfig = attack.moveConfig();
        if (moveConfig.animationName().isBlank()) {
            return AnimatedAttackHitboxLibrary.sampleSeconds(
                    attack.direction,
                    elapsedSeconds,
                    moveConfig.useRealHitbox()
            );
        }

        return AnimatedAttackHitboxLibrary.sampleNamedSeconds(
                moveConfig.animationName(),
                elapsedSeconds,
                moveConfig.useRealHitbox(),
                realHitboxSizeUnits(attacker.getMainHandStack()),
                EquipmentCombatAttributesRegistry.realHitboxOffsetUnits(
                        attacker.getMainHandStack(),
                        AnimatedAttackHitboxLibrary.getRealHitboxOffsetUnits()
                ),
                EquipmentCombatAttributesRegistry.realHitboxRotationDegrees(
                        attacker.getMainHandStack(),
                        AnimatedAttackHitboxLibrary.getRealHitboxRotationDegrees()
                )
        );
    }

    private static Optional<AnimatedAttackHitboxLibrary.SampledHitbox> sampleComboHitboxAt(
            LivingEntity attacker,
            ActiveServerAttack attack,
            float elapsedSeconds,
            boolean useRealHitbox
    ) {
        if (attack.comboMove == null) {
            return Optional.empty();
        }

        return AnimatedAttackHitboxLibrary.sampleNamedSeconds(
                attack.comboMove.animationName(),
                elapsedSeconds,
                useRealHitbox,
                realHitboxSizeUnits(attacker.getMainHandStack()),
                EquipmentCombatAttributesRegistry.realHitboxOffsetUnits(
                        attacker.getMainHandStack(),
                        AnimatedAttackHitboxLibrary.getRealHitboxOffsetUnits()
                ),
                EquipmentCombatAttributesRegistry.realHitboxRotationDegrees(
                        attacker.getMainHandStack(),
                        AnimatedAttackHitboxLibrary.getRealHitboxRotationDegrees()
                )
        );
    }

    private static Vec3d realHitboxSizeUnits(ItemStack stack) {
        Vec3d size = EquipmentCombatAttributesRegistry.realHitboxSizeUnits(
                stack,
                AnimatedAttackHitboxLibrary.getRealHitboxSizeUnits()
        );
        int sweepingLevel = enchantmentLevel(stack, SWEEPING_EDGE_ENCHANTMENT_ID);
        if (sweepingLevel <= 0) {
            return size;
        }

        return new Vec3d(size.x, size.y, size.z + sweepingLevel);
    }

    private static DamageTypeProfile weaponDamagePanelWithDurability(
            ItemStack stack,
            DamageTypeProfile panel
    ) {
        double edgeMultiplier = weaponEdgeDurabilityMultiplier(stack);
        return new DamageTypeProfile(
                panel.thrust() * edgeMultiplier,
                panel.strike(),
                panel.slash() * edgeMultiplier
        );
    }

    private static DamageTypeProfile enchantmentDamage(ItemStack stack) {
        double thrust = 0.0;
        double strike = 0.0;
        double slash = 0.0;

        for (Map.Entry<Identifier, DamageTypeProfile> entry
                : EquipmentFallbackConfig.weaponDamageEnchantments().entrySet()) {
            int level = enchantmentLevel(stack, entry.getKey());
            if (level <= 0) {
                continue;
            }

            DamageTypeProfile profile = entry.getValue();
            thrust += profile.thrust() * level;
            strike += profile.strike() * level;
            slash += profile.slash() * level;
        }

        for (Map.Entry<Identifier, DamageTypeProfile> entry
                : EquipmentFallbackConfig.weaponDamageFirstTwoBonusEnchantments().entrySet()) {
            int level = enchantmentLevel(stack, entry.getKey());
            if (level <= 0) {
                continue;
            }

            int bonusLevels = Math.min(level, 2);
            DamageTypeProfile profile = entry.getValue();
            thrust += profile.thrust() * bonusLevels;
            strike += profile.strike() * bonusLevels;
            slash += profile.slash() * bonusLevels;
        }

        return new DamageTypeProfile(thrust, strike, slash);
    }

    private static void interruptRangedUse(LivingEntity target) {
        if (!(target instanceof MobEntity) || !target.isUsingItem()) {
            return;
        }

        if (target.getActiveItem().getItem() instanceof RangedWeaponItem) {
            target.clearActiveItem();
        }
    }

    private static void applyHeavyHammerVanillaPostDamageEffects(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            float damage,
            ItemStack stack
    ) {
        if (!CombatItemUtil.isFightingMace(stack) || stack.isEmpty()) {
            applyPostDamageEffects(world, attacker, target, damage);
            return;
        }

        boolean shouldPostDamage = stack.postHit(target, attacker);
        EnchantmentHelper.onTargetDamaged(world, target, vanillaHeavyHammerEnchantmentSource(world, attacker), stack);
        PotionCoatingHandler.applyCoatedWeaponEffects(attacker, target, damage);
        if (shouldPostDamage && !stack.isEmpty()) {
            stack.postDamageEntity(target, attacker);
        }
    }

    private static void applyPostDamageEffects(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            float damage
    ) {
        ItemStack stack = attacker.getMainHandStack();
        if (!stack.isEmpty()) {
            stack.postDamageEntity(target, attacker);
        }
        EnchantmentHelper.onTargetDamaged(world, target, createDamageSource(world, attacker), stack);
        PotionCoatingHandler.applyCoatedWeaponEffects(attacker, target, damage);
        if (damage > 0.0F && attacker instanceof WitherSkeletonEntity) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 200), attacker);
        }
    }

    private static double weaponEdgeDurabilityMultiplier(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageable() || stack.getMaxDamage() <= 0) {
            return 1.0;
        }

        double damageRatio = Math.max(0.0, Math.min(1.0, stack.getDamage() / (double) stack.getMaxDamage()));
        return 1.0 - (1.0 - BROKEN_WEAPON_EDGE_PANEL_MULTIPLIER) * damageRatio;
    }
    private static ArmorResult applyArmorReductionAndDurability(
            LivingEntity attacker,
            LivingEntity target,
            List<String> possibleParts,
            ActiveServerAttack attack,
            double baseDamage,
            DamageTypeProfile attackProfile,
            DamageTypeProfile enchantDamage
    ) {
        double bonusStrikeDamage = com.kingdomcomecombat.stamina.ServerStaminaState.getCurrent(target) <= 0.5
                ? attackImpact(attacker, attack) / EXHAUSTED_TARGET_BONUS_STRIKE_DIVISOR
                : 0.0;

        TypeDamage raw = new TypeDamage(
                baseDamage * attackProfile.thrust() + enchantDamage.thrust(),
                baseDamage * attackProfile.strike() + enchantDamage.strike() + bonusStrikeDamage,
                baseDamage * attackProfile.slash() + enchantDamage.slash()
        );

        double rawTotal = raw.thrust() + raw.strike() + raw.slash();

        double penetrationBaseDamage = armorPenetrationBaseDamage(attacker, baseDamage);
        TypeDamage penetrationRaw = new TypeDamage(
                penetrationBaseDamage * attackProfile.thrust() + enchantDamage.thrust(),
                penetrationBaseDamage * attackProfile.strike() + enchantDamage.strike() + bonusStrikeDamage,
                penetrationBaseDamage * attackProfile.slash() + enchantDamage.slash()
        );
        TypeDamage penetration = new TypeDamage(
                Math.ceil(penetrationRaw.thrust() * 5.0),
                Math.ceil(penetrationRaw.strike() * 5.0),
                Math.ceil(penetrationRaw.slash() * 5.0)
        );

        return applyArmorReductionAndDurability(
                attacker,
                target,
                possibleParts,
                raw,
                rawTotal,
                penetration
        );
    }

    private static double armorPenetrationBaseDamage(LivingEntity attacker, double baseDamage) {
        if (attacker instanceof MobEntity mob && HumanoidCombatAiProfiles.getProfile(mob) != null) {
            return 1.0;
        }

        return baseDamage;
    }

    private static ArmorResult applyArmorReductionAndDurability(
            LivingEntity attacker,
            LivingEntity target,
            List<String> possibleParts,
            TypeDamage raw,
            double rawTotal,
            TypeDamage penetration
    ) {
        double armorDurabilityMultiplier = armorDurabilityDamageMultiplier(attacker, target);
        double thrustDamage = raw.thrust();
        double strikeDamage = raw.strike();
        double slashDamage = raw.slash();

        double bestImpactMitigation = 0.0;
        ItemStack durabilityTarget = ItemStack.EMPTY;
        double bestTotalReduction = -1.0;

        for (EquipmentSlot slot : EquipmentCombatAttributesRegistry.ARMOR_PANEL_SLOTS) {
            ItemStack stack = target.getEquippedStack(slot);
            ArmorCombatAttributes armor = EquipmentCombatAttributesRegistry.getArmor(stack);
            if (stack.isEmpty() || armor.protectedParts().isEmpty()) {
                continue;
            }

            Map<String, ArmorCombatAttributes.PartProtection> effectiveProtectedParts =
                    EquipmentCombatAttributesRegistry.effectiveProtectedParts(stack, armor);
            ArmorCombatAttributes.PartProtection matchedProtection = null;
            for (String part : possibleParts) {
                matchedProtection = effectiveProtectedParts.get(part);
                if (matchedProtection != null) {
                    break;
                }
            }

            if (matchedProtection == null) {
                continue;
            }

            DamageTypeProfile resolvedDefense = resolveArmorDefense(armor, stack, target);
            resolvedDefense = resolvedDefense.multiply(DamageTypeProfile.even(armorPanelMultiplierFromBreach(attacker)));
            double partProtection = matchedProtection.protectionPercent();

            double thrustReduction = reductionFromDefense(
                    penetration.thrust(),
                    resolvedDefense.thrust() * partProtection
            );
            double strikeReduction = reductionFromDefense(
                    penetration.strike(),
                    resolvedDefense.strike() * partProtection
            );
            double slashReduction = reductionFromDefense(
                    penetration.slash(),
                    resolvedDefense.slash() * partProtection
            );

            double candidateThrustDamage =
                    raw.thrust() * Math.max(0.0, 1.0 - thrustReduction);
            double candidateStrikeDamage =
                    raw.strike() * Math.max(0.0, 1.0 - strikeReduction);
            double candidateSlashDamage =
                    raw.slash() * Math.max(0.0, 1.0 - slashReduction);

            thrustDamage = Math.min(thrustDamage, candidateThrustDamage);
            strikeDamage = Math.min(strikeDamage, candidateStrikeDamage);
            slashDamage = Math.min(slashDamage, candidateSlashDamage);

            double totalReduction = thrustReduction + strikeReduction + slashReduction;
            if (totalReduction > bestTotalReduction && stack.isDamageable()) {
                bestTotalReduction = totalReduction;
                durabilityTarget = stack;
            }

            bestImpactMitigation = Math.max(
                    bestImpactMitigation,
                    Math.min(0.95, matchedProtection.impactMitigationPercent()
                            + enchantmentImpactMitigation(stack))
            );
        }

        boolean naturalArmorReducedDamage = false;
        MobCombatAttributesRegistry.ArmorSection naturalArmorSection = naturalArmorSectionFor(possibleParts);
        Optional<MobCombatAttributes> naturalArmorAttributes = MobCombatAttributesRegistry.get(target);
        MobCombatAttributes.NaturalArmor naturalArmor = naturalArmorAttributes
                .map(attributes -> naturalArmorSection == MobCombatAttributesRegistry.ArmorSection.HEAD
                        ? attributes.headArmor()
                        : attributes.bodyArmor())
                .orElse(null);
        if (naturalArmor != null && naturalArmor.defense().total() > 0.000001) {
            DamageTypeProfile resolvedDefense = resolveNaturalArmorDefense(target, naturalArmor, naturalArmorSection);
            resolvedDefense = resolvedDefense.multiply(DamageTypeProfile.even(armorPanelMultiplierFromBreach(attacker)));

            double thrustReduction = reductionFromDefense(penetration.thrust(), resolvedDefense.thrust());
            double strikeReduction = reductionFromDefense(penetration.strike(), resolvedDefense.strike());
            double slashReduction = reductionFromDefense(penetration.slash(), resolvedDefense.slash());
            naturalArmorReducedDamage = thrustReduction > 0.0
                    || strikeReduction > 0.0
                    || slashReduction > 0.0;

            thrustDamage = Math.min(thrustDamage, raw.thrust() * Math.max(0.0, 1.0 - thrustReduction));
            strikeDamage = Math.min(strikeDamage, raw.strike() * Math.max(0.0, 1.0 - strikeReduction));
            slashDamage = Math.min(slashDamage, raw.slash() * Math.max(0.0, 1.0 - slashReduction));

            double totalReduction = thrustReduction + strikeReduction + slashReduction;
            if (totalReduction > bestTotalReduction && naturalArmor.durability() > 0) {
                bestTotalReduction = totalReduction;
                durabilityTarget = ItemStack.EMPTY;
            }

            bestImpactMitigation = Math.max(bestImpactMitigation, naturalArmor.impactMitigation());
        }

        damageArmorDurability(durabilityTarget, penetration, armorDurabilityMultiplier);
        damageNaturalArmorDurability(
                target,
                naturalArmorReducedDamage ? naturalArmor : null,
                naturalArmorSection,
                penetration
        );

        double damage = thrustDamage + strikeDamage + slashDamage;
        double reduction = rawTotal <= 0.000001
                ? 0.0
                : Math.max(0.0, 1.0 - damage / rawTotal);

        // 核心新增：
        // 无法击穿或被护甲挡掉的 strike 伤害，按 JSON 配置比例转成额外冲击力。
        double blockedStrikeDamage = Math.max(0.0, raw.strike() - strikeDamage);
        double blockedStrikeImpactBonus =
                blockedStrikeDamage * EquipmentFallbackConfig.blockedStrikeToImpactRatio();

        return new ArmorResult(
                damage,
                bestImpactMitigation,
                reduction,
                blockedStrikeImpactBonus,
                damage > 0.000001
        );
    }

    public static ProjectileArmorResult applyProjectileArmorDamage(
            LivingEntity target,
            Entity projectile,
            float damage
    ) {
        if (damage <= 0.0F) {
            return ProjectileArmorResult.blocked();
        }
        if (HandCannonProjectileTracker.isTracked(projectile)) {
            damage *= 14.0F;
        }

        DamageTypeProfile profile = ProjectileCombatAttributesRegistry.get(projectile);
        ProjectileHitDetails hitDetails = projectileHitDetails(target, projectile);
        String detailedPart = hitDetails.detailedPart();
        LivingEntity attacker = projectile instanceof ProjectileEntity projectileEntity
                && projectileEntity.getOwner() instanceof LivingEntity living
                ? living
                : null;
        if (profile.total() <= 0.000001) {
            return new ProjectileArmorResult(damage, true, 0.0);
        }

        TypeDamage raw = new TypeDamage(
                damage * profile.thrust(),
                damage * profile.strike(),
                damage * profile.slash()
        );
        double rawTotal = raw.thrust() + raw.strike() + raw.slash();
        TypeDamage penetration = new TypeDamage(
                Math.ceil(raw.thrust() * 5.0),
                Math.ceil(raw.strike() * 5.0),
                Math.ceil(raw.slash() * 5.0)
        );
        ArmorResult result = applyArmorReductionAndDurability(
                null,
                target,
                List.of(detailedPart),
                raw,
                rawTotal,
                penetration
        );

        double postArmorMultiplier = EquipmentFallbackConfig.partDamageMultiplier(detailedPart)
                * injuryDamageMultiplier(target, detailedPart)
                * (attacker == null ? 1.0 : PassiveSkillPerks.damageMultiplier(
                        attacker,
                        attacker.getMainHandStack(),
                        false,
                        false,
                        detailedPart
                ));
        return new ProjectileArmorResult(
                (float) (result.damage() * postArmorMultiplier),
                result.penetrated(),
                result.reduction()
        );
    }

    public static void spawnProjectileImpactParticles(
            ServerWorld world,
            LivingEntity target,
            Entity projectile,
            ProjectileArmorResult result
    ) {
        String detailedPart = projectileHitDetails(target, projectile).detailedPart();
        playProjectileImpactSounds(world, target, projectile, detailedPart);
        Vec3d position = projectile.getPos();
        Vec3d velocity = projectile.getVelocity();
        Vec3d incomingDirection = velocity.lengthSquared() <= 0.000001
                ? target.getPos().subtract(position)
                : velocity.negate();
        if (!result.penetrated()) {
            spawnArmorSparks(world, target, position, incomingDirection);
            return;
        }

        Vec3d surfaceNormal = incomingDirection.lengthSquared() <= 0.000001
                ? new Vec3d(0.0, 0.35, 0.0)
                : incomingDirection.normalize();
        Vec3d impactDirection = velocity.lengthSquared() <= 0.000001
                ? surfaceNormal.negate()
                : velocity.normalize();
        double bloodScale = bloodParticleScale(result.armorReduction());
        double bloodTypeScale = bloodCutParticleScale(ProjectileCombatAttributesRegistry.get(projectile));
        int bloodDrops = scaledParticleCount(scaleBloodParticleBase(14, bloodTypeScale), bloodScale);
        int bloodSparks = scaledParticleCount(
                scaleBloodParticleBase((int) Math.ceil(BLOOD_SPARKS_PER_CONTACT * 1.5), bloodTypeScale),
                bloodScale
        );
        spawnBloodDrops(world, target, position, surfaceNormal, impactDirection, velocity, bloodDrops);
        for (int i = 0; i < bloodSparks; i++) {
            Vec3d sparkVelocity = randomizedBloodSparkDirection(impactDirection, surfaceNormal, target)
                    .multiply(0.13 + target.getRandom().nextDouble() * 0.15);
            Vec3d sparkPosition = position
                    .add(surfaceNormal.multiply(0.018))
                    .add(
                            (target.getRandom().nextDouble() - 0.5) * 0.038,
                            (target.getRandom().nextDouble() - 0.5) * 0.030,
                            (target.getRandom().nextDouble() - 0.5) * 0.038
                    );
            spawnDirectedParticle(
                    world,
                    ModParticles.BLOOD_SPARK,
                    sparkPosition.x,
                    sparkPosition.y,
                    sparkPosition.z,
                    sparkVelocity
            );
        }
    }

    public static void maybeApplyProjectileDamageInjury(
            LivingEntity target,
            Entity projectile,
            float damage
    ) {
        maybeApplyDamageInjury(target, projectileHitDetails(target, projectile).detailedPart(), damage);
    }

    private static MobCombatAttributesRegistry.ArmorSection naturalArmorSectionFor(List<String> possibleParts) {
        for (String part : possibleParts) {
            if (isHeadDetailedPart(part)) {
                return MobCombatAttributesRegistry.ArmorSection.HEAD;
            }
        }

        return MobCombatAttributesRegistry.ArmorSection.BODY;
    }

    private static boolean isHeadDetailedPart(String detailedPart) {
        return "face".equals(detailedPart)
                || "crown".equals(detailedPart)
                || "side_head".equals(detailedPart);
    }



    private static ProjectileHitDetails projectileHitDetails(LivingEntity target, Entity projectile) {
        Optional<ProjectilePartHit> tracedHit = traceProjectilePart(target, projectile);
        if (tracedHit.isPresent()) {
            ProjectilePartHit hit = tracedHit.get();
            String detailedPart = projectileDetailedPartForHit(target, projectile, hit);
            return new ProjectileHitDetails(hit.part(), detailedPart, hit.position());
        }

        String detailedPart;
        double height = Math.max(0.1, target.getHeight());
        double relativeY = (projectile.getY() - target.getY()) / height;
        Vec3d local = projectile.getPos().subtract(target.getPos());
        Vec3d axisX = AnimatedAttackHitboxLibrary.modelToWorld(
                new Vec3d(1.0, 0.0, 0.0),
                target.getBodyYaw()
        ).normalize();
        Vec3d axisZ = AnimatedAttackHitboxLibrary.modelToWorld(
                new Vec3d(0.0, 0.0, 1.0),
                target.getBodyYaw()
        ).normalize();
        double halfWidth = Math.max(0.18, target.getWidth() * 0.5);
        double localX = local.dotProduct(axisX) / halfWidth;
        double localZ = local.dotProduct(axisZ) / halfWidth;
        double absX = Math.abs(localX);

        if (relativeY >= 0.82) {
            if (relativeY >= 0.94) {
                detailedPart = "crown";
            } else if (localZ <= -0.30 && absX <= 0.52) {
                detailedPart = "face";
            } else {
                detailedPart = "side_head";
            }
            return new ProjectileHitDetails(HumanoidHurtboxLibrary.Part.HEAD, detailedPart, projectile.getPos());
        }

        if (relativeY >= 0.72) {
            return new ProjectileHitDetails(HumanoidHurtboxLibrary.Part.BODY, "neck", projectile.getPos());
        }

        if (relativeY >= 0.38) {
            if (relativeY >= 0.62 && absX >= 0.58) {
                detailedPart = "shoulder";
            } else if (absX >= 0.92) {
                detailedPart = relativeY <= 0.50 ? "hand" : "arm";
            } else if (relativeY >= 0.52) {
                detailedPart = "chest";
            } else {
                detailedPart = "abdomen";
            }
            return new ProjectileHitDetails(fallbackPartForDetailedPart(detailedPart), detailedPart, projectile.getPos());
        }

        if (relativeY >= 0.25) {
            detailedPart = "thigh";
        } else if (relativeY >= 0.16) {
            detailedPart = "knee";
        } else if (relativeY >= 0.06) {
            detailedPart = "calf";
        } else {
            detailedPart = "foot";
        }
        return new ProjectileHitDetails(HumanoidHurtboxLibrary.Part.LOWER, detailedPart, projectile.getPos());
    }

    private static String projectileDetailedPartForHit(
            LivingEntity target,
            Entity projectile,
            ProjectilePartHit hit
    ) {
        if (hit.part() != HumanoidHurtboxLibrary.Part.HEAD) {
            return HumanoidHurtboxLibrary.reportedHitResult(target, hit.part(), hit.position()).detailedPart();
        }

        Vec3d velocity = projectile.getVelocity();
        Vec3d incoming = velocity.lengthSquared() <= 0.000001
                ? projectile.getPos().subtract(target.getPos()).normalize()
                : velocity.normalize();
        AnimatedAttackHitboxLibrary.OrientedBox box = hit.box();
        Vec3d local = hit.position().subtract(box.center());
        double localX = normalizedAxisAmount(local, box.axisX(), box.halfExtents().x);
        double localY = normalizedAxisAmount(local, box.axisY(), box.halfExtents().y);
        double localZ = normalizedAxisAmount(local, box.axisZ(), box.halfExtents().z);
        double frontEntry = incoming.dotProduct(box.axisZ());

        if (localY >= 0.58) {
            return "crown";
        }

        if (frontEntry > 0.18 && localZ <= 0.35 && Math.abs(localX) <= 0.78) {
            return "face";
        }

        if (localZ <= -0.34 && Math.abs(localX) <= 0.82) {
            return "face";
        }

        return "side_head";
    }

    private static double normalizedAxisAmount(Vec3d local, Vec3d axis, double halfExtent) {
        if (halfExtent <= 0.000001) {
            return 0.0;
        }

        return local.dotProduct(axis) / halfExtent;
    }

    private static double projectileSpeedImpactMultiplier(Entity projectile) {
        double speed = projectile.getVelocity().length();
        return MathHelper.clamp(speed / 3.0, 0.55, 1.45);
    }

    private static Optional<ProjectilePartHit> traceProjectilePart(LivingEntity target, Entity projectile) {
        Vec3d end = projectile.getPos();
        Vec3d velocity = projectile.getVelocity();
        double speed = velocity.length();
        if (speed <= 0.000001) {
            return closestProjectilePart(target, end);
        }

        Vec3d direction = velocity.normalize();
        double traceDistance = Math.max(0.75, Math.min(3.0, speed + 0.35));
        Vec3d start = end.subtract(direction.multiply(traceDistance));

        ProjectilePartHit best = null;
        for (HumanoidHurtboxLibrary.PartBox hurtbox : HumanoidHurtboxLibrary.getHurtboxes(target)) {
            Optional<ProjectilePartHit> hit = intersectProjectileSegment(start, end, hurtbox);
            if (hit.isEmpty()) {
                continue;
            }

            if (best == null || hit.get().progress() < best.progress()) {
                best = hit.get();
            }
        }

        return Optional.ofNullable(best).or(() -> closestProjectilePart(target, end));
    }

    public static Optional<Vec3d> traceProjectileHead(
            LivingEntity target,
            Vec3d start,
            Vec3d end
    ) {
        ProjectilePartHit best = null;
        for (HumanoidHurtboxLibrary.PartBox hurtbox : HumanoidHurtboxLibrary.getHurtboxes(target)) {
            if (hurtbox.part() != HumanoidHurtboxLibrary.Part.HEAD) {
                continue;
            }
            Optional<ProjectilePartHit> hit = intersectProjectileSegment(start, end, hurtbox);
            if (hit.isPresent() && (best == null || hit.get().progress() < best.progress())) {
                best = hit.get();
            }
        }
        return best == null ? Optional.empty() : Optional.of(best.position());
    }

    private static Optional<ProjectilePartHit> closestProjectilePart(LivingEntity target, Vec3d point) {
        HumanoidHurtboxLibrary.PartBox best = null;
        Vec3d bestPoint = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (HumanoidHurtboxLibrary.PartBox hurtbox : HumanoidHurtboxLibrary.getHurtboxes(target)) {
            Vec3d closest = closestPointOnOrientedBox(hurtbox.box(), point);
            double distance = closest.squaredDistanceTo(point);
            if (distance < bestDistance) {
                best = hurtbox;
                bestPoint = closest;
                bestDistance = distance;
            }
        }

        if (best == null) {
            return Optional.empty();
        }

        return Optional.of(new ProjectilePartHit(best.part(), best.box(), bestPoint, 1.0));
    }

    private static Optional<ProjectilePartHit> intersectProjectileSegment(
            Vec3d start,
            Vec3d end,
            HumanoidHurtboxLibrary.PartBox hurtbox
    ) {
        AnimatedAttackHitboxLibrary.OrientedBox box = hurtbox.box();
        Vec3d delta = end.subtract(start);
        Vec3d relativeStart = start.subtract(box.center());
        double minProgress = 0.0;
        double maxProgress = 1.0;
        double margin = 0.05;

        double[] startAmounts = {
                relativeStart.dotProduct(box.axisX()),
                relativeStart.dotProduct(box.axisY()),
                relativeStart.dotProduct(box.axisZ())
        };
        double[] deltaAmounts = {
                delta.dotProduct(box.axisX()),
                delta.dotProduct(box.axisY()),
                delta.dotProduct(box.axisZ())
        };
        double[] extents = {
                box.halfExtents().x + margin,
                box.halfExtents().y + margin,
                box.halfExtents().z + margin
        };

        for (int i = 0; i < 3; i++) {
            if (Math.abs(deltaAmounts[i]) <= 0.000001) {
                if (startAmounts[i] < -extents[i] || startAmounts[i] > extents[i]) {
                    return Optional.empty();
                }
                continue;
            }

            double first = (-extents[i] - startAmounts[i]) / deltaAmounts[i];
            double second = (extents[i] - startAmounts[i]) / deltaAmounts[i];
            if (first > second) {
                double swap = first;
                first = second;
                second = swap;
            }

            minProgress = Math.max(minProgress, first);
            maxProgress = Math.min(maxProgress, second);
            if (minProgress > maxProgress) {
                return Optional.empty();
            }
        }

        Vec3d hitPosition = start.add(delta.multiply(minProgress));
        return Optional.of(new ProjectilePartHit(hurtbox.part(), hurtbox.box(), hitPosition, minProgress));
    }

    private static Vec3d closestPointOnOrientedBox(
            AnimatedAttackHitboxLibrary.OrientedBox box,
            Vec3d point
    ) {
        Vec3d local = point.subtract(box.center());
        double x = Math.max(-box.halfExtents().x, Math.min(box.halfExtents().x, local.dotProduct(box.axisX())));
        double y = Math.max(-box.halfExtents().y, Math.min(box.halfExtents().y, local.dotProduct(box.axisY())));
        double z = Math.max(-box.halfExtents().z, Math.min(box.halfExtents().z, local.dotProduct(box.axisZ())));
        return box.center()
                .add(box.axisX().multiply(x))
                .add(box.axisY().multiply(y))
                .add(box.axisZ().multiply(z));
    }

    private record ProjectilePartHit(
            HumanoidHurtboxLibrary.Part part,
            AnimatedAttackHitboxLibrary.OrientedBox box,
            Vec3d position,
            double progress
    ) {
    }

    private record ProjectileHitDetails(
            HumanoidHurtboxLibrary.Part part,
            String detailedPart,
            Vec3d position
    ) {
    }

    private static DamageTypeProfile resolveArmorDefense(
            ArmorCombatAttributes armor,
            ItemStack stack,
            LivingEntity target
    ) {
        double durabilityMultiplier = 1.0;

        if (stack.isDamageable() && stack.getMaxDamage() > 0) {
            durabilityMultiplier = armorDurabilityPanelMultiplier(stack);
        }
        if (durabilityMultiplier <= 0.0) {
            return DamageTypeProfile.even(0.0);
        }

        double staminaRatio = Math.max(0.0, Math.min(1.0, getStaminaRatio(target)));

        double lowReduction = EquipmentFallbackConfig.lowStaminaArmorPanelReduction();
        double exhaustedReduction = EquipmentFallbackConfig.exhaustedArmorPanelReduction();

        double staminaReduction;

        if (staminaRatio >= 0.5) {
            // 100% 体力 -> 50% 体力
            // reduction: 0 -> lowReduction
            double progress = (1.0 - staminaRatio) / 0.5;
            staminaReduction = lowReduction * progress;
        } else {
            // 50% 体力 -> 0% 体力
            // reduction: lowReduction -> exhaustedReduction
            double progress = (0.5 - staminaRatio) / 0.5;
            staminaReduction = lowReduction
                    + (exhaustedReduction - lowReduction) * progress;
        }

        staminaReduction = Math.max(0.0, Math.min(0.95, staminaReduction));
        double staminaMultiplier = Math.max(0.0, 1.0 - staminaReduction);

        DamageTypeProfile enchantDefense = enchantmentDefense(stack);

        return new DamageTypeProfile(
                (armor.defense().thrust() * durabilityMultiplier + enchantDefense.thrust())
                        * staminaMultiplier,
                (armor.defense().strike() * durabilityMultiplier + enchantDefense.strike())
                        * staminaMultiplier,
                (armor.defense().slash() * durabilityMultiplier + enchantDefense.slash())
                        * staminaMultiplier
        );
    }

    private static double armorDurabilityPanelMultiplier(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageable() || stack.getMaxDamage() <= 0) {
            return 1.0;
        }

        double remainingRatio = 1.0 - Math.max(
                0.0,
                Math.min(1.0, stack.getDamage() / (double) stack.getMaxDamage())
        );
        if (remainingRatio >= ARMOR_LOW_DURABILITY_THRESHOLD) {
            return 1.0;
        }

        return Math.max(0.0, remainingRatio / ARMOR_LOW_DURABILITY_THRESHOLD);
    }

    private static DamageTypeProfile resolveNaturalArmorDefense(
            LivingEntity target,
            MobCombatAttributes.NaturalArmor armor,
            MobCombatAttributesRegistry.ArmorSection section
    ) {
        double durabilityMultiplier = 1.0;
        if (armor.durability() > 0) {
            double damageRatio = MobCombatAttributesRegistry.armorDamage(target, section)
                    / (double) armor.durability();
            durabilityMultiplier = 1.0
                    - (1.0 - NATURAL_ARMOR_MIN_DURABILITY_PANEL_MULTIPLIER)
                    * Math.max(0.0, Math.min(1.0, damageRatio));
        }

        double staminaMultiplier = armorStaminaPanelMultiplier(target);
        return new DamageTypeProfile(
                armor.defense().thrust() * durabilityMultiplier * staminaMultiplier,
                armor.defense().strike() * durabilityMultiplier * staminaMultiplier,
                armor.defense().slash() * durabilityMultiplier * staminaMultiplier
        );
    }

    private static double armorPanelMultiplierFromBreach(LivingEntity attacker) {
        if (!(attacker instanceof PlayerEntity player)
                || !CombatItemUtil.isFightingMace(player.getMainHandStack())) {
            return 1.0;
        }

        int level = enchantmentLevel(player.getMainHandStack(), BREACH_ENCHANTMENT_ID);
        return Math.max(0.0, 1.0 - level * 0.05);
    }

    private static double armorStaminaPanelMultiplier(LivingEntity target) {
        double staminaRatio = Math.max(0.0, Math.min(1.0, getStaminaRatio(target)));
        double lowReduction = EquipmentFallbackConfig.lowStaminaArmorPanelReduction();
        double exhaustedReduction = EquipmentFallbackConfig.exhaustedArmorPanelReduction();
        double staminaReduction;
        if (staminaRatio >= 0.5) {
            double progress = (1.0 - staminaRatio) / 0.5;
            staminaReduction = lowReduction * progress;
        } else {
            double progress = (0.5 - staminaRatio) / 0.5;
            staminaReduction = lowReduction
                    + (exhaustedReduction - lowReduction) * progress;
        }

        return Math.max(0.0, 1.0 - Math.max(0.0, Math.min(0.95, staminaReduction)));
    }

    private static double attackImpact(LivingEntity attacker, ActiveServerAttack attack) {
        double actionMultiplier = attack.comboMove != null
                ? attack.comboMove.impact()
                : attack.moveConfig().impact();
        double weaponImpact = EquipmentCombatAttributesRegistry.getWeapon(attacker.getMainHandStack()).baseImpact();
        weaponImpact *= 1.0 + 0.10 * enchantmentLevel(attacker.getMainHandStack(), KNOCKBACK_ENCHANTMENT_ID);
        return weaponImpact * actionMultiplier;
    }

    private static double postArmorTargetEnchantmentDamage(
            LivingEntity attacker,
            LivingEntity target,
            boolean penetrated
    ) {
        if (!penetrated || attacker == null || target == null) {
            return 0.0;
        }

        ItemStack stack = attacker.getMainHandStack();
        double damage = 0.0;
        if (target.getType().isIn(EntityTypeTags.SENSITIVE_TO_SMITE)) {
            damage += enchantmentLevel(stack, SMITE_ENCHANTMENT_ID) * 1.25;
        }
        if (target.getType().isIn(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) {
            damage += enchantmentLevel(stack, BANE_OF_ARTHROPODS_ENCHANTMENT_ID) * 1.25;
        }

        return damage;
    }

    private static void maybeApplyDamageInjury(LivingEntity target, String detailedPart, float damage) {
        if (damage <= 5.0F) {
            return;
        }

        String injuryType = injuryTypeForDetailedPart(detailedPart);
        int rolls = Math.max(0, (int) Math.floor(damage - 6.0F));
        int injuryLevel = 1;
        for (int i = 0; i < rolls; i++) {
            if (target.getRandom().nextFloat() < 0.5F) {
                injuryLevel++;
            }
        }

        if (injuryLevel > 0) {
            ModStatusEffects.applyInjury(target, injuryType, injuryLevel);
        }
    }

    private static String injuryTypeForDetailedPart(String detailedPart) {
        return switch (detailedPart == null ? "" : detailedPart) {
            case "face", "neck", "crown", "side_head" -> "head";
            case "shoulder", "arm" -> "arm";
            case "hand" -> "hand";
            case "thigh", "knee", "calf", "foot" -> "legs";
            case "chest", "abdomen" -> "torso";
            default -> "torso";
        };
    }

    private static double injuryDamageMultiplier(LivingEntity target, String detailedPart) {
        int level = switch (injuryTypeForDetailedPart(detailedPart)) {
            case "head" -> ModStatusEffects.effectiveLevel(target, ModStatusEffects.HEAD_INJURY);
            case "arm" -> ModStatusEffects.effectiveLevel(target, ModStatusEffects.ARM_INJURY);
            case "hand" -> ModStatusEffects.effectiveLevel(target, ModStatusEffects.HAND_INJURY);
            case "legs" -> ModStatusEffects.effectiveLevel(target, ModStatusEffects.LEG_INJURY);
            default -> ModStatusEffects.effectiveLevel(target, ModStatusEffects.TORSO_INJURY);
        };

        return 1.0 + Math.max(0, level) * 0.02;
    }

    private static double reductionFromDefense(double penetration, double defense) {
        if (defense <= 0.0) {
            return 0.0;
        }

        double difference = penetration - defense;

        // 武器面板没有超过护甲面板：完全无法击穿。
        if (difference <= 0.0) {
            return 1.0;
        }

        // 武器面板只超过护甲 0~10 点：
        // 从 100% 减伤线性下降到 50% 减伤。
        if (difference <= 10.0) {
            return 1.0 - 0.5 * (difference / 10.0);
        }

        // 超过护甲 10 点以后：
        // 已经进入 50% 减伤区间，然后继续线性降低。
        double overAfterHalf = difference - 10.0;
        double fullPenetrationWindow = Math.max(10.0, defense);

        return Math.max(
                0.0,
                0.5 * (1.0 - overAfterHalf / fullPenetrationWindow)
        );
    }

    private static double armorDurabilityDamageMultiplier(LivingEntity attacker, LivingEntity target) {
        double weaponMultiplier = 1.0;
        if (attacker != null) {
            weaponMultiplier = EquipmentCombatAttributesRegistry
                    .getWeapon(attacker.getMainHandStack())
                    .armorBreakMultiplier();
        }

        HumanoidCombatAiProfile profile = HumanoidCombatAiProfiles.getProfile(target);
        double wornArmorMultiplier = profile == null
                ? 1.0
                : profile.wornArmorDurabilityMultiplier();
        double playerFacingMonsterMultiplier = target instanceof PlayerEntity && attacker instanceof MobEntity
                ? 5.0
                : 1.0;
        double playerArmorWearMultiplier = target instanceof PlayerEntity ? 2.0 : 1.0;
        return Math.max(0.0, weaponMultiplier * wornArmorMultiplier
                * playerFacingMonsterMultiplier * playerArmorWearMultiplier);
    }

    private static void damageArmorDurability(
            ItemStack stack,
            TypeDamage penetration,
            double durabilityMultiplier
    ) {
        if (durabilityMultiplier <= 0.0
                || stack.isEmpty()
                || !stack.isDamageable()
                || stack.getMaxDamage() <= 0) {
            return;
        }

        double durabilityDamage = (
                penetration.thrust() * 0.6
                        + penetration.strike()
                        + penetration.slash() * 0.4
        ) / 2.0 * ARMOR_DURABILITY_DAMAGE_MULTIPLIER;
        durabilityDamage *= durabilityEnchantmentMultiplier(stack);
        durabilityDamage *= durabilityMultiplier;
        int amount = (int) Math.ceil(durabilityDamage);
        int cap = Math.max(1, (int) Math.ceil(stack.getMaxDamage() * 0.35));
        amount = Math.min(amount, cap);
        if (amount <= 0) {
            return;
        }

        stack.setDamage(Math.min(stack.getMaxDamage(), stack.getDamage() + amount));
    }

    private static void damageNaturalArmorDurability(
            LivingEntity target,
            MobCombatAttributes.NaturalArmor armor,
            MobCombatAttributesRegistry.ArmorSection section,
            TypeDamage penetration
    ) {
        if (armor == null || armor.durability() <= 0) {
            return;
        }

        double durabilityDamage = (
                penetration.thrust() * 0.6
                        + penetration.strike()
                        + penetration.slash() * 0.4
        ) / 2.0 * ARMOR_DURABILITY_DAMAGE_MULTIPLIER;
        int amount = (int) Math.ceil(durabilityDamage);
        int cap = Math.max(1, (int) Math.ceil(armor.durability() * 0.35));
        amount = Math.min(amount, cap);
        if (amount > 0) {
            MobCombatAttributesRegistry.damageArmor(target, section, amount);
        }
    }

    private static void damageShieldDurability(
            LivingEntity blocker,
            TypeDamage penetration,
            boolean perfect
    ) {
        ItemStack stack = blocker.getOffHandStack();
        if (!EquipmentCombatAttributesRegistry.isShield(stack)
                || stack.isEmpty()
                || !stack.isDamageable()
                || stack.getMaxDamage() <= 0) {
            return;
        }

        int amount;
        if (perfect) {
            amount = 1;
        } else {
            double durabilityDamage = (
                    penetration.thrust() * 0.6
                            + penetration.strike()
                            + penetration.slash() * 0.4
            ) / 2.0 * ARMOR_DURABILITY_DAMAGE_MULTIPLIER;
            durabilityDamage *= durabilityEnchantmentMultiplier(stack);
            amount = (int) Math.ceil(durabilityDamage);
            int cap = Math.max(1, (int) Math.ceil(stack.getMaxDamage() * 0.35));
            amount = Math.min(amount, cap);
        }

        if (amount <= 0) {
            return;
        }

        stack.setDamage(Math.min(stack.getMaxDamage(), stack.getDamage() + amount));
    }

    private static void damageWeaponDurability(LivingEntity attacker, boolean penetrated) {
        damageHeldWeaponDurability(
                attacker,
                penetrated ? WEAPON_DURABILITY_PENETRATING_HIT : WEAPON_DURABILITY_NON_PENETRATING_HIT
        );
    }

    public static void damageHeldWeaponDurability(LivingEntity attacker, int amount) {
        ItemStack stack = attacker.getMainHandStack();
        if (amount <= 0 || stack.isEmpty() || !stack.isDamageable() || stack.getMaxDamage() <= 0) {
            return;
        }

        if (attacker instanceof PlayerEntity) {
            UUID uuid = attacker.getUuid();
            double accumulated = PLAYER_WEAPON_DURABILITY_REMAINDERS.getOrDefault(uuid, 0.0)
                    + amount * 0.5;
            amount = (int) Math.floor(accumulated);
            double remainder = accumulated - amount;
            if (remainder > 0.000001) {
                PLAYER_WEAPON_DURABILITY_REMAINDERS.put(uuid, remainder);
            } else {
                PLAYER_WEAPON_DURABILITY_REMAINDERS.remove(uuid);
            }
            if (amount <= 0) {
                return;
            }
        }
        stack.damage(amount, attacker, EquipmentSlot.MAINHAND);
    }

    private static DamageTypeProfile enchantmentDefense(ItemStack stack) {
        double thrust = 0.0;
        double strike = 0.0;
        double slash = 0.0;

        for (Map.Entry<Identifier, DamageTypeProfile> entry
                : EquipmentFallbackConfig.armorDefenseEnchantments().entrySet()) {
            int level = enchantmentLevel(stack, entry.getKey());
            if (level <= 0) {
                continue;
            }

            DamageTypeProfile profile = entry.getValue();
            thrust += profile.thrust() * level;
            strike += profile.strike() * level;
            slash += profile.slash() * level;
        }

        for (Map.Entry<Identifier, DamageTypeProfile> entry
                : EquipmentFallbackConfig.armorDefenseFirstTwoBonusEnchantments().entrySet()) {
            int level = enchantmentLevel(stack, entry.getKey());
            if (level <= 0) {
                continue;
            }

            int bonusLevels = Math.min(level, 2);
            DamageTypeProfile profile = entry.getValue();
            thrust += profile.thrust() * bonusLevels;
            strike += profile.strike() * bonusLevels;
            slash += profile.slash() * bonusLevels;
        }

        return new DamageTypeProfile(thrust, strike, slash);
    }

    private static double enchantmentImpactMitigation(ItemStack stack) {
        double mitigation = 0.0;
        for (Map.Entry<Identifier, Double> entry
                : EquipmentFallbackConfig.armorImpactMitigationEnchantments().entrySet()) {
            int level = enchantmentLevel(stack, entry.getKey());
            if (level <= 0) {
                continue;
            }

            mitigation += entry.getValue() * level;
        }

        return Math.max(0.0, Math.min(0.95, mitigation));
    }

    private static double durabilityEnchantmentMultiplier(ItemStack stack) {
        double reduction = 0.0;
        for (Map.Entry<Identifier, Double> entry
                : EquipmentFallbackConfig.durabilityReductionEnchantments().entrySet()) {
            int level = enchantmentLevel(stack, entry.getKey());
            if (level > 0) {
                reduction += entry.getValue() * level;
            }
        }

        reduction = Math.min(EquipmentFallbackConfig.maxDurabilityReduction(), reduction);
        return Math.max(0.0, 1.0 - reduction);
    }

    private static int enchantmentLevel(ItemStack stack, Identifier enchantmentId) {
        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) {
            return 0;
        }

        int level = 0;
        for (var entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesId(enchantmentId)) {
                level = Math.max(level, entry.getIntValue());
            }
        }

        return level;
    }

    private static List<String> detailedPartsFor(
            HumanoidHurtboxLibrary.Part hitPart,
            ActiveServerAttack attack
    ) {
        List<String> configuredParts = configuredDetailedPartsFor(hitPart, attack);
        if (!configuredParts.isEmpty()) {
            return configuredParts;
        }

        return switch (hitPart) {
            case HEAD -> List.of("face", "neck", "crown", "side_head");
            case SHOULDERS -> List.of("shoulder");
            case BODY -> List.of("shoulder", "chest", "abdomen", "arm", "hand");
            case LOWER -> List.of("thigh", "knee", "calf", "foot");
            case LEFT_ARM, RIGHT_ARM -> List.of("shoulder", "arm", "hand");
            case LEFT_LEG, RIGHT_LEG -> List.of("thigh", "knee", "calf", "foot");
        };
    }

    private static List<String> configuredDetailedPartsFor(
            HumanoidHurtboxLibrary.Part hitPart,
            ActiveServerAttack attack
    ) {
        List<AttackMoveConfig.HitZoneRule> rules = attack.comboMove != null
                ? attack.comboMove.hitZoneRules()
                : attack.moveConfig().hitZoneRules();
        return configuredDetailedPartsFor(hitPart, rules);
    }

    private static List<String> configuredDetailedPartsFor(
            HumanoidHurtboxLibrary.Part hitPart,
            List<AttackMoveConfig.HitZoneRule> rules
    ) {
        AttackMoveConfig.HitZone hitZone = hitZoneFromPart(hitPart);
        int bestPriority = Integer.MAX_VALUE;
        List<String> parts = new java.util.ArrayList<>();

        for (AttackMoveConfig.HitZoneRule rule : rules) {
            int priority = hitZoneMatchPriority(rule.whenHit(), hitZone);
            if (priority < 0 || rule.detailedParts().isEmpty()) {
                continue;
            }

            if (priority < bestPriority) {
                bestPriority = priority;
                parts.clear();
            }

            if (priority == bestPriority) {
                for (String part : rule.detailedParts()) {
                    if (!parts.contains(part)) {
                        parts.add(part);
                    }
                }
            }
        }

        return parts;
    }

    private static void bloodAttackerHeldItems(LivingEntity attacker, float damage) {
        double amount = Math.min(0.45, 0.08 + damage * 0.025)
                * CombatClientConfig.bloodStainPercent()
                * Math.max(0.0, BloodSplashConfig.multiplier(CUSTOM_COMBAT_DAMAGE))
                * 1.5;
        BloodiedEquipment.addBloodPercent(attacker.getMainHandStack(), amount);
        BloodiedEquipment.addBloodPercent(attacker.getOffHandStack(), amount * 0.75);
    }

    private static String detailedPartForHit(
            HumanoidHurtboxLibrary.HitResult hitResult,
            ActiveServerAttack attack
    ) {
        return detailedPartForHit(hitResult, attack, null);
    }

    private static String detailedPartForHit(
            HumanoidHurtboxLibrary.HitResult hitResult,
            ActiveServerAttack attack,
            List<AttackMoveConfig.HitZoneRule> overrideHitZoneRules
    ) {
        List<String> configuredParts = overrideHitZoneRules != null
                ? configuredDetailedPartsFor(hitResult.part(), overrideHitZoneRules)
                : configuredDetailedPartsFor(hitResult.part(), attack);
        if (!configuredParts.isEmpty()) {
            return configuredParts.getFirst();
        }

        return hitResult.detailedPart();
    }

    private static HumanoidHurtboxLibrary.Part reactionPartForDetailedPart(
            HumanoidHurtboxLibrary.Part fallback,
            String detailedPart
    ) {
        return switch (detailedPart) {
            case "face", "neck", "crown", "side_head" -> HumanoidHurtboxLibrary.Part.HEAD;
            case "shoulder" -> HumanoidHurtboxLibrary.Part.SHOULDERS;
            case "chest", "abdomen" -> HumanoidHurtboxLibrary.Part.BODY;
            case "arm", "hand" -> fallback == HumanoidHurtboxLibrary.Part.LEFT_ARM
                    || fallback == HumanoidHurtboxLibrary.Part.RIGHT_ARM
                    ? fallback
                    : HumanoidHurtboxLibrary.Part.BODY;
            case "thigh", "knee", "calf", "foot" -> fallback == HumanoidHurtboxLibrary.Part.LEFT_LEG
                    || fallback == HumanoidHurtboxLibrary.Part.RIGHT_LEG
                    ? fallback
                    : HumanoidHurtboxLibrary.Part.LOWER;
            default -> fallback;
        };
    }

    private static AttackMoveConfig.HitZone hitZoneFromPart(
            HumanoidHurtboxLibrary.Part hitPart
    ) {
        return switch (hitPart) {
            case HEAD -> AttackMoveConfig.HitZone.HEAD;
            case SHOULDERS -> AttackMoveConfig.HitZone.SHOULDERS;
            case BODY -> AttackMoveConfig.HitZone.BODY;
            case LEFT_ARM -> AttackMoveConfig.HitZone.LEFT_ARM;
            case RIGHT_ARM -> AttackMoveConfig.HitZone.RIGHT_ARM;
            case LOWER -> AttackMoveConfig.HitZone.LEGS;
            case LEFT_LEG -> AttackMoveConfig.HitZone.LEFT_LEG;
            case RIGHT_LEG -> AttackMoveConfig.HitZone.RIGHT_LEG;
        };
    }

    private static HumanoidHurtboxLibrary.Part directHitPartFor(ActiveServerAttack attack) {
        List<AttackMoveConfig.HitZoneRule> rules = attack.comboMove != null
                ? attack.comboMove.hitZoneRules()
                : attack.moveConfig().hitZoneRules();
        return directHitPartFor(rules);
    }

    private static String directHitHeightDetailedPart(
            LivingEntity attacker,
            LivingEntity target,
            ActiveServerAttack attack
    ) {
        if (attack.comboMove != null || attack.moveConfig().directHitHeightParts().isEmpty()) {
            return "";
        }

        double targetHeight = Math.max(0.1, target.getHeight());
        double hitY = attacker.getY() + attacker.getHeight() * 0.62;
        double relativeHeight = Math.max(0.0, Math.min(1.0, (hitY - target.getY()) / targetHeight));
        String fallback = "";
        for (AttackMoveConfig.HeightPartRule rule : attack.moveConfig().directHitHeightParts()) {
            if (!rule.detailedPart().isBlank()) {
                fallback = rule.detailedPart();
            }
            if (relativeHeight <= rule.maxHeight() && !rule.detailedPart().isBlank()) {
                return rule.detailedPart();
            }
        }
        return fallback;
    }

    private static HumanoidHurtboxLibrary.Part directHitPartFor(
            List<AttackMoveConfig.HitZoneRule> rules
    ) {
        if (rules.isEmpty()) {
            return HumanoidHurtboxLibrary.Part.BODY;
        }

        AttackMoveConfig.HitZone zone = rules.getFirst().whenHit();
        return switch (zone) {
            case HEAD, UPPER -> HumanoidHurtboxLibrary.Part.HEAD;
            case SHOULDERS -> HumanoidHurtboxLibrary.Part.SHOULDERS;
            case LEFT_ARM -> HumanoidHurtboxLibrary.Part.LEFT_ARM;
            case RIGHT_ARM -> HumanoidHurtboxLibrary.Part.RIGHT_ARM;
            case ARMS -> HumanoidHurtboxLibrary.Part.RIGHT_ARM;
            case LEFT_LEG -> HumanoidHurtboxLibrary.Part.LEFT_LEG;
            case RIGHT_LEG -> HumanoidHurtboxLibrary.Part.RIGHT_LEG;
            case LOWER, LEGS -> HumanoidHurtboxLibrary.Part.LOWER;
            case BODY, MIDDLE, ANY -> HumanoidHurtboxLibrary.Part.BODY;
        };
    }

    private static int hitZoneMatchPriority(
            AttackMoveConfig.HitZone rule,
            AttackMoveConfig.HitZone hit
    ) {
        if (rule == hit) {
            return 0;
        }

        if (rule == AttackMoveConfig.HitZone.ANY) {
            return 2;
        }

        boolean groupedMatch = switch (rule) {
            case UPPER -> hit == AttackMoveConfig.HitZone.HEAD;
            case MIDDLE -> hit == AttackMoveConfig.HitZone.SHOULDERS
                    || hit == AttackMoveConfig.HitZone.BODY
                    || hit == AttackMoveConfig.HitZone.LEFT_ARM
                    || hit == AttackMoveConfig.HitZone.RIGHT_ARM;
            case LOWER, LEGS -> hit == AttackMoveConfig.HitZone.LEGS
                    || hit == AttackMoveConfig.HitZone.LEFT_LEG
                    || hit == AttackMoveConfig.HitZone.RIGHT_LEG;
            case ARMS -> hit == AttackMoveConfig.HitZone.SHOULDERS
                    || hit == AttackMoveConfig.HitZone.LEFT_ARM
                    || hit == AttackMoveConfig.HitZone.RIGHT_ARM;
            default -> false;
        };

        return groupedMatch ? 1 : -1;
    }

    private static net.minecraft.entity.damage.DamageSource createDamageSource(
            ServerWorld world,
            LivingEntity attacker
    ) {
        if (attacker instanceof PlayerEntity player) {
            return world.getDamageSources().create(CUSTOM_COMBAT_DAMAGE, player);
        }

        return world.getDamageSources().create(CUSTOM_COMBAT_DAMAGE, attacker);
    }

    private static DamageSource vanillaHeavyHammerEnchantmentSource(
            ServerWorld world,
            LivingEntity attacker
    ) {
        if (attacker instanceof PlayerEntity player) {
            return world.getDamageSources().maceSmash(player);
        }

        return createDamageSource(world, attacker);
    }

    private static void cancelKnockback(LivingEntity target) {
        target.setVelocity(
                0.0,
                0.0,
                0.0
        );

        target.velocityModified = true;
    }

    private static boolean shouldSuppressVanillaHurtFeedback(LivingEntity target) {
        return target instanceof ServerPlayerEntity || HumanoidHurtboxLibrary.isHumanoidTarget(target);
    }

    private static void notifyReceivedAttack(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target
    ) {
        target.setAttacker(attacker);
    }

    private static void pushAway(
            LivingEntity pushed,
            LivingEntity from,
            double strength
    ) {
        Vec3d away = pushed.getPos().subtract(from.getPos());
        Vec3d horizontal = new Vec3d(away.x, 0.0, away.z);
        if (horizontal.lengthSquared() <= 0.000001) {
            return;
        }

        Vec3d push = horizontal.normalize().multiply(strength);
        Vec3d velocity = pushed.getVelocity();
        pushed.setVelocity(push.x, velocity.y, push.z);
        pushed.velocityModified = true;
    }

    private static void applyConfiguredHorizontalKnockback(
            LivingEntity attacker,
            LivingEntity target,
            ActiveServerAttack attack,
            boolean penetratedArmor
    ) {
        double strength = attack.comboMove != null
                ? attack.comboMove.horizontalKnockback()
                : attack.moveConfig().horizontalKnockback();
        if (!penetratedArmor) {
            strength *= 0.70;
        }
        if (strength <= 0.0) {
            return;
        }

        Vec3d away = target.getPos().subtract(attacker.getPos());
        Vec3d horizontal = new Vec3d(away.x, 0.0, away.z);
        if (horizontal.lengthSquared() <= 0.000001) {
            horizontal = new Vec3d(
                    -Math.sin(Math.toRadians(attacker.getYaw())),
                    0.0,
                    Math.cos(Math.toRadians(attacker.getYaw()))
            );
        }

        Vec3d knockback = horizontal.normalize().multiply(strength);
        Vec3d velocity = target.getVelocity();
        target.setVelocity(knockback.x, velocity.y, knockback.z);
        target.velocityModified = true;
    }

    private static void syncHitReaction(
            ServerWorld world,
            LivingEntity target,
            HumanoidHurtboxLibrary.Part part,
            String detailedPart,
            ActiveServerAttack attack,
            boolean strong
    ) {
        EntityHitReactionPayload payload = new EntityHitReactionPayload(
                target.getId(),
                part.ordinal(),
                detailedPart,
                attack.direction.ordinal(),
                strong,
                strong ? 1.75F : 1.0F,
                attack.comboMove != null ? attack.comboMove.hitReactionAnimation() : "",
                shouldPlayHitReaction(attack)
        );

        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(target) <= 64.0 * 64.0) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    private static void syncNonPenetratingHitFeedback(
            ServerWorld world,
            LivingEntity target,
            HumanoidHurtboxLibrary.Part part,
            ActiveServerAttack attack
    ) {
        if (shouldPlayHitReaction(attack)) {
            syncHitReaction(world, target, part, primaryDetailedPartFor(part, attack), attack, false);
        }

        if (target instanceof ServerPlayerEntity player) {
            clearPlayerVelocityForCustomHit(player);
            ServerPlayNetworking.send(
                    player,
                    new HitFeedbackPayload(attack.direction.ordinal(), false)
            );
        }
    }

    private static void clearPlayerVelocityForCustomHit(ServerPlayerEntity player) {
        player.setVelocity(Vec3d.ZERO);
        player.velocityModified = true;
    }

    public static void syncProjectileHitReaction(
            ServerWorld world,
            LivingEntity target,
            Entity projectile
    ) {
        Vec3d velocity = projectile.getVelocity();
        double speed = velocity.length();
        if (speed <= 0.05) {
            return;
        }

        String detailedPart = projectileHitDetails(target, projectile).detailedPart();
        HumanoidHurtboxLibrary.Part part = reactionPartForDetailedPart(
                fallbackPartForDetailedPart(detailedPart),
                detailedPart
        );
        CombatDirection direction = projectileReactionDirection(target, projectile, velocity);
        float strength = (float) Math.max(0.75, Math.min(2.75, speed * 1.05 * projectileSpeedImpactMultiplier(projectile)));
        EntityHitReactionPayload payload = new EntityHitReactionPayload(
                target.getId(),
                part.ordinal(),
                detailedPart,
                direction.ordinal(),
                strength >= 1.35F,
                strength,
                "",
                true
        );

        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(target) <= 64.0 * 64.0) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    public static void syncSuppressHurtOverlay(ServerWorld world, LivingEntity target) {
        EntitySuppressHurtOverlayPayload payload =
                new EntitySuppressHurtOverlayPayload(target.getId(), 8);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(target) <= 64.0 * 64.0) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    private static CombatDirection projectileReactionDirection(
            LivingEntity target,
            Entity projectile,
            Vec3d velocity
    ) {
        Vec3d incoming = velocity.lengthSquared() <= 0.000001
                ? projectile.getPos().subtract(target.getPos())
                : velocity.normalize().multiply(-1.0);
        Vec3d axisX = AnimatedAttackHitboxLibrary.modelToWorld(
                new Vec3d(1.0, 0.0, 0.0),
                target.getBodyYaw()
        ).normalize();

        double side = incoming.dotProduct(axisX);
        double vertical = incoming.y;
        if (Math.abs(vertical) >= 0.45 && Math.abs(vertical) > Math.abs(side) * 1.15) {
            return vertical >= 0.0 ? CombatDirection.UP : CombatDirection.DOWN;
        }

        if (Math.abs(side) >= 0.18) {
            return side >= 0.0 ? CombatDirection.RIGHT : CombatDirection.LEFT;
        }

        if (Math.abs(vertical) >= 0.18) {
            return vertical >= 0.0 ? CombatDirection.UP : CombatDirection.DOWN;
        }

        return CombatDirection.DOWN;
    }

    private static HumanoidHurtboxLibrary.Part fallbackPartForDetailedPart(String detailedPart) {
        return switch (detailedPart) {
            case "face", "neck", "crown", "side_head" -> HumanoidHurtboxLibrary.Part.HEAD;
            case "shoulder" -> HumanoidHurtboxLibrary.Part.SHOULDERS;
            case "chest", "abdomen" -> HumanoidHurtboxLibrary.Part.BODY;
            case "arm", "hand" -> HumanoidHurtboxLibrary.Part.RIGHT_ARM;
            case "thigh", "knee", "calf", "foot" -> HumanoidHurtboxLibrary.Part.RIGHT_LEG;
            default -> HumanoidHurtboxLibrary.Part.BODY;
        };
    }

    public static void syncAttackInterrupt(ServerWorld world, LivingEntity target) {
        EntityAttackInterruptPayload payload = new EntityAttackInterruptPayload(target.getId());
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(target) <= 64.0 * 64.0) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    private static void syncAttackImpact(ServerWorld world, LivingEntity attacker) {
        EntityAttackImpactPayload payload = new EntityAttackImpactPayload(attacker.getId());
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(attacker) <= 64.0 * 64.0) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    private static void syncBlockImpact(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity blocker
    ) {
        EntityAttackImpactPayload attackerPayload =
                new EntityAttackImpactPayload(attacker.getId());

        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(attacker) <= 64.0 * 64.0) {
                ServerPlayNetworking.send(player, attackerPayload);
            }
        }
    }

    private static void playBlockSound(
            ServerWorld world,
            LivingEntity blocker,
            BlockResult blockResult
    ) {
        if (blockResult.perfect()) {
            playCombatSound(world, blocker, ModSounds.BLOCK_PERFECT, 1.05F, 0.92F, 1.10F);
            playCombatSound(world, blocker, ModSounds.WEAPON_HIT_SWORD, 0.64F, 0.96F, 1.16F);
            return;
        }

        if (isBlockingWithShield(blocker)) {
            playCombatSound(world, blocker, ModSounds.BLOCK_SHIELD, 0.98F, 0.92F, 1.08F);
        } else if (isWoodenWeapon(blocker.getMainHandStack()) || isWoodenWeapon(blocker.getOffHandStack())) {
            playCombatSound(world, blocker, ModSounds.BLOCK_WOOD, 0.95F, 0.92F, 1.08F);
        } else {
            playCombatSound(world, blocker, ModSounds.BLOCK_WEAPON, 0.96F, 0.92F, 1.10F);
            playCombatSound(world, blocker, ModSounds.WEAPON_HIT_SWORD, 0.58F, 0.94F, 1.14F);
        }
    }

    private static void playArmorBlockedSound(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            ActiveServerAttack attack,
            String detailedPart,
            CombatDamageBreakdown damageBreakdown
    ) {
        if (isHelmetProtectedHit(target, detailedPart)) {
            playCombatSound(world, target, ModSounds.HIT_HELMET, 0.96F, 0.90F, 1.10F);
            return;
        }

        playCombatSound(world, target, ModSounds.HIT_ARMOR, 0.98F, 0.88F, 1.10F);
        playCombatSound(world, target, ModSounds.HIT_ARMOR_PLATE, 0.76F, 0.90F, 1.12F);
    }

    private static void playProjectileImpactSounds(
            ServerWorld world,
            LivingEntity target,
            Entity projectile,
            String detailedPart
    ) {
        if (!projectile.getType().isIn(EntityTypeTags.ARROWS)) {
            return;
        }

        playCombatSound(world, target, SoundEvents.ITEM_CROSSBOW_HIT, 1.15F, 0.92F, 1.08F);
        if (!isEquippedArmorProtectedHit(target, detailedPart)) {
            return;
        }

        if (isHelmetProtectedHit(target, detailedPart)) {
            playCombatSound(world, target, ModSounds.HIT_HELMET, 1.10F, 1.02F, 1.18F);
            return;
        }

        playCombatSound(world, target, ModSounds.HIT_ARMOR, 1.08F, 1.02F, 1.18F);
        playCombatSound(world, target, ModSounds.HIT_ARMOR_PLATE, 0.90F, 1.04F, 1.20F);
    }

    private static boolean isEquippedArmorProtectedHit(LivingEntity target, String detailedPart) {
        for (EquipmentSlot slot : EquipmentCombatAttributesRegistry.ARMOR_PANEL_SLOTS) {
            ItemStack stack = target.getEquippedStack(slot);
            if (!stack.isEmpty()
                    && EquipmentCombatAttributesRegistry.getArmor(stack)
                    .protectedParts()
                    .containsKey(detailedPart)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isHelmetProtectedHit(LivingEntity target, String detailedPart) {
        if (!("face".equals(detailedPart)
                || "neck".equals(detailedPart)
                || "crown".equals(detailedPart)
                || "side_head".equals(detailedPart))) {
            return false;
        }

        ItemStack helmet = target.getEquippedStack(EquipmentSlot.HEAD);
        if (helmet.isEmpty()) {
            return false;
        }

        return EquipmentCombatAttributesRegistry.getArmor(helmet)
                .protectedParts()
                .containsKey(detailedPart);
    }

    private static void playFleshHitSound(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            ActiveServerAttack attack,
            String detailedPart,
            CombatDamageBreakdown damageBreakdown
    ) {
        if (isSwordPommelAttack(attacker, attack)) {
            playCombatSound(world, target, ModSounds.HIT_POMMEL, 0.80F, 0.90F, 1.12F);
        }
        if (damageBreakdown.reduction() > 0.08) {
            playCombatSound(world, target, ModSounds.HIT_ARMOR, 0.34F, 0.92F, 1.12F);
            playCombatSound(world, target, ModSounds.HIT_ARMOR_PLATE, 0.26F, 0.94F, 1.14F);
        }
        boolean fightingMace = CombatItemUtil.isFightingMace(attacker.getMainHandStack());
        if (fightingMace) {
            playMaceBodySound(world, target);
        } else {
            if (isSlashOnChainmail(target, detailedPart, damageBreakdown)) {
                playCombatSound(world, target, ModSounds.HIT_CHAINMAIL_SCRAPE, 0.42F, 0.94F, 1.16F);
            }
            if (damageBreakdown.incomingThrust() > damageBreakdown.incomingStrike()
                    && damageBreakdown.incomingThrust() >= damageBreakdown.incomingSlash()) {
                playCombatSound(world, target, ModSounds.HIT_STAB_IN, 0.74F, 0.94F, 1.12F);
            }
        }
        playCombatSound(world, target, ModSounds.HIT_FLESH_1, 0.92F, 0.90F, 1.12F);
        playCombatSound(world, target, ModSounds.HIT_FLESH_2, 0.86F, 0.92F, 1.14F);
        playCombatSound(world, target, ModSounds.HIT_FLESH_ADD, 0.34F, 0.96F, 1.18F);
    }

    private static void playConfiguredMobArmorSound(ServerWorld world, LivingEntity target) {
        playCombatSound(world, target, ModSounds.HIT_ARMOR, 0.90F, 0.88F, 1.10F);
        playCombatSound(world, target, ModSounds.HIT_ARMOR_PLATE, 0.66F, 0.90F, 1.12F);
    }

    private static void playConfiguredMobFleshSound(ServerWorld world, LivingEntity target) {
        playCombatSound(world, target, ModSounds.HIT_FLESH_1, 0.82F, 0.90F, 1.12F);
        playCombatSound(world, target, ModSounds.HIT_FLESH_2, 0.74F, 0.92F, 1.14F);
    }

    private static void playConfiguredMobFleshSound(ServerWorld world, LivingEntity target, ItemStack weapon) {
        if (CombatItemUtil.isHeavyWeapon(weapon)) {
            playMaceBodySound(world, target);
        }

        playConfiguredMobFleshSound(world, target);
    }

    private static void playMaceBodySound(ServerWorld world, LivingEntity target) {
        playCombatSound(world, target, ModSounds.HIT_MACE_BODY, 0.92F, 0.90F, 1.12F);
    }

    private static boolean isSwordPommelAttack(LivingEntity attacker, ActiveServerAttack attack) {
        if (!CombatItemUtil.isSword(attacker.getMainHandStack())) {
            return false;
        }

        DamageTypeProfile move = attack.comboMove != null
                ? attack.comboMove.damageModifiers()
                : new DamageTypeProfile(
                        attack.moveConfig().thrustModifier(),
                        attack.moveConfig().strikeModifier(),
                        attack.moveConfig().slashModifier()
                );
        return move.strike() > move.thrust() && move.strike() >= move.slash();
    }

    private static boolean isSlashOnChainmail(
            LivingEntity target,
            String detailedPart,
            CombatDamageBreakdown damageBreakdown
    ) {
        return damageBreakdown.incomingSlash() > damageBreakdown.incomingThrust()
                && damageBreakdown.incomingSlash() >= damageBreakdown.incomingStrike()
                && hasChainmailProtection(target, detailedPart);
    }

    private static boolean hasChainmailProtection(LivingEntity target, String detailedPart) {
        for (EquipmentSlot slot : List.of(
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        )) {
            ItemStack stack = target.getEquippedStack(slot);
            if (!isChainmailArmor(stack)) {
                continue;
            }

            ArmorCombatAttributes armor = EquipmentCombatAttributesRegistry.getArmor(stack);
            if (armor.protectedParts().containsKey(detailedPart)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isChainmailArmor(ItemStack stack) {
        return stack.isOf(Items.CHAINMAIL_HELMET)
                || stack.isOf(Items.CHAINMAIL_CHESTPLATE)
                || stack.isOf(Items.CHAINMAIL_LEGGINGS)
                || stack.isOf(Items.CHAINMAIL_BOOTS);
    }

    private static void playCombatSound(
            ServerWorld world,
            LivingEntity source,
            SoundEvent sound,
            float volume,
            float minPitch,
            float maxPitch
    ) {
        float pitch = minPitch + source.getRandom().nextFloat() * Math.max(0.0F, maxPitch - minPitch);
        world.playSound(
                null,
                source.getX(),
                source.getY() + source.getHeight() * 0.55,
                source.getZ(),
                sound,
                SoundCategory.PLAYERS,
                volume,
                pitch
        );
    }

    public static void playWeaponClashEffect(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            Vec3d position,
            boolean perfect,
            String soundKey
    ) {
        Vec3d direction = target.getPos().subtract(attacker.getPos());
        spawnWeaponSparks(
                world,
                attacker,
                position,
                direction,
                scaledParticleCount(perfect ? 96 : 58, CombatClientConfig.sparkParticlePercent()),
                perfect ? 2.15 : 1.55
        );
        playCombatSound(
                world,
                attacker,
                resolveWeaponClashSound(soundKey, perfect),
                perfect ? 1.12F : 0.96F,
                perfect ? 1.04F : 0.92F,
                perfect ? 1.24F : 1.10F
        );
    }

    private static SoundEvent resolveWeaponClashSound(String soundKey, boolean perfect) {
        if (soundKey == null || soundKey.isBlank()) {
            return perfect ? ModSounds.BLOCK_PERFECT : ModSounds.BLOCK_WEAPON;
        }

        return switch (soundKey) {
            case "sword_hit_sword", "hit_sword", "combat.weapon_clash.hit_sword" -> ModSounds.WEAPON_HIT_SWORD;
            case "perfect", "block_perfect", "combat.block.perfect" -> ModSounds.BLOCK_PERFECT;
            case "weapon_clash", "sword_clash", "block_weapon", "combat.block.weapon" -> ModSounds.BLOCK_WEAPON;
            default -> perfect ? ModSounds.BLOCK_PERFECT : ModSounds.BLOCK_WEAPON;
        };
    }

    private static boolean isBlockingWithShield(LivingEntity entity) {
        return EquipmentCombatAttributesRegistry.isShield(entity.getOffHandStack());
    }

    private static boolean isWoodenWeapon(ItemStack stack) {
        return stack.isOf(Items.WOODEN_SWORD);
    }

    private static boolean applyImpactStaminaDamage(
            LivingEntity attacker,
            LivingEntity target,
            ActiveServerAttack attack,
            double impactMitigation,
            double blockedStrikeImpactBonus,
            boolean penetratedArmor
    ) {
        double impact = (attackImpact(attacker, attack) + blockedStrikeImpactBonus)
                * Math.max(0.0, 1.0 - impactMitigation);

        double toughness = HumanoidCombatAiProfiles.getToughness(target);
        double staminaDamage = Math.max(0.0, impact - toughness);

        if (staminaDamage > 0.0) {
            com.kingdomcomecombat.stamina.ServerStaminaState.damage(target, staminaDamage);
        }

        boolean exhausted = com.kingdomcomecombat.stamina.ServerStaminaState.getCurrent(target) <= 0.5;
        boolean toughnessInterrupt = CombatServerConfig.mobToughnessEnabled()
                ? exhausted || penetratedArmor
                : attacker instanceof PlayerEntity && target instanceof MobEntity;
        if (toughnessInterrupt
                && impact > toughness
                && ServerCombatState.getAttack(target.getUuid()) != null) {
            ServerCombatState.removeAttack(target.getUuid());
            ServerComboState.clear(target.getUuid());
            if (target instanceof MobEntity mob) {
                HumanoidCombatAiTicker.interruptFollowUps(mob);
            }
            syncAttackInterrupt((ServerWorld) target.getWorld(), target);
            return true;
        }

        return false;
    }

    private static void applyConfiguredMobImpactStaminaDamage(
            LivingEntity target,
            MobCombatAttributes attributes,
            ArmorResult armorResult
    ) {
        double impact = (attributes.meleeImpact() + armorResult.blockedStrikeImpactBonus())
                * Math.max(0.0, 1.0 - armorResult.impactMitigation());
        double toughness = HumanoidCombatAiProfiles.getToughness(target);
        double staminaDamage = Math.max(0.0, impact - toughness);

        if (staminaDamage > 0.0) {
            com.kingdomcomecombat.stamina.ServerStaminaState.damage(target, staminaDamage);
        }

        boolean exhausted = com.kingdomcomecombat.stamina.ServerStaminaState.getCurrent(target) <= 0.5;
        if ((exhausted || armorResult.penetrated())
                && impact > toughness
                && ServerCombatState.getAttack(target.getUuid()) != null) {
            ServerCombatState.removeAttack(target.getUuid());
            ServerComboState.clear(target.getUuid());
            if (target instanceof MobEntity mob) {
                HumanoidCombatAiTicker.interruptFollowUps(mob);
            }
            syncAttackInterrupt((ServerWorld) target.getWorld(), target);
        }
    }

    private static void spawnCustomHitParticles(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            HumanoidHurtboxLibrary.HitResult hitResult,
            String detailedPart,
            boolean damaged,
            float damage,
            Vec3d hitboxMotion
    ) {
        spawnCustomHitParticles(
                world,
                attacker,
                target,
                hitResult,
                detailedPart,
                damaged,
                damage,
                0.0,
                DamageTypeProfile.even(1.0),
                hitboxMotion
        );
    }

    private static void spawnCustomHitParticles(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            HumanoidHurtboxLibrary.HitResult hitResult,
            String detailedPart,
            boolean damaged,
            float damage,
            double armorReduction,
            DamageTypeProfile damageProfile,
            Vec3d hitboxMotion
    ) {
        Vec3d position = particlePositionFor(attacker, target, hitResult, detailedPart);

        if (!damaged) {
            spawnArmorSparks(world, target, position, preferredImpactDirection(
                    attacker,
                    target,
                    position,
                    hitboxMotion,
                    attacker.getEyePos().subtract(position)
            ));
            return;
        }

        int baseBloodDrops = Math.min(48, Math.max(4, (int) Math.ceil(Math.max(0.0F, damage) * 6.0F)));
        double bloodMultiplier = BloodSplashConfig.multiplier(CUSTOM_COMBAT_DAMAGE);
        double bloodScale = bloodParticleScale(armorReduction);
        double bloodTypeScale = bloodCutParticleScale(damageProfile);
        int bloodDrops = scaledParticleCount(
                scaleBloodParticleBase(
                        Math.max(2, Math.min(18, (int) Math.round(baseBloodDrops * 0.28 + 3.0))),
                        bloodTypeScale
                ),
                bloodScale
        );
        int bloodMist = Math.max(4, Math.min(12, baseBloodDrops / 7 + 4));
        int bloodSparks = scaledParticleCount(
                scaleBloodParticleBase(
                        Math.max(
                                24,
                                (int) Math.round(BLOOD_SPARKS_PER_CONTACT * bloodMultiplier)
                        ),
                        bloodTypeScale
                ),
                bloodScale
        );
        Vec3d surfaceNormal = surfaceNormalForHit(hitResult, detailedPart);
        Vec3d impactDirection = preferredImpactDirection(attacker, target, position, hitboxMotion, null);
        Vec3d fallbackImpactDirection = preferredImpactDirection(
                attacker,
                target,
                position,
                null,
                attacker.getEyePos().subtract(position)
        );
        Vec3d sprayDirection = bloodSprayDirection(attacker, target, position, surfaceNormal, fallbackImpactDirection);
        for (int i = 0; i < bloodMist; i++) {
            Vec3d velocity = randomizedSprayDirection(
                    sprayDirection,
                    surfaceNormal,
                    0.70,
                    0.30,
                    target
            ).multiply(0.017 + target.getRandom().nextDouble() * 0.020);
            world.spawnParticles(
                    ModParticles.BLOOD_MIST,
                    position.x,
                    position.y,
                    position.z,
                    0,
                    velocity.x,
                    velocity.y,
                    velocity.z,
                    1.0
            );
        }

        spawnBloodDrops(
                world,
                target,
                position,
                surfaceNormal,
                impactDirection,
                hitboxMotion,
                bloodDrops,
                hitResult
        );

        Vec3d bloodSparkDirection = bloodParticlePrimaryDirection(hitboxMotion, impactDirection);
        for (int i = 0; i < bloodSparks; i++) {
            boolean normalDriven = i % 2 == 0;
            Vec3d velocity = (normalDriven
                    ? randomizedBloodSparkNormalDirection(surfaceNormal, target)
                    : randomizedBloodSparkDirection(bloodSparkDirection, surfaceNormal, target))
                    .multiply(0.16 + target.getRandom().nextDouble() * 0.18);
            if (!normalDriven) {
                velocity = bloodSparkVelocityWithHitboxMotion(velocity, hitboxMotion, surfaceNormal, target);
            }
            Vec3d sparkPosition = position
                    .add(surfaceNormal.multiply(0.025))
                    .add(
                            (target.getRandom().nextDouble() - 0.5) * 0.045,
                            (target.getRandom().nextDouble() - 0.5) * 0.035,
                            (target.getRandom().nextDouble() - 0.5) * 0.045
                    );
            spawnDirectedParticle(
                    world,
                    ModParticles.BLOOD_SPARK,
                    sparkPosition.x,
                    sparkPosition.y,
                    sparkPosition.z,
                    velocity
            );
        }
    }

    private static void spawnSustainedBloodSparks(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            HumanoidHurtboxLibrary.HitResult hitResult,
            ActiveServerAttack attack,
            Vec3d hitboxMotion
    ) {
        String detailedPart = detailedPartForHit(hitResult, attack);
        Vec3d position = particlePositionFor(attacker, target, hitResult, detailedPart);
        Vec3d surfaceNormal = surfaceNormalForHit(hitResult, detailedPart);
        Vec3d impactDirection = preferredImpactDirection(attacker, target, position, hitboxMotion, null);
        Vec3d bloodSparkDirection = bloodParticlePrimaryDirection(hitboxMotion, impactDirection);
        double armorReduction = attack.targetArmorReductions.getOrDefault(target.getUuid(), 0.0);
        int bloodSparks = scaledParticleCount(
                scaleBloodParticleBase(
                        24,
                        bloodCutParticleScale(getAttackDamageProfile(attacker, attack))
                ),
                bloodParticleScale(armorReduction)
        );
        for (int i = 0; i < bloodSparks; i++) {
            boolean normalDriven = i % 2 == 0;
            Vec3d velocity = (normalDriven
                    ? randomizedBloodSparkNormalDirection(surfaceNormal, target)
                    : randomizedBloodSparkDirection(bloodSparkDirection, surfaceNormal, target))
                    .multiply(0.16 + target.getRandom().nextDouble() * 0.18);
            if (!normalDriven) {
                velocity = bloodSparkVelocityWithHitboxMotion(velocity, hitboxMotion, surfaceNormal, target);
            }
            Vec3d sparkPosition = position
                    .add(surfaceNormal.multiply(0.018))
                    .add(
                            (target.getRandom().nextDouble() - 0.5) * 0.038,
                            (target.getRandom().nextDouble() - 0.5) * 0.030,
                            (target.getRandom().nextDouble() - 0.5) * 0.038
                    );
            spawnDirectedParticle(
                    world,
                    ModParticles.BLOOD_SPARK,
                    sparkPosition.x,
                    sparkPosition.y,
                    sparkPosition.z,
                    velocity
            );
        }
    }

    private static void spawnBloodDrops(
            ServerWorld world,
            LivingEntity target,
            Vec3d position,
            Vec3d surfaceNormal,
            Vec3d impactDirection,
            Vec3d hitboxMotion,
            int bloodDrops
    ) {
        spawnBloodDrops(
                world,
                target,
                position,
                surfaceNormal,
                impactDirection,
                hitboxMotion,
                bloodDrops,
                null
        );
    }

    private static void spawnBloodDrops(
            ServerWorld world,
            LivingEntity target,
            Vec3d position,
            Vec3d surfaceNormal,
            Vec3d impactDirection,
            Vec3d hitboxMotion,
            int bloodDrops,
            HumanoidHurtboxLibrary.HitResult hitResult
    ) {
        Vec3d primaryDirection = bloodParticlePrimaryDirection(hitboxMotion, impactDirection);
        for (int i = 0; i < bloodDrops; i++) {
            Vec3d velocity = randomizedBloodDropDirection(primaryDirection, surfaceNormal, target)
                    .multiply(0.11 + target.getRandom().nextDouble() * 0.13);
            velocity = bloodDropVelocityWithHitboxMotion(velocity, hitboxMotion, surfaceNormal, target);
            Vec3d dropPosition = randomizedBloodDropPosition(position, surfaceNormal, target, hitResult);
            spawnDirectedParticle(
                    world,
                    ModParticles.BLOOD_DROP,
                    dropPosition.x,
                    dropPosition.y,
                    dropPosition.z,
                    velocity
            );
        }
    }

    private static double bloodParticleScale(double armorReduction) {
        double remainingBlood = 1.0 - MathHelper.clamp(armorReduction, 0.0, 1.0);
        return CombatClientConfig.bloodParticlePercent() * remainingBlood;
    }

    private static double bloodCutParticleScale(DamageTypeProfile profile) {
        if (profile == null) {
            return 1.0;
        }

        return MathHelper.clamp((profile.slash() + profile.thrust()) * 0.5, 0.30, 1.65);
    }

    private static int scaleBloodParticleBase(int baseCount, double typeScale) {
        return Math.max(1, (int) Math.round(baseCount * typeScale));
    }

    private static Vec3d bloodDropVelocityWithHitboxMotion(
            Vec3d velocity,
            Vec3d hitboxMotion,
            Vec3d surfaceNormal,
            LivingEntity target
    ) {
        if (hitboxMotion == null || hitboxMotion.lengthSquared() <= 0.000001) {
            return velocity;
        }

        Vec3d inheritedDirection = randomizedBloodDropDirection(
                hitboxMotion.normalize(),
                surfaceNormal,
                target
        );
        double speed = Math.min(0.22, hitboxMotion.length() * 0.060);
        return velocity.add(inheritedDirection.multiply(speed));
    }

    private static Vec3d bloodSparkVelocityWithHitboxMotion(
            Vec3d velocity,
            Vec3d hitboxMotion,
            Vec3d surfaceNormal,
            LivingEntity target
    ) {
        if (hitboxMotion == null || hitboxMotion.lengthSquared() <= 0.000001) {
            return velocity;
        }

        Vec3d inheritedDirection = randomizedBloodSparkDirection(
                hitboxMotion.normalize(),
                surfaceNormal,
                target
        );
        double speed = Math.min(0.28, hitboxMotion.length() * 0.105);
        return velocity.add(inheritedDirection.multiply(speed));
    }

    private static Vec3d bloodParticlePrimaryDirection(
            Vec3d hitboxMotion,
            Vec3d fallbackImpactDirection
    ) {
        if (hitboxMotion != null && hitboxMotion.lengthSquared() > 0.000001) {
            return hitboxMotion.normalize();
        }

        return fallbackImpactDirection;
    }

    private static <T extends net.minecraft.particle.ParticleEffect> void spawnDirectedParticle(
            ServerWorld world,
            T particle,
            double x,
            double y,
            double z,
            Vec3d velocity
    ) {
        double speed = velocity.length();
        if (speed <= 0.000001) {
            return;
        }
        Vec3d direction = velocity.normalize();
        world.spawnParticles(
                particle,
                x,
                y,
                z,
                0,
                direction.x,
                direction.y,
                direction.z,
                speed
        );
    }

    private static Vec3d randomizedBloodDropPosition(
            Vec3d position,
            Vec3d surfaceNormal,
            LivingEntity target,
            HumanoidHurtboxLibrary.HitResult hitResult
    ) {
        if (hitResult == null || hitResult.box() == null) {
            return position.add(surfaceNormal.multiply(target.getRandom().nextDouble() * 0.035));
        }

        AnimatedAttackHitboxLibrary.OrientedBox targetBox = hitResult.box();
        AnimatedAttackHitboxLibrary.OrientedBox attackBox = hitResult.attackBox();
        if (attackBox != null) {
            Box targetBounds = targetBox.candidateBox();
            Box attackBounds = attackBox.candidateBox();
            double minX = Math.max(targetBounds.minX, attackBounds.minX);
            double minY = Math.max(targetBounds.minY, attackBounds.minY);
            double minZ = Math.max(targetBounds.minZ, attackBounds.minZ);
            double maxX = Math.min(targetBounds.maxX, attackBounds.maxX);
            double maxY = Math.min(targetBounds.maxY, attackBounds.maxY);
            double maxZ = Math.min(targetBounds.maxZ, attackBounds.maxZ);
            for (int attempt = 0; attempt < 96 && minX <= maxX && minY <= maxY && minZ <= maxZ; attempt++) {
                Vec3d candidate = new Vec3d(
                        MathHelper.lerp(target.getRandom().nextDouble(), minX, maxX),
                        MathHelper.lerp(target.getRandom().nextDouble(), minY, maxY),
                        MathHelper.lerp(target.getRandom().nextDouble(), minZ, maxZ)
                );
                if (isPointInsideOrientedBox(targetBox, candidate)
                        && isPointInsideOrientedBox(attackBox, candidate)) {
                    return candidate;
                }
            }
        }

        return randomPointOnContactFace(targetBox, surfaceNormal, target);
    }

    private static boolean isPointInsideOrientedBox(
            AnimatedAttackHitboxLibrary.OrientedBox box,
            Vec3d point
    ) {
        Vec3d local = point.subtract(box.center());
        return Math.abs(local.dotProduct(box.axisX())) <= box.halfExtents().x + 0.000001
                && Math.abs(local.dotProduct(box.axisY())) <= box.halfExtents().y + 0.000001
                && Math.abs(local.dotProduct(box.axisZ())) <= box.halfExtents().z + 0.000001;
    }

    private static Vec3d randomPointOnContactFace(
            AnimatedAttackHitboxLibrary.OrientedBox box,
            Vec3d surfaceNormal,
            LivingEntity randomSource
    ) {
        Vec3d[] axes = {box.axisX(), box.axisY(), box.axisZ()};
        double[] halfExtents = {box.halfExtents().x, box.halfExtents().y, box.halfExtents().z};
        int normalAxis = 0;
        double strongestProjection = Math.abs(surfaceNormal.dotProduct(axes[0]));
        for (int axis = 1; axis < axes.length; axis++) {
            double projection = Math.abs(surfaceNormal.dotProduct(axes[axis]));
            if (projection > strongestProjection) {
                strongestProjection = projection;
                normalAxis = axis;
            }
        }

        Vec3d result = box.center();
        for (int axis = 0; axis < axes.length; axis++) {
            double coordinate;
            if (axis == normalAxis) {
                double sign = surfaceNormal.dotProduct(axes[axis]) < 0.0 ? -1.0 : 1.0;
                coordinate = sign * halfExtents[axis];
            } else {
                coordinate = (randomSource.getRandom().nextDouble() * 2.0 - 1.0) * halfExtents[axis];
            }
            result = result.add(axes[axis].multiply(coordinate));
        }
        return result.add(surfaceNormal.multiply(randomSource.getRandom().nextDouble() * 0.02));
    }

    private static void spawnArmorSparks(
            ServerWorld world,
            LivingEntity target,
            Vec3d position,
            Vec3d incomingDirection
    ) {
        spawnWeaponSparks(
                world,
                target,
                position,
                incomingDirection,
                scaledParticleCount(94, CombatClientConfig.sparkParticlePercent()),
                1.65
        );
    }

    private static void spawnWeaponSparks(
            ServerWorld world,
            LivingEntity target,
            Vec3d position,
            Vec3d incomingDirection,
            int sparks,
            double speedMultiplier
    ) {
        if (sparks <= 0) {
            return;
        }
        var random = target != null ? target.getRandom() : world.getRandom();
        Vec3d surfaceNormal = incomingDirection.lengthSquared() <= 0.000001
                ? new Vec3d(0.0, 0.35, 0.0)
                : incomingDirection.normalize();
        for (int i = 0; i < sparks; i++) {
            Vec3d velocity = randomizedSprayDirection(
                    surfaceNormal,
                    surfaceNormal,
                    3.20,
                    2.65,
                    random
            ).multiply((0.34 + random.nextDouble() * 0.56) * speedMultiplier * 0.10);
            velocity = velocity.add(0.0, -0.055 - random.nextDouble() * 0.055, 0.0);
            Vec3d sparkPosition = position
                    .add(surfaceNormal.multiply(0.055))
                    .add(
                            (random.nextDouble() - 0.5) * 0.055,
                            (random.nextDouble() - 0.5) * 0.040,
                            (random.nextDouble() - 0.5) * 0.055
                    );
            world.spawnParticles(
                    ModParticles.COMBAT_SPARK,
                    sparkPosition.x,
                    sparkPosition.y,
                    sparkPosition.z,
                    0,
                    velocity.x,
                    velocity.y,
                    velocity.z,
                    1.0
            );
        }
    }

    private static int scaledParticleCount(int baseCount, double percent) {
        if (baseCount <= 0 || percent <= 0.0) {
            return 0;
        }
        return Math.max(1, (int) Math.round(baseCount * percent));
    }

    private static Vec3d particlePositionFor(
            LivingEntity attacker,
            LivingEntity target,
            HumanoidHurtboxLibrary.HitResult hitResult,
            String detailedPart
    ) {
        Vec3d comboContactPosition = comboWeaponContactPosition(attacker, hitResult);
        if (comboContactPosition != null) {
            return comboContactPosition;
        }

        Vec3d surfaceNormal = surfaceNormalForHit(hitResult, detailedPart);
        return hitResult.position()
                .add(surfaceNormal.multiply(0.035))
                .add(0.0, target.getHeight() * 0.004, 0.0);
    }

    private static Vec3d comboWeaponContactPosition(
            LivingEntity attacker,
            HumanoidHurtboxLibrary.HitResult hitResult
    ) {
        ActiveServerAttack activeAttack = ServerCombatState.getAttack(attacker.getUuid());
        if (activeAttack == null || activeAttack.comboMove == null) {
            return null;
        }

        ActiveServerAttack attack = activeAttack;
        Optional<AnimatedAttackHitboxLibrary.SampledHitbox> weaponHitbox =
                sampleComboHitboxAt(attacker, attack, attack.getAnimationElapsedSeconds(), true);
        Optional<AnimatedAttackHitboxLibrary.SampledHitbox> presetHitbox =
                sampleComboHitboxAt(attacker, attack, attack.getAnimationElapsedSeconds(), false);
        if (weaponHitbox.isEmpty() || presetHitbox.isEmpty()) {
            return null;
        }

        AnimatedAttackHitboxLibrary.OrientedBox weaponBox = weaponHitbox.get().toWorldBox(attacker);
        AnimatedAttackHitboxLibrary.OrientedBox presetBox = presetHitbox.get().toWorldBox(attacker);
        Vec3d weaponPoint = closestPointOnBox(weaponBox, presetBox.center());
        Vec3d presetPoint = closestPointOnBox(presetBox, weaponPoint);
        Vec3d midpoint = weaponPoint.add(presetPoint).multiply(0.5);
        Vec3d targetPoint = closestPointOnBox(hitResult.box(), midpoint);
        return midpoint.add(targetPoint).multiply(0.5);
    }

    private static Vec3d closestPointOnBox(
            AnimatedAttackHitboxLibrary.OrientedBox box,
            Vec3d point
    ) {
        Vec3d local = point.subtract(box.center());
        return box.center()
                .add(box.axisX().multiply(MathHelper.clamp(
                        local.dotProduct(box.axisX()),
                        -box.halfExtents().x,
                        box.halfExtents().x
                )))
                .add(box.axisY().multiply(MathHelper.clamp(
                        local.dotProduct(box.axisY()),
                        -box.halfExtents().y,
                        box.halfExtents().y
                )))
                .add(box.axisZ().multiply(MathHelper.clamp(
                        local.dotProduct(box.axisZ()),
                        -box.halfExtents().z,
                        box.halfExtents().z
                )));
    }

    private static Vec3d surfaceNormalForHit(
            HumanoidHurtboxLibrary.HitResult hitResult,
            String detailedPart
    ) {
        AnimatedAttackHitboxLibrary.OrientedBox box = hitResult.box();
        Vec3d fromCenter = hitResult.position().subtract(box.center());
        return closestSurfaceNormal(box, fromCenter, detailedPart);
    }

    private static Vec3d closestSurfaceNormal(
            AnimatedAttackHitboxLibrary.OrientedBox box,
            Vec3d fromCenter,
            String detailedPart
    ) {
        Vec3d preferred = switch (detailedPart) {
            case "face" -> box.axisZ().multiply(-1.0);
            case "crown" -> box.axisY();
            default -> null;
        };
        if (preferred != null) {
            return preferred.normalize();
        }

        double x = Math.abs(normalizedProjection(fromCenter, box.axisX(), box.halfExtents().x));
        double y = Math.abs(normalizedProjection(fromCenter, box.axisY(), box.halfExtents().y));
        double z = Math.abs(normalizedProjection(fromCenter, box.axisZ(), box.halfExtents().z));
        if (x >= y && x >= z) {
            return signedAxis(box.axisX(), fromCenter);
        }
        if (y >= z) {
            return signedAxis(box.axisY(), fromCenter);
        }
        return signedAxis(box.axisZ(), fromCenter);
    }

    private static double normalizedProjection(Vec3d vector, Vec3d axis, double halfExtent) {
        if (halfExtent <= 0.000001) {
            return 0.0;
        }

        return vector.dotProduct(axis) / halfExtent;
    }

    private static Vec3d signedAxis(Vec3d axis, Vec3d fromCenter) {
        double sign = Math.signum(fromCenter.dotProduct(axis));
        return axis.multiply(sign == 0.0 ? 1.0 : sign).normalize();
    }

    private static Vec3d bloodSprayDirection(
            LivingEntity attacker,
            LivingEntity target,
            Vec3d hitPosition,
            Vec3d surfaceNormal,
            Vec3d impactDirection
    ) {
        Vec3d attackTravel = hitPosition.subtract(attacker.getEyePos());
        if (attackTravel.lengthSquared() <= 0.000001) {
            attackTravel = target.getPos().subtract(attacker.getPos());
        }
        attackTravel = attackTravel.lengthSquared() <= 0.000001
                ? surfaceNormal
                : attackTravel.normalize();

        return surfaceNormal
                .normalize()
                .multiply(0.62)
                .add(attackTravel.multiply(0.26))
                .add(impactDirection.multiply(0.74))
                .add(0.0, 0.10, 0.0)
                .normalize();
    }

    private static Vec3d preferredImpactDirection(
            LivingEntity attacker,
            LivingEntity target,
            Vec3d hitPosition,
            Vec3d hitboxMotion,
            Vec3d fallback
    ) {
        if (hitboxMotion != null && hitboxMotion.lengthSquared() > 0.000001) {
            return hitboxMotion.normalize();
        }
        if (fallback != null && fallback.lengthSquared() > 0.000001) {
            return fallback.normalize();
        }
        Vec3d fromAttacker = hitPosition.subtract(attacker.getEyePos());
        if (fromAttacker.lengthSquared() > 0.000001) {
            return fromAttacker.normalize();
        }
        Vec3d targetVelocity = target.getVelocity();
        if (targetVelocity.lengthSquared() > 0.000001) {
            return targetVelocity.normalize();
        }
        Vec3d fromAttackerBody = target.getPos().subtract(attacker.getPos());
        return fromAttackerBody.lengthSquared() <= 0.000001
                ? new Vec3d(0.0, 0.15, 0.0)
                : fromAttackerBody.normalize();
    }

    private static Vec3d randomizedSprayDirection(
            Vec3d sprayDirection,
            Vec3d surfaceNormal,
            double lateralRandomness,
            double verticalRandomness,
            LivingEntity target
    ) {
        return randomizedSprayDirection(
                sprayDirection,
                surfaceNormal,
                lateralRandomness,
                verticalRandomness,
                target.getRandom()
        );
    }

    private static Vec3d randomizedSprayDirection(
            Vec3d sprayDirection,
            Vec3d surfaceNormal,
            double lateralRandomness,
            double verticalRandomness,
            net.minecraft.util.math.random.Random randomSource
    ) {
        Vec3d random = new Vec3d(
                (randomSource.nextDouble() - 0.5) * lateralRandomness,
                (randomSource.nextDouble() - 0.35) * verticalRandomness,
                (randomSource.nextDouble() - 0.5) * lateralRandomness
        );
        Vec3d direction = sprayDirection
                .multiply(1.0 + randomSource.nextDouble() * 0.35)
                .add(surfaceNormal.multiply(randomSource.nextDouble() * 0.30))
                .add(random);
        if (direction.lengthSquared() <= 0.000001) {
            return sprayDirection;
        }
        return direction.normalize();
    }

    private static Vec3d randomizedBloodDropDirection(
            Vec3d primaryDirection,
            Vec3d surfaceNormal,
            LivingEntity target
    ) {
        net.minecraft.util.math.random.Random random = target.getRandom();
        Vec3d randomOffset = new Vec3d(
                (random.nextDouble() - 0.5) * 0.30,
                (random.nextDouble() - 0.35) * 0.12,
                (random.nextDouble() - 0.5) * 0.30
        );
        Vec3d normal = surfaceNormal.lengthSquared() <= 0.000001
                ? new Vec3d(0.0, 0.12, 0.0)
                : surfaceNormal.normalize();
        Vec3d direction = normal
                .multiply(0.72 + random.nextDouble() * 0.20)
                .add(primaryDirection.lengthSquared() <= 0.000001
                        ? Vec3d.ZERO
                        : primaryDirection.normalize().multiply(0.12 + random.nextDouble() * 0.08))
                .add(randomOffset);
        return direction.lengthSquared() <= 0.000001 ? normal : direction.normalize();
    }

    private static Vec3d randomizedBloodSparkNormalDirection(
            Vec3d surfaceNormal,
            LivingEntity target
    ) {
        net.minecraft.util.math.random.Random random = target.getRandom();
        Vec3d randomOffset = new Vec3d(
                (random.nextDouble() - 0.5) * 0.24,
                (random.nextDouble() - 0.45) * 0.10,
                (random.nextDouble() - 0.5) * 0.24
        );
        Vec3d normal = surfaceNormal.lengthSquared() <= 0.000001
                ? new Vec3d(0.0, 0.08, 0.0)
                : surfaceNormal.normalize();
        Vec3d direction = normal
                .multiply(0.84 + random.nextDouble() * 0.16)
                .add(randomOffset);
        return direction.lengthSquared() <= 0.000001 ? normal : direction.normalize();
    }

    private static Vec3d randomizedBloodSparkDirection(
            Vec3d primaryDirection,
            Vec3d surfaceNormal,
            LivingEntity target
    ) {
        net.minecraft.util.math.random.Random random = target.getRandom();
        Vec3d randomOffset = new Vec3d(
                (random.nextDouble() - 0.5) * 0.24,
                (random.nextDouble() - 0.45) * 0.10,
                (random.nextDouble() - 0.5) * 0.24
        );
        Vec3d normal = surfaceNormal.lengthSquared() <= 0.000001
                ? new Vec3d(0.0, 0.08, 0.0)
                : surfaceNormal.normalize();
        Vec3d direction = primaryDirection.lengthSquared() <= 0.000001
                ? normal
                : primaryDirection.normalize()
                        .multiply(0.74 + random.nextDouble() * 0.16)
                .add(normal.multiply(0.10 + random.nextDouble() * 0.09))
                .add(randomOffset);
        return direction.lengthSquared() <= 0.000001 ? normal : direction.normalize();
    }

    private static String primaryDetailedPartFor(
            HumanoidHurtboxLibrary.Part hitPart,
            ActiveServerAttack attack
    ) {
        List<String> parts = detailedPartsFor(hitPart, attack);
        return parts.isEmpty() ? fallbackDetailedPart(hitPart) : parts.getFirst();
    }

    private static String fallbackDetailedPart(HumanoidHurtboxLibrary.Part hitPart) {
        return switch (hitPart) {
            case HEAD -> "face";
            case SHOULDERS -> "shoulder";
            case BODY -> "chest";
            case LOWER -> "thigh";
            case LEFT_ARM, RIGHT_ARM -> "arm";
            case LEFT_LEG, RIGHT_LEG -> "calf";
        };
    }

    private static boolean canHit(
            LivingEntity attacker,
            LivingEntity target,
            ActiveServerAttack attack
    ) {
        if (!target.isAlive()) {
            return false;
        }

        if (target == attacker) {
            return false;
        }

        if (attack.hitTargets.contains(target.getUuid()) && !attack.bloodiedTargets.contains(target.getUuid())) {
            return false;
        }

        if (attacker instanceof MobEntity && attack.targetEntityId >= 0 && target.getId() != attack.targetEntityId) {
            return false;
        }

        if (attacker instanceof ServerPlayerEntity player
                && target instanceof ServerPlayerEntity
                && !player.getServer().isPvpEnabled()) {
            return false;
        }

        return true;
    }

    private static boolean canAttackBeDodged(ActiveServerAttack attack) {
        return attack.comboMove == null || !attack.comboMove.suctionCombo();
    }

    private static boolean isZombieLeader(LivingEntity entity) {
        return ZombieLeaderUtil.isLeader(entity);
    }

    private static BlockResult tryBlock(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            ActiveServerAttack attack
    ) {
        if (attack.comboMove != null) {
            return BlockResult.NONE;
        }

        if (!isAttackInFrontOfTarget(attacker, target)) {
            return BlockResult.NONE;
        }

        if (!ServerCombatControlState.canBlock(target)) {
            return BlockResult.NONE;
        }

        if (!canBlockWithMainHandOrOffhandShield(target)) {
            return BlockResult.NONE;
        }

        ActiveServerAttack targetAttack = ServerCombatState.getAttack(target.getUuid());
        if (targetAttack != null && isBlockingWithShield(target)) {
            return BlockResult.NONE;
        }
        if (targetAttack != null
                && com.kingdomcomecombat.combat.CombatTiming.isInAttackStartupNoDefenseWindow(
                        targetAttack.ageTicks
                )) {
            return BlockResult.NONE;
        }

        CombatDirection expectedDirection = expectedBlockDirection(attack.direction);
        ServerBlockState.BlockWindow window = ServerBlockState.get(target.getUuid());
        if (window != null) {
            ServerBlockState.clear(target.getUuid());
            double handDifficulty = Math.min(
                    0.95,
                    0.05 * ModStatusEffects.effectiveLevel(target, ModStatusEffects.HAND_INJURY)
            );
            boolean perfect = (ModGameRules.classicMode(world) || window.direction == expectedDirection)
                    && target.getRandom().nextDouble() >= handDifficulty;
            return perfect
                    ? BlockResult.perfect(expectedDirection)
                    : BlockResult.unperfect(window.direction);
        }

        if (canClassicNormalBlock(target, false)) {
            ServerBlockState.clear(target.getUuid());
            return BlockResult.unperfect(expectedDirection);
        }

        if (canPassiveShieldBlock(target, expectedDirection)) {
            return BlockResult.unperfect(expectedDirection);
        }

        HumanoidCombatAiProfile profile = HumanoidCombatAiProfiles.getProfile(target);
        if (profile == null || target instanceof PlayerEntity) {
            return BlockResult.NONE;
        }

        StanceMatch stanceMatch = stanceMatchForPerfectBlock(target, expectedDirection);
        double staminaRatio = getStaminaRatio(target);
        double blockChance = profile.blockChance();
        if (target instanceof net.minecraft.entity.mob.ZombieEntity && !isZombieLeader(target)) {
            blockChance = Math.min(blockChance, 0.70);
        } else {
            blockChance *= 0.20 + 0.80 * staminaRatio;
        }
        blockChance *= Math.max(
                0.0,
                1.0 - 0.05 * ModStatusEffects.effectiveLevel(target, ModStatusEffects.HAND_INJURY)
        );
        if (stanceMatch == StanceMatch.SAME_SIDE) {
            blockChance = Math.min(1.0, blockChance * 1.08);
        }
        double basePerfectBlockChance = switch (stanceMatch) {
            case SAME_SIDE -> profile.perfectBlockChance();
            case NEUTRAL_SIDE -> profile.perfectBlockChance() * 0.5;
            case OPPOSITE_SIDE -> 0.0;
        };
        if (isBlockingWithShield(target)) {
            blockChance = Math.min(1.0, blockChance * 2.0);
            basePerfectBlockChance = Math.min(1.0, basePerfectBlockChance * 2.0);
        }
        if (target instanceof net.minecraft.entity.mob.ZombieEntity && !isZombieLeader(target)) {
            basePerfectBlockChance = 0.0;
        }
        boolean canPerfectBlock = com.kingdomcomecombat.stamina.ServerStaminaState.getCurrent(target) >= 25.0;
        double perfectBlockChance = Math.min(
                1.0,
                Math.max(
                        0.0,
                        basePerfectBlockChance
                                + HumanoidCombatAiTicker.getDefensivePressureBonus(target)
                                - HumanoidCombatAiTicker.getPerfectBlockPenalty(target)
                )
        );
        if (profile.perfectBlockChance() < 0.20) {
            perfectBlockChance = 0.0;
        }
        if (!canPerfectBlock) {
            perfectBlockChance = 0.0;
        }
        if (HumanoidCombatAiTicker.shouldForcePerfectBlock(target)) {
            blockChance = 1.0;
            if (attack.comboMove == null && canPerfectBlock) {
                perfectBlockChance = 1.0;
            }
        }

        if (target.getRandom().nextDouble() > blockChance) {
            return BlockResult.NONE;
        }

        boolean perfect = attack.comboMove == null
                && target.getRandom().nextDouble() <= perfectBlockChance;
        if (perfect) {
            HumanoidCombatAiTicker.onPerfectBlockSucceeded(target);
            return BlockResult.perfect(expectedDirection);
        }

        HumanoidCombatAiTicker.onUnperfectBlock(target);
        return BlockResult.unperfect(expectedDirection);
    }

    private static BlockResult tryBlockExternal(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            CombatDirection expectedDirection,
            float amount
    ) {
        return tryBlockExternal(world, attacker, target, expectedDirection, amount, false);
    }

    private static BlockResult tryBlockExternal(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            CombatDirection expectedDirection,
            float amount,
            boolean shieldOnly
    ) {
        if (!isAttackInFrontOfTarget(attacker, target)
                || !ServerCombatControlState.canBlock(target)
                || !(shieldOnly ? canBlockWithOffhandShield(target) : canBlockWithMainHandOrOffhandShield(target))) {
            return BlockResult.NONE;
        }

        ActiveServerAttack targetAttack = ServerCombatState.getAttack(target.getUuid());
        if (targetAttack != null && isBlockingWithShield(target)) {
            return BlockResult.NONE;
        }
        if (targetAttack != null
                && com.kingdomcomecombat.combat.CombatTiming.isInAttackStartupNoDefenseWindow(
                targetAttack.ageTicks
        )) {
            return BlockResult.NONE;
        }

        ServerBlockState.BlockWindow window = ServerBlockState.get(target.getUuid());
        if (window != null) {
            ServerBlockState.clear(target.getUuid());
            double handDifficulty = Math.min(
                    0.95,
                    0.05 * ModStatusEffects.effectiveLevel(target, ModStatusEffects.HAND_INJURY)
            );
            boolean perfect = (ModGameRules.classicMode(world) || window.direction == expectedDirection)
                    && target.getRandom().nextDouble() >= handDifficulty;
            if (perfect) {
                consumeExternalBlockStamina(attacker, target, amount, true);
                return BlockResult.perfect(expectedDirection);
            }

            if (consumeExternalBlockStamina(attacker, target, amount, false)) {
                return BlockResult.unperfect(window.direction);
            }
            return BlockResult.NONE;
        }

        if (canClassicNormalBlock(target, shieldOnly)) {
            ServerBlockState.clear(target.getUuid());
            return consumeExternalBlockStamina(attacker, target, amount, false)
                    ? BlockResult.unperfect(expectedDirection)
                    : BlockResult.NONE;
        }

        if (canPassiveShieldBlock(target, expectedDirection)) {
            return consumeExternalBlockStamina(attacker, target, amount, false)
                    ? BlockResult.unperfect(expectedDirection)
                    : BlockResult.NONE;
        }

        HumanoidCombatAiProfile profile = HumanoidCombatAiProfiles.getProfile(target);
        if (profile == null || target instanceof PlayerEntity) {
            return BlockResult.NONE;
        }

        double shieldChanceMultiplier = isBlockingWithShield(target) ? 2.0 : 1.0;
        double blockChance = Math.min(
                1.0,
                profile.blockChance() * (0.20 + 0.80 * getStaminaRatio(target)) * shieldChanceMultiplier
        );
        if (target.getRandom().nextDouble() > blockChance) {
            return BlockResult.NONE;
        }

        StanceMatch stanceMatch = stanceMatchForPerfectBlock(target, expectedDirection);
        boolean perfect = profile.perfectBlockChance() >= 0.20
                && com.kingdomcomecombat.stamina.ServerStaminaState.getCurrent(target) >= 25.0
                && stanceMatch != StanceMatch.OPPOSITE_SIDE
                && target.getRandom().nextDouble() <= Math.min(
                        1.0,
                        profile.perfectBlockChance() * shieldChanceMultiplier
                )
                * (stanceMatch == StanceMatch.SAME_SIDE ? 1.0 : 0.5);
        consumeExternalBlockStamina(attacker, target, amount, perfect);
        if (perfect) {
            HumanoidCombatAiTicker.onPerfectBlockSucceeded(target);
            ServerCombatControlState.disableAttack(attacker.getUuid(), 14);
            return BlockResult.perfect(expectedDirection);
        }

        HumanoidCombatAiTicker.onUnperfectBlock(target);
        return BlockResult.unperfect(expectedDirection);
    }

    private static boolean consumeExternalBlockStamina(
            LivingEntity attacker,
            LivingEntity blocker,
            float amount,
            boolean perfect
    ) {
        double impact = Math.max(4.0, amount * 4.0);
        boolean consumed = true;
        if (!perfect) {
            double weaponMitigation = blockImpactMitigation(blocker);
            double cost = impact * (1.0 - weaponMitigation);
            cost *= EquipmentCombatAttributesRegistry.armorStaminaCostMultiplier(blocker);
            consumed = com.kingdomcomecombat.stamina.ServerStaminaState.consume(blocker, cost);
        }
        damageShieldDurability(blocker, new TypeDamage(0.0, amount, 0.0), perfect);
        if (!perfect
                && !consumed
                && EquipmentCombatAttributesRegistry.isLargeShield(blocker.getOffHandStack())) {
            ServerCombatStanceState.disableLargeShield(
                    blocker.getUuid(),
                    CombatControlConfig.LARGE_SHIELD_EXHAUSTED_DISABLE_TICKS
            );
            return true;
        }
        if (perfect) {
            com.kingdomcomecombat.stamina.ServerStaminaState.consume(
                    attacker,
                    perfectBlockAttackerImpact(blocker)
            );
        }
        return consumed || perfect;
    }

    private static boolean applyBlockStaminaAndDisable(
            LivingEntity attacker,
            LivingEntity blocker,
            ActiveServerAttack attack,
            BlockResult blockResult
    ) {
        TypeDamage incoming = incomingDamagePanel(attacker, blocker, attack, "body");
        if (blockResult.perfect()) {
            com.kingdomcomecombat.stamina.ServerStaminaState.consume(
                    attacker,
                    perfectBlockAttackerImpact(blocker)
            );
            damageShieldDurability(blocker, incoming, true);
            ServerCombatControlState.disableAttack(
                    attacker.getUuid(),
                    CombatControlConfig.PERFECT_BLOCK_ATTACK_DISABLE_TICKS
            );
            ServerCombatControlState.startPerfectCounterWindow(blocker.getUuid());
            ServerCombatStanceState.set(blocker.getUuid(), blockResult.direction());
            ServerCombatStanceState.set(blocker.getUuid(), CombatDirection.afterPerfectBlock(blockResult.direction()));
            return false;
        }

        double blockCost = attackImpact(attacker, attack)
                * (1.0 - blockImpactMitigation(blocker));
        blockCost *= EquipmentCombatAttributesRegistry.armorStaminaCostMultiplier(blocker);
        boolean consumed = com.kingdomcomecombat.stamina.ServerStaminaState.consume(
                blocker,
                blockCost
        );
        damageShieldDurability(blocker, incoming, false);
        if (!consumed && EquipmentCombatAttributesRegistry.isLargeShield(blocker.getOffHandStack())) {
            ServerCombatStanceState.disableLargeShield(
                    blocker.getUuid(),
                    CombatControlConfig.LARGE_SHIELD_EXHAUSTED_DISABLE_TICKS
            );
            ServerCombatControlState.disableAttack(
                    blocker.getUuid(),
                    CombatControlConfig.UNPERFECT_BLOCK_ATTACK_DISABLE_TICKS
            );
            return false;
        }
        ServerCombatControlState.disableAttack(
                blocker.getUuid(),
                CombatControlConfig.UNPERFECT_BLOCK_ATTACK_DISABLE_TICKS
        );
        return !consumed;
    }

    private static double getStaminaRatio(LivingEntity entity) {
        double max = com.kingdomcomecombat.stamina.ServerStaminaState.getMax(entity);
        if (max <= 0.0001) {
            return 0.0;
        }

        return Math.max(
                0.0,
                Math.min(1.0, com.kingdomcomecombat.stamina.ServerStaminaState.getCurrent(entity) / max)
        );
    }

    private static boolean canBlockWithMainHandOrOffhandShield(LivingEntity entity) {
        boolean canUseShield = isBlockingWithShield(entity)
                && (!EquipmentCombatAttributesRegistry.isLargeShield(entity.getOffHandStack())
                || ServerCombatStanceState.canUseLargeShield(entity.getUuid()));
        return EquipmentCombatAttributesRegistry.canBlockWithHeldItem(entity.getMainHandStack())
                || canUseShield;
    }

    private static boolean canBlockWithOffhandShield(LivingEntity entity) {
        return isBlockingWithShield(entity)
                && (!EquipmentCombatAttributesRegistry.isLargeShield(entity.getOffHandStack())
                || ServerCombatStanceState.canUseLargeShield(entity.getUuid()));
    }

    private static boolean canClassicNormalBlock(LivingEntity entity, boolean shieldOnly) {
        return entity instanceof PlayerEntity
                && ModGameRules.classicMode(entity)
                && ServerBlockState.getHold(entity.getUuid()) != null
                && (shieldOnly ? canBlockWithOffhandShield(entity) : canBlockWithMainHandOrOffhandShield(entity));
    }

    private static boolean canPassiveShieldBlock(
            LivingEntity entity,
            CombatDirection expectedDirection
    ) {
        if (!isBlockingWithShield(entity)) {
            return false;
        }

        if (EquipmentCombatAttributesRegistry.isLargeShield(entity.getOffHandStack())) {
            return ServerCombatStanceState.isLocked(entity.getUuid())
                    && ServerCombatStanceState.canUseLargeShield(entity.getUuid());
        }

        return currentStanceDirection(entity) == expectedDirection;
    }

    private static CombatDirection currentStanceDirection(LivingEntity entity) {
        if (entity instanceof PlayerEntity) {
            return ServerCombatStanceState.get(entity.getUuid());
        }

        return HumanoidCombatAiTicker.getCurrentDirection(entity);
    }

    private static double blockImpactMitigation(LivingEntity blocker) {
        double weaponMitigation = EquipmentCombatAttributesRegistry.getWeapon(
                blocker.getMainHandStack()
        ).blockImpactMitigation();
        if (isBlockingWithShield(blocker)) {
            return Math.max(
                    weaponMitigation,
                    EquipmentCombatAttributesRegistry.getShield(blocker.getOffHandStack()).blockImpactMitigation()
            );
        }
        return weaponMitigation;
    }

    private static double perfectBlockAttackerImpact(LivingEntity blocker) {
        double impact = EquipmentFallbackConfig.perfectBlockImpact();
        if (isBlockingWithShield(blocker)) {
            impact *= CombatControlConfig.SHIELD_PERFECT_BLOCK_ATTACKER_STAMINA_MULTIPLIER;
        }
        return impact;
    }

    private static StanceMatch stanceMatchForPerfectBlock(
            LivingEntity target,
            CombatDirection expectedDirection
    ) {
        HumanoidCombatAiProfile profile = HumanoidCombatAiProfiles.getProfile(target);
        if (profile == null || target instanceof PlayerEntity) {
            return StanceMatch.NEUTRAL_SIDE;
        }

        CombatDirection current = HumanoidCombatAiTicker.getCurrentDirection(target);
        if (current == expectedDirection) {
            return StanceMatch.SAME_SIDE;
        }

        return current == oppositeDirection(expectedDirection)
                ? StanceMatch.OPPOSITE_SIDE
                : StanceMatch.NEUTRAL_SIDE;
    }

    private static CombatDirection oppositeDirection(CombatDirection direction) {
        return switch (direction) {
            case LEFT -> CombatDirection.RIGHT;
            case RIGHT -> CombatDirection.LEFT;
            case UP -> CombatDirection.DOWN;
            case DOWN -> CombatDirection.UP;
        };
    }

    private static boolean isAttackInFrontOfTarget(LivingEntity attacker, LivingEntity target) {
        Vec3d toAttacker = attacker.getPos().subtract(target.getPos());
        Vec3d horizontal = new Vec3d(toAttacker.x, 0.0, toAttacker.z);
        if (horizontal.lengthSquared() <= 0.000001) {
            return true;
        }

        Vec3d forward = horizontalForward(target.getYaw());
        return forward.dotProduct(horizontal.normalize()) >= 0.5;
    }

    private static boolean isProjectileIncomingFromFront(ProjectileEntity projectile, LivingEntity target) {
        Vec3d velocity = projectile.getVelocity();
        Vec3d incoming = velocity.lengthSquared() <= 0.000001
                ? projectile.getPos().subtract(target.getPos())
                : velocity.multiply(-1.0);
        Vec3d horizontal = new Vec3d(incoming.x, 0.0, incoming.z);
        if (horizontal.lengthSquared() <= 0.000001) {
            return true;
        }

        Vec3d forward = horizontalForward(target.getYaw());
        return forward.dotProduct(horizontal.normalize()) >= 0.35;
    }

    private static CombatDirection inferProjectileBlockDirection(
            ProjectileEntity projectile,
            LivingEntity target
    ) {
        Vec3d velocity = projectile.getVelocity();
        Vec3d incoming = velocity.lengthSquared() <= 0.000001
                ? projectile.getPos().subtract(target.getPos())
                : velocity.multiply(-1.0);
        Vec3d horizontal = new Vec3d(incoming.x, 0.0, incoming.z);
        if (horizontal.lengthSquared() <= 0.000001) {
            return CombatDirection.UP;
        }

        Vec3d direction = horizontal.normalize();
        Vec3d forward = horizontalForward(target.getYaw());
        Vec3d right = new Vec3d(forward.z, 0.0, -forward.x).normalize();
        double side = right.dotProduct(direction);
        double front = forward.dotProduct(direction);
        if (Math.abs(side) > Math.abs(front) && Math.abs(side) > 0.35) {
            return side > 0.0 ? CombatDirection.RIGHT : CombatDirection.LEFT;
        }
        return CombatDirection.UP;
    }

    private static CombatDirection inferIncomingBlockDirection(LivingEntity attacker, LivingEntity target) {
        Vec3d toAttacker = attacker.getPos().subtract(target.getPos());
        Vec3d horizontal = new Vec3d(toAttacker.x, 0.0, toAttacker.z);
        if (horizontal.lengthSquared() <= 0.000001) {
            return CombatDirection.UP;
        }

        Vec3d incoming = horizontal.normalize();
        Vec3d forward = horizontalForward(target.getYaw());
        Vec3d right = new Vec3d(forward.z, 0.0, -forward.x).normalize();
        double side = right.dotProduct(incoming);
        double front = forward.dotProduct(incoming);
        if (Math.abs(side) > Math.abs(front) && Math.abs(side) > 0.35) {
            return side > 0.0 ? CombatDirection.RIGHT : CombatDirection.LEFT;
        }
        return front >= 0.0 ? CombatDirection.UP : CombatDirection.DOWN;
    }

    private static Vec3d horizontalForward(float yaw) {
        double rad = Math.toRadians(yaw);
        return new Vec3d(-Math.sin(rad), 0.0, Math.cos(rad)).normalize();
    }

    private static CombatDirection expectedBlockDirection(CombatDirection attackDirection) {
        return switch (attackDirection) {
            case RIGHT -> CombatDirection.LEFT;
            case LEFT -> CombatDirection.RIGHT;
            case UP -> CombatDirection.UP;
            case DOWN -> CombatDirection.DOWN;
        };
    }

    private static void syncBlockAnimation(
            ServerWorld world,
            LivingEntity target,
            LivingEntity attacker,
            BlockResult result
    ) {
        EntityBlockAnimationPayload payload = new EntityBlockAnimationPayload(
                target.getId(),
                result.animationType(),
                result.direction().ordinal(),
                attacker.getId()
        );

        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(target) <= 64.0 * 64.0) {
                ServerPlayNetworking.send(player, payload);
            }
        }
        if (result.perfect() && target instanceof ServerPlayerEntity player) {
            ServerPlayNetworking.send(
                    player,
                    new IncomingAttackWarningPayload(attacker.getId(), result.direction().ordinal(), 2)
            );
        }
    }

    private static void spawnTemporaryBlockParticles(
            ServerWorld world,
            Vec3d position,
            boolean perfect
    ) {
        spawnWeaponSparks(
                world,
                null,
                position,
                new Vec3d(0.0, 0.35, 0.0),
                scaledParticleCount(perfect ? 126 : 98, CombatClientConfig.sparkParticlePercent()),
                perfect ? 2.25 : 1.55
        );
    }

    private record SlimeHitKey(UUID attacker, UUID target) {
    }

    private record BlockResult(
            boolean blocked,
            boolean perfect,
            int animationType,
            CombatDirection direction
    ) {
        static final BlockResult NONE =
                new BlockResult(false, false, EntityBlockAnimationPayload.UNPERFECT_1, CombatDirection.RIGHT);

        static BlockResult perfect(CombatDirection direction) {
            return new BlockResult(true, true, EntityBlockAnimationPayload.PERFECT, direction);
        }

        static BlockResult unperfect(CombatDirection direction) {
            int variant = ServerBlockState.nextUnperfectVariant();
            int animationType = variant == 1
                    ? EntityBlockAnimationPayload.UNPERFECT_1
                    : EntityBlockAnimationPayload.UNPERFECT_2;
            return new BlockResult(true, false, animationType, direction);
        }
    }

    private enum StanceMatch {
        SAME_SIDE,
        NEUTRAL_SIDE,
        OPPOSITE_SIDE
    }

    private record ArmorResult(
            double damage,
            double impactMitigation,
            double reduction,
            double blockedStrikeImpactBonus,
            boolean penetrated
    ) {
    }

    private record TypeDamage(double thrust, double strike, double slash) {
    }

    private record DamageApplication(boolean damaged, float damage, double armorReduction) {
    }

    public record ProjectileArmorResult(float damage, boolean penetrated, double armorReduction) {
        static ProjectileArmorResult blocked() {
            return new ProjectileArmorResult(0.0F, false, 1.0);
        }
    }

    private record PendingClientHit(
            int targetEntityId,
            int partOrdinal,
            Vec3d hitPosition,
            boolean extraHeadHit,
            long attackInstanceId,
            long receivedWorldTick
    ) {
    }

    private record CombatDamageBreakdown(
            double damage,
            double incomingThrust,
            double incomingStrike,
            double incomingSlash,
            double impactMitigation,
            double reduction,
            double blockedStrikeImpactBonus,
            boolean penetrated
    ) {
    }
}
