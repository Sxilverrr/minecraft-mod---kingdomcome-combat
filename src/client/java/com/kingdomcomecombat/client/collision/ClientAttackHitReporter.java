package com.kingdomcomecombat.client.collision;

import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.client.animation.ClientHitReactionState;
import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.kingdomcomecombat.client.animation.GeckoLikeAnimationLibrary;
import com.kingdomcomecombat.client.config.ClientServerConfigState;
import com.kingdomcomecombat.client.collision.ClientItemHitboxCache;
import com.kingdomcomecombat.client.combat.CombatClientState;
import com.kingdomcomecombat.client.input.CombatInputClient;
import com.kingdomcomecombat.collision.AnimatedAttackHitboxLibrary;
import com.kingdomcomecombat.collision.HumanoidAnimationPoseLibrary;
import com.kingdomcomecombat.collision.HumanoidHurtboxLibrary;
import com.kingdomcomecombat.collision.ServerHitDetectionSystem;
import com.kingdomcomecombat.combat.AttackMoveConfig;
import com.kingdomcomecombat.combat.AttackMoveConfigs;
import com.kingdomcomecombat.combat.ComboMoveConfig;
import com.kingdomcomecombat.combat.ComboMoveConfigs;
import com.kingdomcomecombat.combat.CombatAnimationNames;
import com.kingdomcomecombat.config.CombatClientConfig;
import com.kingdomcomecombat.equipment.EquipmentCombatAttributesRegistry;
import com.kingdomcomecombat.network.ClientAttackHitPayload;
import com.kingdomcomecombat.network.ClientProjectileHitPayload;
import com.kingdomcomecombat.particle.ModParticles;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

public class ClientAttackHitReporter {
    private static final int MIN_SWEEP_STEPS = 5;
    private static final int MAX_SWEEP_STEPS = 32;
    private static final double SWEEP_STEP_LENGTH = 0.035;
    private static final Map<Integer, AnimatedAttackHitboxLibrary.OrientedBox> LAST_ENTITY_ATTACK_BOXES =
            new HashMap<>();
    private static final Map<Integer, Float> LAST_ENTITY_ATTACK_SECONDS = new HashMap<>();
    private static final Map<Integer, Set<Integer>> REPORTED_ENTITY_ATTACK_TARGETS = new HashMap<>();
    private static final Map<Integer, Vec3d> LAST_PROJECTILE_POSITIONS = new HashMap<>();
    private static final Map<Integer, Set<Integer>> REPORTED_PROJECTILE_TARGETS = new HashMap<>();
    private static boolean wasAttacking = false;
    private static int lastAttackTicks = -1;
    private static String lastAttackKey = "";
    private static AnimatedAttackHitboxLibrary.OrientedBox lastLocalAttackBox;
    private static AnimatedAttackHitboxLibrary.OrientedBox lastLocalSampledAttackBox;
    private static final Set<Integer> REPORTED_LOCAL_TARGETS = new HashSet<>();
    private static final Set<Integer> CONFIRMED_BLOOD_TRACE_TARGETS = new HashSet<>();
    private static final Map<Integer, PendingBloodTrace> PENDING_BLOOD_TRACES = new HashMap<>();

