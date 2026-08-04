package com.kingdomcomecombat.client.animation;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Applies the shared data-driven combat poses to the pre-render-state model API. */
public final class LegacyBipedAnimationApplier {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private LegacyBipedAnimationApplier() {
    }

    public static void apply(BipedEntityModel<? extends LivingEntity> model, LivingEntity entity) {
        clearExternalPartAnimation(model, model.head, model.body, model.rightArm,
                model.leftArm, model.rightLeg, model.leftLeg);
        for (ClientEntityGeckoAnimationState.ActiveAnimation layer
                : ClientEntityGeckoAnimationState.getLayers(entity.getId())) {
            // PAL applies "body" at renderer/root level. The model torso uses
            // the separate "torso" channel; using body here applies it twice.
            GeckoLikeAnimationLibrary.BoneTransform body = sample(layer, "torso");
            apply(model.body, body, layer.weight());
            apply(model.head, sample(layer, "head"), layer.weight());
            apply(model.rightArm, sample(layer, "rightArm"), layer.weight());
            apply(model.leftArm, sample(layer, "leftArm"), layer.weight());
            apply(model.rightLeg, sample(layer, "rightLeg"), layer.weight());
            apply(model.leftLeg, sample(layer, "leftLeg"), layer.weight());
            // In the modern model the torso is a parent transform. Legacy
            // BipedEntityModel stores every limb as a sibling, so propagate
            // the torso delta after applying each limb's local animation.
            propagateTorso(model, body, layer.weight());
        }
    }

    private static void propagateTorso(
            BipedEntityModel<? extends LivingEntity> model,
            GeckoLikeAnimationLibrary.BoneTransform torso,
            float weight
    ) {
        GeckoLikeAnimationLibrary.BonePose rotation = torso.rotation()
                .orElse(GeckoLikeAnimationLibrary.BonePose.ZERO);
        GeckoLikeAnimationLibrary.BonePose position = torso.position()
                .orElse(GeckoLikeAnimationLibrary.BonePose.ZERO);
        float clamped = Math.max(0.0F, Math.min(1.0F, weight));
        Quaternionf parentRotation = new Quaternionf().rotateZYX(
                rotation.z() * DEG_TO_RAD * clamped,
                rotation.y() * DEG_TO_RAD * clamped,
                rotation.x() * DEG_TO_RAD * clamped);
        float translateX = position.x() * clamped;
        float translateY = -position.y() * clamped;
        float translateZ = position.z() * clamped;
        ModelTransform bodyDefault = model.body.getDefaultTransform();
        propagateChild(model.head, bodyDefault, parentRotation, rotation,
                translateX, translateY, translateZ, clamped);
        propagateChild(model.rightArm, bodyDefault, parentRotation, rotation,
                translateX, translateY, translateZ, clamped);
        propagateChild(model.leftArm, bodyDefault, parentRotation, rotation,
                translateX, translateY, translateZ, clamped);
        propagateChild(model.rightLeg, bodyDefault, parentRotation, rotation,
                translateX, translateY, translateZ, clamped);
        propagateChild(model.leftLeg, bodyDefault, parentRotation, rotation,
                translateX, translateY, translateZ, clamped);
    }

    private static void propagateChild(
            ModelPart child,
            ModelTransform bodyDefault,
            Quaternionf parentRotation,
            GeckoLikeAnimationLibrary.BonePose rotation,
            float translateX,
            float translateY,
            float translateZ,
            float weight
    ) {
        ModelTransform defaults = child.getDefaultTransform();
        Vector3f bind = new Vector3f(
                defaults.pivotX - bodyDefault.pivotX,
                defaults.pivotY - bodyDefault.pivotY,
                defaults.pivotZ - bodyDefault.pivotZ);
        Vector3f rotated = parentRotation.transform(new Vector3f(bind));
        child.pivotX += rotated.x - bind.x + translateX;
        child.pivotY += rotated.y - bind.y + translateY;
        child.pivotZ += rotated.z - bind.z + translateZ;
        child.pitch += rotation.x() * DEG_TO_RAD * weight;
        child.yaw += rotation.y() * DEG_TO_RAD * weight;
        child.roll += rotation.z() * DEG_TO_RAD * weight;
    }

    public static void applyParts(
            LivingEntity entity,
            ModelPart body,
            ModelPart head,
            ModelPart rightArm,
            ModelPart leftArm,
            ModelPart rightLeg,
            ModelPart leftLeg
    ) {
        clearExternalPartAnimation(null, head, body, rightArm, leftArm, rightLeg, leftLeg);
        for (ClientEntityGeckoAnimationState.ActiveAnimation layer
                : ClientEntityGeckoAnimationState.getLayers(entity.getId())) {
            // The renderer-level legacy bridge already applies the PAL "body"
            // root. IllagerEntityModel needs the separate torso channel here;
            // applying body again displaces both the mob and its held item.
            GeckoLikeAnimationLibrary.BoneTransform torso = sample(layer, "torso");
            apply(body, torso, layer.weight());
            apply(head, sample(layer, "head"), layer.weight());
            apply(rightArm, sample(layer, "rightArm"), layer.weight());
            apply(leftArm, sample(layer, "leftArm"), layer.weight());
            apply(rightLeg, sample(layer, "rightLeg"), layer.weight());
            apply(leftLeg, sample(layer, "leftLeg"), layer.weight());
            propagateTorsoParts(body, head, rightArm, leftArm, rightLeg, leftLeg,
                    torso, layer.weight());
        }
    }

