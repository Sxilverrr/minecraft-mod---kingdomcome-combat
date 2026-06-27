package com.kingdomcomecombat.client.feedback;

import net.minecraft.client.MinecraftClient;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CustomHurtOverlaySuppressor {
    private static final Map<Integer, Integer> SUPPRESSED_UNTIL_TICK = new HashMap<>();

    private CustomHurtOverlaySuppressor() {
    }

    public static void suppress(int entityId, int ticks) {
        MinecraftClient client = MinecraftClient.getInstance();
        int currentTick = client.player == null ? 0 : client.player.age;
        SUPPRESSED_UNTIL_TICK.put(entityId, currentTick + Math.max(1, ticks));
    }

    public static boolean shouldSuppress(int entityId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return false;
        }

        Integer untilTick = SUPPRESSED_UNTIL_TICK.get(entityId);
        if (untilTick == null) {
            return false;
        }

        if (client.player.age > untilTick) {
            SUPPRESSED_UNTIL_TICK.remove(entityId);
            return false;
        }

        return true;
    }

    public static void tickCleanup() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            SUPPRESSED_UNTIL_TICK.clear();
            return;
        }

        int currentTick = client.player.age;
        Iterator<Map.Entry<Integer, Integer>> iterator = SUPPRESSED_UNTIL_TICK.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Integer> entry = iterator.next();
            if (currentTick > entry.getValue()
                    || client.world.getEntityById(entry.getKey()) == null) {
                iterator.remove();
            }
        }
    }
}
