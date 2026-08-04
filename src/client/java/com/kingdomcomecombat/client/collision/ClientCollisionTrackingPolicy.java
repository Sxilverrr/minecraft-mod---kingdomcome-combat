package com.kingdomcomecombat.client.collision;

import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.client.combat.CombatClientState;
import com.kingdomcomecombat.client.config.ClientServerConfigState;
import com.kingdomcomecombat.client.lockon.LockOnState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;

/** Keeps expensive rendered collision data limited to entities that can currently use it. */
public final class ClientCollisionTrackingPolicy {
    private ClientCollisionTrackingPolicy() {
    }

    public static boolean shouldCaptureHurtbox(LivingEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!isInRange(client, entity)) return false;
        if (debugHitboxesEnabled(client)) return true;
        return CombatClientState.attacking
                // A remote attack only needs the local player's rendered box;
                // entity-vs-entity targets have deterministic animated fallbacks.
                || (entity == client.player && ClientEntityGeckoAnimationState.hasAnyActiveAttack())
                || entity.getId() == LockOnState.targetEntityId;
    }

    public static boolean shouldCaptureItemHitbox(LivingEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!isInRange(client, entity)) return false;
        if (debugHitboxesEnabled(client)) return true;
        return entity == client.player
                ? CombatClientState.attacking
                : ClientEntityGeckoAnimationState.hasActiveAttack(entity.getId());
    }

    public static double radius() {
        return ClientServerConfigState.collisionCacheRadius();
    }

    public static double radiusSquared() {
        double radius = radius();
        return radius * radius;
    }

    private static boolean isInRange(MinecraftClient client, LivingEntity entity) {
        if (client.world == null || client.player == null || entity == null || entity.isRemoved()) return false;
        return entity == client.player || entity.squaredDistanceTo(client.player) <= radiusSquared();
    }

    private static boolean debugHitboxesEnabled(MinecraftClient client) {
        return client.getEntityRenderDispatcher().shouldRenderHitboxes();
    }
}
