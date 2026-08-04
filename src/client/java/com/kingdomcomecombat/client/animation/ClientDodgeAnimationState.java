package com.kingdomcomecombat.client.animation;

import com.kingdomcomecombat.combat.CombatControlConfig;
import com.kingdomcomecombat.combat.DodgeDirection;
import com.kingdomcomecombat.config.CombatClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ClientDodgeAnimationState {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private static final Map<Integer, DodgeAnimation> DODGES = new HashMap<>();

    private ClientDodgeAnimationState() {
    }

    public static boolean hasActive(int entityId) {
        return DODGES.containsKey(entityId);
    }

    public static void start(int entityId, DodgeDirection direction) {
        if (!CombatClientConfig.dodgeAnimationEnabled()) {
            return;
        }
        float now = clientTick();
        DodgeAnimation current = DODGES.get(entityId);
        // The initiating client predicts locally, then receives the server's
        // tracking broadcast. Do not restart the same multiplayer animation.
        if (current != null
                && current.direction() == direction
                && now - current.startTick() < 3.0F) {
            return;
        }
        DODGES.put(entityId, new DodgeAnimation(direction, now));
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
        if (!CombatClientConfig.dodgeAnimationEnabled()) {
            return;
        }

        DodgeAnimation animation = DODGES.get(entityId);
        if (animation == null || body == null) {
            return;
        }

        float age = clientTick() - animation.startTick();
        if (age < 0.0F || age > CombatControlConfig.DODGE_TOTAL_TICKS) {
            DODGES.remove(entityId);
            return;
        }

        float progress = age / Math.max(1.0F, CombatControlConfig.DODGE_TOTAL_TICKS);
        float weight = (float) Math.sin(progress * Math.PI);
        weight *= (float) MathHelper.clamp(CombatControlConfig.getDodgeSpeedScale((int) age), 0.25, 1.35);
        weight = MathHelper.clamp(weight, 0.0F, 1.0F);
        float curve = weight * weight * (3.0F - 2.0F * weight);

        float forwardTilt = switch (animation.direction()) {
            case FORWARD -> 13.0F;
            case BACK -> -15.0F;
            case LEFT, RIGHT -> 4.0F;
        };
        float sideTilt = switch (animation.direction()) {
            case LEFT -> 20.0F;
            case RIGHT -> -20.0F;
            default -> 0.0F;
        };
        float legYaw = switch (animation.direction()) {
            case LEFT -> 18.0F;
            case RIGHT -> -18.0F;
            case FORWARD -> 0.0F;
            case BACK -> 0.0F;
        };
        float legPitch = switch (animation.direction()) {
            case FORWARD -> 10.0F;
            case BACK -> -10.0F;
            default -> 0.0F;
        };

        float sink = 2.1F * curve;
        float longitudinalTravel = switch (animation.direction()) {
            case FORWARD -> -5.25F * curve;
            case BACK -> 4.50F * curve;
            case LEFT, RIGHT -> 0.0F;
        };
        body.originY += sink;
        body.originZ += longitudinalTravel;
        body.pitch += forwardTilt * curve * DEG_TO_RAD;
        body.roll += sideTilt * curve * DEG_TO_RAD;

        applyChildDodge(head, body, sink, forwardTilt, sideTilt, curve, 0.80F, 0.18F, 0.0F, 0.0F);
        applyChildDodge(rightArm, body, sink, forwardTilt, sideTilt, curve, 1.0F, -0.22F, 0.0F, 0.0F);
        applyChildDodge(leftArm, body, sink, forwardTilt, sideTilt, curve, 1.0F, -0.22F, 0.0F, 0.0F);
        applyChildDodge(rightLeg, body, sink, forwardTilt, sideTilt, curve, 0.92F, -0.55F, -legPitch, legYaw);
        applyChildDodge(leftLeg, body, sink, forwardTilt, sideTilt, curve, 0.92F, -0.55F, -legPitch, legYaw);
        // Biped limbs are siblings rather than children of the body part. Move
        // every part together so forward/back dodges visibly travel instead of
        // only leaning around the torso pivot.
        if (longitudinalTravel != 0.0F) {
            if (head != null) head.originZ += longitudinalTravel;
            if (rightArm != null) rightArm.originZ += longitudinalTravel;
            if (leftArm != null) leftArm.originZ += longitudinalTravel;
            if (rightLeg != null) rightLeg.originZ += longitudinalTravel;
            if (leftLeg != null) leftLeg.originZ += longitudinalTravel;
        }
    }

    public static void tickCleanup() {
        float now = clientTick();
        Iterator<Map.Entry<Integer, DodgeAnimation>> iterator = DODGES.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue().startTick() > CombatControlConfig.DODGE_TOTAL_TICKS + 4) {
                iterator.remove();
            }
        }
    }

    static void applyTorsoLean(
            ModelPart body,
            ModelPart head,
            ModelPart rightArm,
            ModelPart leftArm,
            ModelPart rightLeg,
            ModelPart leftLeg,
            float forwardTilt,
            float sideTilt
    ) {
        if (body == null) {
            return;
        }
        body.pitch += forwardTilt * DEG_TO_RAD;
        body.roll += sideTilt * DEG_TO_RAD;
        applyChildDodge(head, body, 0.0F, forwardTilt, sideTilt, 1.0F, 0.72F, -0.38F, 0.0F, 0.0F);
        applyChildDodge(rightArm, body, 0.0F, forwardTilt, sideTilt, 1.0F, 0.88F, -0.30F, 0.0F, 0.0F);
        applyChildDodge(leftArm, body, 0.0F, forwardTilt, sideTilt, 1.0F, 0.88F, -0.30F, 0.0F, 0.0F);
        applyChildDodge(rightLeg, body, 0.0F, forwardTilt, sideTilt, 1.0F, 0.45F, -0.45F, 0.0F, 0.0F);
        applyChildDodge(leftLeg, body, 0.0F, forwardTilt, sideTilt, 1.0F, 0.45F, -0.45F, 0.0F, 0.0F);
    }

    private static void applyChildDodge(
            ModelPart part,
            ModelPart body,
            float sink,
            float forwardTilt,
            float sideTilt,
            float weight,
            float parentInfluence,
            float counterTilt,
            float extraPitch,
            float extraYaw
    ) {
        if (part == null) {
            return;
        }
        ModelTransform defaults = part.getDefaultTransform();
        ModelTransform bodyDefaults = body.getDefaultTransform();
        float pitch = forwardTilt * weight * DEG_TO_RAD;
        float roll = sideTilt * weight * DEG_TO_RAD;
        Quaternionf bodyRotation = new Quaternionf()
                .rotateX(pitch)
                .rotateZ(roll);
        Vector3f bindOffset = new Vector3f(
                defaults.x() - bodyDefaults.x(),
                defaults.y() - bodyDefaults.y(),
                defaults.z() - bodyDefaults.z()
        );
        Vector3f rotatedOffset = bodyRotation.transform(new Vector3f(bindOffset)).sub(bindOffset);
        part.originX += rotatedOffset.x * parentInfluence;
        part.originY += sink * parentInfluence + rotatedOffset.y * parentInfluence;
        part.originZ += rotatedOffset.z * parentInfluence;
        part.pitch += (forwardTilt * parentInfluence + forwardTilt * counterTilt + extraPitch) * weight * DEG_TO_RAD;
        part.roll += (sideTilt * parentInfluence + sideTilt * counterTilt) * weight * DEG_TO_RAD;
        part.yaw += extraYaw * weight * DEG_TO_RAD;
    }

    private static float clientTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.world == null ? 0.0F : client.world.getTime() + client.getRenderTickCounter().getTickProgress(false);
    }

    private record DodgeAnimation(DodgeDirection direction, float startTick) {
    }
}