    private ClientAttackHitReporter() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientAttackHitReporter::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            LAST_ENTITY_ATTACK_BOXES.clear();
            LAST_ENTITY_ATTACK_SECONDS.clear();
            REPORTED_ENTITY_ATTACK_TARGETS.clear();
            LAST_PROJECTILE_POSITIONS.clear();
            REPORTED_PROJECTILE_TARGETS.clear();
            wasAttacking = false;
            lastLocalAttackBox = null;
            REPORTED_LOCAL_TARGETS.clear();
            CONFIRMED_BLOOD_TRACE_TARGETS.clear();
            PENDING_BLOOD_TRACES.clear();
            return;
        }

        reportEntityAttacks(client);
        reportProjectiles(client);

        if (!CombatClientState.attacking || CombatClientState.lastAttackDirection == null) {
            wasAttacking = false;
            lastAttackTicks = -1;
            lastAttackKey = "";
            lastLocalAttackBox = null;
            lastLocalSampledAttackBox = null;
            REPORTED_LOCAL_TARGETS.clear();
            CONFIRMED_BLOOD_TRACE_TARGETS.clear();
            PENDING_BLOOD_TRACES.clear();
            return;
        }

        String attackKey = currentAttackKey();
        if (!wasAttacking
                || CombatClientState.attackTicks <= 1
                || CombatClientState.attackTicks < lastAttackTicks
                || !attackKey.equals(lastAttackKey)) {
            lastLocalAttackBox = null;
            lastLocalSampledAttackBox = null;
            REPORTED_LOCAL_TARGETS.clear();
            CONFIRMED_BLOOD_TRACE_TARGETS.clear();
            PENDING_BLOOD_TRACES.clear();
            ClientItemHitboxCache.invalidate(client.player.getId());
        }
        wasAttacking = true;
        lastAttackTicks = CombatClientState.attackTicks;
        lastAttackKey = attackKey;

        Optional<AnimatedAttackHitboxLibrary.OrientedBox> sampledHitbox =
                sampleLocalAttackBox(client.player);
        if (sampledHitbox.isEmpty()) {
            return;
        }

        AnimatedAttackHitboxLibrary.OrientedBox attackBox =
                paddedAttackBox(sampledHitbox.get());
        AnimatedAttackHitboxLibrary.OrientedBox previousTraceBox =
                lastLocalAttackBox != null ? lastLocalAttackBox : lastLocalSampledAttackBox;
        AttackTrace attackTrace = buildAttackTrace(previousTraceBox, attackBox);
        lastLocalAttackBox = attackBox;
        lastLocalSampledAttackBox = attackBox;

        // Once this swing has connected, keep sampling the real overlap on
        // following frames. This makes the particles follow the cut across
        // individual animated limb boxes instead of sticking to the first hit.
        for (int targetId : List.copyOf(CONFIRMED_BLOOD_TRACE_TARGETS)) {
            if (client.world.getEntityById(targetId) instanceof LivingEntity tracedTarget
                    && tracedTarget.isAlive()) {
                getSweptHitResult(attackTrace, tracedTarget, false)
                        .ifPresent(hit -> spawnBloodTrace(
                                client, targetId, hit.hitResult().position(), attackTrace));
            }
        }

        Box candidateBox = attackTrace.candidateBox().expand(0.35);
        List<LivingEntity> candidates = client.world.getEntitiesByClass(
                LivingEntity.class,
                candidateBox,
                entity -> entity != client.player
                        && entity.isAlive()
                        && !REPORTED_LOCAL_TARGETS.contains(entity.getId())
        );

        for (LivingEntity target : candidates) {
            Optional<SweptHitResult> hitResult = getSweptHitResult(
                    attackTrace,
                    target,
                    localAttackPrefersHead()
            );
            if (hitResult.isEmpty()) {
                continue;
            }

            CombatInputClient.autoLockHitTarget(target);
            REPORTED_LOCAL_TARGETS.add(target.getId());
            com.kingdomcomecombat.client.feedback.CombatHitFeedbackClient
                    .startPredictedHitFeedback(CombatClientState.lastAttackDirection);
            ClientHitReactionState.start(
                    target.getId(),
                    hitResult.get().hitResult().part(),
                    "",
                    CombatClientState.lastAttackDirection,
                    false,
                    0.65F
            );
            Vec3d hitPosition = hitResult.get().hitResult().position();
            PENDING_BLOOD_TRACES.put(target.getId(), new PendingBloodTrace(hitPosition, attackTrace));
            ClientPlayNetworking.send(new ClientAttackHitPayload(
                    client.player.getId(),
                    target.getId(),
                    hitResult.get().hitResult().part().ordinal(),
                    hitPosition.x,
                    hitPosition.y,
                    hitPosition.z,
                    hitResult.get().extraHeadHit(),
                    CombatClientState.attackInstanceId
            ));
        }
    }

    private static void reportProjectiles(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            return;
        }
        if (!ClientServerConfigState.clientProjectileHurtboxEnabled()) {
            LAST_PROJECTILE_POSITIONS.clear();
            REPORTED_PROJECTILE_TARGETS.clear();
            return;
        }

        Box search = client.player.getBoundingBox().expand(96.0);
        List<PersistentProjectileEntity> projectiles = client.world.getEntitiesByClass(
                PersistentProjectileEntity.class,
                search,
                projectile -> projectile.isAlive()
                        && !projectile.isRemoved()
                        && projectile.getOwner() == client.player
        );
        // Avoid allocating and filling a temporary id set every client tick.
        // The world entity table is already the authoritative active-projectile index.
        LAST_PROJECTILE_POSITIONS.keySet().removeIf(id ->
                !(client.world.getEntityById(id) instanceof PersistentProjectileEntity projectile)
                        || !projectile.isAlive()
                        || projectile.isRemoved()
                        || projectile.getOwner() != client.player);
        REPORTED_PROJECTILE_TARGETS.keySet().removeIf(id ->
                !(client.world.getEntityById(id) instanceof PersistentProjectileEntity projectile)
                        || !projectile.isAlive()
                        || projectile.isRemoved()
                        || projectile.getOwner() != client.player);

        for (PersistentProjectileEntity projectile : projectiles) {
            Vec3d end = projectile.getPos();
            Vec3d velocity = projectile.getVelocity();
            if (velocity.lengthSquared() <= 1.0E-8) {
                LAST_PROJECTILE_POSITIONS.put(projectile.getId(), end);
                continue;
            }

            Vec3d fallbackStart = end.subtract(
                    velocity.normalize().multiply(Math.max(0.75, Math.min(3.0, velocity.length() + 0.35)))
            );
            Vec3d start = LAST_PROJECTILE_POSITIONS.getOrDefault(projectile.getId(), fallbackStart);
            LAST_PROJECTILE_POSITIONS.put(projectile.getId(), end);
            if (start.squaredDistanceTo(end) <= 1.0E-8) {
                continue;
            }

            Set<Integer> reported = REPORTED_PROJECTILE_TARGETS.computeIfAbsent(
                    projectile.getId(), ignored -> new HashSet<>());
            Box candidateBox = new Box(start, end).expand(1.5);
            for (LivingEntity target : client.world.getEntitiesByClass(
                    LivingEntity.class,
                    candidateBox,
                    entity -> entity.isAlive()
                            && entity != client.player
                            && !reported.contains(entity.getId())
            )) {
                if (target.getBoundingBox().expand(0.1).raycast(start, end).isPresent()) {
                    continue;
                }

                Optional<Vec3d> hit = ServerHitDetectionSystem.traceProjectileHurtbox(target, start, end);
                if (hit.isEmpty()) {
                    continue;
                }

                reported.add(target.getId());
                ClientPlayNetworking.send(new ClientProjectileHitPayload(
                        projectile.getId(),
                        target.getId(),
                        hit.get().x,
                        hit.get().y,
                        hit.get().z
                ));
            }
        }
    }

    public static void confirmBloodTrace(int targetEntityId) {
        if (REPORTED_LOCAL_TARGETS.contains(targetEntityId)) {
            CONFIRMED_BLOOD_TRACE_TARGETS.add(targetEntityId);
            MinecraftClient client = MinecraftClient.getInstance();
            PendingBloodTrace pending = PENDING_BLOOD_TRACES.remove(targetEntityId);
            if (pending != null) {
                spawnBloodTrace(client, targetEntityId, pending.position(), pending.trace());
                if (client.world != null
                        && client.world.getEntityById(targetEntityId) instanceof LivingEntity target) {
                    spawnConfirmedBloodBurst(client, pending.position(), target);
                }
            }
        }
    }

    private static void reportEntityAttacks(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            return;
        }

        Set<Integer> activeAttackIds = ClientEntityGeckoAnimationState.getActiveAttackEntityIds();
        LAST_ENTITY_ATTACK_BOXES.keySet().removeIf(id -> !activeAttackIds.contains(id));
        LAST_ENTITY_ATTACK_SECONDS.keySet().removeIf(id -> !activeAttackIds.contains(id));
        REPORTED_ENTITY_ATTACK_TARGETS.keySet().removeIf(id -> !activeAttackIds.contains(id));

        for (int entityId : activeAttackIds) {
            if (!(client.world.getEntityById(entityId) instanceof LivingEntity attacker)
                    || attacker == client.player) {
                LAST_ENTITY_ATTACK_BOXES.remove(entityId);
                LAST_ENTITY_ATTACK_SECONDS.remove(entityId);
                continue;
            }

            if (attacker.squaredDistanceTo(client.player) > ClientCollisionTrackingPolicy.radiusSquared()) {
                LAST_ENTITY_ATTACK_BOXES.remove(entityId);
                LAST_ENTITY_ATTACK_SECONDS.remove(entityId);
                continue;
            }

            ClientEntityGeckoAnimationState.ActiveAnimation animation =
                    ClientEntityGeckoAnimationState.getAttackLayer(entityId);
            if (animation == null) {
                LAST_ENTITY_ATTACK_BOXES.remove(entityId);
                LAST_ENTITY_ATTACK_SECONDS.remove(entityId);
                continue;
            }

            if (animation.elapsedSeconds() <= 0.05F) {
                LAST_ENTITY_ATTACK_BOXES.remove(entityId);
                LAST_ENTITY_ATTACK_SECONDS.remove(entityId);
                REPORTED_ENTITY_ATTACK_TARGETS.remove(entityId);
            }

            Optional<AnimatedAttackHitboxLibrary.OrientedBox> sampledHitbox =
                    sampleEntityAttackBox(attacker, animation);
            if (sampledHitbox.isEmpty()) {
                continue;
            }

            AnimatedAttackHitboxLibrary.OrientedBox attackBox =
                    paddedAttackBox(sampledHitbox.get());
            AnimatedAttackHitboxLibrary.OrientedBox previousBox =
                    getPreviousEntityAttackBox(entityId, animation.elapsedSeconds());
            AttackTrace attackTrace = buildAttackTrace(previousBox, attackBox);
            LAST_ENTITY_ATTACK_BOXES.put(entityId, attackBox);
            LAST_ENTITY_ATTACK_SECONDS.put(entityId, animation.elapsedSeconds());

            Set<Integer> reported = REPORTED_ENTITY_ATTACK_TARGETS.computeIfAbsent(
                    entityId, ignored -> new HashSet<>());
            Box candidatesBox = attackTrace.candidateBox().expand(0.35);
            for (LivingEntity target : client.world.getEntitiesByClass(
                    LivingEntity.class,
                    candidatesBox,
                    entity -> entity != attacker && entity.isAlive() && !reported.contains(entity.getId())
            )) {
                if (!isLocalReporter(client, attacker, target)) continue;
                Optional<SweptHitResult> hitResult = getSweptHitResult(attackTrace, target, false, true);
                if (hitResult.isEmpty()) continue;
                reported.add(target.getId());
                Vec3d hitPosition = hitResult.get().hitResult().position();
                ClientPlayNetworking.send(new ClientAttackHitPayload(
                        entityId, target.getId(), hitResult.get().hitResult().part().ordinal(),
                        hitPosition.x, hitPosition.y, hitPosition.z,
                        hitResult.get().extraHeadHit(), 0L
                ));
            }
        }
    }

    private static boolean isLocalReporter(
            MinecraftClient client,
            LivingEntity attacker,
            LivingEntity target
    ) {
        if (client.player == null || client.world == null) return false;

        // PvP is always owned by the attacking player's client. PvE attacks
        // against a player are owned by that attacked player's client.
        if (attacker instanceof PlayerEntity) return attacker == client.player;
        if (target instanceof PlayerEntity) return target == client.player;

        // For entity-vs-entity combat all clients deterministically choose the
        // player nearest to the interaction midpoint. Entity id breaks ties.
        Vec3d midpoint = attacker.getPos().add(target.getPos()).multiply(0.5);
        PlayerEntity owner = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (PlayerEntity player : client.world.getPlayers()) {
            double distance = player.squaredDistanceTo(midpoint);
            if (distance < bestDistance - 1.0E-7
                    || (Math.abs(distance - bestDistance) <= 1.0E-7
                    && (owner == null || player.getId() < owner.getId()))) {
                owner = player;
                bestDistance = distance;
            }
        }
        return owner == client.player && bestDistance <= 32.0 * 32.0;
    }

    private static void spawnConfirmedBloodBurst(
            MinecraftClient client,
            Vec3d position,
            LivingEntity target
    ) {
        if (client.world == null) return;
        Vec3d away = position.subtract(target.getPos().add(0.0, target.getHeight() * 0.5, 0.0));
        if (away.lengthSquared() < 1.0E-6) away = new Vec3d(0.0, 0.1, 0.0);
        away = away.normalize();
        for (int i = 0; i < 8; i++) {
            double spreadX = (client.world.random.nextDouble() - 0.5) * 0.12;
            double spreadY = client.world.random.nextDouble() * 0.10;
            double spreadZ = (client.world.random.nextDouble() - 0.5) * 0.12;
            client.world.addParticleClient(ModParticles.BLOOD_SPARK,
                    position.x, position.y, position.z,
                    away.x * 0.16 + spreadX, away.y * 0.16 + spreadY, away.z * 0.16 + spreadZ);
        }
        client.world.addParticleClient(ModParticles.BLOOD_MIST,
                position.x, position.y, position.z,
                away.x * 0.025, 0.015, away.z * 0.025);
    }

    private static void spawnBloodTrace(
            MinecraftClient client,
            int targetEntityId,
            Vec3d position,
            AttackTrace trace
    ) {
        if (client.world == null) return;
        position = nearestModelSurfacePoint(client, targetEntityId, position).orElse(position);
        AnimatedAttackHitboxLibrary.OrientedBox traceStart = trace.boxes().getFirst();
        AnimatedAttackHitboxLibrary.OrientedBox traceEnd = trace.boxes().getLast();
        Vec3d cutDirection = traceEnd.center().subtract(traceStart.center());
        if (cutDirection.lengthSquared() < 1.0E-6) {
            cutDirection = traceEnd.axisY();
        }
        cutDirection = cutDirection.normalize();
        double lineLength = Math.min(0.95, Math.max(0.22, traceEnd.center().distanceTo(traceStart.center())));
        int count = Math.max(0, (int) Math.round(38.0 * CombatClientConfig.bloodTraceParticlePercent()));
        for (int i = 0; i < count; i++) {
            double progress = count <= 1 ? 0.5 : i / (double) (count - 1);
            double centered = progress - 0.5;
            Vec3d random = new Vec3d(
                    client.world.random.nextDouble() - 0.5,
                    client.world.random.nextDouble() - 0.5,
                    client.world.random.nextDouble() - 0.5
            ).multiply(0.010);
            Vec3d particlePosition = position.add(cutDirection.multiply(centered * lineLength)).add(random);
            Vec3d velocity = cutDirection.multiply(
                    (0.010 + client.world.random.nextDouble() * 0.010) * 0.07
            ).add(random.multiply(0.07));
            client.world.addParticleClient(ModParticles.BLOOD_TRACE,
                    particlePosition.x, particlePosition.y, particlePosition.z,
                    velocity.x, velocity.y, velocity.z);
        }
    }

    private static Optional<Vec3d> nearestModelSurfacePoint(
            MinecraftClient client,
            int targetEntityId,
            Vec3d hitPosition
    ) {
        if (client.world == null
                || !(client.world.getEntityById(targetEntityId) instanceof LivingEntity target)) {
            return Optional.empty();
        }
        return ClientModelHurtboxCache.get(target).flatMap(boxes -> boxes.stream()
                .map(HumanoidHurtboxLibrary.PartBox::box)
                .map(box -> projectToSurface(box, hitPosition))
                .min(java.util.Comparator.comparingDouble(point -> point.squaredDistanceTo(hitPosition))));
    }

    private static Vec3d projectToSurface(
            AnimatedAttackHitboxLibrary.OrientedBox box,
            Vec3d point
    ) {
        Vec3d relative = point.subtract(box.center());
        double x = relative.dotProduct(box.axisX());
        double y = relative.dotProduct(box.axisY());
        double z = relative.dotProduct(box.axisZ());
        double cx = MathHelper.clamp(x, -box.halfExtents().x, box.halfExtents().x);
        double cy = MathHelper.clamp(y, -box.halfExtents().y, box.halfExtents().y);
        double cz = MathHelper.clamp(z, -box.halfExtents().z, box.halfExtents().z);

        boolean inside = x == cx && y == cy && z == cz;
        if (inside) {
            double dx = box.halfExtents().x - Math.abs(x);
            double dy = box.halfExtents().y - Math.abs(y);
            double dz = box.halfExtents().z - Math.abs(z);
            if (dx <= dy && dx <= dz) cx = Math.copySign(box.halfExtents().x, x == 0.0 ? 1.0 : x);
            else if (dy <= dz) cy = Math.copySign(box.halfExtents().y, y == 0.0 ? 1.0 : y);
            else cz = Math.copySign(box.halfExtents().z, z == 0.0 ? 1.0 : z);
        }
        return box.center()
                .add(box.axisX().multiply(cx))
                .add(box.axisY().multiply(cy))
                .add(box.axisZ().multiply(cz));
    }

    private static Optional<AnimatedAttackHitboxLibrary.OrientedBox> sampleLocalAttackBox(
            LivingEntity attacker
    ) {
        float elapsedSeconds = CombatAnimationClient.getLocalAttackAnimationElapsedSeconds()
                .orElseGet(CombatClientState::currentAttackAnimationElapsedSeconds);
        String animationName = CombatClientState.currentAttackAnimationName;
        if (animationName != null) {
            AttackMoveConfig namedMove = AttackMoveConfigs.getNamed(animationName);
            boolean useRealHitbox = CombatAnimationNames.isHeavyAttack(animationName)
                    || (namedMove != null
                    ? namedMove.useRealHitbox()
                    : ComboMoveConfigs.findByAnimationName(animationName)
                            .map(ComboMoveConfig::useRealHitbox)
                            .orElse(false));
            Vec3d realHitboxSize = EquipmentCombatAttributesRegistry.realHitboxSizeUnits(
                    attacker,
                    attacker.getMainHandStack(),
                    AnimatedAttackHitboxLibrary.getRealHitboxSizeUnits()
            );
            Vec3d realHitboxOffset = EquipmentCombatAttributesRegistry.realHitboxOffsetUnits(
                    attacker.getMainHandStack(),
                    AnimatedAttackHitboxLibrary.getRealHitboxOffsetUnits()
            );
            Vec3d realHitboxRotation = EquipmentCombatAttributesRegistry.realHitboxRotationDegrees(
                    attacker.getMainHandStack(),
                    AnimatedAttackHitboxLibrary.getRealHitboxRotationDegrees()
            );
            Optional<AnimatedAttackHitboxLibrary.SampledHitbox> sample =
                    AnimatedAttackHitboxLibrary.sampleNamedSeconds(
                    animationName,
                    elapsedSeconds,
                    useRealHitbox,
                    realHitboxSize,
                    realHitboxOffset,
                    realHitboxRotation
            );
            return resolveAttackBox(attacker, sample, useRealHitbox);
        }

        boolean useRealHitbox = AttackMoveConfigs.get(
                CombatClientState.lastAttackDirection
        ).useRealHitbox();
        Optional<AnimatedAttackHitboxLibrary.SampledHitbox> sample =
                AnimatedAttackHitboxLibrary.sampleSeconds(
                        CombatClientState.lastAttackDirection,
                        elapsedSeconds,
                        useRealHitbox
                );
        return resolveAttackBox(attacker, sample, useRealHitbox);
    }

    private static Optional<AnimatedAttackHitboxLibrary.OrientedBox> sampleEntityAttackBox(
            LivingEntity attacker,
            ClientEntityGeckoAnimationState.ActiveAnimation animation
    ) {
        if (CombatAnimationNames.isHeavyAttack(animation.customAnimationName())) {
            Vec3d realHitboxSize = EquipmentCombatAttributesRegistry.realHitboxSizeUnits(
                    attacker,
                    attacker.getMainHandStack(),
                    AnimatedAttackHitboxLibrary.getRealHitboxSizeUnits()
            );
            Vec3d realHitboxOffset = EquipmentCombatAttributesRegistry.realHitboxOffsetUnits(
                    attacker.getMainHandStack(),
                    AnimatedAttackHitboxLibrary.getRealHitboxOffsetUnits()
            );
            Vec3d realHitboxRotation = EquipmentCombatAttributesRegistry.realHitboxRotationDegrees(
                    attacker.getMainHandStack(),
                    AnimatedAttackHitboxLibrary.getRealHitboxRotationDegrees()
            );
            Optional<AnimatedAttackHitboxLibrary.SampledHitbox> sample =
                    AnimatedAttackHitboxLibrary.sampleNamedSeconds(
                            animation.customAnimationName(),
                            animation.elapsedSeconds(),
                            true,
                            realHitboxSize,
                            realHitboxOffset,
                            realHitboxRotation
                    );
            return resolveAttackBox(attacker, sample, true);
        }

        AttackMoveConfig moveConfig = animation.customAnimationName().isBlank()
                ? AttackMoveConfigs.get(animation.direction())
                : AttackMoveConfigs.getNamed(animation.customAnimationName());
        if (moveConfig == null) {
            moveConfig = AttackMoveConfigs.get(animation.direction());
        }
        boolean useRealHitbox = moveConfig.useRealHitbox();
        Optional<AnimatedAttackHitboxLibrary.SampledHitbox> sample =
                moveConfig.animationName().isBlank()
                        ? AnimatedAttackHitboxLibrary.sampleSeconds(
                                animation.direction(),
                                animation.elapsedSeconds(),
                                useRealHitbox
                        )
                        : AnimatedAttackHitboxLibrary.sampleNamedSeconds(
                                moveConfig.animationName(),
                                animation.elapsedSeconds(),
                                useRealHitbox,
                                EquipmentCombatAttributesRegistry.realHitboxSizeUnits(
                                        attacker,
                                        attacker.getMainHandStack(),
                                        AnimatedAttackHitboxLibrary.getRealHitboxSizeUnits()
                                ),
                                EquipmentCombatAttributesRegistry.realHitboxOffsetUnits(
                                        attacker.getMainHandStack(),
                                        AnimatedAttackHitboxLibrary.getRealHitboxOffsetUnits()
                                ),
                                EquipmentCombatAttributesRegistry.realHitboxRotationDegrees(
                                        attacker.getMainHandStack(),
                                        AnimatedAttackHitboxLibrary.getRealHitboxRotationDegrees()
                                )
                        );
        return resolveAttackBox(attacker, sample, useRealHitbox);
    }

    private static Optional<AnimatedAttackHitboxLibrary.OrientedBox> resolveAttackBox(
            LivingEntity attacker,
            Optional<AnimatedAttackHitboxLibrary.SampledHitbox> sample,
            boolean useRealHitbox
    ) {
        if (sample.isEmpty()) {
            return Optional.empty();
        }

        if (useRealHitbox) {
            Optional<AnimatedAttackHitboxLibrary.OrientedBox> itemBox =
                    ClientItemHitboxCache.get(attacker.getId());
            if (itemBox.isPresent()) {
                return itemBox;
            }
        }

        return Optional.of(sample.get().toWorldBox(attacker));
    }

    private static List<HumanoidHurtboxLibrary.PartBox> animatedHurtboxes(LivingEntity entity) {
        return ClientHumanoidHurtboxResolver.resolve(entity);
    }

    private static Optional<SweptHitResult> getSweptHitResult(
            AttackTrace attackTrace,
            LivingEntity target
    ) {
        return getSweptHitResult(attackTrace, target, false);
    }

    private static Optional<SweptHitResult> getSweptHitResult(
            AttackTrace attackTrace,
            LivingEntity target,
            boolean preferHead
    ) {
        return getSweptHitResult(attackTrace, target, preferHead, false);
    }

    private static Optional<SweptHitResult> getSweptHitResult(
            AttackTrace attackTrace,
            LivingEntity target,
            boolean preferHead,
            boolean includeRecentTargetMotion
    ) {
        List<HumanoidHurtboxLibrary.PartBox> currentHurtboxes = animatedHurtboxes(target);
        List<HumanoidHurtboxLibrary.PartBox> hurtboxes = currentHurtboxes;
        if (includeRecentTargetMotion) {
            Vec3d horizontalVelocity = new Vec3d(target.getVelocity().x, 0.0, target.getVelocity().z);
            if (horizontalVelocity.lengthSquared() > 0.000001) {
                Vec3d historyOffset = horizontalVelocity.multiply(-2.0);
                if (historyOffset.length() > 0.35) historyOffset = historyOffset.normalize().multiply(0.35);
                List<HumanoidHurtboxLibrary.PartBox> swept = new ArrayList<>(currentHurtboxes.size() * 2);
                swept.addAll(currentHurtboxes);
                for (HumanoidHurtboxLibrary.PartBox partBox : currentHurtboxes) {
                    AnimatedAttackHitboxLibrary.OrientedBox box = partBox.box();
                    swept.add(new HumanoidHurtboxLibrary.PartBox(
                            partBox.part(),
                            new AnimatedAttackHitboxLibrary.OrientedBox(
                                    box.center().add(historyOffset), box.halfExtents(),
                                    box.axisX(), box.axisY(), box.axisZ())
                    ));
                }
                hurtboxes = swept;
            }
        }
        boolean genericTarget = !HumanoidHurtboxLibrary.isHumanoidTarget(target);
        HumanoidHurtboxLibrary.HitResult firstHumanoidHit = null;
        HumanoidHurtboxLibrary.HitResult firstHumanoidHeadHit = null;
        boolean hitGenericHead = false;
        boolean hitGenericBody = false;
        HumanoidHurtboxLibrary.HitResult firstBodyHit = null;
        HumanoidHurtboxLibrary.HitResult firstHeadHit = null;

        for (AnimatedAttackHitboxLibrary.OrientedBox box : attackTrace.boxes()) {
            Optional<HumanoidHurtboxLibrary.HitResult> hitResult =
                    HumanoidHurtboxLibrary.getHitResult(box, target, hurtboxes);
            if (hitResult.isPresent()) {
                if (!genericTarget) {
                    if (!preferHead) {
                        return Optional.of(new SweptHitResult(hitResult.get(), false));
                    }
                    if (firstHumanoidHit == null) {
                        firstHumanoidHit = hitResult.get();
                    }
                    if (hitResult.get().part() == HumanoidHurtboxLibrary.Part.HEAD
                            && firstHumanoidHeadHit == null) {
                        firstHumanoidHeadHit = hitResult.get();
                    }
                    continue;
                }

                if (hitResult.get().part() == HumanoidHurtboxLibrary.Part.HEAD) {
                    hitGenericHead = true;
                    if (firstHeadHit == null) {
                        firstHeadHit = hitResult.get();
                    }
                } else {
                    hitGenericBody = true;
                    if (firstBodyHit == null) {
                        firstBodyHit = hitResult.get();
                    }
                }
            }
        }

        if (preferHead && firstHumanoidHeadHit != null) {
            return Optional.of(new SweptHitResult(firstHumanoidHeadHit, false));
        }

        if (preferHead && firstHumanoidHit != null) {
            return Optional.of(new SweptHitResult(firstHumanoidHit, false));
        }

        if (genericTarget && hitGenericBody && hitGenericHead && firstBodyHit != null) {
            return Optional.of(new SweptHitResult(firstBodyHit, true));
        }

        if (genericTarget && firstHeadHit != null) {
            return Optional.of(new SweptHitResult(firstHeadHit, false));
        }

        if (genericTarget && firstBodyHit != null) {
            return Optional.of(new SweptHitResult(firstBodyHit, false));
        }

        return Optional.empty();
    }

    private static boolean localAttackPrefersHead() {
        return "attack_smash".equals(CombatClientState.currentAttackAnimationName);
    }

    private static AnimatedAttackHitboxLibrary.OrientedBox getPreviousEntityAttackBox(
            int entityId,
            float elapsedSeconds
    ) {
        Float previousSeconds = LAST_ENTITY_ATTACK_SECONDS.get(entityId);
        if (previousSeconds == null || elapsedSeconds < previousSeconds) {
            LAST_ENTITY_ATTACK_BOXES.remove(entityId);
            return null;
        }

        return LAST_ENTITY_ATTACK_BOXES.get(entityId);
    }

    private static AttackTrace buildAttackTrace(
            AnimatedAttackHitboxLibrary.OrientedBox previous,
            AnimatedAttackHitboxLibrary.OrientedBox current
    ) {
        if (previous == null) {
            return new AttackTrace(List.of(current), current.candidateBox());
        }

        int steps = sweepSteps(previous, current);
        List<AnimatedAttackHitboxLibrary.OrientedBox> boxes = new ArrayList<>(steps + 1);
        Box candidateBox = null;
        for (int i = 0; i <= steps; i++) {
            double progress = i / (double) steps;
            AnimatedAttackHitboxLibrary.OrientedBox box = interpolateBox(
                    previous,
                    current,
                    progress
            );
            boxes.add(box);
            candidateBox = candidateBox == null
                    ? box.candidateBox()
                    : union(candidateBox, box.candidateBox());
        }

        return new AttackTrace(boxes, candidateBox);
    }

    private static int sweepSteps(
            AnimatedAttackHitboxLibrary.OrientedBox previous,
            AnimatedAttackHitboxLibrary.OrientedBox current
    ) {
        double centerDistance = previous.center().distanceTo(current.center());
        double tipDistance = maxCornerDistance(previous, current);
        int dynamicSteps = (int) Math.ceil(Math.max(centerDistance, tipDistance) / SWEEP_STEP_LENGTH);
        return Math.max(MIN_SWEEP_STEPS, Math.min(MAX_SWEEP_STEPS, dynamicSteps));
    }

    private static double maxCornerDistance(
            AnimatedAttackHitboxLibrary.OrientedBox previous,
            AnimatedAttackHitboxLibrary.OrientedBox current
    ) {
        Vec3d previousTipA = previous.center().add(previous.axisZ().multiply(previous.halfExtents().z));
        Vec3d previousTipB = previous.center().subtract(previous.axisZ().multiply(previous.halfExtents().z));
        Vec3d currentTipA = current.center().add(current.axisZ().multiply(current.halfExtents().z));
        Vec3d currentTipB = current.center().subtract(current.axisZ().multiply(current.halfExtents().z));
        return Math.max(
                previousTipA.distanceTo(currentTipA),
                previousTipB.distanceTo(currentTipB)
        );
    }

    private static AnimatedAttackHitboxLibrary.OrientedBox interpolateBox(
            AnimatedAttackHitboxLibrary.OrientedBox previous,
            AnimatedAttackHitboxLibrary.OrientedBox current,
            double progress
    ) {
        Vec3d axisX = normalizeOr(lerp(previous.axisX(), current.axisX(), progress), current.axisX());
        Vec3d axisYSeed = normalizeOr(lerp(previous.axisY(), current.axisY(), progress), current.axisY());
        Vec3d axisZ = normalizeOr(axisX.crossProduct(axisYSeed), current.axisZ());
        Vec3d axisY = normalizeOr(axisZ.crossProduct(axisX), current.axisY());

        return new AnimatedAttackHitboxLibrary.OrientedBox(
                lerp(previous.center(), current.center(), progress),
                lerp(previous.halfExtents(), current.halfExtents(), progress),
                axisX,
                axisY,
                axisZ
        );
    }

    private static Vec3d lerp(Vec3d from, Vec3d to, double progress) {
        return from.multiply(1.0 - progress).add(to.multiply(progress));
    }

    private static Vec3d normalizeOr(Vec3d value, Vec3d fallback) {
        if (value.lengthSquared() < 1.0E-8) {
            return fallback.normalize();
        }

        return value.normalize();
    }

    private static Box union(Box first, Box second) {
        return new Box(
                Math.min(first.minX, second.minX),
                Math.min(first.minY, second.minY),
                Math.min(first.minZ, second.minZ),
                Math.max(first.maxX, second.maxX),
                Math.max(first.maxY, second.maxY),
                Math.max(first.maxZ, second.maxZ)
        );
    }

    private static AnimatedAttackHitboxLibrary.OrientedBox paddedAttackBox(
            AnimatedAttackHitboxLibrary.OrientedBox box
    ) {
        return new AnimatedAttackHitboxLibrary.OrientedBox(
                box.center(),
                box.halfExtents().add(new Vec3d(0.16, 0.10, 0.16)),
                box.axisX(),
                box.axisY(),
                box.axisZ()
        );
    }

    private static String currentAttackKey() {
        return CombatClientState.attackInstanceId
                + ":"
                + CombatClientState.lastAttackDirection
                + ":"
                + CombatClientState.currentAttackAnimationName
                + ":"
                + CombatClientState.attackTotalTicks;
    }

    private record AttackTrace(
            List<AnimatedAttackHitboxLibrary.OrientedBox> boxes,
            Box candidateBox
    ) {
    }

    private record SweptHitResult(
            HumanoidHurtboxLibrary.HitResult hitResult,
            boolean extraHeadHit
    ) {
    }

    private record PendingBloodTrace(
            Vec3d position,
            AttackTrace trace
    ) {
    }
}
