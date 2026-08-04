package com.kingdomcomecombat.boss;

import com.kingdomcomecombat.combat.ServerBlockState;
import com.kingdomcomecombat.combat.ServerCombatControlState;
import com.kingdomcomecombat.equipment.EquipmentCombatAttributesRegistry;
import com.kingdomcomecombat.equipment.MobCombatAttributesRegistry;
import com.kingdomcomecombat.combat.CombatDirection;
import com.kingdomcomecombat.config.CombatServerConfig;
import com.kingdomcomecombat.network.CombatNetworkBroadcaster;
import com.kingdomcomecombat.network.EntityBlockAnimationPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class WitherBossHandler {
    private static final int EXPLOSION_WINDUP_TICKS = 160;
    private static final int CHARGE_WINDUP_TICKS = 50;
    private static final int CHARGE_MOVE_TICKS = 20;
    private static final double CHARGE_DISTANCE = 16.0;
    private static final Map<UUID, State> STATES = new HashMap<>();

    private WitherBossHandler() {
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof WitherEntity wither) {
                if (CombatServerConfig.witherOverhaulEnabled()) ensureTracked(wither);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(WitherBossHandler::tickServer);
    }

    private static void tickServer(MinecraftServer server) {
        if (!CombatServerConfig.witherOverhaulEnabled()) {
            for (UUID uuid : STATES.keySet()) {
                Entity entity = find(server, uuid);
                if (entity instanceof WitherEntity wither) wither.setGlowing(false);
            }
            STATES.clear();
            return;
        }
        Iterator<Map.Entry<UUID, State>> iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, State> entry = iterator.next();
            Entity found = find(server, entry.getKey());
            if (!(found instanceof WitherEntity wither) || wither.isRemoved() || !wither.isAlive()) {
                iterator.remove();
                continue;
            }
            tick(wither, entry.getValue());
        }
    }

    public static void ensureTracked(WitherEntity wither) {
        if (!(wither.getWorld() instanceof ServerWorld)) return;
        STATES.computeIfAbsent(wither.getUuid(), ignored -> new State(120 + wither.getRandom().nextInt(80)));
    }

    public static boolean canFireSkull(WitherEntity wither) {
        State state = STATES.computeIfAbsent(wither.getUuid(),
                ignored -> new State(120 + wither.getRandom().nextInt(80)));
        if (state.phase != Phase.IDLE) return false;
        long now = wither.getWorld().getTime();
        long interval = wither.getHealth() <= wither.getMaxHealth() * 0.5F ? 60L : 20L;
        if (now - state.lastSkullFireTick < interval) return false;
        state.lastSkullFireTick = now;
        return true;
    }

    private static void tick(WitherEntity wither, State state) {
        if (!(wither.getWorld() instanceof ServerWorld world)) return;
        if (wither.getInvulnerableTimer() > 0) return;

        if (state.phase == Phase.IDLE) {
            if (state.explosionCooldown > 0) state.explosionCooldown--;
            if (--state.cooldown <= 0) {
                boolean canCharge = wither.getHealth() <= wither.getMaxHealth() * 0.5F;
                if (!canCharge && state.explosionCooldown > 0) {
                    state.cooldown = 80;
                    return;
                }
                Phase next = canCharge
                        && (state.explosionCooldown > 0 || wither.getRandom().nextFloat() >= 0.28F)
                        ? Phase.CHARGE_WINDUP
                        : Phase.EXPLOSION_WINDUP;
                startSkill(wither, state, next);
            }
            return;
        }

        wither.getNavigation().stop();
        wither.setVelocity(Vec3d.ZERO);
        if (state.windupPosition != null && state.phase != Phase.CHARGING) {
            wither.setPosition(state.windupPosition);
        }
        state.ticks++;
        if (state.phase == Phase.EXPLOSION_WINDUP) {
            spawnExplosionWindup(world, wither, state.ticks);
            if (state.ticks >= EXPLOSION_WINDUP_TICKS) {
                wither.setGlowing(false);
                spawnBlackExplosionParticles(world, wither);
                world.createExplosion(wither, wither.getX(), wither.getBodyY(0.5), wither.getZ(),
                        8.0F, World.ExplosionSourceType.MOB);
                state.explosionCooldown = 1200 + wither.getRandom().nextInt(601);
                finish(wither, state, 360, 520);
            }
        } else if (state.phase == Phase.CHARGE_WINDUP) {
            spawnChargeWindup(world, wither);
            if (state.ticks >= CHARGE_WINDUP_TICKS) {
                state.phase = Phase.CHARGING;
                state.ticks = 0;
                state.hitPlayers.clear();
                world.playSound(null, wither.getBlockPos(), SoundEvents.ENTITY_WITHER_SHOOT,
                        wither.getSoundCategory(), 2.0F, 0.55F);
            }
        } else {
            performChargeStep(world, wither, state);
            if (state.ticks >= CHARGE_MOVE_TICKS) {
                finish(wither, state, 180, 260);
            }
        }
    }

    private static void startSkill(WitherEntity wither, State state, Phase phase) {
        state.phase = phase;
        state.ticks = 0;
        state.hitPlayers.clear();
        wither.setTarget(null);
        wither.setGlowing(phase == Phase.EXPLOSION_WINDUP);
        state.windupPosition = wither.getPos();
        if (phase == Phase.CHARGE_WINDUP) {
            state.chargeStep = chargeDirection(wither).multiply(CHARGE_DISTANCE / CHARGE_MOVE_TICKS);
        }
    }

    private static void finish(WitherEntity wither, State state, int minCooldown, int maxCooldown) {
        wither.setGlowing(false);
        state.phase = Phase.IDLE;
        state.ticks = 0;
        state.windupPosition = null;
        state.cooldown = minCooldown + wither.getRandom().nextInt(maxCooldown - minCooldown + 1);
    }

    private static void spawnExplosionWindup(ServerWorld world, WitherEntity wither, int ticks) {
        int count = ticks > 120 ? 12 : 5;
        world.spawnParticles(ParticleTypes.END_ROD, wither.getX(), wither.getBodyY(0.55), wither.getZ(),
                count, wither.getWidth() * 0.7, wither.getHeight() * 0.45, wither.getWidth() * 0.7, 0.04);
        if (ticks % 20 == 0) {
            world.spawnParticles(ParticleTypes.FLASH, wither.getX(), wither.getBodyY(0.55), wither.getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void spawnChargeWindup(ServerWorld world, WitherEntity wither) {
        wither.setYaw(wither.getYaw() + (wither.getRandom().nextBoolean() ? 4.0F : -4.0F));
        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, wither.getX(), wither.getBodyY(0.5), wither.getZ(),
                8, wither.getWidth() * 0.6, wither.getHeight() * 0.4, wither.getWidth() * 0.6, 0.025);
        world.spawnParticles(ParticleTypes.SMOKE, wither.getX(), wither.getBodyY(0.5), wither.getZ(),
                5, 0.55, 0.65, 0.55, 0.02);
    }

    private static void spawnBlackExplosionParticles(ServerWorld world, WitherEntity wither) {
        double y = wither.getBodyY(0.5);
        world.spawnParticles(ParticleTypes.SQUID_INK, wither.getX(), y, wither.getZ(),
                420, 8.0, 8.0, 8.0, 0.25);
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, wither.getX(), y, wither.getZ(),
                260, 7.5, 7.5, 7.5, 0.18);
    }

    private static Vec3d chargeDirection(WitherEntity wither) {
        ServerPlayerEntity nearest = wither.getWorld().getClosestPlayer(wither, 48.0) instanceof ServerPlayerEntity player
                ? player : null;
        Vec3d direction = nearest == null
                ? wither.getRotationVec(1.0F)
                : nearest.getBoundingBox().getCenter().subtract(wither.getBoundingBox().getCenter());
        if (direction.lengthSquared() < 0.0001) direction = wither.getRotationVec(1.0F);
        return direction.normalize();
    }

    private static void performChargeStep(ServerWorld world, WitherEntity wither, State state) {
        Vec3d next = wither.getPos().add(state.chargeStep);
        wither.setPosition(next.x, next.y, next.z);
        destroyChargePath(world, wither.getBoundingBox().expand(0.45));
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, wither.getX(), wither.getBodyY(0.5), wither.getZ(),
                10, 0.6, 0.6, 0.6, 0.03);

        Box hitBox = wither.getBoundingBox().expand(0.6);
        for (ServerPlayerEntity player : world.getEntitiesByClass(ServerPlayerEntity.class, hitBox, Entity::isAlive)) {
            if (!state.hitPlayers.add(player.getUuid())) continue;
            if (shieldBlocksCharge(player)) {
                world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK.value(),
                        player.getSoundCategory(), 1.4F, 0.75F);
                continue;
            }
            player.damage(world, world.getDamageSources().mobAttack(wither), 18.0F);
        }
    }

    private static boolean shieldBlocksCharge(ServerPlayerEntity player) {
        if (!EquipmentCombatAttributesRegistry.isShield(player.getOffHandStack())
                || !ServerCombatControlState.canBlock(player)) return false;
        ServerBlockState.BlockWindow window = ServerBlockState.get(player.getUuid());
        if (window == null && !player.isBlocking()) return false;
        ServerBlockState.clear(player.getUuid());
        return true;
    }

    private static void destroyChargePath(ServerWorld world, Box box) {
        for (BlockPos pos : BlockPos.iterate(
                (int) Math.floor(box.minX), (int) Math.floor(box.minY), (int) Math.floor(box.minZ),
                (int) Math.floor(box.maxX), (int) Math.floor(box.maxY), (int) Math.floor(box.maxZ))) {
            BlockState block = world.getBlockState(pos);
            if (!block.isAir() && block.getHardness(world, pos) >= 0.0F) {
                world.breakBlock(pos, true, null);
            }
        }
    }

    public static boolean handleSkullBlock(WitherSkullEntity skull, ServerPlayerEntity player) {
        if (!(skull.getWorld() instanceof ServerWorld world)
                || !canBlockSkull(player)
                || !ServerCombatControlState.canBlock(player)) return false;
        ServerBlockState.BlockWindow window = ServerBlockState.get(player.getUuid());
        boolean charged = skull.isCharged();
        if (window == null || window.isUnperfectPhase()) return false;
        ServerBlockState.clear(player.getUuid());

        Vec3d reflected = skull.getVelocity().multiply(-1.5);
        if (reflected.lengthSquared() < 0.01) reflected = player.getRotationVec(1.0F).multiply(1.65);
        skull.setOwner(player);
        skull.setVelocity(reflected);
        skull.setPosition(player.getX() + reflected.x * 0.8, player.getEyeY(), player.getZ() + reflected.z * 0.8);
        spawnBlockedSkullParticles(world, skull.getPos(), charged ? 16 : 12);
        CombatNetworkBroadcaster.sendTrackingAndSelf(player, new EntityBlockAnimationPayload(
                player.getId(), EntityBlockAnimationPayload.PERFECT,
                CombatDirection.UP.ordinal(), skull.getId()));
        world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK.value(),
                player.getSoundCategory(), 1.5F, charged ? 1.35F : 0.9F);
        return true;
    }

    private static boolean canBlockSkull(ServerPlayerEntity player) {
        return EquipmentCombatAttributesRegistry.canBlockWithHeldItem(player.getMainHandStack())
                || EquipmentCombatAttributesRegistry.isShield(player.getOffHandStack())
                || player.isBlocking();
    }

    public static void discardMissedSkull(WitherSkullEntity skull) {
        if (!(skull.getWorld() instanceof ServerWorld world)) return;
        spawnBlockedSkullParticles(world, skull.getPos(), 35);
        skull.discard();
    }

    private static void spawnBlockedSkullParticles(ServerWorld world, Vec3d pos, int count) {
        world.spawnParticles(ParticleTypes.SQUID_INK, pos.x, pos.y, pos.z,
                count, 0.65, 0.65, 0.65, 0.18);
        world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z,
                count / 2, 0.55, 0.55, 0.55, 0.08);
    }

    public static void onReflectedSkullHit(WitherSkullEntity skull, WitherEntity wither) {
        if (!(skull.getOwner() instanceof ServerPlayerEntity)
                || !(wither.getWorld() instanceof ServerWorld world)) return;
        int armorDamage = skull.isCharged() ? 10 : 1;
        MobCombatAttributesRegistry.damageArmor(wither, MobCombatAttributesRegistry.ArmorSection.HEAD, armorDamage);
        MobCombatAttributesRegistry.damageArmor(wither, MobCombatAttributesRegistry.ArmorSection.BODY, armorDamage);
        Vec3d pos = skull.getPos();
        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z,
                280, 1.2, 1.2, 1.2, 0.65);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z,
                120, 0.9, 0.9, 0.9, 0.45);
    }

    private static Entity find(MinecraftServer server, UUID uuid) {
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(uuid);
            if (entity != null) return entity;
        }
        return null;
    }

    private enum Phase { IDLE, EXPLOSION_WINDUP, CHARGE_WINDUP, CHARGING }

    private static final class State {
        private Phase phase = Phase.IDLE;
        private int ticks;
        private int cooldown;
        private int explosionCooldown = 400;
        private long lastSkullFireTick = Long.MIN_VALUE / 2;
        private Vec3d chargeStep = Vec3d.ZERO;
        private Vec3d windupPosition;
        private final Set<UUID> hitPlayers = new HashSet<>();

        private State(int cooldown) {
            this.cooldown = cooldown;
        }
    }
}
