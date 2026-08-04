package com.kingdomcomecombat.client.animation;

import com.kingdomcomecombat.client.mixin.EntityRenderStateKccAccess;
import com.kingdomcomecombat.collision.HumanoidHurtboxLibrary;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.SkeletonEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.SkeletonEntityRenderState;
import net.minecraft.client.render.entity.state.ZombieEntityRenderState;
import java.util.Optional;

public class VanillaSkeletonGeckoAnimationApplier {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private VanillaSkeletonGeckoAnimationApplier() {
    }

    public static void apply(
            SkeletonEntityModel<SkeletonEntityRenderState> model,
            SkeletonEntityRenderState state
    ) {
        applyBiped(model, state);
    }

    public static void applyBiped(
            BipedEntityModel<? extends BipedEntityRenderState> model,
            BipedEntityRenderState state
    ) {
        int entityId = ((EntityRenderStateKccAccess) state).kingdomcomecombat$getEntityId();
        if (!ClientEntityGeckoAnimationState.shouldRenderCombatAnimation(entityId)) {
            if (ClientHitReactionState.hasActive(entityId)) {
                applyHitReactionOnly(model, entityId);
            }
            return;
        }
        ClientEntityGeckoAnimationState.ActiveAnimation animation =
                ClientEntityGeckoAnimationState.get(entityId);

        if (animation == null) {
            if (ClientHitReactionState.hasActive(entityId)) {
                applyHitReactionOnly(model, entityId);
            }
            return;
        }

        if (state instanceof SkeletonEntityRenderState skeletonState) {
            skeletonState.attacking = false;
        } else if (state instanceof ZombieEntityRenderState zombieState) {
            zombieState.attacking = false;
        }
        state.handSwingProgress = 0.0F;

        for (ClientEntityGeckoAnimationState.ActiveAnimation layer :
                ClientEntityGeckoAnimationState.getLayers(entityId)) {
            applyAnimationLayer(model, layer);
        }

        if (ClientHitReactionState.hasActive(entityId)) {
            applyHitReactionOnly(model, entityId);
        }
    }

    public static void applyBipedCombatLayers(
            BipedEntityModel<? extends BipedEntityRenderState> model,
            BipedEntityRenderState state
    ) {
        int entityId = ((EntityRenderStateKccAccess) state).kingdomcomecombat$getEntityId();
        if (!ClientEntityGeckoAnimationState.shouldRenderCombatAnimation(entityId)) return;
        if (ClientEntityGeckoAnimationState.get(entityId) == null) return;
        if (state instanceof SkeletonEntityRenderState skeletonState) {
            skeletonState.attacking = false;
        } else if (state instanceof ZombieEntityRenderState zombieState) {
            zombieState.attacking = false;
        }
        state.handSwingProgress = 0.0F;
        for (ClientEntityGeckoAnimationState.ActiveAnimation layer :
                ClientEntityGeckoAnimationState.getLayers(entityId)) {
            applyAnimationLayer(model, layer);
        }
    }

    public static void applyIllager(
            int entityId,
            ModelPart root,
            ModelPart head,
            ModelPart rightArm,
            ModelPart leftArm,
            ModelPart rightLeg,
            ModelPart leftLeg
    ) {
        if (!ClientEntityGeckoAnimationState.shouldRenderCombatAnimation(entityId)) {
            if (ClientHitReactionState.hasActive(entityId)) {
                applyIllagerHitReactionOnly(entityId, head, rightArm, leftArm, rightLeg, leftLeg);
            }
            return;
        }
        ClientEntityGeckoAnimationState.ActiveAnimation animation =
                ClientEntityGeckoAnimationState.get(entityId);

        if (animation == null) {
            if (ClientHitReactionState.hasActive(entityId)) {
                applyIllagerHitReactionOnly(entityId, head, rightArm, leftArm, rightLeg, leftLeg);
            }
            return;
        }

        for (ClientEntityGeckoAnimationState.ActiveAnimation layer :
                ClientEntityGeckoAnimationState.getLayers(entityId)) {
            applyIllagerAnimationLayer(root, head, rightArm, leftArm, rightLeg, leftLeg, layer);
        }

        if (ClientHitReactionState.hasActive(entityId)) {
            applyIllagerHitReactionOnly(entityId, head, rightArm, leftArm, rightLeg, leftLeg);
        }
    }

