package com.kingdomcomecombat.network;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;

/** Entity-tracking-aware fan-out for transient combat state. */
public final class CombatNetworkBroadcaster {
    private CombatNetworkBroadcaster() {
    }

    public static void sendTrackingAndSelf(Entity entity, CustomPayload payload) {
        for (ServerPlayerEntity observer : PlayerLookup.tracking(entity)) {
            sendIfSupported(observer, payload);
        }
        // PlayerLookup.tracking(entity) deliberately excludes the tracked player.
        if (entity instanceof ServerPlayerEntity player) {
            sendIfSupported(player, payload);
        }
    }

    public static void sendTracking(Entity entity, CustomPayload payload) {
        for (ServerPlayerEntity observer : PlayerLookup.tracking(entity)) {
            sendIfSupported(observer, payload);
        }
    }

    private static void sendIfSupported(ServerPlayerEntity player, CustomPayload payload) {
        if (ServerPlayNetworking.canSend(player, payload.getId())) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
