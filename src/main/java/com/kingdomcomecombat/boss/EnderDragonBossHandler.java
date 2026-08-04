package com.kingdomcomecombat.boss;

import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.equipment.MobCombatAttributesRegistry;
import com.kingdomcomecombat.item.HandCannonProjectileTracker;
import com.kingdomcomecombat.potion.PotionCoatingHandler;
import com.kingdomcomecombat.combat.ActiveServerAttack;
import com.kingdomcomecombat.combat.CombatAttackTiming;
import com.kingdomcomecombat.combat.CombatDirection;
import com.kingdomcomecombat.combat.CombatItemUtil;
import com.kingdomcomecombat.combat.CombatTiming;
import com.kingdomcomecombat.combat.ComboMoveConfig;
import com.kingdomcomecombat.combat.ComboMoveConfigs;
import com.kingdomcomecombat.combat.ServerCombatControlState;
import com.kingdomcomecombat.combat.ServerCombatState;
import com.kingdomcomecombat.combat.ServerBlockState;
import com.kingdomcomecombat.network.CombatNetworkBroadcaster;
import com.kingdomcomecombat.network.EntityComboAttackAnimationPayload;
import com.kingdomcomecombat.network.HitFeedbackPayload;
import com.kingdomcomecombat.network.IncomingAttackWarningPayload;
import com.kingdomcomecombat.config.CombatServerConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.boss.dragon.phase.ChargingPlayerPhase;
import net.minecraft.entity.boss.dragon.phase.Phase;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.boss.dragon.phase.StrafePlayerPhase;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