    public static void applyGenericParts(
            int entityId,
            ModelPart body,
            ModelPart head,
            ModelPart rightArm,
            ModelPart leftArm,
            ModelPart rightLeg,
            ModelPart leftLeg
    ) {
        if (!ClientEntityGeckoAnimationState.shouldRenderCombatAnimation(entityId)) return;
        for (ClientEntityGeckoAnimationState.ActiveAnimation layer :
                ClientEntityGeckoAnimationState.getLayers(entityId)) {
            applyBone(body, sampleBone(layer, "torso"), layer.weight());
            applyBone(head, sampleBone(layer, "head"), layer.weight());
            applyBone(rightArm, sampleBone(layer, "rightArm"), layer.weight());
            applyBone(leftArm, sampleBone(layer, "leftArm"), layer.weight());
            applyBone(rightLeg, sampleBone(layer, "rightLeg"), layer.weight());
            applyBone(leftLeg, sampleBone(layer, "leftLeg"), layer.weight());
        }

        if (ClientHitReactionState.hasActive(entityId)) {
            applyIllagerHitReactionOnly(entityId, head, rightArm, leftArm, rightLeg, leftLeg);
        }
    }

    private static void applyAnimationLayer(
            BipedEntityModel<? extends BipedEntityRenderState> model,
            ClientEntityGeckoAnimationState.ActiveAnimation animation
    ) {
        // PAL's PlayerModelMixin maps the model torso to the "torso" channel.
        // The separate "body" channel is applied to the renderer's root matrix
        // by MobPalBodyTransformApplier, exactly as PAL's PlayerRendererMixin does.
        applyBone(model.body, sampleBone(animation, "torso"), animation.weight());
        applyBone(model.head, sampleBone(animation, "head"), animation.weight());
        applyBone(model.rightArm, sampleBone(animation, "rightArm"), animation.weight());
        applyBone(model.leftArm, sampleBone(animation, "leftArm"), animation.weight());
        applyBone(model.rightLeg, sampleBone(animation, "rightLeg"), animation.weight());
        applyBone(model.leftLeg, sampleBone(animation, "leftLeg"), animation.weight());
    }

    private static void applyIllagerAnimationLayer(
            ModelPart root,
            ModelPart head,
            ModelPart rightArm,
            ModelPart leftArm,
            ModelPart rightLeg,
            ModelPart leftLeg,
            ClientEntityGeckoAnimationState.ActiveAnimation animation
    ) {
        ModelPart body = root != null && root.hasChild("body") ? root.getChild("body") : null;

        if (body != null) {
            applyBone(body, sampleBone(animation, "torso"), animation.weight());
            applyBone(head, sampleBone(animation, "head"), animation.weight());
            applyBone(rightArm, sampleBone(animation, "rightArm"), animation.weight());
            applyBone(leftArm, sampleBone(animation, "leftArm"), animation.weight());
            applyBone(rightLeg, sampleBone(animation, "rightLeg"), animation.weight());
            applyBone(leftLeg, sampleBone(animation, "leftLeg"), animation.weight());
            return;
        }

        applyBone(root, sampleBone(animation, "torso"), animation.weight());
        applyBone(head, sampleBone(animation, "head"), animation.weight());
        applyBone(rightArm, sampleBone(animation, "rightArm"), animation.weight());
        applyBone(leftArm, sampleBone(animation, "leftArm"), animation.weight());
        applyBone(rightLeg, sampleBone(animation, "rightLeg"), animation.weight());
        applyBone(leftLeg, sampleBone(animation, "leftLeg"), animation.weight());
    }

    private static void applyIllagerHitReactionOnly(
            int entityId,
            ModelPart head,
            ModelPart rightArm,
            ModelPart leftArm,
            ModelPart rightLeg,
            ModelPart leftLeg
    ) {
        ClientHitReactionState.BoneDelta bodyDelta =
                ClientHitReactionState.getDelta(entityId, HumanoidHurtboxLibrary.Part.BODY);
        applyHitReaction(head, parentedDelta(
                ClientHitReactionState.getDelta(entityId, HumanoidHurtboxLibrary.Part.HEAD),
                bodyDelta,
                0.55F
        ));
        applyHitReaction(rightArm, parentedDelta(
                ClientHitReactionState.getDelta(entityId, HumanoidHurtboxLibrary.Part.RIGHT_ARM),
                bodyDelta,
                0.85F
        ));
        applyHitReaction(leftArm, parentedDelta(
                ClientHitReactionState.getDelta(entityId, HumanoidHurtboxLibrary.Part.LEFT_ARM),
                bodyDelta,
                0.85F
        ));
        applyHitReaction(rightLeg, entityId, HumanoidHurtboxLibrary.Part.RIGHT_LEG);
        applyHitReaction(leftLeg, entityId, HumanoidHurtboxLibrary.Part.LEFT_LEG);
    }

