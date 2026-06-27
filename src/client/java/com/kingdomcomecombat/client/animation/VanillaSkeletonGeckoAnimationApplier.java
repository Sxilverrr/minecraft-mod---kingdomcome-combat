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
import org.joml.Quaternionf;
import org.joml.Vector3f;

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
        ClientEntityGeckoAnimationState.ActiveAnimation animation =
                ClientEntityGeckoAnimationState.get(entityId);

        if (animation == null) {
            applyHitReactionOnly(model, entityId);
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

        applyHitReactionOnly(model, entityId);
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
        ClientEntityGeckoAnimationState.ActiveAnimation animation =
                ClientEntityGeckoAnimationState.get(entityId);

        if (animation == null) {
            applyIllagerHitReactionOnly(entityId, head, rightArm, leftArm, rightLeg, leftLeg);
            return;
        }

        for (ClientEntityGeckoAnimationState.ActiveAnimation layer :
                ClientEntityGeckoAnimationState.getLayers(entityId)) {
            applyIllagerAnimationLayer(root, head, rightArm, leftArm, rightLeg, leftLeg, layer);
        }

        applyIllagerHitReactionOnly(entityId, head, rightArm, leftArm, rightLeg, leftLeg);
    }

    private static void applyAnimationLayer(
            BipedEntityModel<? extends BipedEntityRenderState> model,
            ClientEntityGeckoAnimationState.ActiveAnimation animation
    ) {
        BoneTransform bodyTransform = sampleBone(animation, "body");

        applyRootBone(model.body, bodyTransform, animation.weight());
        applyChildBone(model.head, sampleBone(animation, "head"), animation.weight(), bodyTransform);
        applyChildBone(model.rightArm, sampleBone(animation, "rightArm"), animation.weight(), bodyTransform);
        applyChildBone(model.leftArm, sampleBone(animation, "leftArm"), animation.weight(), bodyTransform);
        applyChildBone(model.rightLeg, sampleBone(animation, "rightLeg"), animation.weight(), bodyTransform);
        applyChildBone(model.leftLeg, sampleBone(animation, "leftLeg"), animation.weight(), bodyTransform);
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
        BoneTransform bodyTransform = sampleBone(animation, "body");

        applyRootBone(root, bodyTransform, animation.weight());
        applyChildBone(head, sampleBone(animation, "head"), animation.weight(), BoneTransform.ZERO);
        applyChildBone(rightArm, sampleBone(animation, "rightArm"), animation.weight(), BoneTransform.ZERO);
        applyChildBone(leftArm, sampleBone(animation, "leftArm"), animation.weight(), BoneTransform.ZERO);
        applyChildBone(rightLeg, sampleBone(animation, "rightLeg"), animation.weight(), BoneTransform.ZERO);
        applyChildBone(leftLeg, sampleBone(animation, "leftLeg"), animation.weight(), BoneTransform.ZERO);
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

    private static void applyRootBone(
            ModelPart part,
            BoneTransform own,
            float weight
    ) {
        ModelTransform defaults = part.getDefaultTransform();

        own.rotation().ifPresent(pose -> {
            part.pitch = lerp(part.pitch, defaults.pitch() + pose.x() * DEG_TO_RAD, weight);
            part.yaw = lerp(part.yaw, defaults.yaw() + pose.y() * DEG_TO_RAD, weight);
            part.roll = lerp(part.roll, defaults.roll() + pose.z() * DEG_TO_RAD, weight);
        });

        own.position().ifPresent(pose -> {
            part.originX = lerp(part.originX, defaults.x() + pose.x(), weight);
            part.originY = lerp(part.originY, defaults.y() + pose.y(), weight);
            part.originZ = lerp(part.originZ, defaults.z() + pose.z(), weight);
        });
    }

    private static void applyChildBone(
            ModelPart part,
            BoneTransform own,
            float weight,
            BoneTransform parent
    ) {
        ModelTransform defaults = part.getDefaultTransform();

        own.rotation().ifPresent(pose -> {
            part.pitch = lerp(part.pitch, defaults.pitch() + pose.x() * DEG_TO_RAD, weight);
            part.yaw = lerp(part.yaw, defaults.yaw() + pose.y() * DEG_TO_RAD, weight);
            part.roll = lerp(part.roll, defaults.roll() + pose.z() * DEG_TO_RAD, weight);
        });

        own.position().ifPresent(pose -> {
            part.originX = lerp(part.originX, defaults.x() + pose.x(), weight);
            part.originY = lerp(part.originY, defaults.y() + pose.y(), weight);
            part.originZ = lerp(part.originZ, defaults.z() + pose.z(), weight);
        });

        applyParentTransform(part, parent, weight);
    }

    private static void applyParentTransform(
            ModelPart part,
            BoneTransform parent,
            float weight
    ) {
        if (parent.rotation().isEmpty() && parent.position().isEmpty()) {
            return;
        }

        ModelTransform defaults = part.getDefaultTransform();
        Vector3f baseOffset = new Vector3f(
                part.originX - defaults.x(),
                part.originY - defaults.y(),
                part.originZ - defaults.z()
        );

        parent.position().ifPresent(pose -> baseOffset.add(pose.x(), pose.y(), pose.z()));

        if (parent.rotation().isPresent()) {
            GeckoLikeAnimationLibrary.BonePose pose = parent.rotation().get();
            Quaternionf parentRotation = new Quaternionf()
                    .rotateZYX(
                            pose.z() * DEG_TO_RAD,
                            pose.y() * DEG_TO_RAD,
                            pose.x() * DEG_TO_RAD
                    );

            Vector3f bindOffset = new Vector3f(
                    defaults.x(),
                    defaults.y(),
                    defaults.z()
            );
            Vector3f rotatedOffset = parentRotation.transform(new Vector3f(bindOffset));
            rotatedOffset.sub(bindOffset);
            baseOffset.add(rotatedOffset);

            part.pitch = lerp(part.pitch, part.pitch + pose.x() * DEG_TO_RAD, weight);
            part.yaw = lerp(part.yaw, part.yaw + pose.y() * DEG_TO_RAD, weight);
            part.roll = lerp(part.roll, part.roll + pose.z() * DEG_TO_RAD, weight);
        }

        part.originX = lerp(part.originX, defaults.x() + baseOffset.x, weight);
        part.originY = lerp(part.originY, defaults.y() + baseOffset.y, weight);
        part.originZ = lerp(part.originZ, defaults.z() + baseOffset.z, weight);
    }

    private static float lerp(float from, float to, float progress) {
        progress = Math.max(0.0F, Math.min(1.0F, progress));
        return from + (to - from) * progress;
    }

    private record BoneTransform(
            Optional<GeckoLikeAnimationLibrary.BonePose> rotation,
            Optional<GeckoLikeAnimationLibrary.BonePose> position
    ) {
        static final BoneTransform ZERO = new BoneTransform(Optional.empty(), Optional.empty());
    }
}