    private static void propagateTorsoParts(
            ModelPart body,
            ModelPart head,
            ModelPart rightArm,
            ModelPart leftArm,
            ModelPart rightLeg,
            ModelPart leftLeg,
            GeckoLikeAnimationLibrary.BoneTransform torso,
            float weight
    ) {
        GeckoLikeAnimationLibrary.BonePose rotation = torso.rotation()
                .orElse(GeckoLikeAnimationLibrary.BonePose.ZERO);
        GeckoLikeAnimationLibrary.BonePose position = torso.position()
                .orElse(GeckoLikeAnimationLibrary.BonePose.ZERO);
        float clamped = Math.max(0.0F, Math.min(1.0F, weight));
        Quaternionf parentRotation = new Quaternionf().rotateZYX(
                rotation.z() * DEG_TO_RAD * clamped,
                rotation.y() * DEG_TO_RAD * clamped,
                rotation.x() * DEG_TO_RAD * clamped);
        ModelTransform bodyDefault = body.getDefaultTransform();
        for (ModelPart child : new ModelPart[]{head, rightArm, leftArm, rightLeg, leftLeg}) {
            propagateChild(child, bodyDefault, parentRotation, rotation,
                    position.x() * clamped,
                    -position.y() * clamped,
                    position.z() * clamped,
                    clamped);
        }
    }

    private static GeckoLikeAnimationLibrary.BoneTransform sample(
            ClientEntityGeckoAnimationState.ActiveAnimation layer,
            String bone
    ) {
        if (!layer.customAnimationName().isBlank()) {
            return GeckoLikeAnimationLibrary.sampleNamedBone(
                    layer.customAnimationName(), bone, layer.elapsedSeconds());
        }
        return GeckoLikeAnimationLibrary.sampleBone(
                layer.kind(), layer.direction(), bone, layer.elapsedSeconds());
    }

    private static void apply(
            ModelPart part,
            GeckoLikeAnimationLibrary.BoneTransform transform,
            float weight
    ) {
        ModelTransform defaults = part.getDefaultTransform();
        transform.rotation().ifPresent(pose -> {
            part.pitch = lerp(part.pitch, defaults.pitch + pose.x() * DEG_TO_RAD, weight);
            part.yaw = lerp(part.yaw, defaults.yaw + pose.y() * DEG_TO_RAD, weight);
            part.roll = lerp(part.roll, defaults.roll + pose.z() * DEG_TO_RAD, weight);
        });
        transform.position().ifPresent(pose -> {
            part.pivotX = lerp(part.pivotX, defaults.pivotX + pose.x(), weight);
            part.pivotY = lerp(part.pivotY, defaults.pivotY - pose.y(), weight);
            part.pivotZ = lerp(part.pivotZ, defaults.pivotZ + pose.z(), weight);
        });
    }

    private static float lerp(float from, float to, float weight) {
        return from + (to - from) * Math.max(0.0F, Math.min(1.0F, weight));
    }

    /**
     * Player Animator compatible mobs may retain a second transform supplier
     * which is evaluated while ModelPart renders. Clear it only while KCC owns
     * an active combat pose, otherwise that late transform masks our limbs.
     */
    private static void clearExternalPartAnimation(Object model, ModelPart... parts) {
        if (model != null) {
            try {
                Method getter = model.getClass().getMethod("getEmoteSupplier");
                Object supplier = getter.invoke(model);
                if (supplier != null) {
                    for (Method method : supplier.getClass().getMethods()) {
                        if (method.getName().equals("set") && method.getParameterCount() == 1) {
                            method.invoke(supplier, new Object[]{null});
                            break;
                        }
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Player Animator is optional and most models do not expose it.
            }
        }

        try {
            Class<?> helperClass = Class.forName("dev.kosmx.playerAnim.impl.animation.IBendHelper");
            Field instanceField = helperClass.getField("INSTANCE");
            Object helper = instanceField.get(null);
            Method bend = null;
            for (Method method : helperClass.getMethods()) {
                if (method.getName().equals("bend") && method.getParameterCount() == 2) {
                    bend = method;
                    break;
                }
            }
            if (bend == null) return;
            for (ModelPart part : parts) {
                bend.invoke(helper, part, null);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            // No Player Animator bend state exists in this installation.
        }
    }
}
