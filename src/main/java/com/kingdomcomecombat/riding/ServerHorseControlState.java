package com.kingdomcomecombat.riding;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerHorseControlState {
    private static final int MAX_INPUT_AGE_TICKS = 8;
    private static final Map<UUID, Input> INPUTS = new ConcurrentHashMap<>();

    private ServerHorseControlState() {
    }

    public static void update(
            ServerPlayerEntity player,
            float sideways,
            float forward,
            boolean sprintPressed,
            boolean jumpPressed,
            float yaw
    ) {
        float safeYaw = Float.isFinite(yaw) ? yaw : player.getYaw();
        INPUTS.put(
                player.getUuid(),
                new Input(
                        clamp(sideways),
                        clamp(forward),
                        sprintPressed,
                        jumpPressed,
                        safeYaw,
                        player.getWorld().getTime()
                )
        );
    }

    public static void clear(UUID playerUuid) {
        INPUTS.remove(playerUuid);
    }

    public static Input get(ServerPlayerEntity player) {
        Input input = INPUTS.get(player.getUuid());
        if (input == null || player.getWorld().getTime() - input.worldTick() > MAX_INPUT_AGE_TICKS) {
            return new Input(
                    clamp(player.sidewaysSpeed),
                    clamp(player.forwardSpeed),
                    player.isSprinting(),
                    false,
                    player.getYaw(),
                    player.getWorld().getTime()
            );
        }

        return input;
    }

    private static float clamp(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(-1.0F, Math.min(1.0F, value));
    }

    public record Input(
            float sideways,
            float forward,
            boolean sprintPressed,
            boolean jumpPressed,
            float yaw,
            long worldTick
    ) {
    }
}
