package com.kingdomcomecombat.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import com.kingdomcomecombat.entity.HandCannonBulletEntity;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public final class HandCannonProjectileTracker {
    private static final Set<UUID> PROJECTILES = new HashSet<>();

    private HandCannonProjectileTracker() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(HandCannonProjectileTracker::tick);
    }

    public static void track(Entity projectile) {
        if (projectile != null) {
            PROJECTILES.add(projectile.getUuid());
        }
    }

    public static boolean isTracked(Entity projectile) {
        return projectile != null && PROJECTILES.contains(projectile.getUuid());
    }

    private static void tick(MinecraftServer server) {
        Iterator<UUID> iterator = PROJECTILES.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            Entity projectile = find(server, uuid);
            if (projectile == null || projectile.isRemoved() || !projectile.isAlive()) {
                iterator.remove();
                continue;
            }
            if (projectile.getWorld() instanceof ServerWorld world) {
                net.minecraft.particle.ParticleEffect trail = projectile instanceof HandCannonBulletEntity bullet
                        ? switch (bullet.getAmmoType()) {
                            case HEAVY -> ParticleTypes.LARGE_SMOKE;
                            case BUCKSHOT -> ParticleTypes.POOF;
                            default -> ParticleTypes.CAMPFIRE_COSY_SMOKE;
                        }
                        : ParticleTypes.CAMPFIRE_COSY_SMOKE;
                world.spawnParticles(
                        trail,
                        projectile.getX(),
                        projectile.getY(),
                        projectile.getZ(),
                        2,
                        0.04,
                        0.04,
                        0.04,
                        0.0
                );
            }
        }
    }

    private static Entity find(MinecraftServer server, UUID uuid) {
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(uuid);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }
}
