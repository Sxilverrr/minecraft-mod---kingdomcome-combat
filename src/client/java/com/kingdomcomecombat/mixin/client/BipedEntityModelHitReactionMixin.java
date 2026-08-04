package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.collision.ClientModelHurtboxCache;
import com.kingdomcomecombat.client.collision.ClientCollisionTrackingPolicy;
import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.client.animation.ClientDodgeAnimationState;
import com.kingdomcomecombat.client.animation.ClientHitReactionState;
import com.kingdomcomecombat.client.animation.ClientLockedMovementLeanState;
import com.kingdomcomecombat.client.animation.MobStanceHeadTargeting;
import com.kingdomcomecombat.client.animation.VanillaSkeletonGeckoAnimationApplier;
import com.kingdomcomecombat.client.compat.FirstPersonRenderCompat;
import com.kingdomcomecombat.client.mixin.EntityRenderStateKccAccess;
import com.kingdomcomecombat.collision.HumanoidHurtboxLibrary;
import com.kingdomcomecombat.compat.GuardVillagersCompat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.SkeletonEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public class BipedEntityModelHitReactionMixin<T extends BipedEntityRenderState> {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    @Shadow public ModelPart head;
    @Shadow public ModelPart body;
    @Shadow public ModelPart rightArm;
    @Shadow public ModelPart leftArm;
    @Shadow public ModelPart rightLeg;
    @Shadow public ModelPart leftLeg;

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V", at = @At("TAIL"))
    private void kingdomcomecombat$applyGenericHitReaction(T state, CallbackInfo ci) {
        if (FirstPersonRenderCompat.isExternalBodyRenderOrPreparing()) {
            return;
        }
        if (state instanceof SkeletonEntityRenderState) {
            return;
        }

        int entityId = ((EntityRenderStateKccAccess) state).kingdomcomecombat$getEntityId();
        MinecraftClient client = MinecraftClient.getInstance();
        net.minecraft.entity.Entity renderedEntity = client.world == null ? null : client.world.getEntityById(entityId);
        boolean renderCombatAnimation =
                ClientEntityGeckoAnimationState.shouldRenderCombatAnimation(entityId);
        boolean trackCollision = renderedEntity instanceof net.minecraft.entity.LivingEntity living
                && ClientCollisionTrackingPolicy.shouldCaptureHurtbox(living);
        if (!renderCombatAnimation
                && !ClientHitReactionState.hasActive(entityId)
                && !ClientDodgeAnimationState.hasActive(entityId)
                && !ClientLockedMovementLeanState.hasActive(entityId)
                && !trackCollision) {
            return;
        }
        boolean genericConfiguredHumanoid = renderedEntity instanceof net.minecraft.entity.LivingEntity living
                && renderCombatAnimation
                && !HumanoidHurtboxLibrary.isBuiltInHumanoidTarget(living);
        if (renderCombatAnimation
                && (GuardVillagersCompat.isGuard(renderedEntity) || genericConfiguredHumanoid)) {
            BipedEntityModel<T> model = (BipedEntityModel<T>) (Object) this;
            if (genericConfiguredHumanoid) {
                VanillaSkeletonGeckoAnimationApplier.applyBipedCombatLayers(model, state);
            } else {
                VanillaSkeletonGeckoAnimationApplier.applyBiped(model, state);
            }
            if (ClientEntityGeckoAnimationState.isStanceOnly(entityId)) {
                MobStanceHeadTargeting.apply(entityId, state, head, body);
            }
        }

        ClientHitReactionState.BoneDelta bodyDelta =
                ClientHitReactionState.getDelta(entityId, HumanoidHurtboxLibrary.Part.BODY);
        apply(body, bodyDelta);
        apply(head, parentedDelta(
                ClientHitReactionState.getDelta(entityId, HumanoidHurtboxLibrary.Part.HEAD),
                bodyDelta,
                0.55F
        ));
        apply(leftArm, parentedDelta(
                ClientHitReactionState.getDelta(entityId, HumanoidHurtboxLibrary.Part.LEFT_ARM),
                bodyDelta,
                1.0F
        ));
        apply(rightArm, parentedDelta(
                ClientHitReactionState.getDelta(entityId, HumanoidHurtboxLibrary.Part.RIGHT_ARM),
                bodyDelta,
                1.0F
        ));
        apply(leftLeg, entityId, HumanoidHurtboxLibrary.Part.LEFT_LEG);
        apply(rightLeg, entityId, HumanoidHurtboxLibrary.Part.RIGHT_LEG);
        ClientDodgeAnimationState.applyBiped(entityId, body, head, rightArm, leftArm, rightLeg, leftLeg);
        ClientLockedMovementLeanState.applyBiped(entityId, body, head, rightArm, leftArm, rightLeg, leftLeg);
        // Armor and other feature models also call BipedEntityModel#setAngles
        // after the player's PAL-animated base model. They must not replace
        // the rendered player hurtbox with their copied/vanilla pose.
        if (renderedEntity instanceof net.minecraft.entity.player.PlayerEntity
                && !((Object) this instanceof PlayerEntityModel)) {
            return;
        }
        if (!ClientModelHurtboxCache.shouldCaptureRenderedPose(entityId)) {
            return;
        }
        ClientModelHurtboxCache.update(entityId, head, body, rightArm, leftArm, rightLeg, leftLeg);
    }

    private static void apply(
            ModelPart part,
            int entityId,
            HumanoidHurtboxLibrary.Part hitboxPart
    ) {
        ClientHitReactionState.BoneDelta delta =
                ClientHitReactionState.getDelta(entityId, hitboxPart);
        apply(part, delta);
    }

    private static void apply(
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
}
