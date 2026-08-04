package com.kingdomcomecombat.combat;

import com.kingdomcomecombat.ai.HumanoidCombatAiTicker;
import com.kingdomcomecombat.collision.HumanoidHurtboxLibrary;
import com.kingdomcomecombat.network.CombatNetworkBroadcaster;
import com.kingdomcomecombat.network.EntityExecutionStunPayload;
import com.kingdomcomecombat.network.EntityHitReactionPayload;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class ServerExecutionState {
    public static final int STUN_TICKS = 40;
    private static final Map<UUID, Stun> STUNNED = new HashMap<>();

    private record Stun(UUID attackerUuid, long expiresAtTick, CombatDirection direction) {
    }

    private ServerExecutionState() {
    }

    public static void stun(ServerWorld world, ServerPlayerEntity attacker, LivingEntity target, CombatDirection direction) {
        long expiresAt = world.getTime() + STUN_TICKS;
        STUNNED.put(target.getUuid(), new Stun(attacker.getUuid(), expiresAt, direction));
        ServerCombatState.removeAttack(target.getUuid());
        ServerComboState.clear(target.getUuid());
        ServerBlockState.clearAll(target.getUuid());
        ServerCombatControlState.disableMovement(target.getUuid(), STUN_TICKS);
        ServerCombatControlState.disableAttack(target.getUuid(), STUN_TICKS);
        ServerCombatControlState.disableBlock(target.getUuid(), STUN_TICKS);
        ServerCombatControlState.disableDodge(target.getUuid(), STUN_TICKS);
        CreeperExplosionSuppressor.suppress(target, STUN_TICKS);
        if (target instanceof MobEntity mob) {
            HumanoidCombatAiTicker.interruptFollowUps(mob);
        }
        CombatNetworkBroadcaster.sendTrackingAndSelf(
                target,
                new EntityExecutionStunPayload(target.getId(), attacker.getId(), direction.ordinal(), STUN_TICKS)
        );
        CombatNetworkBroadcaster.sendTrackingAndSelf(
                target,
                new EntityHitReactionPayload(
                        target.getId(),
                        HumanoidHurtboxLibrary.Part.BODY.ordinal(),
                        "chest",
                        direction.ordinal(),
                        false,
                        0.35F,
                        "stunded",
                        false
                )
        );
    }

    public static boolean canExecute(ServerWorld world, ServerPlayerEntity attacker, LivingEntity target) {
        Stun stun = STUNNED.get(target.getUuid());
        return stun != null
                && stun.attackerUuid().equals(attacker.getUuid())
                && stun.expiresAtTick() >= world.getTime()
                && target.isAlive();
    }

    public static CombatDirection stunDirection(LivingEntity target, CombatDirection fallback) {
        Stun stun = STUNNED.get(target.getUuid());
        return stun == null ? fallback : stun.direction();
    }

    public static void consume(LivingEntity target) {
        STUNNED.remove(target.getUuid());
        CreeperExplosionSuppressor.suppress(target, STUN_TICKS);
    }

    public static void tick(long worldTick) {
        Iterator<Map.Entry<UUID, Stun>> iterator = STUNNED.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().expiresAtTick() < worldTick) {
                iterator.remove();
            }
        }
    }

    public static void clear(UUID entityUuid) {
        STUNNED.remove(entityUuid);
        CreeperExplosionSuppressor.clear(entityUuid);
    }
}