public final class EnderDragonBossHandler {
    public static final int ARMOR_DURABILITY = 200;
    private static final DustParticleEffect PURPLE_GLOW =
            new DustParticleEffect(0xB82CFF, 1.6F);
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final Map<UUID, PendingWrath> PENDING_WRATH = new HashMap<>();
    private static final Set<UUID> ACTIVE_WRATH_COUNTERS = new HashSet<>();
    private static final Map<UUID, UUID> FORCED_WRATH_TARGETS = new HashMap<>();
    private static final Map<UUID, Integer> DRAGON_PARRY_PROTECTION = new HashMap<>();
    private static final Map<UUID, PlayerVerticalAnchor> PLAYER_VERTICAL_ANCHORS = new HashMap<>();
    private static final RegistryKey<DamageType> CUSTOM_COMBAT_DAMAGE = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE, Identifier.of(KingdomComeCombat.MOD_ID, "custom_combat"));

    private EnderDragonBossHandler() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                for (EnderDragonEntity dragon : world.getAliveEnderDragons()) {
                    tick(world, dragon);
                }
            }
            tickPendingWrath(server);
            tickForcedWrathHits(server);
            tickPlayerVerticalAnchors(server);
            DRAGON_PARRY_PROTECTION.replaceAll((uuid, ticks) -> ticks - 1);
            DRAGON_PARRY_PROTECTION.entrySet().removeIf(entry -> entry.getValue() <= 0);
        });
    }

    public static boolean crystalsProtect(EnderDragonEntity dragon) {
        return CombatServerConfig.enderDragonOverhaulEnabled() && !state(dragon).crystalsDestroyed;
    }

    public static boolean armorBroken(EnderDragonEntity dragon) {
        return MobCombatAttributesRegistry.armorDamage(
                dragon, MobCombatAttributesRegistry.ArmorSection.BODY) >= ARMOR_DURABILITY;
    }

    public static boolean blocksDamage(
            EnderDragonEntity dragon,
            Entity directSource
    ) {
        return CombatServerConfig.enderDragonOverhaulEnabled()
                && (crystalsProtect(dragon) || HandCannonProjectileTracker.isTracked(directSource));
    }

    public static boolean canLongswordParryCharge(LivingEntity attacker, LivingEntity blocker) {
        return CombatServerConfig.enderDragonOverhaulEnabled()
                && attacker instanceof EnderDragonEntity dragon
                && blocker instanceof ServerPlayerEntity
                && armorBroken(dragon)
                && dragon.getPhaseManager().getCurrent().getType() == PhaseType.CHARGING_PLAYER
                && CombatItemUtil.isLongsword(blocker.getMainHandStack());
    }

    public static void onPerfectChargeParry(EnderDragonEntity dragon, ServerPlayerEntity player) {
        State state = state(dragon);
        state.frozenTicks = 100;
        ServerCombatControlState.disableAttack(dragon.getUuid(), 100);
        dragon.setVelocity(Vec3d.ZERO);
        player.setVelocity(Vec3d.ZERO);
        player.velocityModified = true;
        player.fallDistance = 0.0F;
        PLAYER_VERTICAL_ANCHORS.put(player.getUuid(), new PlayerVerticalAnchor(player.getY(), 30));
        Vec3d away = dragon.getPos().subtract(player.getPos());
        away = new Vec3d(away.x, 0.0, away.z);
        if (away.lengthSquared() < 0.001) away = new Vec3d(0.0, 0.0, 1.0);
        state.frozenPosition = player.getPos().add(away.normalize().multiply(7.0)).add(0.0, 1.5, 0.0);
        dragon.setPosition(state.frozenPosition);
        DRAGON_PARRY_PROTECTION.put(player.getUuid(), 100);
        PENDING_WRATH.put(player.getUuid(), new PendingWrath(dragon.getUuid(), 20));
    }

    public static boolean tryLongHoldChargeCollisionBlock(
            ServerWorld world, LivingEntity target, Entity damageAttacker
    ) {
        if (!(target instanceof ServerPlayerEntity player)) return false;
        EnderDragonEntity dragon = damageAttacker instanceof EnderDragonEntity direct
                ? direct
                : damageAttacker instanceof EnderDragonPart part ? part.owner : null;
        if (dragon == null || !canLongswordParryCharge(dragon, player)) return false;
        if (!ServerBlockState.isLongHeld(player.getUuid())) return false;
        ServerBlockState.clear(player.getUuid());
        world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK.value(),
                player.getSoundCategory(), 1.8F, 0.65F);
        world.spawnParticles(ParticleTypes.CRIT, player.getX(), player.getBodyY(0.6), player.getZ(),
                70, 0.8, 0.8, 0.8, 0.35);
        onPerfectChargeParry(dragon, player);
        return true;
    }

    public static boolean isProtectedFromDragon(LivingEntity entity, Entity attacker) {
        return entity instanceof ServerPlayerEntity
                && (attacker instanceof EnderDragonEntity
                || attacker instanceof net.minecraft.entity.boss.dragon.EnderDragonPart)
                && DRAGON_PARRY_PROTECTION.getOrDefault(entity.getUuid(), 0) > 0;
    }

    public static boolean hasDragonParryProtection(LivingEntity entity) {
        return entity instanceof ServerPlayerEntity
                && DRAGON_PARRY_PROTECTION.getOrDefault(entity.getUuid(), 0) > 0;
    }

    public static boolean claimChargeContact(EnderDragonEntity dragon, LivingEntity target) {
        if (!CombatServerConfig.enderDragonOverhaulEnabled()) return true;
        if (dragon.getPhaseManager().getCurrent().getType() == PhaseType.DYING) return false;
        State state = state(dragon);
        if (dragon.getPhaseManager().getCurrent().getType() == PhaseType.CHARGING_PLAYER) {
            return state.chargeHitTargets.add(target.getUuid());
        }
        long now = dragon.getWorld().getTime();
        long previous = state.contactHitTicks.getOrDefault(target.getUuid(), Long.MIN_VALUE);
        if (now - previous < 16L) return false;
        state.contactHitTicks.put(target.getUuid(), now);
        return true;
    }

    public static boolean isWrathCounterAttack(LivingEntity attacker, ActiveServerAttack attack) {
        return attacker != null && attack != null
                && ACTIVE_WRATH_COUNTERS.contains(attacker.getUuid())
                && attack.comboMove != null
                && "combo_warth_strike".equals(attack.comboMove.animationName());
    }

    public static boolean applyWrathCounterHit(
            ServerWorld world,
            LivingEntity attacker,
            LivingEntity target,
            ActiveServerAttack attack,
            Vec3d hitPosition
    ) {
        if (!(target instanceof EnderDragonEntity dragon)
                || !isWrathCounterAttack(attacker, attack)) {
            return false;
        }
        attack.hitTargets.add(dragon.getUuid());
        world.spawnParticles(ParticleTypes.DRAGON_BREATH,
                hitPosition.x, hitPosition.y, hitPosition.z,
                260, 1.8, 1.8, 1.8, 0.22);
        world.spawnParticles(PURPLE_GLOW, hitPosition.x, hitPosition.y, hitPosition.z,
                180, 1.5, 1.5, 1.5, 0.20);
        if (attacker instanceof ServerPlayerEntity player) {
            ServerPlayNetworking.send(player,
                    new HitFeedbackPayload(attack.direction.ordinal(), true));
        }
        dragon.damagePart(
                world,
                dragon.head,
                world.getDamageSources().create(CUSTOM_COMBAT_DAMAGE, attacker),
                100.0F
        );
        if (!dragon.isAlive() || dragon.getHealth() <= 0.0F) {
            beginVanillaDragonDeath(dragon);
        }
        ACTIVE_WRATH_COUNTERS.remove(attacker.getUuid());
        return true;
    }

    public static float modifyArrowDamage(
            ServerWorld world,
            EnderDragonEntity dragon,
            Entity directSource,
            float damage
    ) {
        if (!CombatServerConfig.enderDragonOverhaulEnabled()
                || !(directSource instanceof ProjectileEntity projectile)
                || !PotionCoatingHandler.isDragonBreathArrow(projectile)) {
            return damage;
        }

        State state = state(dragon);
        if (state.crystalsDestroyed) {
            Vec3d hit = projectile.getPos();
            if (armorBroken(dragon)) {
                spawnExposedWeakPointBurst(world, dragon, hit, projectile.getVelocity());
                return damage * 3.0F;
            }
            MobCombatAttributesRegistry.damageArmor(dragon,
                    MobCombatAttributesRegistry.ArmorSection.BODY, 30);
            spawnWeakPointBurst(world, hit);
            if (armorBroken(dragon) && !state.breakEffectPlayed) {
                state.breakEffectPlayed = true;
                spawnArmorBreakBurst(world, dragon);
            }
        }
        return damage;
    }

    private static void tick(ServerWorld world, EnderDragonEntity dragon) {
        if (!CombatServerConfig.enderDragonOverhaulEnabled()) {
            dragon.setGlowing(false);
            return;
        }
        if (dragon.getPhaseManager().getCurrent().getType() == PhaseType.DYING) {
            dragon.setGlowing(false);
            State state = state(dragon);
            state.chargeTicks = 0;
            state.chargeTarget = null;
            state.frozenTicks = 0;
            return;
        }
        enforceArenaBounds(world, dragon);
        State state = state(dragon);
        if (!state.crystalsDestroyed && dragon.age > 80 && !hasNearbyCrystal(world, dragon)) {
            state.crystalsDestroyed = true;
            dragon.setGlowing(false);
        }

        if (!state.crystalsDestroyed) {
            dragon.setGlowing(true);
            if ((dragon.age & 1) == 0) {
                world.spawnParticles(PURPLE_GLOW, dragon.getX(), dragon.getBodyY(0.55), dragon.getZ(),
                        12, dragon.getWidth() * 0.35, dragon.getHeight() * 0.30, dragon.getWidth() * 0.35, 0.01);
            }
        } else if (armorBroken(dragon)) {
            dragon.setGlowing(true);
        }
        ((EnderDragonBossStateAccess) dragon).kingdomcomecombat$setArmorBroken(armorBroken(dragon));

        if (state.frozenTicks > 0) {
            state.frozenTicks--;
            dragon.setVelocity(Vec3d.ZERO);
            if (state.frozenPosition != null) dragon.setPosition(state.frozenPosition);
            dragon.getPhaseManager().setPhase(PhaseType.HOVER);
            return;
        }
        state.frozenPosition = null;
        if (tickCharge(world, dragon, state)) {
            return;
        }
        preventPerching(dragon);
        intensifyAttacks(world, dragon, state);
    }

    private static void preventPerching(EnderDragonEntity dragon) {
        PhaseType<?> type = dragon.getPhaseManager().getCurrent().getType();
        if (type == PhaseType.LANDING_APPROACH || type == PhaseType.LANDING
                || type == PhaseType.SITTING_FLAMING || type == PhaseType.SITTING_SCANNING
                || type == PhaseType.SITTING_ATTACKING || type == PhaseType.TAKEOFF
                || type == PhaseType.HOVER) {
            dragon.getPhaseManager().setPhase(PhaseType.HOLDING_PATTERN);
        }
    }

    private static void intensifyAttacks(ServerWorld world, EnderDragonEntity dragon, State state) {
        if (++state.chargeTimer >= state.nextChargeInterval) {
            state.chargeTimer = 0;
            state.nextChargeInterval = 400 + dragon.getRandom().nextInt(101);
            PlayerEntity chargeTarget = randomArenaPlayer(world, dragon);
            if (chargeTarget != null && !chargeTarget.isSpectator()) {
                dragon.getPhaseManager().setPhase(PhaseType.CHARGING_PLAYER);
                dragon.getPhaseManager().create(PhaseType.CHARGING_PLAYER)
                        .setPathTarget(chargeTarget.getEyePos());
                state.chargeTarget = chargeTarget.getUuid();
                state.chargeTicks = 90;
                state.chargeHitTargets.clear();
                sendChargeWarning(dragon, chargeTarget);
            }
            return;
        }

        int interval = armorBroken(dragon) ? 70 : 115;
        if (++state.attackTimer < interval) return;
        state.attackTimer = 0;
        PlayerEntity target = randomArenaPlayer(world, dragon);
        if (target == null || target.isSpectator()) return;
        dragon.getPhaseManager().setPhase(PhaseType.STRAFE_PLAYER);
        StrafePlayerPhase phase = dragon.getPhaseManager().create(PhaseType.STRAFE_PLAYER);
        phase.setTargetEntity(target);
    }

    private static PlayerEntity randomArenaPlayer(ServerWorld world, EnderDragonEntity dragon) {
        java.util.List<ServerPlayerEntity> players = world.getPlayers(player ->
                player.isAlive() && !player.isSpectator() && player.squaredDistanceTo(dragon) <= 160.0 * 160.0);
        return players.isEmpty() ? null : players.get(dragon.getRandom().nextInt(players.size()));
    }

    private static boolean tickCharge(ServerWorld world, EnderDragonEntity dragon, State state) {
        if (state.chargeTicks <= 0 || state.chargeTarget == null) return false;
        Entity entity = world.getEntity(state.chargeTarget);
        if (!(entity instanceof PlayerEntity target) || !target.isAlive() || target.isSpectator()) {
            state.chargeTicks = 0;
            state.chargeTarget = null;
            return false;
        }
        state.chargeTicks--;
        dragon.getPhaseManager().setPhase(PhaseType.CHARGING_PLAYER);
        ChargingPlayerPhase phase = dragon.getPhaseManager().create(PhaseType.CHARGING_PLAYER);
        Vec3d targetPos = target.getEyePos();
        Vec3d origin = Vec3d.ofCenter(dragon.getFightOrigin());
        Vec3d horizontal = new Vec3d(targetPos.x - origin.x, 0.0, targetPos.z - origin.z);
        if (horizontal.lengthSquared() > 75.0 * 75.0) {
            horizontal = horizontal.normalize().multiply(75.0);
        }
        targetPos = new Vec3d(
                origin.x + horizontal.x,
                Math.max(origin.y + 4.0, Math.min(origin.y + 55.0, targetPos.y)),
                origin.z + horizontal.z
        );
        phase.setPathTarget(targetPos);
        Vec3d delta = targetPos.subtract(dragon.getPos());
        if (delta.lengthSquared() > 1.0) {
            double speed = armorBroken(dragon) ? 0.92 : 0.78;
            dragon.setVelocity(delta.normalize().multiply(speed));
            dragon.velocityModified = true;
        }
        if (state.chargeTicks <= 0 || delta.lengthSquared() < 16.0) {
            state.chargeTarget = null;
        }
        Vec3d dragonOffset = dragon.getPos().subtract(origin);
        double horizontalDistance = Math.hypot(dragonOffset.x, dragonOffset.z);
        double minimumY = arenaMinimumY(world, dragon, origin);
        if (dragon.getY() < minimumY || horizontalDistance > 105.0) {
            Vec3d safeHorizontal = horizontalDistance > 0.001
                    ? new Vec3d(dragonOffset.x, 0.0, dragonOffset.z).normalize().multiply(Math.min(90.0, horizontalDistance))
                    : Vec3d.ZERO;
            dragon.setPosition(origin.x + safeHorizontal.x,
                    Math.max(minimumY, dragon.getY()), origin.z + safeHorizontal.z);
        }
        return true;
    }

    private static void enforceArenaBounds(ServerWorld world, EnderDragonEntity dragon) {
        Vec3d origin = Vec3d.ofCenter(dragon.getFightOrigin());
        Vec3d offset = dragon.getPos().subtract(origin);
        Vec3d horizontal = new Vec3d(offset.x, 0.0, offset.z);
        double distance = horizontal.length();
        boolean outside = distance > 96.0;
        double minimumY = arenaMinimumY(world, dragon, origin);
        boolean below = dragon.getY() < minimumY;
        if (!outside && !below) return;
        Vec3d clamped = outside && distance > 0.001
                ? horizontal.normalize().multiply(90.0)
                : horizontal;
        dragon.setPosition(
                origin.x + clamped.x,
                Math.max(minimumY, dragon.getY()),
                origin.z + clamped.z
        );
        Vec3d velocity = dragon.getVelocity();
        dragon.setVelocity(velocity.x * 0.35, Math.max(0.0, velocity.y), velocity.z * 0.35);
        dragon.velocityModified = true;
    }

    private static double arenaMinimumY(ServerWorld world, EnderDragonEntity dragon, Vec3d origin) {
        int altarSurface = world.getTopY(
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                dragon.getFightOrigin().getX(), dragon.getFightOrigin().getZ());
        return Math.max(origin.y + 7.0, altarSurface + 3.5);
    }

    private static void sendChargeWarning(EnderDragonEntity dragon, PlayerEntity target) {
        if (!(target instanceof ServerPlayerEntity player)) return;
        Vec3d incoming = dragon.getPos().subtract(player.getPos());
        incoming = new Vec3d(incoming.x, 0.0, incoming.z);
        CombatDirection direction = CombatDirection.UP;
        if (incoming.lengthSquared() > 0.001) {
            incoming = incoming.normalize();
            double yaw = Math.toRadians(player.getYaw());
            Vec3d forward = new Vec3d(-Math.sin(yaw), 0.0, Math.cos(yaw));
            Vec3d right = new Vec3d(forward.z, 0.0, -forward.x);
            double side = right.dotProduct(incoming);
            double front = forward.dotProduct(incoming);
            direction = Math.abs(side) > Math.abs(front) && Math.abs(side) > 0.35
                    ? (side > 0.0 ? CombatDirection.RIGHT : CombatDirection.LEFT)
                    : (front >= 0.0 ? CombatDirection.UP : CombatDirection.DOWN);
        }
        int warningType = com.kingdomcomecombat.combat.CombatItemUtil.hasDirectionAgnosticBlock(player)
                ? IncomingAttackWarningPayload.DIRECTION_FREE_BLOCK
                : IncomingAttackWarningPayload.DIRECTIONAL_BLOCK;
        ServerPlayNetworking.send(player, new IncomingAttackWarningPayload(
                dragon.getId(), direction.ordinal(), warningType));
    }

    private static void tickPendingWrath(net.minecraft.server.MinecraftServer server) {
        var iterator = PENDING_WRATH.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            PendingWrath pending = entry.getValue();
            if (--pending.delayTicks > 0) continue;
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            Entity target = null;
            for (ServerWorld world : server.getWorlds()) {
                Entity candidate = world.getEntity(pending.dragonUuid);
                if (candidate != null) {
                    target = candidate;
                    break;
                }
            }
            if (player != null && target instanceof EnderDragonEntity dragon
                    && dragon.isAlive() && CombatItemUtil.isLongsword(player.getMainHandStack())) {
                startWrathCounter(player, dragon);
            }
            iterator.remove();
        }
    }

    private static void tickForcedWrathHits(net.minecraft.server.MinecraftServer server) {
        var iterator = FORCED_WRATH_TARGETS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            ActiveServerAttack attack = player == null ? null : ServerCombatState.getAttack(player.getUuid());
            if (player == null || attack == null || !isWrathCounterAttack(player, attack)) {
                ACTIVE_WRATH_COUNTERS.remove(entry.getKey());
                iterator.remove();
                continue;
            }
            if (attack.ageTicks < CombatTiming.getActiveStartTick(attack.attackTotalTicks)) {
                continue;
            }
            Entity target = null;
            for (ServerWorld world : server.getWorlds()) {
                target = world.getEntity(entry.getValue());
                if (target != null) break;
            }
            if (target instanceof EnderDragonEntity dragon && dragon.isAlive()
                    && player.getWorld() instanceof ServerWorld world) {
                applyWrathCounterHit(world, player, dragon, attack, dragon.getBoundingBox().getCenter());
            } else {
                ACTIVE_WRATH_COUNTERS.remove(entry.getKey());
            }
            iterator.remove();
        }
    }

    private static void beginVanillaDragonDeath(EnderDragonEntity dragon) {
        dragon.setHealth(1.0F);
        dragon.setVelocity(Vec3d.ZERO);
        dragon.getPhaseManager().setPhase(PhaseType.DYING);
        State state = state(dragon);
        state.chargeTicks = 0;
        state.chargeTarget = null;
        state.frozenTicks = 0;
        state.frozenPosition = null;
    }

    private static void tickPlayerVerticalAnchors(net.minecraft.server.MinecraftServer server) {
        var iterator = PLAYER_VERTICAL_ANCHORS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            PlayerVerticalAnchor anchor = entry.getValue();
            if (player == null || !player.isAlive() || --anchor.ticks <= 0) {
                iterator.remove();
                continue;
            }
            Vec3d velocity = player.getVelocity();
            player.setVelocity(velocity.x, 0.0, velocity.z);
            player.setPosition(player.getX(), anchor.y, player.getZ());
            player.fallDistance = 0.0F;
            player.velocityModified = true;
        }
    }

    private static void startWrathCounter(ServerPlayerEntity player, EnderDragonEntity dragon) {
        ComboMoveConfig wrath = ComboMoveConfigs.findByAnimationName("combo_warth_strike").orElse(null);
        if (wrath == null) return;
        CombatDirection direction = CombatDirection.LEFT;
        int totalTicks = CombatAttackTiming.getComboAttackTotalTicks(wrath.animationName());
        ServerCombatState.removeAttack(player.getUuid());
        ServerCombatState.startAttack(
                player.getUuid(), direction, player.getYaw(), dragon.getId(), false,
                totalTicks, 1.0F, wrath, null, player.getWorld().getTime(), false, 0.0);
        ACTIVE_WRATH_COUNTERS.add(player.getUuid());
        FORCED_WRATH_TARGETS.put(player.getUuid(), dragon.getUuid());
        CombatNetworkBroadcaster.sendTrackingAndSelf(player, new EntityComboAttackAnimationPayload(
                player.getId(), direction.ordinal(), wrath.animationName(), 1.0F, wrath.bladeTrail()));
    }

    private static boolean hasNearbyCrystal(ServerWorld world, EnderDragonEntity dragon) {
        Box area = dragon.getBoundingBox().expand(192.0);
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof EndCrystalEntity && entity.isAlive() && area.contains(entity.getPos())) return true;
        }
        return false;
    }

    private static State state(EnderDragonEntity dragon) {
        return STATES.computeIfAbsent(dragon.getUuid(), ignored -> new State());
    }

    private static void spawnWeakPointBurst(ServerWorld world, Vec3d pos) {
        world.spawnParticles(ParticleTypes.DRAGON_BREATH, pos.x, pos.y, pos.z,
                180, 1.1, 1.1, 1.1, 0.20);
        world.spawnParticles(PURPLE_GLOW, pos.x, pos.y, pos.z,
                100, 0.8, 0.8, 0.8, 0.12);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.HOSTILE, 2.2F, 0.75F);
    }

    private static void spawnArmorBreakBurst(ServerWorld world, EnderDragonEntity dragon) {
        Vec3d pos = dragon.getBoundingBox().getCenter();
        world.spawnParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z,
                35, dragon.getWidth() * 0.35, dragon.getHeight() * 0.30, dragon.getWidth() * 0.35, 0.08);
        world.spawnParticles(ParticleTypes.DRAGON_BREATH, pos.x, pos.y, pos.z,
                420, dragon.getWidth() * 0.50, dragon.getHeight() * 0.45, dragon.getWidth() * 0.50, 0.25);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.HOSTILE, 4.0F, 0.55F);
    }

    private static void spawnExposedWeakPointBurst(
            ServerWorld world,
            EnderDragonEntity dragon,
            Vec3d pos,
            Vec3d incomingVelocity
    ) {
        Vec3d normal = pos.subtract(dragon.getBoundingBox().getCenter());
        if (normal.lengthSquared() < 0.001) normal = incomingVelocity.negate();
        if (normal.lengthSquared() < 0.001) normal = new Vec3d(0.0, 1.0, 0.0);
        normal = normal.normalize();
        world.spawnParticles(PURPLE_GLOW, pos.x, pos.y, pos.z,
                180, 1.8, 1.8, 1.8, 0.18);
        world.spawnParticles(ParticleTypes.DRAGON_BREATH, pos.x, pos.y, pos.z,
                260, 1.4, 1.4, 1.4, 0.28);
        for (int i = 0; i < 110; i++) {
            Vec3d spray = normal.add(
                    (dragon.getRandom().nextDouble() - 0.5) * 0.9,
                    (dragon.getRandom().nextDouble() - 0.5) * 0.9,
                    (dragon.getRandom().nextDouble() - 0.5) * 0.9
            ).normalize().multiply(0.8 + dragon.getRandom().nextDouble() * 1.8);
            world.spawnParticles(ParticleTypes.DRAGON_BREATH,
                    pos.x, pos.y, pos.z, 0, spray.x, spray.y, spray.z, 1.0);
        }
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.HOSTILE, 2.8F, 0.85F);
    }

    private static final class State {
        private boolean crystalsDestroyed;
        private boolean breakEffectPlayed;
        private int attackTimer;
        private int frozenTicks;
        private int chargeTicks;
        private UUID chargeTarget;
        private int chargeTimer;
        private int nextChargeInterval = 400;
        private Vec3d frozenPosition;
        private final Set<UUID> chargeHitTargets = new HashSet<>();
        private final Map<UUID, Long> contactHitTicks = new HashMap<>();
    }

    private static final class PendingWrath {
        private final UUID dragonUuid;
        private int delayTicks;

        private PendingWrath(UUID dragonUuid, int delayTicks) {
            this.dragonUuid = dragonUuid;
            this.delayTicks = delayTicks;
        }
    }

    private static final class PlayerVerticalAnchor {
        private final double y;
        private int ticks;

        private PlayerVerticalAnchor(double y, int ticks) {
            this.y = y;
            this.ticks = ticks;
        }
    }
}