    private static void applyHitReactionOnly(
            BipedEntityModel<? extends BipedEntityRenderState> model,
            int entityId
    ) {
        ClientHitReactionState.BoneDelta bodyDelta =
                ClientHitReactionState.getDelta(entityId, HumanoidHurtboxLibrary.Part.BODY);
        applyHitReaction(model.body, bodyDelta);
        applyHitReaction(model.head, parentedDelta(
                ClientHitReactionState.getDelta(entityId, HumanoidHurtboxLibrary.Part.HEAD),
                bodyDelta,
                0.55F
        ));
        applyHitReaction(model.rightArm, parentedDelta(
                ClientHitReactionState.getDelta(entityId, HumanoidHurtboxLibrary.Part.RIGHT_ARM),
                bodyDelta,
                0.85F
        ));
        applyHitReaction(model.leftArm, parentedDelta(
                ClientHitReactionState.getDelta(entityId, HumanoidHurtboxLibrary.Part.LEFT_ARM),
                bodyDelta,
                0.85F
        ));
        applyHitReaction(model.rightLeg, entityId, HumanoidHurtboxLibrary.Part.RIGHT_LEG);
        applyHitReaction(model.leftLeg, entityId, HumanoidHurtboxLibrary.Part.LEFT_LEG);
    }

    private static void applyHitReaction(
            ModelPart part,
            int entityId,
            HumanoidHurtboxLibrary.Part hitboxPart
    ) {
        ClientHitReactionState.BoneDelta delta =
                ClientHitReactionState.getDelta(entityId, hitboxPart);
        applyHitReaction(part, delta);
    }

    private static void applyHitReaction(
            ModelPart part,
            ClientHitReactionState.BoneDelta delta
    ) {
        part.originX += delta.positionX();
        part.originY += delta.positionY();
        part.originZ += delta.positionZ();
        part.pitch += delta.rotationX() * DEG_TO_RAD;
        part.yaw += delta.rotationY() * DEG_TO_RAD;
        part.roll += delta.rotationZ() * DEG_TO_RAD;
    }

    private static ClientHitReactionState.BoneDelta parentedDelta(
            ClientHitReactionState.BoneDelta child,
            ClientHitReactionState.BoneDelta parent,
            float parentInfluence
    ) {
        return new ClientHitReactionState.BoneDelta(
                child.positionX() + parent.positionX() * parentInfluence,
                child.positionY() + parent.positionY() * parentInfluence,
                child.positionZ() + parent.positionZ() * parentInfluence,
                child.rotationX() + parent.rotationX() * parentInfluence,
                child.rotationY() + parent.rotationY() * parentInfluence,
                child.rotationZ() + parent.rotationZ() * parentInfluence
        );
    }

    private static BoneTransform sampleBone(
            ClientEntityGeckoAnimationState.ActiveAnimation animation,
            String boneName
    ) {
        if (!animation.customAnimationName().isBlank()) {
            GeckoLikeAnimationLibrary.BoneTransform transform =
                    GeckoLikeAnimationLibrary.sampleNamedBone(
                            animation.customAnimationName(),
                            boneName,
                            animation.elapsedSeconds()
                    );
            return new BoneTransform(transform.rotation(), transform.position());
        }

        Optional<GeckoLikeAnimationLibrary.BonePose> rotation =
                GeckoLikeAnimationLibrary.sampleRotation(
                        animation.kind(),
                        animation.direction(),
                        boneName,
                        animation.elapsedSeconds()
                );

        Optional<GeckoLikeAnimationLibrary.BonePose> position =
                GeckoLikeAnimationLibrary.samplePosition(
                        animation.kind(),
                        animation.direction(),
                        boneName,
                        animation.elapsedSeconds()
                );

        return new BoneTransform(rotation, position);
    }

    private static void applyBone(
            ModelPart part,
            BoneTransform own,
            float weight
    ) {
        if (part == null) return;
        ModelTransform defaults = part.getDefaultTransform();

        own.rotation().ifPresent(pose -> {
            // Match PAL RenderUtil.translatePartToBone exactly: X/Z are the
            // animation's absolute rotations, while Y keeps the model's bind yaw.
            // Adding bind pitch/roll here twists custom humanoid arms noticeably
            // in animations with large roll values (for example halfsword).
            part.pitch = lerp(part.pitch, pose.x() * DEG_TO_RAD, weight);
            part.yaw = lerp(part.yaw, defaults.yaw() + pose.y() * DEG_TO_RAD, weight);
            part.roll = lerp(part.roll, pose.z() * DEG_TO_RAD, weight);
        });

        own.position().ifPresent(pose -> {
            part.originX = lerp(part.originX, defaults.x() + pose.x(), weight);
            // Player Animation Library converts animation-space Y to Minecraft
            // model-space Y by negating it in RenderUtil.translatePartToBone.
            part.originY = lerp(part.originY, defaults.y() - pose.y(), weight);
            part.originZ = lerp(part.originZ, defaults.z() + pose.z(), weight);
        });
    }

    private static float lerp(float from, float to, float progress) {
        progress = Math.max(0.0F, Math.min(1.0F, progress));
        return from + (to - from) * progress;
    }

    private record BoneTransform(
            Optional<GeckoLikeAnimationLibrary.BonePose> rotation,
            Optional<GeckoLikeAnimationLibrary.BonePose> position
    ) {
    }
}
