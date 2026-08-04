package com.kingdomcomecombat.client.stamina;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ClientEntityStaminaState {
    private static final int STALE_TICKS = 200;
    private static final Map<Integer, Snapshot> SNAPSHOTS = new HashMap<>();

    private ClientEntityStaminaState() {
    }

    public static void update(int entityId, float current, float max) {
        SNAPSHOTS.put(entityId, new Snapshot(current, Math.max(0.0F, max), clientTick()));
    }

    public static float progressOrFull(int entityId) {
        Snapshot snapshot = SNAPSHOTS.get(entityId);
        if (snapshot == null) {
            return 1.0F;
        }
        if (snapshot.max <= 0.0F) {
            return 0.0F;
        }
        return MathHelper.clamp(snapshot.current / snapshot.max, 0.0F, 1.0F);
    }

    public static void tickCleanup() {
        int now = clientTick();
        Iterator<Map.Entry<Integer, Snapshot>> iterator = SNAPSHOTS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue().lastUpdateTick > STALE_TICKS) {
                iterator.remove();
            }
        }
    }

    private static int clientTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player == null ? 0 : client.player.age;
    }

    private record Snapshot(float current, float max, int lastUpdateTick) {
    }
}
