package com.kingdomcomecombat.client.animation;

import com.kingdomcomecombat.client.lockon.LockOnState;
import com.kingdomcomecombat.client.combat.CombatClientState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Smooth torso lean derived from world movement while an entity is in combat stance. */
public final class ClientLockedMovementLeanState {
    private static final float MAX_FORWARD_DEGREES = 7.0F;
    private static final float MAX_SIDE_DEGREES = 9.0F;
    private static final Map<Integer, Lean> LEANS = new HashMap<>();

    private ClientLockedMovementLeanState() {
    }

    public static boolean hasActive(int entityId) {
        Lean lean = LEANS.get(entityId);
        return lean != null && (Math.abs(lean.forward) > 0.02F || Math.abs(lean.side) > 0.02F);
    }

    public static void applyBiped(
            int entityId,
            ModelPart body,
            ModelPart head,
            ModelPart rightArm,
            ModelPart leftArm,
            ModelPart rightLeg,
            ModelPart leftLeg
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        Entity entity = client.world == null ? null : client.world.getEntityById(entityId);
        if (entity == null || body == null) {
            LEANS.remove(entityId);
            return;
        }

        boolean localLocked = entity == client.player && LockOnState.locked;
        boolean syncedLocked = ClientEntityGeckoAnimationState.hasActiveStance(entityId);
        boolean attacking = ClientEntityGeckoAnimationState.hasActiveAttack(entityId)
                || entity == client.player && CombatClientState.attacking;

        Vec3d velocity = entity.getVelocity();
        float yaw = entity.getBodyYaw() * ((float) Math.PI / 180.0F);
        float localForward = (float) (-velocity.x * Math.sin(yaw) + velocity.z * Math.cos(yaw));
        float localSide = (float) (velocity.x * Math.cos(yaw) + velocity.z * Math.sin(yaw));
        boolean moving = velocity.x * velocity.x + velocity.z * velocity.z > 0.0004;

        float targetForward = 0.0F;
        float targetSide = 0.0F;
        if ((localLocked || syncedLocked) && moving && !attacking && !ClientDodgeAnimationState.hasActive(entityId)) {
            targetForward = MathHelper.clamp(localForward * 38.0F, -MAX_FORWARD_DEGREES, MAX_FORWARD_DEGREES);
            targetSide = MathHelper.clamp(-localSide * 46.0F, -MAX_SIDE_DEGREES, MAX_SIDE_DEGREES);
        }

        float now = clientTick();
        Lean lean = LEANS.computeIfAbsent(entityId, ignored -> new Lean(now));
        float elapsed = MathHelper.clamp(now - lean.lastTick, 0.0F, 2.0F);
        lean.lastTick = now;
        float response = attacking ? 0.62F : 0.34F;
        float alpha = 1.0F - (float) Math.pow(1.0F - response, elapsed);
        lean.forward = MathHelper.lerp(alpha, lean.forward, targetForward);
        lean.side = MathHelper.lerp(alpha, lean.side, targetSide);

        if (Math.abs(lean.forward) < 0.01F && Math.abs(lean.side) < 0.01F
                && targetForward == 0.0F && targetSide == 0.0F) {
            lean.forward = 0.0F;
            lean.side = 0.0F;
        }
        ClientDodgeAnimationState.applyTorsoLean(
                body, head, rightArm, leftArm, rightLeg, leftLeg,
                lean.forward, lean.side
        );
    }

    public static void tickCleanup() {
        MinecraftClient client = MinecraftClient.getInstance();
        float now = clientTick();
        Iterator<Map.Entry<Integer, Lean>> iterator = LEANS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Lean> entry = iterator.next();
            if (client.world == null || client.world.getEntityById(entry.getKey()) == null
                    || now - entry.getValue().lastTick > 40.0F) {
                iterator.remove();
            }
        }
    }

    private static float clientTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.world == null ? 0.0F
                : client.world.getTime() + client.getRenderTickCounter().getTickProgress(false);
    }

    private static final class Lean {
        private float forward;
        private float side;
        private float lastTick;

        private Lean(float lastTick) {
            this.lastTick = lastTick;
        }
    }
}
